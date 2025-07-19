<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useUserStore, type PasswordChangeRequest } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Setting, Document, DataBoard } from '@element-plus/icons-vue'

// 获取用户信息
const userStore = useUserStore()
const adminName = ref(userStore.username || '管理员')

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

// 用户信息相关
interface UserInfo {
  id: number
  username: string
  name: string
  email: string
  role: string
  status: string
  registerTime: string
}

const userList = ref<UserInfo[]>([
  {
    id: 1001,
    username: 'teacher001',
    name: '张老师',
    email: 'teacher001@example.com',
    role: '教师',
    status: '正常',
    registerTime: '2023-07-10 10:23'
  },
  {
    id: 1002,
    username: 'student002',
    name: '王明',
    email: 'student002@example.com',
    role: '学生',
    status: '正常',
    registerTime: '2023-07-10 14:35'
  },
  {
    id: 1003,
    username: 'teacher003',
    name: '李老师',
    email: 'teacher003@example.com',
    role: '教师',
    status: '待认证',
    registerTime: '2023-07-09 09:12'
  }
])

// 用户搜索
const userSearchForm = reactive({
  keyword: '',
  role: '',
  status: ''
})

// 用户对话框
const userDialogVisible = ref(false)
const userDialogTitle = ref('')
const userForm = reactive({
  id: 0,
  username: '',
  name: '',
  email: '',
  role: '学生',
  status: '正常',
  password: ''
})

const userFormRef = ref()

// 用户表单规则
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3 到 20 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 科目信息相关
interface SubjectInfo {
  id: number
  name: string
  description: string
  category: string
  status: string
  createTime: string
  teacherCount: number
}

const subjectList = ref<SubjectInfo[]>([
  {
    id: 1,
    name: '高中数学',
    description: '高中数学相关课程',
    category: '理科',
    status: '启用',
    createTime: '2023-01-15 10:00',
    teacherCount: 35
  },
  {
    id: 2,
    name: '高中英语',
    description: '高中英语相关课程',
    category: '文科',
    status: '启用',
    createTime: '2023-01-15 10:00',
    teacherCount: 28
  },
  {
    id: 3,
    name: '高中物理',
    description: '高中物理相关课程',
    category: '理科',
    status: '启用',
    createTime: '2023-01-15 10:00',
    teacherCount: 22
  },
  {
    id: 4,
    name: '初中语文',
    description: '初中语文相关课程',
    category: '文科',
    status: '停用',
    createTime: '2023-02-10 14:30',
    teacherCount: 15
  }
])

// 科目搜索
const subjectSearchForm = reactive({
  keyword: '',
  category: '',
  status: ''
})

// 科目对话框
const subjectDialogVisible = ref(false)
const subjectDialogTitle = ref('')
const subjectForm = reactive({
  id: 0,
  name: '',
  description: '',
  category: '理科',
  status: '启用'
})

const subjectFormRef = ref()

// 科目表单规则
const subjectRules = {
  name: [
    { required: true, message: '请输入科目名称', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入科目描述', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择科目分类', trigger: 'change' }
  ]
}

