package com.jumbo.trus.controller;

import com.jumbo.trus.controller.error.ErrorResponse;
import com.jumbo.trus.service.exceptions.AuthException;
import org.springframework.http.HttpStatus;
import com.jumbo.trus.dto.UserDTO;
import com.jumbo.trus.entity.UserEntity;

import com.jumbo.trus.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;


import java.util.Arrays;
import java.util.List;

@ControllerAdvice
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;


    @PostMapping("/create")
    public UserDTO createUser(@RequestBody @Valid UserDTO userDTO) {
        return userService.create(userDTO);
    }

    @PostMapping({"/auth", "/auth/"})
    public UserDTO login(@RequestBody @Valid UserDTO userDTO, HttpServletRequest req) throws ServletException {
        req.login(userDTO.getMail().toLowerCase().trim(), userDTO.getPassword());
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDTO model = new UserDTO();
        model.setMail(user.getMail());
        model.setId(user.getId());
        model.setAdmin(user.isAdmin());
        model.setPlayerId(user.getPlayerId());
        model.setName(user.getName());
        return model;
    }

    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable Long userId) throws NotFoundException {
        userService.deleteUser(userId);
    }

    @DeleteMapping({"/auth",})
    public void logout(HttpServletRequest req) throws ServletException {
        req.logout();
    }

    @GetMapping("/get-all")
    public List<UserDTO> getUsers() {
        return userService.getAll();
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
        try {
            UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserDTO model = new UserDTO();
            if(user.getName() != null) {
                model.setName(user.getName());
            }
            model.setMail(user.getMail());
            model.setId(user.getId());
            model.setAdmin(user.isAdmin());
            model.setPlayerId(user.getPlayerId());
            return model;
        } catch (ClassCastException e) {
            throw new AuthException("Uživatel je odhlášen", AuthException.NOT_LOGGED_IN);
        }
    }

    @ExceptionHandler({ServletException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleServletException(ServletException e) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
}
