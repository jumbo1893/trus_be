package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.dto.goal.multi.GoalListDTO;
import com.jumbo.trus.dto.goal.response.GoalMultiAddResponse;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.goal.response.get.GoalSetupResponse;
import com.jumbo.trus.entity.filter.GoalFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.GoalService;
import com.jumbo.trus.service.auth.AppTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/goal")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final AppTeamService appTeamService;

    @RoleRequired("ADMIN")
    @PostMapping("/add")
    public GoalDTO addGoal(@RequestBody GoalDTO goalDTO) {
        return goalService.addGoal(goalDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/get-all")
    public List<GoalDTO> getGoals(GoalFilter goalFilter) {
        goalFilter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return goalService.getAll(goalFilter);
    }

    @RoleRequired("READER")
    @GetMapping("/setup")
    public List<GoalSetupResponse> getGoalSetup(GoalFilter goalFilter) {
        goalFilter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return goalService.getGoalSetup(goalFilter);
    }

    @RoleRequired("READER")
    @GetMapping("/get-all-detailed")
    public GoalDetailedResponse getDetailedGoals(StatisticsFilter filter) {
        filter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return goalService.getAllDetailed(filter);
    }

    @RoleRequired("ADMIN")
    @PostMapping("/multiple-add")
    public GoalMultiAddResponse addMultipleGoal(@RequestBody GoalListDTO goalListDTO) {
        return goalService.addMultipleGoal(goalListDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("ADMIN")
    @DeleteMapping("/{goalId}")
    public void deleteGoal(@PathVariable Long goalId) throws NotFoundException {
        goalService.deleteGoal(goalId);
    }
}
