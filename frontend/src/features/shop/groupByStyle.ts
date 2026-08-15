import type { ProductDto } from '@/types'

export interface StyleGroup {
  styleCode: string
  name: string
  /** Lowest price across variants — what a "from LKR X" card shows. */
  fromPrice: number
  /** One representative image per style (first variant with an image). */
  imageUrl: string | null
  colours: string[]
  /** Any variant in stock — used to grey out a fully sold-out style. */
  inStock: boolean
  /** A representative variant id to link the card to, resolved further on the PDP. */
  representativeId: number
}

/** GET /api/products returns one row per colour/size variant; group by styleCode for the grid. */
export function groupByStyle(products: ProductDto[]): StyleGroup[] {
  const groups = new Map<string, ProductDto[]>()
  for (const p of products) {
    const existing = groups.get(p.styleCode)
    if (existing) existing.push(p)
    else groups.set(p.styleCode, [p])
  }

  return Array.from(groups.entries()).map(([styleCode, variants]) => {
    const withImage = variants.find((v) => v.imageUrl) ?? variants[0]
    return {
      styleCode,
      name: variants[0].name,
      fromPrice: Math.min(...variants.map((v) => v.price)),
      imageUrl: withImage.imageUrl,
      colours: Array.from(new Set(variants.map((v) => v.colour))),
      inStock: variants.some((v) => v.stockQty > 0),
      representativeId: variants[0].id,
    }
  })
}

export type SortKey = 'name_asc' | 'name_desc' | 'price_asc' | 'price_desc'

export const SORT_OPTIONS: { key: SortKey; label: string }[] = [
  { key: 'name_asc', label: 'Name: A to Z' },
  { key: 'name_desc', label: 'Name: Z to A' },
  { key: 'price_asc', label: 'Price: Low to High' },
  { key: 'price_desc', label: 'Price: High to Low' },
]

/** Sorts a copy of the grouped styles — never mutates the array passed in. */
export function sortStyles(styles: StyleGroup[], sortKey: SortKey | undefined): StyleGroup[] {
  if (!sortKey) return styles
  const sorted = [...styles]
  switch (sortKey) {
    case 'name_asc':
      return sorted.sort((a, b) => a.name.localeCompare(b.name))
    case 'name_desc':
      return sorted.sort((a, b) => b.name.localeCompare(a.name))
    case 'price_asc':
      return sorted.sort((a, b) => a.fromPrice - b.fromPrice)
    case 'price_desc':
      return sorted.sort((a, b) => b.fromPrice - a.fromPrice)
  }
}
