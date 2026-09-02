package com.jumbo.trus.service.football.team;

import com.jumbo.trus.dto.auth.AppTeamDTO;
import com.jumbo.trus.dto.auth.registration.RegistrationSetup;
import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.dto.football.Organization;
import com.jumbo.trus.dto.football.TeamDTO;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.mapper.football.TeamMapper;
import com.jumbo.trus.repository.football.TeamRepository;
import com.jumbo.trus.service.auth.AppTeamProvider;
import com.jumbo.trus.service.football.league.LeagueService;
import com.jumbo.trus.service.football.match.FootballMatchDetailProcessor;
import com.jumbo.trus.service.football.tablezone.FootballTableZoneEnricher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamServiceRegistrationCompatibilityTest {

    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamMapper teamMapper = mock(TeamMapper.class);
    private final TeamProcessor teamProcessor = mock(TeamProcessor.class);
    private final TeamRetriever teamRetriever = mock(TeamRetriever.class);
    private final FootballMatchDetailProcessor footballMatchDetailProcessor =
            mock(FootballMatchDetailProcessor.class);
    private final TableTeamProcessor tableTeamProcessor = mock(TableTeamProcessor.class);
    private final LeagueService leagueService = mock(LeagueService.class);
    private final AppTeamProvider appTeamProvider = mock(AppTeamProvider.class);
    private final FootballTableZoneEnricher footballTableZoneEnricher =
            mock(FootballTableZoneEnricher.class);
    private final TeamService service = new TeamService(
            teamRepository,
            teamMapper,
            teamProcessor,
            teamRetriever,
            footballMatchDetailProcessor,
            tableTeamProcessor,
            leagueService,
            appTeamProvider,
            footballTableZoneEnricher
    );

    private AppTeamDTO publicTeam;

    @BeforeEach
    void setUp() {
        TeamDTO publicFootballTeam = teamDto(101L, "Liščí Trus");
        TeamDTO privateFootballTeam = teamDto(102L, "Soukromý tým");
        publicTeam = new AppTeamDTO(1L, "Liščí Trus", null, null, publicFootballTeam);

        LeagueDTO league = new LeagueDTO(
                5L,
                "Liga",
                1,
                Organization.PKFL,
                "Praha",
                "liga",
                "2026",
                List.of(),
                true
        );
        TeamEntity publicEntity = teamEntity(101L);
        TeamEntity privateEntity = teamEntity(102L);

        when(appTeamProvider.getLisciTrusAppTeam()).thenReturn(publicTeam);
        when(leagueService.getAllLeagues(Organization.PKFL, true)).thenReturn(List.of(league));
        when(leagueService.getLeagueBy(5L)).thenReturn(league);
        when(teamRepository.findAllTeamsByCurrentLeagueId(5L))
                .thenReturn(List.of(publicEntity, privateEntity));
        when(teamMapper.toDTO(publicEntity)).thenReturn(publicFootballTeam);
        when(teamMapper.toDTO(privateEntity)).thenReturn(privateFootballTeam);
    }

    @Test
    void registrationSetupOnlyExposesPublicTeam() {
        RegistrationSetup setup = service.getRegistrationSetup();

        assertEquals(List.of(publicTeam), setup.getAppTeamList());
        assertEquals(1, setup.getLeagueWithTeamsList().get(0).getTeamWithAppTeamsList()
                .stream().mapToInt(team -> team.getAppTeamList().size()).sum());
    }

    private TeamDTO teamDto(long id, String name) {
        TeamDTO team = new TeamDTO();
        team.setId(id);
        team.setName(name);
        team.setCurrentLeagueId(5L);
        return team;
    }

    private TeamEntity teamEntity(long id) {
        TeamEntity team = new TeamEntity();
        team.setId(id);
        return team;
    }
}
