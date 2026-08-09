import { apiClient } from './client'
import type { CreateOrderInput, CreateOrderResponse, OrderDetailDto, ValidateCodeResponse } from '@/types'

export const ordersApi = {
  create: (input: CreateOrderInput) =>
    apiClient.post<CreateOrderResponse>('/api/orders', input).then((r) => r.data),

  get: (id: number) => apiClient.get<OrderDetailDto>(`/api/orders/${id}`).then((r) => r.data),
}

export const athleteCodeApi = {
  /** Always returns 200 — an unrecognized/inactive code resolves to valid:false, not a 404. */
  validate: (code: string) =>
    apiClient.get<ValidateCodeResponse>(`/api/athletes/validate-code/${encodeURIComponent(code)}`).then((r) => r.data),
}
