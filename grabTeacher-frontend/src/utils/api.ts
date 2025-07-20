// API基础配置
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 创建API请求函数
export const apiRequest = async (endpoint: string, options: RequestInit = {}) => {
  const url = `${API_BASE_URL}${endpoint}`

  const defaultHeaders = {
    'Content-Type': 'application/json',
  }

  // 从localStorage获取token
  const token = localStorage.getItem('token')
  if (token) {
    defaultHeaders['Authorization'] = `Bearer ${token}`
  }

  const config: RequestInit = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  }

  try {
    const response = await fetch(url, config)

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.json()
  } catch (error) {
    console.error('API请求失败:', error)
    throw error
  }
}

// 科目管理API
export const subjectAPI = {
  // 获取科目列表
  getList: (params: any) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/admin/subjects?${searchParams}`)
  },

  // 创建科目
  create: (data: any) => {
    return apiRequest('/api/admin/subjects', {
      method: 'POST',
      body: JSON.stringify(data)
    })
  },

  // 更新科目
  update: (id: number, data: any) => {
    return apiRequest(`/api/admin/subjects/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  },

  // 删除科目
  delete: (id: number) => {
    return apiRequest(`/api/admin/subjects/${id}`, {
      method: 'DELETE'
    })
  },

  // 更新科目状态
  updateStatus: (id: number, isActive: boolean) => {
    return apiRequest(`/api/admin/subjects/${id}/status?isActive=${isActive}`, {
      method: 'PATCH'
    })
  },

  // 获取活跃科目列表
  getActiveSubjects: () => {
    return apiRequest('/api/public/subjects/active')
  }
}

// 学生管理 API
export const studentAPI = {
  // 获取学生列表
  getList: (params: {
    page?: number
    size?: number
    keyword?: string
    gradeLevel?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/admin/students?${searchParams}`)
  },

  // 获取学生详情
  getById: (id: number) => apiRequest(`/api/admin/students/${id}`),

  // 添加学生
  create: (data: {
    realName: string
    gradeLevel?: string
    subjectsInterested?: string
    learningGoals?: string
    preferredTeachingStyle?: string
    budgetRange?: string
  }) => apiRequest('/api/admin/students', {
    method: 'POST',
    body: JSON.stringify(data)
  }),

  // 更新学生
  update: (id: number, data: {
    realName: string
    gradeLevel?: string
    subjectsInterested?: string
    learningGoals?: string
    preferredTeachingStyle?: string
    budgetRange?: string
  }) => apiRequest(`/api/admin/students/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  }),

  // 删除学生
  delete: (id: number) => apiRequest(`/api/admin/students/${id}`, {
    method: 'DELETE'
  })
}

// 教师管理 API
export const teacherAPI = {
  // 获取教师列表
  getList: (params: {
    page?: number
    size?: number
    keyword?: string
    subject?: string
    isVerified?: boolean
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/admin/teachers?${searchParams}`)
  },

  // 获取教师详情
  getById: (id: number) => apiRequest(`/api/admin/teachers/${id}`),

  // 添加教师
  create: (data: {
    realName: string
    educationBackground?: string
    teachingExperience?: number
    specialties?: string
    subjects?: string
    hourlyRate?: number
    introduction?: string
    videoIntroUrl?: string
  }) => apiRequest('/api/admin/teachers', {
    method: 'POST',
    body: JSON.stringify(data)
  }),

  // 更新教师
  update: (id: number, data: {
    realName: string
    educationBackground?: string
    teachingExperience?: number
    specialties?: string
    subjects?: string
    hourlyRate?: number
    introduction?: string
    videoIntroUrl?: string
  }) => apiRequest(`/api/admin/teachers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  }),

  // 删除教师
  delete: (id: number) => apiRequest(`/api/admin/teachers/${id}`, {
    method: 'DELETE'
  }),

  // 审核教师 - 使用请求体
  verify: (id: number, isVerified: boolean) =>
    apiRequest(`/api/admin/teachers/${id}/verify`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ isVerified: isVerified })
    })
}

// 管理员统计 API
export const adminAPI = {
  // 获取统计数据
  getStatistics: () => apiRequest('/api/admin/statistics')
}

// 课程管理 API
export const courseAPI = {
  // 获取课程列表（分页）
  getList: (params: {
    page?: number
    size?: number
    keyword?: string
    subjectId?: number
    teacherId?: number
    status?: string
    courseType?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/courses?${searchParams}`)
  },

  // 获取课程详情
  getById: (id: number) => apiRequest(`/api/courses/${id}`),

  // 创建课程
  create: (data: {
    teacherId?: number
    subjectId: number
    title: string
    description?: string
    courseType: string
    durationMinutes: number
    status?: string
  }) => apiRequest('/api/courses', {
    method: 'POST',
    body: JSON.stringify(data)
  }),

  // 更新课程
  update: (id: number, data: {
    teacherId?: number
    subjectId: number
    title: string
    description?: string
    courseType: string
    durationMinutes: number
    status?: string
  }) => apiRequest(`/api/courses/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  }),

  // 删除课程
  delete: (id: number) => apiRequest(`/api/courses/${id}`, {
    method: 'DELETE'
  }),

  // 获取活跃课程列表
  getActiveCourses: () => apiRequest('/api/courses/active'),

  // 获取教师的课程列表
  getTeacherCourses: (teacherId: number) => apiRequest(`/api/courses/teacher/${teacherId}`),

  // 获取当前教师的课程列表
  getMyCourses: () => apiRequest('/api/courses/my-courses'),

  // 更新课程状态
  updateStatus: (id: number, status: string) => apiRequest(`/api/courses/${id}/status?status=${status}`, {
    method: 'PATCH'
  })
}
