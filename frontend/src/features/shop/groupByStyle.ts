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
