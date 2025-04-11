package com.jumbo.trus.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class UserDTO {

    private long id;

    @Email
    private String mail;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @NotBlank(message = "Vyplňte uživatelské heslo")
    @NotNull(message = "Vyplňte uživatelské heslo")
    private String password;

    private Boolean admin;

    private List<UserTeamRoleDTO> teamRoles;

}
