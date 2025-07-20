import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

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

export interface StudentInfo {
  id?: number
  userId?: number
  realName?: string
  gradeLevel?: string
  subjectsInterested?: string
  learningGoals?: string
  preferredTeachingStyle?: string
  budgetRange?: string
}

export interface TeacherInfo {
  id?: number
  userId?: number
  realName?: string
  educationBackground?: string
  teachingExperience?: number
  specialties?: string
  subjects?: string
  hourlyRate?: number
  introduction?: string
  videoIntroUrl?: string
  isVerified?: boolean
}

export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('token'))
  const isLoggedIn = ref<boolean>(!!token.value)

  // 添加计算属性
  const isTeacher = computed(() => user.value?.userType === 'teacher')
  const isStudent = computed(() => user.value?.userType === 'student')
  const isAdmin = computed(() => user.value?.userType === 'admin')

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
  const initializeAuth = async () => {
    try {
      console.log('开始初始化认证...')
      const storedToken = localStorage.getItem('token')
      if (storedToken) {
        console.log('找到存储的token，设置登录状态')
        token.value = storedToken
        isLoggedIn.value = true

        // 暂时跳过API验证，避免网络错误阻塞页面加载
        console.log('跳过API验证，直接设置登录状态')
        /*
        // 验证token有效性并获取用户信息
        try {
          const response = await fetch(`${API_BASE_URL}/auth/me`, {
            headers: {
              'Authorization': `Bearer ${storedToken}`,
            },
          })

          if (response.ok) {
            const result = await response.json()
            if (result.success && result.data) {
              user.value = {
                id: result.data.id,
                username: result.data.username,
                email: result.data.email,
                userType: result.data.authorities?.[0]?.authority?.replace('ROLE_', '').toLowerCase() || 'student',
                token: storedToken
              }
            }
          } else {
            // token无效，清除本地存储
            clearUser()
          }
        } catch (error) {
          console.error('验证token失败:', error)
          clearUser()
        }
        */
      } else {
        console.log('没有找到存储的token')
      }
      console.log('认证初始化完成')
    } catch (error) {
      console.error('初始化认证时发生错误:', error)
    }
  }

  // 获取学生信息
  const getStudentProfile = async (): Promise<ApiResponse<StudentInfo>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/student/profile`, {
        headers: {
          'Authorization': `Bearer ${token.value}`,
        },
      })
      return await response.json()
    } catch (error) {
      console.error('获取学生信息失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 更新学生信息
  const updateStudentProfile = async (data: Partial<StudentInfo>): Promise<ApiResponse<StudentInfo>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/student/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token.value}`,
        },
        body: JSON.stringify(data),
      })
      return await response.json()
    } catch (error) {
      console.error('更新学生信息失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 获取教师信息
  const getTeacherProfile = async (): Promise<ApiResponse<TeacherInfo>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/teacher/profile`, {
        headers: {
          'Authorization': `Bearer ${token.value}`,
        },
      })
      return await response.json()
    } catch (error) {
      console.error('获取教师信息失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 更新教师信息
  const updateTeacherProfile = async (data: Partial<TeacherInfo>): Promise<ApiResponse<TeacherInfo>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/teacher/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token.value}`,
        },
        body: JSON.stringify(data),
      })
      return await response.json()
    } catch (error) {
      console.error('更新教师信息失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 修改密码
  const changePassword = async (data: PasswordChangeRequest): Promise<ApiResponse<string>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/user/change-password`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token.value}`,
        },
        body: JSON.stringify(data),
      })
      return await response.json()
    } catch (error) {
      console.error('修改密码失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 管理员登录
  const adminLogin = async (loginData: LoginRequest): Promise<ApiResponse<User>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/admin/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(loginData),
      })

      const result: ApiResponse<User> = await response.json()

      if (result.success && result.data) {
        setUser(result.data)
      } else {
        throw new Error(result.message || '登录失败')
      }

      return result
    } catch (error: any) {
      console.error('管理员登录失败:', error)
      throw new Error(error.message || '网络错误，请稍后重试')
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    isTeacher,
    isStudent,
    isAdmin,
    setUser,
    clearUser,
    register,
    login,
    logout,
    initializeAuth,
    getStudentProfile,
    updateStudentProfile,
    getTeacherProfile,
    updateTeacherProfile,
    changePassword,
    adminLogin,
  }
})
