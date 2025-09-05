package com.jumbo.trus.service.activity;

import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.strava.AthleteActivities;
import com.jumbo.trus.dto.strava.StravaActivity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.strava.ActivityRepository;
import com.jumbo.trus.repository.strava.AthleteRepository;
import com.jumbo.trus.entity.strava.ActivityEntity;
import com.jumbo.trus.entity.strava.AthleteEntity;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.StravaActivityMapper;
import com.jumbo.trus.mapper.auth.UserMapper;
import com.jumbo.trus.service.football.match.FootballMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StravaService {

    private final AthleteRepository athleteRepository;
    private final ActivityRepository activityRepository;
    private final StravaConnect stravaConnect;
    private final FootballMatchService footballMatchService;
    private final StravaActivityMapper stravaActivityMapper;
    private final UserMapper userMapper;
    private final PlayerMapper playerMapper;
    private final StravaActivityProcessor stravaActivityProcessor;


    public void exchangeCodeForToken(String code, Long userId) {
        stravaConnect.exchangeCodeForToken(code, userId);
    }

    public String connectRedirectUrl(Long userId) {
        return stravaConnect.connectRedirectUrl(userId);
    }

    public void syncActivities(Long athleteId) {
        AthleteEntity athlete = athleteRepository.findById(athleteId).orElseThrow(() -> new RuntimeException("Athlete not found"));
        stravaActivityProcessor.saveActivities(athlete);
    }

    public void syncActivities(AppTeamEntity appTeam) {
        List<AthleteEntity> athleteEntities = athleteRepository.findAllAthletesByAppTeam(appTeam);
        for (AthleteEntity athlete : athleteEntities) {
            log.debug("athlete: {}" , athlete.getId());
            stravaActivityProcessor.saveActivities(athlete);
        }
    }

    public List<AthleteActivities> getListOfAthletesByFootballMatch(AppTeamEntity appTeam, Long footballMatchId) {
        Instant referenceTime = getMatchTime(footballMatchId);
        List<ActivityEntity> activities = getActivitiesAroundTime(appTeam, referenceTime);
        Map<AthleteEntity, List<ActivityEntity>> groupedActivities = groupActivitiesByAthlete(activities);

        return groupedActivities.entrySet().stream()
                .map(entry -> buildAthleteActivities(entry.getKey(), entry.getValue(), appTeam))
                .toList();
    }

    private Instant getMatchTime(Long footballMatchId) {
        FootballMatchDTO match = footballMatchService.getFootballMatchById(footballMatchId);
        return match.getDate().toInstant();
    }

    private List<ActivityEntity> getActivitiesAroundTime(AppTeamEntity appTeam, Instant referenceTime) {
        Date before = Date.from(referenceTime.minus(1, ChronoUnit.HOURS));
        Date after = Date.from(referenceTime.plus(2, ChronoUnit.HOURS));
        return activityRepository.findActivitiesForAppTeamAroundTime(appTeam, before, after);
    }

    private Map<AthleteEntity, List<ActivityEntity>> groupActivitiesByAthlete(List<ActivityEntity> activities) {
        return activities.stream().collect(Collectors.groupingBy(ActivityEntity::getAthlete));
    }

    private AthleteActivities buildAthleteActivities(AthleteEntity athlete, List<ActivityEntity> activities, AppTeamEntity appTeam) {
        List<StravaActivity> dtoActivities = activities.stream()
                .map(stravaActivityMapper::toDTO)
                .toList();
        UserDTO userDTO = userMapper.toDTO(athlete.getUser());
        PlayerDTO playerDTO = athlete.getUser().getTeamRoles().stream()
                .filter(role -> role.getAppTeam().equals(appTeam))
                .map(UserTeamRole::getPlayer)
                .filter(Objects::nonNull)
                .findFirst()
                .map(playerMapper::toDTO)
                .orElse(null);

        return new AthleteActivities(
                athlete.getId(),
                userDTO,
                playerDTO,
                dtoActivities
        );
    }

}