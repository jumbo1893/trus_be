package com.jumbo.trus.service.chat;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.chat.ChatMute;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.chat.ChatMuteRepository;
import com.jumbo.trus.service.notification.push.DeviceTokenCollector;
import com.jumbo.trus.service.notification.push.PushService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatPushServiceTest {
    @Test void reactionPushGoesOnlyToAuthorWhenRoomIsEnabled() throws Exception {
        UserTeamRoleRepository roles = mock(UserTeamRoleRepository.class);
        ChatMuteRepository mutes = mock(ChatMuteRepository.class);
        DeviceTokenCollector tokens = mock(DeviceTokenCollector.class);
        PushService push = mock(PushService.class);
        ChatPushService service = new ChatPushService(roles, mutes, tokens, push);
        AppTeamEntity team = new AppTeamEntity(); team.setId(3L);
        UserEntity author = new UserEntity(); author.setId(2L);
        UserTeamRole role = new UserTeamRole(); role.setAppTeam(team); role.setUser(author);
        when(roles.findByUserIdAndAppTeamId(2L, 3L)).thenReturn(Optional.of(role));
        DeviceToken authorToken = token(author, "11.0.0");
        when(tokens.getTokensByUserList(List.of(2L))).thenReturn(List.of(authorToken));

        service.notifyReaction(3L, 2L, 1L, "all", 12L, "Jan", "❤️", "Ahoj");
        verify(push).sendPush(eq(authorToken), eq("Uživatel Jan reagoval na zprávu"), eq("Ahoj ❤️"), eq(NotificationType.CHAT),
                argThat(data -> "all".equals(data.get("room")) && "12".equals(data.get("messageId"))));
        verify(tokens).getTokensByUserList(List.of(2L));

        clearInvocations(push);
        String longText = "x".repeat(55);
        service.notifyReaction(3L, 2L, 1L, "all", 12L, "Jan", "👍", longText);
        verify(push).sendPush(eq(authorToken), anyString(), eq("x".repeat(50) + "... 👍"),
                eq(NotificationType.CHAT), anyMap());

        clearInvocations(push);
        service.notifyReaction(3L, 2L, 2L, "all", 12L, "Jan", "❤️", "Ahoj");
        verifyNoInteractions(push);

        ChatMute mute = new ChatMute(); mute.setMuted(true);
        when(mutes.findByAppTeamIdAndUserIdAndRoom(3L, 2L, "all")).thenReturn(Optional.of(mute));
        service.notifyReaction(3L, 2L, 1L, "all", 12L, "Jan", "❤️", "Ahoj");
        verifyNoInteractions(push);
    }

    @Test void sendsOnlyToUnmutedOtherMembersOnVersionElevenOrNewer() throws Exception {
        UserTeamRoleRepository roles = mock(UserTeamRoleRepository.class);
        ChatMuteRepository mutes = mock(ChatMuteRepository.class);
        DeviceTokenCollector tokens = mock(DeviceTokenCollector.class);
        PushService push = mock(PushService.class);
        ChatPushService service = new ChatPushService(roles, mutes, tokens, push);
        AppTeamEntity team = new AppTeamEntity(); team.setId(3L);
        UserEntity author = new UserEntity(); author.setId(1L);
        UserEntity recipient = new UserEntity(); recipient.setId(2L);
        UserTeamRole authorRole = new UserTeamRole(); authorRole.setAppTeam(team); authorRole.setUser(author);
        UserTeamRole recipientRole = new UserTeamRole(); recipientRole.setAppTeam(team); recipientRole.setUser(recipient);
        when(roles.findAllByAppTeamId(3L)).thenReturn(List.of(authorRole, recipientRole));

        DeviceToken old = token(recipient, "10.9.9");
        DeviceToken unknown = token(recipient, null);
        DeviceToken current = token(recipient, "11.0.0");
        DeviceToken newer = token(recipient, "11.2.0");
        when(tokens.getTokensByUserList(anyList())).thenReturn(List.of(old, unknown, current, newer));
        when(mutes.findByAppTeamIdAndUserIdAndRoom(3L, 2L, "all")).thenReturn(Optional.empty());

        var message = new ChatService.Message(12L, 1L, "Autor", "Ahoj", Instant.now(), false, List.of(), false, null);
        service.notifyMembers(3L, 1L, "all", message);
        verify(push, times(1)).sendPush(eq(current), eq("Autor:"), eq("Ahoj"), eq(NotificationType.CHAT), anyMap());
        verify(push, times(1)).sendPush(eq(newer), eq("Autor:"), eq("Ahoj"), eq(NotificationType.CHAT), anyMap());
        verify(push, never()).sendPush(eq(old), anyString(), anyString(), any(), anyMap());
        verify(push, never()).sendPush(eq(unknown), anyString(), anyString(), any(), anyMap());

        clearInvocations(push);
        ChatMute mute = new ChatMute(); mute.setMuted(true);
        when(mutes.findByAppTeamIdAndUserIdAndRoom(3L, 2L, "all")).thenReturn(Optional.of(mute));
        service.notifyMembers(3L, 1L, "all", message);
        verifyNoInteractions(push);
    }

    private DeviceToken token(UserEntity user, String version) {
        DeviceToken token = new DeviceToken();
        token.setUser(user);
        token.setStatus("ACTIVE");
        token.setAppVersion(version);
        return token;
    }
}
