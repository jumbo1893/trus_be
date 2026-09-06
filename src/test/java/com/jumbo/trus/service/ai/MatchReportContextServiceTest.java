package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.*;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.service.exceptions.AiUnavailableException;
import com.jumbo.trus.service.exceptions.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchReportContextServiceTest {
    private final FootballMatchRepository football = mock(FootballMatchRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final MatchReportContextService service = new MatchReportContextService(football, matches, mapper);
    private final AppTeamEntity team = new AppTeamEntity();
    private FootballMatchEntity target;

    @BeforeEach
    void setup() {
        team.setId(1L);
        team.setName("Trus");
        TeamEntity home = new TeamEntity(); home.setId(10L); home.setName("Trus");
        TeamEntity away = new TeamEntity(); away.setId(20L); away.setName("Soupeř");
        team.setTeam(home);
        LeagueEntity league = new LeagueEntity(); league.setId(30L); league.setName("Liga"); league.setYear("2025");
        target = new FootballMatchEntity(); target.setId(40L); target.setHomeTeam(home); target.setAwayTeam(away);
        target.setLeague(league); target.setDate(Date.from(Instant.parse("2025-05-01T12:00:00Z")));
        target.setAlreadyPlayed(true); target.setHomeGoalNumber(2); target.setAwayGoalNumber(1);
        target.setRefereeComment("Rozhodl závěr.");
        when(football.findById(40L)).thenReturn(Optional.of(target));
        when(matches.findAllByFootballMatchId(40L, 1L)).thenReturn(Optional.empty());
    }

    @Test
    void rejectsAnotherTeamsMatchBeforeReadingPrivateStatistics() {
        TeamEntity stranger = new TeamEntity(); stranger.setId(99L); team.setTeam(stranger);
        assertThrows(AuthException.class, () -> service.build(40L, team));
        verifyNoInteractions(matches);
    }

    @Test
    void rejectsUnplayedAndFutureMatches() {
        target.setAlreadyPlayed(false);
        assertThrows(AiUnavailableException.class, () -> service.build(40L, team));
        target.setAlreadyPlayed(true);
        target.setDate(Date.from(Instant.now().plusSeconds(3600)));
        assertThrows(AiUnavailableException.class, () -> service.build(40L, team));
    }

    @Test
    void excludesTargetFutureAndUnplayedGamesFromSeasonAndHeadToHead() throws Exception {
        var before = previous(41L, "2025-04-01T12:00:00Z");
        var after = previous(42L, "2025-06-01T12:00:00Z");
        var unplayed = previous(43L, "2025-03-01T12:00:00Z"); unplayed.setAlreadyPlayed(false);
        var games = List.of(target, before, after, unplayed);
        when(football.findAiTeamMatchesInLeague(anyLong(), eq(30L))).thenReturn(games);
        when(football.findReportForm(anyLong(), eq(target.getDate()), any())).thenReturn(games);
        when(football.findAlreadyPlayedMatchesOfTwoTeams(10L, 20L)).thenReturn(games);
        var json = mapper.readTree(service.build(40L, team));
        for (String key : List.of("homeSeasonBeforeMatch", "awaySeasonBeforeMatch", "previousHeadToHead", "homeFormLastFiveBeforeMatch")) {
            assertEquals(1, json.path(key).size());
            assertEquals(41L, json.path(key).get(0).path("id").asLong());
        }
        assertEquals("Rozhodl závěr.", json.path("match").path("refereeComment").asText());
        assertTrue(json.path("localMatchData").isNull());
    }

    @Test
    void includesLocalScoreFinesBeerAndWeatherWithoutOtherTeamData() throws Exception {
        var local = new MatchEntity(); local.setHomeGoalNumber(3); local.setAwayGoalNumber(2);
        var player = new PlayerEntity(); player.setName("Karel");
        var beer = new BeerEntity(); beer.setPlayer(player); beer.setAppTeam(team); beer.setBeerNumber(8); beer.setLiquorNumber(2);
        var other = new BeerEntity(); var otherTeam = new AppTeamEntity(); otherTeam.setId(99L);
        other.setAppTeam(otherTeam); other.setBeerNumber(100);
        local.setBeerList(List.of(beer, other));
        var fineType = new FineEntity(); fineType.setName("Zahozená šance"); fineType.setAmount(20);
        var fine = new ReceivedFineEntity(); fine.setPlayer(player); fine.setAppTeam(team); fine.setFine(fineType); fine.setFineNumber(2);
        local.setFineList(List.of(fine));
        var weather = new MatchWeatherEntity(); weather.setTemperature(new java.math.BigDecimal("12.5")); local.setWeather(weather);
        when(matches.findAllByFootballMatchId(40L, 1L)).thenReturn(Optional.of(local));
        var json = mapper.readTree(service.build(40L, team));
        assertEquals(3, json.path("match").path("homeGoals").asInt());
        var data = json.path("localMatchData");
        assertEquals(8, data.path("totalBeersRecorded").asInt());
        assertEquals(1, data.path("drinks").size());
        assertEquals("Zahozená šance", data.path("fines").get(0).path("situation").asText());
        assertEquals(12.5, data.path("weather").path("temperatureC").asDouble());
    }

    private FootballMatchEntity previous(long id, String date) {
        var m = new FootballMatchEntity(); m.setId(id); m.setDate(Date.from(Instant.parse(date)));
        m.setHomeTeam(target.getHomeTeam()); m.setAwayTeam(target.getAwayTeam());
        m.setAlreadyPlayed(true); m.setHomeGoalNumber(1); m.setAwayGoalNumber(0); return m;
    }

    @Test
    void linksOfficialStatisticsToNicknameAndLocalDrinksUsingIds() throws Exception {
        var official = new FootballPlayerEntity(); official.setId(501L); official.setName("Karel Novák");
        var localPlayer = new PlayerEntity(); localPlayer.setId(601L); localPlayer.setName("Karlos");
        localPlayer.setAppTeam(team); localPlayer.setFootballPlayer(official); official.setPlayer(localPlayer);
        var performance = new FootballMatchPlayerEntity(); performance.setPlayer(official);
        performance.setTeam(target.getHomeTeam()); performance.setGoals(2); target.setPlayerList(Set.of(performance));
        var beer = new BeerEntity(); beer.setPlayer(localPlayer); beer.setAppTeam(team); beer.setBeerNumber(3);
        var localMatch = new MatchEntity(); localMatch.setBeerList(List.of(beer));
        when(matches.findAllByFootballMatchId(40L, 1L)).thenReturn(Optional.of(localMatch));
        var json = mapper.readTree(service.build(40L, team));
        var identity = json.path("match").path("players").get(0).path("player");
        assertEquals("Karlos", identity.path("displayName").asText());
        assertEquals("Karel Novák", identity.path("officialName").asText());
        assertEquals(identity, json.path("localMatchData").path("drinks").get(0).path("player"));
        var otherTeam = new AppTeamEntity(); otherTeam.setId(99L); localPlayer.setAppTeam(otherTeam);
        var otherIdentity = mapper.readTree(service.build(40L, team)).path("match").path("players").get(0).path("player");
        assertFalse(otherIdentity.has("nickname"));
        assertEquals("Karel Novák", otherIdentity.path("displayName").asText());
    }
}
