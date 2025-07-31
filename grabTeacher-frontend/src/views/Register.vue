<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getApiBaseUrl } from '@/utils/env'

// 简化注册请求接口
interface ExtendedRegisterRequest {
  username: string
  email: string
  password: string
  confirmPassword: string
  userType: 'student' | 'teacher' | 'admin'
  realName: string
  phone?: string
  birthDate: string // 出生年月
}

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const registerForm = reactive<ExtendedRegisterRequest>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  userType: 'student',
  realName: '',
  phone: '',
  birthDate: '' // 出生年月
})





// 检查用户名是否可用
const checkUsername = async (username: string): Promise<boolean> => {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/auth/check-username?username=${encodeURIComponent(username)}`)
    const result = await response.json()
    return result.success && result.data
  } catch (error) {
    console.error('检查用户名失败:', error)
    return false
  }
}

// 检查邮箱是否可用
const checkEmail = async (email: string): Promise<boolean> => {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/auth/check-email?email=${encodeURIComponent(email)}`)
    const result = await response.json()
    return result.success && result.data
  } catch (error) {
    console.error('检查邮箱失败:', error)
    return false
  }
}

const validateForm = async (): Promise<boolean> => {
  if (!registerForm.username || registerForm.username.length < 3 || registerForm.username.length > 50) {
    errorMessage.value = '用户名长度必须在3-50个字符之间'
    return false
  }

  // 检查用户名是否已被使用
  const usernameAvailable = await checkUsername(registerForm.username)
  if (!usernameAvailable) {
    errorMessage.value = '用户名已被使用，请选择其他用户名'
    return false
  }

  if (!registerForm.email || !registerForm.email.includes('@')) {
    errorMessage.value = '请输入有效的邮箱地址'
    return false
  }

  // 检查邮箱是否已被注册
  const emailAvailable = await checkEmail(registerForm.email)
  if (!emailAvailable) {
    errorMessage.value = '邮箱已被注册，请使用其他邮箱'
    return false
  }

  if (!registerForm.realName) {
    errorMessage.value = '请输入真实姓名'
    return false
  }

  if (!registerForm.password || registerForm.password.length < 6) {
    errorMessage.value = '密码长度不能少于6位'
    return false
  }

  // 验证密码必须包含字母和数字
  const hasLetter = /[a-zA-Z]/.test(registerForm.password)
  const hasNumber = /[0-9]/.test(registerForm.password)
  if (!hasLetter || !hasNumber) {
    errorMessage.value = '密码必须包含字母和数字'
    return false
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    errorMessage.value = '密码和确认密码不匹配'
    return false
  }

  if (!registerForm.birthDate) {
    errorMessage.value = '请选择出生年月'
    return false
  }

  return true
}

