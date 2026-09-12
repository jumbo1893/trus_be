package com.jumbo.trus.dto.participation;

import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchParticipationMemberDTO {

    private PlayerDTO player;
    private List<MatchParticipationCommentDTO> comments = new ArrayList<>();
    private boolean playing;
    private PlayerDTO respondedBy;
    private boolean canDelete;

    public MatchParticipationMemberDTO(PlayerDTO player, List<MatchParticipationCommentDTO> comments) {
        this.player = player;
        this.comments = comments;
    }
}
