import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { productsApi } from '@/api/products'
import { customersApi } from '@/api/customers'
import { ordersApi, athleteCodeApi } from '@/api/orders'
import { useAuthStore } from '@/stores/auth'
import { useGuestCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import { getErrorMessage } from '@/api/client'
import { Button } from '@/components/ui/Button'
import { Price } from '@/components/ui/Price'
import { FormInput } from '@/components/ui/FormField'
import type { OrderItemInput } from '@/types'

interface CheckoutLine {
  productId: number
  name: string
  size: string
  colour: string
  quantity: number
  unitPrice: number
  imageUrl: string | null
}

interface DirectBuyState {
  directBuy?: { productId: number; quantity: number }
}

export function CheckoutPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const role = useAuthStore((s) => s.role)
  const pushToast = useToastStore((s) => s.push)
  const guestCart = useGuestCartStore()

  const directBuy = (location.state as DirectBuyState | null)?.directBuy

  // "Buy now" carries just a productId/quantity in nav state — resolve it to a full line.
  const { data: directProduct } = useQuery({
    queryKey: ['product', directBuy?.productId],
    queryFn: () => productsApi.get(directBuy!.productId),
    enabled: !!directBuy,
  })

  const { data: serverCartItems } = useQuery({
    queryKey: ['cart'],
    queryFn: customersApi.cart.list,
    enabled: role === 'CUSTOMER' && !directBuy,
  })

  // Pre-fill the delivery fields below from the account's saved details for
  // a logged-in customer — same ['profile'] cache ProfilePage uses, so this
  // is instant if they've already visited My Account this session.
  const { data: profile } = useQuery({
    queryKey: ['profile'],
    queryFn: customersApi.me,
    enabled: role === 'CUSTOMER',
  })

  const lines: CheckoutLine[] = useMemo(() => {
    if (directBuy && directProduct) {
      return [
        {
          productId: directProduct.id,
          name: directProduct.name,
          size: directProduct.size,
          colour: directProduct.colour,
          quantity: directBuy.quantity,
          unitPrice: directProduct.price,
          imageUrl: directProduct.imageUrl,
        },
      ]
    }
    if (role === 'CUSTOMER') {
      return (serverCartItems ?? []).map((i) => ({
        productId: i.productId,
        name: i.productName,
        size: i.size,
        colour: i.colour,
        quantity: i.quantity,
        unitPrice: i.price,
        imageUrl: i.imageUrl,
      }))
    }
    return guestCart.lines.map((l) => ({
      productId: l.productId,
      name: l.name,
      size: l.size,
      colour: l.colour,
      quantity: l.quantity,
      unitPrice: l.price,
      imageUrl: l.imageUrl,
    }))
  }, [directBuy, directProduct, role, serverCartItems, guestCart.lines])

  const subtotal = lines.reduce((sum, l) => sum + l.unitPrice * l.quantity, 0)

  const [customerName, setCustomerName] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')

  // Runs once, the first time the profile arrives — not on every refetch —
  // so a background refetch (e.g. tab refocus) can't stomp on something the
  // customer has already typed or edited on this form.
  const hasPrefilled = useRef(false)
  useEffect(() => {
    if (!profile || hasPrefilled.current) return
    hasPrefilled.current = true
    setCustomerName((prev) => prev || profile.name)
    setPhone((prev) => prev || profile.phone)
    setAddress((prev) => prev || profile.address)
  }, [profile])

  const [athleteCode, setAthleteCode] = useState('')
  const [codePreview, setCodePreview] = useState<{ valid: boolean; discountPercentage: number } | null>(null)
  const [checking, setChecking] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const codeCheckTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  function handleCodeChange(value: string) {
    setAthleteCode(value)
    setCodePreview(null)
    clearTimeout(codeCheckTimer.current)
    if (!value.trim()) return
    codeCheckTimer.current = setTimeout(async () => {
      setChecking(true)
      try {
        const result = await athleteCodeApi.validate(value.trim())
        setCodePreview({ valid: result.valid, discountPercentage: result.discountPercentage })
      } catch {
        // Live preview is informational only — a failed check here shouldn't block anything.
        setCodePreview(null)
      } finally {
        setChecking(false)
      }
    }, 500)
  }

  function validate(): boolean {
    const errors: Record<string, string> = {}
    if (!customerName.trim()) errors.customerName = 'Required'
    if (!phone.trim()) errors.phone = 'Required'
    if (!address.trim()) errors.address = 'Required'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return
    if (lines.length === 0) return

    setSubmitting(true)
    try {
      const items: OrderItemInput[] = lines.map((l) => ({
        productId: l.productId,
        size: l.size,
        colour: l.colour,
        quantity: l.quantity,
      }))

      const order = await ordersApi.create({
        customerName: customerName.trim(),
        phone: phone.trim(),
        address: address.trim(),
        items,
        athleteCode: athleteCode.trim() || undefined,
      })

      // The backend clears a logged-in customer's server cart automatically on order
      // creation; the guest cart is entirely client-side, so we clear it ourselves.
      if (role !== 'CUSTOMER' && !directBuy) {
        guestCart.clear()
      }

      window.location.href = order.paymentUrl
    } catch (err) {
      pushToast(getErrorMessage(err), 'error')
      setSubmitting(false)
    }
  }

  if (lines.length === 0) {
    return (
      <div className="mx-auto flex max-w-2xl flex-col items-center px-4 py-32 text-center">
        <h1 className="font-display text-3xl">Nothing to check out</h1>
        <p className="mt-3 text-sm text-ep-muted">Your cart is empty.</p>
        <Button className="mt-8" onClick={() => navigate('/shop')}>
          Shop now
        </Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8">
      <h1 className="font-display text-4xl">Checkout</h1>

      <div className="mt-10 grid gap-10 lg:grid-cols-[1.2fr_1fr]">
        <form onSubmit={handleSubmit} className="space-y-5">
          <FormInput
            label="Full name"
            value={customerName}
            onChange={(e) => setCustomerName(e.target.value)}
            error={fieldErrors.customerName}
          />
          <FormInput
            label="Phone"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            error={fieldErrors.phone}
          />
          <FormInput
            label="Delivery address"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            error={fieldErrors.address}
          />

          <div>
            <FormInput
              label="Athlete discount code (optional)"
              value={athleteCode}
              onChange={(e) => handleCodeChange(e.target.value)}
              placeholder="e.g. RISE10"
            />
            {checking && <p className="mt-1 text-xs text-ep-muted">Checking code…</p>}
            {!checking && codePreview?.valid && (
              <p className="mt-1 text-xs text-green-400">
                Code applied — {codePreview.discountPercentage}% off at checkout.
              </p>
            )}
            {!checking && codePreview && !codePreview.valid && (
              <p className="mt-1 text-xs text-ep-muted">Code not recognized — checking out at full price.</p>
            )}
          </div>

          <Button type="submit" loading={submitting} className="w-full">
            Continue to payment
          </Button>
          <p className="text-xs text-ep-muted">
            You'll be redirected to PayHere to complete payment securely. Delivery fee is payable to the
            courier on delivery.
          </p>
        </form>

        <div className="border border-ep-border bg-ep-surface p-6">
          <h2 className="text-xs uppercase tracking-widest text-ep-muted">Order summary</h2>
          <div className="mt-4 space-y-4">
            {lines.map((line) => (
              <div key={`${line.productId}-${line.size}-${line.colour}`} className="flex items-center gap-3">
                <div className="h-16 w-14 shrink-0 overflow-hidden bg-ep-surface-2">
                  {line.imageUrl && <img src={line.imageUrl} alt={line.name} className="h-full w-full object-cover" />}
                </div>
                <div className="flex-1 text-sm">
                  <div>{line.name}</div>
                  <div className="text-xs text-ep-muted">
                    {line.colour} · {line.size} · Qty {line.quantity}
                  </div>
                </div>
                <Price amount={line.unitPrice * line.quantity} className="text-sm" />
              </div>
            ))}
          </div>
          <div className="mt-6 flex items-center justify-between border-t border-ep-border pt-4 text-sm">
            <span>Subtotal</span>
            <Price amount={subtotal} />
          </div>
          {codePreview?.valid && (
            <div className="mt-2 flex items-center justify-between text-sm text-green-400">
              <span>Discount ({codePreview.discountPercentage}%)</span>
              <span>applied at payment</span>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
