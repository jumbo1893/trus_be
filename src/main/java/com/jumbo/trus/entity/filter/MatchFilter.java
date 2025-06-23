package com.jumbo.trus.entity.filter;


import com.jumbo.trus.entity.auth.AppTeamEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class MatchFilter {


    private String name;

    private Date date;

    private List<Long> playerList;

    private Long seasonId;

    private boolean home;

    private AppTeamEntity appTeam;

    //defaultní hodnota
    private int limit = 1000;

    public MatchFilter(AppTeamEntity appTeam) {
        this.appTeam = appTeam;
    }
}
