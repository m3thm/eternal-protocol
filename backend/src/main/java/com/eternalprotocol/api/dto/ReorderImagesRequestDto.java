package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Image IDs for one colour's gallery, in the new desired carousel order. */
public record ReorderImagesRequestDto(
        @NotEmpty(message = "orderedImageIds is required") List<Long> orderedImageIds
) {}
