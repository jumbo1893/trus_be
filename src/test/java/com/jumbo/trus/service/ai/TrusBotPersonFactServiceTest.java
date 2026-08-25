package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.dto.attendance.AttendanceDetailedResponse;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.player.stats.PlayerAchievementCount;
import com.jumbo.trus.dto.player.stats.PlayerBeerCount;
import com.jumbo.trus.dto.player.stats.PlayerGoalCount;
import com.jumbo.trus.dto.player.stats.PlayerStats;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.AttendanceService;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.player.PlayerStatsFacade;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrusBotPersonFactServiceTest {

    private final PlayerService playerService = mock(PlayerService.class);
    private final PlayerStatsFacade playerStatsFacade = mock(PlayerStatsFacade.class);
    private final AttendanceService attendanceService = mock(AttendanceService.class);
    private final BeerService beerService = mock(BeerService.class);
    private final ReceivedFineService receivedFineService = mock(ReceivedFineService.class);
    private final FootballPlayerStatsService footballPlayerStatsService = mock(FootballPlayerStatsService.class);

    private TrusBotPersonFactService service;
    private AppTeamEntity appTeam;
    private AiToolContext context;

    @BeforeEach
    void setUp() {
        service = new TrusBotPersonFactService(
                new ObjectMapper(),
                playerService,
                playerStatsFacade,
                attendanceService,
                beerService,
                receivedFineService,
                footballPlayerStatsService,
                new FixedRandom()
        );

        appTeam = new AppTeamEntity();
        appTeam.setId(11L);
        appTeam.setName("Liščí Trus");
        UserEntity user = new UserEntity();
        user.setId(7L);
        context = new AiToolContext(user, appTeam, null, null);

        when(attendanceService.getAllDetailed(any())).thenReturn(
                new AttendanceDetailedResponse(0, 0, List.of())
        );
        when(beerService.getAllDetailed(any())).thenReturn(
                new BeerDetailedResponse(0, 0, 0, 0, List.of())
        );
        when(receivedFineService.getAllDetailed(any())).thenReturn(
                new ReceivedFineDetailedResponse(0, 0, 0, 0, List.of())
        );
    }

    @Test
    void resolvesCivilNameInReverseDatabaseOrderAndAddsTwoInterviewFacts() {
        PlayerDTO marty = player(27L, "Marty", false, "Humpl Martin");
        when(playerService.getAll(11L)).thenReturn(List.of(marty));
        when(playerStatsFacade.setupPlayerStats(27L, appTeam, false)).thenReturn(stats());

        Map<String, Object> result = service.readRandomFacts("Martin Humpl", context);

        assertEquals("FOUND", result.get("status"));
        assertEquals(27L, person(result).get("player_id"));
        assertEquals(2, list(result, "database_facts").size());
        assertEquals(2, list(result, "interview_facts").size());
    }

    @Test
    void resolvesDatabaseNickname() {
        PlayerDTO marty = player(27L, "Marty", false, "Humpl Martin");
        when(playerService.getAll(11L)).thenReturn(List.of(marty));
        when(playerStatsFacade.setupPlayerStats(27L, appTeam, false)).thenReturn(stats());

        Map<String, Object> result = service.readRandomFacts("Marty", context);

        assertEquals("FOUND", result.get("status"));
        assertEquals(27L, person(result).get("player_id"));
    }

    @Test
    void unlinkedInterviewReturnsFourAnswersForItsOwnTeam() {
        when(playerService.getAll(11L)).thenReturn(List.of());

        Map<String, Object> result = service.readRandomFacts("Mára", context);

        assertEquals("FOUND", result.get("status"));
        assertNull(person(result).get("player_id"));
        assertEquals("Marek Vávra", person(result).get("official_name"));
        assertEquals(0, list(result, "database_facts").size());
        assertEquals(4, list(result, "interview_facts").size());
        verify(playerStatsFacade, never()).setupPlayerStats(anyLong(), any(), anyBoolean());
    }

    @Test
    void unlinkedInterviewIsNotVisibleToAnotherAppTeam() {
        appTeam.setName("Jiný tým");
        when(playerService.getAll(11L)).thenReturn(List.of());

        Map<String, Object> result = service.readRandomFacts("Mára", context);

        assertEquals("NOT_FOUND", result.get("status"));
    }

    @Test
    void fanFactsNeverReadOrReturnFines() {
        PlayerDTO fan = player(50L, "Kotelník", true, null);
        when(playerService.getAll(11L)).thenReturn(List.of(fan));
        when(playerStatsFacade.setupPlayerStats(50L, appTeam, false)).thenReturn(stats());

        Map<String, Object> result = service.readRandomFacts("Kotelník", context);

        assertEquals("FOUND", result.get("status"));
        assertEquals("fan", person(result).get("person_type"));
        verify(receivedFineService, never()).getAllDetailed(any());
        boolean containsFine = list(result, "database_facts").stream()
                .map(value -> (Map<?, ?>) value)
                .anyMatch(fact -> "fines".equals(fact.get("kind")));
        assertEquals(false, containsFine);
    }

    @Test
    void findsSpecificInterviewAnswerByInflectedTopic() {
        PlayerDTO jumbo = player(15L, "Jumbo", false, "Jandák Matěj");
        when(playerService.getAll(11L)).thenReturn(List.of(jumbo));

        Map<String, Object> result = service.searchInterviewAnswers(
                "Jumbo",
                "co si myslí o zimě",
                List.of("zima", "léto"),
                3,
                context
        );

        assertEquals("FOUND", result.get("status"));
        Map<?, ?> firstMatch = (Map<?, ?>) list(result, "matches").get(0);
        assertEquals(true, String.valueOf(firstMatch.get("question")).contains("Zima nebo léto"));
        assertEquals(false, String.valueOf(firstMatch.get("answer")).isBlank());
        assertEquals(false, ((Map<?, ?>) result.get("response_policy")).get("censor_interview_language"));
    }

    @Test
    void searchesOneRelevantAnswerPerInterviewAcrossWholeTeam() {
        when(playerService.getAll(11L)).thenReturn(linkedInterviewPlayers());

        Map<String, Object> result = service.searchInterviewAnswers(
                null,
                "kteří hráči hráli v mládí fotbal",
                List.of("fotbalové zkušenosti", "před Trusem", "mládí"),
                20,
                context
        );

        assertEquals("FOUND", result.get("status"));
        assertEquals(14, list(result, "matches").size());
        boolean allQuestionsAreAboutExperience = list(result, "matches").stream()
                .map(value -> (Map<?, ?>) value)
                .allMatch(match -> String.valueOf(match.get("question")).contains("fotbalov"));
        assertEquals(true, allQuestionsAreAboutExperience);
    }

    @Test
    void crossInterviewSearchKeepsLinkedPlayersButExcludesOtherTeamAndUnlinkedInterviews() {
        appTeam.setName("Jiný tým");
        when(playerService.getAll(11L)).thenReturn(List.of(player(15L, "Jumbo", false, "Jandák Matěj")));

        Map<String, Object> result = service.searchInterviewAnswers(
                null,
                "zima nebo léto",
                List.of("zima", "léto"),
                20,
                context
        );

        assertEquals("FOUND", result.get("status"));
        assertEquals(1, list(result, "matches").size());
        Map<?, ?> person = (Map<?, ?>) ((Map<?, ?>) list(result, "matches").get(0)).get("person");
        assertEquals(15L, person.get("player_id"));
    }

    @Test
    void reportsWhenTeamPlayerHasNoInterview() {
        when(playerService.getAll(11L)).thenReturn(List.of(player(999L, "Nováček", false, null)));

        Map<String, Object> result = service.searchInterviewAnswers(
                "Nováček",
                "zima",
                List.of("léto"),
                3,
                context
        );

        assertEquals("NO_INTERVIEW", result.get("status"));
    }

    private PlayerDTO player(long id, String nickname, boolean fan, String officialName) {
        PlayerDTO player = new PlayerDTO();
        player.setId(id);
        player.setName(nickname);
        player.setBirthday(Date.from(Instant.parse("1990-05-14T00:00:00Z")));
        player.setFan(fan);
        player.setActive(true);
        if (officialName != null) {
            FootballPlayerDTO footballPlayer = new FootballPlayerDTO();
            footballPlayer.setId(100L + id);
            footballPlayer.setName(officialName);
            player.setFootballPlayer(footballPlayer);
        }
        return player;
    }

    private PlayerStats stats() {
        PlayerStats stats = new PlayerStats();
        stats.setPlayerBeerCount(new PlayerBeerCount(17, 3));
        stats.setPlayerGoalCount(new PlayerGoalCount(8, 5));
        stats.setPlayerAchievementCount(new PlayerAchievementCount(20, 7));
        return stats;
    }

    private List<PlayerDTO> linkedInterviewPlayers() {
        return List.of(
                player(27L, "Marty", false, "Humpl Martin"),
                player(40L, "Haklos", false, "Hakl Lukáš"),
                player(36L, "Venca", false, "Kadleček Václav"),
                player(32L, "Sláva", false, "Slavata Michal"),
                player(19L, "Luky", false, "Novotný Lukáš"),
                player(12L, "Hajzlák", false, "Novák Jakub"),
                player(9L, "Flígl", false, "Flégl Jan"),
                player(15L, "Jumbo", false, "Jandák Matěj"),
                player(7L, "Dolly", false, "Doležal Jan"),
                player(24L, "Malyny", false, "Malý Jan"),
                player(16L, "Karloss", false, "Dvořák Karel"),
                player(2L, "Benny", false, "Beneš Petr")
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> person(Map<String, Object> result) {
        return (Map<String, Object>) result.get("person");
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Map<String, Object> result, String key) {
        return (List<Object>) result.get(key);
    }

    private static class FixedRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
