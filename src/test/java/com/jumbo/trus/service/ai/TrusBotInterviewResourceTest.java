package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrusBotInterviewResourceTest {

    private static final String RESOURCE = "/ai/trusbot-interviews.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void containsEveryQuestionAndConfirmedPlayerLink() throws Exception {
        JsonNode root;
        try (InputStream input = getClass().getResourceAsStream(RESOURCE)) {
            assertNotNull(input, "Interview resource must be available on the runtime classpath");
            root = objectMapper.readTree(input);
        }

        assertEquals(1, root.path("schemaVersion").asInt());
        assertEquals("Liščí Trus", root.path("appTeamName").asText());
        JsonNode interviews = root.path("interviews");
        assertEquals(14, interviews.size());

        Map<String, Integer> expectedPlayerIds = new HashMap<>();
        expectedPlayerIds.put("Martin Humpl", 27);
        expectedPlayerIds.put("Lukáš Hakl", 40);
        expectedPlayerIds.put("Václav Kadleček", 36);
        expectedPlayerIds.put("Marek Vávra", null);
        expectedPlayerIds.put("Michal Slavata", 32);
        expectedPlayerIds.put("Lukáš Novotný", 19);
        expectedPlayerIds.put("Jakub Novák", 12);
        expectedPlayerIds.put("Jan Flégl", 9);
        expectedPlayerIds.put("Matěj Jandák", 15);
        expectedPlayerIds.put("Jan Doležal", 7);
        expectedPlayerIds.put("Jan Malý", 24);
        expectedPlayerIds.put("Lukáš Mařan", null);
        expectedPlayerIds.put("Karel Dvořák", 16);
        expectedPlayerIds.put("Petr Beneš", 2);

        int questionCount = 0;
        Set<String> names = new HashSet<>();
        for (JsonNode interview : interviews) {
            JsonNode interviewee = interview.path("interviewee");
            String name = interviewee.path("fullName").asText();
            assertTrue(names.add(name), "Interviewee must occur only once: " + name);
            assertTrue(expectedPlayerIds.containsKey(name), "Unexpected interviewee: " + name);

            Integer expectedPlayerId = expectedPlayerIds.get(name);
            JsonNode playerId = interviewee.get("playerId");
            if (expectedPlayerId == null) {
                assertTrue(playerId.isNull(), name + " must remain unlinked");
            } else {
                assertEquals(expectedPlayerId.intValue(), playerId.asInt(), name);
            }

            JsonNode questions = interview.path("questions");
            int expectedInterviewSize = name.equals("Václav Kadleček") ? 40 : 41;
            assertEquals(expectedInterviewSize, questions.size(), name);
            for (int index = 0; index < questions.size(); index++) {
                JsonNode question = questions.get(index);
                assertEquals(index + 1, question.path("number").asInt(), name);
                assertFalse(question.path("question").asText().isBlank(), name);
                assertFalse(question.path("answer").asText().isBlank(), name);
            }
            questionCount += questions.size();
        }

        assertEquals(expectedPlayerIds.keySet(), names);
        assertEquals(573, questionCount);
    }
}
