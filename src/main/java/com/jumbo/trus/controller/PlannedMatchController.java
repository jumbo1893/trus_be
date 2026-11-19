package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.match.PlannedMatchDTO;
import com.jumbo.trus.dto.match.PlannedMatchParticipationDTO;
import com.jumbo.trus.entity.participation.MatchParticipationStatus;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.match.PlannedMatchService;
import com.jumbo.trus.service.match.PlannedMatchParticipationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/planned-match")
@RequiredArgsConstructor
public class PlannedMatchController {
    private final PlannedMatchService service;
    private final PlannedMatchParticipationService participation;
    private final AppTeamService appTeams;

    @RoleRequired("READER")
    @GetMapping("/{id}/participation")
    public PlannedMatchParticipationDTO participation(@PathVariable Long id) {
        return participation.get(id, currentUserId(), appTeams.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @PostMapping("/{id}/participation")
    public PlannedMatchParticipationDTO respond(@PathVariable Long id, @RequestBody PlannedResponse request) {
        return participation.respond(id, currentUserId(), appTeams.getCurrentAppTeamOrThrow(),
                request.status(), request.playing());
    }

    public record PlannedResponse(MatchParticipationStatus status, Boolean playing) {}

    private Long currentUserId() {
        return ((UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    @RoleRequired("READER")
    @GetMapping
    public List<PlannedMatchDTO> list() {
        return service.list(appTeams.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/{id}")
    public PlannedMatchDTO get(@PathVariable Long id) {
        return service.get(id, appTeams.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/by-football/{footballMatchId}")
    public PlannedMatchDTO getByFootballMatch(@PathVariable Long footballMatchId) {
        return service.getByFootballMatch(footballMatchId, appTeams.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("EDITOR")
    @PostMapping
    public PlannedMatchDTO create(@RequestBody @Valid PlannedMatchDTO request) {
        return service.create(request, appTeams.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("EDITOR")
    @PutMapping("/{id}")
    public PlannedMatchDTO update(@PathVariable Long id, @RequestBody @Valid PlannedMatchDTO request) {
        return service.update(id, request, appTeams.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("EDITOR")
    @PostMapping("/{id}/complete")
    public PlannedMatchDTO complete(@PathVariable Long id) {
        return service.complete(id, appTeams.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("EDITOR")
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadPhoto(@PathVariable Long id, @RequestPart("image") MultipartFile image) throws IOException {
        service.savePhoto(id, appTeams.getCurrentAppTeamOrThrow(), image);
    }

    @RoleRequired("READER")
    @GetMapping(value = "/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).header("Cache-Control", "no-store")
                .body(service.photo(id, appTeams.getCurrentAppTeamOrThrow()));
    }

    @RoleRequired("EDITOR")
    @DeleteMapping("/{id}/photo")
    public void deletePhoto(@PathVariable Long id) {
        service.deletePhoto(id, appTeams.getCurrentAppTeamOrThrow());
    }
}
