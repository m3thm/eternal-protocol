import type { InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

const inputClasses =
  'w-full border border-ep-border bg-ep-surface px-4 py-2.5 text-sm text-ep-bone placeholder-ep-muted outline-none focus:border-ep-bone'

interface FormInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
}

export function FormInput({ label, error, ...inputProps }: FormInputProps) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs uppercase tracking-widest text-ep-muted">{label}</span>
      <input className={inputClasses} {...inputProps} />
      {error && <span className="mt-1 block text-xs text-red-400">{error}</span>}
    </label>
  )
}

interface FormSelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
}

export function FormSelect({ label, error, children, ...selectProps }: FormSelectProps) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs uppercase tracking-widest text-ep-muted">{label}</span>
      <select className={inputClasses} {...selectProps}>
        {children}
      </select>
      {error && <span className="mt-1 block text-xs text-red-400">{error}</span>}
    </label>
  )
}

interface FormTextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  error?: string
}

export function FormTextarea({ label, error, ...textareaProps }: FormTextareaProps) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs uppercase tracking-widest text-ep-muted">{label}</span>
      <textarea className={inputClasses} rows={4} {...textareaProps} />
      {error && <span className="mt-1 block text-xs text-red-400">{error}</span>}
    </label>
  )
}
