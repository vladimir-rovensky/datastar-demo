package com.bookie.infra;

import org.intellij.lang.annotations.Language;
import org.springframework.web.util.HtmlUtils;

public class TemplatingEngine {

    public static EscapedHtml html(@Language("HTML") String template, Object... params) {
        String result = template;
        for (int i = 0; i < params.length; i += 2) {
            var key = params[i].toString();
            var value = params[i + 1];
            var encoded = value instanceof Renderable r
                    ? r.render().toString()
                    : HtmlUtils.htmlEscape(String.valueOf(value));
            result = result.replace("${" + key + "}", encoded);
        }
        return EscapedHtml.rawHtml(result);
    }

}