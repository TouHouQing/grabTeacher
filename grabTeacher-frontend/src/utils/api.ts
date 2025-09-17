// API基础配置 - 使用环境变量，生产环境使用相对路径
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD ? '' : 'http://localhost:8080')

console.log('当前API环境:', API_BASE_URL)

// 自定义错误类型，包含响应数据
interface ApiError extends Error {
  response?: any
}

// 扩展RequestInit接口以支持data参数
interface ApiRequestOptions extends RequestInit {
  data?: any
  params?: Record<string, any>
}

// 创建API请求函数
export const apiRequest = async (endpoint: string, options: ApiRequestOptions = {}) => {
  const url = `${API_BASE_URL}${endpoint}`

  const defaultHeaders = {
    'Content-Type': 'application/json',
  }

  // 从localStorage获取token
  const token = localStorage.getItem('token')
  if (token) {
    defaultHeaders['Authorization'] = `Bearer ${token}`
    console.log('API请求包含token:', token.substring(0, 20) + '...')
  } else {
    console.log('API请求没有token')
  }

  // 处理data参数
  const { data, params, ...restOptions } = options
  const config: RequestInit = {
    ...restOptions,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  }

  // 如果有data参数，将其转换为JSON字符串并设置为body
  if (data) {
    config.body = JSON.stringify(data)
  }

  // 如果有params参数，将其附加到URL
  let finalUrl = url
  if (params) {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
        searchParams.append(key, params[key].toString())
      }
    })
    const paramString = searchParams.toString()
    if (paramString) {
      finalUrl += (url.includes('?') ? '&' : '?') + paramString
    }
  }

  console.log('API请求:', {
    url: finalUrl,
    method: config.method || 'GET',
    headers: config.headers
  })

  try {
    const response = await fetch(finalUrl, config)

    console.log('API响应状态:', response.status)

    // 先解析响应体
    const responseData = await response.json()

    if (!response.ok) {
      if (response.status === 401) {
        console.error('401错误 - 认证失败，可能token无效或过期')
        // 如果是401错误，清除本地token
        localStorage.removeItem('token')
      }

      // 如果响应体包含错误信息，抛出包含具体错误信息的错误
      const errorMessage = responseData.message || `HTTP error! status: ${response.status}`
      const error = new Error(errorMessage) as ApiError
      error.response = responseData
      throw error
    }

    return responseData
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

// 授课地点管理 API（仅管理员）
export const teachingLocationAPI = {
  // 获取授课地点列表
  getList: (params: any) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null && v !== '') searchParams.append(key, v.toString())
    })
    return apiRequest(`/api/admin/teaching-locations?${searchParams}`)
  },
  // 创建授课地点
  create: (data: { name: string; address: string; isActive?: boolean; sortOrder?: number }) =>
    apiRequest('/api/admin/teaching-locations', { method: 'POST', body: JSON.stringify(data) }),
  // 更新授课地点
  update: (id: number, data: { name: string; address: string; isActive?: boolean; sortOrder?: number }) =>
    apiRequest(`/api/admin/teaching-locations/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  // 删除授课地点
  delete: (id: number) => apiRequest(`/api/admin/teaching-locations/${id}`, { method: 'DELETE' }),
  // 更新启用状态
  updateStatus: (id: number, isActive: boolean) =>
    apiRequest(`/api/admin/teaching-locations/${id}/status?isActive=${isActive}`, { method: 'PATCH' })
}

// 公开：授课地点 API（学生端使用）
export const publicTeachingLocationAPI = {
  getActive: () => apiRequest('/api/public/teaching-locations/active')
}


// 年级管理 API（仅管理员）
export const gradeAPI = {
  // 获取年级列表
  getList: (params: any) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null && v !== '') searchParams.append(key, v.toString())
    })
    return apiRequest(`/api/admin/grades?${searchParams}`)
  },
  // 创建年级
  create: (data: { name: string; isActive?: boolean; sortOrder?: number }) =>
    apiRequest('/api/admin/grades', { method: 'POST', body: JSON.stringify(data) }),
  // 更新年级
  update: (id: number, data: { name?: string; isActive?: boolean; sortOrder?: number }) =>
    apiRequest(`/api/admin/grades/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  // 删除年级
  delete: (id: number) => apiRequest(`/api/admin/grades/${id}`, { method: 'DELETE' }),
  // 更新启用状态
  updateStatus: (id: number, isActive: boolean) =>
    apiRequest(`/api/admin/grades/${id}/status?isActive=${isActive}`, { method: 'PATCH' })
}



