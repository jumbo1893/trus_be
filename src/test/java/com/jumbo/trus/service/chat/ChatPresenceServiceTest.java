package com.jumbo.trus.service.chat;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.chat.ChatReadRepository;
import com.jumbo.trus.repository.chat.ChatMessageRepository;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.header.HeaderManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatPresenceServiceTest {
    @Test void onlineExcludesViewerAndCountsOtherUserOnlyOnce() {
        UserService users = mock(UserService.class);
        HeaderManager headers = mock(HeaderManager.class);
        UserTeamRoleRepository roles = mock(UserTeamRoleRepository.class);
        ChatReadRepository reads = mock(ChatReadRepository.class);
        ChatMessageRepository messages = mock(ChatMessageRepository.class);
        ChatPresenceService presence = new ChatPresenceService(users, headers, roles, reads, messages);
        UserEntity user = new UserEntity(); user.setId(8L); user.setName("Pavel");
        UserEntity other = new UserEntity(); other.setId(9L); other.setName("Eva");
        AppTeamEntity team = new AppTeamEntity(); team.setId(3L);
        UserTeamRole role = new UserTeamRole(); role.setUser(user); role.setAppTeam(team);
        UserTeamRole otherRole = new UserTeamRole(); otherRole.setUser(other); otherRole.setAppTeam(team);
        when(users.getCurrentUserEntity()).thenReturn(user);
        when(headers.getAppTeamIdHeader()).thenReturn(3L);
        when(roles.findByUserIdAndAppTeamId(8L, 3L)).thenReturn(Optional.of(role));
        when(roles.findByUserIdAndAppTeamId(9L, 3L)).thenReturn(Optional.of(otherRole));
        when(roles.findAllByAppTeamId(3L)).thenReturn(List.of(role, otherRole));

        presence.heartbeat("phone", true);
        presence.heartbeat("tablet", true);
        assertTrue(presence.online("all").isEmpty());
        when(users.getCurrentUserEntity()).thenReturn(other);
        presence.heartbeat("other-phone", true);
        when(users.getCurrentUserEntity()).thenReturn(user);
        assertEquals(List.of(new ChatPresenceService.OnlineUser(9L, "Eva")), presence.online("all"));
        presence.heartbeat("phone", false);
        assertEquals(1, presence.online("all").size());
        presence.heartbeat("tablet", false);
        assertEquals(1, presence.online("all").size());
        when(users.getCurrentUserEntity()).thenReturn(other);
        presence.heartbeat("other-phone", false);
        when(users.getCurrentUserEntity()).thenReturn(user);
        assertTrue(presence.online("all").isEmpty());
        assertThrows(SecurityException.class, () -> presence.online("players"));
    }
}
