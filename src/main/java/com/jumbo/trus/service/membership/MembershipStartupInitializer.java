package com.jumbo.trus.service.membership;

import com.jumbo.trus.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipStartupInitializer {

    private final UserRepository userRepository;
    private final MembershipService membershipService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeExistingUsers() {
        userRepository.findAll().forEach(user -> {
            try {
                membershipService.initializeBaseline(user.getId());
            } catch (RuntimeException exception) {
                log.error("Could not initialize membership baseline for userId={}", user.getId(), exception);
            }
        });
    }
}
