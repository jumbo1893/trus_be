package com.jumbo.trus.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamAdministrationDTO {

    private long appTeamId;

    private String teamName;

    private Long ownerId;

    private String ownerName;

    private long currentUserId;

    private String readerCode;

    private String editorCode;

    private List<TeamMemberDTO> members;
}
