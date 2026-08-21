package com.jumbo.trus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.openai")
public class AiOpenAiProperties {
    private boolean enabled = false;
    private String apiKey;
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-5.6-luna";
    private String reasoningEffort = "low";
    private int maxOutputTokens = 1200;
    private int timeoutSeconds = 90;
    private int maxToolRounds = 6;
}
