package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.notification.push.DeviceTokenDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.HeaderManager;
import com.jumbo.trus.service.auth.AuthService;
import com.jumbo.trus.service.notification.settings.EnabledPushNotificationInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeviceTokenCollector {

    private final DeviceTokenRepository deviceTokenRepository;
    private final AuthService authService;
    private final HeaderManager headerManager;
    private final EnabledPushNotificationInitializer enabledPushNotificationInitializer;

    public DeviceTokenDTO addNewToken(DeviceTokenDTO deviceTokenDTO) {
        String token = deviceTokenDTO.getToken();
        UserEntity currentUser = authService.getCurrentUserEntity();
        DeviceToken existing = deviceTokenRepository.findByToken(token).orElse(null);

        if (existing == null) {
            saveNewToken(token, currentUser);
        } else {
            updateIfDifferentUser(existing, currentUser);
        }
        enabledPushNotificationInitializer.ensureUserHasAllTypes(currentUser);
        return deviceTokenDTO;
    }

    public List<DeviceTokenDTO> getAllTokens() {
        return deviceTokenRepository.findAll()
                .stream()
                .map(DeviceTokenDTO::fromEntity)
                .toList();
    }

    public void initAllTokensForUsers() {
        List<DeviceToken> tokens = deviceTokenRepository.findAll();
        for (DeviceToken token : tokens) {
            enabledPushNotificationInitializer.ensureUserHasAllTypes(token.getUser());
        }
    }

    public List<DeviceToken> getTokensByUserList(List<Long> userIds) {
        return deviceTokenRepository.findByUser_IdIn(userIds);
    }

    public List<UserEntity> getAdminTokenUsersByAppTeam(Long appTeamId) {
        return deviceTokenRepository.findAdminUsersByAppTeamOrdered(appTeamId);
    }

    private void saveNewToken(String token, UserEntity user) {
        DeviceToken deviceToken = new DeviceToken(
                token,
                user,
                new Date(),
                headerManager.getDeviceHeader(),
                "ACTIVE"
        );
        deviceTokenRepository.save(deviceToken);
    }

    private void updateIfDifferentUser(DeviceToken existing, UserEntity currentUser) {
        if (!existing.getUser().equals(currentUser)) {
            existing.setUser(currentUser);
            existing.setRegistrationTime(new Date());
            existing.setDeviceType(headerManager.getDeviceHeader());
            deviceTokenRepository.save(existing);
        }
    }

    public void invalidateToken(DeviceToken deviceToken, String status) {
        deviceToken.setStatus(status);
        deviceToken.setModificationTime(new Date());
        deviceTokenRepository.save(deviceToken);
    }

    public void deleteToken(DeviceToken deviceToken) {
        deviceTokenRepository.delete(deviceToken);
    }
}

