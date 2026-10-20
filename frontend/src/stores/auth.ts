import { defineStore } from 'pinia'

import { getCurrentUserApi, loginApi } from '@/api/auth'
import type { UserInfo } from '@/types/api'
import { clearToken, getToken, setToken } from '@/utils/token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: null as UserInfo | null,
    loading: false,
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
  },

  actions: {
    async login(username: string, password: string) {
      this.loading = true
      try {
        const result = await loginApi({ username, password })
        setToken(result.token)
        this.token = result.token
        await this.fetchMe()
      } finally {
        this.loading = false
      }
    },

    async fetchMe() {
      this.user = await getCurrentUserApi()
    },

    logout() {
      clearToken()
      this.token = ''
      this.user = null
    },
  },
})
