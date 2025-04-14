package com.jumbo.trus.aspect.appteam;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.auth.AppTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreAppTeamAspect {

    private final AppTeamService appTeamService;

    @Before("@within(StoreAppTeam) || @annotation(StoreAppTeam)")
    public void storeAppTeamId() {

        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        AppTeamContextHolder.setAppTeam(appTeam);
    }

    @After("@within(StoreAppTeam) || @annotation(StoreAppTeam)")
    public void clearAppTeamId() {
        AppTeamContextHolder.clear();
    }
}
