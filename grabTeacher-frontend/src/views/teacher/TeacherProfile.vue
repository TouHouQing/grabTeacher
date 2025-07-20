<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore, type TeacherInfo, type PasswordChangeRequest } from '../../stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

// 教师信息表单
const teacherForm = reactive<TeacherInfo>({
  realName: '',
  educationBackground: '',
  teachingExperience: 0,
  specialties: '',
  subjects: '',
  hourlyRate: 0,
  introduction: '',
  videoIntroUrl: '',
  gender: '不愿透露'
})

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

// 性别选项
const genderOptions = [
  { label: '不愿透露', value: '不愿透露' },
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

// 获取教师信息
const fetchTeacherProfile = async () => {
  loading.value = true
  try {
    const response = await userStore.getTeacherProfile()
    if (response.success && response.data) {
      Object.assign(teacherForm, response.data)
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
    const response = await userStore.updateTeacherProfile(teacherForm)
    if (response.success) {
      ElMessage.success('保存成功')
      if (response.data) {
        Object.assign(teacherForm, response.data)
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
onMounted(() => {
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
                <el-input :value="userStore.user?.email" disabled></el-input>
              </el-form-item>
              <el-form-item label="真实姓名" required>
                <el-input v-model="teacherForm.realName" placeholder="请输入真实姓名"></el-input>
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
                  v-model="teacherForm.subjects"
                  placeholder="请输入教授科目，用逗号分隔"
                ></el-input>
              </el-form-item>
              <el-form-item label="专业特长">
                <el-input
                  v-model="teacherForm.specialties"
                  placeholder="请输入专业特长，用逗号分隔"
                ></el-input>
              </el-form-item>
              <el-form-item label="小时收费(元)">
                <el-input-number
                  v-model="teacherForm.hourlyRate"
                  :min="0"
                  :max="2000"
                  :step="10"
                  style="width: 100%"
                ></el-input-number>
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
                placeholder="请输入新密码(至少6位)"
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
