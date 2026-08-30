package com.jumbo.trus.controller;

import com.jumbo.trus.aspect.PostCommitTask;
import com.jumbo.trus.aspect.appteam.StoreAppTeam;
import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.participation.MatchParticipationDetail;
import com.jumbo.trus.dto.participation.MatchParticipationCommentRequest;
import com.jumbo.trus.dto.participation.MatchParticipationPromptAudienceConfig;
import com.jumbo.trus.dto.participation.MatchParticipationReactionRequest;
import com.jumbo.trus.dto.participation.MatchParticipationRequest;
import com.jumbo.trus.dto.participation.NewPlayerParticipationRequest;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.participation.MatchParticipationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/match-participation")
@RequiredArgsConstructor
public class MatchParticipationController {

    private final MatchParticipationService participationService;
    private final AppTeamService appTeamService;

    @RoleRequired("READER")
    @GetMapping("/{footballMatchId}")
    public MatchParticipationDetail getDetail(@PathVariable Long footballMatchId) {
        return participationService.getDetail(
                footballMatchId,
                currentUser().getId(),
                appTeamService.getCurrentAppTeamOrThrow()
        );
    }

    @RoleRequired("READER")
    @PostMapping("/respond")
    public MatchParticipationDetail respond(@RequestBody @Valid MatchParticipationRequest request) {
        return participationService.respond(
                currentUser().getId(),
                appTeamService.getCurrentAppTeamOrThrow(),
                request
        );
    }

    @RoleRequired("READER")
    @PostMapping("/respond-with-new-player")
    @PostCommitTask
    @StoreAppTeam
    public MatchParticipationDetail createPlayerAndRespond(
            @RequestBody @Valid NewPlayerParticipationRequest request
    ) {
        AppTeamEntity appTeam = appTeamService.getCurrentAppTeamOrThrow();
        return participationService.createPlayerAndRespond(currentUser().getId(), appTeam, request);
    }

    @RoleRequired("READER")
    @PostMapping("/comment")
    public MatchParticipationDetail addComment(
            @RequestBody @Valid MatchParticipationCommentRequest request
    ) {
        return participationService.addComment(
                currentUser().getId(),
                appTeamService.getCurrentAppTeamOrThrow(),
                request
        );
    }

    @RoleRequired("READER")
    @PostMapping("/comment/{commentId}/reaction")
    public MatchParticipationDetail reactToComment(
            @PathVariable Long commentId,
            @RequestBody @Valid MatchParticipationReactionRequest request
    ) {
        return participationService.reactToComment(
                currentUser().getId(),
                appTeamService.getCurrentAppTeamOrThrow(),
                commentId,
                request
        );
    }

    @RoleRequired("READER")
    @DeleteMapping("/comment/{commentId}")
    public MatchParticipationDetail deleteComment(@PathVariable Long commentId) {
        return participationService.deleteComment(
                currentUser().getId(),
                appTeamService.getCurrentAppTeamOrThrow(),
                commentId
        );
    }

    @RoleRequired("READER")
    @GetMapping("/prompt-audience")
    public MatchParticipationPromptAudienceConfig getPromptAudience() {
        return participationService.getPromptAudience(appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("ADMIN")
    @PutMapping("/prompt-audience")
    public MatchParticipationPromptAudienceConfig updatePromptAudience(
            @RequestBody @Valid MatchParticipationPromptAudienceConfig config
    ) {
        return participationService.updatePromptAudience(
                appTeamService.getCurrentAppTeamOrThrow(),
                config
        );
    }

    private UserEntity currentUser() {
        return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
