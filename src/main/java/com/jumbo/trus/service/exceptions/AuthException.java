package com.jumbo.trus.service.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthException extends RuntimeException {

    private String message;

    private String code;

    public static final String NOT_LOGGED_IN = "not_logged_in";

    public static final String INSUFFICIENT_RIGHTS = "insufficient_rights";

    public static final String MISSING_TEAM_ID = "missing_app_team_id";

}
