package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrusBotQuoteServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loadsAndEnablesAllApprovedQuotes() {
        TrusBotQuoteService service = serviceChoosing(true);

        assertEquals(160, service.quoteCount());
        assertEquals(160, service.enabledQuoteCount());
    }

    @Test
    void doesNotSelectQuoteWithoutTerminalPunctuation() {
        TrusBotQuoteService service = serviceChoosing(true);

        assertTrue(service.selectFor("Kdo letos vypil nejvíc piv", List.of()).isEmpty());
        assertTrue(service.selectFor("Kdo dal nejvíc gólů   ", List.of("find_matches {}"))
                .isEmpty());
    }

    @Test
    void allConfiguredTerminalCharactersTriggerQuote() {
        TrusBotQuoteService service = serviceChoosing(false);

        for (String question : List.of("Dotaz.", "Dotaz!", "Dotaz!   ")) {
            assertTrue(service.selectFor(question, List.of()).isPresent(), question);
        }
    }

    @Test
    void questionMarkDoesNotTriggerQuote() {
        TrusBotQuoteService service = serviceChoosing(true);

        assertTrue(service.selectFor("Dotaz?", List.of()).isEmpty());
    }

    @Test
    void thematicHalfCanSelectBeerQuote() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Kdo vypil nejvíc piv v sezoně.",
                List.of()
        ).orElseThrow();

        assertTrue(candidate.categories().contains("BEER"));
    }

    @Test
    void generalHalfCanSelectGeneralQuoteForRelevantQuestion() {
        TrusBotQuoteService service = serviceChoosing(false);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Kdo letos vypil nejvíc piv!",
                List.of()
        ).orElseThrow();

        assertTrue(candidate.categories().contains("GENERAL"));
    }

    @Test
    void toolUsageCanProvideFootballContext() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Jak to tedy vypadá.",
                List.of("find_matches {}")
        ).orElseThrow();

        assertTrue(candidate.categories().contains("MATCH"));
    }

    @Test
    void alcoholWithoutBeerOrShotKeywordUsesGeneral() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Kdo vypil nejvíc alkoholu.",
                List.of()
        ).orElseThrow();

        assertTrue(candidate.categories().contains("GENERAL"));
    }

    @Test
    void moneyQuestionCanSelectFineQuote() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Kolik peněz jsme zaplatili!",
                List.of()
        ).orElseThrow();

        assertTrue(candidate.categories().contains("FINE"));
    }

    @Test
    void resultQuestionCanSelectMatchQuote() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Jaký byl výsledek zápasu.",
                List.of()
        ).orElseThrow();

        assertTrue(candidate.categories().contains("MATCH"));
    }

    @Test
    void questionWithoutKnownCategoryAlwaysSelectsGeneral() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Jaké mám achievementy!",
                List.of("read_achievements {}")
        ).orElseThrow();

        assertTrue(candidate.categories().contains("GENERAL"));
    }

    private TrusBotQuoteService serviceChoosing(boolean thematicQuote) {
        return new TrusBotQuoteService(objectMapper, new FixedRandom(thematicQuote));
    }

    private static final class FixedRandom extends Random {

        private final boolean thematicQuote;

        private FixedRandom(boolean thematicQuote) {
            this.thematicQuote = thematicQuote;
        }

        @Override
        public boolean nextBoolean() {
            return thematicQuote;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
