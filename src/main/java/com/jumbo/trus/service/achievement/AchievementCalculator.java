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
import com.jumbo.trus.entity.repository.achievement.AchievementRepository;
import com.jumbo.trus.entity.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.service.GoalService;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.achievement.helper.*;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.order.OrderMatchByDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final Map<String, AchievementFunction> achievementCalculators =
            Map.ofEntries(
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
                    Map.entry("AUTICKO", (p, a, at, t) -> calculateAUTICKOAchievement(p, a, t)),
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
                    Map.entry("HLADINKA", (p, a, at, t)-> calculateHLADINKAAchievement(p, a, t)),
                    Map.entry("STENE", this::calculateSTENEAchievement),
                    Map.entry("CIRHOZA", (p, a, at, t) -> calculateCIRHOZAAchievement(p, a, t)),
                    Map.entry("TEN_TO_PERFEKTNE_KOPE", (p, a, at, t) -> calculateTEN_TO_PERFEKTNE_KOPEAchievement(p, a, t)),
                    Map.entry("ADA_VETVICKA", (p, a, at, t) -> calculateADA_VETVICKAAchievement(p, a, t))
            );


    public void calculateAllAchievements(List<PlayerDTO> playerList, AppTeamEntity appTeam, AchievementType achievementType) {
        List<AchievementDTO> achievements = achievementRepository.findAll().stream().map(achievementMapper::toDTO).toList();
        for (PlayerDTO player : playerList) {
            calculateAndSaveAchievementForPlayer(achievements, player, appTeam, achievementType);
        }
    }

    private void calculateAndSaveAchievementForPlayer(List<AchievementDTO> achievements, PlayerDTO player, AppTeamEntity appTeam, AchievementType achievementType) {
        for (AchievementDTO achievement : achievements) {
            PlayerAchievementDTO playerAchievementDTO = calculateAchievementForPlayer(achievement, player, appTeam, achievementType);
            if (playerAchievementDTO != null) {
                if (isNeededToSaveAchievement(playerAchievementDTO)) {
                    saveOrRewriteAchievementToRepository(playerAchievementDTO);
                }
                else {
                    initSaveManuallyAchievements(playerAchievementDTO);
                }
            }
        }
    }


    private PlayerAchievementDTO calculateAchievementForPlayer(AchievementDTO achievement, PlayerDTO player, AppTeamEntity appTeam, AchievementType achievementType) {
        if (isNeededToCalculateAchievementForPlayer(player, achievement)) {
            return achievementCalculators.get(achievement.getCode())
                    .apply(player, achievement, appTeam, achievementType);
        }
        return null;
    }

    private boolean isNeededToSaveAchievement(PlayerAchievementDTO playerAchievementDTO) {
        return !playerAchievementDTO.getAchievement().isManually();
    }

    private void saveOrRewriteAchievementToRepository(PlayerAchievementDTO playerAchievement) {
        Long achievementId = getExistingPlayerAchievementId(playerAchievement);
        if (achievementId != null) {
            playerAchievement.setId(achievementId);
        }
        saveNewAchievementToRepository(playerAchievement);
    }

    private Long getExistingPlayerAchievementId(PlayerAchievementDTO playerAchievement) {
        PlayerAchievementEntity entity = playerAchievementRepository.findByPlayerIdAndAchievementId(playerAchievement.getPlayer().getId(), playerAchievement.getAchievement().getId()).orElse(null);
        if (entity == null) {
            return null;
        }
        return entity.getId();
    }

    private void saveNewAchievementToRepository(PlayerAchievementDTO playerAchievement) {
        playerAchievementRepository.save(playerAchievementMapper.toEntity(playerAchievement));
    }

    private void initSaveManuallyAchievements(PlayerAchievementDTO playerAchievementDTO) {
        if(!playerAchievementRepository.existsByPlayerAndAchievement(playerAchievementDTO.getPlayer().getId(), playerAchievementDTO.getAchievement().getId())) {
            saveNewAchievementToRepository(playerAchievementDTO);
        }
    }

    private boolean isNeededToCalculateAchievementForPlayer(PlayerDTO player, AchievementDTO achievement) {
        return !achievement.isOnlyForPlayers() || !player.isFan();
    }


    private PlayerAchievementDTO calculateKAZDEMU_CO_MU_PATRIAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.MATCH || achievementType == AchievementType.BEER || achievementType == AchievementType.GOAL) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.SEASON || achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.MATCH) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.BEER) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.BEER) {
            BeerDTO beerDTO = beerService.getFirstMatchWhereLiquorMoreThanBeer(playerDTO.getId());
            if (beerDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, beerDTO.getMatchId(), "V zápase vypil " + beerDTO.getBeerNumber() + " piv a " + beerDTO.getLiquorNumber() + " kořalek");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateMECENASAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.BEER) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.BEER || achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.MATCH) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.GOAL || achievementType == AchievementType.MATCH) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.MATCH) {
            Long matchId = playerAchievementRepository.getFirstMatchWithHangoverAndHattrickOrCleanSheet(playerDTO.getId(), "Zbytkáč či kocovina");
            if (matchId != null) {
                return returnPlayerAchievement(achievement, playerDTO, matchId, "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateAUTICKOAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.GOAL || achievementType == AchievementType.MATCH) {
            GoalDTO goalDTO = goalService.getGoalkeeperWithMostPointsInMatch(playerDTO.getId());
            if (goalDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, goalDTO.getMatchId(), "Počer gólů: " + goalDTO.getGoalNumber() + ", počet asistencí: " + goalDTO.getAssistNumber());
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateOZEN_SE_OZER_SEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.BEER || achievementType == AchievementType.RECEIVED_FINE) {
            BeerDTO beerDTO = beerService.getFirstMatchWhereAtLeastBeersWithFine(playerDTO.getId(), "Svatba", 7);
            if (beerDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, beerDTO.getMatchId(), "Vypil " + beerDTO.getBeerNumber() + " piv a " + beerDTO.getLiquorNumber() + " kořalek");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateROSS_GELLERAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
            Integer weddingNumber = receivedFineService.getAtLeastNumberOfFineInHistory(playerDTO.getId(), "Svatba", 3);
            if (weddingNumber != null) {
                return returnPlayerAchievement(achievement, playerDTO, null, "Hráč byl již " + weddingNumber + "x ženatý");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateZASTRELOVANIAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.GOAL || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWithAtLeastXFines(playerDTO.getId(), "Překop", "Gól", 2, 2);
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "Překopy: " + iMatchIdNumberOneNumberTwo.getFirstNumber() + ", góly: " + iMatchIdNumberOneNumberTwo.getSecondNumber());
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateSOBECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.GOAL) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.GOAL) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWithAtLeastOneOfFinesAndXSecondFines(playerDTO.getId(), "Pozdní příchod do začátku", "Pozdní příchod po začátku", "Pozdní příchod po 10. minutě", "Červená karta", 1);
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateHVEZDNE_MANYRYAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.MATCH) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWherePlayerIsBestPlayerWithFine(playerDTO.getId(), "Pozdní příchod do začátku", "Pozdní příchod po začátku", "Pozdní příchod po 10. minutě");
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateMIREK_DUSINAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.GOAL || achievementType == AchievementType.MATCH) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.MATCH || achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWherePlayerIsBestPlayerWithFine(playerDTO.getId(), "Zmínka v tisku");
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateDLOUHA_NOCAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWithAtLeastOneOfFinesAndXSecondFines(playerDTO.getId(), "Pozdní příchod do začátku", "Pozdní příchod po začáku", "Pozdní příchod po 10. minutě", "Zbytkáč či kocovina", 1);
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateZBYTECNE_PRASEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
            Long matchId = playerAchievementRepository.getFirstWinningMatchWithFine(playerDTO.getId(), "Červená karta", appTeam.getId());
            if (matchId != null) {
                return returnPlayerAchievement(achievement, playerDTO, matchId, "");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateDEN_BLBECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
            Long matchId = playerAchievementRepository.findFirstMatchWherePlayerReceivedAtLeastXFines(playerDTO.getId());
            if (matchId != null) {
                return returnPlayerAchievement(achievement, playerDTO, matchId, getListOfFines(playerDTO.getId(), matchId, appTeam));

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculatePOROUCHANY_BUDIKAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.getFirstMatchWithAtLeastXFines(playerDTO.getId(), "Zbytkáč či kocovina", "Vyprazdňování při zápase", 1, 1);
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateSBERATELAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.MATCH) {
            List<PlayerAchievementDTO> playerAchievements = playerAchievementRepository.findFirstDuplicateMatchAchievements(playerDTO.getId()).stream().map(playerAchievementMapper::toDTO).toList();
            if (playerAchievements.size() > 1) {
                return returnPlayerAchievement(achievement, playerDTO, playerAchievements.get(0).getMatch().getId(), getListOfAchievements(playerAchievements));
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateMEDMRDKAAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.MATCH || achievementType == AchievementType.PLAYER) {
            MatchDTO matchDTO = matchService.getFirstMatchWherePlayerAttends(playerDTO);
            if (matchDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, matchDTO.getId(), "Všechno nejlepší!");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculatePRIORITYAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.MATCH || achievementType == AchievementType.PLAYER) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.BEER) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.findFirstMatchWhereFineExistsAndPlayerHasBeer(playerDTO.getId(), "Třetí poločas");
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "Hráč si dal " + iMatchIdNumberOneNumberTwo.getSecondNumber() + " piv");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateSPORTOVECAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.GOAL || achievementType == AchievementType.BEER) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE || achievementType == AchievementType.BEER) {
            IMatchIdNumberOneNumberTwo iMatchIdNumberOneNumberTwo = playerAchievementRepository.findFirstMatchWhereFineExistsAndPlayerHasLiquor(playerDTO.getId(), "Zbytkáč či kocovina");
            if (iMatchIdNumberOneNumberTwo != null) {
                return returnPlayerAchievement(achievement, playerDTO, iMatchIdNumberOneNumberTwo.getMatchId(), "Hráč si dal " + iMatchIdNumberOneNumberTwo.getSecondNumber() + " panáků");
            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateSTENEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AppTeamEntity appTeam, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.BEER) {
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
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.MATCH || achievementType == AchievementType.BEER) {
            BeerDTO beerDTO = beerService.getFirstBeerIfPlayerDrinksAtLeastXLiquorsAndThenNotAttendInNextMatch(playerDTO.getId(), 5);
            if (beerDTO != null) {
                return returnPlayerAchievement(achievement, playerDTO, beerDTO.getMatchId(), "Vypil " + beerDTO.getBeerNumber() + " piv a " + beerDTO.getLiquorNumber() + " kořalek");

            }
            return returnFailedPlayerAchievement(achievement, playerDTO);
        }
        return null;
    }

    private PlayerAchievementDTO calculateTEN_TO_PERFEKTNE_KOPEAchievement(PlayerDTO playerDTO, AchievementDTO achievement, AchievementType achievementType) {
        if (achievementType == AchievementType.ALL ||achievementType == AchievementType.RECEIVED_FINE) {
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
}
