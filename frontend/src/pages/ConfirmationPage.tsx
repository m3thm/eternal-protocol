import { useEffect, useState } from 'react'
import { useParams, useSearchParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ordersApi } from '@/api/orders'
import { Price } from '@/components/ui/Price'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Button } from '@/components/ui/Button'
import { paymentStatusMeta } from '@/lib/status'
import { formatDate } from '@/lib/format'

const POLL_INTERVAL_MS = 3000
const POLL_TIMEOUT_MS = 90_000

export function ConfirmationPage() {
  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  // UX hint only — PayHere can redirect the browser before, or independently of, its
  // own server-to-server webhook actually confirming payment. Never trust this alone.
  const redirectStatus = searchParams.get('status') // "success" | "cancelled" | null

  const [timedOut, setTimedOut] = useState(false)
  const [startedAt] = useState(() => Date.now())

  const { data: order, isLoading } = useQuery({
    queryKey: ['order', id],
    queryFn: () => ordersApi.get(Number(id)),
    enabled: !!id,
    refetchInterval: (query) => {
      const status = query.state.data?.paymentStatus
      if (status === 'PAID' || status === 'CANCELLED') return false
      return POLL_INTERVAL_MS
    },
  })

  useEffect(() => {
    if (!order || order.paymentStatus === 'PAID' || order.paymentStatus === 'CANCELLED') return
    const timer = setTimeout(() => setTimedOut(true), POLL_TIMEOUT_MS - (Date.now() - startedAt))
    return () => clearTimeout(timer)
  }, [order, startedAt])

  if (isLoading || !order) {
    return <div className="mx-auto max-w-2xl px-4 py-24 text-center text-sm text-ep-muted">Loading your order…</div>
  }

  const statusMeta = paymentStatusMeta(order.paymentStatus)
  const stillPending = order.paymentStatus === 'PENDING'

  return (
    <div className="mx-auto max-w-2xl px-4 py-16 sm:px-6">
      <div className="text-center">
        <StatusBadge label={statusMeta.label} className={statusMeta.className} />
        <h1 className="mt-4 font-display text-4xl">
          {order.paymentStatus === 'PAID'
            ? 'Payment confirmed'
            : order.paymentStatus === 'CANCELLED'
              ? 'Payment cancelled'
              : redirectStatus === 'cancelled'
                ? 'Payment not completed'
                : 'Confirming your payment…'}
        </h1>
        {stillPending && !timedOut && (
          <p className="mt-3 text-sm text-ep-muted">
            This can take a few moments. We'll update this page automatically — no need to refresh.
          </p>
        )}
        {stillPending && timedOut && (
          <p className="mt-3 text-sm text-amber-400">
            Still confirming — this is taking longer than usual. Your order is saved either way; we'll update
            it as soon as payment clears. You can safely leave this page.
          </p>
        )}
      </div>

      <div className="mt-10 border border-ep-border bg-ep-surface p-6">
        <div className="flex items-center justify-between text-xs uppercase tracking-widest text-ep-muted">
          <span>Order #{order.id}</span>
          <span>{formatDate(order.orderDate)}</span>
        </div>

        <div className="mt-4 divide-y divide-ep-border">
          {order.items.map((item) => (
            <div key={`${item.productId}-${item.size}-${item.colour}`} className="flex items-center justify-between py-3 text-sm">
              <div>
                <div>{item.productName}</div>
                <div className="text-xs text-ep-muted">
                  {item.colour} · {item.size} · Qty {item.quantity}
                </div>
              </div>
              <Price amount={item.lineTotal} className="text-sm" />
            </div>
          ))}
        </div>

        <div className="mt-4 space-y-1 border-t border-ep-border pt-4 text-sm">
          <div className="flex justify-between text-ep-muted">
            <span>Subtotal</span>
            <Price amount={order.subtotalAmount} />
          </div>
          {order.discountAmount > 0 && (
            <div className="flex justify-between text-green-400">
              <span>Discount {order.athleteCode ? `(${order.athleteCode})` : ''}</span>
              <span>−<Price amount={order.discountAmount} /></span>
            </div>
          )}
          <div className="flex justify-between pt-2 text-base">
            <span>Total</span>
            <Price amount={order.totalAmount} />
          </div>
        </div>

        <p className="mt-4 text-xs text-ep-muted">Delivery fee is payable to the courier on delivery.</p>
      </div>

      <div className="mt-8 flex justify-center gap-3">
        <Link to="/shop">
          <Button variant="secondary">Continue shopping</Button>
        </Link>
      </div>
    </div>
  )
}
