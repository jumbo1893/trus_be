package com.jumbo.trus.service.chat;

import com.jumbo.trus.entity.chat.ChatMessage;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.chat.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor @Slf4j
public class ChatBotReplyService {
    private static final Pattern NAME_PREFIX = Pattern.compile("(?iu)^\\s*(?:\\*\\*)?trusbot(?:\\*\\*)?\\s*[:：]\\s*");
    private final UserTeamRoleRepository roles;
    private final ChatMessageRepository messages;
    private final SimpMessagingTemplate websocket;
    private final ChatPushService push;

    @Transactional
    public void save(Long teamId, Long userId, String kind, String answer) {
        var role = roles.findByUserIdAndAppTeamId(userId, teamId)
                .orElseThrow(() -> new SecurityException("Uživatel už není členem týmu"));
        if ("players".equals(kind) && (role.getPlayer() == null || role.getPlayer().isFan()
                || role.getPlayer().isDeleted())) throw new SecurityException("Uživatel už není hráčem týmu");
        ChatMessage entity = new ChatMessage();
        entity.setAppTeam(role.getAppTeam());
        entity.setAuthor(role.getUser());
        entity.setRoom(kind);
        entity.setBot(true);
        entity.setAuthorName("TrusBot");
        String text = answer == null || answer.isBlank() ? "Teď nemám odpověď. Zkus to prosím později." : stripNamePrefix(answer);
        if (text.isBlank()) text = "Teď nemám odpověď. Zkus to prosím později.";
        entity.setText(text.length() > 4000 ? text.substring(0, 3999) + "…" : text);
        ChatMessage saved = messages.saveAndFlush(entity);
        ChatService.Message dto = new ChatService.Message(saved.getId(), 0L, "TrusBot", saved.getText(),
                saved.getCreatedAt(), false, List.of(), true);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { websocket.convertAndSend("/topic/chat/" + teamId + "/" + kind, dto); }
                catch (Exception e) { log.warn("Chat bot websocket delivery failed for message {}", dto.id(), e); }
                try { push.notifyMembers(teamId, 0L, kind, dto); }
                catch (Exception e) { log.warn("Chat bot push delivery failed for message {}", dto.id(), e); }
            }
        });
    }

    static String stripNamePrefix(String answer) {
        return NAME_PREFIX.matcher(answer).replaceFirst("").trim();
    }
}
