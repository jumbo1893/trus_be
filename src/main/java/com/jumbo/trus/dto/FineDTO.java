package com.jumbo.trus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FineDTO {

    @JsonProperty("_id")
    private long id;

    private String name;

    private int amount;

}
