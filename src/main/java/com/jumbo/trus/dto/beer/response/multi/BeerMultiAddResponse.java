package com.jumbo.trus.dto.beer.response.multi;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BeerMultiAddResponse {

    @NotNull
    private int editedPlayersCount = 0;

    @NotNull
    private int addedPlayersCount = 0;

    @NotNull
    private int totalBeersAdded = 0;

    @NotNull
    private int totalLiquorsAdded = 0;

    @NotNull
    private String match = "";

    public void addEditedPlayer() {
        editedPlayersCount++;
    }

    public void addAddedPlayer() {
        addedPlayersCount++;
    }

    public void addBeers(int beers) {
        totalBeersAdded+=beers;
    }

    public void addLiquors(int liquors) {
        totalLiquorsAdded+=liquors;
    }

    public void addBeersLiquorsAndPlayer(int beers, int liquors, boolean addedPlayer) {
        if (addedPlayer) {
            addAddedPlayer();
        }
        else {
            addEditedPlayer();
        }
        addBeers(beers);
        addLiquors(liquors);
    }

}
