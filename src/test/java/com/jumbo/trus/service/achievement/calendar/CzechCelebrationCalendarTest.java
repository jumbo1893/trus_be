package com.jumbo.trus.service.achievement.calendar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CzechCelebrationCalendarTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CzechCelebrationCalendar calendar = new CzechCelebrationCalendar(mapper);
    CzechCelebrationCalendarTest() throws Exception {}

    @Test
    void givenNameIsSecondWordAndMatchingIgnoresCaseAndAccents() {
        assertThat(calendar.reasons(LocalDate.of(2026, 6, 24), "  Novák   JAN ")).containsExactly("Jmeniny: Jan");
        assertThat(calendar.reasons(LocalDate.of(2026, 4, 24), "Novák jiri")).containsExactly("Jmeniny: Jiří");
        assertThat(calendar.reasons(LocalDate.of(2026, 6, 24), "Jan Novák")).isEmpty();
        assertThat(calendar.reasons(LocalDate.of(2026, 6, 24), "Jan")).isEmpty();
        assertThat(calendar.reasons(LocalDate.of(2026, 6, 24), null)).isEmpty();
        assertThat(calendar.reasons(LocalDate.of(2026, 6, 24), "Novák Jan-Pavel")).isEmpty();
    }

    @Test
    void handlesMultipleNamesLeapDayAndOverlappingCelebrations() {
        assertThat(calendar.reasons(LocalDate.of(2026, 6, 29), "Novák Pavel")).containsExactly("Jmeniny: Pavel");
        assertThat(calendar.reasons(LocalDate.of(2026, 6, 29), "Novák Petr")).containsExactly("Jmeniny: Petr");
        assertThat(calendar.reasons(LocalDate.of(2024, 2, 29), "Novák Horymír")).containsExactly("Jmeniny: Horymír");
        assertThat(calendar.reasons(LocalDate.of(2025, 2, 28), "Novák Horymír")).isEmpty();
        assertThat(calendar.reasons(LocalDate.of(2026, 9, 28), "Novák Václav"))
                .containsExactly("Státní svátek: Den české státnosti", "Jmeniny: Václav");
        assertThat(calendar.reasons(LocalDate.of(2026, 2, 2), "Novák Hromnice")).isEmpty();
    }

    @Test
    void allYearsContainExactlyTheRequestedFixedAndMovableHolidays() throws Exception {
        Map<String, String> dates;
        try (var stream = new ClassPathResource("calendar/czech-public-holidays-2020-2040.json").getInputStream()) {
            dates = mapper.readValue(stream, new TypeReference<>() {
            });
        }
        assertThat(dates).hasSize(273);
        for (int year = 2020; year <= 2040; year++) {
            Set<String> expected = new HashSet<>();
            for (String day : List.of("01-01", "05-01", "05-08", "07-05", "07-06", "09-28", "10-28", "11-17", "12-24", "12-25", "12-26"))
                expected.add(year + "-" + day);
            LocalDate easter = easterSunday(year);
            expected.add(easter.minusDays(2).toString()); expected.add(easter.plusDays(1).toString());
            String prefix = year + "-";
            assertThat(dates.keySet().stream().filter(d -> d.startsWith(prefix)).toList())
                    .containsExactlyInAnyOrderElementsOf(expected);
            assertThat(dates.get(easter.minusDays(2).toString())).isEqualTo("Velký pátek");
            assertThat(dates.get(easter.plusDays(1).toString())).isEqualTo("Velikonoční pondělí");
        }
        // Independently published dates (CNB/MZV) anchor the computus calculation.
        assertThat(dates.get("2020-04-10")).isEqualTo("Velký pátek");
        assertThat(dates.get("2025-04-21")).isEqualTo("Velikonoční pondělí");
        assertThat(dates.get("2026-04-03")).isEqualTo("Velký pátek");
        assertThat(dates.get("2026-04-06")).isEqualTo("Velikonoční pondělí");
        assertThat(calendar.reasons(LocalDate.of(2041, 5, 1), null)).isEmpty();
    }

    // Gregorian Meeus/Jones/Butcher computus; also documents reproduction of the resource.
    private static LocalDate easterSunday(int y) {
        int a=y%19, b=y/100, c=y%100, d=b/4, e=b%4, f=(b+8)/25, g=(b-f+1)/3;
        int h=(19*a+b-d-g+15)%30, i=c/4, k=c%4, l=(32+2*e+2*i-h-k)%7;
        int m=(a+11*h+22*l)/451, v=h+l-7*m+114;
        return LocalDate.of(y, v/31, v%31+1);
    }
}
