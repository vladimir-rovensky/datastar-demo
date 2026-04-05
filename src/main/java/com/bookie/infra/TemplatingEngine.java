package com.bookie.infra;

import org.intellij.lang.annotations.Language;
import org.springframework.web.util.HtmlUtils;

public class TemplatingEngine {

    public static EscapedHtml html(@Language("HTML") String template, Object... params) {
        String result = template;
        for (int i = 0; i < params.length; i += 2) {
            var key = params[i].toString();
            var value = params[i + 1];
            if (result.contains("@{" + key + "}") && !(value instanceof Renderable)) {
                throw new IllegalArgumentException("@{" + key + "} requires Renderable but got " + value.getClass().getSimpleName());
            }
            var raw = value instanceof Renderable r ? r.render().html() : value.toString();
            result = result
                    .replace("${" + key + "}", HtmlUtils.htmlEscape(value.toString()))
                    .replace("@{" + key + "}", raw);
        }
        return EscapedHtml.html(result);
    }

}