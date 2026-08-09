import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { productsApi } from '@/api/products'
import { useToastStore } from '@/stores/toast'
import { getErrorMessage } from '@/api/client'
import { DataTable, type Column } from '@/components/ui/DataTable'
import { Button } from '@/components/ui/Button'
import { FormInput, FormTextarea } from '@/components/ui/FormField'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Price } from '@/components/ui/Price'
import type { ProductDto, ProductInput } from '@/types'

const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

const emptyForm: ProductInput = {
  styleCode: '',
  name: '',
  price: 0,
  colour: '',
  size: '',
  stockQty: 0,
  imageUrl: '',
  description: '',
}

export function AdminProductsPage() {
  const queryClient = useQueryClient()
  const pushToast = useToastStore((s) => s.push)
  const [editing, setEditing] = useState<ProductDto | 'new' | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ProductDto | null>(null)

  const { data: products, isLoading } = useQuery({ queryKey: ['admin-products'], queryFn: () => productsApi.list() })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => productsApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-products'] })
      pushToast('Product deleted', 'success')
      setDeleteTarget(null)
    },
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const columns: Column<ProductDto>[] = [
    {
      header: '',
      cell: (p) => (
        <div className="h-12 w-10 overflow-hidden bg-ep-surface-2">
          {p.imageUrl && <img src={p.imageUrl} alt={p.name} className="h-full w-full object-cover" />}
        </div>
      ),
    },
    { header: 'Style', cell: (p) => p.styleCode },
    { header: 'Name', cell: (p) => p.name },
    { header: 'Colour', cell: (p) => p.colour },
    { header: 'Size', cell: (p) => p.size },
    { header: 'Price', cell: (p) => <Price amount={p.price} /> },
    {
      header: 'Stock',
      cell: (p) => (p.stockQty < 1 ? <span className="text-red-400">0</span> : p.stockQty),
    },
    {
      header: '',
      cell: (p) => (
        <div className="flex gap-3">
          <button className="text-xs underline text-ep-muted hover:text-ep-bone" onClick={() => setEditing(p)}>
            Edit
          </button>
          <button className="text-xs underline text-ep-muted hover:text-red-400" onClick={() => setDeleteTarget(p)}>
            Delete
          </button>
        </div>
      ),
    },
  ]

  return (
    <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-4xl">Products</h1>
        <Button onClick={() => setEditing('new')}>New product</Button>
      </div>

      <div className="mt-8">
        {isLoading ? (
          <p className="text-sm text-ep-muted">Loading…</p>
        ) : (
          <DataTable columns={columns} rows={products ?? []} rowKey={(p) => p.id} emptyMessage="No products yet." />
        )}
      </div>

      {editing && (
        <ProductFormModal
          product={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
        />
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete product"
        message={`Delete "${deleteTarget?.name}" (${deleteTarget?.colour}/${deleteTarget?.size})? This can't be undone.`}
        confirmLabel="Delete"
        danger
        loading={deleteMutation.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
      />
    </div>
  )
}

function ProductFormModal({ product, onClose }: { product: ProductDto | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const pushToast = useToastStore((s) => s.push)

  const [form, setForm] = useState<ProductInput>(
    product
      ? {
          styleCode: product.styleCode,
          name: product.name,
          price: product.price,
          colour: product.colour,
          size: product.size,
          stockQty: product.stockQty,
          imageUrl: product.imageUrl ?? '',
          description: product.description ?? '',
        }
      : emptyForm,
  )
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [uploadProgress, setUploadProgress] = useState<number | null>(null)

  const saveMutation = useMutation({
    mutationFn: (input: ProductInput) => (product ? productsApi.update(product.id, input) : productsApi.create(input)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-products'] })
      pushToast(product ? 'Product updated' : 'Product created', 'success')
      onClose()
    },
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const uploadMutation = useMutation({
    mutationFn: (file: File) => productsApi.uploadImage(file, setUploadProgress),
    onSuccess: (res) => {
      setForm((f) => ({ ...f, imageUrl: res.imageUrl }))
      setUploadProgress(null)
    },
    onError: (err) => {
      pushToast(getErrorMessage(err, 'Image upload failed'), 'error')
      setUploadProgress(null)
    },
  })

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      pushToast('Please choose a JPEG, PNG, WebP, or GIF image.', 'error')
      return
    }
    if (file.size > MAX_IMAGE_BYTES) {
      pushToast('Image must be 5MB or smaller.', 'error')
      return
    }
    uploadMutation.mutate(file)
  }

  function validate(): boolean {
    const next: Record<string, string> = {}
    if (!form.styleCode.trim()) next.styleCode = 'Required'
    if (!form.name.trim()) next.name = 'Required'
    if (!form.colour.trim()) next.colour = 'Required'
    if (!form.size.trim()) next.size = 'Required'
    if (!form.price || form.price <= 0) next.price = 'Must be greater than 0'
    if (form.stockQty === undefined || form.stockQty < 0) next.stockQty = 'Must be 0 or more'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return
    saveMutation.mutate(form)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 overflow-y-auto">
      <form onSubmit={handleSubmit} className="w-full max-w-2xl border border-ep-border bg-ep-surface p-6">
        <h2 className="font-display text-2xl">{product ? 'Edit product' : 'New product'}</h2>

        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          <FormInput
            label="Style code"
            value={form.styleCode}
            onChange={(e) => setForm((f) => ({ ...f, styleCode: e.target.value }))}
            error={errors.styleCode}
            placeholder="e.g. PROTOCOL-001"
          />
          <FormInput
            label="Name"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            error={errors.name}
          />
          <FormInput
            label="Colour"
            value={form.colour}
            onChange={(e) => setForm((f) => ({ ...f, colour: e.target.value }))}
            error={errors.colour}
          />
          <FormInput
            label="Size"
            value={form.size}
            onChange={(e) => setForm((f) => ({ ...f, size: e.target.value }))}
            error={errors.size}
          />
          <FormInput
            label="Price (LKR)"
            type="number"
            min={0}
            step="0.01"
            value={form.price}
            onChange={(e) => setForm((f) => ({ ...f, price: Number(e.target.value) }))}
            error={errors.price}
          />
          <FormInput
            label="Stock quantity"
            type="number"
            min={0}
            value={form.stockQty}
            onChange={(e) => setForm((f) => ({ ...f, stockQty: Number(e.target.value) }))}
            error={errors.stockQty}
          />
        </div>

        <div className="mt-4">
          <FormTextarea
            label="Description"
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
          />
        </div>

        <div className="mt-4">
          <span className="mb-1.5 block text-xs uppercase tracking-widest text-ep-muted">Product image</span>
          <div className="flex items-center gap-4">
            {form.imageUrl && (
              <div className="h-16 w-14 overflow-hidden bg-ep-surface-2">
                <img src={form.imageUrl} alt="" className="h-full w-full object-cover" />
              </div>
            )}
            <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={handleFileSelect} className="text-sm text-ep-muted" />
          </div>
          {uploadProgress !== null && <p className="mt-1 text-xs text-ep-muted">Uploading… {uploadProgress}%</p>}
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <Button type="button" variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={saveMutation.isPending} disabled={uploadMutation.isPending}>
            {product ? 'Save changes' : 'Create product'}
          </Button>
        </div>
      </form>
    </div>
  )
}
