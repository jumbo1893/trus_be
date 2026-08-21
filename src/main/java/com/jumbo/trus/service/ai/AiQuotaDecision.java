package com.jumbo.trus.service.ai;

import com.jumbo.trus.dto.ai.AiUsageDTO;
import com.jumbo.trus.entity.ai.AiQuestionEntity;
import com.jumbo.trus.entity.ai.AiQuestionStatus;

public record AiQuotaDecision(
        boolean allowed,
        AiQuestionEntity question,
        AiUsageDTO usage,
        AiQuestionStatus deniedStatus,
        String deniedMessage
) {
    public static AiQuotaDecision allowed(AiQuestionEntity question, AiUsageDTO usage) {
        return new AiQuotaDecision(true, question, usage, null, null);
    }

    public static AiQuotaDecision denied(
            AiUsageDTO usage,
            AiQuestionStatus status,
            String message
    ) {
        return new AiQuotaDecision(false, null, usage, status, message);
    }
}
