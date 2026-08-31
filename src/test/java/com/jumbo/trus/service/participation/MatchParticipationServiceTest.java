package com.jumbo.trus.service.participation;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.participation.MatchParticipationDetail;
import com.jumbo.trus.dto.participation.MatchParticipationPrompt;
import com.jumbo.trus.dto.participation.MatchParticipationCommentRequest;
import com.jumbo.trus.dto.participation.MatchParticipationReactionRequest;
import com.jumbo.trus.dto.participation.MatchParticipationRequest;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.entity.participation.MatchParticipationEntity;
import com.jumbo.trus.entity.participation.MatchParticipationCommentEntity;
import com.jumbo.trus.entity.participation.MatchParticipationCommentReactionEntity;
import com.jumbo.trus.entity.participation.MatchParticipationCommentReactionType;
import com.jumbo.trus.entity.participation.MatchParticipationPromptAudience;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.repository.participation.MatchParticipationRepository;
import com.jumbo.trus.repository.participation.MatchParticipationCommentRepository;
import com.jumbo.trus.repository.participation.MatchParticipationCommentReactionRepository;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.player.PlayerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class MatchParticipationServiceTest {

    private final MatchParticipationRepository participationRepository = mock(MatchParticipationRepository.class);
    private final MatchParticipationCommentRepository commentRepository = mock(MatchParticipationCommentRepository.class);
    private final MatchParticipationCommentReactionRepository reactionRepository = mock(MatchParticipationCommentReactionRepository.class);
    private final FootballMatchRepository footballMatchRepository = mock(FootballMatchRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final UserTeamRoleRepository userTeamRoleRepository = mock(UserTeamRoleRepository.class);
    private final AppTeamRepository appTeamRepository = mock(AppTeamRepository.class);
    private final PlayerMapper playerMapper = mock(PlayerMapper.class);
    private final FootballMatchService footballMatchService = mock(FootballMatchService.class);
    private final PlayerService playerService = mock(PlayerService.class);
    private final AppTeamService appTeamService = mock(AppTeamService.class);
    private final MatchParticipationService service = new MatchParticipationService(
            participationRepository,
            commentRepository,
            reactionRepository,
            footballMatchRepository,
            playerRepository,
            userTeamRoleRepository,
            appTeamRepository,
            playerMapper,
            footballMatchService,
            playerService,
            appTeamService
    );

    @Test
    void offersPromptWhenPlayerHasNotAnswered() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        UserTeamRole role = role(appTeam, player(3L, appTeam));
        FootballMatchDTO match = footballMatchDto(20L);
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L)).thenReturn(Optional.of(role));
        when(participationRepository.findByFootballMatchIdAndAppTeamIdAndPlayerId(20L, 1L, 3L))
                .thenReturn(Optional.empty());
        when(playerMapper.toDTO(role.getPlayer())).thenReturn(playerDto(3L));

        MatchParticipationPrompt prompt = service.getPrompt(2L, appTeam, match);

        assertThat(prompt).isNotNull();
        assertThat(prompt.isReconsideration()).isFalse();
        assertThat(prompt.getCurrentPlayer().getId()).isEqualTo(3L);
    }

    @Test
    void offersPromptToInactiveFanWhenAllPaired() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        PlayerEntity fan = player(3L, appTeam);
        fan.setFan(true);
        fan.setActive(false);
        UserTeamRole role = role(appTeam, fan);
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L)).thenReturn(Optional.of(role));
        when(participationRepository.findByFootballMatchIdAndAppTeamIdAndPlayerId(20L, 1L, 3L))
                .thenReturn(Optional.empty());
        when(playerMapper.toDTO(fan)).thenReturn(playerDto(3L));

        MatchParticipationPrompt prompt = service.getPrompt(2L, appTeam, footballMatchDto(20L));

        assertThat(prompt).isNotNull();
        assertThat(prompt.getCurrentPlayer().getId()).isEqualTo(3L);
    }

    @Test
    void respectsPlayersOnlyPromptAudience() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        appTeam.setMatchParticipationPromptAudience(MatchParticipationPromptAudience.PLAYERS);
        PlayerEntity fan = player(3L, appTeam);
        fan.setFan(true);
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L))
                .thenReturn(Optional.of(role(appTeam, fan)));

        assertThat(service.getPrompt(2L, appTeam, footballMatchDto(20L))).isNull();
        verify(participationRepository, never())
                .findByFootballMatchIdAndAppTeamIdAndPlayerId(20L, 1L, 3L);
    }

    @Test
    void doesNotOfferAutomaticPromptToUnpairedUser() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L))
                .thenReturn(Optional.of(role(appTeam, null)));

        assertThat(service.getPrompt(2L, appTeam, footballMatchDto(20L))).isNull();
    }

    @Test
    void doesNotRepeatMaybePromptBeforeTwentyFourHours() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        PlayerEntity player = player(3L, appTeam);
        UserTeamRole role = role(appTeam, player);
        MatchParticipationEntity participation = participation(
                player,
                MatchParticipationStatus.MAYBE,
                Instant.now().minus(Duration.ofHours(23))
        );
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L)).thenReturn(Optional.of(role));
        when(participationRepository.findByFootballMatchIdAndAppTeamIdAndPlayerId(20L, 1L, 3L))
                .thenReturn(Optional.of(participation));

        assertThat(service.getPrompt(2L, appTeam, footballMatchDto(20L))).isNull();
    }

    @Test
    void repeatsMaybePromptAfterTwentyFourHours() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        PlayerEntity player = player(3L, appTeam);
        UserTeamRole role = role(appTeam, player);
        MatchParticipationEntity participation = participation(
                player,
                MatchParticipationStatus.MAYBE,
                Instant.now().minus(Duration.ofHours(25))
        );
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L)).thenReturn(Optional.of(role));
        when(participationRepository.findByFootballMatchIdAndAppTeamIdAndPlayerId(20L, 1L, 3L))
                .thenReturn(Optional.of(participation));
        when(playerMapper.toDTO(player)).thenReturn(playerDto(3L));

        MatchParticipationPrompt prompt = service.getPrompt(2L, appTeam, footballMatchDto(20L));

        assertThat(prompt).isNotNull();
        assertThat(prompt.isReconsideration()).isTrue();
        assertThat(prompt.getCurrentStatus()).isEqualTo(MatchParticipationStatus.MAYBE);
    }

    @Test
    void doesNotOfferPromptAfterFinalAnswer() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        PlayerEntity player = player(3L, appTeam);
        UserTeamRole role = role(appTeam, player);
        MatchParticipationEntity participation = participation(
                player,
                MatchParticipationStatus.ATTENDING,
                Instant.now().minus(Duration.ofDays(2))
        );
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L)).thenReturn(Optional.of(role));
        when(participationRepository.findByFootballMatchIdAndAppTeamIdAndPlayerId(20L, 1L, 3L))
                .thenReturn(Optional.of(participation));

        assertThat(service.getPrompt(2L, appTeam, footballMatchDto(20L))).isNull();
    }

    @Test
    void pairsUnpairedUserAndStoresAnswerForInactiveFan() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        UserTeamRole role = role(appTeam, null);
        PlayerEntity player = player(3L, appTeam);
        player.setFan(true);
        player.setActive(false);
        FootballMatchEntity footballMatch = footballMatch(20L, 10L, 11L);
        PlayerDTO playerDto = playerDto(3L);

        when(footballMatchRepository.findById(20L)).thenReturn(Optional.of(footballMatch));
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L)).thenReturn(Optional.of(role));
        when(appTeamService.pairPlayerToRole(role, 2L, 3L, appTeam)).thenAnswer(invocation -> {
            role.setPlayer(player);
            return player;
        });
        when(participationRepository.findByFootballMatchIdAndAppTeamIdAndPlayerId(20L, 1L, 3L))
                .thenReturn(Optional.empty());
        when(participationRepository.findAllByFootballMatchIdAndAppTeamIdOrderByPlayerNameAsc(20L, 1L))
                .thenReturn(List.of());
        when(playerMapper.toDTO(player)).thenReturn(playerDto);
        when(footballMatchService.getFootballMatchById(20L)).thenReturn(footballMatchDto(20L));

        MatchParticipationDetail detail = service.respond(
                2L,
                appTeam,
                new MatchParticipationRequest(20L, 3L, MatchParticipationStatus.ATTENDING, "Dorazím později.")
        );

        assertThat(role.getPlayer()).isSameAs(player);
        assertThat(detail.getCurrentPlayer()).isSameAs(playerDto);
        verify(appTeamService).pairPlayerToRole(role, 2L, 3L, appTeam);
        verify(participationRepository).save(any(MatchParticipationEntity.class));
        verify(commentRepository).save(any(MatchParticipationCommentEntity.class));
    }

    @Test
    void refusesParticipationPairingWhenPlayerBelongsToAnotherUser() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        UserTeamRole role = role(appTeam, null);
        FootballMatchEntity footballMatch = footballMatch(20L, 10L, 11L);
        String message = "Tento hráč je již spárovaný s uživatelem Petr.";

        when(footballMatchRepository.findById(20L)).thenReturn(Optional.of(footballMatch));
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L)).thenReturn(Optional.of(role));
        when(appTeamService.pairPlayerToRole(role, 2L, 3L, appTeam))
                .thenThrow(new FieldValidationException(message, List.of()));

        assertThatThrownBy(() -> service.respond(
                2L,
                appTeam,
                new MatchParticipationRequest(20L, 3L, MatchParticipationStatus.ATTENDING, null)
        ))
                .isInstanceOf(FieldValidationException.class)
                .hasMessage(message);

        verify(participationRepository, never()).save(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void replyIsStoredUnderTheCommentedParticipantsAnswer() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        PlayerEntity author = player(3L, appTeam);
        PlayerEntity target = player(4L, appTeam);
        FootballMatchEntity footballMatch = footballMatch(20L, 10L, 11L);
        MatchParticipationEntity targetParticipation = participation(
                target,
                MatchParticipationStatus.NOT_ATTENDING,
                Instant.now()
        );
        targetParticipation.setId(30L);
        targetParticipation.setAppTeam(appTeam);
        targetParticipation.setFootballMatch(footballMatch);
        MatchParticipationCommentEntity parent = new MatchParticipationCommentEntity();
        parent.setId(40L);
        parent.setParticipation(targetParticipation);

        when(footballMatchRepository.findById(20L)).thenReturn(Optional.of(footballMatch));
        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L))
                .thenReturn(Optional.of(role(appTeam, author)));
        when(commentRepository.findByIdAndParticipationAppTeamId(40L, 1L))
                .thenReturn(Optional.of(parent));
        when(footballMatchService.getFootballMatchById(20L)).thenReturn(footballMatchDto(20L));

        service.addComment(
                2L,
                appTeam,
                new MatchParticipationCommentRequest(20L, "Držím palce.", 40L)
        );

        ArgumentCaptor<MatchParticipationCommentEntity> captor =
                ArgumentCaptor.forClass(MatchParticipationCommentEntity.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipation()).isSameAs(targetParticipation);
        assertThat(captor.getValue().getAuthor()).isSameAs(author);
        assertThat(captor.getValue().getParentComment()).isSameAs(parent);
    }

    @Test
    void clickingTheSameReactionAgainRemovesIt() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        PlayerEntity player = player(3L, appTeam);
        FootballMatchEntity footballMatch = footballMatch(20L, 10L, 11L);
        MatchParticipationEntity participation = participation(
                player,
                MatchParticipationStatus.ATTENDING,
                Instant.now()
        );
        participation.setId(30L);
        participation.setAppTeam(appTeam);
        participation.setFootballMatch(footballMatch);
        MatchParticipationCommentEntity comment = new MatchParticipationCommentEntity();
        comment.setId(40L);
        comment.setParticipation(participation);
        MatchParticipationCommentReactionEntity existing =
                new MatchParticipationCommentReactionEntity();
        existing.setComment(comment);
        existing.setPlayer(player);
        existing.setReaction(MatchParticipationCommentReactionType.UP);

        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L))
                .thenReturn(Optional.of(role(appTeam, player)));
        when(commentRepository.findByIdAndParticipationAppTeamId(40L, 1L))
                .thenReturn(Optional.of(comment));
        when(reactionRepository.findByCommentIdAndPlayerId(40L, 3L))
                .thenReturn(Optional.of(existing));
        when(footballMatchService.getFootballMatchById(20L)).thenReturn(footballMatchDto(20L));

        service.reactToComment(
                2L,
                appTeam,
                40L,
                new MatchParticipationReactionRequest(MatchParticipationCommentReactionType.UP)
        );

        verify(reactionRepository).delete(existing);
        verify(reactionRepository, never()).save(existing);
    }

    @Test
    void onlyCommentAuthorCanDeleteIt() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        PlayerEntity currentPlayer = player(3L, appTeam);
        PlayerEntity author = player(4L, appTeam);
        MatchParticipationCommentEntity comment = new MatchParticipationCommentEntity();
        comment.setId(40L);
        comment.setAuthor(author);

        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L))
                .thenReturn(Optional.of(role(appTeam, currentPlayer)));
        when(commentRepository.findByIdAndParticipationAppTeamId(40L, 1L))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.deleteComment(2L, appTeam, 40L))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining("autor");
        verify(commentRepository, never()).deleteAllByIds(any());
        verify(reactionRepository, never()).deleteAllByCommentIds(any());
    }

    @Test
    void deletingOwnCommentAlsoDeletesRepliesAndReactions() {
        AppTeamEntity appTeam = appTeam(1L, 10L);
        PlayerEntity author = player(3L, appTeam);
        FootballMatchEntity footballMatch = footballMatch(20L, 10L, 11L);
        MatchParticipationEntity participation = participation(
                author,
                MatchParticipationStatus.ATTENDING,
                Instant.now()
        );
        participation.setId(30L);
        participation.setAppTeam(appTeam);
        participation.setFootballMatch(footballMatch);
        MatchParticipationCommentEntity root = new MatchParticipationCommentEntity();
        root.setId(40L);
        root.setAuthor(author);
        root.setParticipation(participation);
        MatchParticipationCommentEntity reply = new MatchParticipationCommentEntity();
        reply.setId(41L);
        reply.setAuthor(player(4L, appTeam));
        reply.setParticipation(participation);
        reply.setParentComment(root);

        when(userTeamRoleRepository.findByUserIdAndAppTeamId(2L, 1L))
                .thenReturn(Optional.of(role(appTeam, author)));
        when(commentRepository.findByIdAndParticipationAppTeamId(40L, 1L))
                .thenReturn(Optional.of(root));
        when(commentRepository.findAllByParticipationIdOrderByCreatedAtAsc(30L))
                .thenReturn(List.of(root, reply));
        when(footballMatchService.getFootballMatchById(20L)).thenReturn(footballMatchDto(20L));

        service.deleteComment(2L, appTeam, 40L);

        verify(reactionRepository).deleteAllByCommentIds(argThat(ids -> ids.containsAll(List.of(40L, 41L))));
        verify(commentRepository).deleteAllByIds(argThat(ids -> ids.containsAll(List.of(40L, 41L))));
    }

    private MatchParticipationEntity participation(
            PlayerEntity player,
            MatchParticipationStatus status,
            Instant respondedAt
    ) {
        MatchParticipationEntity participation = new MatchParticipationEntity();
        participation.setPlayer(player);
        participation.setStatus(status);
        participation.setRespondedAt(respondedAt);
        return participation;
    }

    private UserTeamRole role(AppTeamEntity appTeam, PlayerEntity player) {
        UserTeamRole role = new UserTeamRole();
        role.setAppTeam(appTeam);
        role.setPlayer(player);
        return role;
    }

    private AppTeamEntity appTeam(long id, long teamId) {
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(id);
        appTeam.setTeam(team(teamId));
        return appTeam;
    }

    private PlayerEntity player(long id, AppTeamEntity appTeam) {
        PlayerEntity player = new PlayerEntity();
        player.setId(id);
        player.setName("Test");
        player.setAppTeam(appTeam);
        player.setActive(true);
        player.setFan(false);
        return player;
    }

    private PlayerDTO playerDto(long id) {
        PlayerDTO player = new PlayerDTO();
        player.setId(id);
        player.setName("Test");
        return player;
    }

    private FootballMatchDTO footballMatchDto(long id) {
        FootballMatchDTO match = new FootballMatchDTO();
        match.setId(id);
        return match;
    }

    private FootballMatchEntity footballMatch(long id, long homeTeamId, long awayTeamId) {
        FootballMatchEntity match = new FootballMatchEntity();
        match.setId(id);
        match.setHomeTeam(team(homeTeamId));
        match.setAwayTeam(team(awayTeamId));
        return match;
    }

    private TeamEntity team(long id) {
        TeamEntity team = new TeamEntity();
        team.setId(id);
        return team;
    }
}
