import type { ButtonHTMLAttributes, ReactNode } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  loading?: boolean
  children: ReactNode
}

const variants: Record<string, string> = {
  primary: 'bg-ep-bone text-ep-black hover:bg-white disabled:bg-ep-surface-2 disabled:text-ep-muted',
  secondary: 'bg-transparent text-ep-bone border border-ep-bone hover:bg-ep-bone hover:text-ep-black',
  ghost: 'bg-transparent text-ep-bone hover:bg-ep-surface-2',
  danger: 'bg-ep-red text-ep-bone hover:bg-ep-red-bright',
}

export function Button({ variant = 'primary', loading, disabled, children, className = '', ...rest }: ButtonProps) {
  return (
    <button
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 px-6 py-3 text-sm font-medium uppercase tracking-widest transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${variants[variant]} ${className}`}
      {...rest}
    >
      {loading && (
        <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent" />
      )}
      {children}
    </button>
  )
}
