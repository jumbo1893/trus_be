package com.jumbo.trus.service.auth;

public record TeamMemberPendingEvent(Long appTeamId, Long userTeamRoleId,
                                     Long userId, String userName) {
}
