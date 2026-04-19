package com.bookie;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class SmokeTest extends TestBase {

    @Test
    void rootRedirectsToTrades() throws Exception {
        HttpResponse<String> response = getHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/"))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(307);
        assertThat(response.headers().firstValue("Location").orElse("")).endsWith("/trades");
    }

}
