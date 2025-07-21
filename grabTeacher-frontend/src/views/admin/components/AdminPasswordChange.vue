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

// 表单验证规则
const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
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

const passwordFormRef = ref()

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

// 重置表单
const resetForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.resetFields()
}
</script>

<template>
  <div class="password-change">
    <h3>修改密码</h3>

    <el-card class="password-card" shadow="never">
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
            placeholder="请输入新密码（至少6位）"
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
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 密码安全提示 -->
    <el-card class="security-tips" shadow="never">
      <h4>密码安全提示</h4>
      <ul>
        <li>密码长度至少6位字符</li>
        <li>建议使用字母、数字和特殊字符的组合</li>
        <li>不要使用过于简单的密码，如生日、电话号码等</li>
        <li>定期更换密码，提高账户安全性</li>
        <li>不要在多个平台使用相同密码</li>
      </ul>
    </el-card>
  </div>
</template>

<style scoped>
.password-change {
  padding: 20px;
  padding-top: 5px; /* 增加顶部间距，避免被导航栏遮挡 */
}

.password-change h3 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.password-card {
  margin-bottom: 20px;
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
