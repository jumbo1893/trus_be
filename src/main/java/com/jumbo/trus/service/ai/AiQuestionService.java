package com.jumbo.trus.service.ai;

import com.jumbo.trus.dto.ai.*;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ai.AiQuestionEntity;
import com.jumbo.trus.entity.ai.AiQuestionStatus;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.auth.AuthService;
import com.jumbo.trus.service.exceptions.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private final AuthService authService;
    private final AppTeamService appTeamService;
    private final UserTeamRoleRepository userTeamRoleRepository;
    private final AiQuotaService quotaService;
    private final OpenAiClient openAiClient;

    public AiQuestionResponse ask(AiAskRequest request) {
        // Konfigurační chyba nesmí uživateli spotřebovat denní dotaz.
        openAiClient.requireConfigured();

        UserEntity user = authService.getCurrentUserEntity();
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        AiQuotaDecision decision = quotaService.reserve(user.getId(), appTeam, request.getQuestion());

        if (!decision.allowed()) {
            return new AiQuestionResponse(
                    null,
                    request.getQuestion().trim(),
                    decision.deniedMessage(),
                    decision.deniedStatus(),
                    Instant.now(),
                    Instant.now(),
                    decision.usage()
            );
        }

        AiQuestionEntity reservedQuestion = decision.question();
        try {
            AiToolContext context = createToolContext(user, appTeam);
            OpenAiAnswer answer = openAiClient.answer(
                    reservedQuestion.getQuestion(),
                    context,
                    decision.usage().getTier()
            );
            AiQuestionEntity completed = quotaService.complete(reservedQuestion.getId(), answer);
            return toResponse(completed, decision.usage());
        } catch (RuntimeException exception) {
            try {
                quotaService.fail(reservedQuestion.getId(), exception);
            } catch (RuntimeException ignored) {
                // Původní chyba z OpenAI je pro klienta důležitější než případná chyba auditu.
            }
            throw exception;
        }
    }

    public List<AiQuestionResponse> getHistory(int limit) {
        UserEntity user = authService.getCurrentUserEntity();
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        AiUsageDTO usage = quotaService.getUsage(user.getId());
        return quotaService.getHistory(user.getId(), appTeam.getId(), limit)
                .stream()
                .map(question -> toResponse(question, usage))
                .toList();
    }

    public AiUsageDTO getUsage() {
        return quotaService.getUsage(authService.getCurrentUserEntity().getId());
    }

    public List<AiAccessDTO> getAllAccess() {
        requireGlobalAdmin();
        return quotaService.getAllAccess();
    }

    public AiAccessDTO updateAccess(Long userId, AiAccessUpdateRequest request) {
        requireGlobalAdmin();
        return quotaService.updateAccess(userId, request);
    }

    private AiToolContext createToolContext(UserEntity user, AppTeamEntity appTeam) {
        PlayerEntity player = userTeamRoleRepository
                .findByUserIdAndAppTeamId(user.getId(), appTeam.getId())
                .map(UserTeamRole::getPlayer)
                .orElse(null);
        return new AiToolContext(
                user,
                appTeam,
                player == null ? null : player.getId(),
                player == null ? null : player.getName()
        );
    }

    private AiQuestionResponse toResponse(AiQuestionEntity question, AiUsageDTO usage) {
        String answer = question.getAnswer();
        if (answer == null && question.getStatus() == AiQuestionStatus.FAILED) {
            answer = "Odpověď se nepodařilo vytvořit. Zkuste to prosím později.";
        } else if (answer == null && question.getStatus() == AiQuestionStatus.PENDING) {
            answer = "Dotaz se stále zpracovává nebo bylo zpracování přerušeno.";
        }
        return new AiQuestionResponse(
                question.getId(),
                question.getQuestion(),
                answer,
                question.getStatus(),
                question.getCreatedAt(),
                question.getCompletedAt(),
                usage
        );
    }

    private void requireGlobalAdmin() {
        if (!authService.getCurrentUserEntity().isAdmin()) {
            throw new AuthException(
                    "Správu AI limitů může provádět pouze administrátor aplikace.",
                    AuthException.INSUFFICIENT_RIGHTS
            );
        }
    }
}
