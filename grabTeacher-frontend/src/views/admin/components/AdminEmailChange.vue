<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const loading = ref(false)

// 邮箱修改表单
const emailForm = reactive({
  newEmail: '',
  currentPassword: ''
})

// 表单验证规则
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

const emailFormRef = ref()

// 修改邮箱
const changeEmail = async () => {
  try {
    await emailFormRef.value.validate()
    loading.value = true

    // 临时使用fetch直接调用API
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
    loading.value = false
  }
}

// 重置表单
const resetForm = () => {
  emailForm.newEmail = ''
  emailForm.currentPassword = ''
  emailFormRef.value?.resetFields()
}
</script>

<template>
  <div class="email-change">
    <h3>修改邮箱</h3>

    <el-card class="email-card" shadow="never">
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
          <el-button type="primary" @click="changeEmail" :loading="loading">
            修改邮箱
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.email-change {
  padding: 20px;
}

.email-card {
  margin-top: 20px;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-button {
  margin-right: 10px;
}

h3 {
  margin-bottom: 0;
  color: #303133;
  font-weight: 500;
}
</style>
