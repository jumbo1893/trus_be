package com.jumbo.trus.controller.advice;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.service.exceptions.MembershipGrantException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MembershipGrantExceptionAdvice {

    @ExceptionHandler(MembershipGrantException.class)
    public ResponseEntity<ErrorResponse> handle(MembershipGrantException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getMessage(), "invalid_membership_grant"));
    }
}
