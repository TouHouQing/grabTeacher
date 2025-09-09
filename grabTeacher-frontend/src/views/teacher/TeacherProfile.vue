<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useUserStore, type TeacherInfo, type PasswordChangeRequest } from '../../stores/user'
import { ElMessage } from 'element-plus'
import WideTimeSlotSelector from '../../components/WideTimeSlotSelector.vue'
import { fileAPI } from '../../utils/api'
import defaultAvatar from '../../assets/pictures/teacherBoy2.jpeg'

// 学历枚举与归一化（与后端保持一致）
const EDUCATION_ALLOWED = ['专科及以下', '本科', '硕士', '博士'] as const
const normalizeEducation = (value: string): string => {
  if (!value) return ''
  if (EDUCATION_ALLOWED.includes(value as any)) return value
  if (value.includes('硕士')) return '硕士'
  if (value.includes('博士')) return '博士'
  if (value.includes('本科')) return '本科'
  if (value.includes('专科')) return '专科及以下'
  return ''
}

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
  const url = await fileAPI.presignAndPut(_localAvatarFile.value, 'avatar')
  return url.split('?')[0] // 去掉签名参数
}
// 组件卸载时清理预览URL
onUnmounted(() => {
  if (_localAvatarPreview.value) {
    URL.revokeObjectURL(_localAvatarPreview.value)
  }
})

// 时间段接口
interface TimeSlot {
  weekday: number
  timeSlots: string[]
}

// 科目相关数据
const subjects = ref<{id: number, name: string}[]>([])
const selectedSubjectIds = ref<number[]>([])

// 教师信息表单
const teacherForm = reactive<TeacherInfo>({
  realName: '',
  birthDate: '',
  educationBackground: '',
  teachingExperience: 0,
  specialties: '',
  subjects: '',
  subjectIds: [],
  hourlyRate: 0,
  introduction: '',
  videoIntroUrl: '',
  gender: '不愿透露'
})

// 可上课时间
const availableTimeSlots = ref<TimeSlot[]>([])

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

