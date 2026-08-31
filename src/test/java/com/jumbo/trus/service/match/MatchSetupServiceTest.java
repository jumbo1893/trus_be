package com.jumbo.trus.service.match;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.response.SetupMatchResponse;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.participation.MatchParticipationEntity;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.participation.MatchParticipationRepository;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.player.PlayerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchSetupServiceTest {

    private final MatchQueryService matchQueryService = mock(MatchQueryService.class);
    private final MatchMapper matchMapper = mock(MatchMapper.class);
    private final FootballMatchMapper footballMatchMapper = mock(FootballMatchMapper.class);
    private final SeasonService seasonService = mock(SeasonService.class);
    private final PlayerService playerService = mock(PlayerService.class);
    private final FootballMatchService footballMatchService = mock(FootballMatchService.class);
    private final MatchParticipationRepository participationRepository = mock(MatchParticipationRepository.class);
    private final MatchSetupService service = new MatchSetupService(
            matchQueryService,
            matchMapper,
            footballMatchMapper,
            seasonService,
            playerService,
            footballMatchService,
            participationRepository
    );

    @Test
    void newMatchSetupReturnsAttendingPlayersAndFansForFootballMatch() {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(1L);
        SeasonDTO season = new SeasonDTO();
        PlayerDTO attendingPlayer = playerDto(2L, false);
        PlayerDTO otherPlayer = playerDto(3L, false);
        PlayerDTO attendingFan = playerDto(4L, true);

        when(seasonService.getAutomaticSeason()).thenReturn(season);
        when(playerService.getAllByFan(false, 1L)).thenReturn(List.of(attendingPlayer, otherPlayer));
        when(playerService.getAllByFan(true, 1L)).thenReturn(List.of(attendingFan));
        when(participationRepository
                .findAllByFootballMatchIdAndAppTeamIdAndStatusOrderByPlayerNameAsc(
                        20L,
                        1L,
                        MatchParticipationStatus.ATTENDING
                ))
                .thenReturn(List.of(
                        participation(player(2L)),
                        participation(player(4L))
                ));

        SetupMatchResponse response = service.setupMatch(null, 20L, appTeam);

        assertThat(response.getAttendingPlayers()).containsExactly(attendingPlayer);
        assertThat(response.getAttendingFans()).containsExactly(attendingFan);
        verify(participationRepository)
                .findAllByFootballMatchIdAndAppTeamIdAndStatusOrderByPlayerNameAsc(
                        20L,
                        1L,
                        MatchParticipationStatus.ATTENDING
                );
    }

    private PlayerDTO playerDto(long id, boolean fan) {
        PlayerDTO player = new PlayerDTO();
        player.setId(id);
        player.setFan(fan);
        return player;
    }

    private PlayerEntity player(long id) {
        PlayerEntity player = new PlayerEntity();
        player.setId(id);
        player.setDeleted(false);
        return player;
    }

    private MatchParticipationEntity participation(PlayerEntity player) {
        MatchParticipationEntity participation = new MatchParticipationEntity();
        participation.setPlayer(player);
        participation.setStatus(MatchParticipationStatus.ATTENDING);
        return participation;
    }
}
