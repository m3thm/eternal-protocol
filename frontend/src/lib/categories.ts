import type { ProductCategory } from '@/types'

/**
 * Single source of truth for how the three fixed storefront sections map
 * between the backend's ProductCategory enum, the /shop/:categorySlug
 * route, and what's displayed in the navbar/page headings. Keeping this in
 * one place means the navbar, ShopPage, and admin category picker can't
 * drift out of sync with each other.
 */
export const CATEGORY_NAV: { category: ProductCategory; slug: string; label: string }[] = [
  { category: 'MENS', slug: 'mens', label: "Men's" },
  { category: 'WOMENS', slug: 'womens', label: "Women's" },
  { category: 'ACCESSORIES', slug: 'accessories', label: 'Accessories' },
]

export function categoryFromSlug(slug: string | undefined): ProductCategory | undefined {
  return CATEGORY_NAV.find((c) => c.slug === slug)?.category
}

export function labelForCategory(category: ProductCategory | null | undefined): string {
  return CATEGORY_NAV.find((c) => c.category === category)?.label ?? 'Shop'
}

export function slugForCategory(category: ProductCategory): string {
  return CATEGORY_NAV.find((c) => c.category === category)!.slug
}
