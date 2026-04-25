package com.bookie.infra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class Format {

    private static final ZoneId US_EASTERN = ZoneId.of("America/New_York");
    private static final DateTimeFormatter usDateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter usDateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

    public static String dateTime(Date date) {
        if (date == null) {
            return "";
        }

        return date.toInstant().atZone(US_EASTERN).format(usDateTimeFormatter);
    }

    public static String usDate(LocalDate date) {
        if(date == null) {
            return "";
        }

        return date.format(usDateFormatter);
    }

    public static String usDate(Date date) {
        if(date == null) {
            return "";
        }

        return date.toInstant().atZone(US_EASTERN).toLocalDate().format(usDateFormatter);
    }

    public static String bool(boolean value) {
        return value ? "Y" : "N";
    }

    public static String usd(BigDecimal amount) {
        return "$" + decimal(amount);
    }

    public static String ratings(String moodys, String sp, String fitch) {
        return blankToDash(moodys) + " / " + blankToDash(sp) + " / " + blankToDash(fitch);
    }

    private static String blankToDash(String rating) {
        return rating == null || rating.isBlank() ? "—" : rating;
    }

    public static String decimal(BigDecimal decimal) {
        return String.format(Locale.US, "%,.2f", decimal);
    }
}
