package com.jumbo.trus.service.chat;

import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.chat.ChatMuteRepository;
import com.jumbo.trus.service.appnotice.AppVersionComparator;
import com.jumbo.trus.service.notification.push.DeviceTokenCollector;
import com.jumbo.trus.service.notification.push.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service @RequiredArgsConstructor @Slf4j
public class ChatPushService {
    private final UserTeamRoleRepository roles;
    private final ChatMuteRepository mutes;
    private final DeviceTokenCollector tokens;
    private final PushService push;

    @Async
    public void notifyMembers(Long teamId, Long authorId, String kind, ChatService.Message message) {
        try {
            Set<Long> recipients = new HashSet<>();
            for (UserTeamRole role : roles.findAllByAppTeamId(teamId)) {
                if (!role.getUser().getId().equals(authorId)
                        && (!"players".equals(kind) || role.getPlayer() != null
                            && !role.getPlayer().isFan() && !role.getPlayer().isDeleted())) {
                    recipients.add(role.getUser().getId());
                }
            }
            for (var token : tokens.getTokensByUserList(new ArrayList<>(recipients))) {
                if (!"ACTIVE".equals(token.getStatus())) continue;
                if (!AppVersionComparator.isWithinRange(token.getAppVersion(), "11.0.0", null)) continue;
                if (mutes.findByAppTeamIdAndUserIdAndRoom(teamId, token.getUser().getId(), kind)
                        .map(mute -> mute.isMuted()).orElse(false)) continue;
                try {
                    String preview = message.text() == null ? "📷 Obrázek" : message.text();
                    if (preview.length() > 180) preview = preview.substring(0, 180) + "…";
                    push.sendPush(token, message.author() + ":",
                            preview, NotificationType.CHAT,
                            Map.of("type", "chat", "notificationType", "CHAT", "screenId", "chat-screen", "appTeamId", teamId.toString(), "room", kind));
                } catch (Exception e) { log.warn("Chat push failed for token {}", token.getId(), e); }
            }
        } catch (Exception e) { log.warn("Chat push dispatch failed for message {}", message.id(), e); }
    }
}
