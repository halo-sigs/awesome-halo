package com.kyotom.halo.immich.record;

/**
 * @param size in KB
 */
public record UploadResponse(
    String id,
    String status
) {
}