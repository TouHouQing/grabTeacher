<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { studentAPI, subjectAPI, fileAPI } from '../../../utils/api'
import { getApiBaseUrl } from '../../../utils/env'
import AvatarUploader from '../../../components/AvatarUploader.vue'

// 学生列表
const studentList = ref([])
const loading = ref(false)
const subjects = ref([])
const loadingSubjects = ref(false)

// 搜索表单
const studentSearchForm = reactive({
  keyword: '',
  gender: ''
})

// 分页信息
const studentPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 对话框
const studentDialogVisible = ref(false)
const studentDialogTitle = ref('')
const studentForm = reactive({
  id: 0,
  realName: '',
  username: '',
  email: '',
  phone: '',
  password: '', // 添加密码字段
  subjectsInterested: '',
  subjectIds: [] as number[],
  learningGoals: '',
  preferredTeachingStyle: '',
  budgetRange: '',
  gender: '不愿透露',
  avatarUrl: '',
  balance: 0.00,
  trialTimes: 1,
  adjustmentTimes: 3
})

const _studentAvatarFile = ref<File | null>(null)

const handleStudentAvatarFileSelected = (file: File | null) => {
  _studentAvatarFile.value = file
}

// 上传学生头像到OSS（仅在保存时调用）
const uploadStudentAvatarIfNeeded = async (): Promise<string | null> => {
  if (!_studentAvatarFile.value) return null
  // 传递目标学生ID，如果是新建学生则为undefined
  const targetId = studentForm.id !== 0 ? studentForm.id : undefined
  return await fileAPI.presignAndPut(_studentAvatarFile.value, 'admin/student/avatar', targetId)
}
// 移除旧的头像处理逻辑

// 性别选项
const genderOptions = [
  { label: '不愿透露', value: '不愿透露' },
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

// 检查用户名是否可用（仅在新增时检查）
const checkUsernameAvailable = async (username: string): Promise<boolean> => {
  if (!username || studentForm.id !== 0) return true // 编辑时不检查
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/auth/check-username?username=${encodeURIComponent(username)}`)
    const result = await response.json()
    return result.success && result.data
  } catch (error) {
    console.error('检查用户名失败:', error)
    return false
  }
}

// 检查邮箱是否可用（仅在新增时检查）
const checkEmailAvailable = async (email: string): Promise<boolean> => {
  if (!email || studentForm.id !== 0) return true // 编辑时不检查
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/auth/check-email?email=${encodeURIComponent(email)}`)
    const result = await response.json()
    return result.success && result.data
  } catch (error) {
    console.error('检查邮箱失败:', error)
    return false
  }
}

// 自定义验证器
const validateUsername = async (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入用户名'))
    return
  }
  if (value.length < 3 || value.length > 50) {
    callback(new Error('用户名长度必须在3-50个字符之间'))
    return
  }
  if (studentForm.id === 0) { // 仅在新增时检查
    const available = await checkUsernameAvailable(value)
    if (!available) {
      callback(new Error('用户名已被使用，请选择其他用户名'))
      return
    }
  }
  callback()
}

const validateEmail = async (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入邮箱'))
    return
  }
  const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
  if (!emailRegex.test(value)) {
    callback(new Error('请输入正确的邮箱格式'))
    return
  }
  if (studentForm.id === 0) { // 仅在新增时检查
    const available = await checkEmailAvailable(value)
    if (!available) {
      callback(new Error('邮箱已被注册，请使用其他邮箱'))
      return
    }
  }
  callback()
}

