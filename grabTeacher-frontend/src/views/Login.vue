<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore, type LoginRequest } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMessage = ref('')
const selectedUserType = ref<'student' | 'teacher' | 'admin'>('student')

const loginForm = reactive<LoginRequest>({
  username: '',
  password: ''
})

// 用户类型选项
const userTypeOptions = [
  { value: 'student', label: '学生', icon: '👨‍🎓', color: '#667eea' },
  { value: 'teacher', label: '教师', icon: '👨‍🏫', color: '#f093fb' },
  { value: 'admin', label: '管理员', icon: '👨‍💼', color: '#4facfe' }
]

// 计算当前选中的用户类型信息
const currentUserType = computed(() => {
  return userTypeOptions.find(option => option.value === selectedUserType.value)
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    errorMessage.value = '请填写完整的登录信息'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    let result

    // 根据选择的用户类型调用不同的登录方法
    if (selectedUserType.value === 'admin') {
      result = await userStore.adminLogin(loginForm)
    } else {
      result = await userStore.login(loginForm)
    }

    if (result.success) {
      // 登录成功，根据用户类型跳转
      const userType = userStore.user?.userType
      if (userType === 'student') {
        router.push('/student-center')
      } else if (userType === 'teacher') {
        router.push('/teacher-center')
      } else if (userType === 'admin') {
        router.push('/admin-center')
      } else {
        router.push('/')
      }
    } else {
      errorMessage.value = result.message || '登录失败'
    }
  } catch (error: any) {
    console.error('登录错误:', error)
    errorMessage.value = error.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <h2>用户登录</h2>

      <!-- 用户类型选择 -->
      <div class="user-type-selector">
        <div class="user-type-label">选择登录身份</div>
        <div class="user-type-options">
          <div
            v-for="option in userTypeOptions"
            :key="option.value"
            class="user-type-option"
            :class="{ active: selectedUserType === option.value }"
            @click="selectedUserType = option.value"
          >
            <div class="user-type-icon">{{ option.icon }}</div>
            <div class="user-type-text">{{ option.label }}</div>
          </div>
        </div>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">用户名或邮箱</label>
          <input
            id="username"
            v-model="loginForm.username"
            type="text"
            placeholder="请输入用户名或邮箱"
            required
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="login-btn"
          :style="{ background: currentUserType?.color }"
        >
          {{ loading ? '登录中...' : `${currentUserType?.label}登录` }}
        </button>
      </form>

      <div class="register-tip">
        <router-link to="/about#contact-us">如需注册请联系管理员</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  background: white;
  padding: 2rem;
  border-radius: 10px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 450px;
}

.login-card h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #333;
}

/* 用户类型选择器样式 */
.user-type-selector {
  margin-bottom: 2rem;
}

.user-type-label {
  text-align: center;
  margin-bottom: 1rem;
  font-weight: 500;
  color: #555;
  font-size: 0.9rem;
}

.user-type-options {
  display: flex;
  gap: 0.5rem;
  justify-content: center;
}

.user-type-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem 0.8rem;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f8f9fa;
  min-width: 80px;
}

.user-type-option:hover {
  border-color: #667eea;
  background: #f0f4ff;
}

.user-type-option.active {
  border-color: #667eea;
  background: #667eea;
  color: white;
}

.user-type-icon {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
}

.user-type-text {
  font-size: 0.8rem;
  font-weight: 500;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #555;
}

.form-group input {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 1rem;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
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

.login-btn {
  padding: 0.75rem;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.login-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.login-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 1rem;
  font-size: 0.9rem;
}

.login-links a {
  color: #667eea;
  text-decoration: none;
}

.login-links a:hover {
  text-decoration: underline;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .login-container {
    padding: 16px;
    align-items: flex-start;
    padding-top: 80px; /* 为移动端导航留出空间 */
  }

  .login-card {
    padding: 24px;
    max-width: 100%;
    width: 100%;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .login-card h2 {
    font-size: 24px;
    margin-bottom: 24px;
  }

  .user-type-selector {
    margin-bottom: 1.5rem;
  }

  .user-type-options {
    gap: 0.3rem;
  }

  .user-type-option {
    padding: 0.8rem 0.6rem;
    min-width: 70px;
  }

  .user-type-icon {
    font-size: 1.3rem;
    margin-bottom: 0.3rem;
  }

  .user-type-text {
    font-size: 0.75rem;
  }

  .form-group label {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .form-group input {
    padding: 12px 16px;
    font-size: 16px; /* 防止iOS缩放 */
    border-radius: 8px;
  }

  .login-btn {
    padding: 14px;
    font-size: 16px;
    border-radius: 8px;
    margin-top: 8px;
  }

  .login-links {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }

  .login-links a {
    font-size: 14px;
    padding: 8px 0;
  }

  .error-message {
    font-size: 14px;
    padding: 12px;
    border-radius: 6px;
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: 12px;
    padding-top: 70px;
  }

  .login-card {
    padding: 20px;
  }

  .login-card h2 {
    font-size: 22px;
  }

  .user-type-options {
    gap: 0.2rem;
  }

  .user-type-option {
    padding: 0.7rem 0.5rem;
    min-width: 65px;
  }

  .user-type-icon {
    font-size: 1.2rem;
  }

  .user-type-text {
    font-size: 0.7rem;
  }

  .form-group input {
    padding: 14px 16px;
  }

  .login-btn {
    padding: 16px;
    font-size: 16px;
  }
}
</style>
