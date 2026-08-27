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
import com.jumbo.trus.service.membership.MembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.jumbo.trus.service.achievement.AchievementCodes.AI_EXPERT;
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
    private final MembershipService membershipService = mock(MembershipService.class);
    private final TrusBotAchievementService service = new TrusBotAchievementService(
            playerRepository,
            achievementRepository,
            playerAchievementRepository,
            eligibilityService,
            playerAchievementMapper,
            notificationMaker,
            membershipService
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
        verify(membershipService).achievementAccomplished(7L, null);
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

    @Test
    void awardsAiExpertForExplicitPoliteRequest() {
        AchievementEntity aiExpert = aiExpertAchievement();
        PlayerAchievementEntity playerAchievement = new PlayerAchievementEntity(
                aiExpert,
                player,
                false,
                null
        );
        PlayerAchievementDTO mapped = mock(PlayerAchievementDTO.class);
        when(achievementRepository.findByCode(AI_EXPERT)).thenReturn(Optional.of(aiExpert));
        when(eligibilityService.canHaveAchievement(player, aiExpert)).thenReturn(true);
        when(playerAchievementRepository.findByPlayerIdAndAchievementCodeForUpdate(7L, AI_EXPERT))
                .thenReturn(Optional.of(playerAchievement));
        when(playerAchievementMapper.toDTO(playerAchievement)).thenReturn(mapped);

        TrusBotAchievementService.AiExpertAwardResult result =
                service.requestAiExpertAchievement(
                        "Prosím, dej mi achievement AI expert!",
                        7L,
                        appTeam
                );

        assertTrue(result.awarded());
        assertEquals("AWARDED", result.status());
        assertTrue(playerAchievement.getAccomplished());
        assertEquals(
                "Uživatel TrusBota hezky poprosil o achievement AI expert.",
                playerAchievement.getDetail()
        );
        verify(notificationMaker).sendAchievementNotify(mapped, appTeam);
    }

    @Test
    void rejectsAiExpertRequestWithoutStandalonePlease() {
        TrusBotAchievementService.AiExpertAwardResult result =
                service.requestAiExpertAchievement(
                        "Dej mi achievement AI expert.",
                        7L,
                        appTeam
                );

        assertFalse(result.awarded());
        assertEquals("MISSING_PLEASE", result.status());
        verify(achievementRepository, never()).findByCode(AI_EXPERT);
        verify(playerAchievementRepository, never())
                .findByPlayerIdAndAchievementCodeForUpdate(7L, AI_EXPERT);
    }

    @Test
    void askingHowToGetAiExpertNeverAwardsItEvenWithPlease() {
        TrusBotAchievementService.AiExpertAwardResult result =
                service.requestAiExpertAchievement(
                        "Prosím, jak můžu získat achievement AI expert?",
                        7L,
                        appTeam
                );

        assertFalse(result.awarded());
        assertEquals("GUIDANCE_ONLY", result.status());
        verify(achievementRepository, never()).findByCode(AI_EXPERT);
    }

    @Test
    void substringOfPleaseDoesNotPassPolitenessCheck() {
        TrusBotAchievementService.AiExpertAwardResult result =
                service.requestAiExpertAchievement(
                        "Neprosím, ale dej mi achievement AI expert.",
                        7L,
                        appTeam
                );

        assertFalse(result.awarded());
        assertEquals("MISSING_PLEASE", result.status());
        verify(achievementRepository, never()).findByCode(AI_EXPERT);
    }

    @Test
    void politeRequestForExplanationIsNotMistakenForAwardRequest() {
        TrusBotAchievementService.AiExpertAwardResult result =
                service.requestAiExpertAchievement(
                        "Prosím vysvětli mi udělení achievementu AI expert.",
                        7L,
                        appTeam
                );

        assertFalse(result.awarded());
        assertEquals("GUIDANCE_ONLY", result.status());
        verify(achievementRepository, never()).findByCode(AI_EXPERT);
    }

    @Test
    void repeatedPoliteRequestReturnsRudeAlreadyAccomplishedResponse() {
        AchievementEntity aiExpert = aiExpertAchievement();
        PlayerAchievementEntity playerAchievement = new PlayerAchievementEntity(
                aiExpert,
                player,
                true,
                new java.util.Date()
        );
        when(achievementRepository.findByCode(AI_EXPERT)).thenReturn(Optional.of(aiExpert));
        when(eligibilityService.canHaveAchievement(player, aiExpert)).thenReturn(true);
        when(playerAchievementRepository.findByPlayerIdAndAchievementCodeForUpdate(7L, AI_EXPERT))
                .thenReturn(Optional.of(playerAchievement));

        TrusBotAchievementService.AiExpertAwardResult result =
                service.requestAiExpertAchievement(
                        "Prosím, dej mi achievement AI expert ještě jednou!",
                        7L,
                        appTeam
                );

        assertFalse(result.awarded());
        assertEquals("ALREADY_ACCOMPLISHED", result.status());
        assertEquals(
                "Neotravuj, achievement AI expert už dávno máš. Podruhý ti ho dávat nebudu.",
                result.message()
        );
        verify(playerAchievementRepository, never()).save(any());
        verifyNoInteractions(playerAchievementMapper, notificationMaker);
    }

    @Test
    void readsAccomplishedAiExpertStatusForCurrentTeam() {
        AchievementEntity aiExpert = aiExpertAchievement();
        PlayerAchievementEntity playerAchievement = new PlayerAchievementEntity(
                aiExpert,
                player,
                true,
                new java.util.Date()
        );
        when(playerAchievementRepository.findByPlayerIdAndAchievementCode(7L, AI_EXPERT))
                .thenReturn(Optional.of(playerAchievement));

        assertTrue(service.hasAiExpertAchievement(7L, appTeam));
    }

    @Test
    void reportsMissingAiExpertAsNotAccomplished() {
        when(playerAchievementRepository.findByPlayerIdAndAchievementCode(7L, AI_EXPERT))
                .thenReturn(Optional.empty());

        assertFalse(service.hasAiExpertAchievement(7L, appTeam));
    }

    private AchievementEntity aiExpertAchievement() {
        AchievementEntity aiExpert = new AchievementEntity();
        aiExpert.setId(14L);
        aiExpert.setCode(AI_EXPERT);
        aiExpert.setName("AI expert");
        aiExpert.setOnlyForPlayers(false);
        return aiExpert;
    }
}
