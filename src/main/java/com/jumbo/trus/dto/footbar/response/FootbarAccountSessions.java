package com.jumbo.trus.dto.footbar.response;

import com.jumbo.trus.dto.footbar.FootbarSessionDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FootbarAccountSessions {

    private MatchDTO match;
    private List<PlayerDTO> players;
    private PlayerDTO primaryPlayer;
    private PlayerDTO secondaryPlayer;
    private List<FootbarSessionDTO> sessions;
}