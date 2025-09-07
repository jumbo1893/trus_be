package com.jumbo.trus.service.notification.push;

import com.jumbo.trus.dto.log.LogDTO;
import com.jumbo.trus.entity.log.LogEntity;
import com.jumbo.trus.repository.log.LogRepository;
import com.jumbo.trus.service.HeaderManager;
import com.jumbo.trus.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final UserService userService;
    private final HeaderManager headerManager;

    public LogDTO addLog(LogDTO logDTO) {
        LogEntity entity = new LogEntity(logDTO, userService.getCurrentUserEntity(), headerManager.getDeviceHeader());
        logRepository.save(entity);
        return logDTO;

    }
}

