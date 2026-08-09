import { formatCurrency } from '@/lib/format'

export function Price({ amount, className = '' }: { amount: number; className?: string }) {
  return <span className={className}>{formatCurrency(amount)}</span>
}
