package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.response.SetupMatchResponse;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.BaseSeasonFilter;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.entity.participation.MatchParticipationEntity;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.participation.MatchParticipationRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.helper.PairSeasonMatch;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;

@Service
@RequiredArgsConstructor
public class MatchSetupService {

    private final MatchQueryService matchQueryService;
    private final MatchMapper matchMapper;
    private final FootballMatchMapper footballMatchMapper;
    private final SeasonService seasonService;
    private final PlayerService playerService;
    private final FootballMatchService footballMatchService;
    private final MatchParticipationRepository matchParticipationRepository;

    public SetupMatchResponse setupMatch(
            Long matchId,
            Long footballMatchId,
            AppTeamEntity appTeam
    ) {
        SetupMatchResponse response = new SetupMatchResponse();

        if (matchId == null) {
            response.setPrimarySeason(seasonService.getAutomaticSeason());
        } else {
            fillExistingMatch(response, matchId, appTeam);
        }

        fillSelectableValues(response, appTeam);
        if (matchId == null && footballMatchId != null) {
            fillAttendingParticipants(response, footballMatchId, appTeam);
        }
        return response;
    }

    public PairSeasonMatch returnSeasonAndMatchByFilter(BaseSeasonFilter filter) {
        if (filter.getMatchId() != null) {
            return pairForSelectedMatch(filter);
        }

        SeasonDTO primarySeason = filter.getSeasonId() != null
                ? seasonService.getSeason(filter.getSeasonId())
                : seasonService.getCurrentSeason(true, filter.getAppTeam());

        MatchDTO match = matchQueryService.getLatestMatchBySeasonId(
                primarySeason.getId(),
                filter.getAppTeam().getId()
        );

        if (match != null) {
            return new PairSeasonMatch(primarySeason, match);
        }

        MatchDTO fallbackMatch = matchQueryService.getLatestMatchBySeasonId(
                ALL_SEASON_ID,
                filter.getAppTeam().getId()
        );

        if (fallbackMatch == null) {
            return new PairSeasonMatch(primarySeason, null);
        }

        SeasonDTO fallbackSeason = seasonService.getSeason(fallbackMatch.getSeasonId());
        return new PairSeasonMatch(fallbackSeason, fallbackMatch);
    }

    private PairSeasonMatch pairForSelectedMatch(BaseSeasonFilter filter) {
        MatchDTO match = matchQueryService.getMatch(
                filter.getMatchId(),
                filter.getAppTeam().getId()
        );
        Long seasonId = filter.getSeasonId() != null
                ? filter.getSeasonId()
                : match.getSeasonId();

        return new PairSeasonMatch(
                seasonService.getSeason(seasonId),
                match
        );
    }

    private void fillExistingMatch(
            SetupMatchResponse response,
            Long matchId,
            AppTeamEntity appTeam
    ) {
        MatchEntity match = matchQueryService.getMatchEntity(matchId, appTeam.getId());

        response.setMatch(matchMapper.toDTO(match));
        response.setPrimarySeason(seasonService.getSeason(match.getSeason().getId()));
        response.setFootballMatch(
                match.getFootballMatch() == null
                        ? footballMatchService.getFootballMatchByDate(match.getDate(), appTeam)
                        : footballMatchMapper.toDTO(match.getFootballMatch())
        );
    }

    private void fillSelectableValues(
            SetupMatchResponse response,
            AppTeamEntity appTeam
    ) {
        SeasonFilter seasonFilter = new SeasonFilter(false, true, true);
        seasonFilter.setAppTeam(appTeam);

        response.setSeasonList(seasonService.getAll(seasonFilter));
        response.setFanList(playerService.getAllByFan(true, appTeam.getId()));
        response.setPlayerList(playerService.getAllByFan(false, appTeam.getId()));
    }

    private void fillAttendingParticipants(
            SetupMatchResponse response,
            Long footballMatchId,
            AppTeamEntity appTeam
    ) {
        Set<Long> attendingPlayerIds = matchParticipationRepository
                .findAllByFootballMatchIdAndAppTeamIdAndStatusOrderByPlayerNameAsc(
                        footballMatchId,
                        appTeam.getId(),
                        MatchParticipationStatus.ATTENDING
                )
                .stream()
                .map(MatchParticipationEntity::getPlayer)
                .filter(player -> player != null && !player.isDeleted())
                .map(player -> player.getId())
                .collect(Collectors.toSet());

        response.setAttendingPlayers(response.getPlayerList().stream()
                .filter(player -> attendingPlayerIds.contains(player.getId()))
                .toList());
        response.setAttendingFans(response.getFanList().stream()
                .filter(player -> attendingPlayerIds.contains(player.getId()))
                .toList());
    }
}
