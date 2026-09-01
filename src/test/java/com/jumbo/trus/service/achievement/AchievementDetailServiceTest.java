package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.player.PlayerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AchievementDetailServiceTest {

    private final PlayerService playerService = mock(PlayerService.class);
    private final PlayerMapper playerMapper = mock(PlayerMapper.class);
    private final PlayerAchievementRepository playerAchievementRepository =
            mock(PlayerAchievementRepository.class);
    private final PlayerAchievementMapper playerAchievementMapper =
            mock(PlayerAchievementMapper.class);
    private final AchievementDetailService service = new AchievementDetailService(
            playerService,
            playerMapper,
            playerAchievementRepository,
            playerAchievementMapper
    );

    @Test
    void buildsAllDetailsFromOneBatchedRepositoryCall() {
        AchievementDTO first = achievementDto(10L, "První");
        AchievementDTO second = achievementDto(20L, "Druhý");
        AchievementEntity firstEntity = achievementEntity(10L);
        AchievementEntity secondEntity = achievementEntity(20L);

        when(playerAchievementRepository.findAllForDetailsByPlayerIds(List.of(1L, 2L)))
                .thenReturn(List.of(
                        playerAchievement(firstEntity, player(1L, "Žaneta"), true),
                        playerAchievement(firstEntity, player(2L, "adam"), false),
                        playerAchievement(secondEntity, player(1L, "Žaneta"), false)
                ));

        List<AchievementDetail> result = service.returnAchievementDetails(
                List.of(first, second),
                List.of(1L, 2L)
        );

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getTotalCount());
        assertEquals(1, result.get(0).getAccomplishedCount());
        assertEquals(0.5F, result.get(0).getSuccessRate());
        assertEquals("Žaneta", result.get(0).getAccomplishedPlayers());
        assertEquals(1, result.get(1).getTotalCount());
        assertEquals(0, result.get(1).getAccomplishedCount());
        assertNull(result.get(1).getAccomplishedPlayers());

        verify(playerAchievementRepository).findAllForDetailsByPlayerIds(List.of(1L, 2L));
        verify(playerAchievementRepository, never()).countAchievements(
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyLong()
        );
        verify(playerAchievementRepository, never()).findAccomplishedPlayersByAchievement(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    void skipsRepositoryQueryWhenTeamHasNoPlayers() {
        AchievementDTO achievement = achievementDto(10L, "První");

        List<AchievementDetail> result = service.returnAchievementDetails(
                List.of(achievement),
                List.of()
        );

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getTotalCount());
        assertEquals(0, result.get(0).getAccomplishedCount());
        assertEquals(0F, result.get(0).getSuccessRate());
        verify(playerAchievementRepository, never())
                .findAllForDetailsByPlayerIds(org.mockito.ArgumentMatchers.anyCollection());
    }

    private AchievementDTO achievementDto(long id, String name) {
        AchievementDTO achievement = new AchievementDTO();
        achievement.setId(id);
        achievement.setName(name);
        return achievement;
    }

    private AchievementEntity achievementEntity(long id) {
        AchievementEntity achievement = new AchievementEntity();
        achievement.setId(id);
        return achievement;
    }

    private PlayerEntity player(long id, String name) {
        PlayerEntity player = new PlayerEntity();
        player.setId(id);
        player.setName(name);
        return player;
    }

    private PlayerAchievementEntity playerAchievement(
            AchievementEntity achievement,
            PlayerEntity player,
            boolean accomplished
    ) {
        PlayerAchievementEntity playerAchievement = new PlayerAchievementEntity();
        playerAchievement.setAchievement(achievement);
        playerAchievement.setPlayer(player);
        playerAchievement.setAccomplished(accomplished);
        return playerAchievement;
    }
}
