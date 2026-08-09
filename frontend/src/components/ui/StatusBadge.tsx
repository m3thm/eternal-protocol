export function StatusBadge({ label, className }: { label: string; className: string }) {
  return (
    <span className={`inline-block rounded-full border px-3 py-1 text-xs font-medium uppercase tracking-wide ${className}`}>
      {label}
    </span>
  )
}
