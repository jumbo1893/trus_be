package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.dto.auth.UserSetup;
import com.jumbo.trus.dto.auth.UserTeamRoleDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.config.security.firebase.FirebaseIdentity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserAvatar;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.auth.UserTeamRoleMapper;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.auth.UserAvatarRepository;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.exceptions.DuplicateEmailException;
import com.jumbo.trus.service.football.team.TeamProcessor;
import com.jumbo.trus.service.notification.push.DeviceTokenCollector;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.membership.MembershipService;
import com.jumbo.trus.service.outbox.OutboxEventService;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserAvatarRepository avatarRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserTeamRoleMapper userTeamRoleMapper;
    private final TeamProcessor teamProcessor;
    private final DeviceTokenCollector deviceTokenCollector;
    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final MembershipService membershipService;
    private final OutboxEventService outboxEventService;

    public UserSetup returnPlayerSetup(AppTeamEntity appTeamEntity) {
        UserSetup userSetup = new UserSetup();
        PlayerDTO noPlayer = PlayerService.noPlayer();
        userSetup.setCurrentUser(getCurrentUser());
        List<PlayerDTO> eligiblePlayers = new ArrayList<>(playerRepository.getAll(appTeamEntity.getId()).stream().map(playerMapper::toDTO).toList());
        eligiblePlayers.add(0, noPlayer);
        userSetup.setEligiblePlayersToPairWith(eligiblePlayers);
        UserDTO userWithCurrentTeamRole = getCurrentUser();
        removeAllTeamRolesExceptAppTeam(appTeamEntity.getId(), userWithCurrentTeamRole);
        if (!userWithCurrentTeamRole.getTeamRoles().isEmpty() && !userWithCurrentTeamRole.getTeamRoles().get(0).getRole().equals("ADMIN")) {
            List<UserDTO> usersWithToken = new ArrayList<>(deviceTokenCollector.getAdminTokenUsersByAppTeam(appTeamEntity.getId())
                    .stream()
                    .map(this::returnUserWithoutSensitiveData)
                    .toList());
            userSetup.setEligibleUsersToSendNotification(usersWithToken);
        }
        else {
            userSetup.setEligibleUsersToSendNotification(new ArrayList<>());
        }
        if (userWithCurrentTeamRole.getTeamRoles().isEmpty()) {
            userSetup.setPrimaryPlayer(noPlayer);
        }
        else if (userWithCurrentTeamRole.getTeamRoles().get(0).getPlayer() == null) {
            userSetup.setPrimaryPlayer(noPlayer);
        }
        else {
            userSetup.setPrimaryPlayer(userWithCurrentTeamRole.getTeamRoles().get(0).getPlayer());
        }
        return userSetup;
    }

    @Transactional
    public UserDTO create(UserDTO user) {
        UserEntity entity = new UserEntity();
        entity.setOnboardingCompleted(0);
        entity.setMail(user.getMail().toLowerCase().trim());
        entity.setPassword(passwordEncoder.encode(user.getPassword()));
        entity.setName(user.getName().trim());
        try {
            entity = userRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }
        membershipService.initializeBaselineForNewUser(entity);
        outboxEventService.createAuditEvent(OutboxEventType.USER_CREATED, entity.getId(), null, entity.getId());

        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setMail(entity.getMail());
        dto.setAdmin(entity.isAdmin());
        //notificationService.addAdminNotification("Zaregistrován nový uživatel", entity.getMail());
        return dto;
    }

    @Transactional
    public UserDTO provisionFirebaseUser(FirebaseIdentity identity, String requestedName) {
        if (identity.email() == null || identity.email().isBlank()) {
            throw new AuthException("Firebase účet neobsahuje e-mail", AuthException.NOT_LOGGED_IN);
        }

        String normalizedEmail = identity.email().trim().toLowerCase(Locale.ROOT);
        UserEntity existingByUid = userRepository.findByFirebaseUid(identity.uid()).orElse(null);
        if (existingByUid != null) {
            return returnUserWithoutSensitiveData(existingByUid);
        }

        UserEntity existingByEmail = userRepository.findByMailIgnoreCase(normalizedEmail).orElse(null);
        if (existingByEmail != null) {
            if (existingByEmail.getFirebaseUid() != null
                    && !existingByEmail.getFirebaseUid().equals(identity.uid())) {
                throw new AuthException(
                        "E-mail je už propojený s jiným Firebase účtem",
                        AuthException.INSUFFICIENT_RIGHTS
                );
            }
            existingByEmail.setFirebaseUid(identity.uid());
            if ((existingByEmail.getName() == null || existingByEmail.getName().isBlank())
                    && requestedName != null && !requestedName.isBlank()) {
                existingByEmail.setName(requestedName.trim());
            }
            UserEntity linked = userRepository.saveAndFlush(existingByEmail);
            outboxEventService.createAuditEvent(OutboxEventType.USER_UPDATED, linked.getId(), null, linked.getId());
            return returnUserWithoutSensitiveData(linked);
        }

        UserEntity entity = new UserEntity();
        entity.setFirebaseUid(identity.uid());
        entity.setOnboardingCompleted(0);
        entity.setMail(normalizedEmail);
        entity.setName(resolveRegistrationName(requestedName, identity));
        // Sloupec zůstává během kompatibilní migrace NOT NULL, ale nové přihlášení
        // už tento náhodný interní údaj nikdy nepoužívá.
        entity.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        try {
            entity = userRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
        membershipService.initializeBaselineForNewUser(entity);
        outboxEventService.createAuditEvent(OutboxEventType.USER_CREATED, entity.getId(), null, entity.getId());
        return returnUserWithoutSensitiveData(entity);
    }

    private String resolveRegistrationName(String requestedName, FirebaseIdentity identity) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }
        if (identity.displayName() != null && !identity.displayName().isBlank()) {
            return identity.displayName().trim();
        }
        int atIndex = identity.email().indexOf('@');
        return atIndex > 0 ? identity.email().substring(0, atIndex) : identity.email();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByMail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Uživatel " + username + " nenalezen"));
    }

    @Transactional
    public void deleteUser(Long userId) {
        avatarRepository.deleteById(userId);
        userRepository.deleteById(userId);
        outboxEventService.createAuditEvent(OutboxEventType.USER_DELETED, userId, null, userId);
    }

    @Transactional
    public void saveAvatar(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 2_000_000) {
            throw new IllegalArgumentException("Fotka musí mít nejvýše 2 MB");
        }
        byte[] bytes = file.getBytes();
        var reader = ImageIO.getImageReaders(ImageIO.createImageInputStream(new ByteArrayInputStream(bytes)));
        if (!reader.hasNext() || !"JPEG".equalsIgnoreCase(reader.next().getFormatName())) {
            throw new IllegalArgumentException("Soubor není platný obrázek");
        }
        Long userId = getCurrentUserEntity().getId();
        UserAvatar avatar = avatarRepository.findById(userId).orElseGet(UserAvatar::new);
        avatar.setUserId(userId);
        avatar.setImage(bytes);
        avatarRepository.save(avatar);
    }

    @Transactional(readOnly = true)
    public byte[] avatar(Long userId) {
        return avatarRepository.findById(userId).map(UserAvatar::getImage)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
    }

    public List<UserDTO> getAll(Long appTeamId, Boolean appTeamTeamRolesOnly) {
        List<UserDTO> userList = new ArrayList<>();
        List<UserEntity> entities = userRepository.findVisibleTeamUsers(
                appTeamId, getCurrentUserEntity().getId());
        for (UserEntity entity : entities) {
            UserDTO userDTO = returnUserWithoutSensitiveData(entity);
            if (appTeamTeamRolesOnly) {
                removeAllTeamRolesExceptAppTeam(appTeamId, userDTO);
            }
            userList.add(userDTO);
        }
        return userList;
    }

    private void removeAllTeamRolesExceptAppTeam(Long appTeamId, UserDTO userDTO) {
        List<UserTeamRoleDTO> newRoles = new ArrayList<>();
        for (UserTeamRoleDTO userTeamRoleDTO : userDTO.getTeamRoles()) {
            if (userTeamRoleDTO.getAppTeam().getId() == appTeamId) {
                newRoles.add(userTeamRoleDTO);
            }
        }
        userDTO.setTeamRoles(newRoles);
    }

    public UserEntity findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Uživatel id " + id + " nenalezen"));
    }

    @Transactional
    public UserDTO editUser(Long userId, UserDTO user) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Uživatel s id " + userId + " nenalezen v db"));
        if (user.getAdmin() != null) {
            userEntity.setAdmin(user.getAdmin());
        }
        if (user.getName() != null) {
            userEntity.setName(user.getName());
        }
        UserDTO saved = returnUserWithoutSensitiveData(userRepository.save(userEntity));
        outboxEventService.createAuditEvent(OutboxEventType.USER_UPDATED, userId, null, null);
        return saved;
    }

    @Transactional
    public UserDTO editUserById(Long userId, UserDTO user) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Uživatel s id " + userId + " nenalezen v db"));
        userEntity.setAdmin(user.getAdmin());
        userEntity.setName(user.getName());
        UserDTO saved = returnUserWithoutSensitiveData(userRepository.save(userEntity));
        outboxEventService.createAuditEvent(OutboxEventType.USER_UPDATED, userId, null, null);
        return saved;
    }

    public UserDTO getCurrentUser() {
        try {
            Long userId = getCurrentUserEntity().getId();
            UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Uživatel s id " + userId + " nenalezen v db"));
            return returnUserWithoutSensitiveData(user);
        } catch (ClassCastException e) {
            throw new AuthException("Uživatel je odhlášen", AuthException.NOT_LOGGED_IN);
        }
    }

    public UserEntity getCurrentUserEntity() {
        try {
            return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (ClassCastException e) {
            throw new AuthException("Uživatel je odhlášen", AuthException.NOT_LOGGED_IN);
        }
    }

    public UserDTO returnUserWithoutSensitiveData(UserEntity entity) {
        UserDTO dto = new UserDTO();
        dto.setName(entity.getName());
        dto.setId(entity.getId());
        dto.setMail(entity.getMail());
        dto.setAdmin(entity.isAdmin());
        dto.setTeamRoles(entity.getTeamRoles().stream().map(userTeamRoleMapper::toDTO).toList());
        for (UserTeamRoleDTO teamRole : dto.getTeamRoles())
            teamProcessor.enhanceTeamWithTableTeam(teamRole.getAppTeam().getTeam());
        return dto;
    }

    public void refreshUserInSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity oldUser)) {
            return;
        }

        UserEntity freshUser = findById(oldUser.getId()); // z DB
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                freshUser,
                authentication.getCredentials(),
                freshUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

}
