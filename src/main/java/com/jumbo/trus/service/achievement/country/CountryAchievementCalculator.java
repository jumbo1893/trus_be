package com.jumbo.trus.service.achievement.country;

import com.jumbo.trus.dto.VisitedCountryResponse;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.achievement.helper.AchievementEligibilityService;
import com.jumbo.trus.service.achievement.rule.CountryAchievementRule;
import com.jumbo.trus.service.notification.push.maker.AchievementNotificationMaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jumbo.trus.service.achievement.AchievementCodes.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CountryAchievementCalculator {

    private static final List<CountryAchievementRule> RULES = List.of(
            new CountryAchievementRule(
                    ZAHRANICNI_POZOROVATEL,
                    VisitedCountryResponse::isForeignCountry,
                    country -> "Hráč splnil výzvu a vydal na zahraniční cestu do země "
                            + country.nameCs()
                            + "."
            ),
            new CountryAchievementRule(
                    DO_AFRIKY_NA_CERNOSKY,
                    country -> country.isInContinent("AF"),
                    country -> "V zimě vás všechny zvu. Splněno v zemi "
                            + country.nameCs()
                            + "."
            ),
            new CountryAchievementRule(
                    HEDVABNA_STEZKA,
                    country -> country.isInContinent("AS"),
                    country -> "Rejžožrouti, šikmoočka či pouštní negři. Aspoň něco z toho se dá nalézt v zemi "
                            + country.nameCs()
                            + ", kde se hráč poprvé z Asie připojil."
            ),
            new CountryAchievementRule(
                    AMERICAN_Z_VYSOCAN,
                    country -> country.isInContinent("NA"),
                    country -> "Kdo umí zahrát gé-á-há-cé, od přírody touží aspoň jednou podívat se za pořádnou louži. První Trusí zastávka v Americe hráče byla v zemi "
                            + country.nameCs()
                            + "."
            ),
            new CountryAchievementRule(
                    PO_STOPACH_DIEGA,
                    country -> country.isInContinent("SA"),
                    country -> "Pokus nabrat fyzičku? První zastávku v latinské americe si hráč udělal v zemi "
                            + country.nameCs()
                            + "."
            ),
            new CountryAchievementRule(
                    TRUSI_AMUNDSEN,
                    country -> country.isInContinent("AN"),
                    country -> "Tady se dá dost pochybovat, že to nebylo nafingované VPN. Pokud ne, tak gratulujeme k návštěvě země "
                            + country.nameCs()
                            + "."
            ),
            new CountryAchievementRule(
                    LISAK_A_MORE,
                    country -> country.isInContinent("OC"),
                    country -> "Díky Trusu hráč rozšířil vědomí o tom, že součást Oceánie je země "
                            + country.nameCs()
                            + ", kde udělal první zastávku v rámci Trusí appky."
            )
    );

    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerMapper playerMapper;
    private final AchievementNotificationMaker achievementNotificationMaker;
    private final PlayerAchievementMapper playerAchievementMapper;
    private final AchievementEligibilityService achievementEligibilityService;

    @Transactional
    public void calculateCountryAchievementsByPlayer(
            PlayerDTO player,
            VisitedCountryResponse visitedCountry,
            AppTeamEntity appTeam
    ) {
        if (player == null || visitedCountry == null) {
            return;
        }

        /*
         * Načítáme všechny country achievementy, nejen právě splněné.
         * Díky tomu vytvoříme chybějící PlayerAchievementEntity
         * i s accomplished = false.
         */
        Map<String, AchievementEntity> achievementsByCode =
                loadAchievementsByCode(RULES);

        PlayerEntity playerEntity = playerMapper.toEntity(player);

        List<CountryAchievementRule> eligibleRules =
                filterEligibleRules(
                        RULES,
                        achievementsByCode,
                        playerEntity
                );

        if (eligibleRules.isEmpty()) {
            return;
        }

        Collection<AchievementEntity> eligibleAchievements =
                eligibleRules.stream()
                        .map(CountryAchievementRule::achievementCode)
                        .map(achievementsByCode::get)
                        .toList();

        Map<Long, PlayerAchievementEntity>
                playerAchievementsByAchievementId =
                loadPlayerAchievements(
                        player.getId(),
                        eligibleAchievements
                );

        Date now = new Date();

        List<PlayerAchievementEntity> achievementsToSave =
                new ArrayList<>();

        List<PlayerAchievementEntity> newlyAccomplishedAchievements =
                new ArrayList<>();

        for (CountryAchievementRule rule : eligibleRules) {
            AchievementEntity achievement =
                    achievementsByCode.get(rule.achievementCode());

            PlayerAchievementEntity playerAchievement =
                    playerAchievementsByAchievementId.get(
                            achievement.getId()
                    );

            /*
             * Záznam dosud neexistuje:
             * vytvoříme ho nejprve jako nesplněný.
             */
            if (playerAchievement == null) {
                playerAchievement = createPlayerAchievement(
                        playerEntity,
                        achievement,
                        now
                );

                playerAchievementsByAchievementId.put(
                        achievement.getId(),
                        playerAchievement
                );

                achievementsToSave.add(playerAchievement);
            }

            /*
             * Už splněný achievement znovu neměníme
             * a neposíláme další notifikaci.
             */
            if (Boolean.TRUE.equals(
                    playerAchievement.getAccomplished()
            )) {
                continue;
            }

            /*
             * Pravidlo není splněné:
             * nový záznam zůstane accomplished = false.
             */
            if (!rule.isSatisfiedBy(visitedCountry)) {
                continue;
            }

            accomplishAchievement(
                    playerAchievement,
                    rule.createDetail(visitedCountry),
                    now
            );

            /*
             * Pokud je entita nová, už v achievementsToSave je.
             * Pokud existovala, musíme ji do seznamu přidat.
             */
            if (!achievementsToSave.contains(playerAchievement)) {
                achievementsToSave.add(playerAchievement);
            }

            newlyAccomplishedAchievements.add(
                    playerAchievement
            );
        }

        if (achievementsToSave.isEmpty()) {
            return;
        }

        List<PlayerAchievementEntity> savedAchievements =
                playerAchievementRepository.saveAll(
                        achievementsToSave
                );

        /*
         * Notifikaci posíláme pouze za achievementy,
         * které byly během tohoto výpočtu nově splněné.
         */
        if (newlyAccomplishedAchievements.isEmpty()) {
            return;
        }

        Set<Long> newlyAccomplishedAchievementIds =
                newlyAccomplishedAchievements.stream()
                        .map(entity ->
                                entity.getAchievement().getId()
                        )
                        .collect(Collectors.toSet());

        List<PlayerAchievementDTO> accomplishedDTOs =
                savedAchievements.stream()
                        .filter(entity ->
                                newlyAccomplishedAchievementIds.contains(
                                        entity.getAchievement().getId()
                                )
                        )
                        .map(playerAchievementMapper::toDTO)
                        .toList();

        if (!accomplishedDTOs.isEmpty()) {
            achievementNotificationMaker.sendAchievementNotify(
                    accomplishedDTOs,
                    appTeam
            );
        }
    }
    private List<CountryAchievementRule> filterEligibleRules(
            List<CountryAchievementRule> satisfiedRules,
            Map<String, AchievementEntity> achievementsByCode,
            PlayerEntity player
    ) {
        return satisfiedRules.stream()
                .filter(rule -> {
                    AchievementEntity achievement =
                            achievementsByCode.get(
                                    rule.achievementCode()
                            );

                    boolean eligible =
                            achievementEligibilityService
                                    .canHaveAchievement(
                                            player,
                                            achievement
                                    );

                    if (!eligible) {
                        log.debug(
                                "Player id={} is not eligible for achievement code={}",
                                player.getId(),
                                achievement.getCode()
                        );
                    }

                    return eligible;
                })
                .toList();
    }

    private Map<String, AchievementEntity> loadAchievementsByCode(
            List<CountryAchievementRule> rules
    ) {
        Set<String> achievementCodes = rules.stream()
                .map(CountryAchievementRule::achievementCode)
                .collect(Collectors.toSet());

        Map<String, AchievementEntity> achievementsByCode =
                achievementRepository
                        .findAllByCodeIn(achievementCodes)
                        .stream()
                        .collect(Collectors.toMap(
                                AchievementEntity::getCode,
                                Function.identity()
                        ));

        Set<String> missingCodes = achievementCodes.stream()
                .filter(code ->
                        !achievementsByCode.containsKey(code)
                )
                .collect(Collectors.toSet());

        if (!missingCodes.isEmpty()) {
            throw new IllegalStateException(
                    "V databázi chybí achievementy s kódy: "
                            + missingCodes
            );
        }

        return achievementsByCode;
    }

    private Map<Long, PlayerAchievementEntity> loadPlayerAchievements(
            Long playerId,
            Collection<AchievementEntity> achievements
    ) {
        List<Long> achievementIds = achievements.stream()
                .map(AchievementEntity::getId)
                .distinct()
                .toList();

        if (achievementIds.isEmpty()) {
            return new HashMap<>();
        }

        return playerAchievementRepository
                .findAllByPlayerIdAndAchievementIdIn(
                        playerId,
                        achievementIds
                )
                .stream()
                .collect(Collectors.toMap(
                        playerAchievement ->
                                playerAchievement
                                        .getAchievement()
                                        .getId(),
                        Function.identity()
                ));
    }

    private PlayerAchievementEntity createPlayerAchievement(
            PlayerEntity player,
            AchievementEntity achievement,
            Date createdAt
    ) {
        return new PlayerAchievementEntity(
                achievement,
                player,
                false,
                createdAt
        );
    }

    private void accomplishAchievement(
            PlayerAchievementEntity playerAchievement,
            String detail,
            Date accomplishedDate
    ) {
        playerAchievement.setAccomplished(true);
        playerAchievement.setAccomplishedDate(
                accomplishedDate
        );
        playerAchievement.setDetail(detail);
    }
}