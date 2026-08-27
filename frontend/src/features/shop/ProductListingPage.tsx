import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { FaChevronDown, FaSliders } from 'react-icons/fa6'
import { productsApi } from '@/api/products'
import { ProductCard } from '@/components/ui/ProductCard'
import { useClickOutside } from '@/lib/useClickOutside'
import { groupByStyle, sortStyles, sortStylesByNewest, SORT_OPTIONS, type SortKey } from './groupByStyle'
import type { ProductCategory, ProductStatus } from '@/types'

interface ProductListingPageProps {
  /** Page heading, e.g. "Men's", "New Drops", "Upcoming". */
  heading: string
  /** Top-level category to scope to, or omit for a cross-category listing (New Drops, Upcoming). */
  category?: ProductCategory
  /**
   * Lifecycle status every page must pass explicitly — this is what keeps
   * COMING_SOON products out of the regular category pages and New Drops,
   * and out of the Upcoming page once they've launched.
   */
  status: ProductStatus
  /**
   * When true, "Featured" (the default/no-sort option) means newest-first
   * instead of the API's natural order — used by New Drops and Upcoming,
   * where recency is the whole point of the page.
   */
  sortNewestFirst?: boolean
  /** Shown when the listing is empty — defaults to a generic message. */
  emptyMessage?: string
}

/**
 * Shared filter/sort/grid chrome behind the MEN'S/WOMEN'S/ACCESSORIES
 * category pages, New Drops, and Upcoming — they only differ in which
 * category/status they fetch and what the page is called.
 */
export function ProductListingPage({ heading, category, status, sortNewestFirst, emptyMessage }: ProductListingPageProps) {
  const [searchParams, setSearchParams] = useSearchParams()

  const subCategory = searchParams.get('subCategory') ?? undefined
  const colour = searchParams.get('colour') ?? undefined
  const size = searchParams.get('size') ?? undefined
  const sort = (searchParams.get('sort') as SortKey | null) ?? undefined

  const { data: products, isLoading, isError } = useQuery({
    queryKey: ['products', { category, status, subCategory, colour, size }],
    queryFn: () => productsApi.list({ category, status, subCategory, colour, size }),
  })

  // Fetched without colour/size (but still scoped to category/status/subCategory)
  // so the Filter dropdown's colour/size options don't shrink to just
  // what's left after a filter is already applied.
  const { data: unfilteredProducts } = useQuery({
    queryKey: ['products', 'unfiltered', { category, status, subCategory }],
    queryFn: () => productsApi.list({ category, status, subCategory }),
  })
  const availableColours = useMemo(
    () => Array.from(new Set((unfilteredProducts ?? []).map((p) => p.colour))).sort(),
    [unfilteredProducts],
  )
  const availableSizes = useMemo(
    () => Array.from(new Set((unfilteredProducts ?? []).map((p) => p.size))).sort(),
    [unfilteredProducts],
  )

  function setParam(key: string, value: string | undefined) {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    setSearchParams(next)
  }

  function clearFilters() {
    const next = new URLSearchParams(searchParams)
    next.delete('colour')
    next.delete('size')
    setSearchParams(next)
  }

  const grouped = products ? groupByStyle(products) : []
  const styles = sort ? sortStyles(grouped, sort) : sortNewestFirst ? sortStylesByNewest(grouped) : grouped
  const activeFilterCount = [colour, size].filter(Boolean).length

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl">{heading}</h1>

      <div className="mt-6 flex items-center justify-between border-b border-ep-border pb-6">
        <FilterDropdown
          availableColours={availableColours}
          availableSizes={availableSizes}
          colour={colour}
          size={size}
          activeCount={activeFilterCount}
          onChangeColour={(v) => setParam('colour', v)}
          onChangeSize={(v) => setParam('size', v)}
          onClear={clearFilters}
        />
        <SortDropdown sort={sort} onChange={(v) => setParam('sort', v)} />
      </div>

      {isLoading && <p className="mt-10 text-sm text-ep-muted">Loading products…</p>}
      {isError && <p className="mt-10 text-sm text-red-400">Couldn't load products. Please try again.</p>}
      {!isLoading && !isError && styles.length === 0 && (
        <p className="mt-10 text-sm text-ep-muted">{emptyMessage ?? 'No products match these filters.'}</p>
      )}

      <div className="mt-10 grid grid-cols-2 gap-x-6 gap-y-10 sm:grid-cols-3 lg:grid-cols-4">
        {styles.map((style) => (
          <ProductCard key={style.styleCode} style={style} />
        ))}
      </div>
    </div>
  )
}

