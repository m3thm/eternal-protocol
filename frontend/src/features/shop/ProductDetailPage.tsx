import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { productsApi } from '@/api/products'
import { customersApi } from '@/api/customers'
import { useAuthStore } from '@/stores/auth'
import { useGuestCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import { Button } from '@/components/ui/Button'
import { Price } from '@/components/ui/Price'
import { getErrorMessage } from '@/api/client'
import { NotFoundPage } from '@/pages/NotFoundPage'

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const role = useAuthStore((s) => s.role)
  const addToGuestCart = useGuestCartStore((s) => s.add)
  const pushToast = useToastStore((s) => s.push)
  const [adding, setAdding] = useState(false)

  const { data: product, isLoading, isError } = useQuery({
    queryKey: ['product', id],
    queryFn: () => productsApi.get(Number(id)),
    enabled: !!id,
  })

  // Once we know the styleCode, fetch every colour/size variant of this style
  // so the selectors can resolve to the matching sibling row (see BACKEND_API_SPEC.md,
  // GET /api/products?styleCode= — this is exactly what it's for).
  const { data: variants } = useQuery({
    queryKey: ['product-variants', product?.styleCode],
    queryFn: () => productsApi.byStyle(product!.styleCode),
    enabled: !!product?.styleCode,
  })

  const [selectedColour, setSelectedColour] = useState<string | null>(null)
  const [selectedSize, setSelectedSize] = useState<string | null>(null)

  if (isLoading) {
    return <div className="mx-auto max-w-7xl px-4 py-20 text-sm text-ep-muted">Loading…</div>
  }
  if (isError || !product) {
    return <NotFoundPage />
  }

  const colour = selectedColour ?? product.colour
  const size = selectedSize ?? product.size
  const colours = Array.from(new Set((variants ?? [product]).map((v) => v.colour)))
  const sizes = Array.from(new Set((variants ?? [product]).map((v) => v.size)))
  const activeVariant =
    (variants ?? [product]).find((v) => v.colour === colour && v.size === size) ?? product
  const outOfStock = activeVariant.stockQty < 1

  async function handleAddToCart() {
    if (outOfStock) return
    setAdding(true)
    try {
      if (role === 'CUSTOMER') {
        await customersApi.cart.add({
          productId: activeVariant.id,
          size: activeVariant.size,
          colour: activeVariant.colour,
          quantity: 1,
        })
      } else {
        addToGuestCart(activeVariant, 1)
      }
      pushToast(`${activeVariant.name} added to cart`, 'success')
    } catch (err) {
      pushToast(getErrorMessage(err), 'error')
    } finally {
      setAdding(false)
    }
  }

  function handleBuyNow() {
    if (outOfStock) return
    navigate('/checkout', { state: { directBuy: { productId: activeVariant.id, quantity: 1 } } })
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
      <div className="grid gap-10 lg:grid-cols-2">
        <div className="aspect-[3/4] overflow-hidden bg-ep-surface">
          {activeVariant.imageUrl ? (
            <img src={activeVariant.imageUrl} alt={activeVariant.name} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-xs uppercase tracking-widest text-ep-muted">
              No image
            </div>
          )}
        </div>

        <div>
          <h1 className="font-display text-4xl">{product.name}</h1>
          <Price amount={activeVariant.price} className="mt-3 block text-lg" />

          <div className="mt-8">
            <div className="mb-2 text-xs uppercase tracking-widest text-ep-muted">Colour — {colour}</div>
            <div className="flex flex-wrap gap-2">
              {colours.map((c) => (
                <button
                  key={c}
                  onClick={() => setSelectedColour(c)}
                  className={`border px-4 py-2 text-xs uppercase tracking-wide transition-colors ${
                    c === colour ? 'border-ep-bone bg-ep-bone text-ep-black' : 'border-ep-border text-ep-muted hover:border-ep-bone hover:text-ep-bone'
                  }`}
                >
                  {c}
                </button>
              ))}
            </div>
          </div>

          <div className="mt-6">
            <div className="mb-2 text-xs uppercase tracking-widest text-ep-muted">Size — {size}</div>
            <div className="flex flex-wrap gap-2">
              {sizes.map((s) => (
                <button
                  key={s}
                  onClick={() => setSelectedSize(s)}
                  className={`border px-4 py-2 text-xs uppercase tracking-wide transition-colors ${
                    s === size ? 'border-ep-bone bg-ep-bone text-ep-black' : 'border-ep-border text-ep-muted hover:border-ep-bone hover:text-ep-bone'
                  }`}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>

          <div className="mt-6 text-sm">
            {outOfStock ? (
              <span className="text-red-400">Out of stock in this colour/size</span>
            ) : activeVariant.stockQty <= 5 ? (
              <span className="text-amber-400">Only {activeVariant.stockQty} left</span>
            ) : (
              <span className="text-ep-muted">In stock</span>
            )}
          </div>

          <div className="mt-8 flex gap-3">
            <Button onClick={handleAddToCart} loading={adding} disabled={outOfStock} variant="secondary" className="flex-1">
              Add to cart
            </Button>
            <Button onClick={handleBuyNow} disabled={outOfStock} className="flex-1">
              Buy now
            </Button>
          </div>

          {product.description && (
            <p className="mt-10 whitespace-pre-line text-sm leading-relaxed text-ep-muted">{product.description}</p>
          )}
        </div>
      </div>
    </div>
  )
}
