package com.jumbo.trus.service.football.match;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.FootballMatchPlayerDTO;
import com.jumbo.trus.dto.football.detail.BestScorer;
import com.jumbo.trus.entity.football.detail.BestScorerView;
import com.jumbo.trus.entity.repository.football.BestScorerViewRepository;
import com.jumbo.trus.entity.repository.football.FootballMatchPlayerRepository;
import com.jumbo.trus.mapper.football.BestScorerViewMapper;
import com.jumbo.trus.mapper.football.FootballMatchPlayerMapper;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.football.pkfl.task.helper.FootballMatchDetailTaskHelper;
import com.jumbo.trus.service.football.pkfl.task.helper.PlayerMatchStatsHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.jumbo.trus.dto.football.TeamDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerProcessor {

    private final FootballMatchPlayerRepository footballMatchPlayerRepository;
    private final FootballMatchPlayerMapper footballMatchPlayerMapper;
    private final FootballPlayerService footballPlayerService;
    private final BestScorerViewRepository bestScorerViewRepository;
    private final BestScorerViewMapper bestScorerViewMapper;


    public List<FootballMatchPlayerDTO> processPlayers(FootballMatchDetailTaskHelper detail, Long matchId, FootballMatchDTO footballMatchDTO) {
        List<FootballMatchPlayerDTO> players = new ArrayList<>();
        players.addAll(getListOfFootballers(detail.getHomeTeamPlayers(), matchId, footballMatchDTO.getHomeTeam()));
        players.addAll(getListOfFootballers(detail.getAwayTeamPlayers(), matchId, footballMatchDTO.getAwayTeam()));
        return savePlayerList(matchId, players);
    }

    public BestScorer findBestScorerByTeamIdAndLeagueId(long teamId, long leagueId) {
        bestScorerViewRepository.findBestScorerByTeamAndLeague(teamId, leagueId);
        Optional<BestScorerView> bestScorer = bestScorerViewRepository.findBestScorerByTeamAndLeague(teamId, leagueId);
        return bestScorer.map(bestScorerViewMapper::toDTO)
                .orElse(null);
    }

    private List<FootballMatchPlayerDTO> savePlayerList(Long matchId, List<FootballMatchPlayerDTO> playerList) {
        List<FootballMatchPlayerDTO> newPlayerList = new ArrayList<>();
        for (FootballMatchPlayerDTO player : playerList) {
            Optional<Long> playerId = footballMatchPlayerRepository.findFirstIdByTeamAndMatchAndPlayer(
                player.getTeam().getId(), player.getMatchId(), player.getPlayer().getId()
            );
            playerId.ifPresentOrElse(
                id -> {
                    player.setId(id);
                    newPlayerList.add(savePlayerToRepository(player));
                },
                () -> newPlayerList.add(savePlayerToRepository(player))
            );
        }
        cleanAllPlayerNotBelongingToMatch(matchId, newPlayerList);
        return newPlayerList;
    }

    private void cleanAllPlayerNotBelongingToMatch(Long matchId, List<FootballMatchPlayerDTO> newPlayerList) {
        if (newPlayerList == null || newPlayerList.isEmpty()) {
            return;
        }
        log.debug("smazáno přebytečných hráčů: {} ", footballMatchPlayerRepository.deleteByMatchIdAndMatchPlayerIdNotIn(matchId, convertToIdList(newPlayerList)));
    }

    private List<Long> convertToIdList(List<FootballMatchPlayerDTO> newPlayerList) {
        if (newPlayerList == null || newPlayerList.isEmpty()) {
            return List.of(); // Vrátí prázdný seznam, pokud je vstup prázdný nebo null
        }
        return newPlayerList.stream()
                .map(FootballMatchPlayerDTO::getId) // Získá hodnotu `id` z každého objektu `FootballMatchPlayerDTO`
                .collect(Collectors.toList());     // Převede stream na seznam
    }



    private FootballMatchPlayerDTO savePlayerToRepository(FootballMatchPlayerDTO player) {
        return footballMatchPlayerMapper.toDTO(footballMatchPlayerRepository.save(footballMatchPlayerMapper.toEntity(player)));
    }

    private List<FootballMatchPlayerDTO> getListOfFootballers(List<PlayerMatchStatsHelper> playerHelpers, Long matchId, TeamDTO team) {
        List<FootballMatchPlayerDTO> footballers = new ArrayList<>();
        for (PlayerMatchStatsHelper playerHelper : playerHelpers) {
            FootballMatchPlayerDTO footballMatchPlayerDTO = new FootballMatchPlayerDTO(
                playerHelper,
                footballPlayerService.getFootballerByUri(playerHelper.getPlayerUri()),
                team,
                matchId
            );
            footballers.add(footballMatchPlayerDTO);
        }
        return footballers;
    }
}
