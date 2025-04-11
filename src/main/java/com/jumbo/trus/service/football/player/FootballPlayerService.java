package com.jumbo.trus.service.football.player;

import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.repository.football.FootballPlayerRepository;
import com.jumbo.trus.entity.repository.football.TeamRepository;
import com.jumbo.trus.mapper.football.FootballPlayerMapper;
import com.jumbo.trus.mapper.football.TeamMapper;
import com.jumbo.trus.service.UpdateService;
import com.jumbo.trus.service.football.pkfl.task.RetrievePkflPlayer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FootballPlayerService {

    private final Logger logger = LoggerFactory.getLogger(FootballPlayerService.class);
    private static final String PLAYER_UPDATE = "player_update";

    private final FootballPlayerRepository footballPlayerRepository;
    private final FootballPlayerMapper footballPlayerMapper;
    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final UpdateService updateService;
    private final RetrievePkflPlayer retrievePkflPlayer;
    private final FootballPlayerProcessor footballPlayerProcessor;
    private final PlayerUpdateHelper playerUpdateHelper;

    public List<FootballPlayerDTO> getAllPlayers() {
        return footballPlayerRepository.findAll().stream().map(footballPlayerMapper::toDTO).toList();
    }

    public List<FootballPlayerDTO> getAllPlayersByCurrentTeam(AppTeamEntity appTeam) {
        return footballPlayerRepository.findAllByTeamId(appTeam.getTeam().getId()).stream().map(footballPlayerMapper::toDTO).toList();
    }

    public List<FootballPlayerDTO> getAllPastPlayersByCurrentTeam(AppTeamEntity appTeam) {
        return footballPlayerRepository.findAllByTeamIdWithInactive(appTeam.getTeam().getId()).stream().map(footballPlayerMapper::toDTO).toList();
    }

    public void updatePlayers() {
        logger.debug("updatuji hráče z PKFL");
        List<TeamDTO> teams = loadTeams();
        PlayerProcessingResult allAndUpdatedPlayers = processPlayers(teams);
        logger.debug("update hráčů dokončen, celkem se zkontrolovalo hráčů: {}", allAndUpdatedPlayers.getTotalPlayers());
        logger.debug("počet aktualizovaných hráčů: {}", allAndUpdatedPlayers.getUpdatedPlayers());
    }

    public FootballPlayerDTO getFootballerByUri(String playerUri) {
        if (footballPlayerRepository.existsByUri(playerUri)) {
            return footballPlayerMapper.toDTO(footballPlayerRepository.findByUri(playerUri));
        }
        return playerUpdateHelper.saveNewPlayer(retrievePkflPlayer.getPlayer(playerUri, null));
    }

    public Integer getAverageBirthYearOfTeam(long teamId) {
        Optional<Double> averageBirthYear = footballPlayerRepository.findAverageBirthYearByTeam(teamId);
        return averageBirthYear
                .map(Double::intValue)
                .orElse(null);
    }

    private List<TeamDTO> loadTeams() {
        return isNeededToLoadAllTeams() ? getAllTeams() : getAllTeamsFromCurrentSeason();
    }

    private List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream().map(teamMapper::toDTO).toList();
    }

    private List<TeamDTO> getAllTeamsFromCurrentSeason() {
        return teamRepository.findTeamsFromCurrentSeason().stream().map(teamMapper::toDTO).toList();
    }

    private boolean isNeededToLoadAllTeams() {
        logger.debug("isNeededToLoadAllTeams: {}", updateService.getUpdateByName(PLAYER_UPDATE) == null);
        return updateService.getUpdateByName(PLAYER_UPDATE) == null;
    }

    private PlayerProcessingResult processPlayers(List<TeamDTO> teamList) {
        int updatedFootballers = 0;
        int allFootballers = 0;
        for (TeamDTO team : teamList) {
            updatedFootballers += footballPlayerProcessor.processPlayer(team);
        }
        if (updatedFootballers > 0) {
            updateService.saveNewUpdate(PLAYER_UPDATE);
        }
        return new PlayerProcessingResult(allFootballers, updatedFootballers);
    }

}
