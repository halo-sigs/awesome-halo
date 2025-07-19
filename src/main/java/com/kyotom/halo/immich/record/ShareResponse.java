package com.kyotom.halo.immich.record;

import java.util.List;

/**
 * @param
 */
public record ShareResponse(
    String id,
    String description,
    String password,
    String userId,
    String key,
    String type,
    String createdAt,
    String expiresAt,
    List<Asset> assets,
    boolean allowUpload,
    boolean allowDownload,
    boolean showMetadata
) {
}