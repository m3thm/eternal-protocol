import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { productsApi } from '@/api/products'
import { useToastStore } from '@/stores/toast'
import { getErrorMessage } from '@/api/client'
import { DataTable, type Column } from '@/components/ui/DataTable'
import { Button } from '@/components/ui/Button'
import { FormInput, FormSelect, FormTextarea } from '@/components/ui/FormField'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Price } from '@/components/ui/Price'
import { FaCloudArrowUp, FaStar } from 'react-icons/fa6'
import { CATEGORY_NAV } from '@/lib/categories'
import type { ProductDto, ProductImageDto, ProductInput } from '@/types'

const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

const emptyForm: ProductInput = {
  styleCode: '',
  name: '',
  category: 'MENS',
  subCategory: '',
  price: 0,
  colour: '',
  size: '',
  stockQty: 0,
  description: '',
  status: 'ACTIVE',
  showPrice: true,
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
    { header: 'Category', cell: (p) => (p.category ? `${p.category} / ${p.subCategory ?? '—'}` : '—') },
    { header: 'Colour', cell: (p) => p.colour },
    { header: 'Size', cell: (p) => p.size },
    { header: 'Price', cell: (p) => <Price amount={p.price} /> },
    {
      header: 'Status',
      cell: (p) =>
        p.status === 'COMING_SOON' ? (
          <span className="text-xs uppercase tracking-widest text-amber-400">Coming soon</span>
        ) : (
          <span className="text-xs uppercase tracking-widest text-ep-muted">Active</span>
        ),
    },
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
          category: product.category ?? 'MENS',
          subCategory: product.subCategory ?? '',
          price: product.price,
          colour: product.colour,
          size: product.size,
          stockQty: product.stockQty,
          description: product.description ?? '',
          status: product.status,
          showPrice: product.showPrice,
        }
      : emptyForm,
  )
  const [errors, setErrors] = useState<Record<string, string>>({})

  // Suggestions only — the admin can still type a brand-new subcategory
  // freely; this datalist just surfaces what's already in use for the
  // selected category so naming stays consistent (e.g. always "Hoodies",
  // never a mix of "Hoodies"/"Hoodie"/"hoodies").
  const { data: subCategorySuggestions } = useQuery({
    queryKey: ['subcategories', form.category],
    queryFn: () => productsApi.subCategories(form.category),
  })

  const saveMutation = useMutation({
    mutationFn: (input: ProductInput) => (product ? productsApi.update(product.id, input) : productsApi.create(input)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-products'] })
      pushToast(product ? 'Product updated' : 'Product created', 'success')
      onClose()
    },
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  function validate(): boolean {
    const next: Record<string, string> = {}
    if (!form.styleCode.trim()) next.styleCode = 'Required'
    if (!form.name.trim()) next.name = 'Required'
    if (!form.subCategory.trim()) next.subCategory = 'Required'
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
          <FormSelect
            label="Category"
            value={form.category}
            onChange={(e) => setForm((f) => ({ ...f, category: e.target.value as ProductInput['category'] }))}
          >
            {CATEGORY_NAV.map(({ category, label }) => (
              <option key={category} value={category}>
                {label}
              </option>
            ))}
          </FormSelect>
          <div>
            <FormInput
              label="Subcategory"
              list="subcategory-suggestions"
              value={form.subCategory}
              onChange={(e) => setForm((f) => ({ ...f, subCategory: e.target.value }))}
              error={errors.subCategory}
              placeholder="e.g. Hoodies, Caps, Bottoms"
            />
            <datalist id="subcategory-suggestions">
              {(subCategorySuggestions ?? []).map((sub) => (
                <option key={sub} value={sub} />
              ))}
            </datalist>
          </div>
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
          <FormSelect
            label="Status"
            value={form.status}
            onChange={(e) => setForm((f) => ({ ...f, status: e.target.value as ProductInput['status'] }))}
          >
            <option value="ACTIVE">Active</option>
            <option value="COMING_SOON">Coming soon</option>
          </FormSelect>
          {form.status === 'COMING_SOON' && (
            <label className="flex items-center gap-2 self-end pb-2 text-sm text-ep-bone">
              <input
                type="checkbox"
                checked={form.showPrice ?? true}
                onChange={(e) => setForm((f) => ({ ...f, showPrice: e.target.checked }))}
                className="h-4 w-4 accent-ep-bone"
              />
              Show price while coming soon
            </label>
          )}
        </div>

        {form.status === 'COMING_SOON' && (
          <p className="mt-2 text-xs text-ep-muted">
            Coming soon products only appear on the Upcoming page and can't be added to a cart — switch this back to
            Active to launch it on the regular shop.
          </p>
        )}

        <div className="mt-4">
          <FormTextarea
            label="Description"
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
          />
        </div>

        <div className="mt-6">
          {form.styleCode.trim() && form.colour.trim() ? (
            <ProductGalleryEditor styleCode={form.styleCode.trim()} colour={form.colour.trim()} />
          ) : (
            <p className="text-xs text-ep-muted">
              Enter a style code and colour above, then upload this colour's photos here.
            </p>
          )}
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <Button type="button" variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={saveMutation.isPending}>
            {product ? 'Save changes' : 'Create product'}
          </Button>
        </div>
      </form>
    </div>
  )
}

/**
 * Manages the carousel photo set for one style+colour. Deliberately
 * independent of whether the Product row itself has been saved yet — the
 * gallery is keyed on the (styleCode, colour) strings, not a product id, so
 * an admin can build out photos for a colour before or after saving sizes
 * for it. Every add/delete/reorder here also updates that colour's
 * Product.imageUrl on the backend (see ProductImageService), which is what
 * keeps the shop-grid thumbnail in sync automatically.
 */
function ProductGalleryEditor({ styleCode, colour }: { styleCode: string; colour: string }) {
  const queryClient = useQueryClient()
  const pushToast = useToastStore((s) => s.push)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [isDragging, setIsDragging] = useState(false)
  // Batch upload progress: how many of the files picked in this drop/select have finished so far.
  const [batch, setBatch] = useState<{ done: number; total: number } | null>(null)

  const galleryKey = ['product-images', styleCode]

  const { data: allImages, isLoading } = useQuery({
    queryKey: galleryKey,
    queryFn: () => productsApi.imagesByStyle(styleCode),
  })

  const images = (allImages ?? [])
    .filter((img) => img.colour.toLowerCase() === colour.toLowerCase())
    .sort((a, b) => a.sortOrder - b.sortOrder)

  const invalidate = () => queryClient.invalidateQueries({ queryKey: galleryKey })

  const removeMutation = useMutation({
    mutationFn: (imageId: number) => productsApi.removeImage(imageId),
    onSuccess: invalidate,
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const setCoverMutation = useMutation({
    mutationFn: (imageId: number) => productsApi.setCoverImage(imageId),
    onSuccess: invalidate,
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  const reorderMutation = useMutation({
    mutationFn: (orderedImageIds: number[]) => productsApi.reorderImages(styleCode, colour, orderedImageIds),
    onSuccess: invalidate,
    onError: (err) => pushToast(getErrorMessage(err), 'error'),
  })

  function validFile(file: File): boolean {
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      pushToast(`${file.name}: only JPEG, PNG, WebP, or GIF images are allowed.`, 'error')
      return false
    }
    if (file.size > MAX_IMAGE_BYTES) {
      pushToast(`${file.name}: image must be 5MB or smaller.`, 'error')
      return false
    }
    return true
  }

  /** Uploads every valid file in the batch, one at a time, so the progress counter is meaningful. */
  async function uploadBatch(fileList: FileList | File[]) {
    const files = Array.from(fileList).filter(validFile)
    if (files.length === 0) return

    setBatch({ done: 0, total: files.length })
    for (const file of files) {
      try {
        const uploaded = await productsApi.uploadImage(file)
        await productsApi.addImage(styleCode, {
          colour,
          imageUrl: uploaded.imageUrl,
          publicId: uploaded.publicId,
        })
      } catch (err) {
        pushToast(getErrorMessage(err, `Failed to upload ${file.name}`), 'error')
      }
      setBatch((b) => (b ? { ...b, done: b.done + 1 } : b))
    }
    setBatch(null)
    invalidate()
  }

  function handleFileInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    if (e.target.files?.length) uploadBatch(e.target.files)
    e.target.value = ''
  }

  function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault()
    setIsDragging(false)
    if (e.dataTransfer.files?.length) uploadBatch(e.dataTransfer.files)
  }

  function move(index: number, direction: -1 | 1) {
    const target = index + direction
    if (target < 0 || target >= images.length) return
    const reordered = [...images]
    ;[reordered[index], reordered[target]] = [reordered[target], reordered[index]]
    reorderMutation.mutate(reordered.map((img) => img.id))
  }

  return (
    <div>
      <span className="mb-1.5 block text-xs uppercase tracking-widest text-ep-muted">
        Photo gallery — {colour} (all sizes)
      </span>
      <p className="mb-3 text-xs text-ep-muted">
        Upload this colour's photos once here, then pick one as the cover shown on the shop grid.
      </p>

      {isLoading ? (
        <p className="text-xs text-ep-muted">Loading gallery…</p>
      ) : images.length > 0 ? (
        <div className="mb-4 flex flex-wrap gap-3">
          {images.map((img: ProductImageDto, i: number) => (
            <div key={img.id} className="group relative">
              <div className="relative h-24 w-20 overflow-hidden bg-ep-surface-2">
                <img src={img.imageUrl} alt="" className="h-full w-full object-cover" />
                {img.isCover && (
                  <span className="absolute left-1 top-1 flex items-center gap-1 bg-ep-black/70 px-1.5 py-0.5 text-[9px] uppercase tracking-wide text-ep-bone">
                    <FaStar className="h-2 w-2" /> Cover
                  </span>
                )}
              </div>
              <div className="mt-1 flex items-center justify-center gap-1.5">
                <button
                  type="button"
                  disabled={i === 0}
                  onClick={() => move(i, -1)}
                  className="text-[10px] text-ep-muted hover:text-ep-bone disabled:opacity-30"
                  aria-label="Move earlier"
                >
                  ←
                </button>
                {!img.isCover && (
                  <button
                    type="button"
                    onClick={() => setCoverMutation.mutate(img.id)}
                    className="text-[10px] text-ep-muted hover:text-ep-bone"
                    aria-label="Set as cover photo"
                    title="Set as cover"
                  >
                    <FaStar className="h-2.5 w-2.5" />
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => removeMutation.mutate(img.id)}
                  className="text-[10px] text-ep-muted hover:text-red-400"
                  aria-label="Remove photo"
                >
                  ✕
                </button>
                <button
                  type="button"
                  disabled={i === images.length - 1}
                  onClick={() => move(i, 1)}
                  className="text-[10px] text-ep-muted hover:text-ep-bone disabled:opacity-30"
                  aria-label="Move later"
                >
                  →
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : null}

      <div
        role="button"
        tabIndex={0}
        onClick={() => fileInputRef.current?.click()}
        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && fileInputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault()
          setIsDragging(true)
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        className={`flex cursor-pointer flex-col items-center justify-center gap-2 border border-dashed px-6 py-8 text-center transition-colors ${
          isDragging ? 'border-ep-bone bg-ep-surface-2' : 'border-ep-border hover:border-ep-muted'
        }`}
      >
        <FaCloudArrowUp className="h-6 w-6 text-ep-muted" />
        <p className="text-sm text-ep-bone">
          <span className="underline">Click to upload</span> or drag photos here
        </p>
        <p className="text-xs text-ep-muted">JPEG, PNG, WebP, or GIF — up to 5MB each. You can select several at once.</p>
        <input
          ref={fileInputRef}
          type="file"
          multiple
          accept="image/jpeg,image/png,image/webp,image/gif"
          onChange={handleFileInputChange}
          className="hidden"
        />
      </div>
      {batch && (
        <p className="mt-2 text-xs text-ep-muted">
          Uploading photo {Math.min(batch.done + 1, batch.total)} of {batch.total}…
        </p>
      )}
    </div>
  )
}
