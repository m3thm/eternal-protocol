package com.eternalprotocol.api.dto;

/**
 * One gallery image belonging to a specific product style/colour, as shown
 * on the product detail page's photo carousel.
 *
 * @param id        image's database ID
 * @param colour    colour this image belongs to
 * @param imageUrl  public URL of the image
 * @param sortOrder display position within its colour's gallery, smallest first
 * @param isCover   whether this is the cover photo shown on the shop grid
 */
public record ProductImageDto(
        Long id,
        String colour,
        String imageUrl,
        Integer sortOrder,
        boolean isCover
) {}
