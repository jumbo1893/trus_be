package com.jumbo.trus.service.activity.footbar.session;

import com.jumbo.trus.dto.footbar.FootbarSessionDTO;
import com.jumbo.trus.dto.footbar.response.FootbarAccountSessions;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.MatchEntity;
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
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FootbarSessionGetter {

    private final FootbarSessionRepository footbarSessionRepository;
    private final FootbarSessionMapper footbarSessionMapper;
    private final PlayerMapper playerMapper;
    private final MatchMapper matchMapper;
    private final UserRepository userRepository;

    public List<FootbarAccountSessions> getListOfFootbarAccountsByMatch(
            AppTeamEntity appTeam,
            MatchEntity match,
            Long userId
    ) {
        List<FootbarSessionEntity> sessions = footbarSessionRepository.findSessionsByAppTeamAndMatch(appTeam, match);
        if (sessions.isEmpty()) {
            return new ArrayList<>();
        }

        PlayerDTO currentUserPlayerDTO = findCurrentUserPlayerDTO(appTeam, userId);

        Map<FootbarAccountEntity, List<FootbarSessionEntity>> groupedSessions = groupSessionsByFootbarAccount(sessions);

        return groupedSessions.entrySet().stream()
                .map(entry -> buildFootbarAccountsSessions(
                        entry.getKey(),
                        entry.getValue(),
                        match,
                        currentUserPlayerDTO
                ))
                .toList();
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

    private Map<FootbarAccountEntity, List<FootbarSessionEntity>> groupSessionsByFootbarAccount(List<FootbarSessionEntity> sessions) {
        return sessions.stream()
                .collect(Collectors.groupingBy(FootbarSessionEntity::getFootbarAccount));
    }

    private FootbarAccountSessions buildFootbarAccountsSessions(
            FootbarAccountEntity account,
            List<FootbarSessionEntity> sessions,
            MatchEntity matchEntity,
            PlayerDTO currentUserPlayerDTO
    ) {
        List<FootbarSessionDTO> dtoSessions = sessions.stream()
                .map(footbarSessionMapper::toDTO)
                .peek(session -> {
                    if (session.getPlayer() == null) {
                        session.setPlayer(createFallbackPlayer(account));
                    }
                })
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

    private PlayerDTO createFallbackPlayer(FootbarAccountEntity account) {
        PlayerDTO playerDTO = new PlayerDTO();
        playerDTO.setId(-account.getId());
        playerDTO.setName(account.getUser().getName());
        playerDTO.setFootballPlayer(null);
        playerDTO.setFan(false);
        playerDTO.setActive(true);
        playerDTO.setBirthday(new Date(0));
        return playerDTO;
    }

    public double getTotalDistanceForPlayerAndSeason(long playerId, long seasonId) {
        return footbarSessionRepository.findDistanceByPlayerIdAndSeasonIdAndAppTeam(seasonId, playerId);
    }
}