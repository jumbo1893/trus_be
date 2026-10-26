package com.jumbo.trus.controller.advice;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.service.exceptions.DuplicateFootballMatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class DuplicateFootballMatchExceptionAdvice {
    @ExceptionHandler(DuplicateFootballMatchException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateFootballMatch() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("K tomuto soupeři již vytvořený zápas existuje", "DUPLICATE_FOOTBALL_MATCH"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleUniqueIndexViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains("uq_match_app_team_football_match")) {
                return handleDuplicateFootballMatch();
            }
            cause = cause.getCause();
        }
        throw exception;
    }
}
