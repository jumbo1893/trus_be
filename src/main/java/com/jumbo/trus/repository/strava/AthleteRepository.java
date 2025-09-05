package com.jumbo.trus.repository.strava;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.strava.AthleteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AthleteRepository extends JpaRepository<AthleteEntity, Long> {
    Optional<AthleteEntity> findByStravaAthleteId(String stravaAthleteId);

    @Query("""
    SELECT a FROM strava_athlete a
    WHERE a.user IN (
        SELECT r.user FROM UserTeamRole r WHERE r.appTeam = :appTeam
    )
""")
    List<AthleteEntity> findAllAthletesByAppTeam(@Param("appTeam") AppTeamEntity appTeam);
}
