package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.service.achievement.AchievementService;
import com.jumbo.trus.service.achievement.AchievementAwardAuditService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Set;
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
    private final AchievementAwardAuditService awardAuditService;

    @RoleRequired("ADMIN")
    @PostMapping("/audit-awards")
    public AchievementAwardAuditService.Result auditAwards(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "0") long afterId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Set<String> codes) {
        if (afterId < 0 || limit < 1 || limit > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "afterId >= 0, limit 1–50");
        }
        return awardAuditService.audit(appTeamService.getCurrentAppTeamOrThrow(), dryRun,
                afterId, limit, codes == null ? Set.of() : codes);
    }

    @RoleRequired("EDITOR")
    @PostMapping("/{playerId}")
    public void updatePlayer(@PathVariable Long playerId) {
        achievementService.updatePlayerAchievements(playerId, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("EDITOR")
    @GetMapping("/test")
    public void testAllPlayers() {
        achievementService.updateAllPlayerAchievements(appTeamService.getCurrentAppTeamOrThrow(), AchievementType.ALL);
    }

    @RoleRequired("READER")
    @GetMapping("/get-detail")
    public AchievementDetail getDetail(@RequestParam Long playerAchievementId) {
        return achievementService.getAchievementDetail(playerAchievementId, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("EDITOR")
    @PutMapping("/player/{playerAchievementId}")
    public PlayerAchievementDTO editPlayerAchievement(@PathVariable Long playerAchievementId, @RequestBody PlayerAchievementDTO playerAchievementDTO) throws NotFoundException {
        return achievementService.editPlayerAchievement(playerAchievementId, playerAchievementDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @GetMapping("/get-all-detailed")
    public List<AchievementDetail> getAchievementsDetail() {
        return achievementService.getAllDetailedAchievements(appTeamService.getCurrentAppTeamOrThrow().getId());
    }
}
