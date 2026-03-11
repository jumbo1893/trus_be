package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.dto.auth.UserTeamRoleDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.mapper.auth.UserTeamRoleMapper;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.football.team.TeamProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserTeamRoleMapper userTeamRoleMapper;
    private final TeamProcessor teamProcessor;

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

}
