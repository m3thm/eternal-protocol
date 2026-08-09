import { useEffect } from 'react'
import { FaChevronLeft, FaChevronRight } from 'react-icons/fa6'

interface ProductImageCarouselProps {
  images: string[]
  alt: string
  activeIndex: number
  onChange: (index: number) => void
}

/**
 * Pure presentational carousel for one colour's image set. The caller owns
 * `activeIndex` and resets it to 0 whenever the selected colour changes —
 * that's what makes picking a new colour swap the whole photo set rather
 * than just the current frame.
 */
export function ProductImageCarousel({ images, alt, activeIndex, onChange }: ProductImageCarouselProps) {
  const count = images.length
  const safeIndex = count === 0 ? 0 : ((activeIndex % count) + count) % count

  function goTo(index: number) {
    if (count === 0) return
    onChange(((index % count) + count) % count)
  }

  useEffect(() => {
    if (count === 0) return
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'ArrowLeft') goTo(safeIndex - 1)
      if (e.key === 'ArrowRight') goTo(safeIndex + 1)
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [safeIndex, count])

  if (count === 0) {
    return (
      <div className="flex aspect-[3/4] w-full items-center justify-center bg-ep-surface text-xs uppercase tracking-widest text-ep-muted">
        No image
      </div>
    )
  }

  return (
    <div>
      <div className="group relative aspect-[3/4] w-full overflow-hidden bg-ep-surface">
        <img src={images[safeIndex]} alt={alt} className="h-full w-full object-cover" />

        {count > 1 && (
          <>
            <button
              type="button"
              aria-label="Previous image"
              onClick={() => goTo(safeIndex - 1)}
              className="absolute left-3 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center bg-ep-black/50 text-ep-bone opacity-0 transition-opacity hover:bg-ep-black/70 group-hover:opacity-100"
            >
              <FaChevronLeft className="h-3.5 w-3.5" />
            </button>
            <button
              type="button"
              aria-label="Next image"
              onClick={() => goTo(safeIndex + 1)}
              className="absolute right-3 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center bg-ep-black/50 text-ep-bone opacity-0 transition-opacity hover:bg-ep-black/70 group-hover:opacity-100"
            >
              <FaChevronRight className="h-3.5 w-3.5" />
            </button>

            <div className="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-1.5">
              {images.map((_, i) => (
                <button
                  key={i}
                  type="button"
                  aria-label={`Go to image ${i + 1}`}
                  onClick={() => goTo(i)}
                  className={`h-1.5 w-1.5 rounded-full transition-colors ${
                    i === safeIndex ? 'bg-ep-bone' : 'bg-ep-bone/40 hover:bg-ep-bone/70'
                  }`}
                />
              ))}
            </div>
          </>
        )}
      </div>

      {count > 1 && (
        <div className="mt-3 flex gap-2 overflow-x-auto">
          {images.map((src, i) => (
            <button
              key={i}
              type="button"
              onClick={() => goTo(i)}
              className={`h-16 w-14 flex-shrink-0 overflow-hidden border transition-colors ${
                i === safeIndex ? 'border-ep-bone' : 'border-ep-border hover:border-ep-muted'
              }`}
            >
              <img src={src} alt={`${alt} thumbnail ${i + 1}`} className="h-full w-full object-cover" />
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
