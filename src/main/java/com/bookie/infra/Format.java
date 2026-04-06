package com.bookie.infra;

import java.math.BigDecimal;

public class Format {

    public static String usd(BigDecimal amount) {
        return "$" + String.format("%,.2f", amount);
    }

}
