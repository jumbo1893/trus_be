package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.config.AiOpenAiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiClientQuoteTest {

    private final TrusbotQuoteService quoteService = mock(TrusbotQuoteService.class);
    private final OpenAiClient client = new OpenAiClient(
            new AiOpenAiProperties(),
            new ObjectMapper(),
            mock(AiReadOnlyToolService.class),
            quoteService
    );

    @Test
    void appendsSelectedQuoteAsSeparateParagraph() {
        when(quoteService.selectFor("Dotaz?", List.of("find_matches {}")))
                .thenReturn(Optional.of(new TrusbotQuoteService.QuoteCandidate(
                        "quote-1",
                        "První řádek\nDruhý řádek",
                        "OKRESNI_PREBOR",
                        List.of("MATCH")
                )));

        String result = client.appendSelectedQuote(
                "Věcná odpověď.",
                "Dotaz?",
                List.of("find_matches {}")
        );

        assertEquals("Věcná odpověď.\n\nPrvní řádek\nDruhý řádek", result);
    }

    @Test
    void leavesAnswerUntouchedWhenQuestionHasNoQuote() {
        when(quoteService.selectFor("Dotaz", List.of())).thenReturn(Optional.empty());

        String result = client.appendSelectedQuote("Věcná odpověď.", "Dotaz", List.of());

        assertEquals("Věcná odpověď.", result);
    }
}
