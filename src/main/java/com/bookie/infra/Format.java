package com.bookie.infra;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Format {

    public static String dateTime(Date date) {
        return new SimpleDateFormat("MM/dd/yyyy hh:mm:ss").format(date);
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
