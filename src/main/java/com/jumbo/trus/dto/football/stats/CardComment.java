package com.jumbo.trus.dto.football.stats;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CardComment {

    @NotNull
    private FootballMatchDTO footballMatch;

    private String comment;
}
