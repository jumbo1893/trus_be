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

import java.text.Normalizer;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static com.jumbo.trus.service.achievement.AchievementCodes.AI_EXPERT;
import static com.jumbo.trus.service.achievement.AchievementCodes.TRUSBOT;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrusBotAchievementService {

    private static final String TRUSBOT_DETAIL = "První dotaz položený TrusBotovi.";
    private static final String AI_EXPERT_DETAIL = "Uživatel TrusBota hezky poprosil o achievement AI expert.";

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
        return award(playerId, appTeam, TRUSBOT, TRUSBOT_DETAIL) == AwardOutcome.AWARDED;
    }

    @Transactional(readOnly = true)
    public boolean hasAiExpertAchievement(Long playerId, AppTeamEntity appTeam) {
        if (playerId == null || appTeam == null) {
            return false;
        }
        return playerAchievementRepository
                .findByPlayerIdAndAchievementCode(playerId, AI_EXPERT)
                .filter(playerAchievement -> playerAchievement.getPlayer() != null)
                .filter(playerAchievement -> playerAchievement.getPlayer().getAppTeam() != null)
                .filter(playerAchievement -> Objects.equals(
                        playerAchievement.getPlayer().getAppTeam().getId(),
                        appTeam.getId()
                ))
                .map(PlayerAchievementEntity::getAccomplished)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    /**
     * Jediná zapisovací akce dostupná AI. Původní uživatelský dotaz se ověřuje
     * na backendu, takže model nemůže podmínku obejít chybným nebo podvrženým
     * voláním nástroje.
     */
    @Transactional
    public AiExpertAwardResult requestAiExpertAchievement(
            String originalQuestion,
            Long playerId,
            AppTeamEntity appTeam
    ) {
        String normalizedQuestion = normalize(originalQuestion);
        if (!containsWord(normalizedQuestion, "prosim")) {
            return new AiExpertAwardResult(
                    false,
                    "MISSING_PLEASE",
                    "Achievement nebyl udělen. Uživatel musí použít samostatné slovo prosím."
            );
        }
        if (!normalizedQuestion.contains("ai expert")) {
            return new AiExpertAwardResult(
                    false,
                    "MISSING_ACHIEVEMENT_NAME",
                    "Achievement nebyl udělen. Dotaz musí výslovně uvést AI expert."
            );
        }
        if (isGuidanceQuestion(normalizedQuestion)) {
            return new AiExpertAwardResult(
                    false,
                    "GUIDANCE_ONLY",
                    "Uživatel se pouze ptá na způsob získání. Řekni mu, že musí TrusBota hezky poprosit."
            );
        }
        if (!hasExplicitAwardIntent(normalizedQuestion)) {
            return new AiExpertAwardResult(
                    false,
                    "NOT_EXPLICIT_REQUEST",
                    "Achievement nebyl udělen. Uživatel musí výslovně požádat o jeho udělení."
            );
        }

        AwardOutcome outcome = award(playerId, appTeam, AI_EXPERT, AI_EXPERT_DETAIL);
        return switch (outcome) {
            case AWARDED -> new AiExpertAwardResult(
                    true,
                    "AWARDED",
                    "Achievement AI expert byl právě udělen."
            );
            case ALREADY_ACCOMPLISHED -> new AiExpertAwardResult(
                    false,
                    "ALREADY_ACCOMPLISHED",
                    "Neotravuj, achievement AI expert už dávno máš. Podruhý ti ho dávat nebudu."
            );
            case UNAVAILABLE -> new AiExpertAwardResult(
                    false,
                    "UNAVAILABLE",
                    "Achievement nelze tomuto účtu udělit."
            );
        };
    }

    private AwardOutcome award(
            Long playerId,
            AppTeamEntity appTeam,
            String achievementCode,
            String detail
    ) {
        if (playerId == null) {
            log.debug("Skipping achievement {} because current user has no linked player", achievementCode);
            return AwardOutcome.UNAVAILABLE;
        }

        AchievementEntity achievement = achievementRepository.findByCode(achievementCode)
                .orElseThrow(() -> new IllegalStateException(
                        "V databázi chybí achievement s kódem " + achievementCode
                ));
        PlayerEntity player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Hráč pro udělení achievementu nebyl nalezen: " + playerId
                ));

        if (appTeam == null
                || player.getAppTeam() == null
                || !Objects.equals(player.getAppTeam().getId(), appTeam.getId())) {
            log.warn(
                    "Skipping achievement {} because player does not belong to current team. playerId={}, appTeamId={}",
                    achievementCode,
                    playerId,
                    appTeam == null ? null : appTeam.getId()
            );
            return AwardOutcome.UNAVAILABLE;
        }

        if (!achievementEligibilityService.canHaveAchievement(player, achievement)) {
            return AwardOutcome.UNAVAILABLE;
        }

        PlayerAchievementEntity playerAchievement = playerAchievementRepository
                .findByPlayerIdAndAchievementCodeForUpdate(playerId, achievementCode)
                .orElseGet(() -> new PlayerAchievementEntity(
                        achievement,
                        player,
                        false,
                        null
                ));

        if (Boolean.TRUE.equals(playerAchievement.getAccomplished())) {
            return AwardOutcome.ALREADY_ACCOMPLISHED;
        }

        playerAchievement.setAccomplished(true);
        playerAchievement.setAccomplishedDate(new Date());
        playerAchievement.setDetail(detail);

        PlayerAchievementEntity saved = playerAchievementRepository.save(playerAchievement);
        PlayerAchievementDTO savedDto = playerAchievementMapper.toDTO(saved);
        achievementNotificationMaker.sendAchievementNotify(savedDto, appTeam);
        return AwardOutcome.AWARDED;
    }

    private boolean isGuidanceQuestion(String normalizedQuestion) {
        return containsWord(normalizedQuestion, "jak")
                || normalizedQuestion.contains("co musim")
                || normalizedQuestion.contains("co mam")
                || normalizedQuestion.contains("zpusob ziskani")
                || normalizedQuestion.contains("podmink")
                || normalizedQuestion.contains("napoved")
                || normalizedQuestion.contains("vysvetl")
                || containsWord(normalizedQuestion, "porad");
    }

    private boolean hasExplicitAwardIntent(String normalizedQuestion) {
        return containsWord(normalizedQuestion, "dej")
                || containsWord(normalizedQuestion, "dat")
                || containsWord(normalizedQuestion, "chci")
                || containsWord(normalizedQuestion, "udelej")
                || containsWord(normalizedQuestion, "udelte")
                || containsWord(normalizedQuestion, "udelis")
                || containsWord(normalizedQuestion, "udelit")
                || containsWord(normalizedQuestion, "pridel")
                || containsWord(normalizedQuestion, "pridelis")
                || containsWord(normalizedQuestion, "pridelit")
                || normalizedQuestion.contains("prosim o achievement ai expert")
                || normalizedQuestion.contains("prosim o ai expert");
    }

    private boolean containsWord(String normalizedText, String word) {
        return (" " + normalizedText + " ").contains(" " + word + " ");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    public record AiExpertAwardResult(
            boolean awarded,
            String status,
            String message
    ) {
    }

    private enum AwardOutcome {
        AWARDED,
        ALREADY_ACCOMPLISHED,
        UNAVAILABLE
    }
}
