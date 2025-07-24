<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 扩展注册请求接口
interface ExtendedRegisterRequest {
  username: string
  email: string
  password: string
  confirmPassword: string
  userType: 'student' | 'teacher' | 'admin'
  realName: string
  phone?: string
  // 教师信息
  educationBackground?: string
  teachingExperience?: number
  specialties?: string
  subjects?: string
  hourlyRate?: number
  introduction?: string
  gender?: string
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
  userType: 'teacher', // 固定为教师
  realName: '',
  phone: '',
  educationBackground: '',
  teachingExperience: 0,
  specialties: '',
  subjects: '',
  hourlyRate: 0,
  introduction: '',
  gender: '不愿透露'
})

const selectedSubjects = ref<string[]>([])

// 计算属性：将选中的科目转换为字符串
const subjectsString = computed(() => {
  return selectedSubjects.value.join(',')
})

// 性别选项
const genderOptions = [
  { label: '不愿透露', value: '不愿透露' },
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

const validateForm = (): boolean => {
  if (!registerForm.username || registerForm.username.length < 3 || registerForm.username.length > 50) {
    errorMessage.value = '用户名长度必须在3-50个字符之间'
    return false
  }

  if (!registerForm.email || !registerForm.email.includes('@')) {
    errorMessage.value = '请输入有效的邮箱地址'
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

  if (registerForm.password !== registerForm.confirmPassword) {
    errorMessage.value = '密码和确认密码不匹配'
    return false
  }

  return true
}

const handleRegister = async () => {
  errorMessage.value = ''
  successMessage.value = ''

  if (!validateForm()) {
    return
  }

  // 设置可教授科目
  registerForm.subjects = subjectsString.value

  // 调试：打印要发送的数据
  console.log('发送的教师注册数据:', JSON.stringify(registerForm, null, 2))

  loading.value = true

  try {
    const response = await fetch('http://grabteacher.ltd/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(registerForm),
    })

    const result = await response.json()
    console.log('教师注册响应:', result)

    if (result.success && result.data) {
      userStore.setUser(result.data)
      successMessage.value = '教师注册成功！正在跳转...'

      setTimeout(() => {
        // 教师注册成功后跳转到教师中心
        router.push('/teacher-center')
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
      <h2>教师注册</h2>
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
            <label for="phone">手机号码</label>
            <input
              id="phone"
              v-model="registerForm.phone"
              type="tel"
              placeholder="请输入手机号码"
            />
          </div>

          <div class="form-group">
            <label for="password">密码 *</label>
            <input
              id="password"
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码（至少6位）"
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

        <!-- 教师专用字段 -->
        <div class="form-section teacher-info">
          <h3>教师信息</h3>

          <div class="form-group">
            <label>性别</label>
            <div class="radio-group">
              <label
                v-for="option in genderOptions"
                :key="option.value"
                class="radio-option"
              >
                <input
                  v-model="registerForm.gender"
                  type="radio"
                  :value="option.value"
                />
                <span>{{ option.label }}</span>
              </label>
            </div>
          </div>

          <div class="form-group">
            <label for="educationBackground">教育背景</label>
            <textarea
              id="educationBackground"
              v-model="registerForm.educationBackground"
              placeholder="请简述您的教育背景（学历、毕业院校等）"
              rows="3"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="teachingExperience">教学经验（年）</label>
            <input
              id="teachingExperience"
              v-model.number="registerForm.teachingExperience"
              type="number"
              min="0"
              max="50"
              placeholder="请输入教学经验年数"
            />
          </div>

          <div class="form-group">
            <label for="subjects">可教授科目</label>
            <select
              id="subjects"
              v-model="selectedSubjects"
              multiple
              class="subjects-select"
            >
              <option value="数学">数学</option>
              <option value="语文">语文</option>
              <option value="英语">英语</option>
              <option value="物理">物理</option>
              <option value="化学">化学</option>
              <option value="生物">生物</option>
              <option value="历史">历史</option>
              <option value="地理">地理</option>
              <option value="政治">政治</option>
            </select>
            <small>按住 Ctrl 键可以选择多个科目</small>
          </div>

          <div class="form-group">
            <label for="specialties">专业特长</label>
            <input
              id="specialties"
              v-model="registerForm.specialties"
              type="text"
              placeholder="如：高考数学、竞赛辅导、基础提升等"
            />
          </div>

          <div class="form-group">
            <label for="hourlyRate">每小时收费（元）</label>
            <input
              id="hourlyRate"
              v-model.number="registerForm.hourlyRate"
              type="number"
              min="0"
              step="10"
              placeholder="请输入每小时收费标准"
            />
          </div>

          <div class="form-group">
            <label for="introduction">个人简介</label>
            <textarea
              id="introduction"
              v-model="registerForm.introduction"
              placeholder="请简述您的教学理念、教学方法等"
              rows="4"
            ></textarea>
          </div>
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>

        <div v-if="successMessage" class="success-message">
          {{ successMessage }}
        </div>

        <button type="submit" :disabled="loading" class="register-btn">
          {{ loading ? '注册中...' : '注册教师账号' }}
        </button>
      </form>

      <div class="register-links">
        <router-link to="/login">已有账号？立即登录</router-link>
        <router-link to="/register">学生注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 样式与学生注册页面相同 */
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
