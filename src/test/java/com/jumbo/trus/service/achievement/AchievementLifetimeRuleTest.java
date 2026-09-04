package com.jumbo.trus.service.achievement;

import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.achievement.AchievementMapper;
import com.jumbo.trus.mapper.achievement.PlayerAchievementMapper;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import com.jumbo.trus.repository.achievement.PlayerAchievementRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.achievement.helper.AchievementType;
import com.jumbo.trus.service.achievement.helper.IMatchIdNumberOneNumberTwo;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.fine.FineService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.goal.GoalService;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.membership.MembershipService;
import com.jumbo.trus.service.notification.push.maker.AchievementNotificationMaker;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementLifetimeRuleTest {

    @Mock private BeerService beerService;
    @Mock private AchievementMapper achievementMapper;
    @Mock private AchievementRepository achievementRepository;
    @Mock private PlayerAchievementRepository playerAchievementRepository;
    @Mock private PlayerAchievementMapper playerAchievementMapper;
    @Mock private FootballMatchService footballMatchService;
    @Mock private MatchService matchService;
    @Mock private SeasonService seasonService;
    @Mock private FootballPlayerStatsService footballPlayerStatsService;
    @Mock private ReceivedFineService receivedFineService;
    @Mock private FineService fineService;
    @Mock private GoalService goalService;
    @Mock private AchievementNotificationMaker achievementNotificationMaker;
    @Mock private StepAchievementCalculator stepAchievementCalculator;
    @Mock private MembershipService membershipService;

    @InjectMocks private AchievementCalculator calculator;

    private PlayerDTO player;
    private AppTeamEntity appTeam;

    @BeforeEach
    void setUp() {
        player = new PlayerDTO();
        player.setId(7L);
        appTeam = new AppTeamEntity();
        appTeam.setId(11L);
    }

    @Test
    void maratonecUsesThe42100MeterCumulativeMilestoneReturnedByTheQuery() {
        when(playerAchievementRepository.findMaratonec(7L, 11L))
                .thenReturn(numbers(101L, 42_100, 42_100));
        MatchDTO match = new MatchDTO();
        match.setId(101L);
        when(matchService.getMatch(101L)).thenReturn(match);

        PlayerAchievementDTO result = invoke("calculateMARATONECAchievement", AchievementCodes.MARATONEC);

        assertThat(result.getAccomplished()).isTrue();
        assertThat(result.getMatch().getId()).isEqualTo(101L);
    }

    @Test
    void firstPlayerOfTheMatchAwardIsStoredAgainstTheFootballMatch() {
        when(playerAchievementRepository.findFirstBestPlayerMatch(7L, 11L))
                .thenReturn(numbers(201L, 1, 0));
        FootballMatchDTO footballMatch = new FootballMatchDTO();
        footballMatch.setId(201L);
        when(footballMatchService.getFootballMatchById(201L)).thenReturn(footballMatch);

        PlayerAchievementDTO result = invoke(
                "calculateHVEZDA_CO_SE_NEZDAAchievement",
                AchievementCodes.HVEZDA_CO_SE_NEZDA
        );

        assertThat(result.getAccomplished()).isTrue();
        assertThat(result.getFootballMatch().getId()).isEqualTo(201L);
    }

    private PlayerAchievementDTO invoke(String method, String code) {
        AchievementDTO achievement = new AchievementDTO();
        achievement.setCode(code);
        return ReflectionTestUtils.invokeMethod(
                calculator, method, player, achievement, appTeam, AchievementType.ALL);
    }

    private static IMatchIdNumberOneNumberTwo numbers(long matchId, int first, int second) {
        return new IMatchIdNumberOneNumberTwo() {
            @Override public Long getMatchId() { return matchId; }
            @Override public Integer getFirstNumber() { return first; }
            @Override public Integer getSecondNumber() { return second; }
        };
    }
}
