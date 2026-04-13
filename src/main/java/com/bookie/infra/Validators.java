package com.bookie.infra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

public class Validators {

    public static String and(String... errors) {
        for (var error : errors) {
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    public static String required(String value) {
        return (value == null || value.isBlank()) ? "This field is required" : null;
    }

    public static String required(Object value) {
        return value == null ? "This field is required" : null;
    }

    public static String greaterThan(BigDecimal value, BigDecimal min) {
        if (value == null) {
            return null;
        }
        return value.compareTo(min) <= 0 ? "Must be greater than " + min.stripTrailingZeros().toPlainString() : null;
    }

    public static String atLeast(BigDecimal value, BigDecimal min) {
        if (value == null) {
            return null;
        }
        return value.compareTo(min) < 0 ? "Must be greater than or equal to " + min.stripTrailingZeros().toPlainString() : null;
    }

    public static <T extends Comparable<T>> String oneOf(T value, Set<T> validValues) {
        if (!validValues.contains(value)) {
            var sorted = validValues.stream().sorted().toList();
            return "Must be one of " + sorted;
        }
        return null;
    }

    public static String after(LocalDate date, LocalDate other, String otherLabel) {
        if (date == null || other == null) {
            return null;
        }
        return !date.isAfter(other) ? "Must be after " + otherLabel : null;
    }

    public static String before(LocalDate date, LocalDate other, String otherLabel) {
        if (date == null || other == null) {
            return null;
        }
        return !date.isBefore(other) ? "Must be before " + otherLabel : null;
    }

    public static <T> String unique(T value, Stream<T> allValues) {
        if (value == null) {
            return null;
        }
        return allValues.filter(value::equals).count() > 1 ? "The value must be unique" : null;
    }
}
