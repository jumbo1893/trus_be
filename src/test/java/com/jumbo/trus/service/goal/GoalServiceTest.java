package com.jumbo.trus.service.goal;

import com.jumbo.trus.dto.goal.multi.GoalListDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.GoalMapper;
import com.jumbo.trus.mapper.GoalSetupMapper;
import com.jumbo.trus.repository.GoalRepository;
import com.jumbo.trus.service.match.MatchService;
import com.jumbo.trus.service.notification.NotificationService;
import com.jumbo.trus.service.outbox.OutboxEventService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GoalServiceTest {

    private final GoalRepository goalRepository = mock(GoalRepository.class);
    private final GoalMapper goalMapper = mock(GoalMapper.class);
    private final GoalSetupMapper goalSetupMapper = mock(GoalSetupMapper.class);
    private final MatchService matchService = mock(MatchService.class);
    private final PlayerService playerService = mock(PlayerService.class);
    private final ReceivedFineService receivedFineService = mock(ReceivedFineService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final GoalDetailedStatsService goalDetailedStatsService = mock(GoalDetailedStatsService.class);
    private final OutboxEventService outboxEventService = mock(OutboxEventService.class);

    private final GoalService goalService = new GoalService(
            goalRepository,
            goalMapper,
            goalSetupMapper,
            matchService,
            playerService,
            receivedFineService,
            notificationService,
            goalDetailedStatsService,
            outboxEventService
    );

    @Test
    void doesNotCreateGoalEventWhenNothingChanged() {
        MatchDTO match = new MatchDTO();
        match.setName("Test");
        match.setSeasonId(5L);
        when(matchService.getMatch(10L)).thenReturn(match);

        goalService.addMultipleGoal(new GoalListDTO(10L, false, List.of()), mock(AppTeamEntity.class));

        verifyNoInteractions(outboxEventService);
    }
}
