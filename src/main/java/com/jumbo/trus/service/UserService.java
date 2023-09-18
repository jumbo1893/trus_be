package com.jumbo.trus.service;

import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.UserDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.FineEntity;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.UserEntity;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.repository.FineRepository;
import com.jumbo.trus.entity.repository.UserRepository;
import com.jumbo.trus.entity.repository.specification.MatchSpecification;
import com.jumbo.trus.mapper.FineMapper;
import com.jumbo.trus.service.exceptions.AuthException;
import com.jumbo.trus.service.exceptions.DuplicateEmailException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    public UserDTO create(UserDTO user) {
        try {
            UserEntity entity = new UserEntity();
            entity.setMail(user.getMail().toLowerCase().trim());
            entity.setPassword(passwordEncoder.encode(user.getPassword()));

            entity = userRepository.save(entity);

            UserDTO dto = new UserDTO();
            dto.setId(entity.getId());
            dto.setMail(entity.getMail());
            dto.setAdmin(entity.isAdmin());
            notificationService.addAdminNotification("Zaregistrován nový uživatel", entity.getMail());
            return dto;
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }}

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByMail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Uživatel " + username + " nenalezen"));
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public List<UserDTO> getAll(){
        List<UserDTO> userList = new ArrayList<>();
        List<UserEntity> entities = userRepository.findAll(PageRequest.of(0, 1000)).stream().toList();
        for (UserEntity entity : entities) {
            userList.add(returnUserWithoutSensitiveData(entity));
        }
        return userList;
    }

    public UserDTO editUser(Long userId, UserDTO user) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Uživatel s id " + userId + " nenalezen v db"));
        if(user.getAdmin() != null) {
            userEntity.setAdmin(user.getAdmin());
        }
        if(user.getName() != null) {
            userEntity.setName(user.getName());
        }
        else if(user.getPlayerId() != null) {
            userEntity.setPlayerId(user.getPlayerId());
        }
        return returnUserWithoutSensitiveData(userRepository.save(userEntity));
    }

    public UserDTO editUserById(Long userId, UserDTO user) throws NotFoundException {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Uživatel s id " + userId + " nenalezen v db"));
        userEntity.setAdmin(user.getAdmin());
        userEntity.setName(user.getName());
        userEntity.setPlayerId(user.getPlayerId());
        return returnUserWithoutSensitiveData(userRepository.save(userEntity));
    }

    public UserDTO getCurrentUser() {
        try {
            UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserDTO model = new UserDTO();
            model.setMail(user.getMail());
            model.setId(user.getId());
            model.setAdmin(user.isAdmin());
            model.setPlayerId(user.getPlayerId());
            model.setName(user.getName());
            return model;
        } catch (ClassCastException e) {
            throw new AuthException("Uživatel je odhlášen", AuthException.NOT_LOGGED_IN);
        }
    }

    private UserDTO returnUserWithoutSensitiveData(UserEntity entity) {
        UserDTO dto = new UserDTO();
        dto.setName(entity.getName());
        dto.setId(entity.getId());
        dto.setMail(entity.getMail());
        dto.setAdmin(entity.isAdmin());
        dto.setPlayerId(entity.getPlayerId());
        return dto;
    }
}
