import { customersApi } from '@/api/customers'
import { useGuestCartStore } from '@/stores/cart'

/**
 * On customer login, push any guest-cart lines into the server cart, then clear
 * the guest store. Safe to call naively — POST /api/customers/me/cart already
 * increments quantity on an existing product+size+colour line rather than duplicating it.
 */
export async function mergeGuestCartIntoServer(): Promise<void> {
  const { lines, clear } = useGuestCartStore.getState()
  if (lines.length === 0) return

  await Promise.all(
    lines.map((line) =>
      customersApi.cart.add({
        productId: line.productId,
        size: line.size,
        colour: line.colour,
        quantity: line.quantity,
      }),
    ),
  )

  clear()
}
