package com.jumbo.trus.dto.participation;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.participation.MatchParticipationCommentReactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchParticipationCommentDTO {

    private Long id;
    private PlayerDTO author;
    private String text;
    private Instant createdAt;
    private int upVotes;
    private int downVotes;
    private MatchParticipationCommentReactionType currentUserReaction;
    private List<MatchParticipationCommentDTO> replies = new ArrayList<>();
}
