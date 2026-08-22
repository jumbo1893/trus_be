package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.ai.AiFineSummaryProjection;
import com.jumbo.trus.dto.ai.AiRepeatOpponentProjection;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.step.StepLeaderboardDTO;
import com.jumbo.trus.dto.step.StepLeaderboardResponseDTO;
import com.jumbo.trus.dto.step.StepPeriod;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.football.LeagueEntity;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.repository.TeamVisitedCountryProjection;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.service.AttendanceService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.StepService;
import com.jumbo.trus.service.UserVisitedCountryService;
import com.jumbo.trus.service.achievement.AchievementService;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.football.team.TeamService;
import com.jumbo.trus.service.goal.GoalService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.player.PlayerAchievementService;
import com.jumbo.trus.service.player.PlayerStatsFacade;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AiReadOnlyToolServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final ReceivedFineRepository receivedFineRepository = mock(ReceivedFineRepository.class);
    private final FootballMatchRepository footballMatchRepository = mock(FootballMatchRepository.class);
    private final PlayerService playerService = mock(PlayerService.class);
    private final SeasonService seasonService = mock(SeasonService.class);
    private final GoalService goalService = mock(GoalService.class);
    private final BeerService beerService = mock(BeerService.class);
    private final ReceivedFineService receivedFineService = mock(ReceivedFineService.class);
    private final AttendanceService attendanceService = mock(AttendanceService.class);
    private final FootballMatchService footballMatchService = mock(FootballMatchService.class);
    private final FootballPlayerStatsService footballPlayerStatsService = mock(FootballPlayerStatsService.class);
    private final TeamService teamService = mock(TeamService.class);
    private final PlayerStatsFacade playerStatsFacade = mock(PlayerStatsFacade.class);
    private final AchievementService achievementService = mock(AchievementService.class);
    private final PlayerAchievementService playerAchievementService = mock(PlayerAchievementService.class);
    private final StepService stepService = mock(StepService.class);
    private final UserVisitedCountryService userVisitedCountryService = mock(UserVisitedCountryService.class);

    private AiReadOnlyToolService service;
    private AiToolContext context;
    private AppTeamEntity appTeam;

    @BeforeEach
    void setUp() {
        service = new AiReadOnlyToolService(
                objectMapper,
                matchRepository,
                receivedFineRepository,
                footballMatchRepository,
                playerService,
                seasonService,
                goalService,
                beerService,
                receivedFineService,
                attendanceService,
                footballMatchService,
                footballPlayerStatsService,
                teamService,
                playerStatsFacade,
                achievementService,
                playerAchievementService,
                stepService,
                userVisitedCountryService
        );

        appTeam = new AppTeamEntity();
        appTeam.setId(11L);
        appTeam.setName("Trus");
        UserEntity user = new UserEntity();
        user.setId(7L);
        context = new AiToolContext(user, appTeam, null, null);
    }

    @Test
    void fineSummaryReturnsPreviousSeasonAndAllTimeInOneRead() throws Exception {
        Instant now = Instant.now();
        SeasonDTO current = new SeasonDTO(
                2L,
                "2026/27",
                Date.from(now.minus(30, ChronoUnit.DAYS)),
                Date.from(now.plus(30, ChronoUnit.DAYS))
        );
        SeasonDTO previous = new SeasonDTO(
                1L,
                "2025/26",
                Date.from(now.minus(400, ChronoUnit.DAYS)),
                Date.from(now.minus(100, ChronoUnit.DAYS))
        );
        when(seasonService.getAll(any())).thenReturn(List.of(current, previous));
        when(receivedFineRepository.findAiFineSummaryByName(11L, "Překop")).thenReturn(List.of(
                projection(10L, "Překop", current, 2L, 100L),
                projection(10L, "Překop", previous, 8L, 400L)
        ));

        String output = service.execute(
                "read_fine_summary",
                objectMapper.readTree("{\"fine_name\":\"Překop\"}"),
                context
        );
        JsonNode result = objectMapper.readTree(output);

        assertEquals(10, result.path("all_time").path("fine_count").asLong());
        assertEquals(8, result.path("previous_season").path("fine_count").asLong());
        assertEquals("2025/26", result.path("previous_season").path("name").asText());
        assertEquals(1, result.path("matched_fine_count").asLong());
        assertTrue(result.path("matched_fines").get(0).path("exact_name_match").asBoolean());
        verify(receivedFineRepository).findAiFineSummaryByName(11L, "Překop");
        verifyNoInteractions(receivedFineService);
    }

    @Test
    void findMatchesDoesNotPassNullFiltersToAStaticRepositoryQuery() throws Exception {
        when(matchRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<MatchEntity>>any(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        String output = service.execute("find_matches", objectMapper.readTree("{}"), context);

        assertTrue(objectMapper.readTree(output).isArray());
        verify(matchRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<MatchEntity>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void combinedMatchesPreferLinkedOfficialImportAndRemoveDuplicateManualMatch() throws Exception {
        TeamEntity trus = team(5L, "Trus");
        TeamEntity opponent = team(6L, "Soupeř");
        appTeam.setTeam(trus);
        Instant date = Instant.now().minus(10, ChronoUnit.DAYS);

        FootballMatchEntity official = footballMatch(100L, trus, opponent, null, date, true);
        MatchEntity manual = new MatchEntity();
        manual.setId(200L);
        manual.setName("Soupeř");
        manual.setDate(Date.from(date));
        manual.setFootballMatch(official);
        manual.setPlayerList(List.of());

        when(matchRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<MatchEntity>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(manual)));
        when(footballMatchRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<FootballMatchEntity>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(official)));

        JsonNode result = objectMapper.readTree(service.execute(
                "find_matches",
                objectMapper.readTree("{}"),
                context
        ));

        assertEquals(1, result.size());
        assertEquals("official_import", result.get(0).path("data_source").asText());
        assertEquals(100L, result.get(0).path("id").asLong());
    }

    @Test
    void achievementCatalogIsAvailableAsCompactReadOnlyData() throws Exception {
        AchievementDTO achievement = new AchievementDTO();
        achievement.setId(12L);
        achievement.setName("Kanón");
        achievement.setCode("KANON");
        achievement.setDescription("Vstřel deset gólů.");
        AchievementDetail detail = new AchievementDetail(
                achievement,
                20,
                3,
                0.15F,
                null,
                "Pepa, Karel, Matěj"
        );
        when(achievementService.getAllDetailedAchievements(11L)).thenReturn(List.of(detail));

        JsonNode result = objectMapper.readTree(service.execute(
                "read_achievements",
                objectMapper.readTree("""
                        {
                          "dataset": "catalog",
                          "player_id": null,
                          "search": "kanón",
                          "accomplished_only": null,
                          "limit": 30
                        }
                        """),
                context
        ));

        assertEquals(1, result.size());
        assertEquals("Kanón", result.get(0).path("name").asText());
        assertEquals(3, result.get(0).path("accomplished_count").asInt());
    }

    @Test
    void toolDefinitionsContainCombinedMatchesAndAchievements() {
        String definitions = service.toolDefinitions().toString();

        assertTrue(definitions.contains("find_matches"));
        assertTrue(definitions.contains("read_achievements"));
        assertTrue(definitions.contains("read_steps"));
        assertTrue(definitions.contains("read_visited_countries"));
    }

    @Test
    void stepLeaderboardUsesConsentAwareTeamService() throws Exception {
        when(stepService.getLeaderboardForTeam(StepPeriod.ALL_TIME, appTeam)).thenReturn(
                new StepLeaderboardResponseDTO(
                        StepPeriod.ALL_TIME,
                        null,
                        null,
                        null,
                        null,
                        List.of(new StepLeaderboardDTO(7L, "Matěj", 12345L, null, null))
                )
        );

        JsonNode result = objectMapper.readTree(service.execute(
                "read_steps",
                objectMapper.readTree("""
                        {
                          "dataset": "leaderboard",
                          "period": "ALL_TIME",
                          "from_date": null,
                          "to_date": null,
                          "limit": 30
                        }
                        """),
                context
        ));

        assertEquals(12345L, result.path("entries").get(0).path("stepCount").asLong());
        verify(stepService).getLeaderboardForTeam(StepPeriod.ALL_TIME, appTeam);
    }

    @Test
    void visitedCountriesTeamSummaryIsScopedAndSortedByCountryCount() throws Exception {
        PlayerDTO matej = new PlayerDTO();
        matej.setId(21L);
        matej.setName("Matěj");
        PlayerDTO pepa = new PlayerDTO();
        pepa.setId(22L);
        pepa.setName("Pepa");
        when(playerService.getAll(11L)).thenReturn(List.of(matej, pepa));

        TeamVisitedCountryProjection czechia = visitedCountry(21L, "Matěj", "CZ", "Česko", "EU");
        TeamVisitedCountryProjection germany = visitedCountry(21L, "Matěj", "DE", "Německo", "EU");
        TeamVisitedCountryProjection slovakia = visitedCountry(22L, "Pepa", "SK", "Slovensko", "EU");
        when(userVisitedCountryService.getTeamVisitedCountries(11L))
                .thenReturn(List.of(czechia, germany, slovakia));

        JsonNode result = objectMapper.readTree(service.execute(
                "read_visited_countries",
                objectMapper.readTree("""
                        {"dataset": "team", "player_id": null, "limit": 50}
                        """),
                context
        ));

        assertEquals(2, result.size());
        assertEquals("Matěj", result.get(0).path("player").asText());
        assertEquals(2L, result.get(0).path("country_count").asLong());
        assertEquals(1L, result.get(0).path("foreign_country_count").asLong());
    }

    @Test
    void repeatOpponentsAreReturnedInOneDatabaseRead() throws Exception {
        Instant now = Instant.now();
        SeasonDTO current = new SeasonDTO(
                2L,
                "2026/27",
                Date.from(now.minus(30, ChronoUnit.DAYS)),
                Date.from(now.plus(300, ChronoUnit.DAYS))
        );
        when(seasonService.getAll(any())).thenReturn(List.of(current));
        when(matchRepository.findAiRepeatOpponents(11L, 2L, current.getFromDate()))
                .thenReturn(List.of(repeatOpponent("FC Test", now)));

        String output = service.execute(
                "read_repeat_opponents",
                objectMapper.readTree("{}"),
                context
        );
        JsonNode result = objectMapper.readTree(output);

        assertEquals(1, result.path("repeat_opponent_count").asInt());
        assertEquals("FC Test", result.path("repeat_opponents").get(0).path("opponent").asText());
        assertEquals(3, result.path("repeat_opponents").get(0).path("historical_match_count").asLong());
        verify(matchRepository).findAiRepeatOpponents(11L, 2L, current.getFromDate());
    }

    @Test
    void repeatOpponentsPreferOfficialImportedData() throws Exception {
        LeagueEntity currentLeague = new LeagueEntity();
        currentLeague.setId(90L);
        currentLeague.setName("1. liga");
        currentLeague.setYear("2026/2027");
        currentLeague.setCurrentLeague(true);

        TeamEntity trus = team(5L, "Trus");
        trus.setCurrentLeague(currentLeague);
        TeamEntity opponent = team(6L, "Staří známí FC");
        appTeam.setTeam(trus);

        FootballMatchEntity currentMatch = footballMatch(
                100L,
                trus,
                opponent,
                currentLeague,
                Instant.now().plus(20, ChronoUnit.DAYS),
                false
        );
        LeagueEntity historicalLeague = new LeagueEntity();
        historicalLeague.setId(80L);
        historicalLeague.setYear("2024/2025");
        FootballMatchEntity historicalMatch = footballMatch(
                50L,
                opponent,
                trus,
                historicalLeague,
                Instant.now().minus(500, ChronoUnit.DAYS),
                true
        );

        when(footballMatchRepository.findAiTeamMatchesInLeague(5L, 90L))
                .thenReturn(List.of(currentMatch));
        when(footballMatchRepository.findAiPlayedTeamMatchesOutsideLeague(5L, 90L))
                .thenReturn(List.of(historicalMatch));

        JsonNode result = objectMapper.readTree(service.execute(
                "read_repeat_opponents",
                objectMapper.readTree("{}"),
                context
        ));

        assertEquals("official_import", result.path("data_source").asText());
        assertEquals(1, result.path("repeat_opponent_count").asInt());
        assertEquals(
                "Staří známí FC",
                result.path("repeat_opponents").get(0).path("opponent").asText()
        );
        verifyNoInteractions(matchRepository);
    }

    @Test
    void accessTiersExposeIncreasingToolRoundLimits() {
        assertEquals(6, com.jumbo.trus.entity.ai.AiAccessTier.STANDARD.getMaxToolRounds());
        assertEquals(10, com.jumbo.trus.entity.ai.AiAccessTier.PREMIUM.getMaxToolRounds());
        assertEquals(14, com.jumbo.trus.entity.ai.AiAccessTier.ULTRA.getMaxToolRounds());
    }

    private AiFineSummaryProjection projection(
            Long fineId,
            String fineName,
            SeasonDTO season,
            Long fineCount,
            Long totalAmount
    ) {
        return new AiFineSummaryProjection() {
            public Long getFineId() { return fineId; }
            public String getFineName() { return fineName; }
            public Long getSeasonId() { return season.getId(); }
            public String getSeasonName() { return season.getName(); }
            public Date getSeasonFrom() { return season.getFromDate(); }
            public Date getSeasonTo() { return season.getToDate(); }
            public Long getFineCount() { return fineCount; }
            public Long getTotalAmount() { return totalAmount; }
        };
    }

    private AiRepeatOpponentProjection repeatOpponent(String name, Instant now) {
        return new AiRepeatOpponentProjection() {
            public String getOpponent() { return name; }
            public Long getCurrentSeasonMatchCount() { return 2L; }
            public Date getFirstCurrentSeasonMatch() { return Date.from(now.plus(10, ChronoUnit.DAYS)); }
            public Long getHistoricalMatchCount() { return 3L; }
            public Date getLastHistoricalMatch() { return Date.from(now.minus(200, ChronoUnit.DAYS)); }
        };
    }

    private TeamVisitedCountryProjection visitedCountry(
            Long playerId,
            String playerName,
            String code,
            String name,
            String continentCode
    ) {
        TeamVisitedCountryProjection projection = mock(TeamVisitedCountryProjection.class);
        when(projection.getPlayerId()).thenReturn(playerId);
        when(projection.getPlayerName()).thenReturn(playerName);
        when(projection.getCode()).thenReturn(code);
        when(projection.getNameCs()).thenReturn(name);
        when(projection.getContinentCode()).thenReturn(continentCode);
        return projection;
    }

    private TeamEntity team(Long id, String name) {
        TeamEntity team = new TeamEntity();
        team.setId(id);
        team.setName(name);
        return team;
    }

    private FootballMatchEntity footballMatch(
            Long id,
            TeamEntity homeTeam,
            TeamEntity awayTeam,
            LeagueEntity league,
            Instant date,
            boolean alreadyPlayed
    ) {
        FootballMatchEntity match = new FootballMatchEntity();
        match.setId(id);
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setLeague(league);
        match.setDate(Date.from(date));
        match.setAlreadyPlayed(alreadyPlayed);
        return match;
    }
}
