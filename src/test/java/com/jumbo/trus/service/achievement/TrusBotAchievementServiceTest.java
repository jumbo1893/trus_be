package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.helper.AchievementEligibilityService;
import com.jumbo.trus.service.notification.push.maker.AchievementNotificationMaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.jumbo.trus.service.achievement.AchievementCodes.TRUSBOT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrusBotAchievementServiceTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final AchievementRepository achievementRepository = mock(AchievementRepository.class);
    private final PlayerAchievementRepository playerAchievementRepository = mock(PlayerAchievementRepository.class);
    private final AchievementEligibilityService eligibilityService = mock(AchievementEligibilityService.class);
    private final PlayerAchievementMapper playerAchievementMapper = mock(PlayerAchievementMapper.class);
    private final AchievementNotificationMaker notificationMaker = mock(AchievementNotificationMaker.class);
    private final TrusBotAchievementService service = new TrusBotAchievementService(
            playerRepository,
            achievementRepository,
            playerAchievementRepository,
            eligibilityService,
            playerAchievementMapper,
            notificationMaker
    );

    private final AppTeamEntity appTeam = new AppTeamEntity();
    private final PlayerEntity player = new PlayerEntity();
    private final AchievementEntity achievement = new AchievementEntity();

    @BeforeEach
    void setUp() {
        appTeam.setId(11L);
        player.setId(7L);
        player.setAppTeam(appTeam);
        achievement.setId(13L);
        achievement.setCode(TRUSBOT);
        achievement.setName("TrusBot");
        achievement.setOnlyForPlayers(false);

        when(achievementRepository.findByCode(TRUSBOT)).thenReturn(Optional.of(achievement));
        when(playerRepository.findById(7L)).thenReturn(Optional.of(player));
        when(eligibilityService.canHaveAchievement(player, achievement)).thenReturn(true);
        when(playerAchievementRepository.save(any(PlayerAchievementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void awardsAchievementAndSendsNotificationOnlyForFirstQuestion() {
        PlayerAchievementEntity playerAchievement = new PlayerAchievementEntity(
                achievement,
                player,
                false,
                null
        );
        PlayerAchievementDTO mapped = mock(PlayerAchievementDTO.class);
        when(playerAchievementRepository.findByPlayerIdAndAchievementCodeForUpdate(7L, TRUSBOT))
                .thenReturn(Optional.of(playerAchievement));
        when(playerAchievementMapper.toDTO(playerAchievement)).thenReturn(mapped);

        boolean awarded = service.awardForSuccessfulQuestion(7L, appTeam);

        assertTrue(awarded);
        assertTrue(playerAchievement.getAccomplished());
        assertNotNull(playerAchievement.getAccomplishedDate());
        assertEquals("První dotaz položený TrusBotovi.", playerAchievement.getDetail());
        verify(playerAchievementRepository).save(playerAchievement);
        verify(notificationMaker).sendAchievementNotify(mapped, appTeam);
    }

    @Test
    void doesNotAwardOrNotifyAlreadyAccomplishedAchievementAgain() {
        PlayerAchievementEntity playerAchievement = new PlayerAchievementEntity(
                achievement,
                player,
                true,
                new java.util.Date()
        );
        when(playerAchievementRepository.findByPlayerIdAndAchievementCodeForUpdate(7L, TRUSBOT))
                .thenReturn(Optional.of(playerAchievement));

        boolean awarded = service.awardForSuccessfulQuestion(7L, appTeam);

        assertFalse(awarded);
        verify(playerAchievementRepository, never()).save(any());
        verifyNoInteractions(playerAchievementMapper, notificationMaker);
    }

    @Test
    void skipsUserWithoutPlayerLinkedToCurrentTeam() {
        assertFalse(service.awardForSuccessfulQuestion(null, appTeam));

        verifyNoInteractions(
                playerRepository,
                achievementRepository,
                playerAchievementRepository,
                eligibilityService,
                playerAchievementMapper,
                notificationMaker
        );
    }
}
