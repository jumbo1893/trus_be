package com.jumbo.trus.dto.receivedfine.response;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedFineResponse {

    private int editedPlayersCount;

    private String player;

    @NotNull
    private int totalFinesAdded = 0;

    @NotNull
    private String match = "";

    public void addEditedPlayer() {
        editedPlayersCount++;
    }

    public void addFine(int number) {
        totalFinesAdded+=number;
    }

    public ReceivedFineResponse(String match) {
        this.match = match;
    }

}
