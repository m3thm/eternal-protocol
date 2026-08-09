import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/Button'

export function NotFoundPage() {
  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center px-4 py-32 text-center">
      <h1 className="font-display text-6xl">404</h1>
      <p className="mt-4 text-sm text-ep-muted">We couldn't find what you're looking for.</p>
      <Link to="/shop" className="mt-8">
        <Button>Back to shop</Button>
      </Link>
    </div>
  )
}
