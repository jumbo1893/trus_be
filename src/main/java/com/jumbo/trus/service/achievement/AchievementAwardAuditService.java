package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AchievementAwardAuditService {
    private final PlayerAchievementRepository repository;
    private final PlayerAchievementMapper mapper;
    private final AchievementCalculator calculator;
    private final MatchRepository matches;

    public record Entry(long playerAchievementId, long playerId, String playerName, String code, String outcome,
                        Long previousSeasonId, Long seasonId, Long previousMatchId, Long matchId) { }
    public record Result(boolean dryRun, long nextAfterId, boolean hasMore, List<Entry> entries) { }

    @Transactional
    public Result audit(AppTeamEntity team, boolean dryRun, long afterId, int limit, Set<String> codes) {
        List<PlayerAchievementEntity> rows = repository.findAwardAuditBatch(team.getId(), afterId,
                PageRequest.of(0, limit + 1));
        boolean hasMore = rows.size() > limit;
        List<Entry> entries = new ArrayList<>();
        List<Long> matchIds = null;
        long cursor = afterId;
        for (PlayerAchievementEntity row : rows.subList(0, Math.min(limit, rows.size()))) {
            cursor = row.getId();
            String code = row.getAchievement().getCode();
            if (!codes.isEmpty() && !codes.contains(code)) continue;
            if (matchIds == null) matchIds = matches.findCompletedMatchIds(team.getId(), new Date());
            PlayerAchievementDTO calculated = calculator.auditAward(mapper.toDTO(row), team, matchIds);
            Long oldMatch = row.getMatch() == null ? null : row.getMatch().getId();
            Long oldSeason = row.getSeasonId();
            String outcome = "SKIPPED";
            Long newMatch = oldMatch;
            Long newSeason = oldSeason;
            if (calculated != null && calculated.getAccomplished() != null) {
                if (Boolean.TRUE.equals(calculated.getAccomplished())) {
                    newMatch = calculated.getMatch() == null ? null : calculated.getMatch().getId();
                    newSeason = calculated.getSeasonId();
                    outcome = "VALID";
                    Long oldFootball = row.getFootballMatch() == null ? null : row.getFootballMatch().getId();
                    Long newFootball = calculated.getFootballMatch() == null ? null : calculated.getFootballMatch().getId();
                    if (!Objects.equals(oldMatch, newMatch) || !Objects.equals(oldSeason, newSeason)
                            || !Objects.equals(oldFootball, newFootball) || !Objects.equals(row.getDetail(), calculated.getDetail())) {
                        outcome = "REASSIGNED";
                        if (!dryRun) {
                            PlayerAchievementEntity replacement = mapper.toEntity(calculated);
                            row.setMatch(replacement.getMatch());
                            row.setFootballMatch(replacement.getFootballMatch());
                            row.setSeasonId(newSeason);
                            row.setDetail(calculated.getDetail());
                            // Keep the original award timestamp; auditing is not a new award.
                        }
                    }
                } else {
                    outcome = "REVOKED";
                    newMatch = null;
                    newSeason = null;
                    if (!dryRun) {
                        row.setAccomplished(false);
                        row.setAccomplishedDate(null);
                        row.setDetail(null);
                        row.setMatch(null);
                        row.setFootballMatch(null);
                        row.setSeasonId(null);
                    }
                }
            }
            entries.add(new Entry(row.getId(), row.getPlayer().getId(), row.getPlayer().getName(), code, outcome,
                    oldSeason, newSeason, oldMatch, newMatch));
        }
        return new Result(dryRun, cursor, hasMore, entries);
    }
}
