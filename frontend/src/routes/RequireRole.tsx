import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types'

/**
 * Mirrors the backend's own route-protection table (see BACKEND_API_SPEC.md,
 * Authentication & Authorization Model). This is a UX guard only — the real
 * access control is enforced server-side; this just avoids flashing a page
 * the API would reject anyway.
 */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { role: currentRole, token } = useAuthStore()
  const location = useLocation()

  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  if (currentRole !== role) {
    return <Navigate to="/403" replace />
  }
  return <>{children}</>
}
