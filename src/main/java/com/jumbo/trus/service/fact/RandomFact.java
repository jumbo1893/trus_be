package com.jumbo.trus.service.fact;

import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class RandomFact {
    /**
     * @return Vrátí text s hráčem nebo seznam hráčů, kteří vypili za celou historii (všechny zápasy v db) nejvíce piv
     */
    public String getPlayerWithMostBeers(BeerDetailedDTO beer) {
        if (beer == null ) {
            return "Nelze najít největšího pijana, protože si ještě nikdo nedal pivo???!!";
        }
        return "Nejvíce velkých piv za historii si dal " + beer.getPlayer().getName() + ", který vypil " + beer.getBeerNumber() + " piv.";
    }


}
