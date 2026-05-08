package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.notification.push.DeviceTokenDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.HeaderManager;
import com.jumbo.trus.service.auth.AuthService;
import com.jumbo.trus.service.notification.settings.EnabledPushNotificationInitializer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.webjars.NotFoundException;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeviceTokenCollector {

    private final DeviceTokenRepository deviceTokenRepository;
    private final AuthService authService;
    private final HeaderManager headerManager;
    private final EnabledPushNotificationInitializer enabledPushNotificationInitializer;

    @Transactional
    public DeviceTokenDTO addNewToken(DeviceTokenDTO deviceTokenDTO) {
        String token = deviceTokenDTO.getToken();
        String clientDeviceId = deviceTokenDTO.getClientDeviceId();

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("FCM token nesmí být prázdný");
        }

        if (clientDeviceId == null || clientDeviceId.isBlank()) {
            throw new IllegalArgumentException("clientDeviceId nesmí být prázdný");
        }

        UserEntity currentUser = authService.getCurrentUserEntity();

        DeviceToken savedToken = deviceTokenRepository.findByToken(token)
                .map(existing -> updateExistingToken(existing, currentUser, clientDeviceId))
                .orElseGet(() -> saveNewTokenAndInvalidateOldDeviceTokens(token, currentUser, clientDeviceId));

        enabledPushNotificationInitializer.ensureUserHasAllTypes(currentUser);

        return DeviceTokenDTO.fromEntity(savedToken);
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

    private DeviceToken saveNewTokenAndInvalidateOldDeviceTokens(
            String token,
            UserEntity user,
            String clientDeviceId
    ) {
        invalidateOldActiveTokensForDevice(user, clientDeviceId, token);

        DeviceToken deviceToken = new DeviceToken(
                token,
                user,
                new Date(),
                headerManager.getDeviceHeader(),
                "ACTIVE",
                clientDeviceId
        );

        return deviceTokenRepository.save(deviceToken);
    }

    private DeviceToken updateExistingToken(
            DeviceToken existing,
            UserEntity currentUser,
            String clientDeviceId
    ) {
        boolean changed = false;

        if (existing.getUser() == null || !existing.getUser().getId().equals(currentUser.getId())) {
            existing.setUser(currentUser);
            changed = true;
        }

        if (!"ACTIVE".equals(existing.getStatus())) {
            existing.setStatus("ACTIVE");
            changed = true;
        }

        if (!clientDeviceId.equals(existing.getClientDeviceId())) {
            existing.setClientDeviceId(clientDeviceId);
            changed = true;
        }

        String currentDeviceType = headerManager.getDeviceHeader();
        if (currentDeviceType != null && !currentDeviceType.equals(existing.getDeviceType())) {
            existing.setDeviceType(currentDeviceType);
            changed = true;
        }

        if (changed) {
            existing.setModificationTime(new Date());
            existing = deviceTokenRepository.save(existing);
        }

        invalidateOldActiveTokensForDevice(currentUser, clientDeviceId, existing.getToken());

        return existing;
    }

    private void invalidateOldActiveTokensForDevice(
            UserEntity user,
            String clientDeviceId,
            String currentToken
    ) {
        List<DeviceToken> oldActiveTokens =
                deviceTokenRepository.findByUser_IdAndClientDeviceIdAndStatus(
                        user.getId(),
                        clientDeviceId,
                        "ACTIVE"
                );

        for (DeviceToken oldToken : oldActiveTokens) {
            if (!oldToken.getToken().equals(currentToken)) {
                invalidateToken(oldToken, "REPLACED");
            }
        }
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

    public DeviceToken findByIdOrThrow(Long id) {
        return deviceTokenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Device token s id " + id + " nenalezen"));
    }
}

