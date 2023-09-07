package com.jumbo.trus.controller.advice;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.service.exceptions.DuplicateEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class DuplicateEmailExceptionAdvice {

    @ExceptionHandler({DuplicateEmailException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException() {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("Zadaný mail již existuje");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

}