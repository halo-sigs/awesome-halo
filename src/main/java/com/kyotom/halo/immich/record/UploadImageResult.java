package com.kyotom.halo.immich.record;

public record UploadImageResult(
    String imageId,
    String shardId,
    String originalFileName,
    String key,
    long size,
    String originalMimeType
    ) {
}
