package com.jumbo.trus.service.football.match;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.dto.football.Organization;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.dto.football.detail.FootballMatchDetail;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.service.UpdateService;
import com.jumbo.trus.service.football.league.LeagueService;
import com.jumbo.trus.service.football.pkfl.task.RetrievePkflMatchesByLeague;
import com.jumbo.trus.service.football.pkfl.task.helper.FootballMatchTaskHelper;
import com.jumbo.trus.service.football.team.TeamService;
import com.jumbo.trus.service.helper.Pair;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private final FootballMatchDetailProcessor footballMatchDetailProcessor;

    public List<FootballMatchDTO> getAllMatches() {
        return footballMatchProcessor.getAllMatches();
    }

    public List<FootballMatchDetail> getNextAndLastFootballMatchDetail(AppTeamEntity appTeamEntity) {
        Long teamId = appTeamEntity.getTeam().getId();
        List<FootballMatchDetail> matches = new ArrayList<>();
        if (teamId != null) {
            FootballMatchDTO nextMatch = footballMatchProcessor.getNextMatch(teamId);
            if (nextMatch != null) {
                FootballMatchDetail nextFootballMatchDetail = getFootballMatchDetail(nextMatch.getId(), appTeamEntity, false);
                matches.add(nextFootballMatchDetail);
            }
            FootballMatchDTO lastMatch = footballMatchProcessor.getLastMatch(teamId);
            if (lastMatch != null) {
                FootballMatchDetail lastFootballMatchDetail = getFootballMatchDetail(lastMatch.getId(), appTeamEntity, false);
                matches.add(lastFootballMatchDetail);
            }

        }
        return matches;
    }

    public List<FootballMatchDTO> getNextMatches(AppTeamEntity appTeam) {
        Long teamId = appTeam.getTeam().getId();
        return footballMatchProcessor.getNextMatches(teamId);
    }

    public FootballMatchDetail getFootballMatchDetail(Long matchId, AppTeamEntity appTeam, boolean includeMutualMatches) {
        FootballMatchDetail footballMatchDetail = new FootballMatchDetail();
        FootballMatchDTO footballMatchDTO = footballMatchProcessor.getMatchById(matchId);
        enhanceTeamsInFootballMatchWithTableMatch(footballMatchDTO);
        footballMatchDetail.setFootballMatch(footballMatchDTO);
        return footballMatchDetailProcessor.enhanceFootballMatchDetail(footballMatchDetail, appTeam, includeMutualMatches);
    }

    public FootballMatchDTO getFootballMatchByDate(Date date, AppTeamEntity appTeam) {
        Long teamId = appTeam.getTeam().getId();
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(date);
        endCalendar.add(Calendar.DATE, 1);
        return footballMatchProcessor.findFootballMatchByDate(teamId, date, endCalendar.getTime());
    }

    public FootballMatchDTO getFootballMatchById(Long id) {
        return footballMatchProcessor.getMatchById(id);
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

    private void enhanceTeamsInFootballMatchWithTableMatch(FootballMatchDTO footballMatchDTO) {
        if (footballMatchDTO != null) {
            teamService.enhanceTeamWithFootballTeam(footballMatchDTO.getAwayTeam());
            teamService.enhanceTeamWithFootballTeam(footballMatchDTO.getHomeTeam());
        }
    }

    private void processMatches(List<FootballMatchTaskHelper> matches, LeagueDTO league) {
        logger.debug("celkem procházím {} zápasů z ligy {}, rok {}, id {}", matches.size(), league.getName(), league.getYear(), league.getId());
        MatchResultCounter matchResultCounter = new MatchResultCounter();
        for (FootballMatchTaskHelper match : matches) {
            Pair<TeamDTO, TeamDTO> teams = getHomeAndAwayTeam(match.getHomeTeamUri(), match.getAwayTeamUri());
            if (teams.getFirst() != null || teams.getSecond() != null) {
                matchResultCounter.addCounts(processSingleMatch(match, teams));
            }
        }
        cleanUpUnprocessedMatches(league, matchResultCounter.getProcessedMatchIds());
        logger.debug("počet plně zpracovaných zápasů: {}\n počet částečně zpracovaných zápasů: {}\n, počet nezpracovaných zápasů: {}",
                matchResultCounter.getFullyProcessedMatches(), matchResultCounter.getPartiallyProcessedMatches(), matchResultCounter.getUnprocessedMatches());
    }

    private Pair<MatchProcessingResult, Long> processSingleMatch(FootballMatchTaskHelper match, Pair<TeamDTO, TeamDTO> teams) {
        FootballMatchDTO footballMatchDTO = new FootballMatchDTO(match, teams.getFirst(), teams.getSecond());
        FootballMatchDTO repositoryMatch = getFootballMatchFromRepository(teams.getFirst().getId(), match.getRound(), match.getLeague().getId());
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
