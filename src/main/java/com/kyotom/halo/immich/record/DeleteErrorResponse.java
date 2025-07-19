package com.kyotom.halo.immich.record;

public record DeleteErrorResponse(
    String message,
    String error,
    int statusCode,
    String correlationId
) {
}
