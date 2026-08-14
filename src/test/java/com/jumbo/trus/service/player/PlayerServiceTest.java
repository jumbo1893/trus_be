package com.jumbo.trus.service.player;

import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.achievement.init.PlayerAchievementInitializationService;
import com.jumbo.trus.service.football.player.FootballPlayerService;
import com.jumbo.trus.service.notification.NotificationService;
import com.jumbo.trus.service.outbox.OutboxEventService;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerServiceTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final PlayerMapper playerMapper = mock(PlayerMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final FootballPlayerService footballPlayerService = mock(FootballPlayerService.class);
    private final UserTeamRoleRepository userTeamRoleRepository = mock(UserTeamRoleRepository.class);
    private final OutboxEventService outboxEventService = mock(OutboxEventService.class);
    private final PlayerAchievementInitializationService initializationService =
            mock(PlayerAchievementInitializationService.class);
    private final PlayerService playerService = new PlayerService(
            playerRepository,
            playerMapper,
            notificationService,
            footballPlayerService,
            userTeamRoleRepository,
            outboxEventService,
            initializationService
    );

    @Test
    void initializesAchievementsWhenPlayerIsCreated() {
        PlayerDTO player = player(false);
        PlayerEntity entity = entity(12L, false);
        when(playerMapper.toEntity(player)).thenReturn(entity);
        when(playerRepository.save(entity)).thenReturn(entity);
        when(playerRepository.findMatchIdsWherePlayerAttends(12L)).thenReturn(Set.of());
        when(playerMapper.toDTO(entity)).thenReturn(player);

        playerService.addPlayer(player, new AppTeamEntity());

        verify(initializationService).initializeAchievementsForPlayer(12L);
    }

    @Test
    void initializesMissingAchievementsWhenFanBecomesPlayer() {
        PlayerEntity entity = entity(12L, true);
        PlayerDTO update = player(false);
        update.setId(12L);
        stubEdit(entity, update);

        playerService.editPlayer(12L, update);

        verify(initializationService).initializeAchievementsForPlayer(12L);
    }

    @Test
    void keepsExistingAchievementsWithoutInitializationWhenPlayerBecomesFan() {
        PlayerEntity entity = entity(12L, false);
        PlayerDTO update = player(true);
        update.setId(12L);
        stubEdit(entity, update);

        playerService.editPlayer(12L, update);

        verify(initializationService, never()).initializeAchievementsForPlayer(12L);
    }

    private void stubEdit(PlayerEntity entity, PlayerDTO update) {
        when(playerRepository.findById(12L)).thenReturn(Optional.of(entity));
        when(playerRepository.findMatchIdsWherePlayerAttends(12L)).thenReturn(Set.of());
        when(playerRepository.save(entity)).thenReturn(entity);
        when(playerMapper.toDTO(entity)).thenReturn(update);
    }

    private PlayerDTO player(boolean fan) {
        PlayerDTO player = new PlayerDTO();
        player.setName("Test");
        player.setBirthday(new Date());
        player.setFan(fan);
        player.setActive(true);
        return player;
    }

    private PlayerEntity entity(long id, boolean fan) {
        PlayerEntity player = new PlayerEntity();
        player.setId(id);
        player.setName("Test");
        player.setBirthday(new Date());
        player.setFan(fan);
        player.setActive(true);
        return player;
    }
}
