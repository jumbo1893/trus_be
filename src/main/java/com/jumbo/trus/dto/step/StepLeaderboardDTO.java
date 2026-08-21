package com.jumbo.trus.dto.step;

public record StepLeaderboardDTO(
        Long userId,
        String userName,
        long stepCount,
        long dayCount,
        double averageStepsPerDay) {

    public StepLeaderboardDTO(
            Long userId,
            String userName,
            Long stepCount,
            Long dayCount,
            Double averageStepsPerDay) {
        this(
                userId,
                userName,
                stepCount == null ? 0 : stepCount,
                dayCount == null ? 0 : dayCount,
                averageStepsPerDay == null ? 0 : averageStepsPerDay);
    }
}
