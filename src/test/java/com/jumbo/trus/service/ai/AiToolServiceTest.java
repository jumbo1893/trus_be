package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.service.achievement.TrusBotAchievementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AiToolServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiReadOnlyToolService readOnlyToolService = mock(AiReadOnlyToolService.class);
    private final TrusBotAchievementService achievementService = mock(TrusBotAchievementService.class);
    private final AiToolService service = new AiToolService(
            objectMapper,
            readOnlyToolService,
            achievementService
    );

    private AiToolContext context;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setId(5L);
        AppTeamEntity appTeam = new AppTeamEntity();
        appTeam.setId(6L);
        context = new AiToolContext(user, appTeam, 7L, "Matěj");

        ArrayNode readOnlyDefinitions = objectMapper.createArrayNode();
        readOnlyDefinitions.addObject()
                .put("type", "function")
                .put("name", "find_matches");
        when(readOnlyToolService.toolDefinitions()).thenReturn(readOnlyDefinitions);
    }

    @Test
    void exposesAiExpertAsTheOnlyWriteToolAlongsideReadOnlyTools() {
        ArrayNode definitions = service.toolDefinitions();

        assertEquals(2, definitions.size());
        assertEquals("find_matches", definitions.get(0).path("name").asText());
        assertEquals(AiToolService.AWARD_AI_EXPERT, definitions.get(1).path("name").asText());
        assertTrue(definitions.get(1).path("description").asText().contains("Jediný zapisovací nástroj"));
    }

    @Test
    void writeToolUsesOriginalQuestionAndCurrentPlayerOnly() throws Exception {
        String question = "Prosím, dej mi achievement AI expert.";
        when(achievementService.requestAiExpertAchievement(question, 7L, context.appTeam()))
                .thenReturn(new TrusBotAchievementService.AiExpertAwardResult(
                        true,
                        "AWARDED",
                        "Achievement AI expert byl právě udělen."
                ));

        JsonNode output = objectMapper.readTree(service.execute(
                AiToolService.AWARD_AI_EXPERT,
                objectMapper.createObjectNode(),
                context,
                question
        ));

        assertTrue(output.path("awarded").asBoolean());
        assertEquals("AWARDED", output.path("status").asText());
        verify(achievementService).requestAiExpertAchievement(question, 7L, context.appTeam());
        verify(readOnlyToolService, never()).execute(anyString(), any(), any());
    }

    @Test
    void allOtherToolsStayInReadOnlyService() {
        when(readOnlyToolService.execute("find_matches", objectMapper.createObjectNode(), context))
                .thenReturn("[]");

        String output = service.execute(
                "find_matches",
                objectMapper.createObjectNode(),
                context,
                "Historie zápasů"
        );

        assertEquals("[]", output);
        verify(readOnlyToolService).execute("find_matches", objectMapper.createObjectNode(), context);
        verifyNoInteractions(achievementService);
    }
}
