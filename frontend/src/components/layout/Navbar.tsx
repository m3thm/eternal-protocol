import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { FaCartShopping, FaChevronDown, FaXmark } from 'react-icons/fa6'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/stores/auth'
import { useGuestCartStore } from '@/stores/cart'
import { useCartCount } from '@/features/cart/useCartCount'
import { productsApi } from '@/api/products'
import { CategoryNavDropdown } from '@/components/layout/CategoryNavDropdown'
import { UserMenu } from '@/components/layout/UserMenu'
import { CATEGORY_NAV } from '@/lib/categories'
import type { ProductCategory } from '@/types'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `block text-sm uppercase tracking-widest transition-colors hover:text-white ${isActive ? 'text-white' : 'text-ep-muted'}`

export function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { role, logout } = useAuthStore()
  const clearGuestCart = useGuestCartStore((s) => s.clear)
  const cartCount = useCartCount()

  function handleLogout() {
    setMobileOpen(false)
    logout()
    clearGuestCart()
  }

  return (
    <header className="sticky top-0 z-40 border-b border-ep-border/60 bg-ep-black/45 backdrop-blur-md">
      <div className="mx-auto grid max-w-7xl grid-cols-2 items-center gap-4 px-4 py-4 sm:px-6 md:grid-cols-3 lg:px-8">
        <Link to="/" className="text-sm font-semibold uppercase tracking-widest text-ep-bone">
          ETERNAL PROTOCOL
        </Link>

        <nav className="hidden items-center justify-center gap-10 md:flex">
          {CATEGORY_NAV.map(({ category, slug, label }) => (
            <CategoryNavDropdown key={category} category={category} slug={slug} label={label} />
          ))}
        </nav>

        <div className="flex items-center justify-end gap-5">
          <div className="hidden md:block">
            <UserMenu />
          </div>
          <Link to="/cart" className="relative text-ep-muted transition-colors hover:text-white" aria-label="Cart">
            <FaCartShopping className="h-5 w-5" />
            {cartCount > 0 && (
              <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-ep-red-bright text-[10px] text-white">
                {cartCount}
              </span>
            )}
          </Link>
          <button
            className="text-ep-muted md:hidden"
            aria-label="Toggle menu"
            onClick={() => setMobileOpen((v) => !v)}
          >
            {mobileOpen ? <FaXmark className="h-5 w-5" /> : <span className="text-lg leading-none">☰</span>}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <nav className="flex flex-col gap-1 border-t border-ep-border px-4 py-4 md:hidden">
          {CATEGORY_NAV.map(({ category, slug, label }) => (
            <MobileCategorySection key={category} category={category} slug={slug} label={label} onNavigate={() => setMobileOpen(false)} />
          ))}

          <div className="mt-3 border-t border-ep-border pt-3">
            {role === 'CUSTOMER' && (
              <>
                <NavLink to="/account" className={navLinkClass + ' py-2'} onClick={() => setMobileOpen(false)}>
                  Account
                </NavLink>
                <NavLink to="/account/orders" className={navLinkClass + ' py-2'} onClick={() => setMobileOpen(false)}>
                  Orders
                </NavLink>
              </>
            )}
            {role === 'ATHLETE' && (
              <NavLink to="/athlete/dashboard" className={navLinkClass + ' py-2'} onClick={() => setMobileOpen(false)}>
                Dashboard
              </NavLink>
            )}
            {role === 'ADMIN' && (
              <NavLink to="/admin/products" className={navLinkClass + ' py-2'} onClick={() => setMobileOpen(false)}>
                Admin Dashboard
              </NavLink>
            )}
            {role ? (
              <button onClick={handleLogout} className="block py-2 text-left text-sm uppercase tracking-widest text-ep-muted">
                Log out
              </button>
            ) : (
              <NavLink to="/login" className={navLinkClass + ' py-2'} onClick={() => setMobileOpen(false)}>
                Log in
              </NavLink>
            )}
          </div>
        </nav>
      )}
    </header>
  )
}

/** Mobile accordion version of one category — tap to expand its subcategory list instead of a hover dropdown. */
function MobileCategorySection({
  category,
  slug,
  label,
  onNavigate,
}: {
  category: ProductCategory
  slug: string
  label: string
  onNavigate: () => void
}) {
  const [expanded, setExpanded] = useState(false)
  const { data: subCategories } = useQuery({
    queryKey: ['subcategories', category],
    queryFn: () => productsApi.subCategories(category),
    enabled: expanded,
    staleTime: 5 * 60 * 1000,
  })

  return (
    <div className="border-b border-ep-border/60 py-1 last:border-b-0">
      <div className="flex items-center justify-between">
        <NavLink to={`/shop/${slug}`} className={navLinkClass + ' py-2'} onClick={onNavigate}>
          {label}
        </NavLink>
        <button
          type="button"
          aria-label={`Toggle ${label} subcategories`}
          onClick={() => setExpanded((v) => !v)}
          className="p-2 text-ep-muted"
        >
          <FaChevronDown className={`h-3 w-3 transition-transform ${expanded ? 'rotate-180' : ''}`} />
        </button>
      </div>
      {expanded && subCategories && subCategories.length > 0 && (
        <div className="flex flex-col gap-1 pb-2 pl-4">
          {subCategories.map((sub) => (
            <Link
              key={sub}
              to={`/shop/${slug}?subCategory=${encodeURIComponent(sub)}`}
              onClick={onNavigate}
              className="py-1.5 text-xs uppercase tracking-widest text-ep-muted hover:text-white"
            >
              {sub}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
