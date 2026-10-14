package com.jumbo.trus.service.chat;

import com.jumbo.trus.dto.ai.AiQuestionResponse;
import com.jumbo.trus.entity.ai.AiQuestionStatus;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.chat.ChatMessageRepository;
import com.jumbo.trus.service.ai.AiQuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatBotServiceTest {
    @Test void invocationMustStartTheMessageIgnoringCase() {
        assertTrue(ChatBotService.isInvocation("TrusBote, má Dolly pravdu?"));
        assertTrue(ChatBotService.isInvocation("  trusbote kolik piv?"));
        assertFalse(ChatBotService.isInvocation("Co říká TrusBote?"));
        assertFalse(ChatBotService.isInvocation("Trusbotem se neřiď"));
    }

    @Test void readsOnlyCurrentRoomAndPostsTheAnswer() {
        UserRepository users = mock(UserRepository.class);
        AppTeamRepository teams = mock(AppTeamRepository.class);
        UserTeamRoleRepository roles = mock(UserTeamRoleRepository.class);
        ChatMessageRepository messages = mock(ChatMessageRepository.class);
        AiQuestionService ai = mock(AiQuestionService.class);
        ChatBotReplyService replies = mock(ChatBotReplyService.class);
        ChatBotService service = new ChatBotService(users, teams, roles, messages, ai, replies);
        UserEntity user = new UserEntity(); user.setId(4L);
        AppTeamEntity team = new AppTeamEntity(); team.setId(7L);
        UserTeamRole role = new UserTeamRole(); role.setUser(user); role.setAppTeam(team);
        when(roles.findByUserIdAndAppTeamId(4L, 7L)).thenReturn(Optional.of(role));
        when(users.findById(4L)).thenReturn(Optional.of(user));
        when(teams.findById(7L)).thenReturn(Optional.of(team));
        ChatContextLine previous = new ChatContextLine("Dolly", "Vypitch piva byla dobrá.",
                Instant.parse("2026-10-13T18:00:00Z"));
        when(messages.findRecentText(eq(7L), eq("all"), eq(12L), any(Pageable.class)))
                .thenReturn(List.of(previous));
        when(ai.askForChat(eq(user), eq(team), eq("má Dolly pravdu?"), contains("Dolly: Vypitch piva byla dobrá.")))
                .thenReturn(new AiQuestionResponse(1L, "má Dolly pravdu?", "Ano.", AiQuestionStatus.COMPLETED,
                        Instant.now(), Instant.now(), null));

        service.answer(7L, 4L, "all", 12L, "TrusBote, má Dolly pravdu?");

        verify(messages).findRecentText(eq(7L), eq("all"), eq(12L), any(Pageable.class));
        verify(replies).save(7L, 4L, "all", "Ano.");
    }
}
