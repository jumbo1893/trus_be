package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.detail.FootballMatchDetail;
import com.jumbo.trus.dto.football.detail.FootballTableTeamDetail;
import com.jumbo.trus.dto.football.stats.FootballAllIndividualStats;
import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.stats.FootballPlayerFact;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.football.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/football")
@RequiredArgsConstructor
public class FootballController {

    private final FootballMatchService footballMatchService;
    private final TeamService teamService;
    private final FootballPlayerStatsService footballPlayerStatsService;
    private final FootballPlayerFact footballPlayerFact;
    private final FootballPlayerService footballPlayerService;
    private final AppTeamService appTeamService;

    @RoleRequired("READER")
    @GetMapping("/next-and-last-match")
    public List<FootballMatchDetail> getNextAndLastMatch() {
        return footballMatchService.getNextAndLastFootballMatchDetail(appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/fixtures")
    public List<FootballMatchDTO> getTeamFixtures() {
        return footballMatchService.getNextMatches(appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/table")
    public List<TableTeamDTO> getTable() {
        Long teamId = appTeamService.getCurrentAppTeamOrThrow().getTeam().getId();
        return teamService.getTable(teamId);
    }

    @RoleRequired("READER")
    @GetMapping("/detail/{footballMatchId}")
    public FootballMatchDetail getFootballMatchDetail(@PathVariable Long footballMatchId) {
        return footballMatchService.getFootballMatchDetail(footballMatchId, appTeamService.getCurrentAppTeamOrThrow(), true);
    }

    @RoleRequired("READER")
    @GetMapping("/team-detail/{tableTeamId}")
    public FootballTableTeamDetail getFootballTeamDetail(@PathVariable Long tableTeamId) {
        return teamService.getFootballTeamDetail(tableTeamId, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/player-stats")
    public List<FootballAllIndividualStats> getPlayerStats(@RequestParam boolean currentSeason) {
        return footballPlayerStatsService.getPlayerStatsForTeam(currentSeason, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/player-facts")
    public List<StringAndString> getPlayerFacts(@RequestParam long playerId) {
        return footballPlayerFact.getFactsForPlayer(playerId, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("NONE")
    @GetMapping("/player/get-all")
    public List<FootballPlayerDTO> getPlayers() {
        return footballPlayerService.getAllPlayersByCurrentTeam(appTeamService.getCurrentAppTeamOrThrow());
    }

}
