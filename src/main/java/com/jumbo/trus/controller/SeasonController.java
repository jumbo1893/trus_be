package com.jumbo.trus.controller;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/season")
public class SeasonController {

    @Autowired
    SeasonService seasonService;

    @PostMapping("/add")
    public SeasonDTO addSeason(@RequestBody SeasonDTO seasonDTO) {
        return seasonService.addSeason(seasonDTO);
    }

    @GetMapping("/get-all")
    public List<SeasonDTO> getSeasons(@RequestParam(defaultValue = "1000")int limit) {
        return seasonService.getAll(limit);
    }

    @PutMapping("/{seasonId}")
    public SeasonDTO editSeason(@PathVariable Long seasonId, @RequestBody SeasonDTO seasonDTO) throws NotFoundException {
        return seasonService.editSeason(seasonId, seasonDTO);
    }

    @DeleteMapping("/{seasonId}")
    public void deleteSeason(@PathVariable Long seasonId) throws NotFoundException {
        seasonService.deleteSeason(seasonId);
    }
}
