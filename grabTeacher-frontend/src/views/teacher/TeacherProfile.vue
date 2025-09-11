<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useUserStore, type TeacherInfo } from '../../stores/user'
import { ElMessage } from 'element-plus'
import WideTimeSlotSelector from '../../components/WideTimeSlotSelector.vue'
import { fileAPI } from '../../utils/api'
import defaultAvatar from '../../assets/pictures/teacherBoy2.jpeg'


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


// 教师信息表单（仅保留需要在教师端编辑/展示的字段）
const teacherForm = reactive<TeacherInfo>({
  introduction: ''
})

// 可上课时间
const availableTimeSlots = ref<TimeSlot[]>([])



// 加载状态
const loading = ref(false)
const formLoading = ref(false)



// 获取教师信息（剔除与教师端无关的字段处理）
const fetchTeacherProfile = async () => {
  loading.value = true
  try {
    const response = await userStore.getTeacherProfile()
    if (response.success && response.data) {
      Object.assign(teacherForm, response.data)
      // 设置可上课时间
      if (response.data.availableTimeSlots) {
        availableTimeSlots.value = [...response.data.availableTimeSlots]
      } else {
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

    // 提交教师端可修改字段（移除邮箱、姓名、出生年月、性别、学历、经验、科目、授课地点、特长、时薪）
    const formData = {
      introduction: teacherForm.introduction,
      availableTimeSlots: availableTimeSlots.value,
      ...(uploadedAvatarUrl && { avatarUrl: uploadedAvatarUrl })
    }

    const response = await userStore.updateTeacherProfile(formData)
    if (response.success) {
      ElMessage.success('保存成功')
      if (response.data) {
        Object.assign(teacherForm, response.data)
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




// 页面加载时获取数据
onMounted(() => {
  fetchTeacherProfile()
})
</script>

<template>
  <div class="teacher-profile">
    <h2>个人资料</h2>

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
