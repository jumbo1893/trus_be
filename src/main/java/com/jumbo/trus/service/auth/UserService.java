package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.dto.auth.UserSetup;
import com.jumbo.trus.dto.auth.UserTeamRoleDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.mapper.auth.UserTeamRoleMapper;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.exceptions.DuplicateEmailException;
import com.jumbo.trus.service.football.team.TeamProcessor;
import com.jumbo.trus.service.notification.push.DeviceTokenCollector;
import com.jumbo.trus.service.player.PlayerService;
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
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserTeamRoleMapper userTeamRoleMapper;
    private final TeamProcessor teamProcessor;
    private final PlayerService playerService;
    private final DeviceTokenCollector deviceTokenCollector;


    public UserSetup returnPlayerSetup(AppTeamEntity appTeamEntity) {
        UserSetup userSetup = new UserSetup();
        PlayerDTO noPlayer = playerService.noPlayer();
        userSetup.setCurrentUser(getCurrentUser());
        List<PlayerDTO> eligiblePlayers = new ArrayList<>(playerService.getAll(appTeamEntity.getId()));
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

    public UserDTO create(UserDTO user) {
        try {
            UserEntity entity = new UserEntity();
            entity.setMail(user.getMail().toLowerCase().trim());
            entity.setPassword(passwordEncoder.encode(user.getPassword()));
            entity.setName(user.getName().trim());
            entity = userRepository.save(entity);

            UserDTO dto = new UserDTO();
            dto.setId(entity.getId());
            dto.setMail(entity.getMail());
            dto.setAdmin(entity.isAdmin());
            //notificationService.addAdminNotification("Zaregistrován nový uživatel", entity.getMail());
            return dto;
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByMail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Uživatel " + username + " nenalezen"));
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public List<UserDTO> getAll(Long appTeamId, Boolean appTeamTeamRolesOnly) {
        List<UserDTO> userList = new ArrayList<>();
        List<UserEntity> entities = userRepository.findDistinctByTeamRoles_AppTeam_Id(appTeamId);
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

    public UserDTO editUser(Long userId, UserDTO user) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Uživatel s id " + userId + " nenalezen v db"));
        if (user.getAdmin() != null) {
            userEntity.setAdmin(user.getAdmin());
        }
        if (user.getName() != null) {
            userEntity.setName(user.getName());
        }
        return returnUserWithoutSensitiveData(userRepository.save(userEntity));
    }

    public UserDTO editUserById(Long userId, UserDTO user) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Uživatel s id " + userId + " nenalezen v db"));
        userEntity.setAdmin(user.getAdmin());
        userEntity.setName(user.getName());
        return returnUserWithoutSensitiveData(userRepository.save(userEntity));
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
                authentication.getAuthorities() // můžeš tam dát Collections.emptyList(), protože nepoužíváš authorities
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

}
