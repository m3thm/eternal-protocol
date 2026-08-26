import { useToastStore } from '@/stores/toast'

const variantClasses: Record<string, string> = {
  error: 'border-red-800 bg-red-950 text-red-300',
  success: 'border-green-800 bg-green-950 text-green-300',
  info: 'border-ep-border bg-ep-surface text-ep-bone',
}

export function ToastViewport() {
  const { toasts, dismiss } = useToastStore()

  return (
    <div className="fixed bottom-4 right-4 z-[100] flex w-full max-w-sm flex-col gap-2">
      {toasts.map((t) => (
        <div
          key={t.id}
          role="alert"
          className={`flex items-start justify-between gap-3 border px-4 py-3 text-sm shadow-lg ${variantClasses[t.variant]}`}
        >
          <span className="flex-1">{t.message}</span>
          {t.action && (
            <button
              onClick={() => {
                t.action!.onClick()
                dismiss(t.id)
              }}
              className="shrink-0 text-xs font-medium uppercase tracking-widest underline hover:text-white"
            >
              {t.action.label}
            </button>
          )}
          <button onClick={() => dismiss(t.id)} className="shrink-0 text-ep-muted hover:text-ep-bone" aria-label="Dismiss">
            ×
          </button>
        </div>
      ))}
    </div>
  )
}