const handleRegister = async () => {
  errorMessage.value = ''
  successMessage.value = ''

  if (!(await validateForm())) {
    return
  }

  loading.value = true

  try {
    const response = await fetch(`${getApiBaseUrl()}/api/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(registerForm),
    })

    const result = await response.json()

    if (result.success && result.data) {
      userStore.setUser(result.data)
      successMessage.value = '注册成功！正在跳转...'

      setTimeout(() => {
        // 学生注册成功后跳转到学生中心
        router.push('/student-center')
      }, 1500)
    } else {
      errorMessage.value = result.message || '注册失败'
    }
  } catch (error) {
    console.error('注册错误:', error)
    errorMessage.value = '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-container">
    <div class="register-card">
      <h2>用户注册</h2>

      <!-- 注册类型选择 -->
      <div class="register-type-selector">
        <div class="type-options">
          <button
            type="button"
            class="type-btn student-btn"
            :class="{ active: registerForm.userType === 'student' }"
            @click="registerForm.userType = 'student'"
          >
            <div class="type-icon">👨‍🎓</div>
            <div class="type-text">
              <h3>学生注册</h3>
              <p>寻找优质教师，开启学习之旅</p>
            </div>
          </button>

          <button
            type="button"
            class="type-btn teacher-btn"
            :class="{ active: registerForm.userType === 'teacher' }"
            @click="$router.push('/teacher-register')"
          >
            <div class="type-icon">👨‍🏫</div>
            <div class="type-text">
              <h3>教师注册</h3>
              <p>分享知识，成就他人</p>
            </div>
          </button>
        </div>
      </div>

      <form @submit.prevent="handleRegister" class="register-form">
        <!-- 基本信息 -->
        <div class="form-section">
          <h3>基本信息</h3>

          <div class="form-group">
            <label for="username">用户名 *</label>
            <input
              id="username"
              v-model="registerForm.username"
              type="text"
              placeholder="请输入用户名（3-50个字符）"
              required
            />
          </div>

          <div class="form-group">
            <label for="email">邮箱 *</label>
            <input
              id="email"
              v-model="registerForm.email"
              type="email"
              placeholder="请输入邮箱地址"
              required
            />
          </div>

          <div class="form-group">
            <label for="realName">真实姓名 *</label>
            <input
              id="realName"
              v-model="registerForm.realName"
              type="text"
              placeholder="请输入真实姓名"
              required
            />
          </div>

          <div class="form-group">
            <label for="birthDate">出生年月 *</label>
            <input
              id="birthDate"
              v-model="registerForm.birthDate"
              type="month"
              placeholder="请选择出生年月"
              required
            />
          </div>

          <div class="form-group">
            <label for="phone">手机号</label>
            <input
              id="phone"
              v-model="registerForm.phone"
              type="tel"
              placeholder="请输入手机号"
            />
          </div>

          <div class="form-group">
            <label for="password">密码 *</label>
            <input
              id="password"
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码（至少6位，必须包括字母和数字）"
              required
            />
          </div>

          <div class="form-group">
            <label for="confirmPassword">确认密码 *</label>
            <input
              id="confirmPassword"
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              required
            />
          </div>
        </div>



        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>

        <div v-if="successMessage" class="success-message">
          {{ successMessage }}
        </div>

        <button type="submit" :disabled="loading" class="register-btn">
          {{ loading ? '注册中...' : '注册学生账号' }}
        </button>
      </form>

      <div class="register-links">
        <router-link to="/login">已有账号？立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem 1rem;
}

.register-card {
  background: white;
  padding: 2rem;
  border-radius: 10px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.register-card h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #333;
}

.register-type-selector {
  margin-bottom: 2rem;
}

.type-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.type-btn {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
}

.type-btn:hover {
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
}

.type-btn.active {
  border-color: #667eea;
  background: #f8f9ff;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.type-icon {
  font-size: 2.5rem;
  flex-shrink: 0;
}

.type-text h3 {
  margin: 0 0 0.5rem 0;
  color: #333;
  font-size: 1.1rem;
}

.type-text p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
  line-height: 1.4;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-section {
  border: 1px solid #e0e0e0;
  padding: 1.5rem;
  border-radius: 8px;
  background: #f9f9f9;
}

.form-section h3 {
  margin: 0 0 1rem 0;
  color: #333;
  font-size: 1.1rem;
  border-bottom: 2px solid #667eea;
  padding-bottom: 0.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  margin-bottom: 1rem;
}

.form-group label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #555;
}

.form-group input,
.form-group textarea,
.form-group select {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 1rem;
  font-family: inherit;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

.subjects-select {
  min-height: 120px;
}

.radio-group {
  display: flex;
  gap: 1rem;
  margin-top: 0.5rem;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.9rem;
}

.radio-option input[type="radio"] {
  margin: 0;
  width: auto;
  height: auto;
}

.form-group small {
  margin-top: 0.25rem;
  color: #666;
  font-size: 0.85rem;
}

.error-message {
  color: #e74c3c;
  font-size: 0.9rem;
  text-align: center;
  padding: 0.5rem;
  background: #fdf2f2;
  border-radius: 5px;
  border: 1px solid #fecaca;
}

.success-message {
  color: #27ae60;
  font-size: 0.9rem;
  text-align: center;
  padding: 0.5rem;
  background: #f0f9f0;
  border-radius: 5px;
  border: 1px solid #d4edda;
}

.register-btn {
  padding: 0.75rem;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.3s;
  margin-top: 1rem;
}

.register-btn:hover:not(:disabled) {
  background: #5a6fd8;
}

.register-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.register-links {
  display: flex;
  justify-content: space-between;
  margin-top: 1rem;
  font-size: 0.9rem;
}

.register-links a {
  color: #667eea;
  text-decoration: none;
}

.register-links a:hover {
  text-decoration: underline;
}

/* 滚动条样式 */
.register-card::-webkit-scrollbar {
  width: 6px;
}

.register-card::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.register-card::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

.register-card::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .register-container {
    padding: 16px;
    align-items: flex-start;
    padding-top: 80px; /* 为移动端导航留出空间 */
  }

  .register-card {
    padding: 24px;
    max-width: 100%;
    width: 100%;
    max-height: calc(100vh - 100px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .register-card h2 {
    font-size: 24px;
    margin-bottom: 24px;
  }

  .register-form {
    gap: 20px;
  }

  .form-row {
    flex-direction: column;
    gap: 16px;
  }

  .form-group {
    margin-bottom: 0;
  }

  .form-group label {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .form-group input,
  .form-group select,
  .form-group textarea {
    padding: 12px 16px;
    font-size: 16px; /* 防止iOS缩放 */
    border-radius: 8px;
  }

  .form-group textarea {
    min-height: 80px;
  }

  .checkbox-group {
    flex-wrap: wrap;
    gap: 8px;
  }

  .checkbox-item {
    min-width: calc(50% - 4px);
    font-size: 14px;
    padding: 8px 12px;
  }

  .register-btn {
    padding: 14px;
    font-size: 16px;
    border-radius: 8px;
    margin-top: 16px;
  }

  .type-options {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .type-btn {
    padding: 16px;
    gap: 12px;
  }

  .type-icon {
    font-size: 2rem;
  }

  .type-text h3 {
    font-size: 16px;
  }

  .type-text p {
    font-size: 13px;
  }

  .register-links {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }

  .register-links a {
    font-size: 14px;
    padding: 8px 0;
  }

  .error-message,
  .success-message {
    font-size: 14px;
    padding: 12px;
    border-radius: 6px;
  }
}

@media (max-width: 480px) {
  .register-container {
    padding: 12px;
    padding-top: 70px;
  }

  .register-card {
    padding: 20px;
    max-height: calc(100vh - 90px);
  }

  .register-card h2 {
    font-size: 22px;
  }

  .checkbox-item {
    min-width: 100%;
    font-size: 13px;
  }

  .form-group input,
  .form-group select,
  .form-group textarea {
    padding: 14px 16px;
  }

  .register-btn {
    padding: 16px;
    font-size: 16px;
  }
}
</style>
