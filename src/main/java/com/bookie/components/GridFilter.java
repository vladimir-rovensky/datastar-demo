package com.bookie.components;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class GridFilter {

    public static boolean matches(Object cellValue, String filterText) {
        if (filterText == null || filterText.isBlank()) {
            return true;
        }

        return switch (cellValue) {
            case null -> false;
            case Number number -> matchesOrdinal(number.doubleValue(), filterText, Double::parseDouble);
            case Boolean booleanValue -> matchesBoolean(booleanValue, filterText);
            case LocalDate localDate ->
                    matchesOrdinal(localDate, filterText, text -> LocalDate.parse(text, DateTimeFormatter.ofPattern("MM/dd/yyyy")));
            case Date date -> matchesOrdinal(date, filterText, text -> new SimpleDateFormat("MM/dd/yyyy").parse(text));
            default -> matchesString(cellValue.toString(), filterText);
        };

    }

    private static boolean matchesString(String value, String filter) {
        var tokens = filter.split(",");

        for (var token : tokens) {
            var trimmed = token.trim();

            if (!trimmed.isEmpty() && value.toLowerCase().contains(trimmed.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private static <T> boolean matchesOrdinal(Comparable<T> value, String filter, ThrowingFunction<String, T> parse) {
        try {
            var stripped = filter.replace(" ", "");

            if (stripped.startsWith(">=")) {
                return value.compareTo(parse.apply(stripped.substring(2))) >= 0;
            }

            if (stripped.startsWith("<=")) {
                return value.compareTo(parse.apply(stripped.substring(2))) <= 0;
            }

            if (stripped.startsWith(">")) {
                return value.compareTo(parse.apply(stripped.substring(1))) > 0;
            }

            if (stripped.startsWith("<")) {
                return value.compareTo(parse.apply(stripped.substring(1))) < 0;
            }

            if (stripped.startsWith("=")) {
                return value.compareTo(parse.apply(stripped.substring(1))) == 0;
            }

            var dashIndex = stripped.indexOf('-');

            if (dashIndex > 0) {
                var low = parse.apply(stripped.substring(0, dashIndex));
                var high = parse.apply(stripped.substring(dashIndex + 1));
                return value.compareTo(low) >= 0 && value.compareTo(high) <= 0;
            }

            return value.compareTo(parse.apply(stripped)) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean matchesBoolean(Boolean value, String filter) {
        var trimmed = filter.trim();

        if ("Y".equalsIgnoreCase(trimmed)) {
            return value;
        }

        if ("N".equalsIgnoreCase(trimmed)) {
            return !value;
        }

        return false;
    }

    @FunctionalInterface
    private interface ThrowingFunction<A, B> {
        B apply(A input) throws Exception;
    }

}
