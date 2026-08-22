package com.jumbo.trus.service.ai;

import com.jumbo.trus.dto.ai.AiAskRequest;
import com.jumbo.trus.dto.ai.AiQuestionResponse;
import com.jumbo.trus.dto.ai.AiUsageDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ai.AiAccessTier;
import com.jumbo.trus.entity.ai.AiQuestionEntity;
import com.jumbo.trus.entity.ai.AiQuestionStatus;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.achievement.TrusBotAchievementService;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiQuestionServiceTest {

    private final AuthService authService = mock(AuthService.class);
    private final AppTeamService appTeamService = mock(AppTeamService.class);
    private final UserTeamRoleRepository userTeamRoleRepository = mock(UserTeamRoleRepository.class);
    private final AiQuotaService quotaService = mock(AiQuotaService.class);
    private final OpenAiClient openAiClient = mock(OpenAiClient.class);
    private final TrusBotAchievementService achievementService = mock(TrusBotAchievementService.class);
    private final AiQuestionService service = new AiQuestionService(
            authService,
            appTeamService,
            userTeamRoleRepository,
            quotaService,
            openAiClient,
            achievementService
    );

    private final UserEntity user = new UserEntity();
    private final AppTeamEntity appTeam = new AppTeamEntity();
    private final PlayerEntity player = new PlayerEntity();
    private final AiUsageDTO usage = new AiUsageDTO(
            AiAccessTier.STANDARD,
            1,
            2,
            1,
            false,
            true,
            LocalDate.of(2026, 8, 22)
    );

    @BeforeEach
    void setUp() {
        user.setId(5L);
        appTeam.setId(6L);
        player.setId(7L);

        UserTeamRole role = new UserTeamRole();
        role.setUser(user);
        role.setAppTeam(appTeam);
        role.setPlayer(player);

        when(authService.getCurrentUserEntity()).thenReturn(user);
        when(appTeamService.getCurrentAppTeamOrThrow()).thenReturn(appTeam);
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(5L, 6L)).thenReturn(Optional.of(role));
    }

    @Test
    void awardsTrusBotAchievementAfterSuccessfulAnswer() {
        AiQuestionEntity reserved = question(21L, "Kolik jsem vypil piv?");
        AiQuestionEntity completed = question(21L, "Kolik jsem vypil piv?");
        completed.setAnswer("Sedm piv.");
        completed.setStatus(AiQuestionStatus.COMPLETED);
        completed.setCompletedAt(Instant.now());
        OpenAiAnswer answer = new OpenAiAnswer("Sedm piv.", "gpt-test", 10, 5);

        when(quotaService.reserve(5L, appTeam, reserved.getQuestion()))
                .thenReturn(AiQuotaDecision.allowed(reserved, usage));
        when(openAiClient.answer(eq(reserved.getQuestion()), any(AiToolContext.class), eq(AiAccessTier.STANDARD)))
                .thenReturn(answer);
        when(quotaService.complete(21L, answer)).thenReturn(completed);

        AiQuestionResponse response = service.ask(request(reserved.getQuestion()));

        assertEquals("Sedm piv.", response.getAnswer());
        verify(achievementService).awardForSuccessfulQuestion(7L, appTeam);
    }

    @Test
    void achievementFailureDoesNotTurnCompletedAnswerIntoFailure() {
        AiQuestionEntity reserved = question(22L, "Kdo dal nejvíc gólů?");
        AiQuestionEntity completed = question(22L, "Kdo dal nejvíc gólů?");
        completed.setAnswer("Franta.");
        completed.setStatus(AiQuestionStatus.COMPLETED);
        completed.setCompletedAt(Instant.now());
        OpenAiAnswer answer = new OpenAiAnswer("Franta.", "gpt-test", 8, 3);

        when(quotaService.reserve(5L, appTeam, reserved.getQuestion()))
                .thenReturn(AiQuotaDecision.allowed(reserved, usage));
        when(openAiClient.answer(eq(reserved.getQuestion()), any(AiToolContext.class), eq(AiAccessTier.STANDARD)))
                .thenReturn(answer);
        when(quotaService.complete(22L, answer)).thenReturn(completed);
        doThrow(new IllegalStateException("achievement unavailable"))
                .when(achievementService).awardForSuccessfulQuestion(7L, appTeam);

        AiQuestionResponse response = service.ask(request(reserved.getQuestion()));

        assertEquals(AiQuestionStatus.COMPLETED, response.getStatus());
        assertEquals("Franta.", response.getAnswer());
        verify(quotaService, never()).fail(anyLong(), any());
    }

    private AiQuestionEntity question(Long id, String text) {
        AiQuestionEntity question = new AiQuestionEntity();
        question.setId(id);
        question.setQuestion(text);
        question.setStatus(AiQuestionStatus.PENDING);
        question.setCreatedAt(Instant.now());
        return question;
    }

    private AiAskRequest request(String question) {
        AiAskRequest request = new AiAskRequest();
        request.setQuestion(question);
        return request;
    }
}
