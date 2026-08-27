import { apiClient } from './client'
import type {
  ProductCategory,
  ProductDto,
  ProductImageDto,
  ProductImageInput,
  ProductInput,
  ProductStatus,
  UploadImageResponse,
} from '@/types'

export interface ProductQuery {
  colour?: string
  size?: string
  styleCode?: string
  category?: ProductCategory
  subCategory?: string
  /**
   * Storefront pages (category pages, New Drops, Upcoming) always pass this
   * explicitly so a COMING_SOON product never leaks into normal browsing.
   * Leave it out — as the admin product table does — to see every product
   * regardless of status.
   */
  status?: ProductStatus
}

export const productsApi = {
  list: (query: ProductQuery = {}) =>
    apiClient.get<ProductDto[]>('/api/products', { params: query }).then((r) => r.data),

  /** All colour/size variants of one style — what the PDP selector should call. */
  byStyle: (styleCode: string) =>
    apiClient.get<ProductDto[]>('/api/products', { params: { styleCode } }).then((r) => r.data),

  get: (id: number) =>
    apiClient.get<ProductDto>(`/api/products/${id}`).then((r) => r.data),

  /** Distinct subcategories currently in use within a category — powers the navbar's dropdowns. */
  subCategories: (category: ProductCategory) =>
    apiClient.get<string[]>('/api/products/subcategories', { params: { category } }).then((r) => r.data),

  create: (input: ProductInput) =>
    apiClient.post<ProductDto>('/api/products', input).then((r) => r.data),

  update: (id: number, input: ProductInput) =>
    apiClient.put<ProductDto>(`/api/products/${id}`, input).then((r) => r.data),

  remove: (id: number) => apiClient.delete(`/api/products/${id}`),

  uploadImage: (file: File, onProgress?: (pct: number) => void) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient
      .post<UploadImageResponse>('/api/products/upload-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (evt) => {
          if (onProgress && evt.total) onProgress(Math.round((evt.loaded / evt.total) * 100))
        },
      })
      .then((r) => r.data)
  },

  /** Full gallery for a style, every colour — what the PDP carousel groups by colour. */
  imagesByStyle: (styleCode: string) =>
    apiClient.get<ProductImageDto[]>(`/api/products/style/${styleCode}/images`).then((r) => r.data),

  /** Attach an already-uploaded image (see uploadImage) to a style+colour's gallery. */
  addImage: (styleCode: string, input: ProductImageInput) =>
    apiClient.post<ProductImageDto>(`/api/products/style/${styleCode}/images`, input).then((r) => r.data),

  removeImage: (imageId: number) => apiClient.delete(`/api/products/images/${imageId}`),

  /** Marks this gallery photo as the cover — the one shown on the shop grid and as the PDP's first frame. */
  setCoverImage: (imageId: number) =>
    apiClient.put<ProductImageDto>(`/api/products/images/${imageId}/cover`).then((r) => r.data),

  reorderImages: (styleCode: string, colour: string, orderedImageIds: number[]) =>
    apiClient
      .put<ProductImageDto[]>(`/api/products/style/${styleCode}/images/reorder`, { orderedImageIds }, {
        params: { colour },
      })
      .then((r) => r.data),
}
