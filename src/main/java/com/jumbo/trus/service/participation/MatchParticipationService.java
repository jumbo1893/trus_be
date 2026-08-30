package com.jumbo.trus.service.participation;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.participation.MatchParticipationCommentDTO;
import com.jumbo.trus.dto.participation.MatchParticipationCommentRequest;
import com.jumbo.trus.dto.participation.MatchParticipationDetail;
import com.jumbo.trus.dto.participation.MatchParticipationMemberDTO;
import com.jumbo.trus.dto.participation.MatchParticipationPrompt;
import com.jumbo.trus.dto.participation.MatchParticipationPromptAudienceConfig;
import com.jumbo.trus.dto.participation.MatchParticipationReactionRequest;
import com.jumbo.trus.dto.participation.MatchParticipationRequest;
import com.jumbo.trus.dto.participation.NewPlayerParticipationRequest;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.participation.MatchParticipationCommentEntity;
import com.jumbo.trus.entity.participation.MatchParticipationCommentReactionEntity;
import com.jumbo.trus.entity.participation.MatchParticipationCommentReactionType;
import com.jumbo.trus.entity.participation.MatchParticipationEntity;
import com.jumbo.trus.entity.participation.MatchParticipationPromptAudience;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.repository.participation.MatchParticipationCommentReactionRepository;
import com.jumbo.trus.repository.participation.MatchParticipationCommentRepository;
import com.jumbo.trus.repository.participation.MatchParticipationRepository;
import com.jumbo.trus.service.exceptions.FieldValidationException;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.helper.ValidationField;
import com.jumbo.trus.service.player.PlayerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchParticipationService {

    static final Duration MAYBE_REMINDER_DELAY = Duration.ofHours(24);

    private final MatchParticipationRepository participationRepository;
    private final MatchParticipationCommentRepository commentRepository;
    private final MatchParticipationCommentReactionRepository reactionRepository;
    private final FootballMatchRepository footballMatchRepository;
    private final PlayerRepository playerRepository;
    private final UserTeamRoleRepository userTeamRoleRepository;
    private final AppTeamRepository appTeamRepository;
    private final PlayerMapper playerMapper;
    private final FootballMatchService footballMatchService;
    private final PlayerService playerService;

    @Transactional
    public MatchParticipationPrompt getPrompt(
            Long userId,
            AppTeamEntity appTeam,
            FootballMatchDTO footballMatch
    ) {
        if (footballMatch == null) {
            return null;
        }

        UserTeamRole role = getCurrentRole(userId, appTeam.getId());
        PlayerEntity currentPlayer = isTeamParticipant(role.getPlayer(), appTeam)
                ? role.getPlayer()
                : null;
        if (currentPlayer == null || !isPromptAllowed(currentPlayer, appTeam)) {
            return null;
        }

        MatchParticipationEntity participation = participationRepository
                .findByFootballMatchIdAndAppTeamIdAndPlayerId(
                        footballMatch.getId(),
                        appTeam.getId(),
                        currentPlayer.getId()
                )
                .orElse(null);

        boolean reconsideration = participation != null
                && participation.getStatus() == MatchParticipationStatus.MAYBE
                && isMaybeReminderDue(participation, Instant.now());

        if (participation != null
                && (participation.getStatus() != MatchParticipationStatus.MAYBE || !reconsideration)) {
            return null;
        }

        MatchParticipationPrompt prompt = new MatchParticipationPrompt();
        prompt.setFootballMatch(footballMatch);
        prompt.setCurrentPlayer(playerMapper.toDTO(currentPlayer));
        prompt.setCurrentStatus(participation == null ? null : participation.getStatus());
        prompt.setReconsideration(reconsideration);
        prompt.setEligiblePlayers(List.of());
        return prompt;
    }

    @Transactional
    public MatchParticipationDetail getDetail(Long footballMatchId, Long userId, AppTeamEntity appTeam) {
        FootballMatchEntity footballMatch = getFootballMatchForTeam(footballMatchId, appTeam);
        UserTeamRole role = getCurrentRole(userId, appTeam.getId());
        PlayerEntity currentPlayer = isTeamParticipant(role.getPlayer(), appTeam)
                ? role.getPlayer()
                : null;
        return buildDetail(footballMatch, appTeam, currentPlayer);
    }

    @Transactional
    public MatchParticipationDetail respond(
            Long userId,
            AppTeamEntity appTeam,
            MatchParticipationRequest request
    ) {
        FootballMatchEntity footballMatch = getFootballMatchForTeam(request.getFootballMatchId(), appTeam);
        UserTeamRole role = getCurrentRole(userId, appTeam.getId());
        PlayerEntity player = resolveAndPairParticipant(role, request.getPlayerId(), appTeam);

        MatchParticipationEntity participation = participationRepository
                .findByFootballMatchIdAndAppTeamIdAndPlayerId(
                        footballMatch.getId(),
                        appTeam.getId(),
                        player.getId()
                )
                .orElseGet(MatchParticipationEntity::new);

        participation.setAppTeam(appTeam);
        participation.setFootballMatch(footballMatch);
        participation.setPlayer(player);
        participation.setStatus(request.getStatus());
        participation.setRespondedAt(Instant.now());
        participationRepository.save(participation);
        addCommentIfPresent(participation, player, request.getComment(), null);

        return buildDetail(footballMatch, appTeam, player);
    }

    @Transactional
    public MatchParticipationDetail createPlayerAndRespond(
            Long userId,
            AppTeamEntity appTeam,
            NewPlayerParticipationRequest request
    ) {
        UserTeamRole role = getCurrentRole(userId, appTeam.getId());
        if (isTeamParticipant(role.getPlayer(), appTeam)) {
            throw validationError("player", "Uživatel už je s hráčem nebo fanouškem spárovaný.");
        }
        validateNewPlayer(request.getPlayer());

        PlayerDTO createdPlayer = playerService.addPlayer(request.getPlayer(), appTeam);
        return respond(
                userId,
                appTeam,
                new MatchParticipationRequest(
                        request.getFootballMatchId(),
                        createdPlayer.getId(),
                        request.getStatus(),
                        request.getComment()
                )
        );
    }

    @Transactional
    public MatchParticipationDetail addComment(
            Long userId,
            AppTeamEntity appTeam,
            MatchParticipationCommentRequest request
    ) {
        FootballMatchEntity footballMatch = getFootballMatchForTeam(request.getFootballMatchId(), appTeam);
        PlayerEntity author = requireCurrentParticipant(userId, appTeam);
        MatchParticipationCommentEntity parent = null;
        MatchParticipationEntity participation;
        if (request.getParentCommentId() != null) {
            parent = commentRepository
                    .findByIdAndParticipationAppTeamId(request.getParentCommentId(), appTeam.getId())
                    .filter(comment -> Objects.equals(
                            comment.getParticipation().getFootballMatch().getId(),
                            footballMatch.getId()
                    ))
                    .orElseThrow(() -> new NotFoundException("Komentář pro tuto účast nebyl nalezen."));
            participation = parent.getParticipation();
        } else {
            participation = participationRepository
                    .findByFootballMatchIdAndAppTeamIdAndPlayerId(
                            footballMatch.getId(),
                            appTeam.getId(),
                            author.getId()
                    )
                    .orElseThrow(() -> validationError(
                            "status",
                            "Nejdřív odpověz, zda se zápasu zúčastníš."
                    ));
        }

        addCommentIfPresent(participation, author, request.getText(), parent);
        return buildDetail(footballMatch, appTeam, author);
    }

    @Transactional
    public MatchParticipationDetail reactToComment(
            Long userId,
            AppTeamEntity appTeam,
            Long commentId,
            MatchParticipationReactionRequest request
    ) {
        PlayerEntity player = requireCurrentParticipant(userId, appTeam);
        MatchParticipationCommentEntity comment = commentRepository
                .findByIdAndParticipationAppTeamId(commentId, appTeam.getId())
                .orElseThrow(() -> new NotFoundException("Komentář nebyl nalezen."));

        reactionRepository.findByCommentIdAndPlayerId(commentId, player.getId())
                .ifPresentOrElse(existing -> {
                    if (existing.getReaction() == request.getReaction()) {
                        reactionRepository.delete(existing);
                    } else {
                        existing.setReaction(request.getReaction());
                        existing.setReactedAt(Instant.now());
                        reactionRepository.save(existing);
                    }
                }, () -> {
                    MatchParticipationCommentReactionEntity reaction =
                            new MatchParticipationCommentReactionEntity();
                    reaction.setComment(comment);
                    reaction.setPlayer(player);
                    reaction.setReaction(request.getReaction());
                    reaction.setReactedAt(Instant.now());
                    reactionRepository.save(reaction);
                });

        return buildDetail(comment.getParticipation().getFootballMatch(), appTeam, player);
    }

    @Transactional
    public MatchParticipationDetail deleteComment(
            Long userId,
            AppTeamEntity appTeam,
            Long commentId
    ) {
        PlayerEntity player = requireCurrentParticipant(userId, appTeam);
        MatchParticipationCommentEntity comment = commentRepository
                .findByIdAndParticipationAppTeamId(commentId, appTeam.getId())
                .orElseThrow(() -> new NotFoundException("Komentář nebyl nalezen."));
        if (!Objects.equals(comment.getAuthor().getId(), player.getId())) {
            throw validationError("comment", "Smazat komentář může pouze jeho autor.");
        }

        List<MatchParticipationCommentEntity> participationComments = commentRepository
                .findAllByParticipationIdOrderByCreatedAtAsc(comment.getParticipation().getId());
        Set<Long> commentIds = new LinkedHashSet<>();
        commentIds.add(comment.getId());
        boolean added;
        do {
            added = false;
            for (MatchParticipationCommentEntity candidate : participationComments) {
                if (candidate.getParentComment() != null
                        && commentIds.contains(candidate.getParentComment().getId())) {
                    added |= commentIds.add(candidate.getId());
                }
            }
        } while (added);

        reactionRepository.deleteAllByCommentIds(commentIds);
        commentRepository.deleteAllByIds(commentIds);
        return buildDetail(comment.getParticipation().getFootballMatch(), appTeam, player);
    }

    public MatchParticipationPromptAudienceConfig getPromptAudience(AppTeamEntity appTeam) {
        return new MatchParticipationPromptAudienceConfig(resolvePromptAudience(appTeam));
    }

    @Transactional
    public MatchParticipationPromptAudienceConfig updatePromptAudience(
            AppTeamEntity appTeam,
            MatchParticipationPromptAudienceConfig config
    ) {
        appTeam.setMatchParticipationPromptAudience(config.getAudience());
        appTeamRepository.save(appTeam);
        return config;
    }

    boolean isMaybeReminderDue(MatchParticipationEntity participation, Instant now) {
        return participation.getRespondedAt() == null
                || !participation.getRespondedAt().plus(MAYBE_REMINDER_DELAY).isAfter(now);
    }

    private MatchParticipationDetail buildDetail(
            FootballMatchEntity footballMatch,
            AppTeamEntity appTeam,
            PlayerEntity currentPlayer
    ) {
        List<MatchParticipationMemberDTO> attendingPlayers = new ArrayList<>();
        List<MatchParticipationMemberDTO> attendingFans = new ArrayList<>();
        List<MatchParticipationMemberDTO> maybePlayers = new ArrayList<>();
        List<MatchParticipationMemberDTO> maybeFans = new ArrayList<>();
        List<MatchParticipationMemberDTO> notAttendingPlayers = new ArrayList<>();
        List<MatchParticipationMemberDTO> notAttendingFans = new ArrayList<>();
        MatchParticipationStatus currentStatus = null;

        List<MatchParticipationCommentEntity> comments = commentRepository
                .findAllByParticipationFootballMatchIdAndParticipationAppTeamIdOrderByCreatedAtAsc(
                        footballMatch.getId(),
                        appTeam.getId()
                );
        List<MatchParticipationCommentReactionEntity> reactions = reactionRepository
                .findAllByCommentParticipationFootballMatchIdAndCommentParticipationAppTeamId(
                        footballMatch.getId(),
                        appTeam.getId()
                );

        Map<Long, List<MatchParticipationCommentEntity>> commentsByParticipation = new LinkedHashMap<>();
        for (MatchParticipationCommentEntity comment : comments) {
            commentsByParticipation
                    .computeIfAbsent(comment.getParticipation().getId(), ignored -> new ArrayList<>())
                    .add(comment);
        }
        Map<Long, List<MatchParticipationCommentReactionEntity>> reactionsByComment = new LinkedHashMap<>();
        for (MatchParticipationCommentReactionEntity reaction : reactions) {
            reactionsByComment
                    .computeIfAbsent(reaction.getComment().getId(), ignored -> new ArrayList<>())
                    .add(reaction);
        }

        for (MatchParticipationEntity participation : participationRepository
                .findAllByFootballMatchIdAndAppTeamIdOrderByPlayerNameAsc(
                        footballMatch.getId(),
                        appTeam.getId()
                )) {
            if (!isTeamParticipant(participation.getPlayer(), appTeam)) {
                continue;
            }

            MatchParticipationMemberDTO member = new MatchParticipationMemberDTO(
                    playerMapper.toDTO(participation.getPlayer()),
                    buildCommentTree(
                            commentsByParticipation.getOrDefault(participation.getId(), List.of()),
                            reactionsByComment,
                            currentPlayer
                    )
            );
            boolean fan = participation.getPlayer().isFan();
            switch (participation.getStatus()) {
                case ATTENDING -> (fan ? attendingFans : attendingPlayers).add(member);
                case MAYBE -> (fan ? maybeFans : maybePlayers).add(member);
                case NOT_ATTENDING -> (fan ? notAttendingFans : notAttendingPlayers).add(member);
            }
            if (currentPlayer != null
                    && Objects.equals(participation.getPlayer().getId(), currentPlayer.getId())) {
                currentStatus = participation.getStatus();
            }
        }

        MatchParticipationDetail detail = new MatchParticipationDetail();
        detail.setFootballMatch(footballMatchService.getFootballMatchById(footballMatch.getId()));
        detail.setCurrentPlayer(currentPlayer == null ? null : playerMapper.toDTO(currentPlayer));
        detail.setCurrentStatus(currentStatus);
        detail.setAttendingPlayers(attendingPlayers);
        detail.setAttendingFans(attendingFans);
        detail.setMaybePlayers(maybePlayers);
        detail.setMaybeFans(maybeFans);
        detail.setNotAttendingPlayers(notAttendingPlayers);
        detail.setNotAttendingFans(notAttendingFans);
        detail.setEligiblePlayers(currentPlayer == null ? getEligibleParticipants(appTeam.getId()) : List.of());
        return detail;
    }

    private List<MatchParticipationCommentDTO> buildCommentTree(
            List<MatchParticipationCommentEntity> comments,
            Map<Long, List<MatchParticipationCommentReactionEntity>> reactionsByComment,
            PlayerEntity currentPlayer
    ) {
        Map<Long, MatchParticipationCommentDTO> mapped = new LinkedHashMap<>();
        for (MatchParticipationCommentEntity comment : comments) {
            int upVotes = 0;
            int downVotes = 0;
            MatchParticipationCommentReactionType currentReaction = null;
            for (MatchParticipationCommentReactionEntity reaction
                    : reactionsByComment.getOrDefault(comment.getId(), List.of())) {
                if (reaction.getReaction() == MatchParticipationCommentReactionType.UP) {
                    upVotes++;
                } else {
                    downVotes++;
                }
                if (currentPlayer != null
                        && Objects.equals(reaction.getPlayer().getId(), currentPlayer.getId())) {
                    currentReaction = reaction.getReaction();
                }
            }
            mapped.put(comment.getId(), new MatchParticipationCommentDTO(
                    comment.getId(),
                    playerMapper.toDTO(comment.getAuthor()),
                    comment.getText(),
                    comment.getCreatedAt(),
                    upVotes,
                    downVotes,
                    currentReaction,
                    new ArrayList<>()
            ));
        }

        List<MatchParticipationCommentDTO> roots = new ArrayList<>();
        for (MatchParticipationCommentEntity comment : comments) {
            MatchParticipationCommentDTO dto = mapped.get(comment.getId());
            if (comment.getParentComment() == null) {
                roots.add(dto);
            } else {
                MatchParticipationCommentDTO parent = mapped.get(comment.getParentComment().getId());
                if (parent == null) {
                    roots.add(dto);
                } else {
                    parent.getReplies().add(dto);
                }
            }
        }
        return roots;
    }

    private void addCommentIfPresent(
            MatchParticipationEntity participation,
            PlayerEntity author,
            String text,
            MatchParticipationCommentEntity parent
    ) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        String normalized = text.trim();
        if (normalized.length() > 1000) {
            throw validationError("comment", "Komentář může mít maximálně 1000 znaků.");
        }
        MatchParticipationCommentEntity comment = new MatchParticipationCommentEntity();
        comment.setParticipation(participation);
        comment.setAuthor(author);
        comment.setParentComment(parent);
        comment.setText(normalized);
        comment.setCreatedAt(Instant.now());
        commentRepository.save(comment);
    }

    private List<PlayerDTO> getEligibleParticipants(Long appTeamId) {
        return playerRepository.getAll(appTeamId).stream()
                .map(playerMapper::toDTO)
                .toList();
    }

    private PlayerEntity resolveAndPairParticipant(
            UserTeamRole role,
            Long requestedPlayerId,
            AppTeamEntity appTeam
    ) {
        if (isTeamParticipant(role.getPlayer(), appTeam)) {
            if (requestedPlayerId != null && !Objects.equals(requestedPlayerId, role.getPlayer().getId())) {
                throw validationError(
                        "playerId",
                        "Odpověď lze uložit jen za hráče nebo fanouška spárovaného s účtem."
                );
            }
            return role.getPlayer();
        }
        if (requestedPlayerId == null) {
            throw validationError("playerId", "Nejdřív vyber hráče nebo fanouška, za kterého odpovídáš.");
        }

        PlayerEntity player = playerRepository.findById(requestedPlayerId)
                .filter(candidate -> isTeamParticipant(candidate, appTeam))
                .orElseThrow(() -> new NotFoundException("Hráč nebo fanoušek v tomto týmu nebyl nalezen."));

        role.setPlayer(player);
        userTeamRoleRepository.save(role);
        return player;
    }

    private PlayerEntity requireCurrentParticipant(Long userId, AppTeamEntity appTeam) {
        PlayerEntity player = getCurrentRole(userId, appTeam.getId()).getPlayer();
        if (!isTeamParticipant(player, appTeam)) {
            throw validationError("player", "Pro komentáře a reakce nejdřív spáruj účet s hráčem nebo fanouškem.");
        }
        return player;
    }

    private boolean isTeamParticipant(PlayerEntity player, AppTeamEntity appTeam) {
        return player != null
                && player.getAppTeam() != null
                && Objects.equals(player.getAppTeam().getId(), appTeam.getId())
                && !player.isDeleted();
    }

    private boolean isPromptAllowed(PlayerEntity player, AppTeamEntity appTeam) {
        return switch (resolvePromptAudience(appTeam)) {
            case DISABLED -> false;
            case PLAYERS -> !player.isFan();
            case FANS -> player.isFan();
            case ALL_PAIRED -> true;
        };
    }

    private MatchParticipationPromptAudience resolvePromptAudience(AppTeamEntity appTeam) {
        return appTeam.getMatchParticipationPromptAudience() == null
                ? MatchParticipationPromptAudience.ALL_PAIRED
                : appTeam.getMatchParticipationPromptAudience();
    }

    private UserTeamRole getCurrentRole(Long userId, Long appTeamId) {
        return userTeamRoleRepository.findByUserIdAndAppTeamId(userId, appTeamId)
                .orElseThrow(() -> new NotFoundException("Uživatel nemá v aktuálním týmu roli."));
    }

    private FootballMatchEntity getFootballMatchForTeam(Long footballMatchId, AppTeamEntity appTeam) {
        FootballMatchEntity footballMatch = footballMatchRepository.findById(footballMatchId)
                .orElseThrow(() -> new NotFoundException("Zápas nebyl nalezen."));
        Long teamId = appTeam.getTeam() == null ? null : appTeam.getTeam().getId();
        Long homeTeamId = footballMatch.getHomeTeam() == null ? null : footballMatch.getHomeTeam().getId();
        Long awayTeamId = footballMatch.getAwayTeam() == null ? null : footballMatch.getAwayTeam().getId();
        if (teamId == null || (!Objects.equals(teamId, homeTeamId) && !Objects.equals(teamId, awayTeamId))) {
            throw new NotFoundException("Zápas nepatří do aktuálního týmu.");
        }
        return footballMatch;
    }

    private void validateNewPlayer(PlayerDTO player) {
        List<ValidationField> fields = new ArrayList<>();
        if (player.getName() == null || player.getName().trim().isEmpty()) {
            fields.add(new ValidationField("name", "Vyplň jméno hráče nebo fanouška."));
        }
        if (player.getBirthday() == null) {
            fields.add(new ValidationField("birthday", "Vyplň datum narození."));
        }
        if (!fields.isEmpty()) {
            throw new FieldValidationException("Osobu se nepodařilo vytvořit.", fields);
        }
    }

    private FieldValidationException validationError(String field, String message) {
        return new FieldValidationException(message, List.of(new ValidationField(field, message)));
    }
}
