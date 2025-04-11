package com.jumbo.trus.controller;

import com.jumbo.trus.aspect.PostCommitTask;
import com.jumbo.trus.aspect.appteam.StoreAppTeam;
import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.exceptions.NonEditableEntityException;
import com.jumbo.trus.service.fine.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/fine")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;
    private final AppTeamService appTeamService;

    @RoleRequired("ADMIN")
    @PostMapping("/add")
    @PostCommitTask
    @StoreAppTeam
    public FineDTO addFine(@RequestBody FineDTO fineDTO) {
        return fineService.addFine(fineDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/get-all")
    public List<FineDTO> getFines(@RequestParam(defaultValue = "1000")int limit) {
        return fineService.getAll(limit, appTeamService.getCurrentAppTeamOrThrow().getId());
    }

    @RoleRequired("ADMIN")
    @PutMapping("/{fineId}")
    @PostCommitTask
    @StoreAppTeam
    public FineDTO editFine(@PathVariable Long fineId, @RequestBody FineDTO fineDTO) throws NotFoundException {
        return fineService.editFine(fineId, fineDTO);
    }

    @RoleRequired("ADMIN")
    @DeleteMapping("/{fineId}")
    @PostCommitTask
    @StoreAppTeam
    public void deleteFine(@PathVariable Long fineId) throws NotFoundException {
        fineService.deleteFine(fineId);
    }

    @ExceptionHandler({NonEditableEntityException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<ErrorResponse> handleServletException(NonEditableEntityException e) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(e.getMessage());
        errorResponse.setCode(e.getCode());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }
}
