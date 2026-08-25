package com.jumbo.trus.service.appnotice;

import com.jumbo.trus.dto.appnotice.AppNoticeActionDTO;
import com.jumbo.trus.dto.appnotice.AppNoticeDTO;
import com.jumbo.trus.dto.appnotice.CurrentAppNoticeDTO;
import com.jumbo.trus.entity.appnotice.AppNoticeEntity;
import com.jumbo.trus.entity.appnotice.AppNoticeReceiptEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.repository.appnotice.AppNoticeReceiptRepository;
import com.jumbo.trus.repository.appnotice.AppNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AppNoticeService {

    private final AppNoticeRepository noticeRepository;
    private final AppNoticeReceiptRepository receiptRepository;

    @Transactional(readOnly = true)
    public CurrentAppNoticeDTO getCurrent(UserEntity user, String appVersion) {
        Instant now = Instant.now();
        AppNoticeDTO notice = noticeRepository.findAllByActiveTrueOrderByPriorityDescIdDesc().stream()
                .filter(candidate -> isCurrentlyValid(candidate, now))
                .filter(candidate -> AppVersionComparator.isWithinRange(
                        appVersion,
                        candidate.getMinAppVersion(),
                        candidate.getMaxAppVersion()
                ))
                .filter(candidate -> candidate.isRepeatable()
                        || !receiptRepository.existsByNoticeIdAndUserId(candidate.getId(), user.getId()))
                .findFirst()
                .map(this::toDto)
                .orElse(null);
        return new CurrentAppNoticeDTO(notice);
    }

    @Transactional
    public void markShown(Long noticeId, UserEntity user) {
        AppNoticeEntity notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NotFoundException(
                        "App notice s id " + noticeId + " nenalezen v db"
                ));

        AppNoticeReceiptEntity receipt = receiptRepository
                .findByNoticeIdAndUserId(noticeId, user.getId())
                .orElseGet(AppNoticeReceiptEntity::new);
        receipt.setNotice(notice);
        receipt.setUser(user);
        receipt.setDisplayCount(receipt.getDisplayCount() + 1);
        receipt.setLastDisplayedAt(Instant.now());
        receiptRepository.save(receipt);
    }

    private boolean isCurrentlyValid(AppNoticeEntity notice, Instant now) {
        return (notice.getValidFrom() == null || !notice.getValidFrom().isAfter(now))
                && (notice.getValidUntil() == null || !notice.getValidUntil().isBefore(now));
    }

    private AppNoticeDTO toDto(AppNoticeEntity notice) {
        return new AppNoticeDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getMessage(),
                notice.isDismissible(),
                notice.getActions().stream()
                        .map(action -> new AppNoticeActionDTO(
                                action.getId(),
                                action.getLabel(),
                                action.getType(),
                                action.getStyle(),
                                action.getValue()
                        ))
                        .toList()
        );
    }
}
