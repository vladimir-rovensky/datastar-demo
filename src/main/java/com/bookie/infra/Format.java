package com.bookie.infra;

import java.math.BigDecimal;

public class Format {

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
