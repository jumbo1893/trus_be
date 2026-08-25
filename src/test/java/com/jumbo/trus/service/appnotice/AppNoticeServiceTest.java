package com.jumbo.trus.service.appnotice;

import com.jumbo.trus.dto.appnotice.CurrentAppNoticeDTO;
import com.jumbo.trus.entity.appnotice.AppNoticeActionEntity;
import com.jumbo.trus.entity.appnotice.AppNoticeActionStyle;
import com.jumbo.trus.entity.appnotice.AppNoticeActionType;
import com.jumbo.trus.entity.appnotice.AppNoticeEntity;
import com.jumbo.trus.entity.appnotice.AppNoticeReceiptEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.repository.appnotice.AppNoticeReceiptRepository;
import com.jumbo.trus.repository.appnotice.AppNoticeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppNoticeServiceTest {

    private final AppNoticeRepository noticeRepository = mock(AppNoticeRepository.class);
    private final AppNoticeReceiptRepository receiptRepository = mock(AppNoticeReceiptRepository.class);
    private final AppNoticeService service = new AppNoticeService(noticeRepository, receiptRepository);
    private final UserEntity user = user(7L);

    @Test
    void returnsHighestPriorityEligibleNoticeForClientVersion() {
        AppNoticeEntity futureVersion = notice(1L, "10.0.0", null, false);
        AppNoticeEntity matching = notice(2L, "9.0.0", "9.9.9", false);
        matching.setTitle("Co je nového");
        matching.setActions(List.of(action(matching)));
        when(noticeRepository.findAllByActiveTrueOrderByPriorityDescIdDesc())
                .thenReturn(List.of(futureVersion, matching));
        when(receiptRepository.existsByNoticeIdAndUserId(2L, 7L)).thenReturn(false);

        CurrentAppNoticeDTO result = service.getCurrent(user, "9.0.1");

        assertNotNull(result.notice());
        assertEquals(2L, result.notice().id());
        assertEquals("Co je nového", result.notice().title());
        assertEquals(AppNoticeActionType.OPEN_URL, result.notice().actions().get(0).type());
    }

    @Test
    void doesNotReturnOneTimeNoticeThatUserAlreadySaw() {
        AppNoticeEntity notice = notice(3L, null, null, false);
        when(noticeRepository.findAllByActiveTrueOrderByPriorityDescIdDesc())
                .thenReturn(List.of(notice));
        when(receiptRepository.existsByNoticeIdAndUserId(3L, 7L)).thenReturn(true);

        assertNull(service.getCurrent(user, "9.0.1").notice());
    }

    @Test
    void returnsRepeatableNoticeEvenAfterPreviousDisplay() {
        AppNoticeEntity notice = notice(4L, null, "9.0.1", true);
        when(noticeRepository.findAllByActiveTrueOrderByPriorityDescIdDesc())
                .thenReturn(List.of(notice));

        assertEquals(4L, service.getCurrent(user, "8.5.0").notice().id());
        verify(receiptRepository, never()).existsByNoticeIdAndUserId(any(), any());
    }

    @Test
    void ignoresNoticeOutsideItsValidityWindow() {
        AppNoticeEntity notice = notice(5L, null, null, false);
        notice.setValidFrom(Instant.now().plusSeconds(60));
        when(noticeRepository.findAllByActiveTrueOrderByPriorityDescIdDesc())
                .thenReturn(List.of(notice));

        assertNull(service.getCurrent(user, "9.0.1").notice());
    }

    @Test
    void createsDisplayReceipt() {
        AppNoticeEntity notice = notice(6L, null, null, false);
        when(noticeRepository.findById(6L)).thenReturn(Optional.of(notice));
        when(receiptRepository.findByNoticeIdAndUserId(6L, 7L))
                .thenReturn(Optional.empty());

        service.markShown(6L, user);

        ArgumentCaptor<AppNoticeReceiptEntity> captor =
                ArgumentCaptor.forClass(AppNoticeReceiptEntity.class);
        verify(receiptRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getDisplayCount());
        assertEquals(notice, captor.getValue().getNotice());
        assertEquals(user, captor.getValue().getUser());
        assertNotNull(captor.getValue().getLastDisplayedAt());
    }

    private static AppNoticeEntity notice(
            Long id,
            String minVersion,
            String maxVersion,
            boolean repeatable
    ) {
        AppNoticeEntity notice = new AppNoticeEntity();
        notice.setId(id);
        notice.setTitle("Test");
        notice.setMessage("Text");
        notice.setMinAppVersion(minVersion);
        notice.setMaxAppVersion(maxVersion);
        notice.setRepeatable(repeatable);
        return notice;
    }

    private static AppNoticeActionEntity action(AppNoticeEntity notice) {
        AppNoticeActionEntity action = new AppNoticeActionEntity();
        action.setId(11L);
        action.setNotice(notice);
        action.setLabel("Aktualizovat");
        action.setType(AppNoticeActionType.OPEN_URL);
        action.setStyle(AppNoticeActionStyle.PRIMARY);
        action.setValue("https://example.com/app");
        return action;
    }

    private static UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        return user;
    }
}
