package com.eternalprotocol.api.dto;

/** Returned by the image upload endpoint — frontend takes this URL and
 *  includes it as `imageUrl` in a subsequent create/update product request. */
public record ImageUploadResponseDto(String imageUrl) {}
