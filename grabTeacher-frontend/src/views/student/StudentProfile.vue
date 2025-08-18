<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useUserStore, type StudentInfo, type PasswordChangeRequest } from '../../stores/user'
import { ElMessage } from 'element-plus'
import { getApiBaseUrl } from '../../utils/env'
import { fileAPI } from '../../utils/api'
import defaultAvatar from '../../assets/pictures/studentBoy2.jpeg'


const userStore = useUserStore()
const _localAvatarFile = ref<File | null>(null)
const _localAvatarPreview = ref<string | null>(null)

const onSelectAvatar = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files && input.files[0]
  if (!file) {
    _localAvatarFile.value = null
    _localAvatarPreview.value = null
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
  _localAvatarFile.value = file
  // 立即预览新头像
  _localAvatarPreview.value = URL.createObjectURL(file)
}

const saveAvatar = async () => {
  if (!_localAvatarFile.value) return
  try {
    const url = await fileAPI.presignAndPut(_localAvatarFile.value, 'avatar')
    const finalUrl = url.split('?')[0]
    const resp = await userStore.updateStudentProfile({ avatarUrl: finalUrl } as Partial<StudentInfo>)
    if (resp.success) {
      // 立即更新表单中的头像URL，用于页面显示
      studentForm.avatarUrl = finalUrl
      // 立即刷新本地用户头像用于左上角显示
      if (userStore.user) userStore.user.avatarUrl = finalUrl
      ElMessage.success('头像已更新')
      _localAvatarFile.value = null
      // 清理预览URL
      if (_localAvatarPreview.value) {
        URL.revokeObjectURL(_localAvatarPreview.value)
        _localAvatarPreview.value = null
      }
    } else {
      ElMessage.error(resp.message || '头像更新失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '头像上传失败')
  }
}
// 组件卸载时清理预览URL
onUnmounted(() => {
  if (_localAvatarPreview.value) {
    URL.revokeObjectURL(_localAvatarPreview.value)
  }
})

// 科目接口
interface Subject {
  id: number
  name: string
}

// 年级接口
interface Grade {
  id: number
  gradeName: string
  description?: string
}

// 学生信息表单
const studentForm = reactive<StudentInfo>({
  realName: '',
  birthDate: '',
  gradeLevel: '',
  subjectsInterested: '',
  learningGoals: '',
  preferredTeachingStyle: '',
  budgetRange: '',
  gender: '不愿透露'
})

// 科目相关数据
const subjects = ref<Subject[]>([])
const selectedSubjectIds = ref<number[]>([])

// 年级相关数据
const grades = ref<Grade[]>([])

// 计算属性：将选中的科目转换为字符串
const subjectsString = computed(() => {
  return selectedSubjectIds.value
    .map(id => subjects.value.find(s => s.id === id)?.name)
    .filter(Boolean)
    .join(',')
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

// 邮箱修改表单
const emailForm = reactive({
  newEmail: '',
  currentPassword: ''
})

// 加载状态
const loading = ref(false)
const formLoading = ref(false)
const passwordLoading = ref(false)
const emailLoading = ref(false)
const showEmailDialog = ref(false)

// 获取学生信息
const fetchStudentProfile = async () => {
  loading.value = true
  try {
    const response = await userStore.getStudentProfile()
    if (response.success && response.data) {
      Object.assign(studentForm, response.data)
      // 从profile响应中直接设置科目信息
      if ((response.data as any).subjectIds) {
        selectedSubjectIds.value = (response.data as any).subjectIds
      }
    } else {
      ElMessage.warning(response.message || '获取学生信息失败')
    }
  } catch (error) {
    ElMessage.error('获取学生信息失败')
  } finally {
    loading.value = false
  }
}

// 获取科目列表
const fetchSubjects = async () => {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/public/subjects/active`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    if (response.ok) {
      const result = await response.json()
      if (result.success && result.data) {
        subjects.value = result.data.map((subject: any) => ({
          id: subject.id,
          name: subject.name
        }))
      }
    }
  } catch (error) {
    console.error('获取科目列表失败:', error)
  }
}

// 获取年级列表
const fetchGrades = async () => {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/public/grades`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    if (response.ok) {
      const result = await response.json()
      if (result.success && result.data) {
        grades.value = result.data.map((grade: any) => ({
          id: grade.id,
          gradeName: grade.gradeName,
          description: grade.description
        }))
      }
    }
  } catch (error) {
    console.error('获取年级列表失败:', error)
  }
}

// 获取学生感兴趣的科目（从profile数据中获取，不需要单独请求）
const fetchStudentSubjects = async () => {
  // 学生的科目信息已经包含在profile中，不需要单独请求
  // 这个函数保留是为了兼容性，但实际上科目信息会在loadProfile中设置
  console.log('学生科目信息已从profile中获取')
}

// 保存学生信息
const saveProfile = async () => {
  if (!studentForm.realName) {
    ElMessage.warning('请输入真实姓名')
    return
  }

  // 更新科目字符串
  studentForm.subjectsInterested = subjectsString.value

  formLoading.value = true
  try {
    // 构建包含科目ID的请求数据
    const requestData = {
      ...studentForm,
      subjectIds: selectedSubjectIds.value
    }

    const response = await userStore.updateStudentProfile(requestData)
    if (response.success) {
      ElMessage.success('保存成功')
      if (response.data) {
        Object.assign(studentForm, response.data)
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

// 修改邮箱
const changeEmail = async () => {
  if (!emailForm.newEmail || !emailForm.currentPassword) {
    ElMessage.warning('请填写完整的邮箱信息')
    return
  }

  // 简单的邮箱格式验证
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(emailForm.newEmail)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }

  if (emailForm.currentPassword.length < 6) {
    ElMessage.error('密码长度不能少于6位')
    return
  }

  emailLoading.value = true
  try {
    // 临时使用fetch直接调用API，稍后会在store中添加方法
    const response = await fetch(`${getApiBaseUrl()}/api/user/update-email`, {
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
      // 清空表单
      emailForm.newEmail = ''
      emailForm.currentPassword = ''
      // 关闭对话框
      showEmailDialog.value = false
      // 重新获取用户信息以更新显示
      await userStore.initializeAuth()
    } else {
      ElMessage.error(result.message || '邮箱修改失败')
    }
  } catch (error) {
    ElMessage.error('邮箱修改失败')
  } finally {
    emailLoading.value = false
  }
}

// 页面加载时获取数据
onMounted(async () => {
  await fetchSubjects()
  await fetchGrades()
  await fetchStudentProfile() // 这里会自动从profile中获取科目信息
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
              <img :src="_localAvatarPreview || studentForm.avatarUrl || defaultAvatar" alt="头像">
            </div>
            <input type="file" accept="image/*" @change="onSelectAvatar" style="margin-bottom: 8px;" />
            <el-button size="small" type="primary" @click="saveAvatar" :disabled="!_localAvatarFile">保存头像</el-button>
          </div>

          <div class="form-container">
            <el-form :model="studentForm" label-width="120px" :disabled="loading">
              <el-form-item label="用户名">
                <el-input :value="userStore.user?.username" disabled></el-input>
              </el-form-item>
              <el-form-item label="邮箱">
                <div style="display: flex; align-items: center; gap: 10px;">
                  <el-input :value="userStore.user?.email" disabled style="flex: 1;"></el-input>
                  <el-button type="text" @click="showEmailDialog = true">修改</el-button>
                </div>
              </el-form-item>
              <el-form-item label="真实姓名" required>
                <el-input v-model="studentForm.realName" placeholder="请输入真实姓名"></el-input>
              </el-form-item>
              <el-form-item label="出生年月" required>
                <el-input
                  v-model="studentForm.birthDate"
                  type="month"
                  placeholder="请选择出生年月"
                ></el-input>
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
              <el-form-item label="年级">
                <el-select v-model="studentForm.gradeLevel" placeholder="请选择年级" style="width: 100%">
                  <el-option
                    v-for="grade in grades"
                    :key="grade.id"
                    :label="grade.gradeName"
                    :value="grade.gradeName"
                  ></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="感兴趣的科目">
                <el-select
                  v-model="selectedSubjectIds"
                  multiple
                  placeholder="请选择感兴趣的科目"
                  style="width: 100%"
                >
                  <el-option
                    v-for="subject in subjects"
                    :key="subject.id"
                    :label="subject.name"
                    :value="subject.id"
                  />
                </el-select>
                <div style="margin-top: 5px; color: #909399; font-size: 12px;">
                  已选择：{{ subjectsString || '暂无' }}
                </div>
              </el-form-item>
              <el-form-item label="学习目标">
                <el-input
                  v-model="studentForm.learningGoals"
                  type="textarea"
                  :rows="3"
                  placeholder="请描述您的学习目标"
                ></el-input>
              </el-form-item>
              <el-form-item label="偏好教学风格">
                <el-select v-model="studentForm.preferredTeachingStyle" placeholder="请选择偏好的教学风格" style="width: 100%">
                  <el-option label="启发式教学" value="启发式教学"></el-option>
                  <el-option label="严谨型教学" value="严谨型教学"></el-option>
                  <el-option label="幽默风趣型" value="幽默风趣型"></el-option>
                  <el-option label="互动式教学" value="互动式教学"></el-option>
                  <el-option label="实践型教学" value="实践型教学"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="预算范围">
                <el-select v-model="studentForm.budgetRange" placeholder="请选择预算范围" style="width: 100%">
                  <el-option label="50-100元/小时" value="50-100"></el-option>
                  <el-option label="100-200元/小时" value="100-200"></el-option>
                  <el-option label="200-300元/小时" value="200-300"></el-option>
                  <el-option label="300元以上/小时" value="300+"></el-option>
                </el-select>
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

    <!-- 邮箱修改对话框 -->
    <el-dialog v-model="showEmailDialog" title="修改邮箱" width="400px">
      <el-form :model="emailForm" label-width="100px">
        <el-form-item label="当前邮箱">
          <el-input :value="userStore.user?.email" disabled></el-input>
        </el-form-item>
        <el-form-item label="新邮箱">
          <el-input
            v-model="emailForm.newEmail"
            type="email"
            placeholder="请输入新邮箱地址"
          ></el-input>
        </el-form-item>
        <el-form-item label="当前密码">
          <el-input
            v-model="emailForm.currentPassword"
            type="password"
            placeholder="请输入当前密码进行验证"
            show-password
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEmailDialog = false">取消</el-button>
          <el-button type="primary" @click="changeEmail" :loading="emailLoading">
            确认修改
          </el-button>
        </span>
      </template>
    </el-dialog>
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
