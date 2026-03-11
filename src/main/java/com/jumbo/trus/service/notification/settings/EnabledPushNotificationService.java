package com.jumbo.trus.service.notification.settings;

import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.dto.notification.push.EnabledPushNotificationDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.settings.EnabledPushNotification;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.EnabledPushNotificationRepository;
import com.jumbo.trus.service.auth.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnabledPushNotificationService {

    private final EnabledPushNotificationRepository enRepo;
    private final UserService userService;

    public boolean isNotificationEnabled(UserEntity user, NotificationType notificationType) {
        EnabledPushNotification enabledPushNotification = enRepo.findByUserAndType(user, notificationType).orElse(new EnabledPushNotification(true));
        EnabledPushNotification globalEnabledPushNotification = enRepo.findByUserAndType(user, NotificationType.GLOBAL).orElse(new EnabledPushNotification(true));
        return enabledPushNotification.getEnabled() && globalEnabledPushNotification.getEnabled();
    }

    public List<EnabledPushNotificationDTO> getAllByUser() {
        List<EnabledPushNotificationDTO> enabledPushNotificationDTOS = new ArrayList<>(enRepo.findAllByUser(userService.getCurrentUserEntity())
                .stream()
                .map(this::toDto)
                .toList());
        enabledPushNotificationDTOS.sort(Comparator.comparingInt(d -> d.getType().getOrder()));
        return enabledPushNotificationDTOS;
    }

    public EnabledPushNotificationDTO editNotificationPermit(Long notificationId, EnabledPushNotificationDTO enabledPushNotificationDTO) throws NotFoundException {
        EnabledPushNotification foundEntity = enRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notifikace s id " + notificationId + "nenalezena v db"));
        EnabledPushNotification entity = toEntity(enabledPushNotificationDTO);
        entity.setId(notificationId);
        EnabledPushNotification savedEntity = enRepo.save(entity);
        return toDto(savedEntity);
    }

    @Transactional
    public StringAndString editNotificationsPermit(List<EnabledPushNotificationDTO> dtos) {
        String title = "změna oprávnění";
        if (dtos == null || dtos.isEmpty()) {
            return new StringAndString(title, "úspěšně změněno 0 oprávnění");
        }


        List<Long> ids = dtos.stream()
                .map(EnabledPushNotificationDTO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<EnabledPushNotification> existing = enRepo.findAllById(ids);
        Map<Long, EnabledPushNotification> byId = existing.stream()
                .collect(Collectors.toMap(EnabledPushNotification::getId, Function.identity()));

        List<Long> missing = ids.stream().filter(id -> !byId.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new NotFoundException("Notifikace s id " + missing + " nenalezena v db");
        }

        Date now = new Date();
        for (EnabledPushNotificationDTO dto : dtos) {
            EnabledPushNotification entity = byId.get(dto.getId());
            if (dto.getEnabled() != null) {
                entity.setEnabled(dto.getEnabled());
            }
            entity.setModificationTime(now); // pojistka k @PreUpdate
        }
        enRepo.saveAll(existing);

        return new StringAndString(title, "Úspěšně změněno");
    }

    private EnabledPushNotification toEntity(EnabledPushNotificationDTO enabledPushNotificationDTO) {
        return new EnabledPushNotification(enabledPushNotificationDTO.getId(),
                enabledPushNotificationDTO.getType(), enabledPushNotificationDTO.getEnabled(), userService.findById(enabledPushNotificationDTO.getUserId()), enabledPushNotificationDTO.getModificationTime());
    }

    private EnabledPushNotificationDTO toDto(EnabledPushNotification enabledPushNotification) {
        return new EnabledPushNotificationDTO(enabledPushNotification.getId(),
                enabledPushNotification.getType(), enabledPushNotification.getEnabled(), enabledPushNotification.getUser().getId(), enabledPushNotification.getModificationTime());
    }
}
