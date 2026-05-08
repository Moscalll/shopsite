import { create } from 'zustand'
import type { LoginResponse, UserMe } from '@/types/api'
import { http, setStoredToken, getStoredToken } from '@/lib/http'

interface AuthState {
  token: string | null
  user: UserMe | null
  hydrated: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  fetchMe: () => Promise<void>
  hydrateFromStorage: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: null,
  user: null,
  hydrated: false,

  login: async (username, password) => {
    const { data } = await http.post<LoginResponse>('/api/auth/login', { username, password })
    const token = data.accessToken
    setStoredToken(token)
    set({ token })
    await get().fetchMe()
  },

  logout: () => {
    setStoredToken(null)
    set({ token: null, user: null })
  },

  fetchMe: async () => {
    const { data } = await http.get<UserMe>('/api/users/me')
    set({ user: data })
  },

  hydrateFromStorage: async () => {
    const token = getStoredToken()
    set({ token })
    if (!token) {
      set({ hydrated: true, user: null })
      return
    }
    try {
      await get().fetchMe()
    } catch {
      setStoredToken(null)
      set({ token: null, user: null })
    } finally {
      set({ hydrated: true })
    }
  },
}))
