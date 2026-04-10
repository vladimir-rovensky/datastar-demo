package com.bookie.infra;

import org.springframework.web.servlet.function.ServerRequest;

import java.util.UUID;

public record TabID(String sessionID, String localID) {

    public static TabID forExisting(ServerRequest request) {
        //noinspection OptionalGetWithoutIsPresent
        var localID = request.param("tabID").orElse(request.headers().firstHeader("X-tabID"));
        return new TabID(request.session().getId(), localID);
    }

    public static TabID forNew(ServerRequest request) {
        return new TabID(request.session().getId(), UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return sessionID + ":::" + localID;
    }
}
