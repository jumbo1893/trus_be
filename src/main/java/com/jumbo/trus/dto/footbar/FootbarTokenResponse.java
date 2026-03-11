package com.jumbo.trus.dto.footbar;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FootbarTokenResponse {

    @JsonAlias("access_token")
    private String accessToken;

    @JsonAlias("refresh_token")
    private String refreshToken;

    @JsonAlias("expires_in")
    private Long expiresIn;

    @JsonAlias("token_type")
    private String tokenType;

    @JsonAlias("user")
    private FootbarUser user;
}