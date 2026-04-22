package com.bookie.infra;

import java.util.ArrayList;
import java.util.List;

import static com.bookie.infra.TemplatingEngine.html;

public class FetchBuilder implements Renderable {

    public enum Retry { NEVER, ALWAYS }

    private final String method;
    private final String renderedUrl;
    private Boolean openWhenHidden = true;
    private Retry retry = Retry.NEVER;
    private boolean excludeAllSignals;
    private String includeSignals;
    private boolean requestCancellation = true;
    private String payload;

    FetchBuilder(String method, String renderedUrl) {
        this.method = method;
        this.renderedUrl = renderedUrl;
    }

    public FetchBuilder withOpenWhenHidden() {
        this.openWhenHidden = true;
        return this;
    }

    public FetchBuilder withOpenWhenHidden(boolean value) {
        this.openWhenHidden = value;
        return this;
    }

    public FetchBuilder withRetry(Retry retry) {
        this.retry = retry;
        return this;
    }

    public FetchBuilder withExcludeAllSignals() {
        this.excludeAllSignals = true;
        return this;
    }

    public FetchBuilder withIncludeSignals(String pattern) {
        this.includeSignals = "/" + pattern + "/";
        return this;
    }

    public FetchBuilder withIncludeSignals(EscapedHtml expression) {
        this.includeSignals = expression.toString();
        return this;
    }

    public FetchBuilder withRequestCancellation(boolean enabled) {
        this.requestCancellation = enabled;
        return this;
    }

    public FetchBuilder withPayload(String jsExpression) {
        this.payload = jsExpression;
        return this;
    }

    @Override
    public EscapedHtml render() {
        var options = buildOptions();
        var optionsSuffix = options.isEmpty() ? "" : ", {" + String.join(", ", options) + "}";
        return html("@" + method + "(" + renderedUrl + optionsSuffix + ")");
    }

    private List<String> buildOptions() {
        var options = new ArrayList<String>();

        if (openWhenHidden != null) {
            options.add("openWhenHidden: " + openWhenHidden);
        }

        if (retry == Retry.ALWAYS) {
            options.add("retry: 'always'");
        }

        if (retry == Retry.NEVER) {
            options.add("retry: 'never'");
            options.add("retryMaxCount: 0");
        }

        if (excludeAllSignals) {
            options.add("filterSignals: {exclude: /.*/}");
        } else if (includeSignals != null) {
            options.add("filterSignals: {include: " + includeSignals + "}");
        }

        if (!requestCancellation) {
            options.add("requestCancellation: 'disabled'");
        }

        if (payload != null) {
            options.add("payload: " + payload);
        }

        return options;
    }
}
