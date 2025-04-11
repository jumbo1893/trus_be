package com.jumbo.trus.service.order;

import com.jumbo.trus.dto.player.PlayerDTO;

import java.util.Comparator;

public class OrderPlayerByName implements Comparator<PlayerDTO> {



    public int compare(PlayerDTO o1, PlayerDTO o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
    }
}
