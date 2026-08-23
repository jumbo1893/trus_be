package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrusBotChantServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loadsAndEnablesAllProvidedChants() {
        TrusBotChantService service = serviceChoosingFirstChant();

        assertEquals(17, service.chantCount());
        assertEquals(17, service.enabledChantCount());
    }

    @Test
    void inflectedChantWordsSelectAChant() {
        TrusBotChantService service = serviceChoosingFirstChant();

        for (String question : List.of(
                "Dej mi pokřik Liščího Trusu.",
                "Znáš nějaké pokřiky?",
                "Chci týmovou popěvku.",
                "Jaké máme chorály?",
                "Zazpívej písničku Liščího Trusu.",
                "Máš nějakou píseň pro fanoušky?"
        )) {
            TrusBotChantService.ChantCandidate selected = service.selectFor(question).orElseThrow();
            assertEquals("trus-chant-001", selected.id(), question);
            assertEquals("Zrzavá sílá a vzadu hnědá díra!", selected.text(), question);
        }
    }

    @Test
    void unrelatedQuestionDoesNotSelectAChant() {
        TrusBotChantService service = serviceChoosingFirstChant();

        assertTrue(service.selectFor("Kdo letos vstřelil nejvíc gólů?").isEmpty());
        assertTrue(service.selectFor("Jaké máme achievementy?").isEmpty());
    }

    private TrusBotChantService serviceChoosingFirstChant() {
        return new TrusBotChantService(objectMapper, new FixedRandom());
    }

    private static final class FixedRandom extends Random {

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
