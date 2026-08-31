package com.jumbo.trus.repository.participation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional
class MatchParticipationCleanupRepositoryQueryTest {

    private static final long NON_EXISTING_LEAGUE_ID = -1L;

    @Autowired
    private MatchParticipationCommentReactionRepository reactionRepository;

    @Autowired
    private MatchParticipationCommentRepository commentRepository;

    @Autowired
    private MatchParticipationRepository participationRepository;

    @Test
    void obsoleteParticipationCleanupQueriesProduceValidSql() {
        List<Long> retainedMatchIds = List.of(-1L, -2L, -3L);

        reactionRepository.deleteObsoleteByLeague(NON_EXISTING_LEAGUE_ID, retainedMatchIds);
        commentRepository.deleteObsoleteByLeague(NON_EXISTING_LEAGUE_ID, retainedMatchIds);
        participationRepository.deleteObsoleteByLeague(NON_EXISTING_LEAGUE_ID, retainedMatchIds);
    }
}
