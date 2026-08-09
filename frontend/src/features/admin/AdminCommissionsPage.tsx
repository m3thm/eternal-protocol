import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { getErrorMessage } from '@/api/client'
import { DataTable, type Column } from '@/components/ui/DataTable'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Button } from '@/components/ui/Button'
import { Price } from '@/components/ui/Price'
import { commissionStatusMeta } from '@/lib/status'
import { formatDate } from '@/lib/format'
import type { CommissionDto, CommissionStatus } from '@/types'

const STATUSES: CommissionStatus[] = ['AWAITING_PAYMENT', 'EARNED', 'PAID']

export function AdminCommissionsPage() {
  const [status, setStatus] = useState<CommissionStatus | undefined>(undefined)
  const queryClient = useQueryClient()
  const pushToast = useToastStore((s) => s.push)

  const { data: commissions, isLoading } = useQuery({
    queryKey: ['admin-commissions', status],
    queryFn: () => adminApi.commissions.list(status),
  })

  const markPaidMutation = useMutation({
    mutationFn: adminApi.commissions.markPaid,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-commissions'] })
      pushToast('Commission marked as paid', 'success')
    },
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const columns: Column<CommissionDto>[] = [
    { header: 'Athlete', cell: (c) => `${c.athleteName} (${c.athleteCode})` },
    { header: 'Order', cell: (c) => `#${c.orderId}` },
    { header: 'Amount', cell: (c) => <Price amount={c.commissionAmount} /> },
    {
      header: 'Status',
      cell: (c) => {
        const meta = commissionStatusMeta(c.commissionStatus)
        return <StatusBadge label={meta.label} className={meta.className} />
      },
    },
    { header: 'Paid date', cell: (c) => (c.paidDate ? formatDate(c.paidDate) : '—') },
    {
      header: '',
      cell: (c) =>
        c.commissionStatus === 'EARNED' ? (
          <Button variant="ghost" onClick={() => markPaidMutation.mutate(c.id)} loading={markPaidMutation.isPending}>
            Mark paid
          </Button>
        ) : null,
    },
  ]

  return (
    <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl">Commissions</h1>

      <div className="mt-6 flex gap-2">
        <FilterPill selected={!status} onClick={() => setStatus(undefined)}>
          All
        </FilterPill>
        {STATUSES.map((s) => (
          <FilterPill key={s} selected={status === s} onClick={() => setStatus(status === s ? undefined : s)}>
            {s.replace('_', ' ')}
          </FilterPill>
        ))}
      </div>

      <div className="mt-6">
        {isLoading ? (
          <p className="text-sm text-ep-muted">Loading…</p>
        ) : (
          <DataTable columns={columns} rows={commissions ?? []} rowKey={(c) => c.id} emptyMessage="No commissions." />
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
