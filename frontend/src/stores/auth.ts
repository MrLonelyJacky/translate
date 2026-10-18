import { defineStore } from 'pinia'

interface UserProfile {
  id: number
  unionId?: string
  openId?: string
  role: 'USER' | 'ADMIN'
}

interface AuthState {
  token: string
  user: UserProfile | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('docbridge_token') ?? '',
    user: null,
  }),
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('docbridge_token', token)
    },
    setUser(user: UserProfile | null) {
      this.user = user
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('docbridge_token')
    },
  },
})

