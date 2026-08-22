package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrusbotQuoteServiceTest {

    private final TrusbotQuoteService service = new TrusbotQuoteService(new ObjectMapper());

    @Test
    void loadsAndEnablesAllApprovedQuotes() {
        assertEquals(160, service.quoteCount());
        assertEquals(160, service.enabledQuoteCount());
    }

    @Test
    void offersPubQuoteForBeerQuestion() {
        List<TrusbotQuoteService.QuoteCandidate> candidates = service.candidatesFor(
                "Kdo letos vypil nejvíc piv?",
                List.of()
        );

        assertFalse(candidates.isEmpty());
        assertTrue(candidates.size() <= 3);
        assertTrue(candidates.stream().anyMatch(candidate ->
                "HOSPODA".equals(candidate.source()) && candidate.categories().contains("BEER")
        ));
    }

    @Test
    void toolUsageCanAddFootballContext() {
        List<TrusbotQuoteService.QuoteCandidate> candidates = service.candidatesFor(
                "Jak to tedy vypadá?",
                List.of("find_matches {}")
        );

        assertTrue(candidates.stream().anyMatch(candidate ->
                "OKRESNI_PREBOR".equals(candidate.source())
                        && candidate.categories().contains("FOOTBALL")
        ));
    }

    @Test
    void occasionallyOffersGeneralQuoteWithoutSpecificCategory() {
        boolean generalWasOffered = IntStream.range(0, 100)
                .mapToObj(index -> service.candidatesFor("achievement " + index, List.of()))
                .flatMap(List::stream)
                .anyMatch(candidate -> candidate.categories().contains("GENERAL"));

        assertTrue(generalWasOffered);
    }
}
