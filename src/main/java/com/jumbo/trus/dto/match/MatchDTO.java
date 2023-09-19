package com.jumbo.trus.dto.match;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    private long id;

    @NotBlank
    private String name;

    @NotNull
    private Long seasonId;

    @NotNull
    private List<@Positive Long> playerIdList;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    @NotNull
    private Date date;

    private boolean home;
}
