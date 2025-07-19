package com.kyotom.halo.immich.record;

import java.time.OffsetDateTime;
import java.util.List;

public record Album(
    String albumName,
    String description,
    String albumThumbnailAssetId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String id,
    String ownerId,
    Owner owner,
    List<String> albumUsers, // 假设 albumUsers 是一个字符串列表
    boolean shared,
    boolean hasSharedLink,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    List<String> assets, // 假设 assets 是一个字符串列表
    int assetCount,
    boolean isActivityEnabled,
    String order,
    OffsetDateTime lastModifiedAssetTimestamp
) {}
