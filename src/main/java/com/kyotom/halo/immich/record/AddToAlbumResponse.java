package com.kyotom.halo.immich.record;

/**
 * @param size in KB
 */
public record AddToAlbumResponse(
    String id,
    boolean success,
    String error
) {

}