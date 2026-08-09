import axios, { type AxiosError } from 'axios'
import type { ApiError } from '@/types'
import { useAuthStore } from '@/stores/auth'

const baseURL = import.meta.env.VITE_API_BASE_URL

if (!baseURL) {
  // Fail loudly in dev rather than silently hitting the wrong host
  console.error(
    'VITE_API_BASE_URL is not set. Copy .env.example to .env.local and point it at your backend.',
  )
}

export const apiClient = axios.create({ baseURL })

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Distinguishes "not logged in at all" (401) from "logged in, wrong role" (403),
// matching the backend's explicit AuthenticationEntryPoint / AccessDeniedHandler split.
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout()
    }
    // 403s are left for the calling code / route guards to react to (redirect to /403)
    // without destroying a valid session that's simply in the wrong place.
    return Promise.reject(error)
  },
)

/** Pulls the backend's consistent { message } shape out of an Axios error, with a fallback. */
export function getErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (axios.isAxiosError(error)) {
    const apiError = error.response?.data as ApiError | undefined
    if (apiError?.message) return apiError.message
    if (error.code === 'ERR_NETWORK') return 'Could not reach the server. Check your connection and try again.'
  }
  return fallback
}
