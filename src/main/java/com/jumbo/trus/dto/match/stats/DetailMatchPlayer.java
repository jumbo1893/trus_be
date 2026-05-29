package com.jumbo.trus.dto.match.stats;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetailMatchPlayer {

    private int playersCount = 0;

    private int fanCount = 0;

    @NotNull
    private List<String> players;

    @NotNull
    private List<String> fans;
}
