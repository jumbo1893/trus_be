package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.response.SetupMatchResponse;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.BaseSeasonFilter;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.service.helper.PairSeasonMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.Date;
import java.util.List;


@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchCommandService matchCommandService;
    private final MatchQueryService matchQueryService;
    private final MatchSetupService matchSetupService;

    public void pairAllFootballMatches(AppTeamEntity appTeam) {
        matchCommandService.pairAllFootballMatches(appTeam);
    }

    public MatchDTO addMatch(MatchDTO matchDTO, AppTeamEntity appTeam) {
        return matchCommandService.addMatch(matchDTO, appTeam);
    }

    public MatchDTO editMatch(
            Long matchId,
            MatchDTO matchDTO,
            AppTeamEntity appTeam
    ) throws NotFoundException {
        return matchCommandService.editMatch(matchId, matchDTO, appTeam);
    }

    public void deleteMatch(Long matchId, AppTeamEntity appTeam) throws NotFoundException {
        matchCommandService.deleteMatch(matchId, appTeam);
    }

    public List<MatchDTO> getAll(MatchFilter matchFilter) {
        return matchQueryService.getAll(matchFilter);
    }

    public List<MatchEntity> getAllEntitiesBySeasonId(AppTeamEntity appTeam, Long seasonId) {
        return matchQueryService.getAllEntitiesBySeasonId(appTeam, seasonId);
    }

    public List<MatchDTO> getMatchesByDate(int limit, boolean desc, long appTeamId) {
        return matchQueryService.getMatchesByDate(limit, desc, appTeamId);
    }

    public List<PlayerDTO> getPlayerListByMatchId(Long matchId) {
        return matchQueryService.getPlayerListByMatchId(matchId);
    }

    public List<PlayerDTO> getPlayerListByFilteredByFansByMatchId(Long matchId, boolean fan) {
        return matchQueryService.getPlayerListByFilteredByFansByMatchId(matchId, fan);
    }

    public SetupMatchResponse setupMatch(Long matchId, AppTeamEntity appTeam) {
        return matchSetupService.setupMatch(matchId, appTeam);
    }

    public MatchDTO getMatch(long matchId) {
        return matchQueryService.getMatch(matchId);
    }

    public MatchEntity getMatchEntity(long matchId) {
        return matchQueryService.getMatchEntity(matchId);
    }

    public MatchDTO getFirstMatchWherePlayerAttends(PlayerDTO player) {
        return matchQueryService.getFirstMatchWherePlayerAttends(player);
    }

    public MatchDTO getLatestMatchBySeasonId(long seasonId, long appTeamId) {
        return matchQueryService.getLatestMatchBySeasonId(seasonId, appTeamId);
    }

    public PairSeasonMatch returnSeasonAndMatchByFilter(BaseSeasonFilter filter) {
        return matchSetupService.returnSeasonAndMatchByFilter(filter);
    }

    public List<Long> convertMatchesToIds(List<MatchDTO> matches) {
        return matchQueryService.convertMatchesToIds(matches);
    }

    public MatchDTO findMatchByFootballMatchId(long footballMatchId, long appTeamId) {
        return matchQueryService.findMatchByFootballMatchId(footballMatchId, appTeamId);
    }

    public MatchDTO findMatchByFootballMatchIdOrNull(long footballMatchId, long appTeamId) {
        return matchQueryService.findMatchByFootballMatchIdOrNull(footballMatchId, appTeamId);
    }

    public MatchEntity findMatchByAroundTime(AppTeamEntity appTeam, Date startTime, Date endTime) {
        return matchQueryService.findMatchByAroundTime(appTeam, startTime, endTime);
    }
}
