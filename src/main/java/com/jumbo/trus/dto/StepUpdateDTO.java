package com.jumbo.trus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StepUpdateDTO {

    private long id;

    private int stepNumber;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long userId;

    private Date updateTime;

}
