package com.bookie.screens;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerResponse;

public class BaseScreen {
    protected ServerResponse html(String content) {
        return ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(content);
    }
}
