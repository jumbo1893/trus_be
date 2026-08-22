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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static com.jumbo.trus.service.achievement.AchievementCodes.TRUSBOT;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrusBotAchievementService {

    private static final String DETAIL = "První dotaz položený TrusBotovi.";

    private final PlayerRepository playerRepository;
    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final AchievementEligibilityService achievementEligibilityService;
    private final PlayerAchievementMapper playerAchievementMapper;
    private final AchievementNotificationMaker achievementNotificationMaker;

    /**
     * Udělí achievement za první úspěšně zodpovězený dotaz. Metoda je
     * idempotentní, takže další dotazy již achievement ani notifikaci neopakují.
     */
    @Transactional
    public boolean awardForSuccessfulQuestion(Long playerId, AppTeamEntity appTeam) {
        if (playerId == null) {
            log.debug("Skipping TrusBot achievement because current user has no linked player");
            return false;
        }

        AchievementEntity achievement = achievementRepository.findByCode(TRUSBOT)
                .orElseThrow(() -> new IllegalStateException(
                        "V databázi chybí achievement s kódem " + TRUSBOT
                ));
        PlayerEntity player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Hráč pro udělení TrusBot achievementu nebyl nalezen: " + playerId
                ));

        if (!achievementEligibilityService.canHaveAchievement(player, achievement)) {
            return false;
        }

        PlayerAchievementEntity playerAchievement = playerAchievementRepository
                .findByPlayerIdAndAchievementCodeForUpdate(playerId, TRUSBOT)
                .orElseGet(() -> new PlayerAchievementEntity(
                        achievement,
                        player,
                        false,
                        null
                ));

        if (Boolean.TRUE.equals(playerAchievement.getAccomplished())) {
            return false;
        }

        playerAchievement.setAccomplished(true);
        playerAchievement.setAccomplishedDate(new Date());
        playerAchievement.setDetail(DETAIL);

        PlayerAchievementEntity saved = playerAchievementRepository.save(playerAchievement);
        PlayerAchievementDTO savedDto = playerAchievementMapper.toDTO(saved);
        achievementNotificationMaker.sendAchievementNotify(savedDto, appTeam);
        return true;
    }
}
