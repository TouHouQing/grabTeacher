<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore, type TeacherInfo, type PasswordChangeRequest } from '../../stores/user'
import { ElMessage } from 'element-plus'
import WideTimeSlotSelector from '../../components/WideTimeSlotSelector.vue'

// 时间段接口
interface TimeSlot {
  weekday: number
  timeSlots: string[]
}

const userStore = useUserStore()

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
  if (!teacherForm.realName) {
    ElMessage.warning('请输入真实姓名')
    return
  }

  formLoading.value = true
  try {
    // 只提交教师可以修改的字段，不包括科目和收费
    const formData = {
      realName: teacherForm.realName,
      birthDate: teacherForm.birthDate,
      educationBackground: teacherForm.educationBackground,
      teachingExperience: teacherForm.teachingExperience,
      specialties: teacherForm.specialties,
      introduction: teacherForm.introduction,
      videoIntroUrl: teacherForm.videoIntroUrl,
      gender: teacherForm.gender,
      availableTimeSlots: availableTimeSlots.value
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
              <img src="@/assets/pictures/teacherBoy2.jpeg" alt="头像">
            </div>
            <el-button size="small" type="primary">更换头像</el-button>
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
                <el-input v-model="teacherForm.realName" placeholder="请输入真实姓名"></el-input>
              </el-form-item>
              <el-form-item label="出生年月" required>
                <el-input
                  v-model="teacherForm.birthDate"
                  type="month"
                  placeholder="请选择出生年月"
                ></el-input>
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="teacherForm.gender">
                  <el-radio
                    v-for="option in genderOptions"
                    :key="option.value"
                    :label="option.value"
                  >
                    {{ option.label }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="学历">
                <el-select v-model="teacherForm.educationBackground" placeholder="请选择学历" style="width: 100%">
                  <el-option label="专科" value="专科"></el-option>
                  <el-option label="本科" value="本科"></el-option>
                  <el-option label="硕士研究生" value="硕士研究生"></el-option>
                  <el-option label="博士研究生" value="博士研究生"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="教学经验(年)">
                <el-input-number
                  v-model="teacherForm.teachingExperience"
                  :min="0"
                  :max="50"
                  style="width: 100%"
                ></el-input-number>
              </el-form-item>
              <el-form-item label="教授科目">
                <el-input
                  :value="getSelectedSubjectNames()"
                  readonly
                  placeholder="暂未设置教授科目（请联系管理员设置）"
                  style="width: 100%"
                ></el-input>
                <small style="color: #999; font-size: 12px;">注：教授科目只能由管理员设置</small>
              </el-form-item>
              <el-form-item label="专业特长">
                <el-input
                  v-model="teacherForm.specialties"
                  placeholder="请输入专业特长，用逗号分隔"
                ></el-input>
              </el-form-item>
              <el-form-item label="小时收费(元)">
                <el-input
                  :value="teacherForm.hourlyRate ? `¥${teacherForm.hourlyRate}` : '暂未设置收费标准'"
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
              <el-form-item label="介绍视频链接">
                <el-input
                  v-model="teacherForm.videoIntroUrl"
                  placeholder="请输入视频链接(可选)"
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
