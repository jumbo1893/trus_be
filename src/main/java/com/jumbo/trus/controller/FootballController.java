package com.jumbo.trus.controller;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.detail.FootballMatchDetail;
import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.dto.pkfl.PkflPlayerDTO;
import com.jumbo.trus.dto.pkfl.stats.PkflAllIndividualStats;
import com.jumbo.trus.service.HeaderManager;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.pkfl.PkflMatchService;
import com.jumbo.trus.service.football.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/football")
@RequiredArgsConstructor
public class FootballController {

    private final PkflMatchService pkflService;
    private final FootballMatchService footballMatchService;
    private final HeaderManager headerManager;
    private final TeamService teamService;

    @GetMapping("/next-and-last-match")
    public List<FootballMatchDTO> getNextAndLastMatch() {
        return footballMatchService.getNextAndLastFootballMatch(headerManager.getTeamIdHeader());
    }

    @GetMapping("/fixtures")
    public List<FootballMatchDTO> getTeamFixtures() {
        return footballMatchService.getNextMatches(headerManager.getTeamIdHeader());
    }

    @GetMapping("/table")
    public List<TableTeamDTO> getTable() {
        return teamService.getTable(headerManager.getTeamIdHeader());
    }

    @GetMapping("/detail/{pkflMatchId}")
    public FootballMatchDetail getFootballMatchDetail(@PathVariable Long pkflMatchId) {
        return footballMatchService.getFootballMatchDetail(pkflMatchId);
    }

    @GetMapping("/player-stats")
    public List<PkflAllIndividualStats> getPlayerStats(@RequestParam boolean currentSeason) {
        return pkflService.getPlayerStats(currentSeason);
    }

    @GetMapping("/player-facts")
    public List<StringAndString> getPlayerFacts(@RequestParam long playerId) {
        return pkflService.getFactsForPlayer(playerId);
    }

    @GetMapping("/player/get-all")
    public List<PkflPlayerDTO> getPlayers() {
        return pkflService.getPlayers();
    }

}
