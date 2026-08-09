import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { productsApi } from '@/api/products'
import { ProductCard } from '@/components/ui/ProductCard'
import { groupByStyle } from './groupByStyle'

export function ShopPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const colour = searchParams.get('colour') ?? undefined
  const size = searchParams.get('size') ?? undefined

  const { data: products, isLoading, isError } = useQuery({
    queryKey: ['products', { colour, size }],
    queryFn: () => productsApi.list({ colour, size }),
  })

  // Unfiltered fetch so filter pill options don't shrink as filters are applied.
  const { data: allProducts } = useQuery({
    queryKey: ['products', 'all'],
    queryFn: () => productsApi.list(),
  })
  const availableColours = Array.from(new Set((allProducts ?? []).map((p) => p.colour))).sort()
  const availableSizes = Array.from(new Set((allProducts ?? []).map((p) => p.size))).sort()

  function setFilter(key: 'colour' | 'size', value: string | undefined) {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    setSearchParams(next)
  }

  const styles = products ? groupByStyle(products) : []

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl">Shop</h1>

      <div className="mt-6 flex flex-wrap gap-6 border-b border-ep-border pb-6">
        <FilterGroup label="Colour" options={availableColours} active={colour} onChange={(v) => setFilter('colour', v)} />
        <FilterGroup label="Size" options={availableSizes} active={size} onChange={(v) => setFilter('size', v)} />
      </div>

      {isLoading && <p className="mt-10 text-sm text-ep-muted">Loading products…</p>}
      {isError && <p className="mt-10 text-sm text-red-400">Couldn't load products. Please try again.</p>}
      {!isLoading && !isError && styles.length === 0 && (
        <p className="mt-10 text-sm text-ep-muted">No products match these filters.</p>
      )}

      <div className="mt-10 grid grid-cols-2 gap-x-6 gap-y-10 sm:grid-cols-3 lg:grid-cols-4">
        {styles.map((style) => (
          <ProductCard key={style.styleCode} style={style} />
        ))}
      </div>
    </div>
  )
}

function FilterGroup({
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
  return (
    <div>
      <div className="mb-2 text-xs uppercase tracking-widest text-ep-muted">{label}</div>
      <div className="flex flex-wrap gap-2">
        <FilterPill selected={!active} onClick={() => onChange(undefined)}>
          All
        </FilterPill>
        {options.map((opt) => (
          <FilterPill key={opt} selected={active === opt} onClick={() => onChange(active === opt ? undefined : opt)}>
            {opt}
          </FilterPill>
        ))}
      </div>
    </div>
  )
}

function FilterPill({
  children,
  selected,
  onClick,
}: {
  children: string
  selected: boolean
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className={`border px-3 py-1.5 text-xs uppercase tracking-wide transition-colors ${
        selected ? 'border-ep-bone bg-ep-bone text-ep-black' : 'border-ep-border text-ep-muted hover:border-ep-bone hover:text-ep-bone'
      }`}
    >
      {children}
    </button>
  )
}
