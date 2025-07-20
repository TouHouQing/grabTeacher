<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore, type LoginRequest } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMessage = ref('')

const loginForm = reactive<LoginRequest>({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    errorMessage.value = '请填写完整的登录信息'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const result = await userStore.login(loginForm)

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
  } catch (error) {
    console.error('登录错误:', error)
    errorMessage.value = '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <h2>学生登录</h2>
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

        <button type="submit" :disabled="loading" class="login-btn">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="login-links">
        <router-link to="/register">还没有账号？立即注册</router-link>
        <router-link to="/teacher-login">教师登录</router-link>
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
  max-width: 400px;
}

.login-card h2 {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #333;
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
  transition: background 0.3s;
}

.login-btn:hover:not(:disabled) {
  background: #5a6fd8;
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

  .form-group input {
    padding: 14px 16px;
  }

  .login-btn {
    padding: 16px;
    font-size: 16px;
  }
}
</style>
