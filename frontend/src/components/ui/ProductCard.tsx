import { Link } from 'react-router-dom'
import type { StyleGroup } from '@/features/shop/groupByStyle'
import { Price } from './Price'

export function ProductCard({ style }: { style: StyleGroup }) {
  const isComingSoon = style.status === 'COMING_SOON'

  return (
    <Link to={`/product/${style.representativeId}`} className="group block">
      <div className="relative aspect-[3/4] overflow-hidden bg-ep-surface">
        {style.imageUrl ? (
          <img
            src={style.imageUrl}
            alt={style.name}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            loading="lazy"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-xs uppercase tracking-widest text-ep-muted">
            No image
          </div>
        )}
        {isComingSoon ? (
          <div className="absolute left-2 top-2 bg-ep-bone px-2 py-1 text-[10px] uppercase tracking-widest text-ep-black">
            Coming soon
          </div>
        ) : (
          !style.inStock && (
            <div className="absolute left-2 top-2 bg-ep-black/80 px-2 py-1 text-[10px] uppercase tracking-widest text-ep-muted">
              Sold out
            </div>
          )
        )}
      </div>
      <div className="mt-3 flex items-start justify-between">
        <div>
          <h3 className="text-sm">{style.name}</h3>
          <p className="mt-1 text-xs text-ep-muted">{style.colours.join(' · ')}</p>
        </div>
        {(!isComingSoon || style.showPrice) && <Price amount={style.fromPrice} className="text-sm" />}
      </div>
    </Link>
  )
}
