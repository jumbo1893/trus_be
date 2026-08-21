package com.jumbo.trus.service.ai;

import com.jumbo.trus.entity.ai.AiAccessTier;
import com.jumbo.trus.entity.ai.AiQuestionEntity;
import com.jumbo.trus.entity.ai.AiQuestionStatus;
import com.jumbo.trus.entity.ai.AiUserAccessEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.repository.ai.AiQuestionRepository;
import com.jumbo.trus.repository.ai.AiUserAccessRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiQuotaServiceTest {

    private final AiUserAccessRepository accessRepository = mock(AiUserAccessRepository.class);
    private final AiQuestionRepository questionRepository = mock(AiQuestionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiQuotaService service = new AiQuotaService(
            accessRepository,
            questionRepository,
            userRepository
    );

    private UserEntity user;
    private AppTeamEntity appTeam;
    private AiUserAccessEntity access;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(7L);
        appTeam = new AppTeamEntity();
        appTeam.setId(11L);

        access = new AiUserAccessEntity();
        access.setId(1L);
        access.setUser(user);
        access.setTier(AiAccessTier.STANDARD);
        access.setDailyLimit(2);
        access.setEnabled(true);

        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(accessRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(access));
    }

    @Test
    void rejectsQuestionWithoutPersistingWhenDailyLimitIsReached() {
        when(questionRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(7L), any(Instant.class), any(Instant.class)
        )).thenReturn(2L);

        AiQuotaDecision result = service.reserve(7L, appTeam, "Kdo dal nejvíc gólů?");

        assertFalse(result.allowed());
        assertEquals(AiQuestionStatus.LIMIT_REACHED, result.deniedStatus());
        assertEquals(0, result.usage().getRemainingToday());
        verify(questionRepository, never()).save(any(AiQuestionEntity.class));
    }

    @Test
    void reservesQuestionAndReportsUpdatedUsage() {
        when(questionRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(7L), any(Instant.class), any(Instant.class)
        )).thenReturn(1L);
        when(questionRepository.save(any(AiQuestionEntity.class))).thenAnswer(invocation -> {
            AiQuestionEntity question = invocation.getArgument(0);
            question.setId(99L);
            return question;
        });

        AiQuotaDecision result = service.reserve(7L, appTeam, "  Kdo dal nejvíc gólů?  ");

        assertTrue(result.allowed());
        assertEquals(99L, result.question().getId());
        assertEquals("Kdo dal nejvíc gólů?", result.question().getQuestion());
        assertEquals(2, result.usage().getUsedToday());
        assertEquals(0, result.usage().getRemainingToday());
    }

    @Test
    void ultraTierHasNoDailyLimit() {
        access.setTier(AiAccessTier.ULTRA);
        access.setDailyLimit(null);
        when(questionRepository.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(7L), any(Instant.class), any(Instant.class)
        )).thenReturn(10_000L);
        when(questionRepository.save(any(AiQuestionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiQuotaDecision result = service.reserve(7L, appTeam, "Dotaz");

        assertTrue(result.allowed());
        assertTrue(result.usage().isUnlimited());
        assertNull(result.usage().getRemainingToday());
    }
}
