package com.jumbo.trus.controller.error;

import com.jumbo.trus.service.helper.ValidationField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldValidationResponse {
    private String message;
    private List<ValidationField> fields;
}
