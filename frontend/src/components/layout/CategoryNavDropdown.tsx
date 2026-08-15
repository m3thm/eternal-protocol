import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { FaChevronDown } from 'react-icons/fa6'
import { productsApi } from '@/api/products'
import { useClickOutside } from '@/lib/useClickOutside'
import type { ProductCategory } from '@/types'

interface CategoryNavDropdownProps {
  category: ProductCategory
  slug: string
  label: string
  onNavigate?: () => void
}

/**
 * One MEN'S / WOMEN'S / ACCESSORIES navbar entry: clicking the label itself
 * goes straight to that category's shop page, while the chevron/hover
 * opens a panel listing that category's subcategories (fetched live, so it
 * only ever shows subcategories that actually have products right now).
 */
export function CategoryNavDropdown({ category, slug, label, onNavigate }: CategoryNavDropdownProps) {
  const [open, setOpen] = useState(false)
  const ref = useClickOutside<HTMLDivElement>(() => setOpen(false), open)

  const { data: subCategories } = useQuery({
    queryKey: ['subcategories', category],
    queryFn: () => productsApi.subCategories(category),
    enabled: open,
    staleTime: 5 * 60 * 1000,
  })

  return (
    <div ref={ref} className="relative flex items-center gap-1">
      <Link
        to={`/shop/${slug}`}
        onClick={() => { setOpen(false); onNavigate?.() }}
        className="text-sm uppercase tracking-widest text-ep-muted transition-colors hover:text-white"
      >
        {label}
      </Link>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-label={`${label} subcategories`}
        className="text-ep-muted transition-colors hover:text-white"
      >
        <FaChevronDown className={`h-2.5 w-2.5 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div className="absolute left-1/2 top-full z-50 mt-4 w-56 -translate-x-1/2 border border-ep-border bg-ep-black/95 py-2 backdrop-blur-md">
          <Link
            to={`/shop/${slug}`}
            onClick={() => { setOpen(false); onNavigate?.() }}
            className="block px-4 py-2 text-xs uppercase tracking-widest text-ep-bone hover:bg-ep-surface"
          >
            All {label}
          </Link>
          {subCategories && subCategories.length > 0 && (
            <div className="mt-1 border-t border-ep-border pt-1">
              {subCategories.map((sub) => (
                <Link
                  key={sub}
                  to={`/shop/${slug}?subCategory=${encodeURIComponent(sub)}`}
                  onClick={() => { setOpen(false); onNavigate?.() }}
                  className="block px-4 py-2 text-xs uppercase tracking-widest text-ep-muted hover:bg-ep-surface hover:text-ep-bone"
                >
                  {sub}
                </Link>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
