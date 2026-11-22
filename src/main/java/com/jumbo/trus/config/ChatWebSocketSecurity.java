package com.jumbo.trus.config;

import com.jumbo.trus.config.security.firebase.FirebaseAuthenticationService;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.repository.PlayerRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.service.chat.ChatAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component @RequiredArgsConstructor
public class ChatWebSocketSecurity implements WebSocketMessageBrokerConfigurer {
    private static final Pattern CHAT_DESTINATION = Pattern.compile("^/topic/chat/(\\d+)/(all|players|match-\\d+|planned-\\d+)(?:/(?:read|reactions|typing))?$");
    private static final Pattern TEAM_PLAYER_STATS_DESTINATION = Pattern.compile("^/topic/team/(\\d+)/player/stats/(\\d+)$");
    private static final Pattern LEGACY_PLAYER_STATS_DESTINATION = Pattern.compile("^/topic/player/stats(\\d+)$");
    private final FirebaseAuthenticationService authentication;
    private final ChatAccessService chatAccess;
    private final UserTeamRoleRepository userTeamRoles;
    private final PlayerRepository players;

    /**
     * Klienti do verze, která posílá Authorization i pro statistiky hráče, se připojují anonymně.
     * Po vymření starých verzí nastavit na false.
     */
    @Value("${websocket.allow-anonymous-player-stats:true}")
    private boolean allowAnonymousPlayerStats;

    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String bearer = accessor.getFirstNativeHeader("Authorization");
                    if (bearer != null && bearer.startsWith("Bearer ")) {
                        try { accessor.setUser(authentication.authenticate(bearer.substring(7))); }
                        catch (Exception e) { throw new SecurityException("Neplatné přihlášení k chatu", e); }
                    }
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if (destination != null && !subscriptionAllowed(destination, currentUser(accessor.getUser())))
                        throw new SecurityException("Nemáte přístup k odběru");
                }
                if (StompCommand.SEND.equals(accessor.getCommand()) && accessor.getDestination() != null
                        && accessor.getDestination().startsWith("/topic/")) throw new SecurityException("Do topicu nelze posílat");
                return message;
            }
        });
    }

    private boolean subscriptionAllowed(String destination, UserEntity user) {
        if (!destination.startsWith("/topic/")) return true;
        Matcher chat = CHAT_DESTINATION.matcher(destination);
        if (chat.matches()) {
            return user != null && chatAccess.allowed(Long.parseLong(chat.group(1)), user.getId(), chat.group(2));
        }
        Matcher teamStats = TEAM_PLAYER_STATS_DESTINATION.matcher(destination);
        if (teamStats.matches()) {
            if (user == null) return anonymousPlayerStatsAllowed(destination);
            return userTeamRoles.findByUserIdAndAppTeamId(user.getId(), Long.parseLong(teamStats.group(1))).isPresent();
        }
        Matcher legacyStats = LEGACY_PLAYER_STATS_DESTINATION.matcher(destination);
        if (legacyStats.matches()) {
            if (user == null) return anonymousPlayerStatsAllowed(destination);
            return players.findById(Long.parseLong(legacyStats.group(1)))
                    .filter(player -> player.getAppTeam() != null)
                    .map(player -> userTeamRoles.findByUserIdAndAppTeamId(user.getId(), player.getAppTeam().getId()).isPresent())
                    .orElse(false);
        }
        return false;
    }

    private boolean anonymousPlayerStatsAllowed(String destination) {
        if (!allowAnonymousPlayerStats) return false;
        log.debug("Anonymous subscription to {} allowed for legacy clients", destination);
        return true;
    }

    private static UserEntity currentUser(Principal principal) {
        return principal instanceof Authentication auth && auth.getPrincipal() instanceof UserEntity user ? user : null;
    }
}
