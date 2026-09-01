package com.jumbo.trus.service.football.match;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.service.football.pkfl.task.RetrievePkflMatchDetail;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class PkflMatchRefreshPolicyTest {

    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");
    private final PkflMatchRefreshPolicy policy = new PkflMatchRefreshPolicy(
            Clock.fixed(NOW, ZoneOffset.UTC),
            Duration.ofHours(72),
            Duration.ofDays(7)
    );

    @Test
    void fetchesDetailsForNewPlayedMatch() {
        FootballMatchDTO webMatch = matchAt(NOW.minus(Duration.ofDays(10)), true, 2);

        assertThat(policy.shouldFetchDetails(null, webMatch)).isTrue();
    }

    @Test
    void skipsDetailsForUnplayedMatch() {
        FootballMatchDTO webMatch = matchAt(NOW.plus(Duration.ofHours(2)), false, null);

        assertThat(policy.shouldFetchDetails(null, webMatch)).isFalse();
    }

    @Test
    void refreshesUnchangedRecentMatchToPickUpDelayedPlayerStats() {
        FootballMatchDTO match = matchAt(NOW.minus(Duration.ofHours(48)), true, 2);

        assertThat(policy.shouldFetchDetails(match, match)).isTrue();
    }

    @Test
    void stopsRefreshingUnchangedOldMatch() {
        FootballMatchDTO match = matchAt(NOW.minus(Duration.ofHours(73)), true, 2);
        match.setRefereeComment("Komentář rozhodčího");

        assertThat(policy.shouldFetchDetails(match, match)).isFalse();
    }

    @Test
    void keepsRefreshingMissingRefereeCommentForSevenDays() {
        FootballMatchDTO match = matchAt(NOW.minus(Duration.ofDays(6)), true, 2);
        match.setRefereeComment(null);

        assertThat(policy.shouldFetchDetails(match, match)).isTrue();
    }

    @Test
    void recognizesPkflMissingCommentPlaceholder() {
        FootballMatchDTO match = matchAt(NOW.minus(Duration.ofDays(6)), true, 2);
        match.setRefereeComment(RetrievePkflMatchDetail.NO_REFEREE_COMMENT);

        assertThat(policy.shouldFetchDetails(match, match)).isTrue();
    }

    @Test
    void stopsRefreshingMissingRefereeCommentAfterSevenDays() {
        FootballMatchDTO match = matchAt(NOW.minus(Duration.ofDays(8)), true, 2);
        match.setRefereeComment(null);

        assertThat(policy.shouldFetchDetails(match, match)).isFalse();
    }

    @Test
    void fetchesDetailsWhenOldMatchResultChanges() {
        FootballMatchDTO repositoryMatch = matchAt(NOW.minus(Duration.ofDays(10)), true, 1);
        FootballMatchDTO webMatch = matchAt(NOW.minus(Duration.ofDays(10)), true, 2);

        assertThat(policy.shouldFetchDetails(repositoryMatch, webMatch)).isTrue();
    }

    private FootballMatchDTO matchAt(Instant date, boolean alreadyPlayed, Integer homeGoals) {
        TeamDTO homeTeam = new TeamDTO();
        homeTeam.setId(1L);
        TeamDTO awayTeam = new TeamDTO();
        awayTeam.setId(2L);
        LeagueDTO league = new LeagueDTO();
        league.setId(3L);

        FootballMatchDTO match = new FootballMatchDTO();
        match.setDate(Date.from(date));
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setLeague(league);
        match.setRound(1);
        match.setStadium("Stadion");
        match.setReferee("Rozhodčí");
        match.setHomeGoalNumber(homeGoals);
        match.setAwayGoalNumber(homeGoals == null ? null : 1);
        match.setUrlResult("https://pkfl.cz/zapas/1");
        match.setAlreadyPlayed(alreadyPlayed);
        return match;
    }
}
