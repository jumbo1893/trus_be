package com.jumbo.trus.service.participation;

import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.entity.participation.*;
import com.jumbo.trus.repository.notification.push.DeviceTokenRepository;
import com.jumbo.trus.repository.participation.MatchParticipationRepository;
import com.jumbo.trus.service.notification.push.PushService;
import com.jumbo.trus.service.transaction.AfterCommitExecutor;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ParticipationNotificationServiceTest {
    @Test
    void sendsActionCommentAndConfirmedTotalToOthersOnly() throws Exception {
        var after = mock(AfterCommitExecutor.class);
        var tokens = mock(DeviceTokenRepository.class);
        var entries = mock(MatchParticipationRepository.class);
        var push = mock(PushService.class);
        var service = new ParticipationNotificationService(after, tokens, entries, push);
        doAnswer(invocation -> { ((Runnable) invocation.getArgument(1)).run(); return null; })
                .when(after).execute(anyString(), any());
        var attending = new MatchParticipationEntity();
        attending.setStatus(MatchParticipationStatus.ATTENDING);
        attending.setPlayer(new PlayerEntity());
        var maybe = new MatchParticipationEntity();
        maybe.setStatus(MatchParticipationStatus.MAYBE);
        maybe.setPlaying(true);
        var spectator = new MatchParticipationEntity();
        spectator.setStatus(MatchParticipationStatus.ATTENDING);
        spectator.setPlayer(new PlayerEntity());
        spectator.setPlaying(false);
        when(entries.findAllByFootballMatchIdAndAppTeamIdOrderByPlayerNameAsc(20L, 1L))
                .thenReturn(List.of(attending, spectator, maybe));
        var own = token(2L, "own");
        var other = token(3L, "other");
        when(tokens.findDeviceTokensByAppTeamIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(own, other, other));
        service.notifyChange(2L, 1L, 20L, "Petr: zúčastní se", "Přijdu pozdě");
        verify(push).sendPush(eq(other), anyString(),
                argThat(body -> body.contains("Hrajících účastníků: 1") && body.contains("Celkem se účastní: 2") && body.contains("Přijdu pozdě")
                        && body.contains("Petr: zúčastní se")), eq(NotificationType.MATCH_PARTICIPATION),
                argThat(data -> "20".equals(data.get("footballMatchId"))));
        verifyNoMoreInteractions(push);
    }

    private DeviceToken token(Long id, String value) {
        var user = new UserEntity(); user.setId(id);
        var token = new DeviceToken(); token.setUser(user); token.setToken(value);
        return token;
    }
}
