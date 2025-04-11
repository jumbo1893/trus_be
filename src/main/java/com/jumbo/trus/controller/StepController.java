package com.jumbo.trus.controller;

import com.jumbo.trus.dto.StepDTO;
import com.jumbo.trus.dto.StepUpdateDTO;
import com.jumbo.trus.entity.filter.StepFilter;
import com.jumbo.trus.service.StepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/step")
public class StepController {

    @Autowired
    StepService stepService;


    @GetMapping("/get-all")
    public List<StepUpdateDTO> getAllStepUpdates(StepFilter stepFilter) {
        return stepService.getAllStepUpdates(stepFilter);
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/add")
    public StepDTO addStep(@RequestBody StepDTO stepDTO) {
        return stepService.addStepUpdate(stepDTO);
    }

}
