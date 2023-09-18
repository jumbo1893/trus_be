package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.NotificationDTO;
import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.UserDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.response.SetupMatchResponse;
import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.filter.BaseSeasonFilter;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.entity.repository.specification.MatchSpecification;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.NotificationMapper;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.helper.PairSeasonMatch;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;
import static com.jumbo.trus.config.Config.AUTOMATIC_SEASON_ID;

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
