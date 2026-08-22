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
    void punctuationAloneNoLongerTriggersQuote() {
        TrusBotQuoteService service = serviceChoosing(true);

        for (String question : List.of("Dotaz.", "Dotaz!", "Dotaz?", "Kolik se vypilo piv!")) {
            assertTrue(service.selectFor(question, List.of()).isEmpty(), question);
        }
    }

    @Test
    void inflectedHospodaWordsSelectOnlyHospodaQuotes() {
        TrusBotQuoteService service = serviceChoosing(false);

        for (String question : List.of("hospoda", "hospody", "hospodě", "hospodu", "hospodou")) {
            TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(question, List.of())
                    .orElseThrow();
            assertEquals("HOSPODA", candidate.source(), question);
        }
    }

    @Test
    void inflectedPreborWordsSelectOnlyOkresniPreborQuotes() {
        TrusBotQuoteService service = serviceChoosing(false);

        for (String question : List.of("přebor", "přeboru", "přeborem", "přebory")) {
            TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(question, List.of())
                    .orElseThrow();
            assertEquals("OKRESNI_PREBOR", candidate.source(), question);
        }
    }

    @Test
    void inflectedOkresniWordsSelectOnlyOkresniPreborQuotes() {
        TrusBotQuoteService service = serviceChoosing(false);

        for (String question : List.of("okresní", "okresního", "okresním", "okresními")) {
            TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(question, List.of())
                    .orElseThrow();
            assertEquals("OKRESNI_PREBOR", candidate.source(), question);
        }
    }

    @Test
    void sourceTriggerMustBePresentInQuestionNotOnlyInToolOutput() {
        TrusBotQuoteService service = serviceChoosing(true);

        assertTrue(service.selectFor(
                "Jak to vypadá?",
                List.of("find_matches {opponent: Hospoda FC}")
        ).isEmpty());
    }

    @Test
    void thematicHalfCanSelectBeerQuoteFromRequestedSeries() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Kdo vypil nejvíc piv v hospodě?",
                List.of()
        ).orElseThrow();

        assertEquals("HOSPODA", candidate.source());
        assertTrue(candidate.categories().contains("BEER"));
    }

    @Test
    void generalHalfStaysWithinRequestedSeries() {
        TrusBotQuoteService service = serviceChoosing(false);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Kdo letos vypil nejvíc piv v hospodě?",
                List.of()
        ).orElseThrow();

        assertEquals("HOSPODA", candidate.source());
        assertTrue(candidate.categories().contains("GENERAL"));
    }

    @Test
    void toolUsageCanProvideFootballContextWithinOkresniPrebor() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Jak to tedy vypadá v okresním?",
                List.of("find_matches {}")
        ).orElseThrow();

        assertEquals("OKRESNI_PREBOR", candidate.source());
        assertTrue(candidate.categories().contains("MATCH"));
    }

    @Test
    void alcoholWithoutBeerOrShotKeywordUsesGeneral() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Kdo vypil nejvíc alkoholu v hospodě?",
                List.of()
        ).orElseThrow();

        assertEquals("HOSPODA", candidate.source());
        assertTrue(candidate.categories().contains("GENERAL"));
    }

    @Test
    void moneyQuestionCanSelectFineQuote() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Kolik peněz jsme zaplatili v hospodě?",
                List.of()
        ).orElseThrow();

        assertEquals("HOSPODA", candidate.source());
        assertTrue(candidate.categories().contains("FINE"));
    }

    @Test
    void resultQuestionCanSelectMatchQuote() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Jaký byl výsledek okresního přeboru?",
                List.of()
        ).orElseThrow();

        assertEquals("OKRESNI_PREBOR", candidate.source());
        assertTrue(candidate.categories().contains("MATCH"));
    }

    @Test
    void questionWithoutKnownCategoryAlwaysSelectsGeneral() {
        TrusBotQuoteService service = serviceChoosing(true);

        TrusBotQuoteService.QuoteCandidate candidate = service.selectFor(
                "Jaké mám achievementy z hospody?",
                List.of("read_achievements {}")
        ).orElseThrow();

        assertEquals("HOSPODA", candidate.source());
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
