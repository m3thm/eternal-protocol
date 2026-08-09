import { apiClient } from './client'
import type { AthleteDashboardDto } from '@/types'

export const athletesApi = {
  dashboard: () => apiClient.get<AthleteDashboardDto>('/api/athletes/dashboard').then((r) => r.data),
}
