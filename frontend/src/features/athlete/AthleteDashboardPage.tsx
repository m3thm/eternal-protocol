import { useQuery } from '@tanstack/react-query'
import { athletesApi } from '@/api/athletes'
import { StatCard } from '@/components/ui/StatCard'
import { DataTable, type Column } from '@/components/ui/DataTable'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Price } from '@/components/ui/Price'
import { paymentStatusMeta, commissionStatusMeta } from '@/lib/status'
import { formatCurrency, formatDate } from '@/lib/format'
import type { AthleteDashboardOrder } from '@/types'

export function AthleteDashboardPage() {
  const { data, isLoading } = useQuery({ queryKey: ['athlete-dashboard'], queryFn: athletesApi.dashboard })

  if (isLoading || !data) {
    return <div className="mx-auto max-w-6xl px-4 py-20 text-sm text-ep-muted">Loading dashboard…</div>
  }

  const columns: Column<AthleteDashboardOrder>[] = [
    {
      header: 'Order',
      cell: (o) => <span className="text-ep-muted">#{o.orderId}</span>,
    },
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
    {
      header: 'Payment',
      cell: (o) => {
        const meta = paymentStatusMeta(o.paymentStatus)
        return <StatusBadge label={meta.label} className={meta.className} />
      },
    },
    { header: 'Commission', cell: (o) => <Price amount={o.commissionAmount} /> },
    {
      header: 'Status',
      cell: (o) => {
        const meta = commissionStatusMeta(o.commissionStatus)
        return <StatusBadge label={meta.label} className={meta.className} />
      },
    },
  ]

  return (
    <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="font-display text-4xl">Athlete dashboard</h1>
        <span className="text-sm text-ep-muted">
          Code <span className="text-ep-bone">{data.athleteCode}</span> · {data.commissionRate}% commission
        </span>
      </div>

      <div className="mt-8 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        <StatCard label="Total orders" value={String(data.totalOrders)} />
        <StatCard label="Sales generated" value={formatCurrency(data.salesGenerated)} />
        <StatCard label="Commission earned" value={formatCurrency(data.commissionEarned)} />
        <StatCard label="Paid out" value={formatCurrency(data.paidCommission)} />
        <StatCard label="Pending payout" value={formatCurrency(data.pendingCommission)} />
      </div>

      <h2 className="mt-12 text-xs uppercase tracking-widest text-ep-muted">Orders using your code</h2>
      <div className="mt-4">
        <DataTable columns={columns} rows={data.orders} rowKey={(o) => o.orderId} emptyMessage="No orders yet." />
      </div>
    </div>
  )
}
