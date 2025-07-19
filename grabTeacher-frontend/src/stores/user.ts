import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface User {
  id: number
  username: string
  email: string
  userType: 'student' | 'teacher' | 'admin'
  token?: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  confirmPassword: string
  userType: 'student' | 'teacher' | 'admin'
  realName: string
  phone?: string
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data?: T
  code?: number
}

export interface TeacherInfo {
  educationBackground?: string
  teachingExperience?: number
  subjects?: string
  specialties?: string
  hourlyRate?: number
  introduction?: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))
  const isLoggedIn = ref<boolean>(!!token.value)

  // API 基础配置
  const API_BASE_URL = 'http://localhost:8080/api'

  // 设置用户信息
  const setUser = (userData: User) => {
    user.value = userData
    if (userData.token) {
      token.value = userData.token
      localStorage.setItem('token', userData.token)
      isLoggedIn.value = true
    }
  }

  // 清除用户信息
  const clearUser = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    isLoggedIn.value = false
  }

  // 用户注册
  const register = async (registerData: RegisterRequest): Promise<ApiResponse<User>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(registerData),
      })

      const result: ApiResponse<User> = await response.json()

      if (result.success && result.data) {
        setUser(result.data)
      }

      return result
    } catch (error) {
      console.error('注册失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 用户登录
  const login = async (loginData: LoginRequest): Promise<ApiResponse<User>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(loginData),
      })

      const result: ApiResponse<User> = await response.json()

      if (result.success && result.data) {
        setUser(result.data)
      }

      return result
    } catch (error) {
      console.error('登录失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 用户退出
  const logout = async (): Promise<void> => {
    try {
      if (token.value) {
        await fetch(`${API_BASE_URL}/auth/logout`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token.value}`,
          },
        })
      }
    } catch (error) {
      console.error('退出登录失败:', error)
    } finally {
      clearUser()
    }
  }

  // 初始化时检查本地存储的token
  const initializeAuth = () => {
    const storedToken = localStorage.getItem('token')
    if (storedToken) {
      token.value = storedToken
      isLoggedIn.value = true
      // 这里可以添加验证token有效性的逻辑
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    setUser,
    clearUser,
    register,
    login,
    logout,
    initializeAuth,
  }
})
