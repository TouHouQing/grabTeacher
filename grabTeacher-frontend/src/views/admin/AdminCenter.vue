<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useUserStore, type PasswordChangeRequest } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Setting, Document, DataBoard, UserFilled, Avatar } from '@element-plus/icons-vue'
import { subjectAPI, studentAPI, teacherAPI, adminAPI } from '../../utils/api'

// 获取用户信息
const userStore = useUserStore()
const adminName = ref('管理员')

// 当前活跃菜单
const activeMenu = ref('dashboard')

// 页面加载状态
const loading = ref(false)

// 统计数据
const statistics = ref({
  totalUsers: 0,
  totalTeachers: 0,
  totalStudents: 0,
  verifiedTeachers: 0
})

// 修改密码表单
const passwordForm = reactive<PasswordChangeRequest>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordFormRef = ref()

// 修改密码规则
const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 学生相关类型定义
interface StudentInfo {
  id: number
  userId?: number
  realName: string
  gradeLevel?: string
  subjectsInterested?: string
  learningGoals?: string
  preferredTeachingStyle?: string
  budgetRange?: string
}

// 学生列表
const studentList = ref<StudentInfo[]>([])

// 学生搜索
const studentSearchForm = reactive({
  keyword: '',
  gradeLevel: ''
})

// 学生对话框
const studentDialogVisible = ref(false)
const studentDialogTitle = ref('')
const studentForm = reactive({
  id: 0,
  realName: '',
  gradeLevel: '',
  subjectsInterested: '',
  learningGoals: '',
  preferredTeachingStyle: '',
  budgetRange: ''
})

const studentFormRef = ref()

// 学生表单规则
const studentRules = {
  realName: [
    { required: true, message: '请输入学生姓名', trigger: 'blur' }
  ]
}

// 学生分页信息
const studentPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 教师相关类型定义
interface TeacherInfo {
  id: number
  userId?: number
  realName: string
  educationBackground?: string
  teachingExperience?: number
  specialties?: string
  subjects?: string
  hourlyRate?: number
  introduction?: string
  videoIntroUrl?: string
  isVerified: boolean
}

// 教师列表
const teacherList = ref<TeacherInfo[]>([])

// 教师搜索
const teacherSearchForm = reactive({
  keyword: '',
  subject: '',
  isVerified: ''
})

// 教师对话框
const teacherDialogVisible = ref(false)
const teacherDialogTitle = ref('')
const teacherForm = reactive({
  id: 0,
  realName: '',
  educationBackground: '',
  teachingExperience: 0,
  specialties: '',
  subjects: '',
  hourlyRate: 0,
  introduction: '',
  videoIntroUrl: ''
})

const teacherFormRef = ref()

// 教师表单规则
const teacherRules = {
  realName: [
    { required: true, message: '请输入教师姓名', trigger: 'blur' }
  ]
}

// 教师分页信息
const teacherPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 科目相关类型定义
interface SubjectInfo {
  id: number
  name: string
  gradeLevels: string
  iconUrl?: string
  isActive: boolean
}

interface SubjectRequest {
  name: string
  gradeLevels?: string
  iconUrl?: string
  isActive?: boolean
}

// 科目列表
const subjectList = ref<SubjectInfo[]>([])

// 科目搜索
const subjectSearchForm = reactive({
  keyword: '',
  status: ''
})

// 科目对话框
const subjectDialogVisible = ref(false)
const subjectDialogTitle = ref('')
const subjectForm = reactive({
  id: 0,
  name: '',
  gradeLevels: '',
  iconUrl: '',
  isActive: true
})

const subjectFormRef = ref()

// 科目表单规则
const subjectRules = {
  name: [
    { required: true, message: '请输入科目名称', trigger: 'blur' }
  ],
  gradeLevels: [
    { required: true, message: '请输入适用年级', trigger: 'blur' }
  ]
}

