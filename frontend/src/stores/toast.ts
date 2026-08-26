import { create } from 'zustand'

interface Toast {
  id: number
  message: string
  variant: 'error' | 'success' | 'info'
  action?: { label: string; onClick: () => void }
}

interface ToastState {
  toasts: Toast[]
  push: (message: string, variant?: Toast['variant'], action?: Toast['action']) => void
  dismiss: (id: number) => void
}

let nextId = 1

export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],
  push: (message, variant = 'info', action) => {
    const id = nextId++
    set({ toasts: [...get().toasts, { id, message, variant, action }] })
    setTimeout(() => get().dismiss(id), 5000)
  },
  dismiss: (id) => set({ toasts: get().toasts.filter((t) => t.id !== id) }),
}))
