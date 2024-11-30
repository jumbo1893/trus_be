package com.jumbo.trus.service.football.match;

import com.jumbo.trus.dto.football.*;
import com.jumbo.trus.service.UpdateService;
import com.jumbo.trus.service.football.league.LeagueService;
import com.jumbo.trus.service.football.team.TeamService;
import com.jumbo.trus.service.football.pkfl.task.RetrievePkflMatchesByLeague;
import com.jumbo.trus.service.football.pkfl.task.helper.FootballMatchTaskHelper;
import com.jumbo.trus.service.helper.Pair;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FootballMatchService {

    private final Logger logger = LoggerFactory.getLogger(FootballMatchService.class);

    private static final String MATCH_UPDATE = "match_update";

    private final FootballMatchProcessor footballMatchProcessor;
    private final LeagueService leagueService;
    private final TeamService teamService;
    private final UpdateService updateService;
    private final RetrievePkflMatchesByLeague retrievePkflMatches;

    public List<FootballMatchDTO> getAllMatches() {
        logger.debug("Fetching all football matches...");
        return footballMatchProcessor.getAllMatches();
    }

    public void updatePkflMatches() {
        logger.debug("Updating PKFL matches...");
        List<LeagueDTO> leagues = leagueService.getAllLeagues(Organization.PKFL, !isNeededToLoadAllLeagues());
        logger.debug("celkem načteno {} lig", leagues.size());
        for (LeagueDTO league : leagues) {
            processMatches(retrievePkflMatches.getMatches(league), league);
        }
        if (isNeededToLoadAllLeagues()) {
            setUpdateTag();
        }
    }

    private void processMatches(List<FootballMatchTaskHelper> matches, LeagueDTO league) {
        logger.debug("celkem procházím {} zápasů z ligy {}, rok {}, id {}", matches.size(), league.getName(), league.getYear(), league.getId());

        int fullyProcessedMatches = 0;
        int partiallyProcessedMatches = 0;
        int unprocessedMatches = 0;
        List<Long> processedMatchIds = new ArrayList<>();
        for (FootballMatchTaskHelper match : matches) {
            Pair<TeamDTO, TeamDTO> teams = getHomeAndAwayTeam(match.getHomeTeamUri(), match.getAwayTeamUri());
            if (teams.getFirst() != null || teams.getSecond() != null) {
                Pair<MatchProcessingResult, Long> result = processSingleMatch(match, teams, processedMatchIds);
                MatchProcessingResult processType = result.getFirst();
                processedMatchIds.add(result.getSecond());
                switch (processType) {
                    case FULLY_PROCESSED -> fullyProcessedMatches++;
                    case PARTIALLY_PROCESSED -> partiallyProcessedMatches++;
                    case UNPROCESSED -> unprocessedMatches++;
                }
            }
            int totalMatchesProcessed = fullyProcessedMatches + partiallyProcessedMatches + unprocessedMatches;
            if (totalMatchesProcessed > 0 && totalMatchesProcessed % 10 == 0) {
                logger.debug("Počet zpracovaných zápasů: {}", totalMatchesProcessed);
            }
        }
        cleanUpUnprocessedMatches(league, processedMatchIds);
        logger.debug("počet plně zpracovaných zápasů: {}\n počet částečně zpracovaných zápasů: {}\n, počet nezpracovaných zápasů: {}", fullyProcessedMatches, partiallyProcessedMatches, unprocessedMatches);
    }

    private Pair<MatchProcessingResult, Long> processSingleMatch(FootballMatchTaskHelper match, Pair<TeamDTO, TeamDTO> teams, List<Long> processedMatchIds) {
        FootballMatchDTO footballMatchDTO = new FootballMatchDTO(match, teams.getFirst(), teams.getSecond());
        FootballMatchDTO repositoryMatch = getFootballMatchFromRepository(teams.getFirst().getId(), match.getRound(), match.getLeagueId());
        return footballMatchProcessor.processMatch(repositoryMatch, footballMatchDTO); // Return the process type (0, 1, or other)
    }

    private void cleanUpUnprocessedMatches(LeagueDTO league, List<Long> processedMatchIds) {
        footballMatchProcessor.cleanAllMatchesNotBelongingToLeague(league.getId(), processedMatchIds);
    }

    private boolean isNeededToLoadAllLeagues() {
        return updateService.getUpdateByName(MATCH_UPDATE) == null;
    }

    private void setUpdateTag() {
        updateService.saveNewUpdate(MATCH_UPDATE);
    }

    private FootballMatchDTO getFootballMatchFromRepository(Long homeTeamId, Integer round, Long leagueId) {
        return footballMatchProcessor.findMatchByHomeTeamAndRound(homeTeamId, round, leagueId);
    }

    private Pair<TeamDTO, TeamDTO> getHomeAndAwayTeam(String homeTeamUri, String awayTeamUri) {
        TeamDTO homeTeam = teamService.getTeamByUri(homeTeamUri);
        TeamDTO awayTeam = teamService.getTeamByUri(awayTeamUri);
        return new Pair<>(homeTeam, awayTeam);
    }
}
