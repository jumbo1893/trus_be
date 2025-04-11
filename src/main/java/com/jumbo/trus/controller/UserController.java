package com.jumbo.trus.controller;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.exceptions.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.entity.auth.UserEntity;

import com.jumbo.trus.service.auth.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostMapping("/create")
    public UserDTO createUser(@RequestBody @Valid UserDTO userDTO) {
        return userService.create(userDTO);
    }

    @PostMapping("/migration")
    public void migrateUsers() {
        userService.migrateAllUsers(appTeamService.getCurrentAppTeamOrThrow());
    }

    @PostMapping({"/auth", "/auth/"})
    public UserDTO login(@RequestBody @Valid UserDTO userDTO, HttpServletRequest req) throws ServletException {
        req.login(userDTO.getMail().toLowerCase().trim(), userDTO.getPassword());
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.returnUserWithoutSensitiveData(user);
    }

    /*private List<UserTeamRoleDTO> getUserTeamList(List<UserTeamRole> entities) {
        List<UserTeamRoleDTO> roles = new ArrayList<>();
        for (UserTeamRole userTeamRole : entities) {
            roles.add(new UserTeamRoleDTO(userTeamRole.getId(), userTeamRole.getUser().getId(), userTeamRole.getAppTeam(), userTeamRole.getRole()));
        }
        return roles;
    }*/

    @DeleteMapping("/delete")
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
    public void logout(HttpServletRequest req) throws ServletException {
        req.logout();
    }

    @GetMapping("/get-all")
    public List<UserDTO> getUsers(@RequestParam(required = false) Boolean appTeamTeamRolesOnly) {
        return userService.getAll(appTeamService.getCurrentAppTeamOrThrow().getId(), appTeamTeamRolesOnly);
    }

    @PostMapping("/update")
    public UserDTO editCurrentUser(@RequestBody UserDTO userDTO) throws AuthException {
        try {
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userService.editUser(user.getId(), userDTO);
        } catch (ClassCastException e) {
            throw new AuthException("Uživatel je odhlášen", AuthException.NOT_LOGGED_IN);
        }
    }

    @PutMapping("/{userId}")
    public UserDTO editCurrentUser(@PathVariable Long userId, @RequestBody UserDTO userDTO) {
        return userService.editUser(userId, userDTO);
    }

    @GetMapping("/auth")
    public UserDTO getCurrentUser() throws AuthException {
        return userService.getCurrentUser();
    }

    @PostMapping("/player-add")
    public void addPlayerToUserRole(@RequestBody PlayerDTO playerDTO) {
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        appTeamService.addPlayerToCurrentUser(user, playerDTO);
    }

    @PutMapping("/{userRoleId}/role-change")
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
