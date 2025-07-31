<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore, type PasswordChangeRequest } from '../../../stores/user'

const userStore = useUserStore()
const loading = ref(false)

// 修改密码表单
const passwordForm = reactive<PasswordChangeRequest>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 邮箱修改表单
const emailForm = reactive({
  newEmail: '',
  currentPassword: ''
})

// 表单验证规则
const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value && value.length >= 6) {
          const hasLetter = /[a-zA-Z]/.test(value)
          const hasNumber = /[0-9]/.test(value)
          if (!hasLetter || !hasNumber) {
            callback(new Error('密码必须包含字母和数字'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 邮箱修改验证规则
const emailRules = {
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const passwordFormRef = ref()
const emailFormRef = ref()
const emailLoading = ref(false)

// 修改密码
const changePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    loading.value = true

    await userStore.changePassword(passwordForm)
    ElMessage.success('密码修改成功')

    // 重置表单
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordFormRef.value.resetFields()
  } catch (error: any) {
    console.error('修改密码失败:', error)
    if (error.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('修改密码失败')
    }
  } finally {
    loading.value = false
  }
}

// 修改邮箱
const changeEmail = async () => {
  try {
    await emailFormRef.value.validate()
    emailLoading.value = true

    // 调用邮箱更新API
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/user/update-email`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify(emailForm)
    })
    const result = await response.json()

    if (result.success) {
      ElMessage.success('邮箱修改成功')
      // 重置表单
      emailForm.newEmail = ''
      emailForm.currentPassword = ''
      emailFormRef.value.resetFields()
      // 重新获取用户信息以更新显示
      await userStore.initializeAuth()
    } else {
      ElMessage.error(result.message || '邮箱修改失败')
    }
  } catch (error: any) {
    console.error('修改邮箱失败:', error)
    if (error.message) {
      ElMessage.error(error.message)
    } else {
      ElMessage.error('修改邮箱失败')
    }
  } finally {
    emailLoading.value = false
  }
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.resetFields()
}

// 重置邮箱表单
const resetEmailForm = () => {
  emailForm.newEmail = ''
  emailForm.currentPassword = ''
  emailFormRef.value?.resetFields()
}
</script>

<template>
  <div class="account-settings">
    <h3>账户设置</h3>

    <!-- 修改密码 -->
    <el-card class="settings-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
        </div>
      </template>
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="120px"
        style="max-width: 500px"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input
            v-model="passwordForm.currentPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位，必须包括字母和数字）"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="changePassword" :loading="loading">
            修改密码
          </el-button>
          <el-button @click="resetPasswordForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改邮箱 -->
    <el-card class="settings-card" shadow="never" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>修改邮箱</span>
        </div>
      </template>
      <el-form
        ref="emailFormRef"
        :model="emailForm"
        :rules="emailRules"
        label-width="120px"
        style="max-width: 500px"
      >
        <el-form-item label="当前邮箱">
          <el-input :value="userStore.user?.email" disabled />
        </el-form-item>

        <el-form-item label="新邮箱" prop="newEmail">
          <el-input
            v-model="emailForm.newEmail"
            type="email"
            placeholder="请输入新邮箱地址"
            autocomplete="email"
          />
        </el-form-item>

        <el-form-item label="当前密码" prop="currentPassword">
          <el-input
            v-model="emailForm.currentPassword"
            type="password"
            placeholder="请输入当前密码进行验证"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="changeEmail" :loading="emailLoading">
            修改邮箱
          </el-button>
          <el-button @click="resetEmailForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 密码安全提示 -->
    <el-card class="security-tips" shadow="never">
      <h4>密码安全提示</h4>
      <ul>
        <li>密码长度至少6位字符</li>
        <li>密码必须包含字母和数字</li>
        <li>建议使用字母、数字和特殊字符的组合</li>
        <li>不要使用过于简单的密码，如生日、电话号码等</li>
        <li>定期更换密码，提高账户安全性</li>
        <li>不要在多个平台使用相同密码</li>
      </ul>
    </el-card>
  </div>
</template>

<style scoped>
.account-settings {
  padding: 20px;
  padding-top: 5px; /* 增加顶部间距，避免被导航栏遮挡 */
}

.account-settings h3 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.settings-card {
  margin-bottom: 20px;
}

.card-header {
  font-weight: 600;
  color: #303133;
}

.security-tips {
  background-color: #f8f9fa;
}

.security-tips h4 {
  margin: 0 0 15px 0;
  color: #606266;
  font-size: 16px;
}

.security-tips ul {
  margin: 0;
  padding-left: 20px;
  color: #909399;
}

.security-tips li {
  margin-bottom: 8px;
  line-height: 1.5;
}
</style>
