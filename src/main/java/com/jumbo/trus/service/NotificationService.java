package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.NotificationDTO;
import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.mapper.NotificationMapper;
import com.jumbo.trus.service.exceptions.AuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationMapper notificationMapper;


    public List<NotificationDTO> getAll(int limit, int page){
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        return notificationRepository.findAll(PageRequest.of(page, limit, sort)).stream().map(notificationMapper::toDTO).collect(Collectors.toList());
    }

    public void addNotification(String title, String text) {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setDate(new Date());
        notificationEntity.setUserName(getCurrentUser().getName());
        notificationEntity.setTitle(title);
        notificationEntity.setText(text);
        notificationRepository.save(notificationEntity);
    }

    public void addAdminNotification(String title, String text) {
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
}
