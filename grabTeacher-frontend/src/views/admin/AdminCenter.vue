<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useUserStore, type PasswordChangeRequest } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Setting, Document, DataBoard } from '@element-plus/icons-vue'
import { subjectAPI } from '../../utils/api'

// 获取用户信息
const userStore = useUserStore()
const adminName = ref('管理员')

// 当前活跃菜单
const activeMenu = ref('dashboard')

// 页面加载状态
const loading = ref(false)

// 统计数据
const statistics = ref({
  totalUsers: 2587,
  totalTeachers: 368,
  totalStudents: 2219,
  totalCourses: 298,
  totalSubjects: 15
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
      // 添加科目
      result = await subjectAPI.create(subjectData)
    } else {
      // 编辑科目
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
  await loadSubjectList()
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
          <el-menu-item index="password">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </el-menu-item>
          <el-menu-item index="subjects">
            <el-icon><Document /></el-icon>
            <span>科目管理</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 主要内容区域 -->
      <div class="admin-content">
        <!-- 数据概览 -->
        <div v-show="activeMenu === 'dashboard'" class="content-panel">
          <h3>数据概览</h3>
          <el-row :gutter="20" class="statistics-cards">
            <el-col :xs="24" :sm="12" :lg="8">
              <el-card class="statistic-card">
                <div class="statistic">
                  <h4>用户总数</h4>
                  <div class="statistic-value">{{ statistics.totalUsers }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="8">
              <el-card class="statistic-card">
                <div class="statistic">
                  <h4>教师数量</h4>
                  <div class="statistic-value">{{ statistics.totalTeachers }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="8">
              <el-card class="statistic-card">
                <div class="statistic">
                  <h4>学生数量</h4>
                  <div class="statistic-value">{{ statistics.totalStudents }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="8">
              <el-card class="statistic-card">
                <div class="statistic">
                  <h4>课程数量</h4>
                  <div class="statistic-value">{{ statistics.totalCourses }}</div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="8">
              <el-card class="statistic-card">
                <div class="statistic">
                  <h4>科目数量</h4>
                  <div class="statistic-value">{{ statistics.totalSubjects }}</div>
                </div>
              </el-card>
            </el-col>
          </el-row>
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
      </div>
    </div>

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
