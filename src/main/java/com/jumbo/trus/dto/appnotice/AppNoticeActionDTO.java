package com.jumbo.trus.dto.appnotice;

import com.jumbo.trus.entity.appnotice.AppNoticeActionStyle;
import com.jumbo.trus.entity.appnotice.AppNoticeActionType;

public record AppNoticeActionDTO(
        Long id,
        String label,
        AppNoticeActionType type,
        AppNoticeActionStyle style,
        String value
) {
}
