package com.jumbo.trus.controller;

import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.dto.receivedfine.multi.ReceivedFineListDTO;
import com.jumbo.trus.dto.receivedfine.response.ReceivedFineResponse;
import com.jumbo.trus.dto.receivedfine.response.get.setup.ReceivedFineSetupResponse;
import com.jumbo.trus.entity.filter.ReceivedFineFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.ReceivedFineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/fine/received")
public class ReceivedFineController {

    @Autowired
    ReceivedFineService receivedFineService;

    @PostMapping("/add")
    public ReceivedFineDTO addFine(@RequestBody ReceivedFineDTO receivedFineDTO) {
        return receivedFineService.addFine(receivedFineDTO);
    }

    @PostMapping("/player-add")
    public ReceivedFineResponse addFinesToPlayer(@RequestBody ReceivedFineListDTO receivedFineListDTO) {
        return receivedFineService.addFineToPlayer(receivedFineListDTO);
    }

    @GetMapping("/get-all")
    public List<ReceivedFineDTO> getFines(ReceivedFineFilter receivedFineFilter) {
        return receivedFineService.getAll(receivedFineFilter);
    }

    @GetMapping("/get-all-detailed")
    public ReceivedFineDetailedResponse getDetailedFines(StatisticsFilter filter) {
        return receivedFineService.getAllDetailed(filter);
    }

    @PostMapping("/multiple-add")
    public ReceivedFineResponse addMultipleFine(@RequestBody ReceivedFineListDTO receivedFineListDTO) {
        return receivedFineService.addMultipleFines(receivedFineListDTO);
    }

    @DeleteMapping("/{fineId}")
    public void deleteMatch(@PathVariable Long fineId) throws NotFoundException {
        receivedFineService.deleteFine(fineId);
    }

    @GetMapping("/setup")
    public ReceivedFineSetupResponse getReceivedFineSetup(ReceivedFineFilter receivedFineFilter) {
        return receivedFineService.setupPlayers(receivedFineFilter);
    }

    @GetMapping("/player/setup")
    public List<ReceivedFineDTO> getListOfReceivedFinesForPlayer(@RequestParam Long playerId, @RequestParam Long matchId) {
        return receivedFineService.getAllForSetup(playerId, matchId);
    }
}
