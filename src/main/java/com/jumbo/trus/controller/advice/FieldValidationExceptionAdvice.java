package com.jumbo.trus.controller.advice;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.controller.error.FieldValidationResponse;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class FieldValidationExceptionAdvice {

    @ExceptionHandler({FieldValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<FieldValidationResponse> handleAuthException(FieldValidationException e) {
        FieldValidationResponse fieldValidationResponse = new FieldValidationResponse(e.getMessage(), e.getFields());
        fieldValidationResponse.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(fieldValidationResponse);
    }

}