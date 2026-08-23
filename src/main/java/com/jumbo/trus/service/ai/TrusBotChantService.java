package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

@Component
public class TrusBotChantService {

    private static final String CHANTS_RESOURCE = "/ai/trusbot-chants.json";
    private static final List<String> CHANT_SIGNALS = List.of(
            "pokrik",
            "popev",
            "choral",
            "pisn",
            "pisen",
            "zazpiv"
    );

    private final List<ChantDefinition> chants;
    private final Random random;

    @Autowired
    public TrusBotChantService(ObjectMapper objectMapper) {
        this(objectMapper, new Random());
    }

    TrusBotChantService(ObjectMapper objectMapper, Random random) {
        this.chants = loadChants(objectMapper);
        this.random = random;
    }

    public Optional<ChantCandidate> selectFor(String question) {
        String normalizedQuestion = normalize(question);
        if (CHANT_SIGNALS.stream().noneMatch(signal -> containsSignal(normalizedQuestion, signal))) {
            return Optional.empty();
        }

        List<ChantDefinition> enabledChants = chants.stream()
                .filter(ChantDefinition::enabled)
                .toList();
        if (enabledChants.isEmpty()) {
            return Optional.empty();
        }

        ChantDefinition selected = enabledChants.get(random.nextInt(enabledChants.size()));
        return Optional.of(new ChantCandidate(selected.id(), selected.text()));
    }

    int chantCount() {
        return chants.size();
    }

    int enabledChantCount() {
        return (int) chants.stream().filter(ChantDefinition::enabled).count();
    }

    private List<ChantDefinition> loadChants(ObjectMapper objectMapper) {
        try (InputStream inputStream = TrusBotChantService.class.getResourceAsStream(CHANTS_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Chybí soubor s pokřiky TrusBota: " + CHANTS_RESOURCE);
            }
            ChantDocument document = objectMapper.readValue(inputStream, ChantDocument.class);
            if (document.chants() == null || document.chants().isEmpty()) {
                throw new IllegalStateException("Soubor s pokřiky TrusBota je prázdný.");
            }

            List<ChantDefinition> loadedChants = List.copyOf(document.chants());
            boolean hasInvalidChant = loadedChants.stream()
                    .anyMatch(chant -> chant.id() == null
                            || chant.id().isBlank()
                            || chant.text() == null
                            || chant.text().isBlank());
            if (hasInvalidChant) {
                throw new IllegalStateException("Soubor s pokřiky TrusBota obsahuje neplatnou položku.");
            }
            if (loadedChants.stream().noneMatch(ChantDefinition::enabled)) {
                throw new IllegalStateException("TrusBot potřebuje alespoň jeden aktivní pokřik.");
            }
            return loadedChants;
        } catch (IOException exception) {
            throw new IllegalStateException("Nelze načíst pokřiky TrusBota.", exception);
        }
    }

    private static boolean containsSignal(String normalizedQuestion, String signal) {
        return (" " + normalizedQuestion).contains(" " + signal);
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

    public record ChantCandidate(String id, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChantDocument(List<ChantDefinition> chants) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChantDefinition(String id, String text, boolean enabled) {
    }
}
