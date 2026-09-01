package com.jumbo.trus.service.football.pkfl.job;

import com.jumbo.trus.service.football.league.LeagueService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.team.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PkflScheduledJobTest {

    private final LeagueService leagueService = mock(LeagueService.class);
    private final TeamService teamService = mock(TeamService.class);
    private final FootballPlayerService footballPlayerService = mock(FootballPlayerService.class);
    private final FootballMatchService footballMatchService = mock(FootballMatchService.class);
    private final PkflScheduledJob job = new PkflScheduledJob(
            leagueService,
            teamService,
            footballPlayerService,
            footballMatchService
    );

    @Test
    void dailyReferenceJobDoesNotRunExpensiveMatchSynchronization() {
        job.runReferenceDataJob();

        verify(leagueService).updatePkflLeagues();
        verify(teamService).updateTeams();
        verify(footballPlayerService).updatePlayers();
        verify(footballMatchService, never()).updatePkflMatches();
    }

    @Test
    void frequentMatchJobOnlyUpdatesMatches() {
        job.runScheduledMatchJob();

        verify(footballMatchService).updatePkflMatches();
        verify(leagueService, never()).updatePkflLeagues();
        verify(teamService, never()).updateTeams();
        verify(footballPlayerService, never()).updatePlayers();
    }

    @Test
    void weekendMatchScheduleKeepsHourlyDaytimeUpdates() throws NoSuchMethodException {
        Method method = PkflScheduledJob.class.getMethod("runScheduledMatchJob");
        Scheduled[] schedules = method.getAnnotationsByType(Scheduled.class);

        assertThat(schedules)
                .extracting(Scheduled::cron)
                .containsExactlyInAnyOrder(
                        "${pkfl.jobs.matches-weekday-cron:0 0 1,13 * * MON-FRI}",
                        "${pkfl.jobs.matches-weekend-cron:0 0 7-23 * * SAT,SUN}"
                );
        assertThat(schedules)
                .extracting(Scheduled::zone)
                .containsOnly("${pkfl.jobs.zone:Europe/Prague}");
    }
}
