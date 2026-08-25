package com.jumbo.trus.dto.appnotice;

import java.util.List;

public record AppNoticeDTO(
        Long id,
        String title,
        String message,
        boolean dismissible,
        List<AppNoticeActionDTO> actions
) {
}
