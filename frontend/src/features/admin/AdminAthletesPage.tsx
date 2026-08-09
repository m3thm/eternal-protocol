import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { getErrorMessage } from '@/api/client'
import { DataTable, type Column } from '@/components/ui/DataTable'
import { Button } from '@/components/ui/Button'
import { FormInput } from '@/components/ui/FormField'
import type { AthleteDto } from '@/types'

export function AdminAthletesPage() {
  const queryClient = useQueryClient()
  const pushToast = useToastStore((s) => s.push)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editRate, setEditRate] = useState('')

  const { data: athletes, isLoading } = useQuery({ queryKey: ['admin-athletes'], queryFn: adminApi.athletes.list })

  const toggleActiveMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => adminApi.athletes.update(id, { active }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-athletes'] }),
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const updateRateMutation = useMutation({
    mutationFn: ({ id, commissionRate }: { id: number; commissionRate: number }) =>
      adminApi.athletes.update(id, { commissionRate }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-athletes'] })
      setEditingId(null)
    },
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const columns: Column<AthleteDto>[] = [
    { header: 'Name', cell: (a) => a.name },
    { header: 'Email', cell: (a) => a.email },
    { header: 'Code', cell: (a) => a.athleteCode },
    {
      header: 'Commission',
      cell: (a) =>
        editingId === a.id ? (
          <div className="flex items-center gap-1">
            <input
              type="number"
              min={0}
              max={100}
              value={editRate}
              onChange={(e) => setEditRate(e.target.value)}
              className="w-16 border border-ep-border bg-ep-surface-2 px-2 py-1 text-sm"
              autoFocus
            />
            <button
              className="text-xs text-green-400 underline"
              onClick={() => {
                const rate = Number(editRate)
                if (!isNaN(rate) && rate >= 0 && rate <= 100) {
                  updateRateMutation.mutate({ id: a.id, commissionRate: rate })
                }
              }}
            >
              Save
            </button>
            <button className="text-xs text-ep-muted underline" onClick={() => setEditingId(null)}>
              Cancel
            </button>
          </div>
        ) : (
          <button
            className="underline decoration-dotted"
            onClick={() => {
              setEditingId(a.id)
              setEditRate(String(a.commissionRate))
            }}
          >
            {a.commissionRate}%
          </button>
        ),
    },
    {
      header: 'Status',
      cell: (a) => (
        <span className={a.active ? 'text-green-400' : 'text-ep-muted'}>{a.active ? 'Active' : 'Inactive'}</span>
      ),
    },
    {
      header: '',
      cell: (a) => (
        <Button
          variant="ghost"
          onClick={() => toggleActiveMutation.mutate({ id: a.id, active: !a.active })}
          loading={toggleActiveMutation.isPending}
        >
          {a.active ? 'Deactivate' : 'Activate'}
        </Button>
      ),
    },
  ]

  return (
    <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-4xl">Athletes</h1>
        <Button onClick={() => setShowForm((v) => !v)}>{showForm ? 'Close' : 'New athlete'}</Button>
      </div>

      {showForm && <CreateAthleteForm onCreated={() => setShowForm(false)} />}

      <div className="mt-8">
        {isLoading ? (
          <p className="text-sm text-ep-muted">Loading…</p>
        ) : (
          <DataTable columns={columns} rows={athletes ?? []} rowKey={(a) => a.id} emptyMessage="No athletes yet." />
        )}
      </div>
    </div>
  )
}

function CreateAthleteForm({ onCreated }: { onCreated: () => void }) {
  const queryClient = useQueryClient()
  const pushToast = useToastStore((s) => s.push)
  const [form, setForm] = useState({ name: '', email: '', password: '', athleteCode: '', commissionRate: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})

  const createMutation = useMutation({
    mutationFn: adminApi.athletes.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-athletes'] })
      pushToast('Athlete created', 'success')
      onCreated()
    },
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  function validate(): boolean {
    const next: Record<string, string> = {}
    if (!form.name.trim()) next.name = 'Required'
    if (!form.email.trim()) next.email = 'Required'
    if (form.password.length < 6) next.password = 'At least 6 characters'
    if (!form.athleteCode.trim()) next.athleteCode = 'Required'
    const rate = Number(form.commissionRate)
    if (!form.commissionRate || isNaN(rate) || rate < 0 || rate > 100) next.commissionRate = '0–100'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return
    createMutation.mutate({
      name: form.name.trim(),
      email: form.email.trim(),
      password: form.password,
      athleteCode: form.athleteCode.trim(),
      commissionRate: Number(form.commissionRate),
    })
  }

  return (
    <form onSubmit={handleSubmit} className="mt-6 grid gap-4 border border-ep-border bg-ep-surface p-6 sm:grid-cols-2">
      <FormInput label="Name" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} error={errors.name} />
      <FormInput
        label="Email"
        type="email"
        value={form.email}
        onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
        error={errors.email}
      />
      <FormInput
        label="Password"
        type="password"
        value={form.password}
        onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
        error={errors.password}
      />
      <FormInput
        label="Athlete code"
        value={form.athleteCode}
        onChange={(e) => setForm((f) => ({ ...f, athleteCode: e.target.value }))}
        error={errors.athleteCode}
      />
      <FormInput
        label="Commission rate (%)"
        type="number"
        min={0}
        max={100}
        value={form.commissionRate}
        onChange={(e) => setForm((f) => ({ ...f, commissionRate: e.target.value }))}
        error={errors.commissionRate}
      />
      <div className="sm:col-span-2">
        <Button type="submit" loading={createMutation.isPending}>
          Create athlete
        </Button>
      </div>
    </form>
  )
}
