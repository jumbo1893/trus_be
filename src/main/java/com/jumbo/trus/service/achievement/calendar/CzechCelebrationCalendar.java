package com.jumbo.trus.service.achievement.calendar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Versioned offline dictionaries; no external API is called when awarding achievements. */
@Component
public class CzechCelebrationCalendar {
    private final Map<String, List<String>> namedays;
    private final Map<String, String> holidays;

    public CzechCelebrationCalendar(ObjectMapper mapper) throws IOException {
        try (var names = new ClassPathResource("calendar/czech-namedays.json").getInputStream();
             var dates = new ClassPathResource("calendar/czech-public-holidays-2020-2040.json").getInputStream()) {
            namedays = mapper.readValue(names, new TypeReference<>() {
            });
            holidays = mapper.readValue(dates, new TypeReference<>() {
            });
        }
        // Invalid bundled data must fail startup rather than silently miss awards.
        namedays.keySet().forEach(key -> MonthDay.parse("--" + key));
        holidays.keySet().forEach(LocalDate::parse);
        if (namedays.isEmpty() || holidays.size() != 273) throw new IllegalStateException("Invalid Czech calendar dictionaries");
    }

    public List<String> reasons(LocalDate date, String footballPlayerName) {
        List<String> result = new ArrayList<>();
        String holiday = holidays.get(date.toString());
        if (holiday != null) result.add("Státní svátek: " + holiday);
        nameDay(date, footballPlayerName).ifPresent(name -> result.add("Jmeniny: " + name));
        return List.copyOf(result);
    }

    /** Personal name day only: a public holiday is not a reason to congratulate everyone. */
    public java.util.Optional<String> nameDay(LocalDate date, String footballPlayerName) {
        String firstName = firstName(footballPlayerName);
        if (firstName.isEmpty()) return java.util.Optional.empty();
        return namedays.getOrDefault(date.toString().substring(5), List.of()).stream()
                .filter(name -> normalize(name).equals(normalize(firstName))).findFirst();
    }

    /** Feed contract: surname first, given name is the second whitespace-separated word.
     * Never use the app nickname or guess a given name from the surname. */
    static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.strip().split("(?U)\\s+");
        return parts.length >= 2 ? parts[1] : "";
    }

    private static String normalize(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
