package com.jumbo.trus.service.ai;

import com.jumbo.trus.dto.ai.AiAccessDTO;
import com.jumbo.trus.dto.ai.AiAccessUpdateRequest;
import com.jumbo.trus.dto.ai.AiUsageDTO;
import com.jumbo.trus.entity.ai.AiAccessTier;
import com.jumbo.trus.entity.ai.AiQuestionEntity;
import com.jumbo.trus.entity.ai.AiQuestionStatus;
import com.jumbo.trus.entity.ai.AiUserAccessEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.repository.ai.AiQuestionRepository;
import com.jumbo.trus.repository.ai.AiUserAccessRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private static final ZoneId QUOTA_ZONE = ZoneId.of("Europe/Prague");

    private final AiUserAccessRepository accessRepository;
    private final AiQuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Transactional
    public AiQuotaDecision reserve(
            Long userId,
            AppTeamEntity appTeam,
            String questionText
    ) {
        UserEntity user = lockUser(userId);
        AiUserAccessEntity access = getOrCreateAccessForLockedUser(user);
        UsageWindow window = currentUsageWindow();
        long used = countUsage(userId, window);
        AiUsageDTO currentUsage = toUsage(access, used, window.date());

        if (!access.isEnabled()) {
            return AiQuotaDecision.denied(
                    currentUsage,
                    AiQuestionStatus.DISABLED,
                    "Trusbot není pro váš účet povolen."
            );
        }

        Integer dailyLimit = access.getDailyLimit();
        if (dailyLimit != null && used >= dailyLimit) {
            return AiQuotaDecision.denied(
                    currentUsage,
                    AiQuestionStatus.LIMIT_REACHED,
                    "Pro dnešek jste vyčerpali limit AI dotazů. Další dotaz bude dostupný zítra."
            );
        }

        AiQuestionEntity question = new AiQuestionEntity();
        question.setUser(user);
        question.setAppTeam(appTeam);
        question.setQuestion(questionText.trim());
        question.setStatus(AiQuestionStatus.PENDING);
        question = questionRepository.save(question);

        return AiQuotaDecision.allowed(
                question,
                toUsage(access, used + 1, window.date())
        );
    }

    @Transactional
    public AiUsageDTO getUsage(Long userId) {
        UserEntity user = lockUser(userId);
        AiUserAccessEntity access = getOrCreateAccessForLockedUser(user);
        UsageWindow window = currentUsageWindow();
        return toUsage(access, countUsage(userId, window), window.date());
    }

    @Transactional(readOnly = true)
    public List<AiQuestionEntity> getHistory(Long userId, Long appTeamId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return questionRepository.findByUserIdAndAppTeamIdOrderByCreatedAtDesc(
                userId,
                appTeamId,
                PageRequest.of(0, safeLimit)
        );
    }

    @Transactional
    public AiQuestionEntity complete(
            Long questionId,
            OpenAiAnswer answer
    ) {
        AiQuestionEntity question = getQuestion(questionId);
        question.setAnswer(answer.text());
        question.setModel(answer.model());
        question.setInputTokens(answer.inputTokens());
        question.setOutputTokens(answer.outputTokens());
        question.setStatus(AiQuestionStatus.COMPLETED);
        question.setCompletedAt(Instant.now());
        question.setErrorMessage(null);
        return questionRepository.save(question);
    }

    @Transactional
    public void fail(Long questionId, RuntimeException error) {
        AiQuestionEntity question = getQuestion(questionId);
        question.setStatus(AiQuestionStatus.FAILED);
        question.setCompletedAt(Instant.now());
        question.setErrorMessage(truncate(error.getMessage(), 2000));
        questionRepository.save(question);
    }

    @Transactional
    public AiAccessDTO updateAccess(Long userId, AiAccessUpdateRequest request) {
        UserEntity user = lockUser(userId);
        AiUserAccessEntity access = getOrCreateAccessForLockedUser(user);
        AiAccessTier tier = request.getTier();
        access.setTier(tier);
        access.setDailyLimit(
                tier == AiAccessTier.ULTRA
                        ? null
                        : request.getDailyLimit() != null
                        ? request.getDailyLimit()
                        : tier.getDefaultDailyLimit()
        );
        if (request.getEnabled() != null) {
            access.setEnabled(request.getEnabled());
        }
        return toAccessDTO(accessRepository.save(access));
    }

    @Transactional(readOnly = true)
    public List<AiAccessDTO> getAllAccess() {
        return accessRepository.findAllByOrderByUserNameAsc()
                .stream()
                .map(this::toAccessDTO)
                .toList();
    }

    private UserEntity lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new EntityNotFoundException("Uživatel nebyl nalezen: " + userId));
    }

    private AiUserAccessEntity getOrCreateAccessForLockedUser(UserEntity user) {
        return accessRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> {
                    AiUserAccessEntity access = new AiUserAccessEntity();
                    access.setUser(user);
                    access.setTier(AiAccessTier.STANDARD);
                    access.setDailyLimit(AiAccessTier.STANDARD.getDefaultDailyLimit());
                    access.setEnabled(true);
                    return accessRepository.saveAndFlush(access);
                });
    }

    private AiQuestionEntity getQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("AI dotaz nebyl nalezen: " + questionId));
    }

    private long countUsage(Long userId, UsageWindow window) {
        return questionRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                userId,
                window.from(),
                window.to()
        );
    }

    private UsageWindow currentUsageWindow() {
        LocalDate date = LocalDate.now(QUOTA_ZONE);
        Instant from = date.atStartOfDay(QUOTA_ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(QUOTA_ZONE).toInstant();
        return new UsageWindow(date, from, to);
    }

    private AiUsageDTO toUsage(AiUserAccessEntity access, long used, LocalDate date) {
        Integer limit = access.getDailyLimit();
        boolean unlimited = limit == null;
        Integer remaining = unlimited ? null : Math.max(0, limit - Math.toIntExact(used));
        return new AiUsageDTO(
                access.getTier(),
                used,
                limit,
                remaining,
                unlimited,
                access.isEnabled(),
                date
        );
    }

    private AiAccessDTO toAccessDTO(AiUserAccessEntity access) {
        return new AiAccessDTO(
                access.getUser().getId(),
                access.getUser().getName(),
                access.getUser().getMail(),
                access.getTier(),
                access.getDailyLimit(),
                access.isEnabled()
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record UsageWindow(LocalDate date, Instant from, Instant to) {
    }
}
