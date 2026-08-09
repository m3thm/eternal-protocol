export function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="border border-ep-border bg-ep-surface p-5">
      <div className="text-xs uppercase tracking-widest text-ep-muted">{label}</div>
      <div className="mt-2 text-2xl font-display">{value}</div>
    </div>
  )
}
