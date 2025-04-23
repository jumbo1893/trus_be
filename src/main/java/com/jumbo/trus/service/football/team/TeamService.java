package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.auth.AppTeamDTO;
import com.jumbo.trus.dto.auth.registration.LeagueWithTeams;
import com.jumbo.trus.dto.auth.registration.RegistrationSetup;
import com.jumbo.trus.dto.auth.registration.TeamWithAppTeams;
import com.jumbo.trus.dto.football.Organization;
import com.jumbo.trus.dto.football.TableTeamDTO;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.dto.football.detail.FootballTableTeamDetail;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.entity.repository.football.TeamRepository;
import com.jumbo.trus.mapper.football.TeamMapper;
import com.jumbo.trus.service.auth.AppTeamProvider;
import com.jumbo.trus.service.football.helper.TeamTableTeam;
import com.jumbo.trus.service.football.league.LeagueService;
import com.jumbo.trus.service.football.match.FootballMatchDetailProcessor;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final TeamProcessor teamProcessor;
    private final TeamRetriever teamRetriever;
    private final FootballMatchDetailProcessor footballMatchDetailProcessor;
    private final TableTeamProcessor tableTeamProcessor;
    private final LeagueService leagueService;
    private final AppTeamProvider appTeamProvider;

    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream().map(teamMapper::toDTO).toList();
    }

    public List<TableTeamDTO> getTable(Long teamId) {
        return teamProcessor.getTableTeamsByTeamId(teamId);
    }

    public List<TeamDTO> getAllTeamsFromCurrentSeason() {
        return teamRepository.findTeamsFromCurrentSeason().stream().map(teamMapper::toDTO).toList();
    }

    public RegistrationSetup getRegistrationSetup() {
        List<LeagueWithTeams> currentLeagues = leagueService
                .getAllLeagues(Organization.PKFL, true)
                .stream()
                .map(LeagueWithTeams::new)
                .toList();
        currentLeagues.forEach(this::enhanceLeagueWithTeams);
        Map<Long, TeamWithAppTeams> teamIdToTeamMap = currentLeagues.stream()
                .flatMap(league -> league.getTeamWithAppTeamsList().stream())
                .collect(Collectors.toMap(TeamWithAppTeams::getId, team -> team));

        appTeamProvider.getAllAppTeams().forEach(appTeam -> {
            TeamWithAppTeams team = teamIdToTeamMap.get(appTeam.getTeam().getId());
            if (team != null) {
                team.getAppTeamList().add(appTeam);
            }
        });

        AppTeamDTO primaryAppTeam = appTeamProvider.getLisciTrusAppTeam();
        return new RegistrationSetup(
                currentLeagues,
                new LeagueWithTeams(leagueService.getLeagueBy(primaryAppTeam.getTeam().getCurrentLeagueId())),
                new TeamWithAppTeams(primaryAppTeam.getTeam()),
                primaryAppTeam
        );
    }

    private void enhanceLeagueWithTeams(LeagueWithTeams leagueWithTeams) {
        List<TeamWithAppTeams> teams = teamRepository
                .findAllTeamsByCurrentLeagueId(leagueWithTeams.getId())
                .stream()
                .map(teamMapper::toDTO)
                .map(TeamWithAppTeams::new)
                .toList();
        leagueWithTeams.setTeamWithAppTeamsList(teams);
    }

    public TeamDTO getTeamByUri(String uri) {
        return teamRepository.existsByUri(uri)
                ? teamMapper.toDTO(teamRepository.findByUri(uri))
                : null;
    }

    public TeamEntity getTeamById(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(String.valueOf(id)));
    }

    public void enhanceTeamWithFootballTeam(TeamDTO teamDTO) {
        teamProcessor.enhanceTeamWithTableTeam(teamDTO);
    }

    public FootballTableTeamDetail getFootballTeamDetail(Long tableTeamId, AppTeamEntity appTeam) {
        FootballTableTeamDetail footballTableTeamDetail = new FootballTableTeamDetail();
        footballTableTeamDetail.setTableTeam(tableTeamProcessor.getTableTeamById(tableTeamId));
        return footballMatchDetailProcessor.enhanceFootballTeamDetail(footballTableTeamDetail, appTeam);
    }

    public void updateTeams() {
        log.debug("updatuji týmy z PKFL");
        List<TeamTableTeam> teamTableTeams = teamRetriever.retrieveTeams(teamRetriever.isUpdateNeeded());
        TeamUpdateResult teamUpdateResult = processTeams(teamTableTeams);
        log.debug("update týmů dokončen, celkem se prolezlo týmů: {}", teamTableTeams.size());
        log.debug("počet aktualizovaných týmů: {}", teamUpdateResult.getUpdatedTeams());
        log.debug("počet aktualizovaných tabulkovaných týmů: {}", teamUpdateResult.getUpdatedTableTeams());
    }

    private TeamUpdateResult processTeams(List<TeamTableTeam> teamTableTeams) {
        TeamUpdateResult teamUpdateResult = new TeamUpdateResult();
        for (TeamTableTeam teamTableTeam : teamTableTeams) {
            if (teamProcessor.isNewTeam(teamTableTeam.getTeam())) {
                teamProcessor.processNewTeam(teamTableTeam, teamUpdateResult);
            } else {
                teamProcessor.processExistingTeam(teamTableTeam, teamUpdateResult);
            }
        }

        return teamUpdateResult;
    }

}
