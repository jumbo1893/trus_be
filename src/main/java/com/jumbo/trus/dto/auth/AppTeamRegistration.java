package com.jumbo.trus.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppTeamRegistration {

    private String name;

    private Long footballTeamId;

}
