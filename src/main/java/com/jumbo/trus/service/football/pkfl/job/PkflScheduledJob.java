package com.jumbo.trus.service.football.pkfl.job;

import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.league.LeagueService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.team.TeamService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PkflScheduledJob {

    @Autowired
    private LeagueService leagueService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private FootballPlayerService footballPlayerService;

    @Autowired
    private FootballMatchService footballMatchService;

    /*@Scheduled(cron = "0 * * * * *")
    public void runPkflLeagueJob() {
        leagueService.updatePkflLeagues();
    }

    @Scheduled(cron = "5 * * * * *")
    public void runPkflTeamJob() {
        teamService.updateTeams();
    }

    @Scheduled(cron = "10 * * * * *")
    public void runPkflPlayerJob() {
        footballPlayerService.updatePlayers();
    }*/

    /*@Scheduled(cron = "10 52 15 * * *")
    public void runPkflMatchJob() {
        footballMatchService.updatePkflMatches();
    }*/
}