package com.jumbo.trus.dto.match;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.weather.MatchWeatherDTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Objects;

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

    @Min(0)
    private Integer homeGoalNumber;

    @Min(0)
    private Integer awayGoalNumber;

    private MatchWeatherDTO weather;

    private FootballMatchDTO footballMatch;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchDTO matchDTO = (MatchDTO) o;
        return id == matchDTO.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
