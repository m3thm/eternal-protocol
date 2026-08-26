import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { customersApi } from '@/api/customers'
import { useAuthStore } from '@/stores/auth'
import { useGuestCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import { getErrorMessage } from '@/api/client'
import { Button } from '@/components/ui/Button'
import { Price } from '@/components/ui/Price'
import type { CartItemDto } from '@/types'

export function CartPage() {
  const role = useAuthStore((s) => s.role)
  return role === 'CUSTOMER' ? <ServerCart /> : <GuestCart />
}

function GuestCart() {
  const navigate = useNavigate()
  const { lines, setQuantity, remove } = useGuestCartStore()
  const subtotal = lines.reduce((sum, l) => sum + l.price * l.quantity, 0)

  if (lines.length === 0) return <EmptyCart />

  return (
    <CartShell subtotal={subtotal} onCheckout={() => navigate('/checkout')}>
      {lines.map((line) => (
        <CartRow
          key={line.lineId}
          image={line.imageUrl}
          name={line.name}
          size={line.size}
          colour={line.colour}
          price={line.price}
          quantity={line.quantity}
          onQuantityChange={(q) => (q > 0 ? setQuantity(line.lineId, q) : remove(line.lineId))}
          onRemove={() => remove(line.lineId)}
        />
      ))}
    </CartShell>
  )
}

function ServerCart() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const pushToast = useToastStore((s) => s.push)

  const { data: items, isLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: customersApi.cart.list,
  })

  const setQuantityMutation = useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: number; quantity: number }) =>
      customersApi.cart.setQuantity(itemId, quantity),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cart'] }),
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const removeMutation = useMutation({
    mutationFn: (itemId: number) => customersApi.cart.remove(itemId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cart'] }),
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const undoMutation = useMutation({
    mutationFn: customersApi.cart.undo,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['cart'] }),
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const clearMutation = useMutation({
    mutationFn: customersApi.cart.clear,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] })
      pushToast('Cart cleared', 'info', { label: 'Undo', onClick: () => undoMutation.mutate() })
    },
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  if (isLoading) return <div className="mx-auto max-w-4xl px-4 py-20 text-sm text-ep-muted">Loading cart…</div>
  if (!items || items.length === 0) return <EmptyCart />

  const subtotal = items.reduce((sum: number, i: CartItemDto) => sum + i.price * i.quantity, 0)

  return (
    <CartShell
      subtotal={subtotal}
      onCheckout={() => navigate('/checkout')}
      headerAction={
        <button
          onClick={() => clearMutation.mutate()}
          disabled={clearMutation.isPending}
          className="text-xs uppercase tracking-widest text-ep-muted underline hover:text-ep-bone disabled:opacity-60"
        >
          {clearMutation.isPending ? 'Clearing…' : 'Clear cart'}
        </button>
      }
    >
      {items.map((item: CartItemDto) => (
        <CartRow
          key={item.id}
          image={item.imageUrl}
          name={item.productName}
          size={item.size}
          colour={item.colour}
          price={item.price}
          quantity={item.quantity}
          onQuantityChange={(q) =>
            q > 0 ? setQuantityMutation.mutate({ itemId: item.id, quantity: q }) : removeMutation.mutate(item.id)
          }
          onRemove={() => removeMutation.mutate(item.id)}
        />
      ))}
    </CartShell>
  )
}

function CartShell({
  children,
  subtotal,
  onCheckout,
  headerAction,
}: {
  children: ReactNode
  subtotal: number
  onCheckout: () => void
  headerAction?: ReactNode
}) {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6 lg:px-8">
      <div className="flex items-baseline justify-between">
        <h1 className="font-display text-4xl">Your cart</h1>
        {headerAction}
      </div>
      <div className="mt-8 divide-y divide-ep-border border-y border-ep-border">{children}</div>
      <div className="mt-8 flex items-center justify-between border-t border-ep-border pt-6">
        <div>
          <div className="text-xs uppercase tracking-widest text-ep-muted">Subtotal</div>
          <Price amount={subtotal} className="text-xl" />
        </div>
        <Button onClick={onCheckout}>Checkout</Button>
      </div>
      <p className="mt-4 text-xs text-ep-muted">
        Discount codes are applied at checkout. Delivery fee is payable to the courier on delivery.
      </p>
    </div>
  )
}

function CartRow({
  image,
  name,
  size,
  colour,
  price,
  quantity,
  onQuantityChange,
  onRemove,
}: {
  image: string | null
  name: string
  size: string
  colour: string
  price: number
  quantity: number
  onQuantityChange: (quantity: number) => void
  onRemove: () => void
}) {
  return (
    <div className="flex items-center gap-4 py-5">
      <div className="h-24 w-20 shrink-0 overflow-hidden bg-ep-surface">
        {image ? <img src={image} alt={name} className="h-full w-full object-cover" /> : null}
      </div>
      <div className="flex-1">
        <div className="text-sm">{name}</div>
        <div className="mt-1 text-xs text-ep-muted">
          {colour} · {size}
        </div>
        <div className="mt-2 flex items-center gap-3">
          <button
            onClick={() => onQuantityChange(quantity - 1)}
            className="h-7 w-7 border border-ep-border text-sm hover:border-ep-bone"
            aria-label="Decrease quantity"
          >
            −
          </button>
          <span className="w-6 text-center text-sm">{quantity}</span>
          <button
            onClick={() => onQuantityChange(quantity + 1)}
            className="h-7 w-7 border border-ep-border text-sm hover:border-ep-bone"
            aria-label="Increase quantity"
          >
            +
          </button>
        </div>
      </div>
      <div className="text-right">
        <Price amount={price * quantity} className="text-sm" />
        <button onClick={onRemove} className="mt-2 block text-xs text-ep-muted underline hover:text-red-400">
          Remove
        </button>
      </div>
    </div>
  )
}

function EmptyCart() {
  const navigate = useNavigate()
  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center px-4 py-32 text-center">
      <h1 className="font-display text-4xl">Your cart is empty</h1>
      <Button className="mt-8" onClick={() => navigate('/shop')}>
        Shop now
      </Button>
    </div>
  )
}
