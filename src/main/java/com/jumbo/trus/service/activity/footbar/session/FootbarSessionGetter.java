package com.jumbo.trus.service.activity.footbar.session;

import com.jumbo.trus.dto.footbar.FootbarSessionDTO;
import com.jumbo.trus.dto.footbar.IPlayerRunningStats;
import com.jumbo.trus.dto.footbar.response.FootbarAccountSessions;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.footbar.FootbarAccountEntity;
import com.jumbo.trus.entity.footbar.FootbarSessionEntity;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.mapper.footbar.FootbarSessionMapper;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.footbar.FootbarSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FootbarSessionGetter {

    private final FootbarSessionRepository footbarSessionRepository;
    private final FootbarSessionMapper footbarSessionMapper;
    private final PlayerMapper playerMapper;
    private final MatchMapper matchMapper;
    private final UserRepository userRepository;

    public FootbarAccountSessions getFootbarAccountSessionByMatch(
            AppTeamEntity appTeam,
            MatchEntity match,
            Long userId
    ) {
        List<FootbarSessionEntity> sessions = footbarSessionRepository.findSessionsByAppTeamAndMatch(appTeam, match);
        if (sessions.isEmpty()) {
            return null;
        }

        PlayerDTO currentUserPlayerDTO = findCurrentUserPlayerDTO(appTeam, userId);
        return buildFootbarAccountsSessions(sessions, match, currentUserPlayerDTO);
    }

    private PlayerDTO findCurrentUserPlayerDTO(AppTeamEntity appTeam, Long userId) {
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        return user.getTeamRoles().stream()
                .filter(role -> role.getAppTeam().equals(appTeam))
                .map(UserTeamRole::getPlayer)
                .filter(Objects::nonNull)
                .findFirst()
                .map(playerMapper::toDTO)
                .orElse(null);
    }

    private FootbarAccountSessions buildFootbarAccountsSessions(
            List<FootbarSessionEntity> sessions,
            MatchEntity matchEntity,
            PlayerDTO currentUserPlayerDTO
    ) {

        List<FootbarSessionDTO> dtoSessions = sessions.stream()
                .peek(session -> {
                    if (session.getPlayer() == null) {
                        session.setPlayer(createFallbackPlayer(session.getFootbarAccount()));
                    }
                })
                .map(footbarSessionMapper::toDTO)
                .toList();

        List<PlayerDTO> players = dtoSessions.stream()
                .map(FootbarSessionDTO::getPlayer)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        PlayerDTO primaryPlayer;
        if (currentUserPlayerDTO != null) {
            primaryPlayer = players.stream()
                    .filter(player -> Objects.equals(player, currentUserPlayerDTO))
                    .findFirst()
                    .orElse(players.get(0));
        } else {
            primaryPlayer = players.get(0);
        }

        PlayerDTO secondaryPlayer = players.stream()
                .filter(player -> !Objects.equals(player, primaryPlayer))
                .findFirst()
                .orElse(primaryPlayer);

        return new FootbarAccountSessions(
                matchMapper.toDTO(matchEntity),
                players,
                primaryPlayer,
                secondaryPlayer,
                dtoSessions
        );
    }

    private PlayerEntity createFallbackPlayer(FootbarAccountEntity account) {
        PlayerEntity player = new PlayerEntity();
        player.setId(-account.getId());
        player.setName(account.getUser().getName());
        player.setFootballPlayer(null);
        player.setFan(false);
        player.setActive(true);
        player.setBirthday(new Date(0));
        return player;
    }

    public double getTotalDistanceForPlayerAndSeason(long playerId, long seasonId, long appTeamId) {
        if (seasonId == ALL_SEASON_ID) {
            return footbarSessionRepository.findDistanceByPlayerIdAndAppTeam(playerId, appTeamId);
        }
        return footbarSessionRepository.findDistanceByPlayerIdAndSeasonIdAndAppTeam(seasonId, playerId, appTeamId);
    }

    public List<IPlayerRunningStats> getListOfPlayersOrderByAverageTotalDistance(long seasonId, long appTeamId, int count) {
        if (seasonId == ALL_SEASON_ID) {
            return
                    footbarSessionRepository.findTopRunningStatsByAppTeam(
                            appTeamId,
                            PageRequest.of(0, count)
                    );
        }
        return
                footbarSessionRepository.findTopRunningStatsByAppTeamAndSeason(
                        appTeamId,
                        seasonId,
                        PageRequest.of(0, count)
                );
    }
}