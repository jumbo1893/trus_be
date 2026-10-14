package com.jumbo.trus.service.chat;

import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatAccessService {
    private final UserTeamRoleRepository roles;

    public boolean allowed(Long teamId, Long userId, String kind) {
        if (teamId == null || userId == null || (!"all".equals(kind) && !"players".equals(kind))) return false;
        return roles.findByUserIdAndAppTeamId(userId, teamId)
                .filter(role -> !"players".equals(kind) || role.getPlayer() != null
                        && !role.getPlayer().isFan() && !role.getPlayer().isDeleted())
                .isPresent();
    }
}
