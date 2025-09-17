<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useUserStore, type TeacherInfo } from '../../stores/user'
import { ElMessage } from 'element-plus'
import TeacherAvailabilityEditor from '@/components/scheduler/TeacherAvailabilityEditor.vue'
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

// 教师信息表单（仅保留需要在教师端编辑/展示的字段）
const teacherForm = reactive<TeacherInfo>({
  realName: '',
  introduction: '',
  teachingLocationIds: [],
  supportsOnline: false
})



// 科目和授课地点数据
const subjects = ref<{id: number, name: string}[]>([])
const teachingLocations = ref<{id: number, name: string}[]>([])

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

    } else {
      ElMessage.warning(response.message || '获取教师信息失败')
    }
  } catch {
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

    // 提交教师端可修改字段（包括姓名、个人介绍、授课地点、线上授课支持和头像；可上课时间改为“日历”页单独保存）
    const formData = {
      realName: teacherForm.realName,
      introduction: teacherForm.introduction,
      teachingLocationIds: teacherForm.teachingLocationIds,
      supportsOnline: teacherForm.supportsOnline,
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
  } catch {
    ElMessage.error('保存失败')
  } finally {
    formLoading.value = false
  }
}




// 获取科目名称
const getSubjectNames = (subjectIds: number[]): string => {
  if (!subjectIds || subjectIds.length === 0) return '未设置'
  return subjectIds.map(id => {
    const subject = subjects.value.find(s => s.id === id)
    return subject ? subject.name : `科目${id}`
  }).join('、')
}


// 加载基础数据
const loadBasicData = async () => {
  try {
    // 并行加载科目和授课地点数据
    const [subjectsResponse, locationsResponse] = await Promise.all([
      userStore.getSubjects(),
      userStore.getTeachingLocations()
    ])

    if (subjectsResponse.success && subjectsResponse.data) {
      subjects.value = subjectsResponse.data
    }

    if (locationsResponse.success && locationsResponse.data) {
      teachingLocations.value = locationsResponse.data
    }
  } catch (error) {
    console.error('加载基础数据失败:', error)
  }
}

// 页面加载时获取数据
onMounted(async () => {
  await Promise.all([
    fetchTeacherProfile(),
    loadBasicData()
  ])
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

              <el-form-item label="教师姓名">
                <el-input
                  v-model="teacherForm.realName"
                  placeholder="请输入真实姓名"
                  maxlength="20"
                  show-word-limit
                ></el-input>
              </el-form-item>

              <el-form-item label="教授科目">
                <el-input :value="getSubjectNames(teacherForm.subjectIds || [])" disabled></el-input>
              </el-form-item>

              <el-form-item label="性别">
                <el-input :value="teacherForm.gender || '未设置'" disabled></el-input>
              </el-form-item>

              <el-form-item label="时薪">
                <el-input :value="teacherForm.hourlyRate ? `$${teacherForm.hourlyRate}/小时` : '未设置'" disabled></el-input>
              </el-form-item>

              <el-form-item label="级别">
                <el-input :value="teacherForm.level || '未设置'" disabled></el-input>
              </el-form-item>

              <el-form-item label="授课地点">
                <el-select
                  v-model="teacherForm.teachingLocationIds"
                  multiple
                  placeholder="请选择授课地点"
                  style="width: 100%"
                  clearable
                >
                  <el-option
                    v-for="location in teachingLocations"
                    :key="location.id"
                    :label="location.name"
                    :value="location.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="是否支持线上授课">
                <el-radio-group v-model="teacherForm.supportsOnline">
                  <el-radio :label="true">支持</el-radio>
                  <el-radio :label="false">不支持</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="个人介绍">
                <el-input
                  v-model="teacherForm.introduction"
                  type="textarea"
                  :rows="4"
                  placeholder="请简要介绍您的教学风格、经验和专长"
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

  <el-tab-pane label="可上课时间">
    <div class="profile-container">
      <div class="form-section">
        <TeacherAvailabilityEditor v-if="teacherForm.id" :teacher-id="Number(teacherForm.id)" :months="6" />
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

/* 小屏表单适配：标签置顶、控件全宽、头像自适应 */
@media (max-width: 768px) {
  .teacher-profile { padding: 16px; }
  .avatar { width: 120px; height: 120px; }
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
