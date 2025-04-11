package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.auth.AppTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@ControllerAdvice
@RestController
@RequestMapping("/season")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;
    private final AppTeamService appTeamService;

    @RoleRequired("ADMIN")
    @PostMapping("/add")
    public SeasonDTO addSeason(@RequestBody SeasonDTO seasonDTO) {
        return seasonService.addSeason(seasonDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/get-all")
    public List<SeasonDTO> getSeasons(SeasonFilter seasonFilter) {
        seasonFilter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return seasonService.getAll(seasonFilter);
    }

    @RoleRequired("ADMIN")
    @PutMapping("/{seasonId}")
    public SeasonDTO editSeason(@PathVariable Long seasonId, @RequestBody SeasonDTO seasonDTO) throws NotFoundException {
        return seasonService.editSeason(seasonId, seasonDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("ADMIN")
    @DeleteMapping("/{seasonId}")
    public void deleteSeason(@PathVariable Long seasonId) throws NotFoundException {
        seasonService.deleteSeason(seasonId);
    }
}
