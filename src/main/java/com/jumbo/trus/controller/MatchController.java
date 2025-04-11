package com.jumbo.trus.controller;

import com.jumbo.trus.aspect.PostCommitTask;
import com.jumbo.trus.aspect.appteam.StoreAppTeam;
import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.response.SetupMatchResponse;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.auth.AppTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final AppTeamService appTeamService;

    @RoleRequired("ADMIN")
    @PostMapping("/add")
    @PostCommitTask
    @StoreAppTeam
    public MatchDTO addMatch(@RequestBody MatchDTO matchDTO) {
        return matchService.addMatch(matchDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/setup")
    public SetupMatchResponse setupMatch(@RequestParam(required = false) Long matchId) {
        return matchService.setupMatch(matchId, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/get-all")
    public List<MatchDTO> getMatches(MatchFilter matchFilter) {
        matchFilter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return matchService.getAll(matchFilter);
    }

    @RoleRequired("ADMIN")
    @PutMapping("/{matchId}")
    @PostCommitTask
    @StoreAppTeam
    public MatchDTO editMatch(@PathVariable Long matchId, @RequestBody MatchDTO matchDTO) throws NotFoundException {
        return matchService.editMatch(matchId, matchDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("ADMIN")
    @DeleteMapping("/{matchId}")
    @PostCommitTask
    @StoreAppTeam
    public void deleteMatch(@PathVariable Long matchId) throws NotFoundException {
        matchService.deleteMatch(matchId);
    }

    @RoleRequired("ADMIN")
    @PostMapping("/update")
    public void pairFootballMatches() throws NotFoundException {
        matchService.pairAllFootballMatches(appTeamService.getCurrentAppTeamOrThrow());
    }
}
