package com.jumbo.trus.dto.match;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.entity.PlannedMatchStatus;
import com.jumbo.trus.entity.PlannedMatchType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class PlannedMatchDTO {
    private Long id;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date date;
    private String opponent;
    @NotNull
    private PlannedMatchType type;
    private Boolean home;
    private Long seasonId;
    private Long footballMatchId;
    private String opponentLogoUrl;
    private Integer footballRound;
    private String footballStadium;
    private PlannedMatchStatus status;
    private Long matchId;
    private String description;
    private boolean hasPhoto;
    private int halfMinutes;
    private int breakMinutes;
}
