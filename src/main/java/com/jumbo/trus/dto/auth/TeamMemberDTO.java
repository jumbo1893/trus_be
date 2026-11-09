package com.jumbo.trus.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

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

    private Instant lastLoginAt;

    private boolean hidden;

    private boolean roleApproved;
}
