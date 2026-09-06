package com.jumbo.trus.service.ai;

import com.jumbo.trus.dto.ai.MatchReportStateDTO;
import com.jumbo.trus.entity.ai.*;
import com.jumbo.trus.entity.auth.*;
import com.jumbo.trus.repository.ai.MatchReportRepository;
import com.jumbo.trus.service.auth.*;
import com.jumbo.trus.service.exceptions.AiUnavailableException;
import com.jumbo.trus.service.exceptions.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchReportServiceTest {
    private final AppTeamService teams = mock(AppTeamService.class);
    private final AuthService auth = mock(AuthService.class);
    private final MatchReportContextService context = mock(MatchReportContextService.class);
    private final MatchReportRepository reports = mock(MatchReportRepository.class);
    private final MatchReportStore store = mock(MatchReportStore.class);
    private final OpenAiClient ai = mock(OpenAiClient.class);
    private final AiQuotaService quota = mock(AiQuotaService.class);
    private final MatchReportService service = new MatchReportService(teams, auth, context, store, ai, quota);
    private final AppTeamEntity team = new AppTeamEntity();
    @BeforeEach void setup() {
        team.setId(1L); var user = new UserEntity(); user.setId(2L);
        when(teams.getCurrentAppTeamOrThrow()).thenReturn(team);
        when(auth.getCurrentUserEntity()).thenReturn(user);
        when(context.build(3L, team)).thenReturn("{}");
        when(store.begin(1L, 3L, 2L)).thenReturn("token");
        var question = new AiQuestionEntity(); question.setId(4L);
        when(quota.reserve(eq(2L), eq(team), anyString())).thenReturn(AiQuotaDecision.allowed(question, null));
        when(store.save(any(), eq(4L), any(), eq("token"))).thenAnswer(inv -> inv.getArgument(0));
    }
    @Test void alwaysSavesFunnyReportWithoutStyleDisclaimer() {
        when(ai.generateMatchReport(anyString(), eq("{}"))).thenReturn(new OpenAiAnswer("Report", "model", 1, 2));
        var result = service.generate(3L);
        assertEquals(MatchReportStyle.FUN, result.style()); assertEquals("Report", result.text());
        verify(store).save(any(), eq(4L), any(), eq("token"));
    }
    @Test void rejectsRegenerationBeforeQuotaAndModelCall() {
        when(store.begin(1L, 3L, 2L)).thenThrow(new AuthException("ULTRA", AuthException.INSUFFICIENT_RIGHTS));
        assertThrows(AuthException.class, () -> service.generate(3L));
        verify(quota, never()).reserve(any(), any(), any());
        verify(ai, never()).generateMatchReport(anyString(), anyString());
    }
    @Test void failedGenerationReleasesClaimAndKeepsSavedText() {
        var error = new AiUnavailableException("Offline");
        when(ai.generateMatchReport(anyString(), anyString())).thenThrow(error);
        assertSame(error, assertThrows(AiUnavailableException.class, () -> service.generate(3L)));
        verify(quota).fail(4L, error); verify(store).release(1L, 3L, "token");
        verify(store, never()).save(any(), any(), any(), any());
    }
    @Test void quotaDenialReleasesClaimWithoutModelCall() {
        when(quota.reserve(eq(2L), eq(team), anyString())).thenReturn(
                AiQuotaDecision.denied(null, AiQuestionStatus.LIMIT_REACHED, "Limit"));
        assertThrows(AiUnavailableException.class, () -> service.generate(3L));
        verify(store).release(1L, 3L, "token");
        verify(ai, never()).generateMatchReport(anyString(), anyString());
    }
    @Test void returnsServerAuthoritativePermissionAfterCheckingTeamAccess() {
        var state = new MatchReportStateDTO(null, true, false);
        when(store.state(1L, 3L, 2L)).thenReturn(state);
        assertSame(state, service.getReports(3L));
        verify(context).checkAccess(3L, team);
    }
    @Test void hidesOldStyleDisclaimerInPreviouslySavedReports() {
        var report = new MatchReportEntity();
        report.setText("S nadsázkou – fiktivní report inspirovaný statistikami.\n\nReport");
        assertEquals("Report", com.jumbo.trus.dto.ai.MatchReportDTO.from(report).text());
    }
}
