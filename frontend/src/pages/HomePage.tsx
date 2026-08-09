import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { productsApi } from '@/api/products'
import { groupByStyle } from '@/features/shop/groupByStyle'
import { ProductCard } from '@/components/ui/ProductCard'
import { Button } from '@/components/ui/Button'

export function HomePage() {
  const { data: products } = useQuery({ queryKey: ['products', 'all'], queryFn: () => productsApi.list() })
  const styles = products ? groupByStyle(products).slice(0, 8) : []

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

      {/* Featured rail */}
      {styles.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
          <motion.h2
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="font-display text-3xl"
          >
            New arrivals
          </motion.h2>
          <div className="mt-8 grid grid-cols-2 gap-x-6 gap-y-10 sm:grid-cols-4">
            {styles.map((style, i) => (
              <motion.div
                key={style.styleCode}
                initial={{ opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.05 }}
              >
                <ProductCard style={style} />
              </motion.div>
            ))}
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
