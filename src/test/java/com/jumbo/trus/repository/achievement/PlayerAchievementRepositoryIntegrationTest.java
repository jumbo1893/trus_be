package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.ReceivedFineEntity;
import com.jumbo.trus.entity.SeasonEntity;
import com.jumbo.trus.entity.GoalEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.football.FootballMatchPlayerEntity;
import com.jumbo.trus.entity.football.FootballPlayerEntity;
import com.jumbo.trus.repository.BeerRepository;
import com.jumbo.trus.repository.FineRepository;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.repository.SeasonRepository;
import com.jumbo.trus.repository.GoalRepository;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.football.FootballMatchPlayerRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.repository.football.FootballPlayerRepository;
import com.jumbo.trus.service.achievement.helper.IGoalBeerFineMatch;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PlayerAchievementRepositoryIntegrationTest {

    private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");

    @Autowired
    private PlayerAchievementRepository achievementRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private ReceivedFineRepository receivedFineRepository;

    @Autowired
    private AppTeamRepository appTeamRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private FootballPlayerRepository footballPlayerRepository;

    @Autowired
    private FootballMatchRepository footballMatchRepository;

    @Autowired
    private FootballMatchPlayerRepository footballMatchPlayerRepository;

    @Test
    void cardCountsOnlyWhenItIsInAMatchAfterFirstChildbirthFine() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Fotr je lotr query test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);

        PlayerEntity player = new PlayerEntity();
        player.setName("Testovací otec");
        player.setBirthday(date(1990, 1, 1));
        player.setActive(true);
        player.setAppTeam(appTeam);
        player = playerRepository.saveAndFlush(player);

        FineEntity childbirth = fine("Narození dítěte (holka)", appTeam);
        FineEntity yellowCard = fine("Žlutá karta", appTeam);
        MatchEntity cardBeforeBirth = match("Karta před narozením", date(2024, 1, 1), appTeam);
        MatchEntity birth = match("Narození dítěte", date(2024, 2, 1), appTeam);
        MatchEntity cardAfterBirth = match("Karta po narození", date(2024, 3, 1), appTeam);

        receivedFine(player, yellowCard, cardBeforeBirth, appTeam);
        receivedFine(player, childbirth, birth, appTeam);

        assertThat(achievementRepository.findFotrJeLotr(player.getId(), appTeam.getId()))
                .isNull();

        receivedFine(player, yellowCard, cardAfterBirth, appTeam);

        IMatchIdNumberOneNumberTwo result = achievementRepository.findFotrJeLotr(
                player.getId(), appTeam.getId());
        assertThat(result).isNotNull();
        assertThat(result.getMatchId()).isEqualTo(cardAfterBirth.getId());
        assertThat(result.getFirstNumber()).isEqualTo(1);
        assertThat(result.getSecondNumber()).isEqualTo(2);
    }

    @Test
    void denBlbecAcceptsThreeFinesFromAllCategoriesListedInItsDescription() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Den blbec query test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);

        PlayerEntity player = new PlayerEntity();
        player.setName("Testovací smolař");
        player.setBirthday(date(1990, 1, 1));
        player.setActive(true);
        player.setAppTeam(appTeam);
        player = playerRepository.saveAndFlush(player);

        MatchEntity match = match("Den blbec", date(2025, 1, 1), appTeam);
        receivedFine(player, fine("Žlutá karta", appTeam), match, appTeam);
        receivedFine(player, fine("Zapomenutí věcí", appTeam), match, appTeam);
        receivedFine(player, fine("Překop", appTeam), match, appTeam);

        assertThat(achievementRepository.findMatchWherePlayerReceivedAtLeastXFines(
                player.getId(), match.getId()))
                .isEqualTo(match.getId());
    }

    @Test
    void denBlbecRequiresThreeDifferentLogicalCategories() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Den blbec category test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);

        PlayerEntity player = player("Smolař se dvěma kategoriemi", appTeam);
        MatchEntity match = match("Pouze dvě kategorie", date(2025, 1, 2), appTeam);
        receivedFine(player, fine("Žlutá karta", appTeam), match, appTeam);
        receivedFine(player, fine("Červená karta", appTeam), match, appTeam);
        receivedFine(player, fine("Překop", appTeam), match, appTeam);

        assertThat(achievementRepository.findMatchWherePlayerReceivedAtLeastXFines(
                player.getId(), match.getId())).isNull();
    }

    @Test
    void kazdemuCoMuPatriComparesAllDrinksWithGoalsAndAssists() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Každému query test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);
        PlayerEntity player = player("Vyrovnaný hráč", appTeam);
        MatchEntity match = match("Tři body a tři drinky", date(2025, 1, 3), appTeam);

        goal(player, match, appTeam, 1, 2);
        beer(player, match, appTeam, 2, 1);

        assertThat(achievementRepository.getMatchWithSameGoalsAndBeers(
                player.getId(), match.getId())).isNotNull();

        MatchEntity singlePointMatch = match("Jeden bod a jeden drink", date(2025, 1, 5), appTeam);
        goal(player, singlePointMatch, appTeam, 1, 0);
        beer(player, singlePointMatch, appTeam, 1, 0);
        assertThat(achievementRepository.getMatchWithSameGoalsAndBeers(
                player.getId(), singlePointMatch.getId())).isNull();
    }

    @Test
    void ozenSeOzerSeCountsBeersAndShotsTogether() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Svatební oslavy query test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);
        PlayerEntity player = player("Novomanžel", appTeam);
        MatchEntity match = match("Svatební oslava", date(2025, 1, 4), appTeam);

        beer(player, match, appTeam, 3, 5);
        receivedFine(player, fine("Svatba", appTeam), match, appTeam);

        assertThat(beerRepository.findFirstMatchWhereAtLeastBeersAfterFine(
                player.getId(), "Svatba", 7)).isPresent();
    }

    @Test
    void tahounAcceptsSharedFirstPlaceInAllThreeMatches() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Tahoun tie query test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);
        PlayerEntity first = player("První tahoun", appTeam);
        PlayerEntity second = player("Druhý tahoun", appTeam);

        MatchEntity firstMatch = match("První remíza", date(2025, 5, 1), appTeam);
        MatchEntity secondMatch = match("Druhá remíza", date(2025, 5, 2), appTeam);
        MatchEntity thirdMatch = match("Třetí remíza", date(2025, 5, 3), appTeam);
        for (MatchEntity match : List.of(firstMatch, secondMatch, thirdMatch)) {
            beer(first, match, appTeam, 4, 1);
            beer(second, match, appTeam, 5, 0);
        }

        assertThat(achievementRepository.findTahounAtMatch(
                first.getId(), appTeam.getId(), thirdMatch.getId())).isNotNull();
        assertThat(achievementRepository.findTahounAtMatch(
                second.getId(), appTeam.getId(), thirdMatch.getId())).isNotNull();
    }

    @Test
    void machyrekBelongsToThePlayerWhoReceivedTheRabonaGoalFine() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Machýrek query test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);

        PlayerEntity scorer = player("Střelec rabonou", appTeam);
        PlayerEntity teammate = player("Přihlížející spoluhráč", appTeam);
        MatchEntity match = match("Rabona", date(2025, 2, 1), appTeam);
        match.setPlayerList(new ArrayList<>(List.of(scorer, teammate)));
        match = matchRepository.saveAndFlush(match);
        receivedFine(scorer, fine("Rabona (gól)", appTeam), match, appTeam);

        IMatchIdNumberOneNumberTwo result = achievementRepository.findMachyrek(
                scorer.getId(), appTeam.getId(), match.getId());

        assertThat(result).isNotNull();
        assertThat(result.getMatchId()).isEqualTo(match.getId());
        assertThat(result.getFirstNumber()).isEqualTo(2);
        assertThat(achievementRepository.findMachyrek(
                teammate.getId(), appTeam.getId(), match.getId())).isNull();
    }

    @Test
    void sharedTopScorerDoesNotLoseAchievementBecauseAnotherPlayerHasMoreAssists() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Střelec query test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);
        SeasonEntity season = new SeasonEntity();
        season.setName("Testovací sezona");
        season.setFromDate(date(2025, 1, 1));
        season.setToDate(date(2025, 12, 31));
        season.setAppTeam(appTeam);
        season = seasonRepository.saveAndFlush(season);

        PlayerEntity scorer = player("Střelec bez asistencí", appTeam);
        PlayerEntity playmaker = player("Střelec s asistencemi", appTeam);
        MatchEntity match = match("Sdílené první místo", date(2025, 3, 1), appTeam);
        match.setSeason(season);
        match = matchRepository.saveAndFlush(match);
        goal(scorer, match, appTeam, 5, 0);
        goal(playmaker, match, appTeam, 5, 10);

        assertThat(achievementRepository.findStrelecInSeason(
                scorer.getId(), season.getId(), appTeam.getId())).isNotNull();
        assertThat(achievementRepository.findStrelecInSeason(
                playmaker.getId(), season.getId(), appTeam.getId())).isNotNull();
    }

    @Test
    void uspesnyDenAcceptsGoalkeeperCleanSheetInsteadOfGoal() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Úspěšný den query test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);

        FootballPlayerEntity footballPlayer = new FootballPlayerEntity();
        footballPlayer.setName("Brankář s nulou");
        footballPlayer = footballPlayerRepository.saveAndFlush(footballPlayer);

        PlayerEntity goalkeeper = player("Brankář s úspěšným dnem", appTeam);
        goalkeeper.setFootballPlayer(footballPlayer);
        goalkeeper = playerRepository.saveAndFlush(goalkeeper);

        FootballMatchEntity footballMatch = new FootballMatchEntity();
        footballMatch.setDate(date(2025, 4, 1));
        footballMatch = footballMatchRepository.saveAndFlush(footballMatch);

        FootballMatchPlayerEntity performance = new FootballMatchPlayerEntity();
        performance.setPlayer(footballPlayer);
        performance.setMatch(footballMatch);
        performance.setGoalkeepingMinutes(90);
        performance.setCleanSheet(true);
        footballMatchPlayerRepository.saveAndFlush(performance);

        MatchEntity match = match("Čisté konto", date(2025, 4, 1), appTeam);
        match.setFootballMatch(footballMatch);
        match = matchRepository.saveAndFlush(match);

        BeerEntity drinks = new BeerEntity();
        drinks.setPlayer(goalkeeper);
        drinks.setMatch(match);
        drinks.setAppTeam(appTeam);
        drinks.setBeerNumber(1);
        drinks.setLiquorNumber(1);
        beerRepository.saveAndFlush(drinks);
        receivedFine(goalkeeper, fine("Žlutá karta", appTeam), match, appTeam);

        IGoalBeerFineMatch result = achievementRepository.getMatchWithGoalYellowBeerAndLiquor(
                goalkeeper.getId(), "Žlutá karta", match.getId());

        assertThat(result).isNotNull();
        assertThat(result.getMatchId()).isEqualTo(match.getId());
        assertThat(result.getGoalNumber()).isZero();
        assertThat(result.getBeerNumber()).isOne();
        assertThat(result.getLiquorNumber()).isOne();
    }

    @Test
    void teamPlayerAllowsOneMissedMatchAndNeedsFiveAssists() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Týmový hráč test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);
        SeasonEntity season = season(appTeam);
        PlayerEntity accomplished = player("Týmový hráč", appTeam);
        PlayerEntity twoMisses = player("Dva vynechané zápasy", appTeam);
        PlayerEntity fourAssists = player("Čtyři asistence", appTeam);

        List<MatchEntity> matches = List.of(
                match("První", date(2025, 1, 1), season, appTeam,
                        List.of(accomplished, twoMisses, fourAssists)),
                match("Druhý", date(2025, 1, 8), season, appTeam,
                        List.of(accomplished, twoMisses, fourAssists)),
                match("Třetí", date(2025, 1, 15), season, appTeam,
                        List.of(accomplished, fourAssists)),
                match("Čtvrtý", date(2025, 1, 22), season, appTeam, List.of())
        );
        goal(accomplished, matches.get(0), appTeam, 0, 5);
        goal(twoMisses, matches.get(0), appTeam, 0, 5);
        goal(fourAssists, matches.get(0), appTeam, 0, 4);

        IMatchIdNumberOneNumberTwo result = achievementRepository.findTeamPlayerInSeason(
                accomplished.getId(), season.getId(), appTeam.getId());

        assertThat(result).isNotNull();
        assertThat(result.getFirstNumber()).isOne();
        assertThat(result.getSecondNumber()).isEqualTo(5);
        assertThat(achievementRepository.findTeamPlayerInSeason(
                twoMisses.getId(), season.getId(), appTeam.getId())).isNull();
        assertThat(achievementRepository.findTeamPlayerInSeason(
                fourAssists.getId(), season.getId(), appTeam.getId())).isNull();
    }

    @Test
    void flakacNeedsLateArrivalAndSkippedThirdHalfInTheSameMatch() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Flákač test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);
        PlayerEntity accomplished = player("Flákač", appTeam);
        PlayerEntity onlyLate = player("Jen pozdní příchod", appTeam);
        MatchEntity match = match("Flákání", date(2025, 2, 1), appTeam);
        FineEntity late = fine("Pozdní příchod po 10. minutě", appTeam);
        FineEntity thirdHalf = fine("Třetí poločas", appTeam);
        receivedFine(accomplished, late, match, appTeam);
        receivedFine(accomplished, thirdHalf, match, appTeam);
        receivedFine(onlyLate, late, match, appTeam);

        assertThat(findFlakac(accomplished, match)).isNotNull();
        assertThat(findFlakac(onlyLate, match)).isNull();
    }

    @Test
    void maloCasuCountsCardGoalAndAssistAsThreeDistinctThings() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setName("Málo času test");
        appTeam = appTeamRepository.saveAndFlush(appTeam);
        PlayerEntity cardAndAssist = player("Karta a asistence", appTeam);
        PlayerEntity twoCardsOnly = player("Dvě karty jsou jedna věc", appTeam);
        PlayerEntity goalAndAssist = player("Gól a asistence", appTeam);
        PlayerEntity noLateArrival = player("Bez pozdního příchodu", appTeam);
        MatchEntity match = match("Hodně muziky", date(2025, 3, 1), appTeam);
        FineEntity late = fine("Pozdní příchod po začátku", appTeam);
        FineEntity yellow = fine("Žlutá karta", appTeam);
        FineEntity red = fine("Červená karta", appTeam);

        for (PlayerEntity player : List.of(cardAndAssist, twoCardsOnly, goalAndAssist)) {
            receivedFine(player, late, match, appTeam);
        }
        receivedFine(cardAndAssist, yellow, match, appTeam);
        receivedFine(twoCardsOnly, yellow, match, appTeam);
        receivedFine(twoCardsOnly, red, match, appTeam);
        goal(cardAndAssist, match, appTeam, 0, 1);
        goal(twoCardsOnly, match, appTeam, 0, 0);
        goal(goalAndAssist, match, appTeam, 1, 1);
        goal(noLateArrival, match, appTeam, 1, 1);

        assertThat(achievementRepository.findMaloCasuHodneMuziky(
                cardAndAssist.getId(), appTeam.getId(), match.getId())).isNotNull();
        assertThat(achievementRepository.findMaloCasuHodneMuziky(
                twoCardsOnly.getId(), appTeam.getId(), match.getId())).isNull();
        assertThat(achievementRepository.findMaloCasuHodneMuziky(
                goalAndAssist.getId(), appTeam.getId(), match.getId())).isNotNull();
        assertThat(achievementRepository.findMaloCasuHodneMuziky(
                noLateArrival.getId(), appTeam.getId(), match.getId())).isNull();
    }

    private IMatchIdNumberOneNumberTwo findFlakac(PlayerEntity player, MatchEntity match) {
        return achievementRepository.getMatchWithAtLeastOneOfFinesAndXSecondFines(
                player.getId(), match.getId(),
                "Pozdní příchod do začátku",
                "Pozdní příchod po začátku",
                "Pozdní příchod po 10. minutě",
                "Třetí poločas", 1
        );
    }

    private FineEntity fine(String name, AppTeamEntity appTeam) {
        FineEntity fine = new FineEntity();
        fine.setName(name);
        fine.setAmount(100);
        fine.setAppTeam(appTeam);
        return fineRepository.saveAndFlush(fine);
    }

    private PlayerEntity player(String name, AppTeamEntity appTeam) {
        PlayerEntity player = new PlayerEntity();
        player.setName(name);
        player.setBirthday(date(1990, 1, 1));
        player.setActive(true);
        player.setAppTeam(appTeam);
        return playerRepository.saveAndFlush(player);
    }

    private void goal(
            PlayerEntity player,
            MatchEntity match,
            AppTeamEntity appTeam,
            int goals,
            int assists
    ) {
        GoalEntity goal = new GoalEntity();
        goal.setPlayer(player);
        goal.setMatch(match);
        goal.setAppTeam(appTeam);
        goal.setGoalNumber(goals);
        goal.setAssistNumber(assists);
        goalRepository.saveAndFlush(goal);
    }

    private void beer(
            PlayerEntity player,
            MatchEntity match,
            AppTeamEntity appTeam,
            int beers,
            int liquors
    ) {
        BeerEntity beer = new BeerEntity();
        beer.setPlayer(player);
        beer.setMatch(match);
        beer.setAppTeam(appTeam);
        beer.setBeerNumber(beers);
        beer.setLiquorNumber(liquors);
        beerRepository.saveAndFlush(beer);
    }

    private MatchEntity match(String name, Date date, AppTeamEntity appTeam) {
        MatchEntity match = new MatchEntity();
        match.setName(name);
        match.setDate(date);
        match.setAppTeam(appTeam);
        return matchRepository.saveAndFlush(match);
    }

    private MatchEntity match(
            String name,
            Date date,
            SeasonEntity season,
            AppTeamEntity appTeam,
            List<PlayerEntity> players
    ) {
        MatchEntity match = new MatchEntity();
        match.setName(name);
        match.setDate(date);
        match.setSeason(season);
        match.setAppTeam(appTeam);
        match.setPlayerList(new ArrayList<>(players));
        return matchRepository.saveAndFlush(match);
    }

    private SeasonEntity season(AppTeamEntity appTeam) {
        SeasonEntity season = new SeasonEntity();
        season.setName("2024/2025 jaro");
        season.setFromDate(date(2025, 1, 1));
        season.setToDate(date(2025, 6, 30));
        season.setAppTeam(appTeam);
        return seasonRepository.saveAndFlush(season);
    }

    private void receivedFine(
            PlayerEntity player,
            FineEntity fine,
            MatchEntity match,
            AppTeamEntity appTeam
    ) {
        ReceivedFineEntity receivedFine = new ReceivedFineEntity();
        receivedFine.setPlayer(player);
        receivedFine.setFine(fine);
        receivedFine.setMatch(match);
        receivedFine.setAppTeam(appTeam);
        receivedFine.setFineNumber(1);
        receivedFineRepository.saveAndFlush(receivedFine);
    }

    private static Date date(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day)
                .atStartOfDay(PRAGUE)
                .toInstant());
    }
}
