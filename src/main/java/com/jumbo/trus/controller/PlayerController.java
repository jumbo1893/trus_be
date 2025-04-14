package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.player.PlayerSetup;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.player.PlayerAchievementService;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/player")
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerAchievementService playerAchievementService;
    private final AppTeamService appTeamService;

    @RoleRequired("ADMIN")
    @PostMapping("/add")
    public PlayerDTO addPlayer(@RequestBody PlayerDTO playerDTO) {
        return playerService.addPlayer(playerDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/get-all")
    public List<PlayerDTO> getPlayersAndFans() {
        return playerService.getAll(appTeamService.getCurrentAppTeamOrThrow().getId());
    }

    @RoleRequired("READER")
    @GetMapping("/setup")
    public PlayerSetup setupPlayer(@RequestParam(required = false) Long playerId) {
        return playerAchievementService.setupPlayerWithAchievements(playerId, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/get-players")
    public List<PlayerDTO> getPlayers() {
        return playerService.getAllByFan(false, appTeamService.getCurrentAppTeamOrThrow().getId());
    }

    @RoleRequired("READER")
    @GetMapping("/get-fans")
    public List<PlayerDTO> getFans() {
        return playerService.getAllByFan(true, appTeamService.getCurrentAppTeamOrThrow().getId());
    }

    @RoleRequired("READER")
    @GetMapping("/{playerId}")
    public PlayerDTO getPlayer(@PathVariable Long playerId) {
        return playerService.getPlayer(playerId);
    }

    @RoleRequired("ADMIN")
    @PutMapping("/{playerId}")
    public PlayerDTO editPlayer(@PathVariable Long playerId, @RequestBody PlayerDTO playerDTO) throws NotFoundException {
        return playerService.editPlayer(playerId, playerDTO);
    }

    @RoleRequired("ADMIN")
    @DeleteMapping("/{playerId}")
    public void deletePlayer(@PathVariable Long playerId) throws NotFoundException {
        playerService.deletePlayer(playerId);
    }
}
