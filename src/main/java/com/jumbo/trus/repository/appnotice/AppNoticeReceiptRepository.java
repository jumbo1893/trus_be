package com.jumbo.trus.repository.appnotice;

import com.jumbo.trus.entity.appnotice.AppNoticeReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppNoticeReceiptRepository extends JpaRepository<AppNoticeReceiptEntity, Long> {

    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    Optional<AppNoticeReceiptEntity> findByNoticeIdAndUserId(Long noticeId, Long userId);
}
