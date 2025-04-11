package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.dto.auth.UserTeamRoleDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.repository.auth.UserRepository;
import com.jumbo.trus.entity.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.mapper.auth.AppTeamMapper;
import com.jumbo.trus.mapper.auth.UserTeamRoleMapper;
import com.jumbo.trus.service.NotificationService;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.exceptions.DuplicateEmailException;
import com.jumbo.trus.service.football.team.TeamService;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final NotificationService notificationService;
    private final UserTeamRoleMapper userTeamRoleMapper;
    private final AppTeamMapper appTeamMapper;
    private final TeamService teamService;
    private final PlayerService playerService;
    private final UserTeamRoleRepository userTeamRoleRepository;

    //MIGRACE
    public void migrateAllUsers(AppTeamEntity appTeamEntity) {
        List<UserEntity> users = userRepository.findAll();
        for (UserEntity user : users) {
            UserTeamRole userTeamRole = new UserTeamRole();
            userTeamRole.setAppTeam(appTeamEntity);
            Long playerId = userRepository.findPlayerId(user.getId());
            if (playerId != null) {
                userTeamRole.setPlayer(playerService.getPlayerEntity(playerId));
            }
            userTeamRole.setUser(user);
            userTeamRole.setRole(user.isAdmin() ? "ADMIN" : "READER");
            userTeamRoleRepository.save(userTeamRole);
        }
    }

    public UserDTO create(UserDTO user) {
        try {
            UserEntity entity = new UserEntity();
            entity.setMail(user.getMail().toLowerCase().trim());
            entity.setPassword(passwordEncoder.encode(user.getPassword()));

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

    public void registerAppTeam(Long footballTeamId, Long name) {

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
            teamService.enhanceTeamWithFootballTeam(teamRole.getAppTeam().getTeam());
        return dto;
    }
}
