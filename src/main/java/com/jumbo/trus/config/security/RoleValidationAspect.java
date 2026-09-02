package com.jumbo.trus.config.security;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.TeamRole;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.header.HeaderManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Aspect
public class RoleValidationAspect {

    @Autowired
    private HeaderManager headerManager;

    @Autowired
    private AppTeamRepository appTeamRepository;

    @Before("@annotation(roleRequired)")
    public void validateRole(RoleRequired roleRequired) throws AuthException {
        String requiredRole = roleRequired.value();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AuthException("Nelze pokračovat, nejste přihlášen!", AuthException.NOT_LOGGED_IN);
        }

        if ("NONE".equals(requiredRole)) {
            return; // Žádná role není potřeba
        }

        Long appTeamId = headerManager.getAppTeamIdHeader();
        if (appTeamId == null) {
            throw new AuthException(
                    "Nejprve vyber tým, pro který chceš operaci provést.",
                    AuthException.MISSING_TEAM_ID
            );
        }

        boolean hasAccess = user.getTeamRoles().stream()
                .anyMatch(role -> role.getAppTeam().getId().equals(appTeamId) &&
                        hasRequiredOrHigherRole(role.getRole().toUpperCase(), requiredRole));

        if (!hasAccess) {
            throw insufficientRights(user, appTeamId, requiredRole);
        }
    }

    private AuthException insufficientRights(UserEntity user, Long appTeamId, String requiredRole) {
        UserTeamRole currentRole = user.getTeamRoles().stream()
                .filter(role -> role.getAppTeam().getId().equals(appTeamId))
                .findFirst()
                .orElse(null);
        String teamName = currentRole == null
                ? appTeamRepository.findById(appTeamId)
                    .map(team -> team.getName())
                    .filter(name -> name != null && !name.isBlank())
                    .orElse("vybraný tým")
                : displayTeamName(currentRole);

        if (currentRole == null) {
            return new AuthException(
                    "K týmu „" + teamName + "“ nemáš přístup.",
                    AuthException.INSUFFICIENT_RIGHTS
            );
        }

        String message = "ADMIN".equalsIgnoreCase(requiredRole)
                ? "Do administrace týmu „" + teamName
                    + "“ mají přístup pouze administrátoři. Tvoje aktuální práva: "
                    + roleLabel(currentRole.getRole()) + "."
                : "Pro tuto operaci v týmu „" + teamName + "“ potřebuješ práva "
                    + roleLabel(requiredRole) + ". Tvoje aktuální práva: "
                    + roleLabel(currentRole.getRole()) + ".";
        return new AuthException(message, AuthException.INSUFFICIENT_RIGHTS);
    }

    private String displayTeamName(UserTeamRole role) {
        String name = role.getAppTeam().getName();
        return name == null || name.isBlank() ? "vybraný tým" : name;
    }

    private String roleLabel(String role) {
        try {
            return switch (TeamRole.from(role)) {
                case ADMIN -> "administrátor";
                case EDITOR -> "čtení a editace";
                case READER -> "pouze čtení";
            };
        } catch (RuntimeException exception) {
            return "bez oprávnění";
        }
    }

    private boolean hasRequiredOrHigherRole(String userRole, String requiredRole) {
        try {
            return TeamRole.from(userRole).hasAtLeast(TeamRole.from(requiredRole));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
