<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useUserStore, type StudentInfo, type PasswordChangeRequest } from '../../stores/user'
import { ElMessage } from 'element-plus'
import { fileAPI } from '../../utils/api'


const userStore = useUserStore()
const _localAvatarFile = ref<File | null>(null)
const _localAvatarPreview = ref<string | null>(null)

const onSelectAvatar = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files && input.files[0]
  if (!file) {
    _localAvatarFile.value = null
    if (_localAvatarPreview.value) {
      URL.revokeObjectURL(_localAvatarPreview.value)
      _localAvatarPreview.value = null
    }
    return
  }
  if (!/^image\//.test(file.type)) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return
  }

  // 清理之前的预览URL
  if (_localAvatarPreview.value) {
    URL.revokeObjectURL(_localAvatarPreview.value)
  }

  _localAvatarFile.value = file
  // 仅做本地预览，不上传到OSS
  _localAvatarPreview.value = URL.createObjectURL(file)
  input.value = '' // 清空input，允许重复选择同一文件
}

// 上传头像到OSS（仅在保存时调用）
const uploadAvatarIfNeeded = async (): Promise<string | null> => {
  if (!_localAvatarFile.value) return null
  return await fileAPI.presignAndPut(_localAvatarFile.value, 'avatar')
}
// 组件卸载时清理预览URL
onUnmounted(() => {
  if (_localAvatarPreview.value) {
    URL.revokeObjectURL(_localAvatarPreview.value)
  }
})

// 学生信息表单
const studentForm = reactive<StudentInfo>({
  realName: '',
  birthDate: '',
  subjectsInterested: '',
  learningGoals: '',
  preferredTeachingStyle: '',
  budgetRange: '',
  gender: '不愿透露'
})

// 性别选项
const genderOptions = [
  { label: '不愿透露', value: '不愿透露' },
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

// 密码修改表单
const passwordForm = reactive<PasswordChangeRequest>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 加载状态
const loading = ref(false)
const formLoading = ref(false)
const passwordLoading = ref(false)


// 获取学生信息
const fetchStudentProfile = async () => {
  loading.value = true
  try {
    const response = await userStore.getStudentProfile()
    if (response.success && response.data) {
      Object.assign(studentForm, response.data)
    } else {
      ElMessage.warning(response.message || '获取学生信息失败')
    }
  } catch (error) {
    ElMessage.error('获取学生信息失败')
  } finally {
    loading.value = false
  }
}



// 保存学生信息
const saveProfile = async () => {
  if (!studentForm.realName) {
    ElMessage.warning('请输入真实姓名')
    return
  }

  formLoading.value = true
  try {
    // 1) 若选择了新头像，先上传至OSS，拿到最终URL
    const uploadedAvatarUrl = await uploadAvatarIfNeeded()

    // 构建请求数据
    const requestData = {
      ...studentForm,
      // 2) 如果上传了新头像，使用新URL；否则保持原有值
      ...(uploadedAvatarUrl && { avatarUrl: uploadedAvatarUrl })
    }

    const response = await userStore.updateStudentProfile(requestData)
    if (response.success) {
      ElMessage.success('保存成功')
      if (response.data) {
        Object.assign(studentForm, response.data)
      }
      // 3) 保存成功后更新头像显示和清理临时文件
      if (uploadedAvatarUrl) {
        studentForm.avatarUrl = uploadedAvatarUrl
        if (userStore.user) userStore.user.avatarUrl = uploadedAvatarUrl
      }
      // 清理文件状态
      _localAvatarFile.value = null
      if (_localAvatarPreview.value) {
        URL.revokeObjectURL(_localAvatarPreview.value)
        _localAvatarPreview.value = null
      }
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    formLoading.value = false
  }
}

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

// 页面加载时获取数据
onMounted(async () => {
  await fetchStudentProfile()
})


</script>

<template>
  <div class="student-profile">
    <h2>个人资料</h2>

    <el-tabs>
      <el-tab-pane label="基本信息">


        <div class="profile-container" v-loading="loading">
          <div class="avatar-container">
            <div class="avatar">
              <img :src="_localAvatarPreview || studentForm.avatarUrl || $getImageUrl('@/assets/pictures/studentBoy2.jpeg')" alt="头像">
            </div>
            <input type="file" accept="image/*" @change="onSelectAvatar" style="margin-bottom: 8px;" />
            <div v-if="_localAvatarFile" class="avatar-tip">
              <el-text type="info" size="small">选择新头像后，点击"保存信息"生效</el-text>
            </div>
          </div>

          <div class="form-container">
            <el-form :model="studentForm" label-width="120px" :disabled="loading">
              <el-form-item label="用户名">
                <el-input :value="userStore.user?.username" disabled></el-input>
              </el-form-item>
              <el-form-item label="真实姓名" required>
                <el-input v-model="studentForm.realName" placeholder="请输入真实姓名"></el-input>
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="studentForm.gender">
                  <el-radio
                    v-for="option in genderOptions"
                    :key="option.value"
                    :label="option.value"
                  >
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="学习目标">
                <el-input
                  v-model="studentForm.learningGoals"
                  type="textarea"
                  :rows="3"
                  placeholder="请描述您的学习目标"
                ></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProfile" :loading="formLoading">
                  保存信息
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="修改密码">
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
      </el-tab-pane>
    </el-tabs>


  </div>
</template>

<style scoped>
.student-profile {
  padding: 20px;
}

.profile-container {
  display: flex;
  margin-top: 20px;
}

.avatar-container {
  margin-right: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-tip {
  margin-top: 8px;
  text-align: center;
}

.avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: 15px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-container {
  flex: 1;
}

.password-container {
  margin-top: 20px;
  max-width: 600px;
}

@media (max-width: 768px) {
  .student-profile {
    padding: 16px;
  }

  .profile-container {
    flex-direction: column;
    margin-top: 16px;
  }

  .avatar-container {
    margin-right: 0;
    margin-bottom: 24px;
    align-items: center;
  }

  .avatar {
    width: 120px;
    height: 120px;
    margin-bottom: 12px;
  }

  .form-container {
    width: 100%;
  }

  .el-form-item {
    margin-bottom: 16px;
  }

  .el-form-item__label {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .el-input__inner,
  .el-select .el-input__inner,
  .el-textarea__inner {
    font-size: 16px; /* 防止iOS缩放 */
    padding: 12px 16px;
  }

  .el-button {
    width: 100%;
    margin-bottom: 12px;
  }

  .password-container {
    margin-top: 16px;
    max-width: 100%;
  }

  .el-tabs__item {
    font-size: 14px;
    padding: 0 16px;
  }

  .el-tabs__content {
    padding: 16px 0;
  }
}

@media (max-width: 480px) {
  .student-profile {
    padding: 12px;
  }

  .avatar {
    width: 100px;
    height: 100px;
  }

  .el-form-item__label {
    font-size: 13px;
  }

  .el-input__inner,
  .el-select .el-input__inner,
  .el-textarea__inner {
    padding: 14px 16px;
  }

  .el-tabs__item {
    font-size: 13px;
    padding: 0 12px;
  }
}
</style>
