package com.jumbo.trus.aspect.appteam;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AppTeamContextHolder {
    
    private static final ThreadLocal<AppTeamEntity> appTeamHolder = new ThreadLocal<>();

    public static void setAppTeam(AppTeamEntity appTeam) {
        log.debug("setAppTeam : {}", appTeam.getId());
        appTeamHolder.set(appTeam);
    }

    public static AppTeamEntity getAppTeam() {
        log.debug("getAppTeam : {}", appTeamHolder.get().getId());
        return appTeamHolder.get();
    }

    public static void clear() {
        log.debug("clear");
        appTeamHolder.remove();
    }
}
