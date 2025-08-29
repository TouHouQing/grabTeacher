import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface User {
  id: number
  username: string
  email: string
  userType: 'student' | 'teacher' | 'admin'
  avatarUrl?: string
  realName?: string
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
  birthDate?: string
  gradeLevel?: string
  subjectsInterested?: string
  subjectIds?: number[]  // 感兴趣的科目ID列表
  learningGoals?: string
  preferredTeachingStyle?: string
  budgetRange?: string
  gender?: string
  avatarUrl?: string
}

export interface TeacherInfo {
  id?: number
  userId?: number
  realName?: string
  birthDate?: string
  educationBackground?: string
  teachingExperience?: number
  specialties?: string
  subjects?: string
  subjectIds?: number[]  // 保留用于显示，但不允许修改
  hourlyRate?: number
  introduction?: string
  videoIntroUrl?: string
  gender?: string
  avatarUrl?: string
  availableTimeSlots?: {
    weekday: number
    timeSlots: string[]
  }[]
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
  // 初始状态设为false，等待initializeAuth验证后再设置正确状态
  const isLoggedIn = ref<boolean>(false)

  // 添加计算属性
  const isTeacher = computed(() => user.value?.userType === 'teacher')
  const isStudent = computed(() => user.value?.userType === 'student')
  const isAdmin = computed(() => user.value?.userType === 'admin')

  // API 基础配置 - 使用环境变量，生产环境使用相对路径
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD ? '' : 'http://localhost:8080')

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
      const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
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
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
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
        await fetch(`${API_BASE_URL}/api/auth/logout`, {
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
        console.log('找到存储的token，尝试验证有效性')

        // 验证token有效性并获取用户信息
        try {
          const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
            headers: {
              'Authorization': `Bearer ${storedToken}`,
            },
          })

          if (response.ok) {
            const result = await response.json()
            if (result.success && result.data) {
              console.log('Token验证成功，设置用户信息')
              user.value = {
                id: result.data.id,
                username: result.data.username,
                email: result.data.email,
                userType: result.data.authorities?.[0]?.authority?.replace('ROLE_', '').toLowerCase() || 'student',
                token: storedToken,
              }
              token.value = storedToken
              isLoggedIn.value = true

              // 额外拉取头像等资料，用于侧边栏回显
              await loadUserAvatar()
            } else {
              console.log('Token验证失败，清除登录状态')
              clearUser()
            }
          } else {
            console.log('Token无效，清除登录状态')
            // token无效，清除本地存储
            clearUser()
          }
        } catch (error) {
          console.error('验证token失败:', error)
          // 网络错误时，暂时保持登录状态但不设置用户信息
          // 这样可以避免网络问题导致用户被强制登出
          console.log('网络错误，暂时清除登录状态以确保UI正确显示')
          clearUser()
        }
      } else {
        console.log('没有找到存储的token，保持未登录状态')
        // 确保状态一致性
        clearUser()
      }
      console.log('认证初始化完成')
    } catch (error) {
      console.error('初始化认证时发生错误:', error)
      // 发生错误时清除状态
      clearUser()
    }
  }

  // 加载用户头像
  const loadUserAvatar = async () => {
    if (!user.value) return

    try {
      console.log('开始加载用户头像...')
      let profileResult: ApiResponse<StudentInfo | TeacherInfo> | null = null

      if (user.value.userType === 'student') {
        profileResult = await getStudentProfile()
      } else if (user.value.userType === 'teacher') {
        profileResult = await getTeacherProfile()
      }

      if (profileResult?.success && profileResult.data) {
        // 同步头像
        if (profileResult.data.avatarUrl) {
          user.value.avatarUrl = profileResult.data.avatarUrl
          console.log('用户头像加载成功:', profileResult.data.avatarUrl)
        } else {
          console.log('用户头像加载失败或无头像')
        }
        // 同步真实姓名
        if ('realName' in profileResult.data && profileResult.data.realName) {
          user.value.realName = (profileResult.data as StudentInfo | TeacherInfo).realName
        }
      }
    } catch (error) {
      console.error('加载用户头像时发生错误:', error)
    }
  }

  // 获取学生信息
  const getStudentProfile = async (): Promise<ApiResponse<StudentInfo>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/student/profile`, {
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
      const response = await fetch(`${API_BASE_URL}/api/student/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token.value}`,
        },
        body: JSON.stringify(data),
      })
      const result: ApiResponse<StudentInfo> = await response.json()
      if (result.success && result.data?.avatarUrl && user.value) {
        user.value.avatarUrl = result.data.avatarUrl
      }
      return result
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
      const response = await fetch(`${API_BASE_URL}/api/teacher/profile`, {
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
      const response = await fetch(`${API_BASE_URL}/api/teacher/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token.value}`,
        },
        body: JSON.stringify(data),
      })
      const result: ApiResponse<TeacherInfo> = await response.json()
      if (result.success && result.data?.avatarUrl && user.value) {
        user.value.avatarUrl = result.data.avatarUrl
      }
      return result
    } catch (error) {
      console.error('更新教师信息失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 获取科目列表
  const getSubjects = async (): Promise<ApiResponse<{id: number, name: string}[]>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/public/subjects/active`)
      return await response.json()
    } catch (error) {
      console.error('获取科目列表失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 修改密码
  const changePassword = async (data: PasswordChangeRequest): Promise<ApiResponse<string>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/user/change-password`, {
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

  // 更新用户基本信息
  const updateUserProfile = async (data: { birthDate: string }): Promise<ApiResponse<string>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/user/update-profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token.value}`,
        },
        body: JSON.stringify(data),
      })
      return await response.json()
    } catch (error) {
      console.error('更新用户基本信息失败:', error)
      return {
        success: false,
        message: '网络错误，请稍后重试',
      }
    }
  }

  // 管理员登录
  const adminLogin = async (loginData: LoginRequest): Promise<ApiResponse<User>> => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/admin/login`, {
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
    } catch (error: unknown) {
      console.error('管理员登录失败:', error)
      const message = error instanceof Error ? error.message : '网络错误，请稍后重试'
      throw new Error(message)
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
    loadUserAvatar,
    getStudentProfile,
    updateStudentProfile,
    getTeacherProfile,
    updateTeacherProfile,
    getSubjects,
    changePassword,
    updateUserProfile,
    adminLogin,
  }
})
