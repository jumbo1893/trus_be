package com.jumbo.trus.service.appnotice;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppVersionComparator {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^[vV]?(\\d+(?:\\.\\d+)*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$"
    );

    private AppVersionComparator() {
    }

    public static boolean isWithinRange(String version, String minInclusive, String maxInclusive) {
        Optional<ParsedVersion> parsedVersion = parse(version);
        if (parsedVersion.isEmpty()) {
            return false;
        }

        Optional<ParsedVersion> min = parseOptionalBoundary(minInclusive);
        Optional<ParsedVersion> max = parseOptionalBoundary(maxInclusive);
        if (hasText(minInclusive) && min.isEmpty() || hasText(maxInclusive) && max.isEmpty()) {
            return false;
        }

        return min.map(value -> parsedVersion.get().compareTo(value) >= 0).orElse(true)
                && max.map(value -> parsedVersion.get().compareTo(value) <= 0).orElse(true);
    }

    private static Optional<ParsedVersion> parseOptionalBoundary(String value) {
        return hasText(value) ? parse(value) : Optional.empty();
    }

    private static Optional<ParsedVersion> parse(String value) {
        if (!hasText(value)) {
            return Optional.empty();
        }

        Matcher matcher = VERSION_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        List<BigInteger> numbers = new ArrayList<>();
        for (String part : matcher.group(1).split("\\.")) {
            numbers.add(new BigInteger(part));
        }

        List<String> preRelease = matcher.group(2) == null
                ? List.of()
                : List.of(matcher.group(2).split("\\."));
        return Optional.of(new ParsedVersion(numbers, preRelease));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ParsedVersion(List<BigInteger> numbers, List<String> preRelease)
            implements Comparable<ParsedVersion> {

        @Override
        public int compareTo(ParsedVersion other) {
            int length = Math.max(numbers.size(), other.numbers.size());
            for (int index = 0; index < length; index++) {
                BigInteger left = index < numbers.size() ? numbers.get(index) : BigInteger.ZERO;
                BigInteger right = index < other.numbers.size() ? other.numbers.get(index) : BigInteger.ZERO;
                int comparison = left.compareTo(right);
                if (comparison != 0) {
                    return comparison;
                }
            }

            if (preRelease.isEmpty() && other.preRelease.isEmpty()) {
                return 0;
            }
            if (preRelease.isEmpty()) {
                return 1;
            }
            if (other.preRelease.isEmpty()) {
                return -1;
            }

            int lengthToCompare = Math.max(preRelease.size(), other.preRelease.size());
            for (int index = 0; index < lengthToCompare; index++) {
                if (index >= preRelease.size()) {
                    return -1;
                }
                if (index >= other.preRelease.size()) {
                    return 1;
                }

                int comparison = compareIdentifier(preRelease.get(index), other.preRelease.get(index));
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        }

        private static int compareIdentifier(String left, String right) {
            boolean leftNumeric = left.chars().allMatch(Character::isDigit);
            boolean rightNumeric = right.chars().allMatch(Character::isDigit);
            if (leftNumeric && rightNumeric) {
                return new BigInteger(left).compareTo(new BigInteger(right));
            }
            if (leftNumeric) {
                return -1;
            }
            if (rightNumeric) {
                return 1;
            }
            return left.toLowerCase(Locale.ROOT).compareTo(right.toLowerCase(Locale.ROOT));
        }
    }
}