// 表单验证规则
const studentRules = {
  realName: [
    { required: true, message: '请输入学生姓名', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ]
}

// 获取学生列表
const loadStudentList = async () => {
  try {
    loading.value = true
    const params = {
      page: studentPagination.currentPage,
      size: studentPagination.pageSize,
      keyword: studentSearchForm.keyword || undefined,
    }

    const result = await studentAPI.getList(params)

    if (result.success && result.data) {
      if (Array.isArray(result.data)) {
        studentList.value = result.data
        studentPagination.total = result.data.length
      } else if (result.data.records) {
        studentList.value = result.data.records
        studentPagination.total = result.data.total || result.data.records.length
      }
    }
  } catch (error: any) {
    console.error('获取学生列表失败:', error)
    ElMessage.error(error.message || '获取学生列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索学生
const searchStudents = () => {
  studentPagination.currentPage = 1
  loadStudentList()
}

// 重置搜索
const resetStudentSearch = () => {
  studentSearchForm.keyword = ''
  studentSearchForm.gender = ''
  studentPagination.currentPage = 1
  loadStudentList()
}

// 添加学生
const handleAddStudent = () => {
  studentDialogTitle.value = '添加学生'
  Object.assign(studentForm, {
    id: 0,
    realName: '',
    username: '',
    email: '',
    phone: '',
    subjectsInterested: '',
    subjectIds: [],
    learningGoals: '',
    preferredTeachingStyle: '',
    budgetRange: '',
    gender: '不愿透露',
    avatarUrl: '',
    balance: 0.00,
    trialTimes: 1,
    adjustmentTimes: 3
  })
  // 重置头像
  studentForm.avatarUrl = ''

  studentDialogVisible.value = true
}

// 编辑学生（从后端拉取详情，保证头像等字段完整回显）
const handleEditStudent = async (student: any) => {
  studentDialogTitle.value = '编辑学生'
  try {
    const resp = await studentAPI.getById(student.id)
    if (resp.success && resp.data) {
      Object.assign(studentForm, resp.data)
    } else {
      Object.assign(studentForm, student)
    }
  } catch (e) {
    Object.assign(studentForm, student)
  }
  // 编辑时清空密码字段，避免显示敏感信息
  studentForm.password = ''
  studentDialogVisible.value = true
}

// 保存学生
const saveStudent = async () => {
  try {
    loading.value = true

    const wasNew = studentForm.id === 0
    const baseData: any = {
      realName: studentForm.realName,
      username: studentForm.username,
      email: studentForm.email,
      phone: studentForm.phone,
      subjectsInterested: studentForm.subjectsInterested,
      subjectIds: studentForm.subjectIds,
      learningGoals: studentForm.learningGoals,
      preferredTeachingStyle: studentForm.preferredTeachingStyle,
      budgetRange: studentForm.budgetRange,
      gender: studentForm.gender,
      balance: studentForm.balance,
      trialTimes: studentForm.trialTimes,
      adjustmentTimes: studentForm.adjustmentTimes
    }

    // 处理密码：新建时使用默认密码，编辑时如果填写了密码则更新
    if (wasNew) {
      baseData.password = '123456' // 新建学生默认密码
    } else if (studentForm.password && studentForm.password.trim()) {
      baseData.password = studentForm.password // 编辑时如果填写了密码则更新
    }

    let result: any
    let currentId = studentForm.id

    if (wasNew) {
      // 新建：先创建拿到ID
      result = await studentAPI.create(baseData)
      if (!result.success || !result.data?.id) {
        ElMessage.error(result.message || '创建学生失败')
        return
      }
      currentId = result.data.id
      studentForm.id = currentId
    } else {
      // 编辑：先更新基础资料
      result = await studentAPI.update(currentId, baseData)
      if (!result.success) {
        ElMessage.error(result.message || '更新失败')
        return
      }
    }

    // 若选择了新头像：按最终ID上传并二次更新头像URL
    if (_studentAvatarFile.value) {
      const uploadedAvatarUrl = await fileAPI.presignAndPut(_studentAvatarFile.value, 'admin/student/avatar', currentId)
      const updateAvatarResp = await studentAPI.update(currentId, { avatarUrl: uploadedAvatarUrl } as any)
      if (!updateAvatarResp.success) {
        ElMessage.error(updateAvatarResp.message || '头像更新失败')
        return
      }
      studentForm.avatarUrl = uploadedAvatarUrl
    }

    ElMessage.success(wasNew ? '添加成功，默认密码为123456' : '更新成功')
    // 清理状态
    _studentAvatarFile.value = null
    studentDialogVisible.value = false
    await loadStudentList()
  } catch (error: any) {
    console.error('保存学生失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 删除学生
const handleDeleteStudent = async (student: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除学生"${student.realName}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const result = await studentAPI.delete(student.id)
    if (result.success) {
      ElMessage.success('删除成功')
      await loadStudentList()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除学生失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 分页处理
const handleStudentPageChange = (page: number) => {
  studentPagination.currentPage = page
  loadStudentList()
}

const handleStudentSizeChange = (size: number) => {
  studentPagination.pageSize = size
  studentPagination.currentPage = 1
  loadStudentList()
}


// 获取科目列表
const loadSubjects = async () => {
  try {
    loadingSubjects.value = true
    const response = await subjectAPI.getActiveSubjects()
    if (response.success && response.data) {
      subjects.value = response.data
    }
  } catch (error) {
    console.error('获取科目列表失败:', error)
  } finally {
    loadingSubjects.value = false
  }
}

onMounted(() => {
  loadStudentList()
  loadSubjects()
})
</script>

<template>
  <div class="student-management">
    <div class="header">
      <h3>学生管理</h3>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="studentSearchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="studentSearchForm.keyword"
            placeholder="搜索学生姓名"
            :prefix-icon="Search"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="studentSearchForm.gender" placeholder="选择性别" clearable style="width: 120px">
            <el-option
              v-for="option in genderOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="searchStudents">搜索</el-button>
          <el-button :icon="Refresh" @click="resetStudentSearch">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAddStudent">
            添加学生
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 学生表格 -->
    <el-table :data="studentList" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="subjectsInterested" label="感兴趣科目" min-width="150" show-overflow-tooltip />
      <el-table-column prop="learningGoals" label="学习目标" min-width="200" show-overflow-tooltip />
      <el-table-column prop="preferredTeachingStyle" label="偏好教学方式" min-width="150" show-overflow-tooltip />
      <el-table-column prop="budgetRange" label="预算范围" width="120" />
      <el-table-column label="余额" width="100" align="center">
        <template #default="{ row }">
          <span>{{ row.balance || 0 }}M豆</span>
        </template>
      </el-table-column>
      <el-table-column label="试听课次" width="100" align="center">
        <template #default="{ row }">
          <span>{{ row.trialTimes || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <div class="operation-buttons">
            <el-button size="small" :icon="Edit" @click="handleEditStudent(row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDeleteStudent(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        :current-page="studentPagination.currentPage"
        :page-size="studentPagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="studentPagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleStudentSizeChange"
        @current-change="handleStudentPageChange"
      />
    </div>

    <!-- 添加/编辑学生对话框 -->
    <el-dialog
      v-model="studentDialogVisible"
      :title="studentDialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="studentForm" :rules="studentRules" label-width="120px">
        <!-- 账号信息 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item prop="username">
              <template #label>
                <span><span style="color:#f56c6c"></span> 用户名</span>
              </template>
              <el-input v-model="studentForm.username" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="email">
              <template #label>
                <span><span style="color:#f56c6c"></span> 邮箱</span>
              </template>
              <el-input v-model="studentForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="studentForm.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码">
              <el-input
                v-model="studentForm.password"
                type="password"
                :placeholder="studentForm.id === 0 ? '默认密码为123456' : '留空则不修改密码'"
                show-password
              />
              <small style="color: #999; font-size: 12px;">
                {{ studentForm.id === 0 ? '新建学生默认密码为123456' : '留空则不修改密码' }}
              </small>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="头像">
          <AvatarUploader
            v-model="studentForm.avatarUrl"
            :show-upload-button="false"
            :immediate-upload="false"
            @file-selected="handleStudentAvatarFileSelected"
          />
          <div v-if="_studentAvatarFile" class="avatar-tip">
            <el-text type="info" size="small">选择新头像后，点击"保存"生效</el-text>
          </div>
        </el-form-item>

        <!-- 基本信息 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item prop="realName">
              <template #label>
                <span><span style="color:#f56c6c"></span> 学生姓名</span>
              </template>
              <el-input v-model="studentForm.realName" placeholder="请输入学生姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
          </el-col>
        </el-row>
        <el-form-item label="感兴趣科目">
          <el-select
            v-model="studentForm.subjectIds"
            multiple
            placeholder="请选择感兴趣的科目"
            style="width: 100%"
            :loading="loadingSubjects"
          >
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学习目标">
          <el-input
            v-model="studentForm.learningGoals"
            type="textarea"
            placeholder="请输入学习目标"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="偏好教学方式">
          <el-input v-model="studentForm.preferredTeachingStyle" placeholder="请输入偏好的教学方式" />
        </el-form-item>
        <el-form-item label="预算范围">
          <el-input v-model="studentForm.budgetRange" placeholder="请输入预算范围" />
        </el-form-item>
        <el-form-item label="账户余额">
          <el-input-number
            v-model="studentForm.balance"
            :min="0"
            :precision="2"
            :step="10"
            style="width: 200px"
            placeholder="请输入余额"
          />
          <span style="margin-left: 10px; color: #909399;">M豆</span>
        </el-form-item>
        <el-form-item label="试听课次">
          <el-input-number
            v-model="studentForm.trialTimes"
            :min="0"
            :precision="0"
            :step="1"
            style="width: 200px"
            placeholder="请输入试听课次"
          />
        </el-form-item>
        <el-form-item label="本月调课次数">
          <el-input-number
            v-model="studentForm.adjustmentTimes"
            :min="0"
            :precision="0"
            :step="1"
            style="width: 200px"
            placeholder="请输入本月调课次数"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="studentDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveStudent" :loading="loading">
            {{ studentForm.id === 0 ? '添加' : '更新' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-management {
  padding: 20px;
  padding-top: 5px; /* 增加顶部间距，避免被导航栏遮挡 */
}

.header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 20px;
}

.header h3 {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.search-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  width: 100%;
  padding: 0 8px;
}

.operation-buttons .el-button {
  margin: 0;
  min-width: 60px;
  text-align: center;
}

/* 确保操作列标题居中 */
:deep(.el-table__header-wrapper .el-table__header th:last-child .cell) {
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .operation-buttons {
    flex-wrap: wrap;
    gap: 4px;
    justify-content: center;
  }

  .operation-buttons .el-button {
    font-size: 12px;
    padding: 4px 8px;
    min-width: 50px;
  }
}

.avatar-tip {
  margin-top: 8px;
  text-align: center;
}
</style>
