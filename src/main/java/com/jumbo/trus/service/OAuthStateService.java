package com.jumbo.trus.service;

import com.jumbo.trus.entity.OAuthStateEntity;
import com.jumbo.trus.repository.OAuthStateRepository;
import com.jumbo.trus.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class OAuthStateService {

    private final OAuthStateRepository oAuthStateRepository;
    private final UserService userService;

    public void addCodeVerifier(String codeVerifier, String system) {
        OAuthStateEntity oAuthStateEntity = new OAuthStateEntity();
        oAuthStateEntity.setCodeVerifier(codeVerifier);
        oAuthStateEntity.setSystem(system);
        oAuthStateEntity.setUser(userService.getCurrentUserEntity());
        oAuthStateEntity.setExpiresIn(Date.from(new Date().toInstant().plusSeconds(60)));
        oAuthStateRepository.save(oAuthStateEntity);
    }

    public String getCodeVerifier(String system) {
        OAuthStateEntity entity = oAuthStateRepository.findValidByUserAndSystem(userService.getCurrentUserEntity(), system).orElse(null);
        if (entity != null) {
            return entity.getCodeVerifier();
        }
        return null;
    }

    public void deleteCodeVerifier(String system) {
        oAuthStateRepository.deleteByUserAndSystem(userService.getCurrentUserEntity(), system);
    }
}

