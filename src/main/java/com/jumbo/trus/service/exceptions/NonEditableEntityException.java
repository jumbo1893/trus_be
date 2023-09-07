package com.jumbo.trus.service.exceptions;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NonEditableEntityException extends RuntimeException {



    private String message;

    private String code = NOT_EDITABLE;

    public static final String NOT_EDITABLE = "non_editable";

    public NonEditableEntityException(String message) {
        this.message = message;
    }
}
