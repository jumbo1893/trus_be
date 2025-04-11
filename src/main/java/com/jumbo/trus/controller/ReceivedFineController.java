package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.dto.receivedfine.multi.ReceivedFineListDTO;
import com.jumbo.trus.dto.receivedfine.response.ReceivedFineResponse;
import com.jumbo.trus.dto.receivedfine.response.get.setup.ReceivedFineSetupResponse;
import com.jumbo.trus.entity.filter.ReceivedFineFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/fine/received")
@RequiredArgsConstructor
public class ReceivedFineController {

    private final ReceivedFineService receivedFineService;
    private final AppTeamService appTeamService;

    @Secured("ADMIN")
    @PostMapping("/add")
    public ReceivedFineDTO addFine(@RequestBody ReceivedFineDTO receivedFineDTO) {
        return receivedFineService.addFine(receivedFineDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("ADMIN")
    @PostMapping("/player-add")
    public ReceivedFineResponse addFinesToPlayer(@RequestBody ReceivedFineListDTO receivedFineListDTO) {
        return receivedFineService.addFineToPlayer(receivedFineListDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/get-all")
    public List<ReceivedFineDTO> getFines(ReceivedFineFilter receivedFineFilter) {
        receivedFineFilter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return receivedFineService.getAll(receivedFineFilter);
    }

    @RoleRequired("READER")
    @GetMapping("/get-all-detailed")
    public ReceivedFineDetailedResponse getDetailedFines(StatisticsFilter filter) {
        filter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return receivedFineService.getAllDetailed(filter);
    }

    @RoleRequired("ADMIN")
    @PostMapping("/multiple-add")
    public ReceivedFineResponse addMultipleFine(@RequestBody ReceivedFineListDTO receivedFineListDTO) {
        return receivedFineService.addMultipleFines(receivedFineListDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("ADMIN")
    @DeleteMapping("/{fineId}")
    public void deleteMatch(@PathVariable Long fineId) throws NotFoundException {
        receivedFineService.deleteFine(fineId);
    }

    @RoleRequired("READER")
    @GetMapping("/setup")
    public ReceivedFineSetupResponse getReceivedFineSetup(ReceivedFineFilter receivedFineFilter) {
        receivedFineFilter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return receivedFineService.setupPlayers(receivedFineFilter);
    }

    @RoleRequired("READER")
    @GetMapping("/player/setup")
    public List<ReceivedFineDTO> getListOfReceivedFinesForPlayer(@RequestParam Long playerId, @RequestParam Long matchId) {
        return receivedFineService.getAllForSetup(playerId, matchId, appTeamService.getCurrentAppTeamOrThrow());
    }
}
