package com.jumbo.trus.config.security;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.HeaderManager;
import com.jumbo.trus.service.exceptions.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Aspect
public class RoleValidationAspect {

    @Autowired
    private HeaderManager headerManager;

    private static final List<String> ROLE_HIERARCHY = List.of(
            "ADMIN",
            "READER",
            "NONE"
    );

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
            throw new AuthException("Pro tuto operaci je třeba uvést ID týmu v hlavičce!", AuthException.MISSING_TEAM_ID);
        }

        boolean hasAccess = user.getTeamRoles().stream()
                .anyMatch(role -> role.getAppTeam().getId().equals(appTeamId) &&
                        hasRequiredOrHigherRole(role.getRole().toUpperCase(), requiredRole));

        if (!hasAccess) {
            throw new AuthException("Nemáš dostatečná práva na operaci pro tým " + appTeamId + "!", AuthException.INSUFFICIENT_RIGHTS);
        }
    }

    private boolean hasRequiredOrHigherRole(String userRole, String requiredRole) {
        int userRoleIndex = ROLE_HIERARCHY.indexOf(userRole);
        int requiredRoleIndex = ROLE_HIERARCHY.indexOf(requiredRole);
        return userRoleIndex != -1 && requiredRoleIndex != -1 && userRoleIndex <= requiredRoleIndex;
    }
}
