package com.jumbo.trus.controller;

import com.jumbo.trus.dto.MatchDTO;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/match")
public class MatchController {

    @Autowired
    MatchService matchService;

    @PostMapping("/add")
    public MatchDTO addMatch(@RequestBody MatchDTO matchDTO) {
        return matchService.addMatch(matchDTO);
    }

    @GetMapping("/get-all")
    public List<MatchDTO> getMatches(MatchFilter matchFilter) {
        return matchService.getAll(matchFilter);
    }

    @PutMapping("/{matchId}")
    public MatchDTO editMatch(@PathVariable Long matchId, @RequestBody MatchDTO matchDTO) throws NotFoundException {
        return matchService.editMatch(matchId, matchDTO);
    }

    @DeleteMapping("/{matchId}")
    public void deleteMatch(@PathVariable Long matchId) throws NotFoundException {
        matchService.deleteMatch(matchId);
    }
}
