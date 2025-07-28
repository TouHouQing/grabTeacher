/**
 * 环境配置工具
 */

// 获取API基础URL
export const getApiBaseUrl = (): string => {
  return import.meta.env.VITE_API_BASE_URL || (import.meta.env.PROD ? '' : 'http://localhost:8080')
}

// 获取应用标题
export const getAppTitle = (): string => {
  return import.meta.env.VITE_APP_TITLE || '抢老师系统'
}

// 判断是否为开发环境
export const isDevelopment = (): boolean => {
  return import.meta.env.DEV
}

// 判断是否为生产环境
export const isProduction = (): boolean => {
  return import.meta.env.PROD
}

// 获取当前环境模式
export const getMode = (): string => {
  return import.meta.env.MODE
}

// 环境信息
export const envInfo = {
  apiBaseUrl: getApiBaseUrl(),
  appTitle: getAppTitle(),
  isDev: isDevelopment(),
  isProd: isProduction(),
  mode: getMode()
}

// 在开发环境下打印环境信息
if (isDevelopment()) {
  console.log('🌍 环境信息:', envInfo)
}