// 学生管理 API
export const studentAPI = {
  // 获取学生列表
  getList: (params: {
    page?: number
    size?: number
    keyword?: string
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
    username?: string
    email?: string
    phone?: string
    subjectsInterested?: string
    subjectIds?: number[]
    learningGoals?: string
    preferredTeachingStyle?: string
    budgetRange?: string
    gender?: string
    avatarUrl?: string
    balance?: number
    trialTimes?: number
  }) => apiRequest('/api/admin/students', {
    method: 'POST',
    body: JSON.stringify(data)
  }),

  // 更新学生
  update: (id: number, data: {
    realName: string
    username?: string
    email?: string
    phone?: string
    subjectsInterested?: string
    subjectIds?: number[]
    learningGoals?: string
    preferredTeachingStyle?: string
    budgetRange?: string
    gender?: string
    avatarUrl?: string
    balance?: number
    trialTimes?: number
  }) => apiRequest(`/api/admin/students/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  }),

  // 删除学生
  delete: (id: number) => apiRequest(`/api/admin/students/${id}`, {
    method: 'DELETE'
  }),

  // 获取学生统计数据
  getStatistics: () => apiRequest('/api/student/statistics'),

  // 获取学生个人资料
  getProfile: () => apiRequest('/api/student/profile'),

  // 创建课程评价
  createCourseEvaluation: (data: {
    teacherId: number
    studentId: number
    courseId: number
    teacherName: string
    studentName: string
    courseName: string
    rating: number
    studentComment: string
  }) => apiRequest('/api/student/course-evaluations', {
    method: 'POST',
    data
  }),

  // 检查课程评价状态
  checkCourseEvaluationStatus: (studentId: number, courseId: number) =>
    apiRequest(`/api/student/course-evaluations/check/${studentId}/${courseId}`)
}

// 教师管理 API
export const teacherAPI = {
  // 获取教师某月基础段状态矩阵（含忙碌/试听/不可用等）
  getMonthlyCalendar: (teacherId: number, year: number, month: number) =>
    apiRequest(`/api/calendar/teacher/${teacherId}/month?year=${year}&month=${month}`),

  // 教师按日历设置可上课时间（仅允许选择6个基础段）
  setDailyAvailability: (data: { teacherId?: number; overwrite?: boolean; items: Array<{ date: string; timeSlots: string[] }> }) =>
    apiRequest('/api/available-time/daily/teacher/set', {
      method: 'POST',
      body: JSON.stringify(data)
    }),

  // 管理员按日历设置某教师可上课时间
  setDailyAvailabilityByAdmin: (data: { teacherId: number; overwrite?: boolean; items: Array<{ date: string; timeSlots: string[] }> }) =>
    apiRequest('/api/available-time/daily/admin/set', {
      method: 'POST',
      body: JSON.stringify(data)
    }),

  // 查询某教师在日期范围内的日历可上课时间
  getDailyAvailability: (teacherId: number, startDate: string, endDate: string) =>
    apiRequest(`/api/available-time/daily/teacher/${teacherId}?startDate=${startDate}&endDate=${endDate}`),
  // 获取教师个人资料
  getProfile: () => apiRequest('/api/teacher/profile'),
  // 获取教师列表
  getList: (params: {
    page?: number
    size?: number
    keyword?: string
    subject?: string
    gender?: string
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
    username?: string
    email?: string
    phone?: string
    educationBackground?: string
    teachingExperience?: number
    specialties?: string
    subjectIds?: number[]
    introduction?: string
    videoIntroUrl?: string
    gender?: string
    level?: string
    availableTimeSlots?: any[]
    avatarUrl?: string
    // 授课地点
    supportsOnline?: boolean
    teachingLocationIds?: number[]
    hourlyRateText?: string
  }) => apiRequest('/api/admin/teachers', {
    method: 'POST',
    body: JSON.stringify(data)
  }),

  // 更新教师
  update: (id: number, data: {
    realName: string
    username?: string
    email?: string
    phone?: string
    educationBackground?: string
    teachingExperience?: number
    specialties?: string
    subjectIds?: number[]
    introduction?: string
    videoIntroUrl?: string
    gender?: string
    level?: string
    availableTimeSlots?: any[]
    // 授课地点
    supportsOnline?: boolean
    teachingLocationIds?: number[]
    hourlyRateText?: string
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
    }),

  // 更新教师认证状态
  updateVerification: (id: number, isVerified: boolean) =>
    apiRequest(`/api/admin/teachers/${id}/verify`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ isVerified: isVerified })
    }),

  // 设置教师为精选教师
  setFeatured: (id: number, isFeatured: boolean) =>
    apiRequest(`/api/admin/teachers/${id}/featured`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ isFeatured: isFeatured })
    }),

  // 管理员：批量更新某教师的一对一课程的时薪与本月课时（差值入账）
  adminUpdateOneOnOneMetrics: (teacherId: number, items: Array<{ courseId: number; teacherHourlyRate?: number | null; currentHours?: number | null }>) =>
    apiRequest(`/api/admin/teachers/${teacherId}/courses/one-on-one-metrics`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(items || [])
    }),


  // 匹配教师
  matchTeachers: (data: {
    subject?: string
    grade?: string
    teacherLevel?: string
    preferredTime?: string
    preferredDateStart?: string
    preferredDateEnd?: string
    preferredWeekdays?: number[]
    preferredTimeSlots?: string[]
    preferredGender?: string
    limit?: number
  }) => apiRequest('/api/teacher/match', {
    method: 'POST',
    body: JSON.stringify(data)
  }),


  // 批量获取教师科目ID映射（管理员）
  getSubjectsByTeacherIds: (ids: number[]) => apiRequest('/api/admin/teachers/subjects/batch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(ids)
  }),

  // 获取教师详情（公开接口）
  getDetail: (id: number) => apiRequest(`/api/teacher/${id}`),

  // 获取教师的公开课程列表（供学生预约时查看）
  getPublicCourses: (teacherId: number) => apiRequest(`/api/courses/public/teacher/${teacherId}`),

  // 获取教师的公开课表（供学生查看）
  getPublicSchedule: (teacherId: number, params: {
    startDate: string
    endDate: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/teacher/${teacherId}/schedule?${searchParams}`)
  },


  // 获取某日的可用性信息（包括试听课和正式课）
  getDayAvailability: (teacherId: number, date: string, segment?: string) => {
    const params = new URLSearchParams({ teacherId: teacherId.toString(), date })
    if (segment) params.append('segment', segment)
    return apiRequest(`/api/booking/availability/day?${params}`)
  },

  // 获取公开教师列表（无需认证，供主页使用）
  getPublicList: (params: {
    page?: number
    size?: number
    subject?: string
    keyword?: string
    realName?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/teacher/list?${searchParams}`)
  },

  // 获取精选教师列表（不分页，供首页使用）
  getFeaturedList: (params?: { subject?: string; keyword?: string }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null) searchParams.append(key, v.toString())
    })
    const query = searchParams.toString()
    return apiRequest(`/api/teacher/featured${query ? `?${query}` : ''}`)
  },

  // 获取教师统计数据
  getStatistics: () => apiRequest('/api/teacher/statistics'),

  // 获取当前教师的课时明细
  getMyHourDetails: (params: { page?: number; size?: number; transactionType?: number }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null && v !== '') searchParams.append(key, v.toString())
    })
    const query = searchParams.toString()
    return apiRequest(`/api/teacher/hour-details/my${query ? `?${query}` : ''}`)
  },

  // 获取教师课时详情统计
  getHourDetails: () => apiRequest('/api/teacher/hour-details/summary'),

  // 获取教师上课记录
  getClassRecords: (params: {
    page?: number
    size?: number
    year?: number
    month?: number
    studentName?: string
    courseName?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null && v !== '') searchParams.append(key, v.toString())
    })
    const query = searchParams.toString()
    return apiRequest(`/api/teacher/class-records${query ? `?${query}` : ''}`)
  },

}

// 管理员统计 API
export const adminAPI = {
  // 获取统计数据
  getStatistics: () => apiRequest('/api/admin/statistics')
}

// 预约管理 API
export const bookingAPI = {
  // 创建预约申请（学生）
  createRequest: (data: {
    teacherId: number
    courseId?: number
    bookingType: 'single' | 'recurring' | 'calendar'
    requestedDate?: string
    requestedStartTime?: string
    requestedEndTime?: string
    recurringWeekdays?: number[]
    recurringTimeSlots?: string[]
    startDate?: string
    endDate?: string
    totalTimes?: number
    studentRequirements?: string
    grade?: string
    // 授课地点选择
    teachingLocationId?: number // 线下地点ID
    teachingLocation?: string   // 线上时传 '线上'
    trial?: boolean // 兼容旧字段
    isTrial?: boolean // 后端DTO字段名
    trialDurationMinutes?: number
    selectedDurationMinutes?: number
    // 日历预约所需：每次会话明确的日期+时间
    selectedSessions?: Array<{ date: string; startTime: string; endTime: string }>
  }) => apiRequest('/api/booking/request', {
    method: 'POST',
    data: data
  }),

  // 管理员审批预约申请
  approve: (id: number, data: {
    status: 'approved' | 'rejected'
    teacherReply?: string
    adminNotes?: string
  }) => apiRequest(`/api/booking/${id}/approve`, {
    method: 'PUT',
    body: JSON.stringify(data)
  }),

  // 学生取消预约申请
  cancel: (id: number) => apiRequest(`/api/booking/${id}/cancel`, {
    method: 'PUT'
  }),

  // 获取预约申请详情
  getById: (id: number) => apiRequest(`/api/booking/${id}`),

  // 获取学生的预约申请列表
  getStudentRequests: (params: {
    page?: number
    size?: number
    status?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/booking/student/my?${searchParams}`)
  },

  // 获取教师的预约申请列表
  getTeacherRequests: (params: {
    page?: number
    size?: number
    status?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/booking/teacher/my?${searchParams}`)
  },

  // 获取教师的课程安排列表
  getTeacherSchedules: (params: {
    startDate: string
    endDate: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/booking/teacher/schedules?${searchParams}`)
  },

  // 获取学生的课程安排列表
  getStudentSchedules: (params: {
    startDate: string
    endDate: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/booking/student/schedules?${searchParams}`)
  },

  // 获取学生的所有课程安排列表（不限日期范围）
  getAllStudentSchedules: () => {
    return apiRequest('/api/booking/student/schedules/all')
  },

  // 检查免费试听资格
  checkTrialEligibility: () => apiRequest('/api/booking/trial/check'),

  // 取消试听课节（教师/学生端）：删除该节课并恢复学生试听次数
  cancelTrialSchedule: (scheduleId: number) => apiRequest(`/api/booking/trial/schedule/${scheduleId}`, { method: 'DELETE' }),

  // 管理员获取预约申请列表
  getAdminRequests: (params: {
    page?: number
    size?: number
    status?: string
    keyword?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/booking/admin/list?${searchParams}`)
  },

  // 获取某日的可用性信息（包括试听课和正式课）
  getDayAvailability: (teacherId: number, date: string, segment?: string) => {
    const params = new URLSearchParams({ teacherId: teacherId.toString(), date })
    if (segment) params.append('segment', segment)
    return apiRequest(`/api/booking/availability/day?${params}`)
  }
  ,

  // 学生端：我的课程 V2（后端聚合，直接读真实课表）
  getStudentCoursesV2: () => apiRequest('/api/booking/student/my-courses/v2')

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
    gender?: string
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
    gender?: string
    price?: number | null
    teacherHourlyRate?: number | null
    startDate?: string
    endDate?: string
    personLimit?: number | null
    courseTimeSlots?: { weekday: number; timeSlots: string[] }[]
    imageUrl?: string
    courseLocation?: string
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
    gender?: string
    price?: number | null
    teacherHourlyRate?: number | null

    startDate?: string
    endDate?: string
    personLimit?: number | null
    courseTimeSlots?: Array<{ weekday: number; timeSlots: string[] }>
    imageUrl?: string
    courseLocation?: string
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

  // 更新课程状态
  updateStatus: (id: number, status: string) => apiRequest(`/api/courses/${id}/status?status=${status}`, {
    method: 'PATCH'
  }),

  // 获取公开课程列表（最新课程页面使用）
  getPublicLatestCourses: (params: {
    page?: number
    size?: number
    subjectId?: number
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/public/courses/latest?${searchParams}`)
  },

  // 获取公开课程列表
  getPublicCourses: (params: {
    page?: number
    size?: number
    keyword?: string
    subjectId?: number
    teacherId?: number
    courseType?: string
    courseLocation?: string
    teacherLevel?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/public/courses?${searchParams}`)
  },

  // 获取活跃课程列表（公开接口）
  getPublicActiveCourses: () => apiRequest('/api/public/courses/active'),

  // 获取所有精选课程列表（不分页，用于首页滚动展示）
  getAllFeaturedCourses: () => apiRequest('/api/public/courses/featured'),

  // 获取课程详情（公开接口）
  getPublicCourseById: (id: number) => apiRequest(`/api/public/courses/${id}`),

  // 管理员获取课程列表
  getCourseList: (params: {
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

  // 获取精选课程列表
  getFeaturedCourses: (params: {
    page?: number
    size?: number
    subjectId?: number
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/admin/featured-courses?${searchParams}`)
  },

  // 获取精选课程ID列表
  getFeaturedCourseIds: () => apiRequest('/api/admin/featured-courses/ids'),

  // 设置单个课程为精选课程
  setCourseAsFeatured: (courseId: number, featured: boolean) =>
    apiRequest(`/api/admin/courses/${courseId}/featured`, {
      method: 'PUT',
      body: JSON.stringify({ featured })
    }),

  // 批量设置精选课程
  batchSetFeaturedCourses: (courseIds: number[], featured: boolean) =>
    apiRequest('/api/admin/courses/batch-featured', {
      method: 'PUT',
      body: JSON.stringify({ courseIds, featured })
    })
}

// 调课管理 API
export const rescheduleAPI = {
  // 创建调课申请（学生操作）
  createRequest: (data: {
    scheduleId: number
    requestType: 'reschedule' | 'cancel'
    newDate?: string
    newStartTime?: string
    newEndTime?: string
    reason: string
    urgencyLevel?: 'low' | 'medium' | 'high'
    advanceNoticeHours?: number
    notes?: string
  }) => apiRequest('/api/reschedule/request', {
    method: 'POST',
    body: JSON.stringify(data)
  }),

  // 创建调课申请（教师操作）
  createTeacherRequest: (data: {
    scheduleId: number
    requestType: 'reschedule' | 'cancel'
    newDate?: string
    newStartTime?: string
    newEndTime?: string
    reason: string
    urgencyLevel?: 'low' | 'medium' | 'high'
    advanceNoticeHours?: number
    notes?: string
    candidateSessions?: Array<{ date: string; startTime: string; endTime: string }>
  }) => apiRequest('/api/reschedule/teacher/request', {
    method: 'POST',
    body: JSON.stringify(data)
  }),

  // 教师审批调课申请
  approve: (id: number, data: {
    status: 'approved' | 'rejected'
    reviewNotes?: string
    compensationAmount?: number
  }) => apiRequest(`/api/reschedule/${id}/approve`, {
    method: 'PUT',
    body: JSON.stringify(data)
  }),

  // 取消调课申请（学生操作）
  cancel: (id: number) => apiRequest(`/api/reschedule/${id}/cancel`, {
    method: 'PUT'
  }),

  // 获取学生的调课申请列表
  getStudentRequests: (params: {
    page?: number
    size?: number
    status?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/reschedule/student/requests?${searchParams}`)
  },

  // 根据ID获取调课申请详情
  getById: (id: number) => apiRequest(`/api/reschedule/${id}`),

  // 检查是否可以申请调课
  canApply: (scheduleId: number) => apiRequest(`/api/reschedule/can-apply/${scheduleId}`),

  // 检查调课时间冲突
  checkTimeConflict: (scheduleId: number, newDate: string, newStartTime: string, newEndTime: string) => {
    const params = new URLSearchParams({
      newDate,
      newStartTime,
      newEndTime
    })
    return apiRequest(`/api/reschedule/check-conflict/${scheduleId}?${params}`)
  },

  // 教师端：检查调课时间冲突
  checkTeacherTimeConflict: (scheduleId: number, newDate: string, newStartTime: string, newEndTime: string) => {
    const params = new URLSearchParams({
      newDate,
      newStartTime,
      newEndTime
    })
    return apiRequest(`/api/reschedule/teacher/check-conflict/${scheduleId}?${params}`)
  },

  // 管理员：获取所有调课申请列表
  getAdminRequests: (params: {
    page?: number
    size?: number
    status?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null) {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/reschedule/admin/requests?${searchParams}`)
  },

  // 教师：获取调课记录列表（含筛选）
  getTeacherRequests: (params: {
    page?: number
    size?: number
    status?: string
    year?: number
    month?: number
    requestType?: 'reschedule' | 'cancel'
    applicantType?: 'student' | 'teacher'
    applicantName?: string
    courseName?: string
  }) => {
    const sp = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null && v !== '') sp.append(key, v.toString())
    })
    return apiRequest(`/api/reschedule/teacher/requests?${sp}`)
  },

  // 管理员：审批调课申请
  adminApprove: (id: number, data: {
    status: 'approved' | 'rejected'
    reviewNotes?: string
    selectedNewDate?: string
    selectedNewStartTime?: string
    selectedNewEndTime?: string
  }) => apiRequest(`/api/reschedule/${id}/approve`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

// 学生端 停课管理 API
export const suspensionAPI = {
  createRequest: (data: { enrollmentId: number; startDate: string; endDate: string; reason?: string }) =>
    apiRequest('/api/suspension/request', { method: 'POST', body: JSON.stringify(data) }),

  getStudentRequests: (params: { page?: number; size?: number; status?: string }) => {
    const sp = new URLSearchParams()
    Object.keys(params || {}).forEach(k => {
      const v: any = (params as any)[k]
      if (v !== undefined && v !== null && v !== '') sp.append(k, v.toString())
    })
    return apiRequest(`/api/suspension/student/requests?${sp}`)
  },

  // 管理端
  getAdminRequests: (params: { page?: number; size?: number; status?: string }) => {
    const sp = new URLSearchParams()
    Object.keys(params || {}).forEach(k => {
      const v: any = (params as any)[k]
      if (v !== undefined && v !== null && v !== '') sp.append(k, v.toString())
    })
    return apiRequest(`/api/suspension/admin/requests?${sp}`)
  },
  adminApprove: (id: number, data: { status: 'approved' | 'rejected'; reviewNotes: string }) =>
    apiRequest(`/api/suspension/${id}/approve`, { method: 'PUT', body: JSON.stringify(data) })
}

// 报名查询 API（用于展示已停课课程）
export const enrollmentAPI = {
  getStudentSuspended: () => apiRequest('/api/enrollments/student/suspended'),
  getByBookingRequestId: (bookingRequestId: number) => apiRequest(`/api/enrollments/by-booking/${bookingRequestId}`),
  enrollCourse: (courseId: number) => apiRequest(`/api/enrollments/course/${courseId}/enroll`, { method: 'POST' })
}



// 留学咨询 API
export const studyAbroadAPI = {
  // 公开：获取启用的国家/阶段/项目
  getCountries: () => apiRequest('/api/public/study-abroad/countries'),
  getStages: () => apiRequest('/api/public/study-abroad/stages'),
  getPrograms: (params: { limit?: number; countryId?: number; stageId?: number }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null) searchParams.append(key, v.toString())
    })
    const query = searchParams.toString()
    return apiRequest(`/api/public/study-abroad/programs${query ? `?${query}` : ''}`)
  },
  // 公开：启用项目分页列表（含 total）
  getPublicProgramsPaged: (params: { page?: number; size?: number; countryId?: number; stageId?: number }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null) searchParams.append(key, v.toString())
    })
    return apiRequest(`/api/public/study-abroad/programs/page?${searchParams}`)
  },

  // 管理端：国家
  adminListCountries: (params: { page?: number; size?: number; keyword?: string; isActive?: boolean }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null) searchParams.append(key, v.toString())
    })
    return apiRequest(`/api/admin/study-abroad/countries?${searchParams}`)
  },
  adminCreateCountry: (data: { countryName: string; sortOrder?: number; isActive?: boolean }) =>
    apiRequest('/api/admin/study-abroad/countries', { method: 'POST', body: JSON.stringify(data) }),
  adminUpdateCountry: (id: number, data: { countryName: string; sortOrder?: number; isActive?: boolean }) =>
    apiRequest(`/api/admin/study-abroad/countries/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  adminDeleteCountry: (id: number) =>
    apiRequest(`/api/admin/study-abroad/countries/${id}`, { method: 'DELETE' }),
  adminUpdateCountryStatus: (id: number, isActive: boolean) =>
    apiRequest(`/api/admin/study-abroad/countries/${id}/status?isActive=${isActive}`, { method: 'PATCH' }),

  // 管理端：阶段
  adminListStages: (params: { page?: number; size?: number; keyword?: string; isActive?: boolean }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null) searchParams.append(key, v.toString())
    })
    return apiRequest(`/api/admin/study-abroad/stages?${searchParams}`)
  },
  adminCreateStage: (data: { stageName: string; sortOrder?: number; isActive?: boolean }) =>
    apiRequest('/api/admin/study-abroad/stages', { method: 'POST', body: JSON.stringify(data) }),
  adminUpdateStage: (id: number, data: { stageName: string; sortOrder?: number; isActive?: boolean }) =>
    apiRequest(`/api/admin/study-abroad/stages/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  adminDeleteStage: (id: number) =>
    apiRequest(`/api/admin/study-abroad/stages/${id}`, { method: 'DELETE' }),
  adminUpdateStageStatus: (id: number, isActive: boolean) =>
    apiRequest(`/api/admin/study-abroad/stages/${id}/status?isActive=${isActive}`, { method: 'PATCH' }),

  // 管理端：项目
  adminListPrograms: (params: { page?: number; size?: number; keyword?: string; isActive?: boolean; countryId?: number; stageId?: number; isHot?: boolean }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null) searchParams.append(key, v.toString())
    })
    return apiRequest(`/api/admin/study-abroad/programs?${searchParams}`)
  },
  adminCreateProgram: (data: { title: string; countryId: number; stageId: number; description?: string; imageUrl?: string; tags?: string; hot?: boolean; sortOrder?: number; active?: boolean }) =>
    apiRequest('/api/admin/study-abroad/programs', { method: 'POST', body: JSON.stringify(data) }),
  adminUpdateProgram: (id: number, data: { title: string; countryId: number; stageId: number; description?: string; imageUrl?: string; tags?: string; hot?: boolean; sortOrder?: number; active?: boolean }) =>
    apiRequest(`/api/admin/study-abroad/programs/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  adminDeleteProgram: (id: number) =>
    apiRequest(`/api/admin/study-abroad/programs/${id}`, { method: 'DELETE' }),
  adminUpdateProgramStatus: (id: number, isActive: boolean) =>
    apiRequest(`/api/admin/study-abroad/programs/${id}/status?isActive=${isActive}`, { method: 'PATCH' }),
  adminUpdateProgramFlags: (id: number, isHot?: boolean, isFeatured?: boolean) => {
    const params = new URLSearchParams()
    if (isHot !== undefined && isHot !== null) params.append('isHot', String(isHot))
    return apiRequest(`/api/admin/study-abroad/programs/${id}/flags?${params}`, { method: 'PATCH' })
  }
}

// 教师招聘 API
export const adminPublicAPI = {
  getContacts: () => apiRequest('/api/public/admins/contacts')
}

// 课程评价（公开）API
export const evaluationAPI = {
  listPublic: (params: { page?: number; size?: number; teacherId?: number; courseId?: number; minRating?: number; teacherName?: string; courseName?: string }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null && v !== '') searchParams.append(key, v.toString())
    })
    const query = searchParams.toString()
    return apiRequest(`/api/public/course-evaluations${query ? `?${query}` : ''}`)
  }
}

