package com.eternalprotocol.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents a single purchasable variant (specific colour + size
 * combination) of a clothing item. Multiple Product rows sharing the same
 * `styleCode` are the same "style" shown as one card in the Shop page —
 * e.g. "Protocol 001 Oversized Tee" in Black/M and Black/L and White/M are
 * three Product rows, one styleCode. Grouping by an explicit code (set by
 * the admin) rather than inferring it from `name` avoids two real
 * products with the same name accidentally merging, and avoids grouping
 * silently breaking on a typo in `name`.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "style_code", nullable = false)
    private String styleCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String colour;

    @Column(nullable = false)
    private String size;

    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;
}
