package com.jumbo.trus.repository.strava;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.strava.ActivityEntity;
import com.jumbo.trus.entity.strava.AthleteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, Long> {
    List<ActivityEntity> findByAthlete(AthleteEntity athlete);

    @Query("SELECT MAX(a.startTime) FROM strava_activity a WHERE a.athlete = :athlete")
    Instant findLatestActivityTimeByAthlete(@Param("athlete") AthleteEntity athlete);

    @Query("""
    SELECT a FROM strava_activity a
    WHERE a.athlete.user IN (
        SELECT r.user FROM UserTeamRole r WHERE r.appTeam = :appTeam
    )
    AND a.startTime BETWEEN :referenceTimeMinus AND :referenceTimePlus
""")
    List<ActivityEntity> findActivitiesForAppTeamAroundTime(
            @Param("appTeam") AppTeamEntity appTeam,
            @Param("referenceTimeMinus") Date referenceTimeMinus,
            @Param("referenceTimePlus") Date referenceTimePlus
    );
}
