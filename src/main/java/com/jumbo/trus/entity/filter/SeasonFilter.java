package com.jumbo.trus.entity.filter;


import com.jumbo.trus.entity.auth.AppTeamEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SeasonFilter {


    private boolean allSeason = false;

    private boolean otherSeason = false;

    private boolean automaticSeason = false;

    private AppTeamEntity appTeam;

    //defaultní hodnota
    private int limit = 1000;

    public SeasonFilter(boolean allSeason, boolean otherSeason, boolean automaticSeason) {
        this.allSeason = allSeason;
        this.otherSeason = otherSeason;
        this.automaticSeason = automaticSeason;
    }
}
