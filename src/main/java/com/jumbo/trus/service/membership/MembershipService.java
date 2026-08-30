package com.jumbo.trus.service.membership;

import com.jumbo.trus.entity.membership.MembershipTier;
import com.jumbo.trus.entity.membership.AchievementMembershipCreditEntity;
import com.jumbo.trus.entity.membership.MembershipAccountEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.membership.AchievementMembershipCreditRepository;
import com.jumbo.trus.repository.membership.MembershipAccountRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import com.jumbo.trus.service.exceptions.MembershipGrantException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MembershipService {

    public static final int DRINKS_PER_PREMIUM_WEEK = 10;
    public static final int DAYS_PER_REWARD_WEEK = 7;
    private static final long REWARD_WEEK_MILLIS = Duration.ofDays(DAYS_PER_REWARD_WEEK).toMillis();

    private final MembershipAccountRepository accountRepository;
    private final AchievementMembershipCreditRepository achievementCreditRepository;
    private final UserRepository userRepository;
    private final UserTeamRoleRepository userTeamRoleRepository;

    /**
     * Vytvoří výchozí účet bez zpětného přidělení týdnů. Piva a panáky se
     * začínají počítat až od vzniku tohoto účtu.
     */
    @Transactional
    public void initializeBaseline(Long userId) {
        UserEntity user = lockUser(userId);
        if (accountRepository.findByUserIdForUpdate(userId).isPresent()) {
            return;
        }
        createAccount(user);
    }

    /**
     * Vytvoří účet pro právě persistovaného uživatele bez dalšího načítání
     * stejné entity. Volající drží transakci, takže uživatel i členství vzniknou
     * atomicky a Hibernate nemusí během registrace znovu připojovat jeho kolekce.
     */
    @Transactional
    public void initializeBaselineForNewUser(UserEntity user) {
        createAccount(user);
    }

    @Transactional
    public MembershipSnapshot getSnapshot(Long userId) {
        UserEntity user = lockUser(userId);
        return getSnapshotForLockedUser(user);
    }

    public MembershipSnapshot getSnapshotForLockedUser(UserEntity lockedUser) {
        Instant now = Instant.now();
        MembershipAccountEntity account = getOrCreateAccount(lockedUser, now);
        settle(account, now);
        syncDrinkMilestones(account);
        accountRepository.save(account);
        return toSnapshot(account, now);
    }

    @Transactional
    public void recordDrinkChanges(Map<Long, Integer> drinkChangesByPlayer) {
        if (drinkChangesByPlayer == null || drinkChangesByPlayer.isEmpty()) {
            return;
        }
        Map<Long, Integer> drinkChangesByUser = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> change : drinkChangesByPlayer.entrySet()) {
            Long playerId = change.getKey();
            int delta = change.getValue() == null ? 0 : change.getValue();
            if (playerId == null || delta == 0) {
                continue;
            }
            userTeamRoleRepository.findAllByPlayerId(playerId).stream()
                    .map(UserTeamRole::getUser)
                    .map(UserEntity::getId)
                    .distinct()
                    .forEach(userId -> drinkChangesByUser.merge(userId, delta, Integer::sum));
        }
        for (Map.Entry<Long, Integer> change : drinkChangesByUser.entrySet()) {
            Long userId = change.getKey();
            UserEntity user = lockUser(userId);
            Instant now = Instant.now();
            MembershipAccountEntity account = getOrCreateAccount(user, now);
            settle(account, now);
            account.setCountedDrinks(Math.max(0, account.getCountedDrinks() + change.getValue()));
            syncDrinkMilestones(account);
            accountRepository.save(account);
        }
    }

    @Transactional
    public void achievementAccomplished(Long playerId, Long playerAchievementId) {
        changeAchievementCredit(playerId, playerAchievementId, true);
    }

    @Transactional
    public void achievementRevoked(Long playerId, Long playerAchievementId) {
        changeAchievementCredit(playerId, playerAchievementId, false);
    }

    @Transactional
    public void setBackendGrant(
            Long userId,
            MembershipTier tier,
            Duration duration,
            boolean unlimited
    ) {
        UserEntity user = lockUser(userId);
        Instant now = Instant.now();
        MembershipAccountEntity account = getOrCreateAccount(user, now);
        settle(account, now);

        account.setGrantedUltraMillisRemaining(0);
        account.setGrantedPremiumMillisRemaining(0);
        account.setUnlimitedTier(MembershipTier.STANDARD);

        if (tier == null || tier == MembershipTier.STANDARD) {
            accountRepository.save(account);
            return;
        }
        if (unlimited) {
            account.setUnlimitedTier(tier);
            accountRepository.save(account);
            return;
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new MembershipGrantException("Časově omezené členství musí mít kladnou dobu platnosti.");
        }
        long durationMillis;
        try {
            durationMillis = duration.toMillis();
        } catch (ArithmeticException exception) {
            throw new MembershipGrantException("Doba členství je příliš dlouhá.", exception);
        }
        if (tier == MembershipTier.ULTRA) {
            account.setGrantedUltraMillisRemaining(durationMillis);
        } else {
            account.setGrantedPremiumMillisRemaining(durationMillis);
        }
        accountRepository.save(account);
    }

    @Transactional
    public void clearBackendGrant(Long userId) {
        setBackendGrant(userId, MembershipTier.STANDARD, null, false);
    }

    private void changeAchievementCredit(Long playerId, Long playerAchievementId, boolean accomplished) {
        if (playerId == null || playerAchievementId == null) {
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        userTeamRoleRepository.findAllByPlayerId(playerId).stream()
                .map(UserTeamRole::getUser)
                .map(UserEntity::getId)
                .forEach(userIds::add);

        for (Long userId : userIds) {
            UserEntity user = lockUser(userId);
            Instant now = Instant.now();
            MembershipAccountEntity account = getOrCreateAccount(user, now);
            settle(account, now);

            AchievementMembershipCreditEntity credit = achievementCreditRepository
                    .findForUpdate(userId, playerAchievementId)
                    .orElse(null);

            if (accomplished) {
                if (credit != null && credit.isActive()) {
                    continue;
                }
                if (credit == null) {
                    credit = new AchievementMembershipCreditEntity();
                    credit.setUser(user);
                    credit.setPlayerAchievementId(playerAchievementId);
                }
                credit.setActive(true);
                account.setUltraMillisRemaining(safeAdd(account.getUltraMillisRemaining(), REWARD_WEEK_MILLIS));
                achievementCreditRepository.save(credit);
                accountRepository.save(account);
                continue;
            }

            if (credit == null || !credit.isActive()) {
                continue;
            }
            credit.setActive(false);
            account.setUltraMillisRemaining(Math.max(0L, account.getUltraMillisRemaining() - REWARD_WEEK_MILLIS));
            achievementCreditRepository.save(credit);
            accountRepository.save(account);
        }
    }

    private void syncDrinkMilestones(MembershipAccountEntity account) {
        long reachedMilestone = account.getCountedDrinks() / DRINKS_PER_PREMIUM_WEEK;
        if (reachedMilestone <= account.getLastDrinkMilestone()) {
            return;
        }
        long newWeeks = reachedMilestone - account.getLastDrinkMilestone();
        account.setPremiumMillisRemaining(safeAdd(
                account.getPremiumMillisRemaining(),
                Math.multiplyExact(newWeeks, REWARD_WEEK_MILLIS)
        ));
        account.setLastDrinkMilestone(reachedMilestone);
    }

    private MembershipAccountEntity getOrCreateAccount(UserEntity user, Instant now) {
        return accountRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> createAccount(user, now));
    }

    private MembershipAccountEntity createAccount(UserEntity user) {
        return createAccount(user, Instant.now());
    }

    private MembershipAccountEntity createAccount(UserEntity user, Instant now) {
        MembershipAccountEntity account = new MembershipAccountEntity();
        account.setUser(user);
        account.setUltraMillisRemaining(0);
        account.setPremiumMillisRemaining(0);
        account.setGrantedUltraMillisRemaining(0);
        account.setGrantedPremiumMillisRemaining(0);
        account.setUnlimitedTier(MembershipTier.STANDARD);
        account.setBalanceUpdatedAt(now);
        account.setLastDrinkMilestone(0);
        account.setCountedDrinks(0);
        account.setDrinkCountingStartedAt(now);
        return accountRepository.saveAndFlush(account);
    }

    private void settle(MembershipAccountEntity account, Instant now) {
        Instant lastUpdate = account.getBalanceUpdatedAt();
        if (lastUpdate == null || !now.isAfter(lastUpdate)) {
            account.setBalanceUpdatedAt(now);
            return;
        }
        long elapsed = Duration.between(lastUpdate, now).toMillis();
        long fromUltra = Math.min(elapsed, account.getUltraMillisRemaining());
        account.setUltraMillisRemaining(account.getUltraMillisRemaining() - fromUltra);
        long elapsedAfterEarnedUltra = elapsed - fromUltra;
        long fromGrantedUltra = Math.min(elapsedAfterEarnedUltra, account.getGrantedUltraMillisRemaining());
        account.setGrantedUltraMillisRemaining(account.getGrantedUltraMillisRemaining() - fromGrantedUltra);
        long elapsedAfterUltra = elapsedAfterEarnedUltra - fromGrantedUltra;
        long fromPremium = Math.min(elapsedAfterUltra, account.getPremiumMillisRemaining());
        account.setPremiumMillisRemaining(account.getPremiumMillisRemaining() - fromPremium);
        long elapsedAfterEarnedPremium = elapsedAfterUltra - fromPremium;
        long fromGrantedPremium = Math.min(elapsedAfterEarnedPremium, account.getGrantedPremiumMillisRemaining());
        account.setGrantedPremiumMillisRemaining(account.getGrantedPremiumMillisRemaining() - fromGrantedPremium);
        account.setBalanceUpdatedAt(now);
    }

    private MembershipSnapshot toSnapshot(
            MembershipAccountEntity account,
            Instant now
    ) {
        long ultraMillis = safeAdd(
                account.getUltraMillisRemaining(),
                account.getGrantedUltraMillisRemaining()
        );
        long premiumMillis = safeAdd(
                account.getPremiumMillisRemaining(),
                account.getGrantedPremiumMillisRemaining()
        );
        MembershipTier timedTier = ultraMillis > 0
                ? MembershipTier.ULTRA
                : premiumMillis > 0 ? MembershipTier.PREMIUM : MembershipTier.STANDARD;
        Instant ultraUntil = ultraMillis > 0 ? now.plusMillis(ultraMillis) : null;
        Instant premiumUntil = premiumMillis > 0
                ? now.plusMillis(safeAdd(ultraMillis, premiumMillis))
                : null;

        long drinksAfterLastMilestone = Math.max(
                0,
                account.getCountedDrinks() - account.getLastDrinkMilestone() * DRINKS_PER_PREMIUM_WEEK
        );
        int towardNext = (int) Math.min(DRINKS_PER_PREMIUM_WEEK, drinksAfterLastMilestone);
        long nextMilestoneAt = (account.getLastDrinkMilestone() + 1) * DRINKS_PER_PREMIUM_WEEK;
        int toNext = Math.toIntExact(Math.max(1, nextMilestoneAt - account.getCountedDrinks()));
        return new MembershipSnapshot(
                timedTier,
                account.getUnlimitedTier(),
                ultraMillis,
                premiumMillis,
                ultraUntil,
                premiumUntil,
                account.getCountedDrinks(),
                account.getDrinkCountingStartedAt(),
                towardNext,
                toNext
        );
    }

    private UserEntity lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new EntityNotFoundException("Uživatel nebyl nalezen: " + userId));
    }

    private long safeAdd(long first, long second) {
        try {
            return Math.addExact(first, second);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }
}
