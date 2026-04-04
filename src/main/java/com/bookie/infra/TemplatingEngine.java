package com.bookie.infra;

public class TemplatingEngine {

    public static String format(String template, String... params) {
        String result = template;
        for (int i = 0; i < params.length; i += 2)
            result = result.replace("${" + params[i] + "}", params[i + 1]);
        return result;
    }

}