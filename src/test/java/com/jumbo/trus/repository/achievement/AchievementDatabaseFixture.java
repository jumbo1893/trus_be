package com.jumbo.trus.repository.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.footbar.FootbarSessionEntity;
import com.jumbo.trus.entity.football.*;
import com.jumbo.trus.service.achievement.AchievementCalculator;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.achievement.helper.ScopedAchievementFunction;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.achievement.helper.ScopedSeasonAchievementFunction;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Real SQL and calculator decisions; only display/service dependencies are mocked. */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class AchievementDatabaseFixture {
    private static final AtomicLong SESSION_IDS = new AtomicLong(900000000);
    @Autowired protected EntityManager em;
    @Autowired protected PlayerAchievementRepository repository;
    protected AppTeamEntity team;
    protected PlayerEntity player;
    protected PlayerEntity teammate;
    protected MatchEntity match;
    protected AchievementCalculator calculator;
    private int matchOrder;
    private final Map<String, FineEntity> fineDefinitions = new HashMap<>();

    @BeforeEach
    void initializeFacts() throws Exception {
        team = new AppTeamEntity();
        team.setName("Achievement boundaries " + UUID.randomUUID());
        save(team);
        player = player(false);
        teammate = player(false);
        matchOrder = 0;
        fineDefinitions.clear();
        match = match(player, teammate);
        var constructor = AchievementCalculator.class.getDeclaredConstructors()[0];
        Object[] dependencies = Arrays.stream(constructor.getParameterTypes()).map(type -> mock(type)).toArray();
        calculator = (AchievementCalculator) constructor.newInstance(dependencies);
        ReflectionTestUtils.setField(calculator, "seasonClock", java.time.Clock.fixed(
                java.time.Instant.parse("2030-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        ReflectionTestUtils.setField(calculator, "playerAchievementRepository", repository);
        MatchService service = (MatchService) ReflectionTestUtils.getField(calculator, "matchService");
        when(service.getMatch(anyLong())).thenAnswer(invocation -> {
            MatchDTO dto = new MatchDTO();
            dto.setId(invocation.getArgument(0));
            return dto;
        });
        SeasonService seasonService = (SeasonService) ReflectionTestUtils.getField(calculator, "seasonService");
        when(seasonService.getSeason(anyLong())).thenAnswer(invocation -> {
            SeasonEntity season = em.find(SeasonEntity.class, (Long) invocation.getArgument(0));
            return new SeasonDTO(season.getId(), season.getName(), season.getFromDate(), season.getToDate());
        });
    }

    protected <T> T save(T entity) { em.persist(entity); em.flush(); return entity; }
    protected PlayerEntity player(boolean fan) {
        PlayerEntity entity = new PlayerEntity();
        entity.setName("Test " + UUID.randomUUID());
        entity.setBirthday(new Date(0));
        entity.setActive(true);
        entity.setFan(fan);
        entity.setAppTeam(team);
        return save(entity);
    }
    protected MatchEntity match(PlayerEntity... players) {
        MatchEntity entity = new MatchEntity();
        entity.setName("Boundary match " + matchOrder);
        entity.setDate(new Date(1700000000000L + (++matchOrder * 86400000L)));
        entity.setAppTeam(team);
        entity.setPlayerList(new ArrayList<>(List.of(players)));
        return save(entity);
    }
    protected void goal(PlayerEntity who, MatchEntity where, int goals, int assists) {
        GoalEntity entity = new GoalEntity();
        entity.setAppTeam(team); entity.setPlayer(who); entity.setMatch(where);
        entity.setGoalNumber(goals); entity.setAssistNumber(assists); save(entity);
    }
    protected void beer(PlayerEntity who, MatchEntity where, int beers, int shots) {
        BeerEntity entity = new BeerEntity();
        entity.setAppTeam(team); entity.setPlayer(who); entity.setMatch(where);
        entity.setBeerNumber(beers); entity.setLiquorNumber(shots); save(entity);
    }
    protected void fine(PlayerEntity who, MatchEntity where, String code, int count) {
        FineEntity fine = fineDefinitions.computeIfAbsent(code, ignored -> {
            FineEntity definition = new FineEntity();
            definition.setAppTeam(team); definition.setName(code); definition.setCode(code); definition.setAmount(10);
            return save(definition);
        });
        ReceivedFineEntity received = new ReceivedFineEntity();
        received.setAppTeam(team); received.setPlayer(who); received.setMatch(where);
        received.setFine(fine); received.setFineNumber(count); save(received);
    }
    protected FootballMatchPlayerEntity performance(PlayerEntity who, MatchEntity where, int minutes, boolean best, boolean clean) {
        if (where.getFootballMatch() == null) {
            FootballMatchEntity fm = new FootballMatchEntity(); fm.setDate(where.getDate());
            where.setFootballMatch(save(fm));
        }
        if (who.getFootballPlayer() == null) {
            FootballPlayerEntity fp = new FootballPlayerEntity(); fp.setName(who.getName());
            who.setFootballPlayer(save(fp));
        }
        FootballMatchPlayerEntity result = new FootballMatchPlayerEntity();
        result.setPlayer(who.getFootballPlayer()); result.setMatch(where.getFootballMatch());
        result.setGoalkeepingMinutes(minutes); result.setBestPlayer(best); result.setCleanSheet(clean);
        return save(result);
    }
    protected FootbarSessionEntity footbar(PlayerEntity who, MatchEntity where) {
        FootbarSessionEntity result = new FootbarSessionEntity();
        result.setFootbarSessionId(SESSION_IDS.incrementAndGet());
        result.setPlayer(who); result.setMatch(where); result.setDistance(0.0);
        result.setPassCount(0); result.setShotSpeed(0.0); result.setSprintSpeed(0.0);
        return save(result);
    }
    protected PlayerAchievementDTO calculate(String code, PlayerEntity who, MatchEntity where) {
        em.flush();
        @SuppressWarnings("unchecked")
        Map<String, ScopedAchievementFunction> functions = (Map<String, ScopedAchievementFunction>)
                ReflectionTestUtils.getField(calculator, "scopedAchievementCalculators");
        AchievementDTO achievement = new AchievementDTO();
        achievement.setId(1L); achievement.setCode(code); achievement.setName(code);
        PlayerDTO dto = new PlayerDTO(); dto.setId(who.getId()); dto.setName(who.getName()); dto.setFan(who.isFan());
        return functions.get(code).apply(dto, achievement, team, AchievementType.ALL, where.getId());
    }
    protected void assertMatch(String code, boolean expected) {
        PlayerAchievementDTO result = calculate(code, player, match);
        assertThat(result).as(code).isNotNull();
        assertThat(result.getAccomplished()).as(code).isEqualTo(expected);
        if (expected) {
            assertThat(result.getMatch().getId()).isEqualTo(match.getId());
            assertThat(calculate(code, player, match()).getAccomplished()).as("Wrong match: " + code).isFalse();
        }
    }

    protected PlayerAchievementDTO calculateSeason(String code, SeasonEntity season) {
        em.flush();
        @SuppressWarnings("unchecked")
        Map<String, ScopedSeasonAchievementFunction> functions = (Map<String, ScopedSeasonAchievementFunction>)
                ReflectionTestUtils.getField(calculator, "scopedSeasonAchievementCalculators");
        AchievementDTO achievement = new AchievementDTO(); achievement.setId(1L); achievement.setCode(code); achievement.setName(code);
        PlayerDTO dto = new PlayerDTO(); dto.setId(player.getId()); dto.setName(player.getName()); dto.setFan(player.isFan()); dto.setActive(true);
        return functions.get(code).apply(dto, achievement, team, AchievementType.ALL, season.getId());
    }
}
