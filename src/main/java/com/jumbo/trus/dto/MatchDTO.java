package com.jumbo.trus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchDTO {

    @NotNull
    @JsonProperty("_id")
    private long id;

    @NotBlank
    private String name;

    @NotNull
    private Long seasonId;

    @NotNull
    private List<@Positive Long> playerIdList;

    @NotNull
    private Date date;

    private boolean home;
}
