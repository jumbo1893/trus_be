package com.jumbo.trus.dto.achievement;

import com.jumbo.trus.dto.football.TeamDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AchievementDTO {

    private long id;

    private String name;

    private String code;

    private String description;

    private boolean onlyForPlayers;

    private String secondaryCondition;

    private boolean manually;
}
