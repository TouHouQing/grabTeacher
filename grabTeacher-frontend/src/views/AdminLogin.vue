<template>
  <div class="admin-login-container">
    <div class="admin-login-wrapper">
      <div class="admin-login-header">
        <h2>管理员登录</h2>
        <p>Administrator Login</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="admin-login-form"
        label-position="top"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="邮箱/用户名" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入邮箱或用户名"
            prefix-icon="User"
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="admin-login-btn"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="admin-login-footer">
        <el-link @click="$router.push('/')" type="primary">
          返回首页
        </el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

// 表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入邮箱或用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    const valid = await loginFormRef.value.validate()
    if (!valid) return

    loading.value = true

    await userStore.adminLogin({
      username: loginForm.username,
      password: loginForm.password
    })

    ElMessage.success('登录成功')
    router.push('/admin-center')

  } catch (error: any) {
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败，请检查邮箱和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.admin-login-wrapper {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.admin-login-header {
  text-align: center;
  margin-bottom: 32px;
}

.admin-login-header h2 {
  color: #2c3e50;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
}

.admin-login-header p {
  color: #7f8c8d;
  font-size: 14px;
  font-weight: 400;
}

.admin-login-form {
  margin-bottom: 24px;
}

.admin-login-form .el-form-item {
  margin-bottom: 24px;
}

.admin-login-form .el-form-item__label {
  color: #2c3e50;
  font-weight: 500;
  margin-bottom: 8px;
}

.admin-login-form .el-input {
  height: 48px;
}

.admin-login-form .el-input__wrapper {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.admin-login-btn {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.admin-login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.admin-login-footer {
  text-align: center;
  padding-top: 16px;
  border-top: 1px solid #e9ecef;
}

.admin-login-footer .el-link {
  font-size: 14px;
}

@media (max-width: 480px) {
  .admin-login-wrapper {
    padding: 24px;
    margin: 10px;
  }

  .admin-login-header h2 {
    font-size: 24px;
  }
}
</style>
