package com.jumbo.trus.service.football.pkfl.job;

import com.jumbo.trus.service.football.league.LeagueService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.team.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PkflScheduledJob {

    private final LeagueService leagueService;
    private final TeamService teamService;
    private final FootballPlayerService footballPlayerService;
    private final FootballMatchService footballMatchService;

    public void runPkflLeagueJob() {
        leagueService.updatePkflLeagues();
    }

    public void runPkflTeamJob() {
        teamService.updateTeams();
    }

    public void runPkflPlayerJob() {
        footballPlayerService.updatePlayers();
    }

    public void runPkflMatchJob() {
        footballMatchService.updatePkflMatches();
    }

    @Scheduled(
            cron = "${pkfl.jobs.reference-data-cron:0 30 2 * * *}",
            zone = "${pkfl.jobs.zone:Europe/Prague}"
    )
    public void runReferenceDataJob() {
        log.info("Spouštím denní PKFL synchronizaci lig, týmů a soupisek");
        runPkflLeagueJob();
        runPkflTeamJob();
        runPkflPlayerJob();
        log.info("Denní PKFL synchronizace lig, týmů a soupisek dokončena");
    }

    @Scheduled(
            cron = "${pkfl.jobs.matches-weekday-cron:0 0 1,13 * * MON-FRI}",
            zone = "${pkfl.jobs.zone:Europe/Prague}"
    )
    @Scheduled(
            cron = "${pkfl.jobs.matches-weekend-cron:0 0 7-23 * * SAT,SUN}",
            zone = "${pkfl.jobs.zone:Europe/Prague}"
    )
    public void runScheduledMatchJob() {
        log.info("Spouštím PKFL synchronizaci výsledků a statistik zápasů");
        runPkflMatchJob();
        log.info("PKFL synchronizace výsledků a statistik zápasů dokončena");
    }

    public void runFullPkflJob() {
        log.info("Spouštím kompletní PKFL synchronizaci");
        runReferenceDataJob();
        runPkflMatchJob();
        log.info("Kompletní PKFL synchronizace dokončena");
    }
}
