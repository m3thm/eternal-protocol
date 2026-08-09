import { useQuery } from '@tanstack/react-query'
import { customersApi } from '@/api/customers'
import { useAuthStore } from '@/stores/auth'
import { useGuestCartStore } from '@/stores/cart'

export function useCartCount(): number {
  const role = useAuthStore((s) => s.role)
  const guestLines = useGuestCartStore((s) => s.lines)

  const { data: serverCart } = useQuery({
    queryKey: ['cart'],
    queryFn: customersApi.cart.list,
    enabled: role === 'CUSTOMER',
  })

  if (role === 'CUSTOMER') {
    return (serverCart ?? []).reduce((sum, item) => sum + item.quantity, 0)
  }
  return guestLines.reduce((sum, line) => sum + line.quantity, 0)
}
