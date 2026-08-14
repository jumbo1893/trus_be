package com.jumbo.trus.service.player;

import com.jumbo.trus.dto.player.IPlayerBirthday;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballPlayerEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.achievement.init.PlayerAchievementInitializationService;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.helper.BirthdayCalculator;
import com.jumbo.trus.service.helper.ValidationField;
import com.jumbo.trus.service.notification.NotificationService;
import com.jumbo.trus.service.outbox.OutboxEventPayloadFactory;
import com.jumbo.trus.service.outbox.OutboxEventService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final NotificationService notificationService;
    private final FootballPlayerService footballPlayerService;
    private final UserTeamRoleRepository userTeamRoleRepository;
    private final OutboxEventService outboxEventService;
    private final PlayerAchievementInitializationService playerAchievementInitializationService;

    @Transactional
    public PlayerDTO addPlayer(PlayerDTO playerDTO, AppTeamEntity appTeam) {
        PlayerEntity entity = playerMapper.toEntity(playerDTO);
        entity.setAppTeam(appTeam);
        PlayerEntity savedEntity = playerRepository.save(entity);
        playerAchievementInitializationService.initializeAchievementsForPlayer(savedEntity.getId());
        Set<Long> affectedMatches = playerRepository.findMatchIdsWherePlayerAttends(savedEntity.getId());
        notificationService.addNotification("Přidán " + (playerDTO.isFan() ? "fanoušek" : "hráč"), playerDTO.getName() + ", s narozeninami " + playerDTO.getBirthday());
        outboxEventService.createEvent(OutboxEventType.PLAYER_CREATED, OutboxAggregateType.PLAYER, savedEntity.getId(), OutboxEventPayloadFactory.playerCreated(affectedMatches));
        return playerMapper.toDTO(savedEntity);
    }

    public List<PlayerDTO> getAllByFan(boolean fan, long appTeamId){
        List<PlayerEntity> playerEntities = playerRepository.getAllByFan(fan, appTeamId);
        return playerEntities.stream().map(playerMapper::toDTO).toList();
    }

    public List<PlayerDTO> getAllActive(boolean active, long appTeamId){
        List<PlayerEntity> playerEntities = playerRepository.getAllByActive(active, appTeamId);
        return playerEntities.stream().map(playerMapper::toDTO).toList();
    }

    public List<PlayerDTO> getAll(long appTeamId){
        List<PlayerEntity> playerEntities = playerRepository.getAll(appTeamId);
        return playerEntities.stream().map(playerMapper::toDTO).toList();
    }

    public List<PlayerDTO> getAllByIds(Set<Long> playerIds, long appTeamId) {
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }
        return playerRepository.findAllByIdsAndAppTeam(playerIds, appTeamId).stream()
                .map(playerMapper::toDTO)
                .toList();
    }

    public PlayerDTO getPlayer(long playerId) {
        return playerMapper.toDTO(getPlayerEntity(playerId));
    }

    public PlayerEntity getPlayerEntity(long playerId) {
        return playerRepository.findById(playerId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(playerId)));
    }

    public String getListOfNamesFromListOfPlayers(List<PlayerDTO> playerList) {
        StringBuilder players = new StringBuilder();
        for (int i = 0; i < playerList.size(); i++) {
            players.append(playerList.get(i).getName());
            if (i != playerList.size()-1) {
                players.append(", ");
            }
        }
        return players.toString();
    }

    @Transactional
    public PlayerDTO editPlayer(Long playerId, PlayerDTO playerDTO)
            throws NotFoundException {
        PlayerEntity entity = playerRepository.findById(playerId)
                .orElseThrow(() ->
                        new NotFoundException("Hráč s id " + playerId + " nenalezen v db")
                );
        if (entity.isDeleted()) {
            throw new NotFoundException("Smazaného hráče nelze upravit");
        }
        boolean wasFan = entity.isFan();
        Set<Long> affectedMatches = new java.util.HashSet<>(playerRepository.findMatchIdsWherePlayerAttends(playerId));
        validatePlayer(playerDTO);
        entity.setName(playerDTO.getName());
        entity.setBirthday(playerDTO.getBirthday());
        entity.setFan(playerDTO.isFan());
        entity.setActive(playerDTO.isActive());
        if (playerDTO.getFootballPlayer() == null) {
            entity.setFootballPlayer(null);
        } else {
            entity.setFootballPlayer(
                    footballPlayerService.getFootballPlayerEntity(
                            playerDTO.getFootballPlayer().getId()
                    )
            );
        }
        PlayerEntity savedEntity = playerRepository.save(entity);
        if (wasFan && !savedEntity.isFan()) {
            playerAchievementInitializationService.initializeAchievementsForPlayer(savedEntity.getId());
        }
        affectedMatches.addAll(playerRepository.findMatchIdsWherePlayerAttends(savedEntity.getId()));
        notificationService.addNotification(
                "Upraven " + (playerDTO.isFan() ? "fanoušek" : "hráč"),
                playerDTO.getName() + ", s narozeninami " + playerDTO.getBirthday()
        );
        outboxEventService.createEvent(OutboxEventType.PLAYER_UPDATED, OutboxAggregateType.PLAYER, savedEntity.getId(), OutboxEventPayloadFactory.playerUpdated(affectedMatches));
        return playerMapper.toDTO(savedEntity);
    }

    @Transactional
    public void deletePlayer(Long playerId) {
        PlayerEntity playerEntity = playerRepository.findById(playerId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Hráč s id " + playerId + " nenalezen")
                );
        if (playerEntity.isDeleted()) {
            return;
        }
        Set<Long> affectedMatches = playerRepository.findMatchIdsWherePlayerAttends(playerId);
        notificationService.addNotification(
                "Smazán " + (playerEntity.isFan() ? "fanoušek" : "hráč"),
                playerEntity.getName() + ", s narozeninami " + playerEntity.getBirthday()
        );
        playerEntity.setDeleted(true);
        playerEntity.setDeletedAt(new Date());
        playerEntity.setFootballPlayer(null);
        userTeamRoleRepository.findAllByPlayerId(playerId)
                .forEach(userTeamRole -> userTeamRole.setPlayer(null));
        playerRepository.save(playerEntity);
        outboxEventService.createEvent(OutboxEventType.PLAYER_DELETED, OutboxAggregateType.PLAYER, playerId, OutboxEventPayloadFactory.playerDeleted(affectedMatches));

    }

    public List<Long> convertPlayerListToPlayerIdList(List<PlayerDTO> players) {
        List<Long> playerIdList = new ArrayList<>();
        for (PlayerDTO playerDTO : players) {
            playerIdList.add(playerDTO.getId());
        }
        return playerIdList;
    }

    public String returnNextPlayerBirthdayFromList(long appTeamId) {
        List<PlayerDTO> players = playerRepository.getUpcomingBirthdayPlayers(appTeamId)
                .stream()
                .map(this::toBirthdayPlayerDTO)
                .toList();
        BirthdayCalculator birthdayCalculator = new BirthdayCalculator(players);
        return birthdayCalculator.returnNextPlayerBirthdayFromList();
    }

    private PlayerDTO toBirthdayPlayerDTO(IPlayerBirthday playerBirthday) {
        PlayerDTO playerDTO = new PlayerDTO();
        playerDTO.setId(playerBirthday.getId() == null ? 0L : playerBirthday.getId());
        playerDTO.setName(playerBirthday.getName());
        playerDTO.setBirthday(playerBirthday.getBirthday());
        playerDTO.setFan(Boolean.TRUE.equals(playerBirthday.getFan()));
        playerDTO.setActive(true);
        return playerDTO;
    }

    public static PlayerDTO noPlayer() {
        PlayerDTO playerDTO = new PlayerDTO();
        playerDTO.setId(0L);
        playerDTO.setFootballPlayer(null);
        playerDTO.setName("-");
        playerDTO.setFan(false);
        playerDTO.setBirthday(new Date());
        playerDTO.setActive(true);
        return playerDTO;
    }

    private void validatePlayer(PlayerDTO playerDTO) {
        if (playerDTO.getFootballPlayer() == null) return;
        FootballPlayerEntity newFootballPlayerEntity = footballPlayerService.getFootballPlayerEntity(playerDTO.getFootballPlayer().getId());
        if (newFootballPlayerEntity.getPlayer() == null || newFootballPlayerEntity.getPlayer().getId() == playerDTO.getId()) return;
        makeValidationException(newFootballPlayerEntity.getPlayer().getName(), newFootballPlayerEntity.getName());
    }

    private void makeValidationException(String oldPlayerName, String footballerName) {
        List<ValidationField> fields = new ArrayList<>();
        fields.add(new ValidationField("football_player", "Pod hráčem " + footballerName + " hraje již " +  oldPlayerName));
        throw new FieldValidationException("Chyba při úpravě hráče", fields);
    }
}
