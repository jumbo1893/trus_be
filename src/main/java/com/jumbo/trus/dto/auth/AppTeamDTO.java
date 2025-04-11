package com.jumbo.trus.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.auth.UserTeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppTeamDTO {

    private long id;

    private String name;

    private Long ownerId;

    private TeamDTO team;

}
