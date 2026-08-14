package com.jumbo.trus.service.achievement.init;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.helper.AchievementEligibilityService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerAchievementInitializationServiceTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final AchievementRepository achievementRepository = mock(AchievementRepository.class);
    private final PlayerAchievementRepository playerAchievementRepository = mock(PlayerAchievementRepository.class);
    private final PlayerAchievementInitializationService initializationService =
            new PlayerAchievementInitializationService(
                    playerRepository,
                    achievementRepository,
                    playerAchievementRepository,
                    new AchievementEligibilityService()
            );

    @Test
    void initializesFanWithAchievementsAvailableToFansOnly() {
        PlayerEntity fan = player(true);
        AchievementEntity sharedAchievement = achievement(1L, false);
        AchievementEntity playerOnlyAchievement = achievement(2L, true);
        stubInitialization(fan, List.of(), List.of(sharedAchievement, playerOnlyAchievement));

        int initialized = initializationService.initializeAchievementsForPlayer(fan.getId());

        List<PlayerAchievementEntity> saved = savedAchievements();
        assertThat(initialized).isEqualTo(1);
        assertThat(saved).singleElement().satisfies(playerAchievement -> {
            assertThat(playerAchievement.getAchievement()).isSameAs(sharedAchievement);
            assertThat(playerAchievement.getAccomplished()).isFalse();
            assertThat(playerAchievement.getAccomplishedDate()).isNull();
        });
    }

    @Test
    void initializesOnlyMissingPlayerAchievementsAfterFanBecomesPlayer() {
        PlayerEntity player = player(false);
        AchievementEntity sharedAchievement = achievement(1L, false);
        AchievementEntity playerOnlyAchievement = achievement(2L, true);
        stubInitialization(player, List.of(sharedAchievement.getId()), List.of(sharedAchievement, playerOnlyAchievement));

        int initialized = initializationService.initializeAchievementsForPlayer(player.getId());

        List<PlayerAchievementEntity> saved = savedAchievements();
        assertThat(initialized).isEqualTo(1);
        assertThat(saved).singleElement()
                .extracting(PlayerAchievementEntity::getAchievement)
                .isSameAs(playerOnlyAchievement);
    }

    private void stubInitialization(
            PlayerEntity player,
            List<Long> existingAchievementIds,
            List<AchievementEntity> achievements
    ) {
        when(playerRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(playerAchievementRepository.findAchievementIdsByPlayerId(player.getId()))
                .thenReturn(existingAchievementIds);
        when(achievementRepository.findAll()).thenReturn(achievements);
    }

    @SuppressWarnings("unchecked")
    private List<PlayerAchievementEntity> savedAchievements() {
        ArgumentCaptor<Iterable<PlayerAchievementEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(playerAchievementRepository).saveAll(captor.capture());
        return StreamSupport.stream(captor.getValue().spliterator(), false).toList();
    }

    private PlayerEntity player(boolean fan) {
        PlayerEntity player = new PlayerEntity();
        player.setId(12L);
        player.setFan(fan);
        return player;
    }

    private AchievementEntity achievement(long id, boolean onlyForPlayers) {
        AchievementEntity achievement = new AchievementEntity();
        achievement.setId(id);
        achievement.setOnlyForPlayers(onlyForPlayers);
        return achievement;
    }
}
