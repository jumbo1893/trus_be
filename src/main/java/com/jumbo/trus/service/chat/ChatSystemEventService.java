package com.jumbo.trus.service.chat;

import com.jumbo.trus.entity.chat.ChatMessage;
import com.jumbo.trus.repository.PlannedMatchRepository;
import com.jumbo.trus.repository.chat.ChatMessageRepository;
import com.jumbo.trus.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSystemEventService {
    private final PlannedMatchRepository plannedMatches;
    private final ChatMessageRepository messages;
    private final UserService users;
    private final SimpMessagingTemplate websocket;

    @Transactional
    public void postParticipationEvent(Long teamId, Long userId, Long plannedMatchId, String text) {
        if (plannedMatchId == null || text == null || text.isBlank()) return;
        var planned = plannedMatches.findByIdAndAppTeamId(plannedMatchId, teamId).orElse(null);
        var author = users.getCurrentUserEntity();
        if (planned == null || !author.getId().equals(userId)) return;
        String kind = "planned-" + plannedMatchId;
        ChatMessage entity = new ChatMessage();
        entity.setAppTeam(planned.getAppTeam());
        entity.setAuthor(author);
        entity.setRoom(kind);
        entity.setAuthorName("Kurvíza");
        entity.setBot(true);
        entity.setText(text.length() > 4000 ? text.substring(0, 4000) : text);
        ChatMessage saved = messages.saveAndFlush(entity);
        ChatService.Message dto = new ChatService.Message(saved.getId(), 0L, "Kurvíza", saved.getText(),
                saved.getCreatedAt(), false, List.of(), true, true, null);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { websocket.convertAndSend("/topic/chat/" + teamId + "/" + kind, dto); }
                catch (Exception e) { log.warn("Chat event websocket delivery failed for message {}", dto.id(), e); }
            }
        });
    }
}
