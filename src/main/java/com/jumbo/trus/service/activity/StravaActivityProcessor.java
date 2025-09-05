package com.jumbo.trus.service.activity;

import com.jumbo.trus.dto.strava.StravaActivity;
import com.jumbo.trus.repository.strava.ActivityRepository;
import com.jumbo.trus.entity.strava.ActivityEntity;
import com.jumbo.trus.entity.strava.AthleteEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StravaActivityProcessor {

    private final ActivityRepository activityRepository;
    private final RestTemplate restTemplate;
    private final StravaConnect stravaConnect;

    public List<StravaActivity> fetchActivities(String accessToken, Instant after) {
        List<StravaActivity> allActivities = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        while (true) {
            String url = String.format(
                    "https://www.strava.com/api/v3/athlete/activities?after=%d&per_page=%d&page=%d",
                    after.getEpochSecond(),
                    perPage,
                    page
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<StravaActivity[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    StravaActivity[].class
            );

            StravaActivity[] activities = response.getBody();
            if (activities == null || activities.length == 0) {
                break;
            }

            allActivities.addAll(Arrays.asList(activities));
            page++;
        }

        return allActivities;
    }

    public void saveActivities(AthleteEntity athlete) {
        Instant after = activityRepository.findLatestActivityTimeByAthlete(athlete);
        if (after == null) {
            after = Instant.EPOCH;
        }

        List<StravaActivity> activities = fetchActivities(stravaConnect.getValidAccessToken(athlete), after);
        for (StravaActivity activity : activities) {
            activity.setStravaActivityId(activity.getId().toString());

            boolean alreadyExists = activityRepository.findByAthlete(athlete).stream()
                    .anyMatch(a -> a.getStravaActivityId() != null &&
                            a.getStravaActivityId().equals(activity.getStravaActivityId()));

            if (!alreadyExists) {
                ActivityEntity newActivity = getActivityEntity(athlete, activity);
                activityRepository.save(newActivity);
            }
        }
    }

    private ActivityEntity getActivityEntity(AthleteEntity athlete, StravaActivity activity) {
        ActivityEntity newActivity = new ActivityEntity();
        newActivity.setAthlete(athlete);
        newActivity.setStravaActivityId(activity.getStravaActivityId());
        newActivity.setName(activity.getName());
        newActivity.setDistanceKm(activity.getDistance() / 1000);
        newActivity.setDurationSeconds(activity.getElapsedTime());
        newActivity.setStartTime(convertStringToDate(activity.getStartDate()));
        newActivity.setEndTime(convertStringToDate(activity.getEndDate()));
        newActivity.setType(activity.getType());
        newActivity.setAverageSpeed(activity.getAverageSpeed());
        newActivity.setMaxSpeed(activity.getMaxSpeed());
        newActivity.setCalories(activity.getCalories());
        newActivity.setMovingTimeSeconds(activity.getMovingTime());
        newActivity.setAverageHeartRate(activity.getAverageHeartRate());
        newActivity.setMaxHeartRate(activity.getMaxHeartRate());
        return newActivity;
    }

    private Date convertStringToDate(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return null;
        }
        Instant instant = Instant.parse(dateTime);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Europe/Prague"));
        return Date.from(zonedDateTime.toInstant());
    }
}