// 性别选项
const genderOptions = [
  { label: '不愿透露', value: '不愿透露' },
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

// 获取科目列表（用于显示科目名称）
const fetchSubjects = async () => {
  try {
    const response = await userStore.getSubjects()
    if (response.success && response.data) {
      subjects.value = response.data
    } else {
      ElMessage.warning(response.message || '获取科目列表失败')
    }
  } catch (error) {
    ElMessage.error('获取科目列表失败')
  }
}

// 获取教师信息
const fetchTeacherProfile = async () => {
  loading.value = true
  try {
    const response = await userStore.getTeacherProfile()
    if (response.success && response.data) {
      Object.assign(teacherForm, response.data)
      teacherForm.educationBackground = normalizeEducation(teacherForm.educationBackground || '')
      // 设置选中的科目ID
      if (response.data.subjectIds) {
        selectedSubjectIds.value = [...response.data.subjectIds]
      }
      // 设置可上课时间
      if (response.data.availableTimeSlots) {
        availableTimeSlots.value = [...response.data.availableTimeSlots]
      } else {
        // 如果没有设置可上课时间，清空数组（表示所有时间都可以）
        availableTimeSlots.value = []
      }
    } else {
      ElMessage.warning(response.message || '获取教师信息失败')
    }
  } catch (error) {
    ElMessage.error('获取教师信息失败')
  } finally {
    loading.value = false
  }
}

// 保存教师信息
const saveProfile = async () => {

  formLoading.value = true
  try {
    // 1) 若选择了新头像，先上传至OSS，拿到最终URL
    const uploadedAvatarUrl = await uploadAvatarIfNeeded()

    // 提交教师可修改的字段（不包含：姓名/性别/级别/经验/时薪/评分/学历/科目）
    const formData = {
      birthDate: teacherForm.birthDate,
      specialties: teacherForm.specialties,
      introduction: teacherForm.introduction,
      availableTimeSlots: availableTimeSlots.value,
      // 2) 如果上传了新头像，使用新URL；否则保持原有值
      ...(uploadedAvatarUrl && { avatarUrl: uploadedAvatarUrl })
    }

    const response = await userStore.updateTeacherProfile(formData)
    if (response.success) {
      ElMessage.success('保存成功')
      if (response.data) {
        Object.assign(teacherForm, response.data)
        // 更新选中的科目ID（用于显示）
        if (response.data.subjectIds) {
          selectedSubjectIds.value = [...response.data.subjectIds]
        }
      }
      // 3) 保存成功后更新头像显示和清理临时文件
      if (uploadedAvatarUrl) {
        teacherForm.avatarUrl = uploadedAvatarUrl
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

// 获取选中科目的名称
const getSelectedSubjectNames = () => {
  if (!selectedSubjectIds.value || selectedSubjectIds.value.length === 0) {
    return ''
  }

  const subjectNames = selectedSubjectIds.value
    .map(id => subjects.value.find(s => s.id === id)?.name)
    .filter(name => name)

  return subjectNames.join(', ')
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
    // 临时使用fetch直接调用API
    const response = await fetch(`${(window as any).__GT_API_BASE_URL__ || 'http://localhost:8080'}/api/user/update-email`, {
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
onMounted(() => {
  fetchSubjects()
  fetchTeacherProfile()
})
</script>

<template>
  <div class="teacher-profile">
    <h2>个人设置</h2>

    <el-tabs>
      <el-tab-pane label="基本信息">
        <div class="profile-container" v-loading="loading">
          <div class="avatar-section">
            <div class="avatar">
              <img :src="_localAvatarPreview || teacherForm.avatarUrl || defaultAvatar" alt="头像">
            </div>
            <input type="file" accept="image/*" @change="onSelectAvatar" style="margin-bottom: 8px;" />
            <div v-if="_localAvatarFile" class="avatar-tip">
              <el-text type="info" size="small">选择新头像后，点击"保存信息"生效</el-text>
            </div>
          </div>

          <div class="form-section">
            <el-form :model="teacherForm" label-width="120px" :disabled="loading">
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
                <el-input :value="teacherForm.realName" readonly placeholder="仅管理员可修改"></el-input>
                <small style="color: #999; font-size: 12px;">注：姓名仅能由管理员修改</small>
              </el-form-item>
              <el-form-item label="出生年月" required>
                <el-input
                  v-model="teacherForm.birthDate"
                  type="month"
                  placeholder="请选择出生年月"
                ></el-input>
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="teacherForm.gender" disabled>
                  <el-radio
                    v-for="option in genderOptions"
                    :key="option.value"
                    :label="option.value"
                  >
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
                <small style="color: #999; font-size: 12px;">注：性别仅能由管理员修改</small>
              </el-form-item>
              <el-form-item label="学历">
                <el-select v-model="teacherForm.educationBackground" placeholder="请选择学历" style="width: 100%" disabled>
                  <el-option label="专科及以下" value="专科及以下"></el-option>
                  <el-option label="本科" value="本科"></el-option>
                  <el-option label="硕士" value="硕士"></el-option>
                  <el-option label="博士" value="博士"></el-option>
                </el-select>
                <small style="color: #999; font-size: 12px;">注：学历仅能由管理员修改</small>
              </el-form-item>
              <el-form-item label="教学经验(年)">
                <el-input-number
                  v-model="teacherForm.teachingExperience"
                  :min="0"
                  :max="50"
                  style="width: 100%"
                  disabled
                ></el-input-number>
                <small style="color: #999; font-size: 12px;">注：教学经验仅能由管理员修改</small>
              </el-form-item>
              <el-form-item label="教授科目">
                <el-select
                  v-model="selectedSubjectIds"
                  multiple
                  placeholder="请选择您教授的科目"
                  style="width: 100%"
                  collapse-tags
                  collapse-tags-tooltip
                  :max-collapse-tags="4"
                  disabled
                >
                  <el-option
                    v-for="subject in subjects"
                    :key="subject.id"
                    :label="subject.name"
                    :value="subject.id"
                  />
                </el-select>
                <small style="color: #999; font-size: 12px;">注：科目仅能由管理员修改</small>
              </el-form-item>
              <el-form-item label="专业特长">
                <el-input
                  v-model="teacherForm.specialties"
                  placeholder="请输入专业特长，用逗号分隔"
                ></el-input>
              </el-form-item>
              <el-form-item label="小时收费(元)">
                <el-input
                  :value="teacherForm.hourlyRate ? `$${teacherForm.hourlyRate}` : '暂未设置收费标准'"
                  readonly
                  placeholder="暂未设置收费标准（请联系管理员设置）"
                  style="width: 100%"
                ></el-input>
                <small style="color: #999; font-size: 12px;">注：收费标准只能由管理员设置</small>
              </el-form-item>
              <el-form-item label="个人介绍">
                <el-input
                  v-model="teacherForm.introduction"
                  type="textarea"
                  :rows="4"
                  placeholder="请简要介绍您的教学风格、经验和专长"
                ></el-input>
              </el-form-item>
              <el-form-item label="可上课时间">
                <WideTimeSlotSelector
                  v-model="availableTimeSlots"
                  title="设置可上课时间"
                />
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
.teacher-profile {
  padding: 20px;
}

.profile-container {
  display: flex;
  margin-top: 20px;
}

.avatar-section {
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
  margin-bottom: 20px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-section {
  flex: 1;
}

.password-container {
  margin-top: 20px;
  max-width: 600px;
}

@media (max-width: 768px) {
  .profile-container {
    flex-direction: column;
  }

  .avatar-section {
    margin-right: 0;
    margin-bottom: 30px;
  }
}
</style>
