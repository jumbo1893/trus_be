package com.jumbo.trus.service.football.stats;

import com.jumbo.trus.dto.football.FootballMatchPlayerDTO;
import com.jumbo.trus.dto.football.stats.CardComment;
import com.jumbo.trus.dto.football.stats.FootballAllIndividualStats;
import com.jumbo.trus.dto.football.stats.FootballSumIndividualStats;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballMatchPlayerEntity;
import com.jumbo.trus.entity.repository.football.FootballMatchPlayerRepository;
import com.jumbo.trus.entity.repository.view.FootballSumIndividualStatsRepository;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.mapper.football.FootballMatchPlayerMapper;
import com.jumbo.trus.mapper.football.FootballSumIndividualStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FootballPlayerStatsService {

    private final FootballMatchPlayerRepository footballMatchPlayerRepository;
    private final FootballMatchMapper footballMatchMapper;
    private final FootballSumIndividualStatsMapper footballSumIndividualStatsMapper;
    private final FootballSumIndividualStatsRepository footballSumIndividualStatsRepository;
    private final FootballMatchPlayerMapper footballMatchPlayerMapper;

    public List<FootballAllIndividualStats> getPlayerStatsForTeam(boolean currentSeason, AppTeamEntity appTeam) {
        Long teamId = appTeam.getTeam().getId();
        List<FootballSumIndividualStats> footballSumIndividualStats = getIndividualStats(teamId, currentSeason);
        return footballSumIndividualStats.stream()
                .map(this::enhanceWithCardComments)
                .toList();
    }

    public FootballAllIndividualStats getPlayerStatsForPlayer(Long footballPlayerId, AppTeamEntity appTeamEntity) {
        Long teamId = appTeamEntity.getTeam().getId();
        return enhanceWithCardComments(footballSumIndividualStatsMapper.toDTO(footballSumIndividualStatsRepository.findPlayerStatsByTeamIdAndPlayerId(teamId, footballPlayerId)));
    }

    public FootballMatchPlayerDTO getBestPlayerWithoutGoals(Long footballPlayerId) {
        return footballMatchPlayerRepository.findBestPlayerWithoutGoalsByPlayer(footballPlayerId)
                .map(footballMatchPlayerMapper::toDTO)
                .orElse(null);
    }

    public FootballMatchPlayerDTO getRowIfPlayerScoresInThreeMatchesInRow(Long footballPlayerId, Long teamId) {
        return footballMatchPlayerRepository.findIfPlayerScoresInThreeMatchesInRow(footballPlayerId, teamId)
                .map(footballMatchPlayerMapper::toDTO)
                .orElse(null);
    }

    private List<FootballSumIndividualStats> getIndividualStats(Long teamId, boolean currentSeason) {
        if (currentSeason) {
            return footballSumIndividualStatsRepository.findAllByTeamInCurrentLeague(teamId).stream()
                    .map(footballSumIndividualStatsMapper::toDTO)
                    .toList();
        } else {
            return footballSumIndividualStatsRepository.findPlayerStatsByTeamId(teamId).stream()
                    .map(footballSumIndividualStatsMapper::toDTO)
                    .toList();
        }
    }

    private FootballAllIndividualStats enhanceWithCardComments(FootballSumIndividualStats stat) {
        if (stat.getYellowCards() > 0 || stat.getRedCards() > 0) {
            List<FootballMatchPlayerEntity> matchComments = getMatchComments(stat);
            List<CardComment> yellowComments = extractComments(matchComments, true);
            List<CardComment> redComments = extractComments(matchComments, false);
            return new FootballAllIndividualStats(stat, yellowComments, redComments);
        }
        return new FootballAllIndividualStats(stat);
    }

    private List<FootballMatchPlayerEntity> getMatchComments(FootballSumIndividualStats stat) {
        if (stat.getLeague() == null) {
            return footballMatchPlayerRepository.findAllCardComments(stat.getTeam().getId(), stat.getPlayer().getId());
        } else {
            return footballMatchPlayerRepository.findAllCardCommentsInLeague(
                    stat.getTeam().getId(),
                    stat.getLeague().getId(),
                    stat.getPlayer().getId());
        }
    }

    private List<CardComment> extractComments(List<FootballMatchPlayerEntity> matchComments, boolean isYellow) {
        return matchComments.stream()
                .filter(matchComment -> isYellow ? matchComment.getYellowCards() > 0 : matchComment.getRedCards() > 0)
                .map(matchComment -> new CardComment(
                        footballMatchMapper.toDTO(matchComment.getMatch()),
                        isYellow ? matchComment.getYellowCardComment() : matchComment.getRedCardComment()))
                .toList();
    }
}
