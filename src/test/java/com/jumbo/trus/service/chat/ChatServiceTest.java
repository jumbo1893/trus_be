package com.jumbo.trus.service.chat;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.chat.ChatMessage;
import com.jumbo.trus.entity.chat.ChatReaction;
import com.jumbo.trus.entity.chat.ChatMute;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.chat.ChatMessageRepository;
import com.jumbo.trus.repository.chat.ChatReadRepository;
import com.jumbo.trus.repository.chat.ChatReactionRepository;
import com.jumbo.trus.repository.chat.ChatMuteRepository;
import com.jumbo.trus.service.notification.settings.EnabledPushNotificationService;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.header.HeaderManager;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {
    private final UserService users = mock(UserService.class);
    private final HeaderManager headers = mock(HeaderManager.class);
    private final UserTeamRoleRepository roles = mock(UserTeamRoleRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final ChatReadRepository reads = mock(ChatReadRepository.class);
    private final ChatReactionRepository reactions = mock(ChatReactionRepository.class);
    private final ChatMuteRepository mutes = mock(ChatMuteRepository.class);
    private final EnabledPushNotificationService pushSettings = mock(EnabledPushNotificationService.class);
    private final ChatAccessService access = new ChatAccessService(roles);
    private final ChatService chat = new ChatService(users, headers, roles, messages, reads, reactions,
            mutes, pushSettings,
            mock(SimpMessagingTemplate.class), mock(ChatPushService.class), mock(ChatBotService.class));

    @Test void replyKeepsItsParentAndRejectsAnotherRoom() throws Exception {
        UserEntity user = new UserEntity(); user.setId(8L);
        AppTeamEntity team = new AppTeamEntity(); team.setId(3L);
        UserTeamRole role = new UserTeamRole(); role.setUser(user); role.setAppTeam(team);
        ChatMessage parent = new ChatMessage(); parent.setId(12L); parent.setAppTeam(team);
        parent.setRoom("all"); parent.setAuthorName("Jan"); parent.setText("Původní zpráva");
        when(users.getCurrentUserEntity()).thenReturn(user);
        when(headers.getAppTeamIdHeader()).thenReturn(3L);
        when(roles.findByUserIdAndAppTeamId(8L, 3L)).thenReturn(Optional.of(role));
        when(messages.findById(12L)).thenReturn(Optional.of(parent));
        when(messages.saveAndFlush(any())).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setId(13L);
            return saved;
        });
        TransactionSynchronizationManager.initSynchronization();
        try {
            var reply = chat.send("all", "Odpověď", null, 12L);
            assertEquals(12L, reply.replyTo().id());
            assertEquals("Jan", reply.replyTo().author());
            assertEquals("Původní zpráva", reply.replyTo().text());
            parent.setRoom("players");
            assertThrows(IllegalArgumentException.class, () -> chat.send("all", "Odpověď", null, 12L));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test void onlyPairedNonFanMembersSeePlayersRoom() {
        UserEntity user = new UserEntity(); user.setId(8L);
        AppTeamEntity team = new AppTeamEntity(); team.setId(3L);
        UserTeamRole role = new UserTeamRole(); role.setUser(user); role.setAppTeam(team);
        when(users.getCurrentUserEntity()).thenReturn(user);
        when(headers.getAppTeamIdHeader()).thenReturn(3L);
        when(roles.findByUserIdAndAppTeamId(8L, 3L)).thenReturn(Optional.of(role));
        when(reads.findByAppTeamIdAndUserIdAndRoom(eq(3L), eq(8L), anyString())).thenReturn(Optional.empty());

        assertEquals(1, chat.rooms().size());
        assertFalse(access.allowed(3L, 8L, "players"));

        PlayerEntity player = new PlayerEntity(); player.setFan(true); role.setPlayer(player);
        assertEquals(1, chat.rooms().size());
        assertFalse(access.allowed(3L, 8L, "players"));

        player.setFan(false);
        assertEquals(2, chat.rooms().size());
        assertTrue(access.allowed(3L, 8L, "players"));
    }

    @Test void membershipIsScopedToSelectedTeam() {
        when(roles.findByUserIdAndAppTeamId(8L, 3L)).thenReturn(Optional.empty());
        assertFalse(access.allowed(3L, 8L, "all"));
    }

    @Test void reactionIsStoredForTheCurrentMemberAndReturnedWithTheirName() {
        UserEntity user = new UserEntity(); user.setId(8L); user.setName("Jan Novák");
        AppTeamEntity team = new AppTeamEntity(); team.setId(3L);
        UserTeamRole role = new UserTeamRole(); role.setUser(user); role.setAppTeam(team);
        ChatMessage message = new ChatMessage(); message.setId(12L); message.setAppTeam(team); message.setRoom("all");
        when(users.getCurrentUserEntity()).thenReturn(user);
        when(headers.getAppTeamIdHeader()).thenReturn(3L);
        when(roles.findByUserIdAndAppTeamId(8L, 3L)).thenReturn(Optional.of(role));
        when(messages.findById(12L)).thenReturn(Optional.of(message));
        when(reactions.findByMessageIdAndUserId(12L, 8L)).thenReturn(Optional.empty());
        AtomicReference<ChatReaction> saved = new AtomicReference<>();
        when(reactions.saveAndFlush(any())).thenAnswer(invocation -> {
            saved.set(invocation.getArgument(0));
            return saved.get();
        });
        when(reactions.findByMessageId(12L)).thenAnswer(invocation -> List.of(saved.get()));

        TransactionSynchronizationManager.initSynchronization();
        try {
            var update = chat.setReaction("all", 12L, "❤️");
            assertEquals(12L, update.messageId());
            assertEquals("Jan Novák", update.reactions().get(0).name());
            assertEquals("❤️", update.reactions().get(0).emoji());
            assertEquals(8L, update.reactions().get(0).userId());
            assertThrows(IllegalArgumentException.class, () -> chat.setReaction("all", 12L, "invalid"));
            message.setRoom("players");
            assertThrows(SecurityException.class, () -> chat.setReaction("all", 12L, "👍"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test void bellChangesPushPreferenceForTheSelectedRoom() {
        UserEntity user = new UserEntity(); user.setId(8L);
        AppTeamEntity team = new AppTeamEntity(); team.setId(3L);
        UserTeamRole role = new UserTeamRole(); role.setUser(user); role.setAppTeam(team);
        when(users.getCurrentUserEntity()).thenReturn(user);
        when(headers.getAppTeamIdHeader()).thenReturn(3L);
        when(roles.findByUserIdAndAppTeamId(8L, 3L)).thenReturn(Optional.of(role));
        when(pushSettings.isChatTypeEnabled(user)).thenReturn(true);
        when(pushSettings.isNotificationEnabled(user, NotificationType.CHAT)).thenReturn(true);
        AtomicReference<ChatMute> stored = new AtomicReference<>();
        when(mutes.findByAppTeamIdAndUserIdAndRoom(3L, 8L, "all"))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(mutes.save(any())).thenAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return stored.get();
        });

        assertFalse(chat.setPushEnabled("all", false).pushEnabled());
        assertTrue(stored.get().isMuted());
        assertTrue(chat.setPushEnabled("all", true).pushEnabled());
        assertFalse(stored.get().isMuted());
        verify(mutes, never()).save(argThat(mute -> "players".equals(mute.getRoom())));
    }
}
