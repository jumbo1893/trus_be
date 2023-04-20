package com.jumbo.trus.entity.filter;


import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class MatchFilter {


    private String name;

    private Date date;

    private List<Long> playerList;

    private Long seasonId;

    private boolean home;

    //defaultní hodnota
    private int limit = 1000;
}
