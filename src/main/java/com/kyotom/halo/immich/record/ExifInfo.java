package com.kyotom.halo.immich.record;

import java.time.OffsetDateTime;

public record ExifInfo(
    String make,
    String model,
    Integer exifImageWidth,
    Integer exifImageHeight,
    Long fileSizeInByte,
    String orientation,
    OffsetDateTime dateTimeOriginal,
    OffsetDateTime modifyDate,
    String timeZone,
    String lensModel,
    String fNumber,
    String focalLength,
    Integer iso,
    String exposureTime,
    Double latitude,
    Double longitude,
    String city,
    String state,
    String country,
    String description,
    String projectionType,
    Integer rating
) {}
