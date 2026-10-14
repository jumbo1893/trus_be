package com.jumbo.trus.config;

import com.jumbo.trus.config.security.firebase.FirebaseAuthenticationService;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.chat.ChatAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import java.security.Principal;
import java.util.regex.Pattern;

@Component @RequiredArgsConstructor
public class ChatWebSocketSecurity implements WebSocketMessageBrokerConfigurer {
    private static final Pattern CHAT_DESTINATION = Pattern.compile("^/topic/chat/(\\d+)/(all|players)(?:/(?:read|reactions))?$");
    private final FirebaseAuthenticationService authentication;
    private final ChatAccessService chatAccess;

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
                    if (destination != null && destination.startsWith("/topic/chat/")) {
                        var match = CHAT_DESTINATION.matcher(destination);
                        Principal principal = accessor.getUser();
                        if (!match.matches() || !(principal instanceof org.springframework.security.core.Authentication auth)
                                || !(auth.getPrincipal() instanceof UserEntity user)
                                || !chatAccess.allowed(Long.parseLong(match.group(1)), user.getId(), match.group(2)))
                            throw new SecurityException("Nemáte přístup k chatu");
                    }
                }
                if (StompCommand.SEND.equals(accessor.getCommand()) && accessor.getDestination() != null
                        && accessor.getDestination().startsWith("/topic/chat/")) throw new SecurityException("Do topicu nelze posílat");
                return message;
            }
        });
    }
}
