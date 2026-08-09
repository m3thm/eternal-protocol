import { apiClient } from './client'
import type { AuthResponse, LoginInput, RegisterInput } from '@/types'

export const authApi = {
  login: (input: LoginInput) =>
    apiClient.post<AuthResponse>('/api/auth/login', input).then((r) => r.data),

  register: (input: RegisterInput) =>
    apiClient.post<AuthResponse>('/api/auth/register', input).then((r) => r.data),
}
