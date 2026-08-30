package com.jumbo.trus.controller;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.config.security.firebase.FirebaseIdentity;
import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.dto.auth.UserSetup;
import com.jumbo.trus.dto.auth.registration.RegistrationSetup;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.football.team.TeamService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@ControllerAdvice
@RestController
@RequestMapping("/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    final UserService userService;
    final AppTeamService appTeamService;
    final TeamService teamService;

    @PostMapping("/create")
    public UserDTO createUser(@RequestBody UserDTO userDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof FirebaseIdentity identity) {
            return userService.provisionFirebaseUser(identity, userDTO.getName());
        }
        if (authentication != null && authentication.getPrincipal() instanceof UserEntity user) {
            return userService.returnUserWithoutSensitiveData(user);
        }
        // Přechodný fallback pro již vydané verze aplikace.
        return userService.create(userDTO);
    }

    @PostMapping({"/auth", "/auth/"})
    public UserDTO login(@RequestBody @Valid UserDTO userDTO, HttpServletRequest req) throws ServletException {
        req.login(userDTO.getMail().toLowerCase().trim(), userDTO.getPassword());
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.returnUserWithoutSensitiveData(user);
    }

    @GetMapping("/registration-setup")
    public RegistrationSetup getRegistrationSetup() {
        return teamService.getRegistrationSetup();
    }

    @DeleteMapping("/delete")
    @RoleRequired("NONE")
    public void deleteUser(HttpServletRequest req) throws NotFoundException, ServletException {
        try {
            UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            userService.deleteUser(user.getId());
            req.logout();
        } catch (ClassCastException e) {
            throw new AuthException("Uživatel je odhlášen", AuthException.NOT_LOGGED_IN);
        }
    }

    @DeleteMapping({"/auth",})
    @RoleRequired("NONE")
    public void logout(HttpServletRequest req) throws ServletException {
        req.logout();
    }

    @GetMapping("/get-all")
    @RoleRequired("READER")
    public List<UserDTO> getUsers(@RequestParam(required = false) Boolean appTeamTeamRolesOnly) {
        return userService.getAll(appTeamService.getCurrentAppTeamOrThrow().getId(), appTeamTeamRolesOnly);
    }

    @PostMapping("/update")
    @RoleRequired("NONE")
    public UserDTO editCurrentUser(@RequestBody UserDTO userDTO) throws AuthException {
        try {
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.editUser(user.getId(), userDTO);
        } catch (ClassCastException e) {
            throw new AuthException("Uživatel je odhlášen", AuthException.NOT_LOGGED_IN);
        }
    }

    @PutMapping("/{userId}")
    @RoleRequired("ADMIN")
    public UserDTO editCurrentUser(@PathVariable Long userId, @RequestBody UserDTO userDTO) {
        return userService.editUser(userId, userDTO);
    }

    @GetMapping("/auth")
    @RoleRequired("NONE")
    public UserDTO getCurrentUser() throws AuthException {
        return userService.getCurrentUser();
    }

    @GetMapping("/setup")
    @RoleRequired("READER")
    public UserSetup getUserSetup() {
        return userService.returnPlayerSetup(appTeamService.getCurrentAppTeamOrThrow());
    }

    @PostMapping("/player-add")
    @RoleRequired("READER")
    public void addPlayerToUserRole(@RequestBody PlayerDTO playerDTO) {
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        appTeamService.addPlayerToCurrentUser(user, playerDTO);
    }

    @PutMapping("/{userRoleId}/role-change")
    @RoleRequired("ADMIN")
    public void changeUserRole(@PathVariable Long userRoleId, @RequestParam String role) {
        appTeamService.changeUserRole(userRoleId, role);
    }

    @ExceptionHandler({ServletException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleServletException(ServletException e) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
}
