package com.jumbo.trus.controller;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.exceptions.NonEditableEntityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@ControllerAdvice
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
    public List<SeasonDTO> getSeasons(SeasonFilter seasonFilter) {
        return seasonService.getAll(seasonFilter);
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
