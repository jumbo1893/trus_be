package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.beer.BeerDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.FootballMatchPlayerDTO;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.goal.GoalDTO;
import com.jumbo.trus.dto.goal.response.get.GoalDetailedResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.receivedfine.ReceivedFineDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.achievement.PlayerAchievementEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.filter.ReceivedFineFilter;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.GoalService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.achievement.helper.*;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.notification.push.maker.AchievementNotificationMaker;
import com.jumbo.trus.service.order.OrderMatchByDate;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementCalculator {

    private final BeerService beerService;
    private final AchievementMapper achievementMapper;
    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerAchievementMapper playerAchievementMapper;
    private final FootballMatchService footballMatchService;
    private final MatchService matchService;
    private final SeasonService seasonService;
    private final FootballPlayerStatsService footballPlayerStatsService;
    private final ReceivedFineService receivedFineService;
    private final FineService fineService;
    private final GoalService goalService;
    private final AchievementNotificationMaker achievementNotificationMaker;
    private final Map<String, AchievementFunction> achievementCalculators =
            Map.<String, AchievementFunction>ofEntries(
                    Map.entry("KAZDEMU_CO_MU_PATRI", (p, a, at, t) -> calculateKAZDEMU_CO_MU_PATRIAchievement(p, a, t)),
                    Map.entry("FOTBAL_JE_JEN_ZAMINKA", this::calculateFOTBAL_JE_JEN_ZAMINKAAchievement),
                    Map.entry("PO_PORADNE_PRACI_PORADNA_OSLAVA", (p, a, at, t) -> calculatePO_PORADNE_PRACI_PORADNA_OSLAVAAchievement(p, a, t)),
                    Map.entry("TAHOUN", this::calculateTAHOUNAAchievement),
                    Map.entry("KORALA", (p, a, at, t) -> calculateKORALAAchievement(p, a, t)),
                    Map.entry("MECENAS", this::calculateMECENASAchievement),
                    Map.entry("OSLAVENEC", this::calculateOSLAVENECAchievement),
                    Map.entry("USPESNY_DEN", (p, a, at, t) -> calculateUSPESNY_DENAchievement(p, a, t)),
                    Map.entry("CERNA_PRACE", (p, a, at, t) -> calculateCERNA_PRACEAchievement(p, a, t)),
                    //Achievement(p, a)Calculators.put("REZNIK", (p, a, t) -> calculateHattrickHero);
                    Map.entry("DOPING", (p, a, at, t) -> calculateDOPINGAchievement(p, a, t)),
                    Map.entry("AUTICKO", this::calculateAUTICKOAchievement),
                    Map.entry("OZEN_SE_OZER_SE", (p, a, at, t) -> calculateOZEN_SE_OZER_SEAchievement(p, a, t)),
                    Map.entry("ROSS_GELLER", (p, a, at, t) -> calculateROSS_GELLERAchievement(p, a, t)),
                    Map.entry("CESTNY_JAKO_KAREL_ERBEN", (p, a, at, t) -> calculateCESTNY_JAKO_KAREL_ERBENAchievement(p, a, t)),
                    Map.entry("ZASTRELOVANI", (p, a, at, t) -> calculateZASTRELOVANIAchievement(p, a, t)),
                    Map.entry("SOBEC", this::calculateSOBECAchievement),
                    Map.entry("NESOBEC", this::calculateNESOBECAchievement),
                    Map.entry("JEN_NA_SKOK", (p, a, at, t) -> calculateJEN_NA_SKOKAchievement(p, a, t)),
                    Map.entry("HVEZDNE_MANYRY", (p, a, at, t) -> calculateHVEZDNE_MANYRYAchievement(p, a, t)),
                    Map.entry("MIREK_DUSIN", this::calculateMIREK_DUSINAchievement),
                    Map.entry("KONZISTENCE", this::calculateKONZISTENCEAchievement),
                    Map.entry("DAVID_BECKHAM", (p, a, at, t) -> calculateDAVID_BECKHAMAchievement(p, a, t)),
                    Map.entry("DLOUHA_NOC", (p, a, at, t) -> calculateDLOUHA_NOCAchievement(p, a, t)),
                    Map.entry("ZBYTECNE_PRASE", this::calculateZBYTECNE_PRASEAchievement),
                    Map.entry("DEN_BLBEC", this::calculateDEN_BLBECAchievement),
                    Map.entry("POROUCHANY_BUDIK", this::calculatePOROUCHANY_BUDIKAchievement),
                    Map.entry("ZLUTY_HNEDY_POPLACH", (p, a, at, t) -> calculateZLUTY_HNEDY_POPLACHAchievement(p, a, t)),
                    Map.entry("SBERATEL", (p, a, at, t) -> calculateSBERATELAchievement(p, a, t)),
                    Map.entry("MEDMRDKA", this::calculateMEDMRDKAAchievement),
                    Map.entry("NAROD_SE", (p, a, at, t) -> calculateNAROD_SEAchievement(p, a, t)),
                    Map.entry("PRIORITY", this::calculatePRIORITYAchievement),
                    Map.entry("ZLUTA_JE_DOBRA", this::calculateZLUTA_JE_DOBRAAchievement),
                    Map.entry("IONTAK", (p, a, at, t) -> calculateIONTAKAchievement(p, a, t)),
                    Map.entry("SPORTOVEC", this::calculateSPORTOVECAchievement),
                    Map.entry("PROC", (p, a, at, t) -> calculatePROCAchievement(p, a, t)),
                    Map.entry("HLADINKA", (p, a, at, t) -> calculateHLADINKAAchievement(p, a, t)),
                    Map.entry("STENE", this::calculateSTENEAchievement),
                    Map.entry("CIRHOZA", (p, a, at, t) -> calculateCIRHOZAAchievement(p, a, t)),
                    Map.entry("TEN_TO_PERFEKTNE_KOPE", (p, a, at, t) -> calculateTEN_TO_PERFEKTNE_KOPEAchievement(p, a, t)),
                    Map.entry("ADA_VETVICKA", (p, a, at, t) -> calculateADA_VETVICKAAchievement(p, a, t)),
                    Map.entry("KLUB_SRACU", this::calculateKLUB_SRACUAchievement),
                    Map.entry("OSAMELY_DRZAK", this::calculateOSAMELY_DRZAKAchievement),
                    Map.entry("VE_DVOU_SE_TO_LEPE_TAHNE", this::calculateVE_DVOU_SE_TO_LEPE_TAHNEAchievement),
                    Map.entry("STRELEC", this::calculateSTRELECAchievement),
                    Map.entry("FOTR_JE_LOTR", this::calculateFOTR_JE_LOTRAchievement),
                    Map.entry("MARATONEC", this::calculateMARATONECAchievement),
                    Map.entry("ROBERTO_CARLOS", this::calculateROBERTO_CARLOSAchievement),
                    Map.entry("SPILMACHR", this::calculateSPILMACHRAchievement),
                    Map.entry("JA_TO_ZA_VAS_OBEHAL", this::calculateJA_TO_ZA_VAS_OBEHALAchievement),
                    Map.entry("DOPLNENI_TEKUTIN", this::calculateDOPLNENI_TEKUTINAchievement),
                    Map.entry("NASTUP_JAKO_HROM", this::calculateNASTUP_JAKO_HROMAchievement),
                    Map.entry("KDYZ_LEJU_TAK_PORADNE", this::calculateKDYZ_LEJU_TAK_PORADNEAchievement),
                    Map.entry("MACHYREK", this::calculateMACHYREKAchievement),
                    Map.entry("SDILENY_STRELEC", this::calculateSDILENY_STRELECAchievement),
                    Map.entry("NESOBECKY_HRDINA", this::calculateNESOBECKY_HRDINAAchievement),
                    Map.entry("GOLY_NE_RADEJI_PIVO", this::calculateGOLY_NE_RADEJI_PIVOAchievement),
                    Map.entry("JARDA_KUZEL", this::calculateJARDA_KUZELAchievement),
                    Map.entry("MODERNI_GOLMANSKA_SKOLA", this::calculateMODERNI_GOLMANSKA_SKOLAAchievement),
                    Map.entry("MORALNI_PODPORA", this::calculateMORALNI_PODPORAAchievement),
                    Map.entry("LAZAR_NA_TRIBUNACH", this::calculateLAZAR_NA_TRIBUNACHAchievement),
                    Map.entry("JEDNOU_SE_ZACIT_MUSI", (p, a, at, t) -> calculateDrinkMilestoneAchievement(p, a, at, t, 1, 0)),
                    Map.entry("KDYZ_ONO_TO_CHUTNA", (p, a, at, t) -> calculateDrinkMilestoneAchievement(p, a, at, t, 50, 0)),
                    Map.entry("SOUDEK", (p, a, at, t) -> calculateDrinkMilestoneAchievement(p, a, at, t, 100, 0)),
                    Map.entry("CISTERNA", (p, a, at, t) -> calculateDrinkMilestoneAchievement(p, a, at, t, 500, 0)),
                    Map.entry("PRITVRDIME", (p, a, at, t) -> calculateDrinkMilestoneAchievement(p, a, at, t, 0, 1)),
                    Map.entry("RUMOVY_NADENIK", (p, a, at, t) -> calculateDrinkMilestoneAchievement(p, a, at, t, 0, 20)),
                    Map.entry("ACHIEVEMENT_MILANA_CURDY", (p, a, at, t) -> calculateDrinkMilestoneAchievement(p, a, at, t, 0, 50)),
                    Map.entry("HVEZDA_CO_SE_NEZDA", this::calculateHVEZDA_CO_SE_NEZDAAchievement),
                    Map.entry("KOMPLEXNI_HRAC", this::calculateKOMPLEXNI_HRACAchievement),
                    Map.entry("ULTRUS", (p, a, at, t) -> calculateFanAttendanceMilestoneAchievement(p, a, at, t, 30)),
                    Map.entry("PERMICE_NA_TRUS", (p, a, at, t) -> calculateFanAttendanceMilestoneAchievement(p, a, at, t, 10)),
                    Map.entry("DO_POCTU", this::calculateDO_POCTUAchievement),
                    Map.entry("HATTRICK_GORDIEHO_HOWA", this::calculateHATTRICK_GORDIEHO_HOWAAchievement),
                    Map.entry("AMERICKY_FOTBALISTA", (p, a, at, t) -> calculateFineMilestoneAchievement(p, a, at, t, List.of("Překop"), 10)),
                    Map.entry("ALZHEIMER", (p, a, at, t) -> calculateFineMilestoneAchievement(p, a, at, t, List.of("Zapomenutí věcí", "Nekompletní výbava"), 1)),
                    Map.entry("LEO_BERANEK", (p, a, at, t) -> calculateFineMilestoneAchievement(p, a, at, t, List.of("Nové kopačky"), 1)),
                    Map.entry("CERNE_GENY", this::calculateCERNE_GENYAchievement)


            );


    public void calculateAllAchievements(List<PlayerDTO> playerList, AppTeamEntity appTeam, AchievementType achievementType) {
        long totalStart = System.nanoTime();
        List<AchievementDTO> achievements = achievementRepository.findAll().stream().map(achievementMapper::toDTO).toList();
        Map<PlayerAchievementKey, PlayerAchievementDTO> existingAchievements = loadExistingAchievements(playerList);
        Map<String, AchievementCalculationStats> statsByAchievement = new LinkedHashMap<>();
        AchievementCalculationSummary summary = new AchievementCalculationSummary();

        log.info("Achievement calculation started. appTeamId={}, type={}, players={}, achievements={}, existingPlayerAchievements={}",
                appTeam.getId(), achievementType, playerList.size(), achievements.size(), existingAchievements.size());

        for (PlayerDTO player : playerList) {
            long playerStart = System.nanoTime();
            AchievementCalculationSummary beforePlayer = summary.copy();

            calculateAndSaveAchievementForPlayer(
                    achievements,
                    player,
                    appTeam,
                    achievementType,
                    existingAchievements,
                    statsByAchievement,
                    summary
            );

            long playerMillis = nanosToMillis(System.nanoTime() - playerStart);
            if (playerMillis > 5_000) {
                AchievementCalculationSummary playerDiff = summary.minus(beforePlayer);
                log.warn("Slow achievement calculation for playerId={}, playerName='{}': {} ms, calculated={}, new={}, updated={}, unchanged={}, skippedNull={}",
                        player.getId(), player.getName(), playerMillis, playerDiff.calculated, playerDiff.created,
                        playerDiff.updated, playerDiff.unchanged, playerDiff.skippedNull);
            } else {
                log.debug("Achievement calculation for playerId={}, playerName='{}' finished in {} ms",
                        player.getId(), player.getName(), playerMillis);
            }
        }

        logAchievementCalculationStats(appTeam, achievementType, playerList.size(), achievements.size(), summary,
                statsByAchievement, System.nanoTime() - totalStart);
    }

    private void calculateAndSaveAchievementForPlayer(
            List<AchievementDTO> achievements,
            PlayerDTO player,
            AppTeamEntity appTeam,
            AchievementType achievementType,
            Map<PlayerAchievementKey, PlayerAchievementDTO> existingAchievements,
            Map<String, AchievementCalculationStats> statsByAchievement,
            AchievementCalculationSummary summary
    ) {
        for (AchievementDTO achievement : achievements) {
            String achievementCode = achievement.getCode();
            AchievementCalculationStats stats = statsByAchievement.computeIfAbsent(achievementCode, AchievementCalculationStats::new);

            long calculationStart = System.nanoTime();
            PlayerAchievementDTO calculated = calculateAchievementForPlayer(achievement, player, appTeam, achievementType);
            long calculationNanos = System.nanoTime() - calculationStart;

            stats.recordCalculation(calculationNanos, player.getId(), player.getName());
            summary.calculated++;

            if (calculated == null) {
                stats.skippedNull++;
                summary.skippedNull++;
                continue;
            }

            if (Boolean.TRUE.equals(calculated.getAccomplished())) {
                stats.accomplished++;
                summary.accomplished++;
            } else {
                stats.failed++;
                summary.failed++;
            }

            PlayerAchievementDTO existing = existingAchievements.get(playerAchievementKey(calculated));

            if (existing == null) {
                PlayerAchievementDTO saved = saveNewAchievementToRepository(calculated, appTeam);
                existingAchievements.put(playerAchievementKey(saved), saved);
                stats.created++;
                summary.created++;
                continue;
            }

            if (isChanged(existing, calculated)) {
                PlayerAchievementDTO saved = updateExistingAchievement(existing, calculated, appTeam);
                existingAchievements.put(playerAchievementKey(saved), saved);
                stats.updated++;
                summary.updated++;
            } else {
                stats.unchanged++;
                summary.unchanged++;
            }
        }
    }

    private Map<PlayerAchievementKey, PlayerAchievementDTO> loadExistingAchievements(List<PlayerDTO> playerList) {
        List<Long> playerIds = playerList.stream()
                .map(PlayerDTO::getId)
                .filter(Objects::nonNull)
                .toList();

        if (playerIds.isEmpty()) {
            return new HashMap<>();
        }

        long start = System.nanoTime();
        Map<PlayerAchievementKey, PlayerAchievementDTO> result = new HashMap<>();
        playerAchievementRepository.findAllByPlayerIdIn(playerIds).stream()
                .map(playerAchievementMapper::toDTO)
                .forEach(playerAchievement -> result.put(playerAchievementKey(playerAchievement), playerAchievement));

        log.info("Loaded existing player achievements in {} ms. players={}, existingAchievements={}",
                nanosToMillis(System.nanoTime() - start), playerIds.size(), result.size());
        return result;
    }

    private PlayerAchievementKey playerAchievementKey(PlayerAchievementDTO playerAchievement) {
        return new PlayerAchievementKey(
                playerAchievement.getPlayer().getId(),
                playerAchievement.getAchievement().getId()
        );
    }

    private void logAchievementCalculationStats(
            AppTeamEntity appTeam,
            AchievementType achievementType,
            int playersCount,
            int achievementsCount,
            AchievementCalculationSummary summary,
            Map<String, AchievementCalculationStats> statsByAchievement,
            long totalNanos
    ) {
        long totalMillis = nanosToMillis(totalNanos);
        log.info("Achievement calculation finished. appTeamId={}, type={}, players={}, achievements={}, total={} ms, calculated={}, accomplished={}, failed={}, skippedNull={}, new={}, updated={}, unchanged={}",
                appTeam.getId(), achievementType, playersCount, achievementsCount, totalMillis, summary.calculated,
                summary.accomplished, summary.failed, summary.skippedNull, summary.created, summary.updated, summary.unchanged);

        statsByAchievement.values().stream()
                .sorted(Comparator.comparingLong(AchievementCalculationStats::getTotalNanos).reversed())
                .limit(20)
                .forEach(stats -> log.info("Achievement timing: code={}, total={} ms, avg={} ms, max={} ms, maxPlayerId={}, maxPlayerName='{}', calculated={}, accomplished={}, failed={}, skippedNull={}, new={}, updated={}, unchanged={}",
                        stats.code,
                        nanosToMillis(stats.totalNanos),
                        stats.calculated == 0 ? 0 : nanosToMillis(stats.totalNanos / stats.calculated),
                        nanosToMillis(stats.maxNanos),
                        stats.maxPlayerId,
                        stats.maxPlayerName,
                        stats.calculated,
                        stats.accomplished,
                        stats.failed,
                        stats.skippedNull,
                        stats.created,
                        stats.updated,
                        stats.unchanged));
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000;
    }

    private boolean isChanged(PlayerAchievementDTO existing, PlayerAchievementDTO calculated) {
        return !Objects.equals(existing.getAccomplished(), calculated.getAccomplished())
                || !sameId(existing.getMatch(), calculated.getMatch())
                || !sameId(existing.getFootballMatch(), calculated.getFootballMatch())
                || !Objects.equals(existing.getDetail(), calculated.getDetail());
    }


    private boolean sameId(MatchDTO a, MatchDTO b) {
        if (a == null || b == null) {
            return a == b;
        }
        return Objects.equals(a.getId(), b.getId());
    }

    private boolean sameId(FootballMatchDTO a, FootballMatchDTO b) {
        if (a == null || b == null) {
            return a == b;
        }
        return Objects.equals(a.getId(), b.getId());
    }

    public PlayerAchievementDTO updateExistingAchievement(
            PlayerAchievementDTO existing,
            PlayerAchievementDTO calculated,
            AppTeamEntity appTeam
    ) {
        calculated.setId(existing.getId());

        boolean wasAccomplished = Boolean.TRUE.equals(existing.getAccomplished());
        boolean isAccomplished = Boolean.TRUE.equals(calculated.getAccomplished());

        if (!wasAccomplished && isAccomplished) {
            calculated.setAccomplishedDate(new Date());

            PlayerAchievementEntity savedEntity =
                    playerAchievementRepository.save(playerAchievementMapper.toEntity(calculated));

            PlayerAchievementDTO savedDto = playerAchievementMapper.toDTO(savedEntity);

            achievementNotificationMaker.sendAchievementNotify(savedDto, appTeam);
            return savedDto;
        }

        if (wasAccomplished && isAccomplished) {
            calculated.setAccomplishedDate(existing.getAccomplishedDate());
            return playerAchievementMapper.toDTO(playerAchievementRepository.save(playerAchievementMapper.toEntity(calculated)));
        }

        calculated.setAccomplishedDate(null);
        return playerAchievementMapper.toDTO(playerAchievementRepository.save(playerAchievementMapper.toEntity(calculated)));
    }

    private PlayerAchievementDTO calculateAchievementForPlayer(
            AchievementDTO achievement,
            PlayerDTO player,
            AppTeamEntity appTeam,
            AchievementType achievementType
    ) {
        if (!isNeededToCalculateAchievementForPlayer(player, achievement)) {
            return null;
        }

        AchievementFunction calculator = achievementCalculators.get(achievement.getCode());

        if (calculator == null) {
            log.warn("Achievement '{}' s kódem '{}' nemá implementovaný kalkulátor. Přeskakuji výpočet.",
                    achievement.getName(),
                    achievement.getCode());

            return null;
        }

        return calculator.apply(player, achievement, appTeam, achievementType);
    }

    private PlayerAchievementDTO saveNewAchievementToRepository(PlayerAchievementDTO playerAchievement, AppTeamEntity appTeam) {
        if (Boolean.TRUE.equals(playerAchievement.getAccomplished())) {
            playerAchievement.setAccomplishedDate(new Date());
            PlayerAchievementEntity savedEntity =
                    playerAchievementRepository.save(playerAchievementMapper.toEntity(playerAchievement));
            PlayerAchievementDTO savedDto = playerAchievementMapper.toDTO(savedEntity);
            achievementNotificationMaker.sendAchievementNotify(savedDto, appTeam);
            return savedDto;
        }

        playerAchievement.setAccomplishedDate(null);
        return playerAchievementMapper.toDTO(playerAchievementRepository.save(playerAchievementMapper.toEntity(playerAchievement)));
    }

    /*public void saveAchievementWithAccomplishedDate(PlayerAchievementDTO playerAchievement, AppTeamEntity appTeam) {
        if (playerAchievement.getAccomplished()) {
            playerAchievement.setAccomplishedDate(new Date());

        } else {
            playerAchievement.setAccomplishedDate(null);
        }
        playerAchievementRepository.save(playerAchievementMapper.toEntity(playerAchievement));
        if (playerAchievement.getAccomplished()) {
            achievementNotificationMaker.sendAchievementNotify(playerAchievement, appTeam);
        }
    }*/

    private boolean isNeededToCalculateAchievementForPlayer(PlayerDTO player, AchievementDTO achievement) {
        return !achievement.isOnlyForPlayers() || !player.isFan();
    }


    private PlayerAchievementDTO calculateKAZDEMU_CO_MU_PATRIAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.MATCH || achievementType == AchievementType.BEER || achievementType == AchievementType.GOAL) {
            IGoalBeerMatch iGoalBeerMatch = playerAchievementRepository.getFirstMatchWithSameGoalsAndBeers(playerDTO.getId());
            if (iGoalBeerMatch != null) {
                return returnPlayerAchievement(achievement, playerDTO, iGoalBeerMatch.getMatchId(),
                        "Počer gólů: " + iGoalBeerMatch.getGoalNumber() + ", počet piv: " + iGoalBeerMatch.getBeerNumber() + ", počet panáků " + iGoalBeerMatch.getLiquorNumber());
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateFOTBAL_JE_JEN_ZAMINKAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.SEASON || achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.MATCH) {
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                List<MatchDTO> matches = returnMatchesIfPlayerAttendedAll(playerDTO.getId(), season.getId(), appTeam);
                if (!matches.isEmpty() && matches.size() > 7) {
                    if (playerDTO.isFan()) {
                        matches.sort(new OrderMatchByDate());
                        return returnPlayerAchievement(achievement, playerDTO, matches.get(matches.size() - 1).getId(),
                                "V sezoně " + season.getName() + ", počet zápasů: " + matches.size());
                    } else {
                        long fineCount = receivedFineService.getReceivedFineCount(playerDTO.getId(), matchService.convertMatchesToIds(matches), "Třetí poločas", appTeam.getId());
                        if (fineCount == 0) {
                            return returnPlayerAchievement(achievement, playerDTO, matches.get(matches.size() - 1).getId(),
                                    "V sezoně " + season.getName() + ", počet zápasů: " + matches.size());
                        }
                    }
                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateTAHOUNAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.BEER) {
            List<BeerDTO> beers = beerService.getTopDrinkersByMatch(appTeam.getId());
            for (int i = 0; i < beers.size(); i++) {
                Long playerId = playerDTO.getId();
                if (isPlayerBestDrinkerInBeerListIndex(i, beers, playerId) && isPlayerBestDrinkerInBeerListIndex(i + 1, beers, playerId) && isPlayerBestDrinkerInBeerListIndex(i + 2, beers, playerId)) {
                    BeerDTO beerDTO = beers.get(i + 2);
                    return returnPlayerAchievement(achievement, playerDTO, beerDTO.getMatchId(), "V posledním třetím zápase vypil " + beerDTO.getBeerNumber() + " piv a " + beerDTO.getLiquorNumber() + " kořalek");
                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateKORALAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.BEER) {
            BeerDTO beerDTO = beerService.getFirstMatchWhereLiquorMoreThanBeer(playerDTO.getId());
            if (beerDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, beerDTO.getMatchId(), "V zápase vypil " + beerDTO.getBeerNumber() + " piv a " + beerDTO.getLiquorNumber() + " kořalek");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateMECENASAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                StatisticsFilter statisticsFilter = new StatisticsFilter();
                statisticsFilter.setMatchStatsOrPlayerStats(false);
                statisticsFilter.setSeasonId(season.getId());
                statisticsFilter.setAppTeam(appTeam);
                ReceivedFineDetailedResponse response = receivedFineService.getAllDetailed(statisticsFilter);
                if (!response.getFineList().isEmpty() && response.getFineList().get(0).getPlayer().equals(playerDTO)) {
                    return returnPlayerAchievement(achievement, playerDTO, null, "V sezoně " + season.getName() + " dostal na pokutách " + response.getFineList().get(0).getFineAmount() + " Kč");

                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateOSLAVENECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.BEER) {
            List<BeerDTO> beers = beerService.getTopDrinkersByMatch(appTeam.getId());
            for (int i = 0; i < beers.size(); i++) {
                Long playerId = playerDTO.getId();
                if (isPlayerBestDrinkerInBeerListIndex(i, beers, playerId)) {
                    BeerDTO beerDTO = beers.get(i);
                    int playerTotalDrinkNumber = beerDTO.getLiquorNumber() + beerDTO.getBeerNumber();
                    StatisticsFilter statisticsFilter = new StatisticsFilter();
                    statisticsFilter.setMatchStatsOrPlayerStats(false);
                    statisticsFilter.setMatchId(beerDTO.getMatchId());
                    statisticsFilter.setAppTeam(appTeam);
                    BeerDetailedResponse beerDetailedResponse = beerService.getAllDetailed(statisticsFilter);
                    int totalDrinkNumber = beerDetailedResponse.getTotalBeers() + beerDetailedResponse.getTotalLiquors();
                    if ((totalDrinkNumber - playerTotalDrinkNumber) < playerTotalDrinkNumber) {
                        return returnPlayerAchievement(achievement, playerDTO, beerDTO.getMatchId(), "V zápase padlo " + totalDrinkNumber + " piv a kořalek a z toho jich vypil " + playerTotalDrinkNumber);

                    }
                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateUSPESNY_DENAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.BEER || achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.MATCH) {
            IGoalBeerFineMatch iGoalBeerMatch = playerAchievementRepository.getFirstMatchWithGoalYellowBeerAndLiquor(playerDTO.getId(), "Žlutá karta");
            if (iGoalBeerMatch != null) {
                return returnPlayerAchievement(achievement, playerDTO, iGoalBeerMatch.getMatchId(),
                        "Počer gólů: " + iGoalBeerMatch.getGoalNumber() + ", počet piv: " + iGoalBeerMatch.getBeerNumber() +
                                ", počet panáků " + iGoalBeerMatch.getLiquorNumber() + ", počet žlutých " + iGoalBeerMatch.getFineNumber());
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateCERNA_PRACEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.GOAL || achievementType == AchievementType.MATCH) {
            FootballPlayerDTO footballPlayerDTO = playerDTO.getFootballPlayer();
            if (footballPlayerDTO == null) {
                return returnFailedPlayerAchievement(achievement, playerDTO);
            }
            FootballMatchPlayerDTO footballMatchPlayerDTO = footballPlayerStatsService.getBestPlayerWithoutGoals(footballPlayerDTO.getId());
            if (footballMatchPlayerDTO != null) {
                return returnPlayerAchievementForFootballMatch(achievement, playerDTO, footballMatchPlayerDTO.getMatchId(),
                        "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateDOPINGAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.MATCH) {
            Long matchId = playerAchievementRepository.getFirstMatchWithHangoverAndHattrickOrCleanSheet(playerDTO.getId(), "Zbytkáč či kocovina");
            if (matchId != null) {
                return returnPlayerAchievement(achievement, playerDTO, matchId, "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateAUTICKOAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.GOAL || achievementType == AchievementType.MATCH) {
            GoalDTO goalDTO = goalService.getGoalkeeperWithMostPointsInMatch(playerDTO.getId(), appTeam.getId());
            if (goalDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, goalDTO.getMatchId(), "Počer gólů: " + goalDTO.getGoalNumber() + ", počet asistencí: " + goalDTO.getAssistNumber());
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateOZEN_SE_OZER_SEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.BEER || achievementType == AchievementType.RECEIVED_FINE) {
            BeerDTO beerDTO = beerService.getFirstMatchWhereAtLeastBeersWithFine(playerDTO.getId(), "Svatba", 7);
            if (beerDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, beerDTO.getMatchId(), "Vypil " + beerDTO.getBeerNumber() + " piv a " + beerDTO.getLiquorNumber() + " kořalek");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateROSS_GELLERAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            Integer weddingNumber = receivedFineService.getAtLeastNumberOfFineInHistory(playerDTO.getId(), "Svatba", 3);
            if (weddingNumber != null) {
                return returnPlayerAchievement(achievement, playerDTO, null, "Hráč byl již " + weddingNumber + "x ženatý");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateZASTRELOVANIAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.GOAL || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWithAtLeastXFines(playerDTO.getId(), "Překop", "Gól", 2, 2);
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "Překopy: " + iMatchIdNumberOneNumberTwo.getFirstNumber() + ", góly: " + iMatchIdNumberOneNumberTwo.getSecondNumber());
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateSOBECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.GOAL) {
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                StatisticsFilter statisticsFilter = new StatisticsFilter();
                statisticsFilter.setMatchStatsOrPlayerStats(false);
                statisticsFilter.setSeasonId(season.getId());
                statisticsFilter.setPlayerId(playerDTO.getId());
                statisticsFilter.setAppTeam(appTeam);
                GoalDetailedResponse response = goalService.getAllDetailed(statisticsFilter);
                if (!response.getGoalList().isEmpty() && response.getGoalList().get(0).getGoalNumber() >= 5 && response.getGoalList().get(0).getAssistNumber() == 0) {
                    return returnPlayerAchievement(achievement, playerDTO, null, "V sezoně " + season.getName() + " dal " + response.getGoalList().get(0).getGoalNumber() + " gólů");

                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateNESOBECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.GOAL) {
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                StatisticsFilter statisticsFilter = new StatisticsFilter();
                statisticsFilter.setMatchStatsOrPlayerStats(false);
                statisticsFilter.setSeasonId(season.getId());
                statisticsFilter.setPlayerId(playerDTO.getId());
                statisticsFilter.setAppTeam(appTeam);
                GoalDetailedResponse response = goalService.getAllDetailed(statisticsFilter);
                if (!response.getGoalList().isEmpty() && response.getGoalList().get(0).getAssistNumber() >= 4 && response.getGoalList().get(0).getGoalNumber() <= response.getGoalList().get(0).getAssistNumber() / 2) {
                    return returnPlayerAchievement(achievement, playerDTO, null, "V sezoně " + season.getName() + " dal " + response.getGoalList().get(0).getGoalNumber() + " gólů a nasbíral "
                            + response.getGoalList().get(0).getAssistNumber() + " asistencí");

                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateJEN_NA_SKOKAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWithAtLeastOneOfFinesAndXSecondFines(playerDTO.getId(), "Pozdní příchod do začátku", "Pozdní příchod po začátku", "Pozdní příchod po 10. minutě", "Červená karta", 1);
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateHVEZDNE_MANYRYAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.MATCH) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWherePlayerIsBestPlayerWithFine(playerDTO.getId(), "Pozdní příchod do začátku", "Pozdní příchod po začátku", "Pozdní příchod po 10. minutě");
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateMIREK_DUSINAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            if (playerDTO.isActive()) {
                SeasonFilter seasonFilter = new SeasonFilter();
                seasonFilter.setAppTeam(appTeam);
                for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                    StatisticsFilter statisticsFilter = new StatisticsFilter();
                    statisticsFilter.setMatchStatsOrPlayerStats(false);
                    statisticsFilter.setSeasonId(season.getId());
                    statisticsFilter.setAppTeam(appTeam);
                    ReceivedFineDetailedResponse response = receivedFineService.getAllDetailed(statisticsFilter);
                    if (!response.getFineList().isEmpty() &&
                            response.getFineList().get(response.getFineList().size() - 1).getPlayer().equals(playerDTO)) {
                        return returnPlayerAchievement(achievement, playerDTO, null, "V sezoně " + season.getName() + " dostal na pokutách " + response.getFineList().get(response.getFineList().size() - 1).getFineAmount() + " Kč");

                    }
                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateKONZISTENCEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.GOAL || achievementType == AchievementType.MATCH) {
            FootballPlayerDTO footballPlayerDTO = playerDTO.getFootballPlayer();
            if (footballPlayerDTO == null) {
                return returnFailedPlayerAchievement(achievement, playerDTO);
            }
            FootballMatchPlayerDTO footballMatchPlayerDTO = footballPlayerStatsService.getRowIfPlayerScoresInThreeMatchesInRow(footballPlayerDTO.getId(), appTeam.getId());
            if (footballMatchPlayerDTO != null) {
                return returnPlayerAchievementForFootballMatch(achievement, playerDTO, footballMatchPlayerDTO.getMatchId(),
                        "V posledním zápase dal " + footballMatchPlayerDTO.getGoals() + " gólů");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateDAVID_BECKHAMAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.MATCH || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWherePlayerIsBestPlayerWithFine(playerDTO.getId(), "Zmínka v tisku");
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateDLOUHA_NOCAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWithAtLeastOneOfFinesAndXSecondFines(
                    playerDTO.getId(),
                    "Pozdní příchod do začátku",
                    "Pozdní příchod po začátku",
                    "Pozdní příchod po 10. minutě",
                    "Zbytkáč či kocovina",
                    1);
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateZBYTECNE_PRASEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            Long matchId = playerAchievementRepository.getFirstWinningMatchWithFine(playerDTO.getId(), "Červená karta", appTeam.getId());
            if (matchId != null) {
                return returnPlayerAchievement(achievement, playerDTO, matchId, "");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateDEN_BLBECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            Long matchId = playerAchievementRepository.findFirstMatchWherePlayerReceivedAtLeastXFines(playerDTO.getId());
            if (matchId != null) {
                return returnPlayerAchievement(achievement, playerDTO, matchId, getListOfFines(playerDTO.getId(), matchId, appTeam));

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculatePOROUCHANY_BUDIKAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                Long matchId = playerAchievementRepository.findFirstMatchInSeasonWithLateArrival(playerDTO.getId(), season.getId());
                if (matchId != null) {
                    return returnPlayerAchievement(achievement, playerDTO, matchId, "V sezoně " + season.getName());
                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateZLUTY_HNEDY_POPLACHAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWithAtLeastXFines(playerDTO.getId(), "Zbytkáč či kocovina", "Vyprazdňování při zápase", 1, 1);
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateSBERATELAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.MATCH) {
            List<PlayerAchievementDTO> playerAchievements = playerAchievementRepository.findFirstDuplicateMatchAchievements(playerDTO.getId()).stream().map(playerAchievementMapper::toDTO).toList();
            if (playerAchievements.size() > 1) {
                return returnPlayerAchievement(achievement, playerDTO, playerAchievements.get(0).getMatch().getId(), getListOfAchievements(playerAchievements));
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateMEDMRDKAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            FineDTO fineDTO = fineService.getFineByName("Zmínka v tisku", appTeam.getId());
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                ReceivedFineFilter receivedFineFilter = new ReceivedFineFilter();
                receivedFineFilter.setFineId(fineDTO.getId());
                receivedFineFilter.setSeasonId(season.getId());
                receivedFineFilter.setPlayerId(playerDTO.getId());
                receivedFineFilter.setAppTeam(appTeam);
                List<ReceivedFineDTO> receivedFines = receivedFineService.getAll(receivedFineFilter);
                if (receivedFines.size() > 1) {
                    return returnPlayerAchievement(achievement, playerDTO, receivedFines.get(receivedFines.size() - 1).getMatchId(), "V sezoně " + season.getName() + " byl hráč zmíněn celkem " + receivedFines.size() + "x.");

                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateNAROD_SEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.MATCH || achievementType == AchievementType.PLAYER) {
            MatchDTO matchDTO = matchService.getFirstMatchWherePlayerAttends(playerDTO);
            if (matchDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, matchDTO.getId(), "Všechno nejlepší!");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculatePRIORITYAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.MATCH || achievementType == AchievementType.PLAYER) {
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                List<MatchDTO> matches = returnMatchesIfPlayerAttendedAll(playerDTO.getId(), season.getId(), appTeam);
                if (!matches.isEmpty()) {
                    matches.sort(new OrderMatchByDate());
                    return returnPlayerAchievement(achievement, playerDTO, matches.get(matches.size() - 1).getId(),
                            "V sezoně " + season.getName() + ", počet zápasů: " + matches.size());
                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateZLUTA_JE_DOBRAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                String firstFineName = "Vyprazdňování při zápase";
                String secondFineName = "Žlutá karta";
                IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.findLastMatchInSeasonWherePlayerGetsTwoFines(playerDTO.getId(), firstFineName,
                        secondFineName, season.getId());
                if (iMatchIdNumberOneNumberTwo.getFirstNumber() != 0 && iMatchIdNumberOneNumberTwo.getSecondNumber() != 0 && iMatchIdNumberOneNumberTwo.getMatchId() != null) {
                    return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(),
                            iMatchIdNumberOneNumberTwo.getFirstNumber() + "x " + firstFineName + ", " + iMatchIdNumberOneNumberTwo.getSecondNumber() + "x " + secondFineName);
                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateIONTAKAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.BEER) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.findFirstMatchWhereFineExistsAndPlayerHasBeer(playerDTO.getId(), "Třetí poločas");
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "Hráč si dal " + iMatchIdNumberOneNumberTwo.getSecondNumber() + " piv");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateSPORTOVECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.GOAL || achievementType == AchievementType.BEER) {
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.findBeersAndGoalsInSeason(playerDTO.getId(), season.getId());
                if (iMatchIdNumberOneNumberTwo.getFirstNumber() != null && iMatchIdNumberOneNumberTwo.getSecondNumber() != null &&
                        iMatchIdNumberOneNumberTwo.getFirstNumber() > iMatchIdNumberOneNumberTwo.getSecondNumber()) {
                    return returnPlayerAchievement(achievement, playerDTO, null,
                            iMatchIdNumberOneNumberTwo.getFirstNumber() + " gólů a " + iMatchIdNumberOneNumberTwo.getSecondNumber() + " piv v sezoně " + season.getName());
                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculatePROCAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWithAtLeastOneOfFinesAndXSecondFines(playerDTO.getId(),
                    "Žlutá karta", "Červená karta", "Červená karta", "Zbytkáč či kocovina", 1);
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateHLADINKAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.BEER) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.findFirstMatchWhereFineExistsAndPlayerHasLiquor(playerDTO.getId(), "Zbytkáč či kocovina");
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "Hráč si dal " + iMatchIdNumberOneNumberTwo.getSecondNumber() + " panáků");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateSTENEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.BEER) {
            SeasonFilter seasonFilter = new SeasonFilter();
            seasonFilter.setAppTeam(appTeam);
            for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
                StatisticsFilter statisticsFilter = new StatisticsFilter();
                statisticsFilter.setMatchStatsOrPlayerStats(false);
                statisticsFilter.setSeasonId(season.getId());
                statisticsFilter.setPlayerId(playerDTO.getId());
                statisticsFilter.setAppTeam(appTeam);
                BeerDetailedResponse response = beerService.getAllDetailed(statisticsFilter);
                if (!response.getBeerList().isEmpty() && response.getTotalBeers() >= 60) {
                    return returnPlayerAchievement(achievement, playerDTO, null, "V sezoně " + season.getName() + " vypil "
                            + response.getTotalBeers() + " piv a " + response.getTotalLiquors() + " panáků");

                }
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateCIRHOZAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.MATCH || achievementType == AchievementType.BEER) {
            BeerDTO beerDTO = beerService.getFirstBeerIfPlayerDrinksAtLeastXLiquorsAndThenNotAttendInNextMatch(playerDTO.getId(), 5);
            if (beerDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, beerDTO.getMatchId(), "Vypil " + beerDTO.getBeerNumber() + " piv a " + beerDTO.getLiquorNumber() + " kořalek");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateTEN_TO_PERFEKTNE_KOPEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            ReceivedFineDTO receivedFine = receivedFineService.getFirstOccurrenceOfFine(playerDTO.getId(), "Nedal penaltu");
            if (receivedFine != null) {
                return returnPlayerAchievement(achievement, playerDTO, receivedFine.getMatchId(), "");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateCESTNY_JAKO_KAREL_ERBENAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL) {
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateADA_VETVICKAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL) {
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculatePO_PORADNE_PRACI_PORADNA_OSLAVAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        return null;
    }

    private PlayerAchievementDTO calculateKLUB_SRACUAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.findKlubSracu(playerDTO.getId(), appTeam.getId());
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "Po tomto zápase nešlo do hospody celkem " + iMatchIdNumberOneNumberTwo.getFirstNumber() + " sráčů");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateOSAMELY_DRZAKAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.findOsamelyDrzak(playerDTO.getId(), appTeam.getId());
            if (iMatchIdNumberOneNumberTwo != null) {
                StatisticsFilter statisticsFilter = new StatisticsFilter();
                statisticsFilter.setMatchStatsOrPlayerStats(false);
                statisticsFilter.setPlayerId(playerDTO.getId());
                statisticsFilter.setMatchId(iMatchIdNumberOneNumberTwo.getMatchId());
                BeerDetailedResponse response = beerService.getAllDetailed(statisticsFilter);
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "Zápasu se účastnilo celkem " +
                        iMatchIdNumberOneNumberTwo.getSecondNumber() + " lidí a ty jako jediný jsi šel do hospody. Dal sis " +
                        response.getTotalBeers() + " piv a " + response.getTotalLiquors() + " panáků.");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateVE_DVOU_SE_TO_LEPE_TAHNEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL || achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.BEER) {
            IMatchIdThreeNumbersAndText result = playerAchievementRepository.findVeDvouSeToLepeTahne(playerDTO.getId(), appTeam.getId());
            if (result != null) {
                return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Po zápase jste šli z hráčů na pivo jako jediní ty a " + result.getText() +
                        ". Dal sis " + result.getSecondNumber() + " piv a " + result.getThirdNumber() + " panáků.");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateSTRELECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.GOAL, AchievementType.SEASON, AchievementType.MATCH)) return null;
        SeasonFilter seasonFilter = new SeasonFilter();
        seasonFilter.setAppTeam(appTeam);
        for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
            IMatchIdNumberOneNumberTwo r = playerAchievementRepository.findStrelecInSeason(playerDTO.getId(), season.getId(), appTeam.getId());
            if (r != null) {
                return returnPlayerAchievement(achievement, playerDTO, null, "Nejlepší střelec sezony " + season.getName() + " se " + r.getFirstNumber() + " góly a " + r.getSecondNumber() + " asistencemi.");
            }
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateFOTR_JE_LOTRAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.RECEIVED_FINE)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findFotrJeLotr(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            String kids = result.getFirstNumber()==1 ? " dítěte" : "dětí";
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hrdý otec " + result.getFirstNumber() + ". " +
                    kids + " dostal zatím " + result.getSecondNumber() + " karet");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateMARATONECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.FOOTBAR)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findMaratonec(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč doteď uběhl celkem " +
                    String.format("%.1f", result.getFirstNumber() / 1000.0) + " km");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateROBERTO_CARLOSAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.FOOTBAR, AchievementType.GOAL)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findRobertoCarlos(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč v zápase vystřelil střelu s maximální rychlostí " + result.getFirstNumber() +
                    "km/h a dal u toho " + result.getSecondNumber() + " gólů");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateSPILMACHRAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.FOOTBAR)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findSpilmachr(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč v zápase rozdal celkem " + result.getFirstNumber() +
                    " přihrávek. Zároveň zaznamenal v zápase " + result.getSecondNumber() + " asistencí");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateJA_TO_ZA_VAS_OBEHALAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.FOOTBAR)) return null;
        IMatchIdDecimalAndNumber result = playerAchievementRepository.findJaToZaVasObehal(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            String distance = roundDoubleToString(result.getFirstNumber());
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč v zápase naběhal " + distance +
                    "km. Může to být trošku zavádějící, vzhledem k tomu, že footbar mělo nasazeno pouze " + result.getSecondNumber() + " hráčů.");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateDOPLNENI_TEKUTINAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.FOOTBAR, AchievementType.BEER)) return null;
        IMatchIdDecimalAndNumber result = playerAchievementRepository.findDoplneniTekutin(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            String distance = roundDoubleToString(result.getFirstNumber());
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč v zápase naběhal " + distance +
                    " km. Jako doplnění tekutin mu posloužilo " + result.getSecondNumber() + " piv.");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateNASTUP_JAKO_HROMAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.MATCH, AchievementType.GOAL)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findNastupJakoHromGoal(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč ve svém prvním zápase za Trus" +
                    " dal " + result.getFirstNumber() + " gólů.");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateKDYZ_LEJU_TAK_PORADNEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.BEER, AchievementType.SEASON, AchievementType.MATCH)) return null;
        SeasonFilter seasonFilter = new SeasonFilter();
        seasonFilter.setAppTeam(appTeam);
        for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
            ISeasonDrinkAverage r = playerAchievementRepository.findKdyzLejuTakPoradneInSeason(playerDTO.getId(), season.getId(), appTeam.getId());
            if (r != null) {
                return returnPlayerAchievement(achievement, playerDTO, null, "Nejlepší píč sezony " + season.getName() + " s " +
                        r.getThirdNumber() + " pivy a " + r.getFourthNumber() + " panáky a průměrem " + roundDoubleToString(r.getFirstNumber()) + " piv na zápas" +
                        " a " + roundDoubleToString(r.getSecondNumber()) + " panáků na zápas. To vše zvládl v " + r.getFifthNumber() + " zápasech");
            }
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateMACHYREKAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.RECEIVED_FINE)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findMachyrek(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč vstřelil gól rabonou, zatímco " +
                    result.getFirstNumber() + " hráčů a fanoušků se na to s obdivem dívalo");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateSDILENY_STRELECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.GOAL)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findSdilenyStrelec(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč vstřelil " + result.getFirstNumber() +
                    " góly. Celkem v tomto zápase střelilo hattrick " + result.getSecondNumber() + " hráčů");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateNESOBECKY_HRDINAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.GOAL)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findNesobeckyHrdina(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč rozdal celkem " + result.getFirstNumber() +
                    " přihrávek. Jako bonus přidal i " + result.getSecondNumber() + " gólů");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateGOLY_NE_RADEJI_PIVOAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.BEER, AchievementType.SEASON, AchievementType.GOAL)) return null;
        SeasonFilter seasonFilter = new SeasonFilter();
        seasonFilter.setAppTeam(appTeam);
        for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
            IAverageAndTwoNumbers r = playerAchievementRepository.findGolyNeRadejiPivoInSeason(playerDTO.getId(), season.getId(), appTeam.getId());
            if (r != null) {
                return returnPlayerAchievement(achievement, playerDTO, null, "Hráč za sezonu " + season.getName() + " získal průměr " +
                        roundDoubleToString(r.getFirstNumber()) + " piv na gól. Celkem to dělá " + r.getSecondNumber() + " piv a " + r.getThirdNumber()
                + " gólů.");
            }
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateJARDA_KUZELAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.MATCH)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findJardaKuzel(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč vynechal předchozí " +
                    result.getFirstNumber() + " zápasy. Nakonec po velkolepém návratu krom hvězdy utkání vstřelil počet gólů: " + result.getSecondNumber());
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateMODERNI_GOLMANSKA_SKOLAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.MATCH, AchievementType.GOAL)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findModerniGolmanskaSkola(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            String assist = result.getFirstNumber() > 0 ? "asistence" : "asistenci";
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Tento gólman, který odchytal celých " + result.getSecondNumber() +
                    " minut si v zápase připsal " + result.getFirstNumber() + " " + assist + ".");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateMORALNI_PODPORAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.MATCH, AchievementType.SEASON)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findMoralniPodpora(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            String assist = result.getFirstNumber() > 0 ? "asistence" : "asistenci";
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(), "Hráč strávil utkání na tribuně jako podpora týmu. Celkově tým taktéž podpořil " +
                    result.getFirstNumber() + " pivy a " + result.getSecondNumber() + " panáky");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateLAZAR_NA_TRIBUNACHAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.MATCH, AchievementType.SEASON)) return null;
        SeasonFilter seasonFilter = new SeasonFilter();
        seasonFilter.setAppTeam(appTeam);
        for (SeasonDTO season : seasonService.getAll(seasonFilter)) {
            IMatchIdNumberOneNumberTwo r = playerAchievementRepository.findLazarNaTribune(playerDTO.getId(), season.getId(), appTeam.getId());
            if (r != null) {
                return returnPlayerAchievement(achievement, playerDTO, null, "Hráč v sezoně " + season.getName() + " strávil celkem " +
                        r.getFirstNumber() + " zápasů na tribuně a vypil u toho " + r.getSecondNumber() + " piv");
            }
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateDrinkMilestoneAchievement(
            PlayerDTO playerDTO,
            AchievementDTO achievement,
            AppTeamEntity appTeam,
            AchievementType achievementType,
            int beerThreshold,
            int liquorThreshold
    ) {
        if (!shouldCalculate(achievementType, AchievementType.BEER)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findDrinkMilestone(
                playerDTO.getId(), appTeam.getId(), beerThreshold, liquorThreshold);
        if (result != null) {
            return returnPlayerAchievement(
                    achievement,
                    playerDTO,
                    result.getMatchId(),
                    buildDrinkMilestoneDetail(achievement.getCode(), result)
            );
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private String buildDrinkMilestoneDetail(String achievementCode, IMatchIdNumberOneNumberTwo result) {
        return switch (achievementCode) {
            case "JEDNOU_SE_ZACIT_MUSI" ->
                    "Tehdy to bylo první pivko. Po tomto zápase si měl v sobě už " +
                            result.getFirstNumber() + " piv a " + result.getSecondNumber() + " panáků.";
            case "KDYZ_ONO_TO_CHUTNA" ->
                    "Kouzelná hranice 50 piv padla v tomto zápase na milníku " +
                            result.getFirstNumber() + " piv a " + result.getSecondNumber() + " panáků.";
            case "SOUDEK" ->
                    "První sud hráč již vypil. Celkem měl po tomto zápase " +
                            result.getFirstNumber() + " piv a " + result.getSecondNumber() + " panáků.";
            case "CISTERNA" ->
                    "Všechna čest. Kdyby mi takto odtejkala vana. Každopádně hranice byla překročena v tomto zápase v počtu " +
                            result.getFirstNumber() + " piv a " + result.getSecondNumber() + " panáků.";
            case "PRITVRDIME" ->
                    "První panáček padnul. V té době jich měl " +
                            result.getSecondNumber() + " a " + result.getFirstNumber() + " piv k tomu.";
            case "RUMOVY_NADENIK" ->
                    "Opice ožralá dosáhla další mety. V té době konkrétně " +
                            result.getSecondNumber() + " paňáků a " + result.getFirstNumber() + " piv.";
            case "ACHIEVEMENT_MILANA_CURDY" ->
                    "Z nebe se na tebe směje jak si měl v sobě již " +
                            result.getSecondNumber() + " panďuláků a " + result.getFirstNumber() + " piv";
            default -> "Při dosažení milníku měl hráč celkem " + result.getFirstNumber() +
                    " piv a " + result.getSecondNumber() + " panáků.";
        };
    }

    private PlayerAchievementDTO calculateHVEZDA_CO_SE_NEZDAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.MATCH, AchievementType.GOAL)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findFirstBestPlayerMatch(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievementForFootballMatch(achievement, playerDTO, result.getMatchId(),
                    "Pro první hvězdu utkání bylo potřeba vsítit " + result.getFirstNumber() + " gólů");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateKOMPLEXNI_HRACAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.GOAL)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findFirstMatchWithGoalAndAssist(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(),
                    "V tento velký den hráč zaznamenal "
                            + result.getFirstNumber() + " gólů a " + result.getSecondNumber() + " asistencí.");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateFanAttendanceMilestoneAchievement(
            PlayerDTO playerDTO,
            AchievementDTO achievement,
            AppTeamEntity appTeam,
            AchievementType achievementType,
            int attendanceThreshold
    ) {
        if (!shouldCalculate(achievementType, AchievementType.MATCH, AchievementType.PLAYER)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findFanAttendanceMilestone(
                playerDTO.getId(), appTeam.getId(), attendanceThreshold);
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(),
                    "Zatím tento skvělý fanoušek zvádl navštívit " + result.getFirstNumber() + " utkání");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateDO_POCTUAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.MATCH, AchievementType.GOAL, AchievementType.SEASON)) return null;
        IMatchIdThreeNumbersAndText result = playerAchievementRepository.findDoPoctu(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(),
                    "Tato hrůzná série byla zaznamenána v sezoně " + result.getText());
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateHATTRICK_GORDIEHO_HOWAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.GOAL, AchievementType.RECEIVED_FINE)) return null;
        IMatchIdThreeNumbersAndText result = playerAchievementRepository.findHattrickGordiehoHowa(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(),
                    "V zápase zaznamenal " + result.getFirstNumber() + " gólů, " + result.getSecondNumber() +
                            " asistencí a k tomu " + result.getText() + ".");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private PlayerAchievementDTO calculateFineMilestoneAchievement(
            PlayerDTO playerDTO,
            AchievementDTO achievement,
            AppTeamEntity appTeam,
            AchievementType achievementType,
            List<String> fineNames,
            int threshold
    ) {
        if (!shouldCalculate(achievementType, AchievementType.RECEIVED_FINE)) return null;
        IMatchIdNumberOneNumberTwo result = playerAchievementRepository.findFineMilestone(
                playerDTO.getId(), appTeam.getId(), fineNames, threshold);
        if (result != null) {
            return returnPlayerAchievement(
                    achievement,
                    playerDTO,
                    result.getMatchId(),
                    buildFineMilestoneDetail(achievement.getCode(), result)
            );
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private String buildFineMilestoneDetail(String achievementCode, IMatchIdNumberOneNumberTwo result) {
        return switch (achievementCode) {
            case "LEO_BERANEK" ->
                    "Hráč si pořídil nové kopačky již celkem " + result.getFirstNumber() + "x.";
            case "ALZHEIMER" ->
                    "Možná by to chtělo navštívit doktora. Hráč si zapomněl věci na hřišti či doma již " +
                            result.getFirstNumber() + "x.";
            case "AMERICKY_FOTBALISTA" ->
                    "V USA by z něj byl talent. Překop dostal již " + result.getFirstNumber() + "x.";
            default -> "Hráč má aktuálně celkem " + result.getFirstNumber() +
                    " odpovídajících pokut. Milníku dosáhl s počtem " + result.getSecondNumber() + ".";
        };
    }

    private PlayerAchievementDTO calculateCERNE_GENYAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (!shouldCalculate(achievementType, AchievementType.FOOTBAR)) return null;
        IMatchIdDecimalAndNumber result = playerAchievementRepository.findCerneGeny(playerDTO.getId(), appTeam.getId());
        if (result != null) {
            return returnPlayerAchievement(achievement, playerDTO, result.getMatchId(),
                    "Maximální rychlost sprintu v zápase byla " + roundDoubleToString(result.getFirstNumber()) + " km/h.");
        }
        return returnFailedPlayerAchievement(achievement, playerDTO);
    }

    private String roundDoubleToString(Double number) {
        return String.format(Locale.US, "%.2f", number);
    }

    private boolean shouldCalculate(AchievementType type, AchievementType... accepted) {
        if (type == AchievementType.ALL) return true;
        return Arrays.asList(accepted).contains(type);
    }

    private boolean isPlayerBestDrinkerInBeerListIndex(int index, List<BeerDTO> beers, Long playerId) {
        return index < beers.size() && beers.get(index).getPlayerId().equals(playerId);
    }

    private String getListOfFines(Long playerId, Long matchId, AppTeamEntity appTeam) {
        StringBuilder returnString = new StringBuilder("Seznam udělených pokut v zápase:");
        StatisticsFilter receivedFineFilter = new StatisticsFilter();
        receivedFineFilter.setPlayerId(playerId);
        receivedFineFilter.setMatchId(matchId);
        receivedFineFilter.setDetailed(true);
        receivedFineFilter.setAppTeam(appTeam);
        ReceivedFineDetailedResponse response = receivedFineService.getAllDetailed(receivedFineFilter);

        List<ReceivedFineDetailedDTO> fineList = response.getFineList();
        for (int i = 0; i < fineList.size(); i++) {
            returnString.append(" ").append(fineList.get(i).getFine().getName());
            if (i < fineList.size() - 1) {
                returnString.append(",");
            }
        }

        return returnString.toString();
    }

    private String getListOfAchievements(List<PlayerAchievementDTO> playerAchievements) {
        StringBuilder returnString = new StringBuilder("Seznam achievementů:");
        for (int i = 0; i < playerAchievements.size(); i++) {
            returnString.append(" ").append(playerAchievements.get(i).getAchievement().getName());
            if (i < playerAchievements.size() - 1) {
                returnString.append(",");
            }
        }

        return returnString.toString();
    }


    private List<MatchDTO> returnMatchesIfPlayerAttendedAll(Long playerId, Long seasonId, AppTeamEntity appTeam) {
        List<MatchDTO> matches = getAllMatchesBySeasonAndPlayer(playerId, seasonId, appTeam);
        MatchFilter allMatchesFilter = new MatchFilter();
        allMatchesFilter.setSeasonId(seasonId);
        allMatchesFilter.setAppTeam(appTeam);
        List<MatchDTO> allMatches = matchService.getAll(allMatchesFilter);
        if (matches.size() == allMatches.size()) {
            return matches;
        }
        return new ArrayList<>();
    }

    private List<MatchDTO> getAllMatchesBySeasonAndPlayer(Long playerId, Long seasonId, AppTeamEntity appTeam) {
        MatchFilter matchFilter = new MatchFilter();
        matchFilter.setPlayerList(List.of(playerId));
        matchFilter.setSeasonId(seasonId);
        matchFilter.setAppTeam(appTeam);
        return matchService.getAll(matchFilter);
    }

    private PlayerAchievementDTO returnPlayerAchievement(AchievementDTO achievement, PlayerDTO playerDTO, Long matchId, String detail) {
        return new PlayerAchievementDTO(achievement, playerDTO, returnTestMatch(matchId), detail, true);
    }

    private PlayerAchievementDTO returnPlayerAchievementForFootballMatch(AchievementDTO achievement, PlayerDTO playerDTO, Long footballMatchId, String detail) {
        return new PlayerAchievementDTO(achievement, playerDTO, returnTestFootballMatch(footballMatchId), detail, true);
    }

    private PlayerAchievementDTO returnFailedPlayerAchievement(AchievementDTO achievement, PlayerDTO playerDTO) {
        return new PlayerAchievementDTO(achievement, playerDTO, false);
    }

    private MatchDTO returnTestMatch(Long matchId) {
        if (matchId == null) {
            return null;
        }
        MatchDTO match = matchService.getMatch(matchId);
        match.setPlayerIdList(new ArrayList<>());
        match.setFootballMatch(null);
        return match;
    }

    private FootballMatchDTO returnTestFootballMatch(Long matchId) {
        if (matchId == null) {
            return null;
        }
        FootballMatchDTO match = footballMatchService.getFootballMatchById(matchId);
        match.setHomePlayerList(new ArrayList<>());
        match.setAwayPlayerList(new ArrayList<>());
        return match;
    }

    private record PlayerAchievementKey(Long playerId, Long achievementId) {
    }

    private static class AchievementCalculationSummary {
        long calculated;
        long accomplished;
        long failed;
        long skippedNull;
        long created;
        long updated;
        long unchanged;

        AchievementCalculationSummary copy() {
            AchievementCalculationSummary copy = new AchievementCalculationSummary();
            copy.calculated = calculated;
            copy.accomplished = accomplished;
            copy.failed = failed;
            copy.skippedNull = skippedNull;
            copy.created = created;
            copy.updated = updated;
            copy.unchanged = unchanged;
            return copy;
        }

        AchievementCalculationSummary minus(AchievementCalculationSummary other) {
            AchievementCalculationSummary diff = new AchievementCalculationSummary();
            diff.calculated = calculated - other.calculated;
            diff.accomplished = accomplished - other.accomplished;
            diff.failed = failed - other.failed;
            diff.skippedNull = skippedNull - other.skippedNull;
            diff.created = created - other.created;
            diff.updated = updated - other.updated;
            diff.unchanged = unchanged - other.unchanged;
            return diff;
        }
    }

    private static class AchievementCalculationStats {
        private final String code;
        private long calculated;
        private long accomplished;
        private long failed;
        private long skippedNull;
        private long created;
        private long updated;
        private long unchanged;
        private long totalNanos;
        private long maxNanos;
        private Long maxPlayerId;
        private String maxPlayerName;

        private AchievementCalculationStats(String code) {
            this.code = code;
        }

        private void recordCalculation(long nanos, Long playerId, String playerName) {
            calculated++;
            totalNanos += nanos;
            if (nanos > maxNanos) {
                maxNanos = nanos;
                maxPlayerId = playerId;
                maxPlayerName = playerName;
            }
        }

        private long getTotalNanos() {
            return totalNanos;
        }
    }

}
