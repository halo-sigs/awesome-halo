package com.kyotom.halo.immich.record;

import java.time.OffsetDateTime;
import java.util.List;

public record ImageDetail(
    String id,
    String deviceAssetId,
    String ownerId,
    Owner owner,
    String deviceId,
    String libraryId,
    String type,
    String originalPath,
    String originalFileName,
    String originalMimeType,
    String thumbhash,
    OffsetDateTime fileCreatedAt,
    OffsetDateTime fileModifiedAt,
    OffsetDateTime localDateTime,
    OffsetDateTime updatedAt,
    Boolean isFavorite,
    Boolean isArchived,
    Boolean isTrashed,
    String visibility,
    String duration,
    ExifInfo exifInfo,
    String livePhotoVideoId,
    List<String> tags,
    List<String> people,
    List<UnassignedFace> unassignedFaces,
    String checksum,
    String stack,
    Boolean isOffline,
    Boolean hasMetadata,
    String duplicateId,
    Boolean resized
) {}