package com.kyotom.halo.immich.exception;

import org.springframework.http.HttpStatusCode;

public class ImmichException extends RuntimeException {
    public HttpStatusCode statusCode;

    public ImmichException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
