package com.jumbo.trus.controller;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.service.FineService;
import com.jumbo.trus.service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/fine")
public class FineController {

    @Autowired
    FineService fineService;

    @PostMapping("/add")
    public FineDTO addFine(@RequestBody FineDTO fineDTO) {
        return fineService.addFine(fineDTO);
    }

    @GetMapping("/get-all")
    public List<FineDTO> getFines(@RequestParam(defaultValue = "1000")int limit) {
        return fineService.getAll(limit);
    }

    @PutMapping("/{fineId}")
    public FineDTO editFine(@PathVariable Long fineId, @RequestBody FineDTO fineDTO) throws NotFoundException {
        return fineService.editFine(fineId, fineDTO);
    }

    @DeleteMapping("/{fineId}")
    public void deleteFine(@PathVariable Long fineId) throws NotFoundException {
        fineService.deleteFine(fineId);
    }
}
