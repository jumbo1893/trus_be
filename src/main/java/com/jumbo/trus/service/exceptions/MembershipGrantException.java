package com.jumbo.trus.service.exceptions;

public class MembershipGrantException extends RuntimeException {
    public MembershipGrantException(String message) {
        super(message);
    }

    public MembershipGrantException(String message, Throwable cause) {
        super(message, cause);
    }
}
