package com.jumbo.trus.service.ai;

import com.jumbo.trus.dto.ai.AiUsageDTO;
import com.jumbo.trus.dto.membership.MembershipDTO;
import com.jumbo.trus.dto.membership.MembershipGrantRequest;
import com.jumbo.trus.entity.ai.AiAccessTier;
import com.jumbo.trus.entity.ai.AiQuestionEntity;
import com.jumbo.trus.entity.ai.AiQuestionStatus;
import com.jumbo.trus.entity.membership.MembershipTier;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.repository.ai.AiQuestionRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.service.membership.MembershipService;
import com.jumbo.trus.service.membership.MembershipSnapshot;
import com.jumbo.trus.service.exceptions.MembershipGrantException;
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

    private final AiQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;

    @Transactional
    public AiQuotaDecision reserve(
            Long userId,
            AppTeamEntity appTeam,
        String questionText
    ) {
        UserEntity user = lockUser(userId);
        EffectiveAccess effectiveAccess = effectiveAccess(membershipService.getSnapshotForLockedUser(user));
        UsageWindow window = currentUsageWindow();
        long used = countUsage(userId, window);
        AiUsageDTO currentUsage = toUsage(effectiveAccess, used, window.date());

        Integer dailyLimit = effectiveAccess.dailyLimit();
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
                toUsage(effectiveAccess, used + 1, window.date())
        );
    }

    @Transactional
    public AiUsageDTO getUsage(Long userId) {
        UserEntity user = lockUser(userId);
        EffectiveAccess effectiveAccess = effectiveAccess(membershipService.getSnapshotForLockedUser(user));
        UsageWindow window = currentUsageWindow();
        return toUsage(effectiveAccess, countUsage(userId, window), window.date());
    }

    @Transactional
    public MembershipDTO getMembership(Long userId) {
        UserEntity user = lockUser(userId);
        MembershipSnapshot membership = membershipService.getSnapshotForLockedUser(user);
        EffectiveAccess effective = effectiveAccess(membership);
        return new MembershipDTO(
                MembershipTier.valueOf(effective.tier().name()),
                membership.unlimitedTier(),
                membership.timedTier(),
                effective.dailyLimit(),
                membership.ultraMillisRemaining(),
                membership.premiumMillisRemaining(),
                membership.ultraUntil(),
                membership.premiumUntil(),
                membership.countedDrinks(),
                membership.drinkCountingStartedAt(),
                membership.drinksTowardNextPremium(),
                membership.drinksToNextPremium(),
                MembershipService.DRINKS_PER_PREMIUM_WEEK,
                MembershipService.DAYS_PER_REWARD_WEEK
        );
    }

    @Transactional
    public MembershipDTO grantMembership(Long userId, MembershipGrantRequest request) {
        MembershipTier tier = request.getTier();
        boolean unlimited = Boolean.TRUE.equals(request.getUnlimited());
        Duration duration = resolveGrantDuration(request, tier, unlimited);
        membershipService.setBackendGrant(userId, tier, duration, unlimited);
        return getMembership(userId);
    }

    @Transactional
    public MembershipDTO clearMembershipGrant(Long userId) {
        membershipService.clearBackendGrant(userId);
        return getMembership(userId);
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

    private UserEntity lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new EntityNotFoundException("Uživatel nebyl nalezen: " + userId));
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

    private AiUsageDTO toUsage(
            EffectiveAccess effectiveAccess,
            long used,
            LocalDate date
    ) {
        Integer limit = effectiveAccess.dailyLimit();
        boolean unlimited = limit == null;
        Integer remaining = unlimited ? null : Math.max(0, limit - Math.toIntExact(used));
        return new AiUsageDTO(
                effectiveAccess.tier(),
                used,
                limit,
                remaining,
                unlimited,
                true,
                date
        );
    }

    private EffectiveAccess effectiveAccess(MembershipSnapshot membership) {
        MembershipTier membershipTier = maxTier(membership.timedTier(), membership.unlimitedTier());
        AiAccessTier effectiveTier = AiAccessTier.valueOf(membershipTier.name());
        return new EffectiveAccess(effectiveTier, effectiveTier.getDefaultDailyLimit());
    }

    private Duration resolveGrantDuration(
            MembershipGrantRequest request,
            MembershipTier tier,
            boolean unlimited
    ) {
        boolean hasDaysOrHours = request.getDurationDays() != null || request.getDurationHours() != null;
        boolean hasValidUntil = request.getValidUntil() != null;
        if (tier == MembershipTier.STANDARD) {
            return null;
        }
        if (unlimited) {
            if (hasDaysOrHours || hasValidUntil) {
                throw new MembershipGrantException(
                        "Neomezené členství nesmí současně obsahovat dobu platnosti."
                );
            }
            return null;
        }
        if (hasDaysOrHours == hasValidUntil) {
            throw new MembershipGrantException(
                    "Pro časově omezené členství zadejte durationDays/durationHours, nebo validUntil."
            );
        }
        if (hasValidUntil) {
            Duration duration = Duration.between(Instant.now(), request.getValidUntil());
            if (duration.isZero() || duration.isNegative()) {
                throw new MembershipGrantException("validUntil musí ležet v budoucnosti.");
            }
            return duration;
        }
        long days = request.getDurationDays() == null ? 0 : request.getDurationDays();
        long hours = request.getDurationHours() == null ? 0 : request.getDurationHours();
        try {
            Duration duration = Duration.ofDays(days).plusHours(hours);
            if (duration.isZero() || duration.isNegative()) {
                throw new MembershipGrantException("Doba členství musí být delší než nula.");
            }
            return duration;
        } catch (ArithmeticException exception) {
            throw new MembershipGrantException("Doba členství je příliš dlouhá.", exception);
        }
    }

    private MembershipTier maxTier(MembershipTier first, MembershipTier second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record UsageWindow(LocalDate date, Instant from, Instant to) {
    }

    private record EffectiveAccess(AiAccessTier tier, Integer dailyLimit) {
    }
}
