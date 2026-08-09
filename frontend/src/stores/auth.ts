import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, Role } from '@/types'

interface AuthState {
  token: string | null
  role: Role | null
  userId: number | null
  setSession: (auth: AuthResponse) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      role: null,
      userId: null,
      setSession: (auth) => set({ token: auth.token, role: auth.role, userId: auth.userId }),
      logout: () => set({ token: null, role: null, userId: null }),
    }),
    { name: 'ep-auth' },
  ),
)

/** Where to land a user right after login/register, based on their role. */
export function landingRouteForRole(role: Role): string {
  switch (role) {
    case 'ADMIN':
      return '/admin/products'
    case 'ATHLETE':
      return '/athlete/dashboard'
    case 'CUSTOMER':
    default:
      return '/account'
  }
}
