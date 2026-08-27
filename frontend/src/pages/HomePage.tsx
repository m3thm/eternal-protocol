import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { productsApi } from '@/api/products'
import { groupByStyle, sortStylesByNewest } from '@/features/shop/groupByStyle'
import { ProductCard } from '@/components/ui/ProductCard'
import { Button } from '@/components/ui/Button'

type DropsTab = 'new' | 'upcoming'

export function HomePage() {
  const [tab, setTab] = useState<DropsTab>('new')

  const { data: activeProducts } = useQuery({
    queryKey: ['products', { status: 'ACTIVE' }],
    queryFn: () => productsApi.list({ status: 'ACTIVE' }),
  })
  const { data: comingSoonProducts } = useQuery({
    queryKey: ['products', { status: 'COMING_SOON' }],
    queryFn: () => productsApi.list({ status: 'COMING_SOON' }),
  })

  const newDropStyles = activeProducts ? sortStylesByNewest(groupByStyle(activeProducts)).slice(0, 8) : []
  const upcomingStyles = comingSoonProducts ? sortStylesByNewest(groupByStyle(comingSoonProducts)).slice(0, 8) : []
  const hasAnyDrops = newDropStyles.length > 0 || upcomingStyles.length > 0

  return (
    <div>
      {/* Hero */}
      <section className="relative flex h-dvh min-h-[600px] items-center justify-center overflow-hidden border-b border-ep-border bg-ep-black">
        <video
          autoPlay
          muted
          loop
          playsInline
          className="absolute inset-0 h-[101%] w-[101%] -translate-x-1/2 -translate-y-1/2 left-1/2 top-1/2 object-cover"
          poster="/eternal-protocol-logo.png"
        >
          <source src="/hero-video.mp4" type="video/mp4" />
        </video>
        <div className="absolute inset-0 bg-ep-black/55" />

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="relative z-10 flex flex-col items-center px-4 text-center"
        >
          <img
            src="/eternal-protocol-logo.png"
            alt="Eternal Protocol"
            className="h-40 w-40 sm:h-52 sm:w-52"
            style={{ filter: 'brightness(0) invert(1) drop-shadow(0 2px 12px rgba(0,0,0,0.6))' }}
          />
          <h1 className="mt-4 font-display text-5xl font-black leading-[0.95] sm:text-7xl lg:text-8xl">
            ETERNAL
            <br />
            PROTOCOL
          </h1>
          <p className="mt-6 text-xs uppercase tracking-widest-plus text-ep-muted sm:text-sm">
            Rise through discipline.
          </p>
          <Link to="/shop" className="mt-10 inline-block">
            <Button variant="secondary" className="px-10 py-4 tracking-widest-plus">
              Shop now
            </Button>
          </Link>
        </motion.div>
      </section>

      {/* New drops / Upcoming — chrome-tab toggle sharing one section of the page */}
      {hasAnyDrops && (
        <section className="mx-auto max-w-7xl px-4 pt-16 sm:px-6 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="flex gap-1"
          >
            <DropsTabButton active={tab === 'new'} onClick={() => setTab('new')}>
              New drops
            </DropsTabButton>
            <DropsTabButton active={tab === 'upcoming'} onClick={() => setTab('upcoming')}>
              Upcoming
            </DropsTabButton>
          </motion.div>

          <div className="border-t border-ep-border pb-16 pt-8">
            <AnimatePresence mode="wait">
              {tab === 'new' ? (
                <motion.div
                  key="new"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  transition={{ duration: 0.2 }}
                >
                  {newDropStyles.length > 0 ? (
                    <>
                      <div className="grid grid-cols-2 gap-x-6 gap-y-10 sm:grid-cols-4">
                        {newDropStyles.map((style, i) => (
                          <motion.div
                            key={style.styleCode}
                            initial={{ opacity: 0, y: 16 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: i * 0.05 }}
                          >
                            <ProductCard style={style} />
                          </motion.div>
                        ))}
                      </div>
                      <div className="mt-10 text-center">
                        <Link to="/shop/new-drops">
                          <Button variant="secondary">Shop new drops</Button>
                        </Link>
                      </div>
                    </>
                  ) : (
                    <p className="text-sm text-ep-muted">No new drops yet — check back soon.</p>
                  )}
                </motion.div>
              ) : (
                <motion.div
                  key="upcoming"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  transition={{ duration: 0.2 }}
                >
                  {upcomingStyles.length > 0 ? (
                    <>
                      <div className="grid grid-cols-2 gap-x-6 gap-y-10 sm:grid-cols-4">
                        {upcomingStyles.map((style, i) => (
                          <motion.div
                            key={style.styleCode}
                            initial={{ opacity: 0, y: 16 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: i * 0.05 }}
                          >
                            <ProductCard style={style} />
                          </motion.div>
                        ))}
                      </div>
                      <div className="mt-10 text-center">
                        <Link to="/shop/upcoming">
                          <Button variant="secondary">View upcoming</Button>
                        </Link>
                      </div>
                    </>
                  ) : (
                    <p className="text-sm text-ep-muted">Nothing coming soon right now — check back later.</p>
                  )}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </section>
      )}

      {/* Editorial banner */}
      <section className="relative overflow-hidden border-y border-ep-border">
        <img
          src="/eternal-protocol-group-shot.png"
          alt=""
          className="absolute inset-0 h-full w-full object-cover object-center"
        />
        <div className="absolute inset-0 bg-ep-black/70" />
        <div className="relative mx-auto max-w-7xl px-4 py-56 text-center sm:px-6 lg:px-8">
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="mx-auto max-w-xl font-display text-3xl leading-tight sm:text-4xl"
          >
            RISE THROUGH DESCIPLINE.
          </motion.p>
          <Link to="/shop" className="mt-8 inline-block">
            <Button variant="secondary">Explore the collection</Button>
          </Link>
        </div>
      </section>
    </div>
  )
}

/**
 * Chrome-browser-tab styling: the active tab shares the panel's background
 * and its bottom edge overlaps the panel's top border (via -mb-px) so the
 * border line visually "breaks" under the active tab, making it read as
 * physically connected to the content below rather than a separate control.
 */
function DropsTabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-selected={active}
      role="tab"
      className={`relative -mb-px rounded-t-lg border border-b-0 px-6 py-3 font-display text-lg transition-colors ${
        active
          ? 'border-ep-border bg-ep-black text-ep-bone'
          : 'border-transparent bg-transparent text-ep-muted hover:text-ep-bone'
      }`}
    >
      {children}
    </button>
  )
}
