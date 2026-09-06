package com.jumbo.trus.dto.ai;

public record MatchReportStateDTO(MatchReportDTO report, boolean canGenerate, boolean generating) {}
