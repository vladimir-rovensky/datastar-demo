package com.bookie.infra;

public class HtmlExtensions {
    public static final HtmlExtensions X = new HtmlExtensions();

    public FetchBuilder get(String url) {
        return new FetchBuilder("get", "'" + url + "'");
    }

    public FetchBuilder get(EscapedHtml url) {
        return new FetchBuilder("get", url.toString());
    }

    public FetchBuilder post(String url) {
        return new FetchBuilder("post", "'" + url + "'");
    }

    public FetchBuilder post(EscapedHtml url) {
        return new FetchBuilder("post", url.toString());
    }

    public FetchBuilder put(String url) {
        return new FetchBuilder("put", "'" + url + "'");
    }

    public FetchBuilder put(EscapedHtml url) {
        return new FetchBuilder("put", url.toString());
    }

    public FetchBuilder delete(String url) {
        return new FetchBuilder("delete", "'" + url + "'");
    }

    public FetchBuilder delete(EscapedHtml url) {
        return new FetchBuilder("delete", url.toString());
    }
}
