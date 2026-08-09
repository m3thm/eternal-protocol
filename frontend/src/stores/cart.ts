import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { ProductDto } from '@/types'

export interface GuestCartLine {
  /** Composite key so the same product+size+colour combines into one line. */
  lineId: string
  productId: number
  name: string
  price: number
  size: string
  colour: string
  quantity: number
  imageUrl: string | null
}

function lineKey(productId: number, size: string, colour: string) {
  return `${productId}::${size}::${colour}`
}

interface GuestCartState {
  lines: GuestCartLine[]
  add: (product: ProductDto, quantity?: number) => void
  setQuantity: (lineId: string, quantity: number) => void
  remove: (lineId: string) => void
  clear: () => void
}

/**
 * Guest-only cart, persisted to localStorage. The backend has no guest-cart endpoint —
 * /api/customers/me/cart is CUSTOMER-only — so this is intentionally client-side.
 * On login, merge these lines into the server cart (see mergeGuestCartIntoServer in
 * features/cart) and then clear this store; the logged-in UI reads from the server from then on.
 */
export const useGuestCartStore = create<GuestCartState>()(
  persist(
    (set, get) => ({
      lines: [],
      add: (product, quantity = 1) => {
        const key = lineKey(product.id, product.size, product.colour)
        const existing = get().lines.find((l) => l.lineId === key)
        if (existing) {
          set({
            lines: get().lines.map((l) =>
              l.lineId === key ? { ...l, quantity: l.quantity + quantity } : l,
            ),
          })
        } else {
          set({
            lines: [
              ...get().lines,
              {
                lineId: key,
                productId: product.id,
                name: product.name,
                price: product.price,
                size: product.size,
                colour: product.colour,
                quantity,
                imageUrl: product.imageUrl,
              },
            ],
          })
        }
      },
      setQuantity: (lineId, quantity) =>
        set({
          lines: get()
            .lines.map((l) => (l.lineId === lineId ? { ...l, quantity } : l))
            .filter((l) => l.quantity > 0),
        }),
      remove: (lineId) => set({ lines: get().lines.filter((l) => l.lineId !== lineId) }),
      clear: () => set({ lines: [] }),
    }),
    { name: 'ep-guest-cart' },
  ),
)
