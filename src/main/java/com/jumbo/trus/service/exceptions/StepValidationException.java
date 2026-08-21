package com.jumbo.trus.service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StepValidationException extends RuntimeException {
    public StepValidationException(String message) {
        super(message);
    }
}
