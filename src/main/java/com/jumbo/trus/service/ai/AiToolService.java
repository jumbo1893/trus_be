package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jumbo.trus.service.achievement.TrusBotAchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiToolService {

    static final String AWARD_AI_EXPERT = "award_ai_expert";

    private final ObjectMapper objectMapper;
    private final AiReadOnlyToolService readOnlyToolService;
    private final TrusBotAchievementService achievementService;

    public ArrayNode toolDefinitions() {
        ArrayNode definitions = readOnlyToolService.toolDefinitions().deepCopy();
        try {
            definitions.add(objectMapper.readTree(AI_EXPERT_TOOL_DEFINITION));
            return definitions;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nelze načíst definici AI expert nástroje", exception);
        }
    }

    public String execute(
            String toolName,
            JsonNode arguments,
            AiToolContext context,
            String originalQuestion
    ) {
        if (!AWARD_AI_EXPERT.equals(toolName)) {
            return readOnlyToolService.execute(toolName, arguments, context);
        }

        try {
            TrusBotAchievementService.AiExpertAwardResult result =
                    achievementService.requestAiExpertAchievement(
                            originalQuestion,
                            context.currentPlayerId(),
                            context.appTeam()
                    );
            return objectMapper.writeValueAsString(result);
        } catch (RuntimeException | JsonProcessingException exception) {
            log.error(
                    "AI expert tool execution failed. playerId={}, appTeamId={}",
                    context.currentPlayerId(),
                    context.appTeam() == null ? null : context.appTeam().getId(),
                    exception
            );
            return serializeToolError();
        }
    }

    private String serializeToolError() {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "awarded", false,
                    "status", "ERROR",
                    "message", "Achievement AI expert se nepodařilo bezpečně udělit."
            ));
        } catch (JsonProcessingException serializationException) {
            return "{\"awarded\":false,\"status\":\"ERROR\"}";
        }
    }

    private static final String AI_EXPERT_TOOL_DEFINITION = """
            {
              "type": "function",
              "name": "award_ai_expert",
              "description": "Jediný zapisovací nástroj. Udělí aktuálnímu uživateli achievement AI expert. Použij výhradně tehdy, když uživatel výslovně žádá o udělení achievementu AI expert a ve svém původním dotazu použil samostatné slovo prosím. Nepoužívej ho, když se pouze ptá, jak nebo za jakých podmínek achievement získat. Backend původní dotaz znovu ověří.",
              "strict": true,
              "parameters": {
                "type": "object",
                "properties": {},
                "required": [],
                "additionalProperties": false
              }
            }
            """;
}
