package com.jumbo.trus.service.notification.push.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.auth.*;
import com.jumbo.trus.entity.football.FootballPlayerEntity;
import com.jumbo.trus.entity.notification.push.DeviceToken;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.notification.push.*;
import com.jumbo.trus.service.achievement.calendar.CzechCelebrationCalendar;
import com.jumbo.trus.service.notification.push.PushService;
import org.junit.jupiter.api.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CelebrationPushJobTest {
    DeviceTokenRepository tokens = mock(DeviceTokenRepository.class);
    SentPushNotificationRepository sent = mock(SentPushNotificationRepository.class);
    PushService push = mock(PushService.class);
    CelebrationPushJob job;

    @BeforeEach
    void setup() throws Exception {
        job = new CelebrationPushJob(tokens, sent, new CzechCelebrationCalendar(new ObjectMapper()), push);
        ReflectionTestUtils.setField(job, "clock", Clock.fixed(Instant.parse("2026-06-24T07:00:00Z"), ZoneOffset.UTC));
        when(sent.tryGreetingJobLock()).thenReturn(true);
    }

    UserEntity user(String birthday, String footballName) {
        var player = new PlayerEntity(); player.setId(7L);
        if (birthday != null) player.setBirthday(Date.from(LocalDate.parse(birthday).atStartOfDay(ZoneId.of("Europe/Prague")).toInstant()));
        if (footballName != null) {
            var fp = new FootballPlayerEntity(); fp.setName(footballName); player.setFootballPlayer(fp);
        }
        var user = new UserEntity(); user.setId(1L); user.setName("Jan");
        var role = new UserTeamRole(); role.setPlayer(player); role.setUser(user);
        user.getTeamRoles().add(role); return user;
    }
    DeviceToken token(UserEntity user, String value) {
        var token = new DeviceToken(); token.setUser(user); token.setToken(value);
        token.setStatus("ACTIVE"); return token;
    }

    @Test
    void skipsUnknownBirthdayAndOnlyUsesLinkedFootballNameForNameday() {
        assertThat(job.greetings(user("1990-01-01", null), LocalDate.of(2026, 1, 1))).isEmpty();
        assertThat(job.greetings(user(null, null), LocalDate.of(2026, 6, 24))).isEmpty();
        assertThat(job.greetings(user(null, "Novák Jan"), LocalDate.of(2026, 6, 24))).containsExactly(CelebrationPushJob.NAMEDAY);
        assertThat(job.greetings(user("1990-06-24", null), LocalDate.of(2026, 6, 24))).containsExactly(CelebrationPushJob.BIRTHDAY);
        assertThat(job.greetings(user(null, null), LocalDate.of(2026, 5, 1))).isEmpty();
    }

    @Test
    void leapBirthdaysAndDeletedPlayersAreNotMovedOrCongratulated() {
        var user = user("2000-02-29", null);
        assertThat(job.greetings(user, LocalDate.of(2025, 2, 28))).isEmpty();
        assertThat(job.greetings(user, LocalDate.of(2024, 2, 29))).containsExactly(CelebrationPushJob.BIRTHDAY);
        user.getTeamRoles().get(0).getPlayer().setDeleted(true);
        assertThat(job.greetings(user, LocalDate.of(2024, 2, 29))).isEmpty();
    }

    @Test
    void sendsBothGreetingsOncePerDeviceDespiteMultipleRolesAndDuplicateTokens() throws Exception {
        var user = user("1990-06-24", "Novák Jan");
        user.getTeamRoles().add(user.getTeamRoles().get(0));
        var first = token(user, "token"); first.setClientDeviceId("phone");
        var older = token(user, "old-token"); older.setClientDeviceId("phone");
        when(tokens.findDistinctByStatusOrderByModificationTimeDesc("ACTIVE")).thenReturn(List.of(first, first, older));
        job.sendDailyGreetings();
        verify(push).sendPush(first, "Všechno nejlepší!", CelebrationPushJob.BIRTHDAY, NotificationType.GLOBAL,
                Map.of("screenId", "home-screen", "type", "GLOBAL"));
        verify(push).sendPush(first, "Všechno nejlepší!", CelebrationPushJob.NAMEDAY, NotificationType.GLOBAL,
                Map.of("screenId", "home-screen", "type", "GLOBAL"));
        verifyNoMoreInteractions(push);
    }

    @Test
    void successfulDailyLogPreventsRepeatedSendAndReplicaLockSkipsJob() {
        var token = token(user("1990-06-24", null), "token");
        when(tokens.findDistinctByStatusOrderByModificationTimeDesc("ACTIVE")).thenReturn(List.of(token));
        when(sent.wasGreetingSent(anyLong(), anyString(), any(), anyString(), anyString(), any(), any())).thenReturn(true);
        job.sendDailyGreetings(); verifyNoInteractions(push);
        clearInvocations(tokens);
        when(sent.tryGreetingJobLock()).thenReturn(false);
        job.sendDailyGreetings(); verifyNoInteractions(tokens);
    }

    @Test
    void failedTokenDoesNotPreventOtherRecipients() throws Exception {
        var first = token(user("1990-06-24", null), "first");
        var second = token(user("1990-06-24", null), "second");
        when(tokens.findDistinctByStatusOrderByModificationTimeDesc("ACTIVE")).thenReturn(List.of(first, second));
        doThrow(new RuntimeException("FCM unavailable")).when(push).sendPush(eq(first), anyString(), anyString(), any(), anyMap());
        job.sendDailyGreetings();
        verify(push).sendPush(eq(second), anyString(), eq(CelebrationPushJob.BIRTHDAY), eq(NotificationType.GLOBAL), anyMap());
    }

    @Test
    void scheduleIsNineInPragueIncludingDaylightSavingTime() throws Exception {
        var schedule = CelebrationPushJob.class.getMethod("sendDailyGreetings").getAnnotation(Scheduled.class);
        assertThat(schedule.cron()).isEqualTo("0 0 9 * * *");
        assertThat(schedule.zone()).isEqualTo("Europe/Prague");
    }
}
