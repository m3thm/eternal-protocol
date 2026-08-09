import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '@/api/auth'
import { useAuthStore, landingRouteForRole } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { getErrorMessage } from '@/api/client'
import { mergeGuestCartIntoServer } from '@/features/cart/mergeGuestCart'
import { Button } from '@/components/ui/Button'
import { FormInput } from '@/components/ui/FormField'

export function RegisterPage() {
  const navigate = useNavigate()
  const setSession = useAuthStore((s) => s.setSession)
  const pushToast = useToastStore((s) => s.push)

  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '', address: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function update(field: keyof typeof form, value: string) {
    setForm((f) => ({ ...f, [field]: value }))
  }

  function validate(): boolean {
    const next: Record<string, string> = {}
    if (!form.name.trim()) next.name = 'Required'
    if (!form.email.trim()) next.email = 'Required'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) next.email = 'Enter a valid email'
    if (form.password.length < 6) next.password = 'At least 6 characters'
    if (!form.phone.trim()) next.phone = 'Required'
    if (!form.address.trim()) next.address = 'Required'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitError(null)
    if (!validate()) return
    setSubmitting(true)
    try {
      const auth = await authApi.register({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        phone: form.phone.trim(),
        address: form.address.trim(),
      })
      setSession(auth)
      await mergeGuestCartIntoServer().catch(() => {
        pushToast('Account created, but some cart items may not have synced. Check your cart.', 'info')
      })
      navigate(landingRouteForRole(auth.role), { replace: true })
    } catch (err) {
      setSubmitError(getErrorMessage(err, 'Could not create your account.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm px-4 py-20 sm:px-6">
      <h1 className="font-display text-4xl">Create account</h1>
      <form onSubmit={handleSubmit} className="mt-8 space-y-5">
        <FormInput label="Full name" value={form.name} onChange={(e) => update('name', e.target.value)} error={errors.name} />
        <FormInput
          label="Email"
          type="email"
          value={form.email}
          onChange={(e) => update('email', e.target.value)}
          error={errors.email}
        />
        <FormInput
          label="Password"
          type="password"
          value={form.password}
          onChange={(e) => update('password', e.target.value)}
          error={errors.password}
        />
        <FormInput label="Phone" value={form.phone} onChange={(e) => update('phone', e.target.value)} error={errors.phone} />
        <FormInput
          label="Delivery address"
          value={form.address}
          onChange={(e) => update('address', e.target.value)}
          error={errors.address}
        />
        {submitError && <p className="text-sm text-red-400">{submitError}</p>}
        <Button type="submit" loading={submitting} className="w-full">
          Create account
        </Button>
      </form>
      <p className="mt-6 text-sm text-ep-muted">
        Already have an account?{' '}
        <Link to="/login" className="text-ep-bone underline">
          Log in
        </Link>
      </p>
    </div>
  )
}
