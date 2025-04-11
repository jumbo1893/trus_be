package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.service.achievement.AchievementService;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.auth.AppTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/achievement")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;
    private final AppTeamService appTeamService;

    @RoleRequired("ADMIN")
    @PostMapping("/{playerId}")
    public void updatePlayer(@PathVariable Long playerId) {
        achievementService.updatePlayerAchievements(playerId, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("ADMIN")
    @GetMapping("/test")
    public void testAllPlayers() {
        achievementService.updateAllPlayerAchievements(appTeamService.getCurrentAppTeamOrThrow(), AchievementType.ALL);
    }

    @RoleRequired("READER")
    @GetMapping("/get-detail")
    public AchievementDetail getDetail(@RequestParam Long playerAchievementId) {
        return achievementService.getAchievementDetail(playerAchievementId, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("ADMIN")
    @PutMapping("/player/{playerAchievementId}")
    public PlayerAchievementDTO editPlayerAchievement(@PathVariable Long playerAchievementId, @RequestBody PlayerAchievementDTO playerAchievementDTO) throws NotFoundException {
        return achievementService.editPlayerAchievement(playerAchievementId, playerAchievementDTO);
    }

    @GetMapping("/get-all-detailed")
    public List<AchievementDetail> getAchievementsDetail() {
        return achievementService.getAllDetailedAchievements(appTeamService.getCurrentAppTeamOrThrow().getId());
    }
}
