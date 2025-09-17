<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'

// 密码修改表单接口
interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const userStore = useUserStore()

// 密码修改表单
const passwordForm = reactive<PasswordChangeRequest>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 加载状态
const passwordLoading = ref(false)

// 修改密码
const changePassword = async () => {
  if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整的密码信息')
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('新密码和确认密码不一致')
    return
  }

  if (passwordForm.newPassword.length < 6) {
    ElMessage.error('新密码长度不能少于6位')
    return
  }

  // 验证密码必须包含字母和数字
  const hasLetter = /[a-zA-Z]/.test(passwordForm.newPassword)
  const hasNumber = /[0-9]/.test(passwordForm.newPassword)
  if (!hasLetter || !hasNumber) {
    ElMessage.error('密码必须包含字母和数字')
    return
  }

  passwordLoading.value = true
  try {
    const response = await userStore.changePassword(passwordForm)
    if (response.success) {
      ElMessage.success('密码修改成功')
      // 清空表单
      passwordForm.currentPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      ElMessage.error(response.message || '密码修改失败')
    }
  } catch (error) {
    ElMessage.error('密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="teacher-password">
    <h2>修改密码</h2>

    <div class="password-container">
      <el-form :model="passwordForm" label-width="120px">
        <el-form-item label="当前密码">
          <el-input
            v-model="passwordForm.currentPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码(至少6位，必须包括字母和数字)"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="changePassword" :loading="passwordLoading">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.teacher-password {
  padding: 20px;
}

.password-container {
  margin-top: 20px;
  max-width: 600px;
}

@media (max-width: 768px) {
  .teacher-password {
    padding: 16px;
  }

  .password-container {
    max-width: 100%;
  }
}

/* 小屏表单适配：标签置顶、控件全宽、按钮在极小屏满宽 */
@media (max-width: 768px) {
  :deep(.el-form .el-form-item__label) { float: none; display: block; padding-bottom: 4px; }
  :deep(.el-form .el-form-item__content) { margin-left: 0 !important; }
  :deep(.el-input),
  :deep(.el-select),
  :deep(.el-date-editor),
  :deep(.el-textarea),
  :deep(.el-cascader) { width: 100% !important; }
}
@media (max-width: 480px) {
  :deep(.el-button) { width: 100%; }
}

</style>