export const jobPostAPI = {
  // 公开列表
  list: (params: { page?: number; size?: number; subjectId?: number }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null) searchParams.append(key, v.toString())
    })
    const query = searchParams.toString()
    return apiRequest(`/api/job-posts/public/list${query ? `?${query}` : ''}`)
  },
  // 公开详情（fast JSON）
  getDetailFast: (id: number) => apiRequest(`/api/job-posts/public/${id}/fast`),

  // 管理端：分页列表
  adminList: (params: { page?: number; size?: number; subjectId?: number; status?: string; keyword?: string; createdStart?: string; createdEnd?: string; includeDeleted?: boolean }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null) searchParams.append(key, v.toString())
    })
    return apiRequest(`/api/job-posts/admin/list?${searchParams}`)
  },
  // 管理端：详情
  adminGetById: (id: number) => apiRequest(`/api/job-posts/admin/${id}`),
  // 创建
  create: (data: { title: string; introduction?: string; subjectIds: number[]; tags?: string[]; status?: string; priority?: number }) =>
    apiRequest('/api/job-posts', { method: 'POST', body: JSON.stringify(data) }),
  // 更新
  update: (id: number, data: { title?: string; introduction?: string; subjectIds?: number[]; tags?: string[]; status?: string; priority?: number }) =>
    apiRequest(`/api/job-posts/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  // 删除
  delete: (id: number) => apiRequest(`/api/job-posts/${id}`, { method: 'DELETE' }),
  // 批量删除
  batchDelete: (ids: number[]) => apiRequest('/api/job-posts/batch-delete', { method: 'POST', body: JSON.stringify(ids) })
}

// 文件上传 API
export const fileAPI = {
  // 单次直传：获取预签名URL并PUT直传
  presignAndPut: async (file: File, module = 'course-cover', targetUserId?: number) => {
    const token = localStorage.getItem('token')
    const params: Record<string, string> = {
      module,
      filename: file.name,
      contentType: file.type || 'application/octet-stream'
    }

    // 如果提供了目标用户ID，添加到参数中
    if (targetUserId !== undefined) {
      params.targetUserId = targetUserId.toString()
    }

    const qs = new URLSearchParams(params)
    const presignUrl = `${API_BASE_URL}/api/file/presign?${qs}`
    const presignRes = await fetch(presignUrl, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined
    })
    const presignData = await presignRes.json()
    if (!presignRes.ok || !presignData?.success) {
      const msg = presignData?.message || `获取预签名URL失败(${presignRes.status})`
      throw new Error(msg)
    }
    const signedUrl: string = presignData.data

    // 直传到 OSS（PUT）
    const putRes = await fetch(signedUrl, {
      method: 'PUT',
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
      body: file
    })
    if (!putRes.ok) {
      throw new Error(`直传OSS失败(${putRes.status})`)
    }
    // 返回最终可访问URL（去掉查询串即可）
    return signedUrl.split('?')[0]
  }
}

// 余额交易记录 API
export const balanceTransactionAPI = {
  // 获取交易记录列表
  getList: (params: {
    page?: number
    size?: number
    userId?: string
    name?: string
    transactionType?: string
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
        searchParams.append(key, params[key].toString())
      }
    })
    return apiRequest(`/api/admin/balance-transactions?${searchParams}`)
  }
}

// 学生端：我的余额明细
export const myBalanceTransactionAPI = {
  getList: (params: { page?: number; size?: number; transactionType?: string }) => {
    const sp = new URLSearchParams()
    Object.keys(params || {}).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null && v !== '') sp.append(key, v.toString())
    })
    const q = sp.toString()
    return apiRequest(`/api/student/balance-transactions/my${q ? `?${q}` : ''}`)
  }
}

// 教师课时明细 API
export const teacherHourDetailsAPI = {
  getList: (params: {
    page?: number
    size?: number
    userId?: string
    name?: string
    transactionType?: number // 1增加 0减少
  }) => {
    const searchParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      const v: any = (params as any)[key]
      if (v !== undefined && v !== null && v !== '') {
        searchParams.append(key, v.toString())
      }
    })
    return apiRequest(`/api/admin/teacher-hour-details?${searchParams}`)
  }
}

// 级别价格 API（仅管理员）
export const teacherLevelAPI = {
  list: () => apiRequest('/api/admin/teacher-levels'),
  create: (data: { name: string; isActive?: boolean; sortOrder?: number }) =>
    apiRequest('/api/admin/teacher-levels', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: { name?: string; isActive?: boolean; sortOrder?: number }) =>
    apiRequest(`/api/admin/teacher-levels/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id: number) => apiRequest(`/api/admin/teacher-levels/${id}`, { method: 'DELETE' })
}

// 公开教师级别 API（学生端使用）

// 公开年级 API（学生端使用）
export const publicGradeAPI = {
  list: () => apiRequest('/api/public/grades')
}

export const publicTeacherLevelAPI = {
  list: () => apiRequest('/api/public/teacher-levels')
}


// lesson-grades API
export const lessonGradeAPI = {
  create: (data: { scheduleId: number; studentId: number; score?: number; teacherComment?: string }) =>
    apiRequest('/api/lesson-grades', { method: 'POST', body: JSON.stringify(data) }),
  update: (data: { id: number; score?: number; teacherComment?: string }) =>
    apiRequest('/api/lesson-grades', { method: 'PUT', body: JSON.stringify(data) }),
  getByScheduleAndStudent: (scheduleId: number, studentId: number) =>
    apiRequest(`/api/lesson-grades/schedule/${scheduleId}/student/${studentId}`),
  getStudentSubjectChartData: (studentId: number, subjectName: string) =>
    apiRequest(`/api/lesson-grades/student/${studentId}/subject/${encodeURIComponent(subjectName)}/chart-data`),
  checkPermission: (scheduleId: number) =>
    apiRequest(`/api/lesson-grades/check-permission/${scheduleId}`)
}
