import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { clearToken, getToken, setToken } from '../utils/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getToken() || '')
  const loggedIn = computed(() => Boolean(token.value))

  function login(value: string) {
    token.value = value.trim()
    setToken(token.value)
  }

  function logout() {
    token.value = ''
    clearToken()
  }

  return { token, loggedIn, login, logout }
})
