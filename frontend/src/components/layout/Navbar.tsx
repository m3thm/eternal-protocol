import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth'
import { useGuestCartStore } from '@/stores/cart'
import { useCartCount } from '@/features/cart/useCartCount'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `text-sm uppercase tracking-widest transition-colors hover:text-white ${isActive ? 'text-white' : 'text-ep-muted'}`

export function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { role, logout } = useAuthStore()
  const clearGuestCart = useGuestCartStore((s) => s.clear)
  const cartCount = useCartCount()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    clearGuestCart()
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-40 border-b border-ep-border/60 bg-ep-black/45 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <Link to="/" className="text-sm font-semibold uppercase tracking-widest text-ep-bone">
          ETERNAL PROTOCOL
        </Link>

        <nav className="hidden items-center gap-8 md:flex">
          <NavLink to="/shop" className={navLinkClass}>
            Shop
          </NavLink>
          {role === 'CUSTOMER' && (
            <>
              <NavLink to="/account" className={navLinkClass}>
                Account
              </NavLink>
              <NavLink to="/account/orders" className={navLinkClass}>
                Orders
              </NavLink>
            </>
          )}
          {role === 'ATHLETE' && (
            <NavLink to="/athlete/dashboard" className={navLinkClass}>
              Dashboard
            </NavLink>
          )}
          {role === 'ADMIN' && (
            <NavLink to="/admin/products" className={navLinkClass}>
              Admin
            </NavLink>
          )}
        </nav>

        <div className="flex items-center gap-5">
          {role ? (
            <button onClick={handleLogout} className="hidden text-sm uppercase tracking-widest text-ep-muted hover:text-white md:block">
              Log out
            </button>
          ) : (
            <Link to="/login" className="hidden text-sm uppercase tracking-widest text-ep-muted hover:text-white md:block">
              Log in
            </Link>
          )}
          <Link to="/cart" className="relative text-sm uppercase tracking-widest hover:text-white" aria-label="Cart">
            Cart
            {cartCount > 0 && (
              <span className="absolute -right-3 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-ep-red-bright text-[10px] text-white">
                {cartCount}
              </span>
            )}
          </Link>
          <button
            className="text-ep-muted md:hidden"
            aria-label="Toggle menu"
            onClick={() => setMobileOpen((v) => !v)}
          >
            ☰
          </button>
        </div>
      </div>

      {mobileOpen && (
        <nav className="flex flex-col gap-4 border-t border-ep-border px-4 py-4 md:hidden">
          <NavLink to="/shop" className={navLinkClass} onClick={() => setMobileOpen(false)}>
            Shop
          </NavLink>
          {role === 'CUSTOMER' && (
            <>
              <NavLink to="/account" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                Account
              </NavLink>
              <NavLink to="/account/orders" className={navLinkClass} onClick={() => setMobileOpen(false)}>
                Orders
              </NavLink>
            </>
          )}
          {role === 'ATHLETE' && (
            <NavLink to="/athlete/dashboard" className={navLinkClass} onClick={() => setMobileOpen(false)}>
              Dashboard
            </NavLink>
          )}
          {role === 'ADMIN' && (
            <NavLink to="/admin/products" className={navLinkClass} onClick={() => setMobileOpen(false)}>
              Admin
            </NavLink>
          )}
          {role ? (
            <button onClick={handleLogout} className="text-left text-sm uppercase tracking-widest text-ep-muted">
              Log out
            </button>
          ) : (
            <NavLink to="/login" className={navLinkClass} onClick={() => setMobileOpen(false)}>
              Log in
            </NavLink>
          )}
        </nav>
      )}
    </header>
  )
}
