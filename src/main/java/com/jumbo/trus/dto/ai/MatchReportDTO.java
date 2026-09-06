package com.jumbo.trus.dto.ai;

import com.jumbo.trus.entity.ai.MatchReportEntity;
import com.jumbo.trus.entity.ai.MatchReportStyle;
import java.time.Instant;

public record MatchReportDTO(Long id, Long footballMatchId, MatchReportStyle style,
                             String text, Instant generatedAt) {
    public static MatchReportDTO from(MatchReportEntity report) {
        String text = report.getText();
        String legacyPrefix = "S nadsázkou – fiktivní report inspirovaný statistikami.\n\n";
        if (text != null && text.startsWith(legacyPrefix)) {
            text = text.substring(legacyPrefix.length());
        }
        return new MatchReportDTO(report.getId(), report.getFootballMatchId(), report.getStyle(),
                text, report.getGeneratedAt());
    }
}
