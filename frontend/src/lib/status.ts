import type { CommissionStatus, PaymentStatus } from '@/types'

interface StatusMeta {
  label: string
  className: string
}

export function paymentStatusMeta(status: PaymentStatus): StatusMeta {
  switch (status) {
    case 'PAID':
      return { label: 'Paid', className: 'bg-green-950 text-green-400 border-green-800' }
    case 'CANCELLED':
      return { label: 'Cancelled', className: 'bg-red-950 text-red-400 border-red-800' }
    case 'PENDING':
    default:
      return { label: 'Pending', className: 'bg-amber-950 text-amber-400 border-amber-800' }
  }
}

export function commissionStatusMeta(status: CommissionStatus): StatusMeta {
  switch (status) {
    case 'PAID':
      return { label: 'Paid out', className: 'bg-green-950 text-green-400 border-green-800' }
    case 'EARNED':
      return { label: 'Earned — not yet paid', className: 'bg-amber-950 text-amber-400 border-amber-800' }
    case 'AWAITING_PAYMENT':
    default:
      return { label: 'Awaiting payment', className: 'bg-ep-surface-2 text-ep-muted border-ep-border' }
  }
}
