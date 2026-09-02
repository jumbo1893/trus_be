package com.jumbo.trus.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamMemberDTO {

    private long userTeamRoleId;

    private long userId;

    private String userName;

    private String mail;

    private String role;

    private boolean owner;
}
