package com.jumbo.trus.service.ai;

import com.jumbo.trus.config.AiOpenAiProperties;
import com.jumbo.trus.dto.ai.AiUsageDTO;
import com.jumbo.trus.entity.ai.*;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.repository.ai.*;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.service.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchReportStoreTest {
    private final MatchReportRepository reports = mock(MatchReportRepository.class);
    private final AiQuotaService quota = mock(AiQuotaService.class);
    private final MatchReportGenerationRepository generations = mock(MatchReportGenerationRepository.class);
    private final AppTeamRepository teams = mock(AppTeamRepository.class);
    private final MatchReportStore store = new MatchReportStore(reports, quota, generations, teams, new AiOpenAiProperties());
    @BeforeEach void setup() {
        when(teams.findForReportUpdate(1L)).thenReturn(Optional.of(new AppTeamEntity()));
        when(generations.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }
    private void tier(AiAccessTier tier) {
        var usage = new AiUsageDTO(tier, 0, null, null, true, true, java.time.LocalDate.now());
        when(quota.getUsage(2L)).thenReturn(usage);
    }
    @Test void firstGenerationIsAvailableToAnyMemberAndLocksBeforeChecking() {
        assertNotNull(store.begin(1L, 3L, 2L));
        var order = inOrder(teams, reports, generations);
        order.verify(teams).findForReportUpdate(1L);
        order.verify(generations).findById("1:3");
        order.verify(reports).existsByAppTeamIdAndFootballMatchId(1L, 3L);
        order.verify(generations).save(any());
    }
    @Test void onlyUltraCanRegenerateSharedReport() {
        when(reports.existsByAppTeamIdAndFootballMatchId(1L, 3L)).thenReturn(true);
        for (var tier : new AiAccessTier[]{AiAccessTier.STANDARD, AiAccessTier.PREMIUM}) {
            tier(tier); assertThrows(AuthException.class, () -> store.begin(1L, 3L, 2L));
            assertFalse(store.state(1L, 3L, 2L).canGenerate());
        }
        tier(AiAccessTier.ULTRA);
        assertNotNull(store.begin(1L, 3L, 2L));
        assertTrue(store.state(1L, 3L, 2L).canGenerate());
    }
    @Test void inFlightClaimBlocksAnotherUserEvenBeforeFirstReportExists() {
        var pending = new MatchReportGeneration(); pending.setToken("first"); pending.setExpiresAt(Instant.now().plusSeconds(30));
        when(generations.findById("1:3")).thenReturn(Optional.of(pending));
        assertThrows(AiUnavailableException.class, () -> store.begin(1L, 3L, 9L));
        assertTrue(store.state(1L, 3L, 9L).generating());
        assertFalse(store.state(1L, 3L, 9L).canGenerate());
        verify(generations, never()).save(any());
    }
    @Test void expiredClaimCanBeRecoveredButOldWorkerCannotSaveOrReleaseNewClaim() {
        var pending = new MatchReportGeneration(); pending.setToken("new"); pending.setExpiresAt(Instant.now().minusSeconds(1));
        when(generations.findById("1:3")).thenReturn(Optional.of(pending));
        assertNotNull(store.begin(1L, 3L, 2L));
        var report = new MatchReportEntity(); report.setAppTeamId(1L); report.setFootballMatchId(3L);
        assertThrows(AiUnavailableException.class, () -> store.save(report, 5L, null, "old"));
        store.release(1L, 3L, "old");
        verify(generations, never()).delete(any()); verify(reports, never()).save(any());
    }
    @Test void successfulSaveCompletesQuotaAndReleasesMatchingClaim() {
        var pending = new MatchReportGeneration(); pending.setToken("token");
        when(generations.findById("1:3")).thenReturn(Optional.of(pending));
        var report = new MatchReportEntity(); report.setAppTeamId(1L); report.setFootballMatchId(3L);
        var answer = new OpenAiAnswer("text", "test", 1, 1);
        store.save(report, 5L, answer, "token");
        verify(reports).save(report); verify(quota).complete(5L, answer); verify(generations).delete(pending);
    }
}