// 分页信息
const userPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

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

  // 先进行表单验证，使用 try-catch 包装验证逻辑
  let isFormValid = false
  try {
    isFormValid = await passwordFormRef.value.validate()
  } catch (validationError) {
    // 表单验证失败，不显示网络错误，直接返回
    console.log('表单验证失败:', validationError)
    return
  }

  if (!isFormValid) {
    // 表单验证失败，不显示网络错误，直接返回
    return
  }

  // 额外检查两次密码是否一致（双重保险）
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入密码不一致')
    return
  }

  // 开始网络请求
  loading.value = true

  try {
    // 调用后端API修改密码
    const response = await userStore.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })

    if (response.success) {
      ElMessage.success('密码修改成功')
      // 重置表单
      Object.assign(passwordForm, {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      })
      // 清空表单验证状态
      passwordFormRef.value.clearValidate()
    } else {
      ElMessage.error(response.message || '密码修改失败')
    }
  } catch (error: any) {
    console.error('网络请求失败:', error)
    // 只有在网络请求时才显示网络错误
    ElMessage.error(error.message || '网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 用户搜索
const handleUserSearch = () => {
  console.log('搜索用户:', userSearchForm)
  // 这里后续接入后端API
}

// 重置用户搜索
const resetUserSearch = () => {
  Object.assign(userSearchForm, {
    keyword: '',
    role: '',
    status: ''
  })
  handleUserSearch()
}

// 添加用户
const handleAddUser = () => {
  userDialogTitle.value = '添加用户'
  Object.assign(userForm, {
    id: 0,
    username: '',
    name: '',
    email: '',
    role: '学生',
    status: '正常',
    password: ''
  })
  userDialogVisible.value = true
}

// 编辑用户
const handleEditUser = (row: UserInfo) => {
  userDialogTitle.value = '编辑用户'
  Object.assign(userForm, {
    ...row,
    password: '' // 编辑时不显示密码
  })
  userDialogVisible.value = true
}

// 删除用户
const handleDeleteUser = (row: UserInfo) => {
  ElMessageBox.confirm(
    `确定要删除用户 "${row.name}" 吗？`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    const index = userList.value.findIndex(u => u.id === row.id)
    if (index !== -1) {
      userList.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  })
}

// 保存用户
const handleSaveUser = () => {
  userFormRef.value?.validate((valid: boolean) => {
    if (valid) {
      loading.value = true
      setTimeout(() => {
        loading.value = false
        if (userForm.id === 0) {
          // 添加用户
          const newUser = {
            ...userForm,
            id: Date.now(),
            registerTime: new Date().toLocaleString()
          }
          userList.value.unshift(newUser)
          ElMessage.success('用户添加成功')
        } else {
          // 编辑用户
          const index = userList.value.findIndex(u => u.id === userForm.id)
          if (index !== -1) {
            Object.assign(userList.value[index], userForm)
            ElMessage.success('用户信息更新成功')
          }
        }
        userDialogVisible.value = false
      }, 1000)
    }
  })
}

// 科目搜索
const handleSubjectSearch = () => {
  console.log('搜索科目:', subjectSearchForm)
  // 这里后续接入后端API
}

// 重置科目搜索
const resetSubjectSearch = () => {
  Object.assign(subjectSearchForm, {
    keyword: '',
    category: '',
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
    description: '',
    category: '理科',
    status: '启用'
  })
  subjectDialogVisible.value = true
}

// 编辑科目
const handleEditSubject = (row: SubjectInfo) => {
  subjectDialogTitle.value = '编辑科目'
  Object.assign(subjectForm, row)
  subjectDialogVisible.value = true
}

// 删除科目
const handleDeleteSubject = (row: SubjectInfo) => {
  ElMessageBox.confirm(
    `确定要删除科目 "${row.name}" 吗？`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    const index = subjectList.value.findIndex(s => s.id === row.id)
    if (index !== -1) {
      subjectList.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  })
}

// 保存科目
const handleSaveSubject = () => {
  subjectFormRef.value?.validate((valid: boolean) => {
    if (valid) {
      loading.value = true
      setTimeout(() => {
        loading.value = false
        if (subjectForm.id === 0) {
          // 添加科目
          const newSubject = {
            ...subjectForm,
            id: Date.now(),
            createTime: new Date().toLocaleString(),
            teacherCount: 0
          }
          subjectList.value.unshift(newSubject)
          ElMessage.success('科目添加成功')
        } else {
          // 编辑科目
          const index = subjectList.value.findIndex(s => s.id === subjectForm.id)
          if (index !== -1) {
            Object.assign(subjectList.value[index], subjectForm)
            ElMessage.success('科目信息更新成功')
          }
        }
        subjectDialogVisible.value = false
      }, 1000)
    }
  })
}

// 分页处理
const handleUserPageChange = (page: number) => {
  userPagination.currentPage = page
  // 这里后续接入后端API
}

const handleSubjectPageChange = (page: number) => {
  subjectPagination.currentPage = page
  // 这里后续接入后端API
}

// 页面初始化
onMounted(() => {
  userPagination.total = userList.value.length
  subjectPagination.total = subjectList.value.length
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
          <el-menu-item index="users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
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

        <!-- 用户管理 -->
        <div v-show="activeMenu === 'users'" class="content-panel">
          <h3>用户管理</h3>

          <!-- 搜索区域 -->
          <el-card class="search-card">
            <el-form :model="userSearchForm" inline>
              <el-form-item label="关键词">
                <el-input
                  v-model="userSearchForm.keyword"
                  placeholder="用户名/姓名/邮箱"
                  clearable
                />
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="userSearchForm.role" placeholder="请选择" clearable>
                  <el-option label="教师" value="教师" />
                  <el-option label="学生" value="学生" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="userSearchForm.status" placeholder="请选择" clearable>
                  <el-option label="正常" value="正常" />
                  <el-option label="冻结" value="冻结" />
                  <el-option label="待认证" value="待认证" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleUserSearch">搜索</el-button>
                <el-button @click="resetUserSearch">重置</el-button>
                <el-button type="success" @click="handleAddUser">添加用户</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 用户表格 -->
          <el-card class="table-card">
            <el-table :data="userList" style="width: 100%">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="name" label="姓名" />
              <el-table-column prop="email" label="邮箱" />
              <el-table-column prop="role" label="角色" width="80">
                <template #default="scope">
                  <el-tag :type="scope.row.role === '教师' ? 'primary' : 'success'">
                    {{ scope.row.role }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag
                    :type="scope.row.status === '正常' ? 'success' :
                           scope.row.status === '待认证' ? 'warning' : 'danger'"
                  >
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="registerTime" label="注册时间" />
              <el-table-column label="操作" width="150">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="handleEditUser(scope.row)">
                    编辑
                  </el-button>
                  <el-button type="danger" size="small" @click="handleDeleteUser(scope.row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
                         <el-pagination
               :current-page="userPagination.currentPage"
               :page-size="userPagination.pageSize"
               :total="userPagination.total"
               layout="total, prev, pager, next, jumper"
               @current-change="handleUserPageChange"
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
                  placeholder="科目名称/描述"
                  clearable
                />
              </el-form-item>
              <el-form-item label="分类">
                <el-select v-model="subjectSearchForm.category" placeholder="请选择" clearable>
                  <el-option label="理科" value="理科" />
                  <el-option label="文科" value="文科" />
                  <el-option label="艺术" value="艺术" />
                </el-select>
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
            <el-table :data="subjectList" style="width: 100%">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="name" label="科目名称" />
              <el-table-column prop="description" label="描述" />
              <el-table-column prop="category" label="分类" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.category === '理科' ? 'primary' : 'success'">
                    {{ scope.row.category }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.status === '启用' ? 'success' : 'danger'">
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="teacherCount" label="教师数量" width="100" />
              <el-table-column prop="createTime" label="创建时间" />
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

    <!-- 用户对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="userDialogTitle"
      width="500px"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色">
            <el-option label="教师" value="教师" />
            <el-option label="学生" value="学生" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="userForm.status" placeholder="请选择状态">
            <el-option label="正常" value="正常" />
            <el-option label="冻结" value="冻结" />
            <el-option label="待认证" value="待认证" />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="userForm.id === 0"
          label="密码"
          prop="password"
        >
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="handleSaveUser">
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
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="subjectForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入科目描述"
          />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="subjectForm.category" placeholder="请选择分类">
            <el-option label="理科" value="理科" />
            <el-option label="文科" value="文科" />
            <el-option label="艺术" value="艺术" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="subjectForm.status" placeholder="请选择状态">
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
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
