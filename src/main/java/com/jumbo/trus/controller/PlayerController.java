package com.jumbo.trus.controller;

import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/player")
public class PlayerController {

    @Autowired
    PlayerService playerService;

    @Secured("ROLE_ADMIN")
    @PostMapping("/add")
    public PlayerDTO addPlayer(@RequestBody PlayerDTO playerDTO) {
        return playerService.addPlayer(playerDTO);
    }

    @GetMapping("/get-all")
    public List<PlayerDTO> getPlayersAndFans(@RequestParam(defaultValue = "1000")int limit) {
        return playerService.getAll(limit);
    }

    @GetMapping("/get-players")
    public List<PlayerDTO> getPlayers() {
        return playerService.getAllByFan(false);
    }

    @GetMapping("/get-fans")
    public List<PlayerDTO> getFans() {
        return playerService.getAllByFan(true);
    }

    @GetMapping("/{playerId}")
    public PlayerDTO getPlayer(@PathVariable Long playerId) {
        return playerService.getPlayer(playerId);
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/{playerId}")
    public PlayerDTO editPlayer(@PathVariable Long playerId, @RequestBody PlayerDTO playerDTO) throws NotFoundException {
        return playerService.editPlayer(playerId, playerDTO);
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{playerId}")
    public void deletePlayer(@PathVariable Long playerId) throws NotFoundException {
        playerService.deletePlayer(playerId);
    }
}
