import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { customersApi } from '@/api/customers'
import { Price } from '@/components/ui/Price'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { paymentStatusMeta } from '@/lib/status'
import { formatDate } from '@/lib/format'

export function OrderHistoryPage() {
  const { data: orders, isLoading } = useQuery({ queryKey: ['my-orders'], queryFn: customersApi.myOrders })

  return (
    <div className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
      <h1 className="font-display text-4xl">Your orders</h1>

      {isLoading && <p className="mt-8 text-sm text-ep-muted">Loading…</p>}

      {!isLoading && (!orders || orders.length === 0) && (
        <p className="mt-8 text-sm text-ep-muted">
          You haven't placed any orders yet.{' '}
          <Link to="/shop" className="text-ep-bone underline">
            Start shopping
          </Link>
        </p>
      )}

      <div className="mt-8 space-y-4">
        {orders?.map((order) => {
          const meta = paymentStatusMeta(order.paymentStatus)
          return (
            <Link
              key={order.id}
              to={`/order/${order.id}/confirmation`}
              className="block border border-ep-border bg-ep-surface p-5 hover:border-ep-bone"
            >
              <div className="flex items-center justify-between text-xs uppercase tracking-widest text-ep-muted">
                <span>Order #{order.id}</span>
                <span>{formatDate(order.orderDate)}</span>
              </div>
              <div className="mt-3 space-y-1 text-sm">
                {order.items.map((item) => (
                  <div key={`${item.productId}-${item.size}-${item.colour}`} className="text-ep-muted">
                    {item.productName} · {item.colour} · {item.size} · Qty {item.quantity}
                  </div>
                ))}
              </div>
              <div className="mt-3 flex items-center justify-between">
                <StatusBadge label={meta.label} className={meta.className} />
                <Price amount={order.totalAmount} className="text-sm" />
              </div>
            </Link>
          )
        })}
      </div>
    </div>
  )
}