function FilterDropdown({
  availableColours,
  availableSizes,
  colour,
  size,
  activeCount,
  onChangeColour,
  onChangeSize,
  onClear,
}: {
  availableColours: string[]
  availableSizes: string[]
  colour?: string
  size?: string
  activeCount: number
  onChangeColour: (value: string | undefined) => void
  onChangeSize: (value: string | undefined) => void
  onClear: () => void
}) {
  const [open, setOpen] = useState(false)
  const ref = useClickOutside<HTMLDivElement>(() => setOpen(false), open)

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="flex items-center gap-2 border border-ep-border px-4 py-2 text-xs uppercase tracking-widest text-ep-bone transition-colors hover:border-ep-bone"
      >
        <FaSliders className="h-3 w-3" />
        Filter
        {activeCount > 0 && (
          <span className="flex h-4 w-4 items-center justify-center rounded-full bg-ep-bone text-[10px] text-ep-black">
            {activeCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute left-0 top-full z-50 mt-3 w-72 border border-ep-border bg-ep-black/95 p-5 backdrop-blur-md">
          <FilterSection
            label="Colour"
            options={availableColours}
            active={colour}
            onChange={onChangeColour}
          />
          <FilterSection
            label="Size"
            options={availableSizes}
            active={size}
            onChange={onChangeSize}
          />
          {activeCount > 0 && (
            <button
              type="button"
              onClick={() => {
                onClear()
                setOpen(false)
              }}
              className="mt-1 text-xs uppercase tracking-widest text-ep-muted underline hover:text-ep-bone"
            >
              Clear filters
            </button>
          )}
        </div>
      )}
    </div>
  )
}

function FilterSection({
  label,
  options,
  active,
  onChange,
}: {
  label: string
  options: string[]
  active?: string
  onChange: (value: string | undefined) => void
}) {
  if (options.length === 0) return null
  return (
    <div className="mb-5 last:mb-4">
      <div className="mb-2 text-xs uppercase tracking-widest text-ep-muted">{label}</div>
      <div className="flex flex-wrap gap-2">
        {options.map((opt) => (
          <button
            key={opt}
            type="button"
            onClick={() => onChange(active === opt ? undefined : opt)}
            className={`border px-3 py-1.5 text-xs uppercase tracking-wide transition-colors ${
              active === opt
                ? 'border-ep-bone bg-ep-bone text-ep-black'
                : 'border-ep-border text-ep-muted hover:border-ep-bone hover:text-ep-bone'
            }`}
          >
            {opt}
          </button>
        ))}
      </div>
    </div>
  )
}

function SortDropdown({ sort, onChange }: { sort?: SortKey; onChange: (value: SortKey | undefined) => void }) {
  const [open, setOpen] = useState(false)
  const ref = useClickOutside<HTMLDivElement>(() => setOpen(false), open)
  const activeLabel = SORT_OPTIONS.find((o) => o.key === sort)?.label

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="flex items-center gap-2 border border-ep-border px-4 py-2 text-xs uppercase tracking-widest text-ep-bone transition-colors hover:border-ep-bone"
      >
        Sort{activeLabel ? `: ${activeLabel}` : ''}
        <FaChevronDown className={`h-2.5 w-2.5 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-3 w-56 border border-ep-border bg-ep-black/95 py-2 backdrop-blur-md">
          <button
            type="button"
            onClick={() => {
              onChange(undefined)
              setOpen(false)
            }}
            className={`block w-full px-4 py-2 text-left text-xs uppercase tracking-widest hover:bg-ep-surface ${
              !sort ? 'text-ep-bone' : 'text-ep-muted hover:text-ep-bone'
            }`}
          >
            Featured
          </button>
          {SORT_OPTIONS.map((opt) => (
            <button
              key={opt.key}
              type="button"
              onClick={() => {
                onChange(opt.key)
                setOpen(false)
              }}
              className={`block w-full px-4 py-2 text-left text-xs uppercase tracking-widest hover:bg-ep-surface ${
                sort === opt.key ? 'text-ep-bone' : 'text-ep-muted hover:text-ep-bone'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
