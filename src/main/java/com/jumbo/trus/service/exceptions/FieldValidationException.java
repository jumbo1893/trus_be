package com.jumbo.trus.service.exceptions;

import com.jumbo.trus.service.helper.ValidationField;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class FieldValidationException extends RuntimeException {


    private String message;
    private List<ValidationField> fields;
    public FieldValidationException(String message, List<ValidationField> fields) {
        this.message = message;
        this.fields = fields;
    }
}
