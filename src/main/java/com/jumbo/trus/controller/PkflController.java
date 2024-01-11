package com.jumbo.trus.controller;

import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.dto.pkfl.PkflMatchDTO;
import com.jumbo.trus.dto.pkfl.PkflMatchDetail;
import com.jumbo.trus.dto.pkfl.PkflPlayerDTO;
import com.jumbo.trus.dto.pkfl.PkflTableTeamDTO;
import com.jumbo.trus.dto.pkfl.stats.PkflAllIndividualStats;
import com.jumbo.trus.service.pkfl.PkflMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pkfl")
public class PkflController {

    @Autowired
    PkflMatchService pkflService;

    @GetMapping("/next-and-last-match")
    public List<PkflMatchDTO> getNextAndLastMatch() {
        return pkflService.getNextAndLastMatchInPkfl();
    }

    @GetMapping("/fixtures")
    public List<PkflMatchDTO> getPkflFixtures() {
        return pkflService.getPkflFixtures();
    }

    @GetMapping("/table")
    public List<PkflTableTeamDTO> getPkflTable() {
        return pkflService.getTableTeams();
    }

    @GetMapping("/detail/{pkflMatchId}")
    public PkflMatchDetail getPkflMatchDetail(@PathVariable Long pkflMatchId) {
        return pkflService.getPkflMatchDetail(pkflMatchId);
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
