package com.jumbo.trus.service.ai;

public record OpenAiAnswer(
        String text,
        String model,
        int inputTokens,
        int outputTokens
) {
}
