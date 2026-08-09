import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { adminApi } from '@/api/admin'
import { DataTable, type Column } from '@/components/ui/DataTable'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Price } from '@/components/ui/Price'
import { paymentStatusMeta } from '@/lib/status'
import { formatDate } from '@/lib/format'
import type { AdminOrderDto, PaymentStatus } from '@/types'

const STATUSES: PaymentStatus[] = ['PENDING', 'PAID', 'CANCELLED']

export function AdminOrdersPage() {
  const [status, setStatus] = useState<PaymentStatus | undefined>(undefined)

  const { data: orders, isLoading } = useQuery({
    queryKey: ['admin-orders', status],
    queryFn: () => adminApi.orders.list(status),
  })

  const columns: Column<AdminOrderDto>[] = [
    { header: 'Order', cell: (o) => <span className="text-ep-muted">#{o.id}</span> },
    { header: 'Customer', cell: (o) => o.customerName },
    { header: 'Phone', cell: (o) => o.customerPhone },
    { header: 'Address', cell: (o) => <span className="max-w-xs truncate block">{o.customerAddress}</span> },
    {
      header: 'Items',
      cell: (o) => (
        <div className="space-y-0.5">
          {o.items.map((item) => (
            <div key={`${item.productId}-${item.size}-${item.colour}`}>
              {item.productName} ({item.colour}/{item.size}) ×{item.quantity}
            </div>
          ))}
        </div>
      ),
    },
    { header: 'Date', cell: (o) => formatDate(o.orderDate) },
    { header: 'Discount', cell: (o) => (o.discountAmount > 0 ? <Price amount={o.discountAmount} /> : '—') },
    { header: 'Total', cell: (o) => <Price amount={o.totalAmount} /> },
    {
      header: 'Status',
      cell: (o) => {
        const meta = paymentStatusMeta(o.paymentStatus)
        return <StatusBadge label={meta.label} className={meta.className} />
      },
    },
    { header: 'Athlete code', cell: (o) => o.athleteCode ?? '—' },
  ]

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl">Orders</h1>

      <div className="mt-6 flex gap-2">
        <FilterPill selected={!status} onClick={() => setStatus(undefined)}>
          All
        </FilterPill>
        {STATUSES.map((s) => (
          <FilterPill key={s} selected={status === s} onClick={() => setStatus(status === s ? undefined : s)}>
            {s}
          </FilterPill>
        ))}
      </div>

      <div className="mt-6">
        {isLoading ? (
          <p className="text-sm text-ep-muted">Loading…</p>
        ) : (
          <DataTable columns={columns} rows={orders ?? []} rowKey={(o) => o.id} emptyMessage="No orders." />
        )}
      </div>
    </div>
  )
}

function FilterPill({ children, selected, onClick }: { children: string; selected: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={`border px-3 py-1.5 text-xs uppercase tracking-wide transition-colors ${
        selected ? 'border-ep-bone bg-ep-bone text-ep-black' : 'border-ep-border text-ep-muted hover:border-ep-bone hover:text-ep-bone'
      }`}
    >
      {children}
    </button>
  )
}
