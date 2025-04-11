package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.NotificationDTO;
import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.entity.repository.auth.AppTeamRepository;
import com.jumbo.trus.mapper.NotificationMapper;
import com.jumbo.trus.service.exceptions.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final HeaderManager headerManager;
    private final AppTeamRepository appTeamRepository;


    @Value("${notifications.enabled}")
    private boolean isNotificationsEnabled;


    public List<NotificationDTO> getAll(int limit, int page){
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(page, limit, sort);
        Long appTeamId = getCurrentAppTeamOrThrow().getId();
        return notificationRepository.findAllByAppTeamId(appTeamId, pageable)
                .stream()
                .map(notificationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void addNotification(String title, String text) {
        if (!isNotificationsEnabled) {
            return;
        }
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setDate(new Date());
        notificationEntity.setUserName(getCurrentUser().getName());
        notificationEntity.setTitle(title);
        notificationEntity.setText(text);
        notificationEntity.setAppTeam(getCurrentAppTeamOrThrow());
        notificationRepository.save(notificationEntity);
    }

    public void addAdminNotification(String title, String text, AppTeamEntity appTeamEntity) {
        if (!isNotificationsEnabled) {
            return;
        }
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setDate(new Date());
        notificationEntity.setUserName(Config.ADMIN_USER_NAME);
        notificationEntity.setTitle(title);
        notificationEntity.setText(text);
        notificationRepository.save(notificationEntity);
    }

    private UserEntity getCurrentUser() {
        try {
            return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (ClassCastException e) {
            throw new AuthException("Uživatel je odhlášen", AuthException.NOT_LOGGED_IN);
        }
    }

    private AppTeamEntity getCurrentAppTeamOrThrow() {
        Long id = headerManager.getAppTeamIdHeader();
        if (id == null) {
            throw new AuthException("Pro tuto operaci je třeba uvést ID týmu v hlavičce!", AuthException.MISSING_TEAM_ID);
        }
        return findAppTeamByIdOrThrow(id);
    }

    private AppTeamEntity findAppTeamByIdOrThrow(Long id) {
        return findAppTeamById(id)
                .orElseThrow(() -> new NotFoundException("App team s id " + id + " nenalezen v db"));
    }

    private Optional<AppTeamEntity> findAppTeamById(Long id) {
        return appTeamRepository.findById(id);
    }
}
