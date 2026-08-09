import { useEffect, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { customersApi } from '@/api/customers'
import { useToastStore } from '@/stores/toast'
import { getErrorMessage } from '@/api/client'
import { Button } from '@/components/ui/Button'
import { FormInput } from '@/components/ui/FormField'

export function ProfilePage() {
  const pushToast = useToastStore((s) => s.push)
  const queryClient = useQueryClient()

  const { data: profile, isLoading } = useQuery({ queryKey: ['profile'], queryFn: customersApi.me })

  const [form, setForm] = useState({ name: '', phone: '', address: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (profile) setForm({ name: profile.name, phone: profile.phone, address: profile.address })
  }, [profile])

  const updateMutation = useMutation({
    mutationFn: customersApi.updateMe,
    onSuccess: (updated) => {
      queryClient.setQueryData(['profile'], updated)
      pushToast('Profile updated', 'success')
    },
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  function validate(): boolean {
    const next: Record<string, string> = {}
    if (!form.name.trim()) next.name = 'Required'
    if (!form.phone.trim()) next.phone = 'Required'
    if (!form.address.trim()) next.address = 'Required'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return
    updateMutation.mutate({ name: form.name.trim(), phone: form.phone.trim(), address: form.address.trim() })
  }

  if (isLoading || !profile) {
    return <div className="mx-auto max-w-lg px-4 py-20 text-sm text-ep-muted">Loading…</div>
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-16 sm:px-6">
      <h1 className="font-display text-4xl">Your profile</h1>

      <form onSubmit={handleSubmit} className="mt-8 space-y-5">
        <FormInput label="Email" value={profile.email} disabled className="opacity-60" />
        <FormInput
          label="Full name"
          value={form.name}
          onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          error={errors.name}
        />
        <FormInput
          label="Phone"
          value={form.phone}
          onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
          error={errors.phone}
        />
        <FormInput
          label="Delivery address"
          value={form.address}
          onChange={(e) => setForm((f) => ({ ...f, address: e.target.value }))}
          error={errors.address}
        />
        <Button type="submit" loading={updateMutation.isPending}>
          Save changes
        </Button>
      </form>
      <p className="mt-4 text-xs text-ep-muted">
        Email can't be changed here. Contact support if you need to update your login email.
      </p>
    </div>
  )
}
