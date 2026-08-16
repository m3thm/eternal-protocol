package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request body for changing the display order of one colour's photo
 * gallery.
 *
 * @param orderedImageIds image IDs listed in the new desired display order
 */
public record ReorderImagesRequestDto(
        @NotEmpty(message = "orderedImageIds is required") List<Long> orderedImageIds
) {}
