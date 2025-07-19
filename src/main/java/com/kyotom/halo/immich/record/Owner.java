package com.kyotom.halo.immich.record;

import java.time.OffsetDateTime;

public record Owner(
    String id,
    String email,
    String name,
    String profileImagePath,
    String avatarColor,
    OffsetDateTime profileChangedAt
) {}

