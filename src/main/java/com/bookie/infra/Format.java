package com.bookie.infra;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Format {

    private static final DateTimeFormatter usLocalDateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final SimpleDateFormat usDateFormat = new SimpleDateFormat("MM/dd/yyyy");

    public static String dateTime(Date date) {
        return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(date);
    }

    public static String usDate(LocalDate date) {
        return date.format(usLocalDateFormatter);
    }

    public static String usDate(Date date) {
        return usDateFormat.format(date);
    }

    public static String bool(boolean value) {
        return value ? "Y" : "N";
    }

    public static String usd(BigDecimal amount) {
        return "$" + String.format("%,.2f", amount);
    }

    public static String ratings(String moodys, String sp, String fitch) {
        return blankToDash(moodys) + " / " + blankToDash(sp) + " / " + blankToDash(fitch);
    }

    private static String blankToDash(String rating) {
        return rating == null || rating.isBlank() ? "—" : rating;
    }

}
