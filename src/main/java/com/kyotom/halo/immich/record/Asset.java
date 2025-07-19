package com.kyotom.halo.immich.record;

public record Asset(
    String id,
    String deviceAssetId,
    String ownerId,
    String deviceId,
    String libraryId,
    String type,
    String originalPath,
    String originalFileName,
    String originalMimeType,
    String thumbhash,
    String fileCreatedAt,
    String fileModifiedAt,
    String localDateTime,
    String updatedAt,
    boolean isFavorite,
    boolean isArchived,
    boolean isTrashed,
    String visibility,
    String duration,
    String livePhotoVideoId,
    String checksum,
    boolean isOffline,
    boolean hasMetadata,
    String duplicateId,
    boolean resized
    ) {
}
