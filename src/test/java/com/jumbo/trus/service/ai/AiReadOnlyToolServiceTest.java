package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.ai.AiFineSummaryProjection;
import com.jumbo.trus.dto.ai.AiRepeatOpponentProjection;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.service.AttendanceService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.football.team.TeamService;
import com.jumbo.trus.service.goal.GoalService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.player.PlayerStatsFacade;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
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

    private AiReadOnlyToolService service;
    private AiToolContext context;

    @BeforeEach
    void setUp() {
        service = new AiReadOnlyToolService(
                objectMapper,
                matchRepository,
                receivedFineRepository,
                playerService,
                seasonService,
                goalService,
                beerService,
                receivedFineService,
                attendanceService,
                footballMatchService,
                footballPlayerStatsService,
                teamService,
                playerStatsFacade
        );

        AppTeamEntity appTeam = new AppTeamEntity();
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
}
