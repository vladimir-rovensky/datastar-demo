package com.bookie.infra;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class Format {

    private static final DateTimeFormatter usLocalDateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final SimpleDateFormat usDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    private static final NumberFormat DecimalFormat = createNumberFormat();

    public static String dateTime(Date date) {
        if (date == null) {
            return "";
        }
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
        return "$" + decimal(amount);
    }

    public static String ratings(String moodys, String sp, String fitch) {
        return blankToDash(moodys) + " / " + blankToDash(sp) + " / " + blankToDash(fitch);
    }

    private static String blankToDash(String rating) {
        return rating == null || rating.isBlank() ? "—" : rating;
    }

    public static String decimal(BigDecimal decimal) {
        return DecimalFormat.format(decimal);
    }

    private static @NotNull NumberFormat createNumberFormat() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat;
    }
}
