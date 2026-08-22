package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class TrusbotQuoteService {

    private static final String QUOTES_RESOURCE = "/ai/trusbot-quotes.json";
    private static final int MAX_RELEVANT_CANDIDATES = 2;
    private static final int MAX_TOTAL_CANDIDATES = 3;

    private static final Map<String, List<String>> CATEGORY_SIGNALS = Map.of(
            "FOOTBALL", List.of(
                    "fotbal", "zapas", "utkani", "gol", "brank", "bod", "tabulk", "liga",
                    "souper", "hrac", "trener", "strel", "vyhr", "prohr", "remiz", "sezon",
                    "match", "matches", "fixture", "official football", "repeat opponents"
            ),
            "BEER", List.of(
                    "piv", "vypil", "vypito", "napoj", "pitny", "alkohol", "rum", "vodk",
                    "panak", "beer", "drink", "drinks"
            ),
            "FINE", List.of(
                    "pokut", "prekop", "trest", "sazebnik", "fine", "fines"
            ),
            "FOOD", List.of(
                    "jid", "kuchar", "gulas", "maso", "burt", "klobas", "obed", "vecer"
            ),
            "MONEY", List.of(
                    "peniz", "korun", "zaplat", "ucet", "dluh", "vyplat"
            ),
            "WORK", List.of(
                    "prac", "sef", "zamest", "smena", "vypoved"
            ),
            "RELATIONSHIP", List.of(
                    "vztah", "manzel", "rozvod", "svatb", "rande", "zensk", "chlap"
            )
    );

    private final List<QuoteDefinition> quotes;

    public TrusbotQuoteService(ObjectMapper objectMapper) {
        this.quotes = loadQuotes(objectMapper);
    }

    public List<QuoteCandidate> candidatesFor(String question, List<String> toolSignals) {
        String normalizedContext = normalize(String.join(
                " ",
                question == null ? "" : question,
                toolSignals == null ? "" : String.join(" ", toolSignals)
        ));
        Set<String> relevantCategories = detectCategories(normalizedContext);
        List<QuoteCandidate> result = new ArrayList<>();

        if (!relevantCategories.isEmpty()) {
            quotes.stream()
                    .filter(QuoteDefinition::enabled)
                    .filter(quote -> intersects(quote.categories(), relevantCategories))
                    .map(quote -> new RankedQuote(quote, score(quote, relevantCategories, normalizedContext)))
                    .sorted(Comparator.comparingInt(RankedQuote::score).reversed())
                    .limit(MAX_RELEVANT_CANDIDATES)
                    .map(RankedQuote::quote)
                    .map(this::toCandidate)
                    .forEach(result::add);
        }

        if (shouldOfferGeneral(normalizedContext)) {
            quotes.stream()
                    .filter(QuoteDefinition::enabled)
                    .filter(quote -> safeList(quote.categories()).contains("GENERAL"))
                    .map(quote -> new RankedQuote(quote, stableTieBreak(quote, normalizedContext)))
                    .sorted(Comparator.comparingInt(RankedQuote::score).reversed())
                    .map(RankedQuote::quote)
                    .map(this::toCandidate)
                    .filter(candidate -> result.stream().noneMatch(existing -> existing.id().equals(candidate.id())))
                    .findFirst()
                    .ifPresent(result::add);
        }

        return List.copyOf(result.subList(0, Math.min(result.size(), MAX_TOTAL_CANDIDATES)));
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
            return List.copyOf(document.quotes());
        } catch (IOException exception) {
            throw new IllegalStateException("Nelze načíst hlášky Trusbota.", exception);
        }
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

    private int score(
            QuoteDefinition quote,
            Set<String> relevantCategories,
            String normalizedContext
    ) {
        int categoryScore = (int) safeList(quote.categories()).stream()
                .filter(relevantCategories::contains)
                .count() * 1_000;
        int keywordScore = (int) safeList(quote.keywords()).stream()
                .map(TrusbotQuoteService::normalize)
                .filter(keyword -> !keyword.isBlank() && containsSignal(normalizedContext, keyword))
                .count() * 120;
        int sourceScore = sourceMatches(quote.source(), relevantCategories) ? 80 : 0;
        int lengthScore = Math.max(0, 100 - quote.text().length() / 4);
        int weightScore = Math.max(1, quote.weight()) * 5;
        return categoryScore
                + keywordScore
                + sourceScore
                + lengthScore
                + weightScore
                + stableTieBreak(quote, normalizedContext);
    }

    private boolean sourceMatches(String source, Set<String> categories) {
        return "OKRESNI_PREBOR".equals(source) && categories.contains("FOOTBALL")
                || "HOSPODA".equals(source) && categories.stream()
                .anyMatch(category -> Set.of("BEER", "FINE", "FOOD", "MONEY", "WORK", "RELATIONSHIP")
                        .contains(category));
    }

    private boolean shouldOfferGeneral(String normalizedContext) {
        return !normalizedContext.isBlank() && Math.floorMod(normalizedContext.hashCode(), 4) == 0;
    }

    private int stableTieBreak(QuoteDefinition quote, String normalizedContext) {
        return Math.floorMod(Objects.hash(quote.id(), normalizedContext), 100);
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

    private record RankedQuote(QuoteDefinition quote, int score) {
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
