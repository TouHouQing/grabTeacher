import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// API响应拦截器
export const handleApiResponse = async (response: Response) => {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))

    switch (response.status) {
      case 400:
        // 400错误通常包含具体的业务错误信息，直接显示
        ElMessage.error(errorData.message || '请求参数错误')
        break
      case 401:
        ElMessage.error('登录已过期，请重新登录')
        const userStore = useUserStore()
        userStore.clearUser()
        router.push('/login')
        break
      case 403:
        ElMessage.error('权限不足，无法执行此操作')
        break
      case 404:
        ElMessage.error('请求的资源不存在')
        break
      case 409:
        // 409冲突错误，通常是数据冲突
        ElMessage.error(errorData.message || '数据冲突，请刷新页面后重试')
        break
      case 422:
        // 422数据验证错误
        ElMessage.error(errorData.message || '数据验证失败，请检查输入内容')
        break
      case 500:
        ElMessage.error('服务器内部错误，请稍后重试')
        break
      case 502:
        ElMessage.error('网关错误，请稍后重试')
        break
      case 503:
        ElMessage.error('服务暂时不可用，请稍后重试')
        break
      default:
        ElMessage.error(errorData.message || `请求失败 (${response.status})`)
    }

    throw new Error(`HTTP ${response.status}: ${errorData.message || response.statusText}`)
  }

  return response.json()
}

// API错误处理
export const handleApiError = (error: any, customMessage?: string) => {
  console.error('API请求失败:', error)

  if (error.name === 'TypeError' && error.message.includes('fetch')) {
    ElMessage.error('网络连接失败，请检查网络设置')
  } else if (customMessage) {
    ElMessage.error(customMessage)
  } else {
    ElMessage.error('请求失败，请稍后重试')
  }

  throw error
}

// 统一的API请求方法
export const apiRequestWithInterceptor = async (endpoint: string, options: RequestInit = {}) => {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD ? '' : 'http://localhost:8080')
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
    return await handleApiResponse(response)
  } catch (error) {
    return handleApiError(error)
  }
}
