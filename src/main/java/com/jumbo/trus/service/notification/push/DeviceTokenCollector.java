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

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

        List<DeviceToken> tokensWithSameValue = deviceTokenRepository.findAllByToken(token);

        DeviceToken savedToken = tokensWithSameValue.isEmpty()
                ? saveNewTokenAndInvalidateOldDeviceTokens(token, currentUser, clientDeviceId)
                : updateExistingToken(chooseTokenToKeep(tokensWithSameValue, currentUser, clientDeviceId), currentUser, clientDeviceId);

        invalidateDuplicateTokensWithSameValue(tokensWithSameValue, savedToken);

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


    private DeviceToken chooseTokenToKeep(
            List<DeviceToken> tokensWithSameValue,
            UserEntity currentUser,
            String clientDeviceId
    ) {
        return tokensWithSameValue.stream()
                .filter(token -> token.getUser() != null && Objects.equals(token.getUser().getId(), currentUser.getId()))
                .filter(token -> Objects.equals(token.getClientDeviceId(), clientDeviceId))
                .filter(token -> "ACTIVE".equals(token.getStatus()))
                .findFirst()
                .orElseGet(() -> tokensWithSameValue.stream()
                        .filter(token -> token.getUser() != null && Objects.equals(token.getUser().getId(), currentUser.getId()))
                        .filter(token -> "ACTIVE".equals(token.getStatus()))
                        .findFirst()
                        .orElseGet(() -> tokensWithSameValue.stream()
                                .max(Comparator.comparing(
                                        token -> token.getModificationTime() != null
                                                ? token.getModificationTime()
                                                : token.getRegistrationTime(),
                                        Comparator.nullsLast(Comparator.naturalOrder())
                                ))
                                .orElseThrow(() -> new IllegalStateException("Nenalezen žádný token ke zpracování"))));
    }

    private void invalidateDuplicateTokensWithSameValue(List<DeviceToken> tokensWithSameValue, DeviceToken tokenToKeep) {
        for (DeviceToken duplicateToken : tokensWithSameValue) {
            if (!Objects.equals(duplicateToken.getId(), tokenToKeep.getId()) && "ACTIVE".equals(duplicateToken.getStatus())) {
                invalidateToken(duplicateToken, "DUPLICATE");
            }
        }
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

