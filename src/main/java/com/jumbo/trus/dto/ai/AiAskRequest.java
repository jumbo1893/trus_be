package com.jumbo.trus.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiAskRequest {

    @NotBlank
    @Size(max = 1000)
    private String question;
}
