package com.jumbo.trus.service.football.pkfl.job;

import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.league.LeagueService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.team.TeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PkflScheduledJob {

    @Autowired
    private LeagueService leagueService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private FootballPlayerService footballPlayerService;

    @Autowired
    private FootballMatchService footballMatchService;

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

    @Scheduled(cron = "0 0 1,13 * * MON-FRI")  // každý pracovní den ve 01:00 a 13:00
    @Scheduled(cron = "0 0 * * * SAT,SUN")     // každý víkend každou celou hodinu
    public void runFullPkflJob() {
        log.debug("Spuštění plánovaného PKFL jobu");
        runPkflLeagueJob();             // 1. Leagues
        runPkflTeamJob();                     // 2. Teams
        runPkflPlayerJob();         // 3. Players
        runPkflMatchJob();      // 4. Matches
        log.debug("PKFL job dokončen");
    }
}