package com.jumbo.trus.repository.football;

import com.jumbo.trus.dto.football.Organization;
import com.jumbo.trus.entity.football.LeagueEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeagueRepository extends JpaRepository<LeagueEntity, Long> {

    boolean existsByUri(String uri);

    List<LeagueEntity> findAllByOrganization(Organization organization);

    List<LeagueEntity> findAllByOrganizationAndCurrentLeague(Organization organization, boolean currentLeague);

    LeagueEntity findByUri(String uri);

    @Modifying
    @Transactional
    @Query("UPDATE league l SET l.name = :name, l.rank = :rank, l.year = :year,  l.currentLeague = :currentLeague WHERE l.id = :id")
    int updateLeagueFields(@Param("id") Long id,
                           @Param("name") String name,
                           @Param("rank") int rank,
                           @Param("year") String year,
                           @Param("currentLeague") boolean currentLeague);

}
