package com.jumbo.trus.service.auth;

import com.jumbo.trus.dto.auth.UserDTO;
import com.jumbo.trus.repository.membership.MembershipAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserRegistrationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private MembershipAccountRepository membershipAccountRepository;

    @Test
    void registrationPersistsUserAndBaselineMembershipInOneTransaction() {
        UserDTO request = new UserDTO();
        request.setMail("registration-" + UUID.randomUUID() + "@example.com");
        request.setPassword("firebase-uid");
        request.setName("Registration test");

        UserDTO created = userService.create(request);

        assertTrue(created.getId() > 0);
        assertEquals(request.getMail(), created.getMail());
        assertTrue(membershipAccountRepository.findByUserIdForUpdate(created.getId()).isPresent());
    }
}
