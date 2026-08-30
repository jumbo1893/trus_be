package com.jumbo.trus.service.membership;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.membership.AchievementMembershipCreditEntity;
import com.jumbo.trus.entity.membership.MembershipAccountEntity;
import com.jumbo.trus.entity.membership.MembershipTier;
import com.jumbo.trus.repository.membership.AchievementMembershipCreditRepository;
import com.jumbo.trus.repository.membership.MembershipAccountRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MembershipServiceTest {

    private static final long WEEK_MILLIS = Duration.ofDays(7).toMillis();

    private final MembershipAccountRepository accountRepository = mock(MembershipAccountRepository.class);
    private final AchievementMembershipCreditRepository creditRepository = mock(AchievementMembershipCreditRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserTeamRoleRepository roleRepository = mock(UserTeamRoleRepository.class);
    private final MembershipService service = new MembershipService(
            accountRepository,
            creditRepository,
            userRepository,
            roleRepository
    );

    private UserEntity user;
    private PlayerEntity player;
    private MembershipAccountEntity account;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        player = new PlayerEntity();
        player.setId(5L);

        UserTeamRole role = new UserTeamRole();
        role.setUser(user);
        role.setPlayer(player);

        account = new MembershipAccountEntity();
        account.setId(2L);
        account.setUser(user);
        account.setBalanceUpdatedAt(Instant.now());
        account.setDrinkCountingStartedAt(Instant.parse("2026-08-25T00:00:00Z"));
        account.setUnlimitedTier(MembershipTier.STANDARD);

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findAllByPlayerId(5L)).thenReturn(List.of(role));
        when(accountRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(MembershipAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void grantsPremiumWeekOnlyAfterTenthNewDrink() {
        service.recordDrinkChanges(Map.of(5L, 9));

        assertEquals(9, account.getCountedDrinks());
        assertEquals(0, account.getPremiumMillisRemaining());
        assertEquals(0, account.getLastDrinkMilestone());

        service.recordDrinkChanges(Map.of(5L, 1));

        assertEquals(10, account.getCountedDrinks());
        assertEquals(WEEK_MILLIS, account.getPremiumMillisRemaining());
        assertEquals(1, account.getLastDrinkMilestone());
    }

    @Test
    void negativeCorrectionDoesNotRevokeEarnedPremiumWeek() {
        service.recordDrinkChanges(Map.of(5L, 10));
        service.recordDrinkChanges(Map.of(5L, -3));

        assertEquals(7, account.getCountedDrinks());
        assertEquals(1, account.getLastDrinkMilestone());
        assertTrue(account.getPremiumMillisRemaining() > WEEK_MILLIS - 1_000);

        MembershipSnapshot snapshot = service.getSnapshot(1L);
        assertEquals(0, snapshot.drinksTowardNextPremium());
        assertEquals(13, snapshot.drinksToNextPremium());
    }

    @Test
    void ultraRunsBeforePremiumAndRevocationNeverGoesBelowZero() {
        service.recordDrinkChanges(Map.of(5L, 10));

        AtomicReference<AchievementMembershipCreditEntity> savedCredit = new AtomicReference<>();
        when(creditRepository.findForUpdate(1L, 77L))
                .thenAnswer(invocation -> Optional.ofNullable(savedCredit.get()));
        when(creditRepository.save(any(AchievementMembershipCreditEntity.class)))
                .thenAnswer(invocation -> {
                    AchievementMembershipCreditEntity credit = invocation.getArgument(0);
                    savedCredit.set(credit);
                    return credit;
                });

        service.achievementAccomplished(5L, 77L);
        MembershipSnapshot active = service.getSnapshot(1L);

        assertEquals(MembershipTier.ULTRA, active.timedTier());
        assertNotNull(active.ultraUntil());
        assertNotNull(active.premiumUntil());
        assertTrue(active.premiumUntil().isAfter(active.ultraUntil()));

        service.achievementRevoked(5L, 77L);
        service.achievementRevoked(5L, 77L);

        assertEquals(0, account.getUltraMillisRemaining());
        assertTrue(account.getPremiumMillisRemaining() > 0);
    }

    @Test
    void backendCanReplaceGrantWithTimedOrUnlimitedMembership() {
        service.setBackendGrant(1L, MembershipTier.ULTRA, Duration.ofDays(25).plusHours(8), false);

        MembershipSnapshot timed = service.getSnapshot(1L);
        assertEquals(MembershipTier.ULTRA, timed.timedTier());
        assertEquals(MembershipTier.STANDARD, timed.unlimitedTier());
        assertTrue(timed.ultraMillisRemaining() > Duration.ofDays(25).toMillis());

        service.setBackendGrant(1L, MembershipTier.PREMIUM, null, true);

        MembershipSnapshot unlimited = service.getSnapshot(1L);
        assertEquals(MembershipTier.PREMIUM, unlimited.unlimitedTier());
        assertEquals(0, account.getGrantedUltraMillisRemaining());
        assertEquals(0, account.getGrantedPremiumMillisRemaining());
    }

    @Test
    void initializesNewUserWithoutReloadingIt() {
        when(accountRepository.saveAndFlush(any(MembershipAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.initializeBaselineForNewUser(user);

        verify(userRepository, never()).findByIdForUpdate(anyLong());
        verify(accountRepository, never()).findByUserIdForUpdate(anyLong());
        verify(accountRepository).saveAndFlush(argThat(created -> created.getUser() == user));
    }
}
