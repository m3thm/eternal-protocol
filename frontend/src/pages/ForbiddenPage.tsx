import { Link } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth'
import { Button } from '@/components/ui/Button'

export function ForbiddenPage() {
  const role = useAuthStore((s) => s.role)
  const homeFor = role === 'ADMIN' ? '/admin/products' : role === 'ATHLETE' ? '/athlete/dashboard' : role === 'CUSTOMER' ? '/account' : '/'

  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center px-4 py-32 text-center">
      <h1 className="font-display text-6xl">403</h1>
      <p className="mt-4 text-sm text-ep-muted">You don't have access to this page.</p>
      <Link to={homeFor} className="mt-8">
        <Button>Take me back</Button>
      </Link>
    </div>
  )
}
