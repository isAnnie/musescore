import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/score'

interface AuthUser {
  id: string
  username: string
  email: string
}

interface AuthResponse {
  token: string
  user: AuthUser
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const toUser = (authUser: AuthUser): User => ({
  id: authUser.id,
  username: authUser.username,
  email: authUser.email,
  scores: []
})

const parseErrorMessage = async (response: Response) => {
  try {
    const data = await response.json()
    return data.message || '请求失败'
  } catch {
    return '请求失败'
  }
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))

  const isLoggedIn = computed(() => !!token.value)

  const login = async (account: string, password: string) => {
    const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ account, password })
    })

    if (!response.ok) {
      throw new Error(await parseErrorMessage(response))
    }

    const data = await response.json() as AuthResponse
    token.value = data.token
    user.value = toUser(data.user)
    localStorage.setItem('token', data.token)

    return true
  }

  const register = async (username: string, email: string, password: string) => {
    const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password })
    })

    if (!response.ok) {
      throw new Error(await parseErrorMessage(response))
    }

    const data = await response.json() as AuthResponse
    token.value = data.token
    user.value = toUser(data.user)
    localStorage.setItem('token', data.token)

    return true
  }

  const logout = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
  }

  const checkAuth = async () => {
    if (!token.value) {
      return false
    }

    if (user.value) {
      return true
    }

    const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token.value}` }
    })

    if (!response.ok) {
      logout()
      return false
    }

    const data = await response.json() as AuthUser
    user.value = toUser(data)
    return true
  }

  const updateUser = (updates: Partial<User>) => {
    if (user.value) {
      user.value = { ...user.value, ...updates }
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    login,
    register,
    logout,
    checkAuth,
    updateUser
  }
})
