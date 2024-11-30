package com.jumbo.trus.service.football.team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamUpdateResult {
    private int updatedTeams;
    private int updatedTableTeams;

    public void incrementTeams(int count) {
        this.updatedTeams += count;
    }

    public void incrementTableTeams(int count) {
        this.updatedTableTeams += count;
    }
}
