package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Component
public class TrusbotQuoteService {

    private static final String QUOTES_RESOURCE = "/ai/trusbot-quotes.json";

    private static final Map<String, List<String>> CATEGORY_SIGNALS = Map.of(
            "BEER", List.of(
                    "piv", "panak"
            ),
            "FINE", List.of(
                    "pokut", "penez", "peniz", "penezi", "korun", "kc", "zaplat", "platim",
                    "platil", "platit", "ucet", "dluh", "vyplat", "pujc", "utrat", "cena",
                    "stoji", "stalo", "hotovost", "castk", "fine", "fines"
            ),
            "MATCH", List.of(
                    "fotbal", "zapas", "utkani", "vysled", "gol", "goal", "brank", "skore",
                    "vyhr", "prohr", "remiz", "bod", "tabulk", "souper", "strel", "liga",
                    "rozhodc", "trener", "hrist", "dres", "penalt", "ofs", "match", "matches",
                    "fixture", "official football", "repeat opponents"
            )
    );

    private final List<QuoteDefinition> quotes;
    private final Random random;

    @Autowired
    public TrusbotQuoteService(ObjectMapper objectMapper) {
        this(objectMapper, new Random());
    }

    TrusbotQuoteService(ObjectMapper objectMapper, Random random) {
        this.quotes = loadQuotes(objectMapper);
        this.random = random;
    }

    public Optional<QuoteCandidate> selectFor(String question, List<String> toolSignals) {
        if (!hasQuoteTrigger(question)) {
            return Optional.empty();
        }

        String normalizedContext = normalize(String.join(
                " ",
                question == null ? "" : question,
                toolSignals == null ? "" : String.join(" ", toolSignals)
        ));
        Set<String> relevantCategories = detectCategories(normalizedContext);
        List<QuoteDefinition> generalQuotes = enabledQuotesWithCategory("GENERAL");

        List<QuoteDefinition> selectionPool;
        if (relevantCategories.isEmpty()) {
            selectionPool = generalQuotes;
        } else if (random.nextBoolean()) {
            selectionPool = quotes.stream()
                    .filter(QuoteDefinition::enabled)
                    .filter(quote -> intersects(quote.categories(), relevantCategories))
                    .toList();
            if (selectionPool.isEmpty()) {
                selectionPool = generalQuotes;
            }
        } else {
            selectionPool = generalQuotes;
        }

        return selectWeighted(selectionPool).map(this::toCandidate);
    }

    int quoteCount() {
        return quotes.size();
    }

    int enabledQuoteCount() {
        return (int) quotes.stream().filter(QuoteDefinition::enabled).count();
    }

    private List<QuoteDefinition> loadQuotes(ObjectMapper objectMapper) {
        try (InputStream inputStream = TrusbotQuoteService.class.getResourceAsStream(QUOTES_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Chybí soubor s hláškami Trusbota: " + QUOTES_RESOURCE);
            }
            QuoteDocument document = objectMapper.readValue(inputStream, QuoteDocument.class);
            if (document.quotes() == null || document.quotes().isEmpty()) {
                throw new IllegalStateException("Soubor s hláškami Trusbota je prázdný.");
            }
            List<QuoteDefinition> loadedQuotes = List.copyOf(document.quotes());
            boolean hasEnabledGeneral = loadedQuotes.stream()
                    .filter(QuoteDefinition::enabled)
                    .anyMatch(quote -> safeList(quote.categories()).contains("GENERAL"));
            if (!hasEnabledGeneral) {
                throw new IllegalStateException("Trusbot potřebuje alespoň jednu aktivní hlášku GENERAL.");
            }
            return loadedQuotes;
        } catch (IOException exception) {
            throw new IllegalStateException("Nelze načíst hlášky Trusbota.", exception);
        }
    }

    private boolean hasQuoteTrigger(String question) {
        if (question == null) {
            return false;
        }
        String trimmedQuestion = question.stripTrailing();
        if (trimmedQuestion.isEmpty()) {
            return false;
        }
        char lastCharacter = trimmedQuestion.charAt(trimmedQuestion.length() - 1);
        return lastCharacter == '.' || lastCharacter == '?' || lastCharacter == '!';
    }

    private Set<String> detectCategories(String normalizedContext) {
        Set<String> result = new LinkedHashSet<>();
        CATEGORY_SIGNALS.forEach((category, signals) -> {
            if (signals.stream().anyMatch(signal -> containsSignal(normalizedContext, signal))) {
                result.add(category);
            }
        });
        return result;
    }

    private List<QuoteDefinition> enabledQuotesWithCategory(String category) {
        return quotes.stream()
                .filter(QuoteDefinition::enabled)
                .filter(quote -> safeList(quote.categories()).contains(category))
                .toList();
    }

    private Optional<QuoteDefinition> selectWeighted(List<QuoteDefinition> selectionPool) {
        if (selectionPool.isEmpty()) {
            return Optional.empty();
        }
        int totalWeight = selectionPool.stream()
                .mapToInt(quote -> Math.max(1, quote.weight()))
                .sum();
        int selectedWeight = random.nextInt(totalWeight);
        for (QuoteDefinition quote : selectionPool) {
            selectedWeight -= Math.max(1, quote.weight());
            if (selectedWeight < 0) {
                return Optional.of(quote);
            }
        }
        return Optional.of(selectionPool.get(selectionPool.size() - 1));
    }

    private QuoteCandidate toCandidate(QuoteDefinition quote) {
        return new QuoteCandidate(
                quote.id(),
                quote.text(),
                quote.source(),
                List.copyOf(safeList(quote.categories()))
        );
    }

    private static boolean intersects(List<String> quoteCategories, Set<String> relevantCategories) {
        return safeList(quoteCategories).stream().anyMatch(relevantCategories::contains);
    }

    private static boolean containsSignal(String normalizedContext, String signal) {
        String normalizedSignal = normalize(signal);
        return !normalizedSignal.isBlank() && (" " + normalizedContext).contains(" " + normalizedSignal);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    public record QuoteCandidate(
            String id,
            String text,
            String source,
            List<String> categories
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuoteDocument(List<QuoteDefinition> quotes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuoteDefinition(
            String id,
            String text,
            String source,
            List<String> categories,
            List<String> keywords,
            int weight,
            boolean enabled
    ) {
    }
}
