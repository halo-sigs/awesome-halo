package com.kyotom.halo.immich.record;

public record UnassignedFace(
    String id,
    Integer imageHeight,
    Integer imageWidth,
    Integer boundingBoxX1,
    Integer boundingBoxX2,
    Integer boundingBoxY1,
    Integer boundingBoxY2,
    String sourceType
) {}