package com.jumbo.trus.controller;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.service.FineService;
import com.jumbo.trus.service.exceptions.NonEditableEntityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/fine")
public class FineController {

    @Autowired
    FineService fineService;

    @Secured("ROLE_ADMIN")
    @PostMapping("/add")
    public FineDTO addFine(@RequestBody FineDTO fineDTO) {
        return fineService.addFine(fineDTO);
    }

    @GetMapping("/get-all")
    public List<FineDTO> getFines(@RequestParam(defaultValue = "1000")int limit) {
        return fineService.getAll(limit);
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/{fineId}")
    public FineDTO editFine(@PathVariable Long fineId, @RequestBody FineDTO fineDTO) throws NotFoundException {
        return fineService.editFine(fineId, fineDTO);
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{fineId}")
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
