package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.notification.push.DeviceTokenDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.service.HeaderManager;
import com.jumbo.trus.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeviceTokenCollector {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserService userService;
    private final HeaderManager headerManager;

    public DeviceTokenDTO addNewToken(DeviceTokenDTO deviceTokenDTO) {
        String token = deviceTokenDTO.getToken();
        UserEntity currentUser = userService.getCurrentUserEntity();
        DeviceToken existing = deviceTokenRepository.findByToken(token).orElse(null);

        if (existing == null) {
            saveNewToken(token, currentUser);
        } else {
            updateIfDifferentUser(existing, currentUser);
        }
        return deviceTokenDTO;
    }

    public List<DeviceTokenDTO> getAllTokens() {
        return deviceTokenRepository.findAll()
                .stream()
                .map(DeviceTokenDTO::fromEntity)
                .toList();
    }

    public List<DeviceToken> getTokensByUserList(List<Long> userIds) {
        return deviceTokenRepository.findByUser_IdIn(userIds);
    }

    private void saveNewToken(String token, UserEntity user) {
        DeviceToken deviceToken = new DeviceToken(
                token,
                user,
                new Date(),
                headerManager.getDeviceHeader()
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

    public void deleteToken(DeviceToken deviceToken) {
        deviceTokenRepository.delete(deviceToken);
    }
}

