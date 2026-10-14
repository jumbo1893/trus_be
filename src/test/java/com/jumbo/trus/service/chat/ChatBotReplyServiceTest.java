package com.jumbo.trus.service.chat;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.chat.ChatMessage;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.chat.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatBotReplyServiceTest {
    @Test void removesOnlyBotNameAtStartOfAnswer() {
        assertEquals("**42 piv** letos.", ChatBotReplyService.stripNamePrefix("TrusBot: **42 piv** letos."));
        assertEquals("Dolly psal: TrusBot měl pravdu.",
                ChatBotReplyService.stripNamePrefix("Dolly psal: TrusBot měl pravdu."));
    }

    @Test void persistsBotIdentityThenPublishesAfterCommit() {
        UserTeamRoleRepository roles = mock(UserTeamRoleRepository.class);
        ChatMessageRepository messages = mock(ChatMessageRepository.class);
        SimpMessagingTemplate websocket = mock(SimpMessagingTemplate.class);
        ChatPushService push = mock(ChatPushService.class);
        ChatBotReplyService service = new ChatBotReplyService(roles, messages, websocket, push);
        UserEntity user = new UserEntity(); user.setId(4L);
        AppTeamEntity team = new AppTeamEntity(); team.setId(7L);
        UserTeamRole role = new UserTeamRole(); role.setUser(user); role.setAppTeam(team);
        when(roles.findByUserIdAndAppTeamId(4L, 7L)).thenReturn(Optional.of(role));
        AtomicReference<ChatMessage> saved = new AtomicReference<>();
        when(messages.saveAndFlush(any())).thenAnswer(invocation -> {
            ChatMessage value = invocation.getArgument(0);
            value.setId(15L);
            saved.set(value);
            return value;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.save(7L, 4L, "all", "Ano, Dolly má pravdu.");
            assertTrue(saved.get().isBot());
            assertEquals("TrusBot", saved.get().getAuthorName());
            verifyNoInteractions(websocket, push);
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
            verify(websocket).convertAndSend(eq("/topic/chat/7/all"), argThat((ChatService.Message message) ->
                    message.authorId() == 0L && "TrusBot".equals(message.author())));
            verify(push).notifyMembers(eq(7L), eq(0L), eq("all"), any(ChatService.Message.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
