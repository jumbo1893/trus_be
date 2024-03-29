package com.jumbo.trus.controller;

import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.dto.goal.multi.GoalListDTO;
import com.jumbo.trus.dto.goal.response.GoalMultiAddResponse;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.goal.response.get.GoalSetupResponse;
import com.jumbo.trus.entity.filter.GoalFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/goal")
public class GoalController {

    @Autowired
    GoalService goalService;

    @Secured("ROLE_ADMIN")
    @PostMapping("/add")
    public GoalDTO addGoal(@RequestBody GoalDTO goalDTO) {
        return goalService.addGoal(goalDTO);
    }

    @GetMapping("/get-all")
    public List<GoalDTO> getGoals(GoalFilter goalFilter) {
        return goalService.getAll(goalFilter);
    }

    @GetMapping("/setup")
    public List<GoalSetupResponse> getGoalSetup(GoalFilter goalFilter) {
        return goalService.getGoalSetup(goalFilter);
    }

    @GetMapping("/get-all-detailed")
    public GoalDetailedResponse getDetailedGoals(StatisticsFilter filter) {
        return goalService.getAllDetailed(filter);
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/multiple-add")
    public GoalMultiAddResponse addMultipleGoal(@RequestBody GoalListDTO goalListDTO) {
        return goalService.addMultipleGoal(goalListDTO);
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{goalId}")
    public void deleteGoal(@PathVariable Long goalId) throws NotFoundException {
        goalService.deleteGoal(goalId);
    }
}