// 分页信息
const subjectPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 菜单切换
const handleMenuSelect = (key: string) => {
  activeMenu.value = key

  // 根据选择的菜单智能加载数据
  switch (key) {
    case 'students':
      if (studentList.value.length === 0) {
        loadStudentList()
      }
      break
    case 'teachers':
      if (teacherList.value.length === 0) {
        loadTeacherList()
      }
      break
    case 'subjects':
      if (subjectList.value.length === 0) {
        loadSubjectList()
      }
      break
    case 'dashboard':
      loadStatistics()
      break
  }
}

// 获取统计数据
const loadStatistics = async () => {
  try {
    const result = await adminAPI.getStatistics()
    if (result.success && result.data) {
      statistics.value = result.data
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 修改密码
const handlePasswordChange = async () => {
  if (!passwordFormRef.value) return

  let isFormValid = false
  try {
    isFormValid = await passwordFormRef.value.validate()
  } catch (validationError) {
    console.log('表单验证失败:', validationError)
    return
  }

  if (!isFormValid) {
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入密码不一致')
    return
  }

  loading.value = true

  try {
    const response = await userStore.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })

    if (response.success) {
      ElMessage.success('密码修改成功')
      Object.assign(passwordForm, {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      })
      passwordFormRef.value.clearValidate()
    } else {
      ElMessage.error(response.message || '密码修改失败')
    }
  } catch (error: any) {
    console.error('网络请求失败:', error)
    ElMessage.error(error.message || '网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// ===================== 学生管理 =====================

// 获取学生列表
const loadStudentList = async () => {
  try {
    loading.value = true
    const params = {
      page: studentPagination.currentPage,
      size: studentPagination.pageSize,
      keyword: studentSearchForm.keyword || undefined,
      gradeLevel: studentSearchForm.gradeLevel || undefined
    }

    const result = await studentAPI.getList(params)

    if (result.success && result.data) {
      if (result.data.records) {
        studentList.value = result.data.records
        studentPagination.total = result.data.total
      } else if (Array.isArray(result.data)) {
        studentList.value = result.data
        studentPagination.total = result.data.length
      }
    }
  } catch (error) {
    console.error('获取学生列表失败:', error)
    ElMessage.error('获取学生列表失败')
  } finally {
    loading.value = false
  }
}

// 学生搜索
const handleStudentSearch = () => {
  studentPagination.currentPage = 1
  loadStudentList()
}

// 重置学生搜索
const resetStudentSearch = () => {
  Object.assign(studentSearchForm, {
    keyword: '',
    gradeLevel: ''
  })
  handleStudentSearch()
}

// 添加学生
const handleAddStudent = () => {
  studentDialogTitle.value = '添加学生'
  Object.assign(studentForm, {
    id: 0,
    realName: '',
    gradeLevel: '',
    subjectsInterested: '',
    learningGoals: '',
    preferredTeachingStyle: '',
    budgetRange: ''
  })
  studentDialogVisible.value = true
}

// 编辑学生
const handleEditStudent = (row: StudentInfo) => {
  studentDialogTitle.value = '编辑学生'
  Object.assign(studentForm, {
    id: row.id,
    realName: row.realName,
    gradeLevel: row.gradeLevel || '',
    subjectsInterested: row.subjectsInterested || '',
    learningGoals: row.learningGoals || '',
    preferredTeachingStyle: row.preferredTeachingStyle || '',
    budgetRange: row.budgetRange || ''
  })
  studentDialogVisible.value = true
}

// 删除学生
const handleDeleteStudent = async (row: StudentInfo) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除学生 "${row.realName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const result = await studentAPI.delete(row.id)

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
  } finally {
    loading.value = false
  }
}

// 保存学生
const handleSaveStudent = async () => {
  if (!studentFormRef.value) return

  try {
    const isValid = await studentFormRef.value.validate()
    if (!isValid) return

    loading.value = true

    const studentData = {
      realName: studentForm.realName,
      gradeLevel: studentForm.gradeLevel,
      subjectsInterested: studentForm.subjectsInterested,
      learningGoals: studentForm.learningGoals,
      preferredTeachingStyle: studentForm.preferredTeachingStyle,
      budgetRange: studentForm.budgetRange
    }

    let result
    if (studentForm.id === 0) {
      result = await studentAPI.create(studentData)
    } else {
      result = await studentAPI.update(studentForm.id, studentData)
    }

    if (result.success) {
      ElMessage.success(studentForm.id === 0 ? '学生添加成功' : '学生更新成功')
      studentDialogVisible.value = false
      await loadStudentList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('保存学生失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 学生分页处理
const handleStudentPageChange = (page: number) => {
  studentPagination.currentPage = page
  loadStudentList()
}

// ===================== 教师管理 =====================

// 获取教师列表
const loadTeacherList = async () => {
  try {
    loading.value = true
    const params = {
      page: teacherPagination.currentPage,
      size: teacherPagination.pageSize,
      keyword: teacherSearchForm.keyword || undefined,
      subject: teacherSearchForm.subject || undefined,
      isVerified: teacherSearchForm.isVerified === '已认证' ? true :
                  teacherSearchForm.isVerified === '未认证' ? false : undefined
    }

    const result = await teacherAPI.getList(params)

    if (result.success && result.data) {
      if (result.data.records) {
        teacherList.value = result.data.records
        teacherPagination.total = result.data.total
      } else if (Array.isArray(result.data)) {
        teacherList.value = result.data
        teacherPagination.total = result.data.length
      }
    }
  } catch (error) {
    console.error('获取教师列表失败:', error)
    ElMessage.error('获取教师列表失败')
  } finally {
    loading.value = false
  }
}

// 教师搜索
const handleTeacherSearch = () => {
  teacherPagination.currentPage = 1
  loadTeacherList()
}

// 重置教师搜索
const resetTeacherSearch = () => {
  Object.assign(teacherSearchForm, {
    keyword: '',
    subject: '',
    isVerified: ''
  })
  handleTeacherSearch()
}

// 添加教师
const handleAddTeacher = () => {
  teacherDialogTitle.value = '添加教师'
  Object.assign(teacherForm, {
    id: 0,
    realName: '',
    educationBackground: '',
    teachingExperience: 0,
    specialties: '',
    subjects: '',
    hourlyRate: 0,
    introduction: '',
    videoIntroUrl: ''
  })
  teacherDialogVisible.value = true
}

// 编辑教师
const handleEditTeacher = (row: TeacherInfo) => {
  teacherDialogTitle.value = '编辑教师'
  Object.assign(teacherForm, {
    id: row.id,
    realName: row.realName,
    educationBackground: row.educationBackground || '',
    teachingExperience: row.teachingExperience || 0,
    specialties: row.specialties || '',
    subjects: row.subjects || '',
    hourlyRate: row.hourlyRate || 0,
    introduction: row.introduction || '',
    videoIntroUrl: row.videoIntroUrl || ''
  })
  teacherDialogVisible.value = true
}

// 删除教师
const handleDeleteTeacher = async (row: TeacherInfo) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除教师 "${row.realName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const result = await teacherAPI.delete(row.id)

    if (result.success) {
      ElMessage.success('删除成功')
      await loadTeacherList()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除教师失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 保存教师
const handleSaveTeacher = async () => {
  if (!teacherFormRef.value) return

  try {
    const isValid = await teacherFormRef.value.validate()
    if (!isValid) return

    loading.value = true

    const teacherData = {
      realName: teacherForm.realName,
      educationBackground: teacherForm.educationBackground,
      teachingExperience: teacherForm.teachingExperience,
      specialties: teacherForm.specialties,
      subjects: teacherForm.subjects,
      hourlyRate: teacherForm.hourlyRate,
      introduction: teacherForm.introduction,
      videoIntroUrl: teacherForm.videoIntroUrl
    }

    let result
    if (teacherForm.id === 0) {
      result = await teacherAPI.create(teacherData)
    } else {
      result = await teacherAPI.update(teacherForm.id, teacherData)
    }

    if (result.success) {
      ElMessage.success(teacherForm.id === 0 ? '教师添加成功' : '教师更新成功')
      teacherDialogVisible.value = false
      await loadTeacherList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('保存教师失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 审核教师
const handleVerifyTeacher = async (row: TeacherInfo, isVerified: boolean) => {
  try {
    loading.value = true
    const result = await teacherAPI.verify(row.id, isVerified)

    if (result.success) {
      ElMessage.success(isVerified ? '教师认证成功' : '教师认证已取消')
      await loadTeacherList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('审核教师失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 教师分页处理
const handleTeacherPageChange = (page: number) => {
  teacherPagination.currentPage = page
  loadTeacherList()
}

// ===================== 科目管理 =====================

// 获取科目列表
const loadSubjectList = async () => {
  try {
    loading.value = true
    const params = {
      page: subjectPagination.currentPage,
      size: subjectPagination.pageSize,
      keyword: subjectSearchForm.keyword || undefined,
      isActive: subjectSearchForm.status === '启用' ? true : subjectSearchForm.status === '停用' ? false : undefined
    }

    const result = await subjectAPI.getList(params)

    if (result.success && result.data) {
      if (Array.isArray(result.data)) {
        subjectList.value = result.data
        subjectPagination.total = result.data.length
      } else if (result.data.subjects) {
        subjectList.value = result.data.subjects
        subjectPagination.total = result.data.total || result.data.subjects.length
      }
    }
  } catch (error) {
    console.error('获取科目列表失败:', error)
    ElMessage.error('获取科目列表失败')
  } finally {
    loading.value = false
  }
}

// 科目搜索
const handleSubjectSearch = () => {
  subjectPagination.currentPage = 1
  loadSubjectList()
}

// 重置科目搜索
const resetSubjectSearch = () => {
  Object.assign(subjectSearchForm, {
    keyword: '',
    status: ''
  })
  handleSubjectSearch()
}

// 添加科目
const handleAddSubject = () => {
  subjectDialogTitle.value = '添加科目'
  Object.assign(subjectForm, {
    id: 0,
    name: '',
    gradeLevels: '',
    iconUrl: '',
    isActive: true
  })
  subjectDialogVisible.value = true
}

// 编辑科目
const handleEditSubject = (row: SubjectInfo) => {
  subjectDialogTitle.value = '编辑科目'
  Object.assign(subjectForm, {
    id: row.id,
    name: row.name,
    gradeLevels: row.gradeLevels,
    iconUrl: row.iconUrl || '',
    isActive: row.isActive
  })
  subjectDialogVisible.value = true
}

// 删除科目
const handleDeleteSubject = async (row: SubjectInfo) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除科目 "${row.name}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const result = await subjectAPI.delete(row.id)

    if (result.success) {
      ElMessage.success('删除成功')
      await loadSubjectList()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除科目失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 保存科目
const handleSaveSubject = async () => {
  if (!subjectFormRef.value) return

  try {
    const isValid = await subjectFormRef.value.validate()
    if (!isValid) return

    loading.value = true

    const subjectData: SubjectRequest = {
      name: subjectForm.name,
      gradeLevels: subjectForm.gradeLevels,
      iconUrl: subjectForm.iconUrl,
      isActive: subjectForm.isActive
    }

    let result
    if (subjectForm.id === 0) {
      result = await subjectAPI.create(subjectData)
    } else {
      result = await subjectAPI.update(subjectForm.id, subjectData)
    }

    if (result.success) {
      ElMessage.success(subjectForm.id === 0 ? '科目添加成功' : '科目更新成功')
      subjectDialogVisible.value = false
      await loadSubjectList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('保存科目失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 分页处理
const handleSubjectPageChange = (page: number) => {
  subjectPagination.currentPage = page
  loadSubjectList()
}

// 页面初始化
onMounted(async () => {
  await loadStatistics()
  await loadSubjectList()
  // 添加学生和教师数据的初始加载
  await loadStudentList()
  await loadTeacherList()
})
</script>

<template>
  <div class="admin-center">
    <div class="admin-header">
      <h2>管理员控制台</h2>
      <div class="admin-info">
        <span>欢迎回来，{{ adminName }}</span>
      </div>
    </div>

    <div class="admin-container">
      <!-- 侧边菜单 -->
      <div class="admin-sidebar">
        <el-menu
          :default-active="activeMenu"
          class="admin-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>数据概览</span>
          </el-menu-item>
          <el-menu-item index="students">
            <el-icon><UserFilled /></el-icon>
            <span>学生管理</span>
          </el-menu-item>
          <el-menu-item index="teachers">
            <el-icon><Avatar /></el-icon>
            <span>教师管理</span>
          </el-menu-item>
          <el-menu-item index="subjects">
            <el-icon><Document /></el-icon>
            <span>科目管理</span>
          </el-menu-item>
          <el-menu-item index="password">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 主要内容区域 -->
      <div class="admin-content">
        <!-- 数据概览 -->
        <div v-show="activeMenu === 'dashboard'" class="content-panel">
          <h3>数据概览</h3>
          <el-row :gutter="20" class="statistics-cards">
            <el-col :xs="24" :sm="12" :lg="6">
              <el-card class="statistic-card">
                <div class="statistic">
                  <h4>用户总数</h4>
                  <div class="statistic-value">{{ statistics.totalUsers }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-card class="statistic-card">
                <div class="statistic">
                  <h4>教师数量</h4>
                  <div class="statistic-value">{{ statistics.totalTeachers }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-card class="statistic-card">
                <div class="statistic">
                  <h4>学生数量</h4>
                  <div class="statistic-value">{{ statistics.totalStudents }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-card class="statistic-card">
                <div class="statistic">
                  <h4>认证教师</h4>
                  <div class="statistic-value">{{ statistics.verifiedTeachers }}</div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 学生管理 -->
        <div v-show="activeMenu === 'students'" class="content-panel">
          <h3>学生管理</h3>

          <!-- 搜索区域 -->
          <el-card class="search-card">
            <el-form :model="studentSearchForm" inline>
              <el-form-item label="关键词">
                <el-input
                  v-model="studentSearchForm.keyword"
                  placeholder="学生姓名"
                  clearable
                />
              </el-form-item>
              <el-form-item label="年级">
                <el-input
                  v-model="studentSearchForm.gradeLevel"
                  placeholder="年级"
                  clearable
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleStudentSearch">搜索</el-button>
                <el-button @click="resetStudentSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 学生表格 -->
          <el-card class="table-card">
            <el-table :data="studentList" style="width: 100%" v-loading="loading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="realName" label="姓名" />
              <el-table-column prop="gradeLevel" label="年级" />
              <el-table-column prop="subjectsInterested" label="感兴趣科目" />
              <el-table-column prop="budgetRange" label="预算范围" />
              <el-table-column label="操作" width="150">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="handleEditStudent(scope.row)">
                    编辑
                  </el-button>
                  <el-button type="danger" size="small" @click="handleDeleteStudent(scope.row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <el-pagination
              :current-page="studentPagination.currentPage"
              :page-size="studentPagination.pageSize"
              :total="studentPagination.total"
              layout="total, prev, pager, next, jumper"
              @current-change="handleStudentPageChange"
              style="margin-top: 20px; text-align: right"
            />
          </el-card>
        </div>

        <!-- 教师管理 -->
        <div v-show="activeMenu === 'teachers'" class="content-panel">
          <h3>教师管理</h3>

          <!-- 搜索区域 -->
          <el-card class="search-card">
            <el-form :model="teacherSearchForm" inline>
              <el-form-item label="关键词">
                <el-input
                  v-model="teacherSearchForm.keyword"
                  placeholder="教师姓名"
                  clearable
                />
              </el-form-item>
              <el-form-item label="科目">
                <el-input
                  v-model="teacherSearchForm.subject"
                  placeholder="科目"
                  clearable
                />
              </el-form-item>
              <el-form-item label="认证状态">
                <el-select v-model="teacherSearchForm.isVerified" placeholder="请选择" clearable>
                  <el-option label="已认证" value="已认证" />
                  <el-option label="未认证" value="未认证" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleTeacherSearch">搜索</el-button>
                <el-button @click="resetTeacherSearch">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 教师表格 -->
          <el-card class="table-card">
            <el-table :data="teacherList" style="width: 100%" v-loading="loading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="realName" label="姓名" />
              <el-table-column prop="subjects" label="科目" />
              <el-table-column prop="teachingExperience" label="教学经验" width="100">
                <template #default="scope">
                  {{ scope.row.teachingExperience }}年
                </template>
              </el-table-column>
              <el-table-column prop="hourlyRate" label="时薪" width="100">
                <template #default="scope">
                  ¥{{ scope.row.hourlyRate }}
                </template>
              </el-table-column>
              <el-table-column prop="isVerified" label="认证状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.isVerified ? 'success' : 'warning'">
                    {{ scope.row.isVerified ? '已认证' : '未认证' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="handleEditTeacher(scope.row)">
                    编辑
                  </el-button>
                  <el-button
                    :type="scope.row.isVerified ? 'warning' : 'success'"
                    size="small"
                    @click="handleVerifyTeacher(scope.row, !scope.row.isVerified)"
                  >
                    {{ scope.row.isVerified ? '取消认证' : '认证' }}
                  </el-button>
                  <el-button type="danger" size="small" @click="handleDeleteTeacher(scope.row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <el-pagination
              :current-page="teacherPagination.currentPage"
              :page-size="teacherPagination.pageSize"
              :total="teacherPagination.total"
              layout="total, prev, pager, next, jumper"
              @current-change="handleTeacherPageChange"
              style="margin-top: 20px; text-align: right"
            />
          </el-card>
        </div>

        <!-- 科目管理 -->
        <div v-show="activeMenu === 'subjects'" class="content-panel">
          <h3>科目管理</h3>

          <!-- 搜索区域 -->
          <el-card class="search-card">
            <el-form :model="subjectSearchForm" inline>
              <el-form-item label="关键词">
                <el-input
                  v-model="subjectSearchForm.keyword"
                  placeholder="科目名称"
                  clearable
                />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="subjectSearchForm.status" placeholder="请选择" clearable>
                  <el-option label="启用" value="启用" />
                  <el-option label="停用" value="停用" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSubjectSearch">搜索</el-button>
                <el-button @click="resetSubjectSearch">重置</el-button>
                <el-button type="success" @click="handleAddSubject">添加科目</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 科目表格 -->
          <el-card class="table-card">
            <el-table :data="subjectList" style="width: 100%" v-loading="loading">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="name" label="科目名称" />
              <el-table-column prop="gradeLevels" label="适用年级" />
              <el-table-column prop="iconUrl" label="图标URL" />
              <el-table-column prop="isActive" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.isActive ? 'success' : 'danger'">
                    {{ scope.row.isActive ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="handleEditSubject(scope.row)">
                    编辑
                  </el-button>
                  <el-button type="danger" size="small" @click="handleDeleteSubject(scope.row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <el-pagination
              :current-page="subjectPagination.currentPage"
              :page-size="subjectPagination.pageSize"
              :total="subjectPagination.total"
              layout="total, prev, pager, next, jumper"
              @current-change="handleSubjectPageChange"
              style="margin-top: 20px; text-align: right"
            />
          </el-card>
        </div>

        <!-- 修改密码 -->
        <div v-show="activeMenu === 'password'" class="content-panel">
          <h3>修改密码</h3>
          <el-card class="password-card">
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
              style="max-width: 500px"
            >
              <el-form-item label="原密码" prop="currentPassword">
                <el-input
                  v-model="passwordForm.currentPassword"
                  type="password"
                  placeholder="请输入原密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请确认新密码"
                  show-password
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="loading"
                  @click="handlePasswordChange"
                >
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 学生对话框 -->
    <el-dialog
      v-model="studentDialogVisible"
      :title="studentDialogTitle"
      width="600px"
    >
      <el-form
        ref="studentFormRef"
        :model="studentForm"
        :rules="studentRules"
        label-width="120px"
      >
        <el-form-item label="学生姓名" prop="realName">
          <el-input v-model="studentForm.realName" placeholder="请输入学生姓名" />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="studentForm.gradeLevel" placeholder="如：小学三年级" />
        </el-form-item>
        <el-form-item label="感兴趣科目">
          <el-input v-model="studentForm.subjectsInterested" placeholder="如：数学,英语,物理" />
        </el-form-item>
        <el-form-item label="学习目标">
          <el-input
            v-model="studentForm.learningGoals"
            type="textarea"
            :rows="3"
            placeholder="请描述学习目标"
          />
        </el-form-item>
        <el-form-item label="教学风格偏好">
          <el-input v-model="studentForm.preferredTeachingStyle" placeholder="如：严格型,温和型,互动型" />
        </el-form-item>
        <el-form-item label="预算范围">
          <el-input v-model="studentForm.budgetRange" placeholder="如：100-200元/小时" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="studentDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="handleSaveStudent">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 教师对话框 -->
    <el-dialog
      v-model="teacherDialogVisible"
      :title="teacherDialogTitle"
      width="700px"
    >
      <el-form
        ref="teacherFormRef"
        :model="teacherForm"
        :rules="teacherRules"
        label-width="120px"
      >
        <el-form-item label="教师姓名" prop="realName">
          <el-input v-model="teacherForm.realName" placeholder="请输入教师姓名" />
        </el-form-item>
        <el-form-item label="教育背景">
          <el-input
            v-model="teacherForm.educationBackground"
            type="textarea"
            :rows="3"
            placeholder="请描述教育背景"
          />
        </el-form-item>
        <el-form-item label="教学经验">
          <el-input-number v-model="teacherForm.teachingExperience" :min="0" :max="50" />
          <span style="margin-left: 10px;">年</span>
        </el-form-item>
        <el-form-item label="专业领域">
          <el-input v-model="teacherForm.specialties" placeholder="如：高考数学,竞赛辅导" />
        </el-form-item>
        <el-form-item label="可教授科目">
          <el-input v-model="teacherForm.subjects" placeholder="如：数学,物理,化学" />
        </el-form-item>
        <el-form-item label="时薪">
          <el-input-number v-model="teacherForm.hourlyRate" :min="0" :precision="2" />
          <span style="margin-left: 10px;">元/小时</span>
        </el-form-item>
        <el-form-item label="个人介绍">
          <el-input
            v-model="teacherForm.introduction"
            type="textarea"
            :rows="4"
            placeholder="请描述个人介绍和教学理念"
          />
        </el-form-item>
        <el-form-item label="介绍视频URL">
          <el-input v-model="teacherForm.videoIntroUrl" placeholder="请输入视频URL（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="teacherDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="handleSaveTeacher">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 科目对话框 -->
    <el-dialog
      v-model="subjectDialogVisible"
      :title="subjectDialogTitle"
      width="500px"
    >
      <el-form
        ref="subjectFormRef"
        :model="subjectForm"
        :rules="subjectRules"
        label-width="80px"
      >
        <el-form-item label="科目名称" prop="name">
          <el-input v-model="subjectForm.name" placeholder="请输入科目名称" />
        </el-form-item>
        <el-form-item label="适用年级" prop="gradeLevels">
          <el-input
            v-model="subjectForm.gradeLevels"
            placeholder="如：小学,初中,高中"
          />
        </el-form-item>
        <el-form-item label="图标URL">
          <el-input
            v-model="subjectForm.iconUrl"
            placeholder="请输入图标URL（可选）"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="subjectForm.isActive"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="subjectDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="handleSaveSubject">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-center {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.admin-header {
  background: white;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.admin-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.admin-info {
  color: #666;
}

.admin-container {
  display: flex;
  min-height: calc(100vh - 60px);
}

.admin-sidebar {
  width: 200px;
  background: white;
  box-shadow: 2px 0 8px rgba(0, 21, 41, 0.08);
}

.admin-menu {
  height: 100%;
  border-right: none;
}

.admin-content {
  flex: 1;
  padding: 20px;
}

.content-panel h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #333;
}

.statistics-cards {
  margin-bottom: 20px;
}

.statistic-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.statistic-card:hover {
  transform: translateY(-2px);
}

.statistic {
  text-align: center;
  padding: 20px;
}

.statistic h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
  font-weight: normal;
}

.statistic-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.password-card {
  max-width: 600px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-container {
    flex-direction: column;
  }

  .admin-sidebar {
    width: 100%;
    height: auto;
  }

  .admin-menu {
    display: flex;
    height: auto;
  }

  .statistics-cards .el-col {
    margin-bottom: 20px;
  }
}

</style>
