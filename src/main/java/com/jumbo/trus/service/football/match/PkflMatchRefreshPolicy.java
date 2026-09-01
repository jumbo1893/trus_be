package com.jumbo.trus.service.football.match;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.service.football.pkfl.task.RetrievePkflMatchDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class PkflMatchRefreshPolicy {

    private final Clock clock;
    private final Duration detailRefreshWindow;
    private final Duration missingRefereeCommentRefreshWindow;

    @Autowired
    public PkflMatchRefreshPolicy(
            @Value("${pkfl.jobs.match-detail-refresh-hours:72}") long detailRefreshHours,
            @Value("${pkfl.jobs.missing-referee-comment-refresh-hours:168}") long missingRefereeCommentRefreshHours
    ) {
        this(
                Clock.systemUTC(),
                Duration.ofHours(detailRefreshHours),
                Duration.ofHours(missingRefereeCommentRefreshHours)
        );
    }

    PkflMatchRefreshPolicy(
            Clock clock,
            Duration detailRefreshWindow,
            Duration missingRefereeCommentRefreshWindow
    ) {
        this.clock = clock;
        this.detailRefreshWindow = detailRefreshWindow;
        this.missingRefereeCommentRefreshWindow = missingRefereeCommentRefreshWindow;
    }

    public boolean shouldFetchDetails(FootballMatchDTO repositoryMatch, FootballMatchDTO webMatch) {
        if (!webMatch.isAlreadyPlayed()) {
            return false;
        }
        if (repositoryMatch == null || !repositoryMatch.equals(webMatch)) {
            return true;
        }
        if (isInsideRefreshWindow(webMatch.getDate(), detailRefreshWindow)) {
            return true;
        }
        return isRefereeCommentMissing(repositoryMatch)
                && isInsideRefreshWindow(webMatch.getDate(), missingRefereeCommentRefreshWindow);
    }

    private boolean isRefereeCommentMissing(FootballMatchDTO match) {
        String refereeComment = match.getRefereeComment();
        return refereeComment == null
                || refereeComment.isBlank()
                || RetrievePkflMatchDetail.NO_REFEREE_COMMENT.equals(refereeComment);
    }

    private boolean isInsideRefreshWindow(Date matchDate, Duration refreshWindow) {
        if (matchDate == null) {
            return false;
        }
        Instant playedAt = matchDate.toInstant();
        Instant now = clock.instant();
        return !playedAt.isAfter(now) && !playedAt.isBefore(now.minus(refreshWindow));
    }
}
