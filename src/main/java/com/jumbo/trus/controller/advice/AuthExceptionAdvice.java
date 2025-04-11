package com.jumbo.trus.controller.advice;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.service.exceptions.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AuthExceptionAdvice {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case AuthException.NOT_LOGGED_IN -> HttpStatus.UNAUTHORIZED;
            case AuthException.MISSING_TEAM_ID, AuthException.INSUFFICIENT_RIGHTS -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };

        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), ex.getCode());
        return ResponseEntity.status(status).body(errorResponse);
    }

}