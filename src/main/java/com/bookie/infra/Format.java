package com.bookie.infra;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class Format {

    public static String usd(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

}
