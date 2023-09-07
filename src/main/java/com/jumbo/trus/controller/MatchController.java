package com.jumbo.trus.controller;

import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.response.SetupMatchResponse;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/match")
public class MatchController {

    @Autowired
    MatchService matchService;

    @Secured("ROLE_ADMIN")
    @PostMapping("/add")
    public MatchDTO addMatch(@RequestBody MatchDTO matchDTO) {
        return matchService.addMatch(matchDTO);
    }

    @GetMapping("/setup")
    public SetupMatchResponse setupMatch(@RequestParam(required = false) Long matchId) {
        return matchService.setupMatch(matchId);
    }

    @GetMapping("/get-all")
    public List<MatchDTO> getMatches(MatchFilter matchFilter) {
        return matchService.getAll(matchFilter);
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/{matchId}")
    public MatchDTO editMatch(@PathVariable Long matchId, @RequestBody MatchDTO matchDTO) throws NotFoundException {
        return matchService.editMatch(matchId, matchDTO);
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{matchId}")
    public void deleteMatch(@PathVariable Long matchId) throws NotFoundException {
        matchService.deleteMatch(matchId);
    }
}
