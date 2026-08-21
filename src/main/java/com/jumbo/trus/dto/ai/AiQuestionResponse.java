package com.jumbo.trus.dto.ai;

import com.jumbo.trus.entity.ai.AiQuestionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AiQuestionResponse {
    private Long id;
    private String question;
    private String answer;
    private AiQuestionStatus status;
    private Instant createdAt;
    private Instant completedAt;
    private AiUsageDTO usage;
}
