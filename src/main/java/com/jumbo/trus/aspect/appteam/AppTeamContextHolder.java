package com.jumbo.trus.aspect.appteam;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AppTeamContextHolder {
    
    private static final ThreadLocal<AppTeamEntity> appTeamHolder = new ThreadLocal<>();

    public static void setAppTeam(AppTeamEntity appTeam) {
        appTeamHolder.set(appTeam);
    }

    public static AppTeamEntity getAppTeam() {
        return appTeamHolder.get();
    }

    public static void clear() {
        appTeamHolder.remove();
    }
}
