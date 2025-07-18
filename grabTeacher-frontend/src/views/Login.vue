<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 组件名称
defineOptions({
  name: 'LoginView'
})

// 表单引用
const formRef = ref<FormInstance | null>(null)

// 表单数据
const form = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 表单校验规则
const rules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ]
})

const loading = ref(false)

const handleLogin = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    // 在实际应用中，这里应该调用API进行登录验证
    // 这里使用预设的账户进行模拟登录
    try {
      const isValid = userStore.validateLogin(form.username, form.password, 'student')
      if (isValid) {
        userStore.login(form.username, 'student')
        ElMessage.success('登录成功')
        router.push('/student-center')
      } else {
        ElMessage.error('用户名或密码错误')
      }
    } catch (error) {
      console.error('登录失败:', error)
      ElMessage.error('登录失败，请稍后再试')
    } finally {
      loading.value = false
    }
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 表单提交
const submitForm = async (formEl: FormInstance | null) => {
  if (!formEl) return
  await formEl.validate((valid, fields) => {
    if (valid) {
      console.log('登录成功', form)
      handleLogin()
    } else {
      console.log('表单验证失败', fields)
    }
  })
}

// 重置表单
const resetForm = (formEl: FormInstance | null) => {
  if (!formEl) return
  formEl.resetFields()
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>学生登录</h2>
        <p>欢迎回来，请登录您的账号</p>
      </div>
      <div class="login-form">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
          </el-form-item>
          <div class="login-options">
            <el-checkbox v-model="form.rememberMe">记住我</el-checkbox>
            <a href="#">忘记密码？</a>
          </div>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="login-button" @click="submitForm(formRef)">
              登录
            </el-button>
          </el-form-item>
          <div class="login-tips">
            <p>测试账号: student / 密码: 123456</p>
          </div>
        </el-form>
        <div class="login-footer">
          还没有账号？<a href="#" @click.prevent="router.push('/register')">立即注册</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  height: calc(100vh - 60px);
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
  background-image: v-bind('`url(${$getImageUrl("@/assets/pictures/headBackground1.jpeg")})`');
  background-size: cover;
  background-position: center;
  position: relative;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
}

.login-box {
  width: 450px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  padding: 40px;
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 16px;
  color: #666;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.login-options a {
  color: #409EFF;
  text-decoration: none;
}

.login-button {
  width: 100%;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  color: #666;
}

.login-footer a {
  color: #409EFF;
  text-decoration: none;
}

.login-tips {
  text-align: center;
  margin-top: 15px;
  color: #999;
  font-size: 14px;
}

@media (max-width: 768px) {
  .login-box {
    width: 90%;
    padding: 30px;
  }
}
</style>
