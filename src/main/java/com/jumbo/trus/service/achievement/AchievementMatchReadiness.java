package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.entity.outbox.*;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AchievementMatchReadiness {
    private final MatchRepository matches;
    private final OutboxEventRepository events;
    private Clock clock = Clock.systemUTC();
    private static final Set<String> FINAL_RESULT_CODES = Set.of(
            "KAZDEMU_CO_MU_PATRI", "AUTICKO", "CERNA_PRACE", "USPESNY_DEN", "DOPING",
            "PO_PORADNE_PRACI_PORADNA_OSLAVA", "ZBYTECNE_PRASE",
            "JA_TO_ZA_VAS_OBEHAL", "KORALA", "OSLAVENEC", "TAHOUN",
            "OSAMELY_DRZAK", "VE_DVOU_SE_TO_LEPE_TAHNE", "KLUB_SRACU");

    /** Persist a wake-up alongside the current calculation so no further request is needed. */
    @Transactional
    public boolean deferIfPending(String code, PlayerAchievementDTO result, Long teamId) {
        if (result.getSeasonId() == null && !FINAL_RESULT_CODES.contains(code)) return false;
        Long matchId = result.getSeasonId() == null && result.getMatch() != null ? result.getMatch().getId() : null;
        if (matchId == null && result.getSeasonId() == null) return false;
        Instant now = clock.instant();
        var pending = matches.findAwaitingFinalStatistics(teamId, matchId, result.getSeasonId(),
                Date.from(now.minusSeconds(3600)), Date.from(now));
        for (var match : pending) {
            if (events.existsByAppTeamIdAndAggregateIdAndEventTypeAndStatusIn(teamId, match.getId(),
                    OutboxEventType.MATCH_ACHIEVEMENTS_READY, List.of(OutboxEventStatus.NEW, OutboxEventStatus.RETRY))) continue;
            var event = new OutboxEventEntity();
            event.setAppTeamId(teamId);
            event.setAggregateId(match.getId());
            event.setEventType(OutboxEventType.MATCH_ACHIEVEMENTS_READY);
            event.setAggregateType(OutboxAggregateType.ALL);
            event.setPayload(new OutboxEventPayload(match.getId(), result.getSeasonId(), null, null, null, null));
            event.setNextAttemptAt(match.getDate().toInstant().plusSeconds(3600));
            events.save(event);
        }
        return !pending.isEmpty();
    }
}
