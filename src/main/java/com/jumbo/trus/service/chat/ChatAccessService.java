package com.jumbo.trus.service.chat;

import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.PlannedMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatAccessService {
    private final UserTeamRoleRepository roles;
    private final MatchRepository matches;
    private final PlannedMatchRepository plannedMatches;

    public boolean allowed(Long teamId, Long userId, String kind) {
        if (teamId == null || userId == null) return false;
        if (!"all".equals(kind) && !"players".equals(kind)) {
            try {
                if (kind.startsWith("match-")) {
                    if (matches.findByIdAndAppTeamId(Long.parseLong(kind.substring(6)), teamId).isEmpty()) return false;
                } else if (kind.startsWith("planned-")) {
                    if (plannedMatches.findByIdAndAppTeamId(Long.parseLong(kind.substring(8)), teamId).isEmpty()) return false;
                } else return false;
            } catch (NumberFormatException e) { return false; }
        }
        return roles.findByUserIdAndAppTeamId(userId, teamId)
                .filter(role -> !"players".equals(kind) || role.getPlayer() != null
                        && !role.getPlayer().isFan() && !role.getPlayer().isDeleted())
                .isPresent();
    }
}
