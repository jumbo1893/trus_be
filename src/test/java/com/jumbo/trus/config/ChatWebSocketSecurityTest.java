package com.jumbo.trus.config;

import com.jumbo.trus.config.security.firebase.FirebaseAuthenticationService;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.chat.ChatAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatWebSocketSecurityTest {

    private final UserTeamRoleRepository roles = mock(UserTeamRoleRepository.class);
    private final PlayerRepository players = mock(PlayerRepository.class);
    private final ChatWebSocketSecurity security = new ChatWebSocketSecurity(
            mock(FirebaseAuthenticationService.class), mock(ChatAccessService.class), roles, players);
    private ChannelInterceptor interceptor;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(security, "allowAnonymousPlayerStats", true);
        ChannelRegistration registration = new ChannelRegistration();
        security.configureClientInboundChannel(registration);
        @SuppressWarnings("unchecked")
        List<ChannelInterceptor> interceptors = (List<ChannelInterceptor>) ReflectionTestUtils.getField(registration, "interceptors");
        interceptor = interceptors.get(0);
        user = new UserEntity();
        user.setId(5L);
    }

    @Test
    void memberCanSubscribeToOwnTeamPlayerStats() {
        when(roles.findByUserIdAndAppTeamId(5L, 12L)).thenReturn(Optional.of(new UserTeamRole()));
        assertDoesNotThrow(() -> subscribe("/topic/team/12/player/stats/34", user));
    }

    @Test
    void userCannotSubscribeToForeignTeamPlayerStats() {
        when(roles.findByUserIdAndAppTeamId(5L, 13L)).thenReturn(Optional.empty());
        assertThrows(SecurityException.class, () -> subscribe("/topic/team/13/player/stats/34", user));
    }

    @Test
    void anonymousPlayerStatsFollowCompatibilityFlag() {
        assertDoesNotThrow(() -> subscribe("/topic/team/12/player/stats/34", null));
        ReflectionTestUtils.setField(security, "allowAnonymousPlayerStats", false);
        assertThrows(SecurityException.class, () -> subscribe("/topic/team/12/player/stats/34", null));
    }

    @Test
    void legacyPlayerStatsRequireMembershipInPlayersTeam() {
        AppTeamEntity team = new AppTeamEntity();
        team.setId(12L);
        PlayerEntity player = new PlayerEntity();
        player.setAppTeam(team);
        when(players.findById(34L)).thenReturn(Optional.of(player));
        when(roles.findByUserIdAndAppTeamId(5L, 12L)).thenReturn(Optional.empty());
        assertThrows(SecurityException.class, () -> subscribe("/topic/player/stats34", user));
    }

    @Test
    void unknownTopicsAndAnonymousChatAreRejected() {
        assertThrows(SecurityException.class, () -> subscribe("/topic/anything", user));
        assertThrows(SecurityException.class, () -> subscribe("/topic/chat/12/all", null));
    }

    private void subscribe(String destination, UserEntity principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (principal != null) {
            accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        }
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        interceptor.preSend(message, mock(MessageChannel.class));
    }
}
