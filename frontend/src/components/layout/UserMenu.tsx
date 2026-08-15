import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FaCircleUser, FaChevronDown } from 'react-icons/fa6'
import { useAuthStore } from '@/stores/auth'
import { useGuestCartStore } from '@/stores/cart'
import { useClickOutside } from '@/lib/useClickOutside'

/**
 * Replaces the old always-visible Account/Orders/Dashboard/Admin navbar
 * links: everything role-specific now lives behind this one user icon, so
 * the top-level navbar only ever shows the three fixed shop sections.
 */
export function UserMenu() {
  const [open, setOpen] = useState(false)
  const { role, logout } = useAuthStore()
  const clearGuestCart = useGuestCartStore((s) => s.clear)
  const navigate = useNavigate()
  const ref = useClickOutside<HTMLDivElement>(() => setOpen(false), open)

  function handleLogout() {
    setOpen(false)
    logout()
    clearGuestCart()
    navigate('/')
  }

  if (!role) {
    return (
      <Link to="/login" className="text-sm uppercase tracking-widest text-ep-muted transition-colors hover:text-white">
        Log in
      </Link>
    )
  }

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-label="Account menu"
        className="flex items-center gap-1 text-ep-muted transition-colors hover:text-white"
      >
        <FaCircleUser className="h-5 w-5" />
        <FaChevronDown className={`h-2.5 w-2.5 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-4 w-48 border border-ep-border bg-ep-black/95 py-2 backdrop-blur-md">
          {role === 'CUSTOMER' && (
            <>
              <Link to="/account" onClick={() => setOpen(false)} className="block px-4 py-2 text-xs uppercase tracking-widest text-ep-muted hover:bg-ep-surface hover:text-ep-bone">
                Account
              </Link>
              <Link to="/account/orders" onClick={() => setOpen(false)} className="block px-4 py-2 text-xs uppercase tracking-widest text-ep-muted hover:bg-ep-surface hover:text-ep-bone">
                Orders
              </Link>
            </>
          )}
          {role === 'ATHLETE' && (
            <Link to="/athlete/dashboard" onClick={() => setOpen(false)} className="block px-4 py-2 text-xs uppercase tracking-widest text-ep-muted hover:bg-ep-surface hover:text-ep-bone">
              Dashboard
            </Link>
          )}
          {role === 'ADMIN' && (
            <Link to="/admin/products" onClick={() => setOpen(false)} className="block px-4 py-2 text-xs uppercase tracking-widest text-ep-muted hover:bg-ep-surface hover:text-ep-bone">
              Admin Dashboard
            </Link>
          )}
          <button
            type="button"
            onClick={handleLogout}
            className="mt-1 block w-full border-t border-ep-border px-4 py-2 pt-3 text-left text-xs uppercase tracking-widest text-ep-muted hover:bg-ep-surface hover:text-ep-bone"
          >
            Log out
          </button>
        </div>
      )}
    </div>
  )
}
