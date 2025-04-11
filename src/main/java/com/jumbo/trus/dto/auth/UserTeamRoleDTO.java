package com.jumbo.trus.dto.auth;

import com.jumbo.trus.dto.player.PlayerDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTeamRoleDTO {

    private long id;

    private Long userId;

    private AppTeamDTO appTeam;

    private String role;

    private PlayerDTO player;

    public UserTeamRoleDTO(long id, Long userId, AppTeamDTO appTeam, String role) {
        this.id = id;
        this.userId = userId;
        this.appTeam = appTeam;
        this.role = role;
    }
}
