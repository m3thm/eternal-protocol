import { apiClient } from './client'
import type {
  AdminOrderDto,
  AthleteDto,
  CommissionDto,
  CommissionStatus,
  CreateAthleteInput,
  PaymentStatus,
  UpdateAthleteInput,
} from '@/types'

export const adminApi = {
  orders: {
    list: (status?: PaymentStatus) =>
      apiClient.get<AdminOrderDto[]>('/api/admin/orders', { params: status ? { status } : {} }).then((r) => r.data),
  },

  athletes: {
    list: () => apiClient.get<AthleteDto[]>('/api/admin/athletes').then((r) => r.data),

    create: (input: CreateAthleteInput) =>
      apiClient.post<AthleteDto>('/api/admin/athletes', input).then((r) => r.data),

    /** Partial update — only send the fields actually changed. */
    update: (id: number, input: UpdateAthleteInput) =>
      apiClient.put<AthleteDto>(`/api/admin/athletes/${id}`, input).then((r) => r.data),
  },

  commissions: {
    list: (status?: CommissionStatus) =>
      apiClient
        .get<CommissionDto[]>('/api/admin/commissions', { params: status ? { status } : {} })
        .then((r) => r.data),

    markPaid: (id: number) =>
      apiClient.put<CommissionDto>(`/api/admin/commissions/${id}/mark-paid`).then((r) => r.data),
  },
}
