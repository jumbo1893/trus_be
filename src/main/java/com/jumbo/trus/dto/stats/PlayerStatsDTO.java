package com.jumbo.trus.dto.stats;

import com.jumbo.trus.dto.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerStatsDTO {

    private PlayerDTO player;

    private String text;
}
