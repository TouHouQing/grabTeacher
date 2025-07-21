<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, ArrowDown } from '@element-plus/icons-vue'
import { courseAPI, subjectAPI, teacherAPI } from '../../utils/api'

// 课程接口定义
interface Course {
  id: number
  teacherId: number
  teacherName: string
  subjectId: number
  subjectName: string
  title: string
  description?: string
  courseType: string
  courseTypeDisplay: string
  durationMinutes: number
  status: string
  statusDisplay: string
  grade?: string
  gender?: string
  createdAt: string
}

interface Subject {
  id: number
  name: string
  gradeLevels?: string
  iconUrl?: string
  isActive: boolean
}

interface Teacher {
  id: number
  realName: string
  subjects?: string
  isVerified: boolean
}

// 响应式数据
const loading = ref(false)
const courses = ref<Course[]>([])
const subjects = ref<Subject[]>([])
const teachers = ref<Teacher[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEditing = ref(false)

// 分页数据
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
  pages: 0
})

// 搜索和筛选
const searchForm = reactive({
  keyword: '',
  subjectId: null as number | null,
  teacherId: null as number | null,
  status: '',
  courseType: '',
  grade: '',
  gender: ''
})

// 课程表单
const courseForm = reactive({
  id: null as number | null,
  teacherId: null as number | null,
  subjectId: null as number | null,
  title: '',
  description: '',
  courseType: 'one_on_one',
  durationMinutes: 120,
  status: 'active',
  grade: '',
  gender: '不限'
})

// 表单验证规则
const formRules = {
  teacherId: [
    { required: true, message: '请选择教师', trigger: 'change' }
  ],
  subjectId: [
    { required: true, message: '请选择科目', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入课程标题', trigger: 'blur' },
    { min: 2, max: 200, message: '课程标题长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  courseType: [
    { required: true, message: '请选择课程类型', trigger: 'change' }
  ],
  durationMinutes: [
    { required: true, message: '请输入课程时长', trigger: 'blur' },
    { type: 'number', min: 1, message: '课程时长必须大于0分钟', trigger: 'blur' }
  ],
  grade: [
    { required: true, message: '请输入适用年级', trigger: 'blur' }
  ]
}

// 课程类型选项
const courseTypeOptions = [
  { label: '一对一', value: 'one_on_one' },
  { label: '大班课', value: 'large_class' }
]

// 课程状态选项
const statusOptions = [
  { label: '可报名', value: 'active' },
  { label: '已下架', value: 'inactive' },
  { label: '已满员', value: 'full' }
]

// 性别选项
const genderOptions = [
  { label: '不限', value: '不限' },
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

// 获取课程列表
const fetchCourses = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.current,
      size: pagination.size,
      ...searchForm
    }

    const response = await courseAPI.getList(params)
    if (response.success && response.data) {
      courses.value = response.data.courses || []
      pagination.total = response.data.total || 0
      pagination.current = response.data.current || 1
      pagination.size = response.data.size || 10
      pagination.pages = response.data.pages || 0
    } else {
      ElMessage.error(response.message || '获取课程列表失败')
    }
  } catch (error) {
    console.error('获取课程列表失败:', error)
    ElMessage.error('获取课程列表失败')
  } finally {
    loading.value = false
  }
}

// 获取科目列表
const fetchSubjects = async () => {
  try {
    const response = await subjectAPI.getActiveSubjects()
    if (response.success) {
      subjects.value = response.data || []
    }
  } catch (error) {
    console.error('获取科目列表失败:', error)
  }
}

// 获取教师列表
const fetchTeachers = async () => {
  try {
    const response = await teacherAPI.getList({ page: 1, size: 1000, isVerified: true })
    if (response.success && response.data) {
      teachers.value = response.data.records || []
    }
  } catch (error) {
    console.error('获取教师列表失败:', error)
  }
}

// 重置表单
const resetForm = () => {
  courseForm.id = null
  courseForm.teacherId = null
  courseForm.subjectId = null
  courseForm.title = ''
  courseForm.description = ''
  courseForm.courseType = 'one_on_one'
  courseForm.durationMinutes = 120
  courseForm.status = 'active'
  courseForm.grade = ''
  courseForm.gender = '不限'
}

// 打开新增对话框
const openAddDialog = () => {
  resetForm()
  dialogTitle.value = '新增课程'
  isEditing.value = false
  dialogVisible.value = true
}

// 打开编辑对话框
const openEditDialog = (course: Course) => {
  courseForm.id = course.id
  courseForm.teacherId = course.teacherId
  courseForm.subjectId = course.subjectId
  courseForm.title = course.title
  courseForm.description = course.description || ''
  courseForm.courseType = course.courseType
  courseForm.durationMinutes = course.durationMinutes
  courseForm.status = course.status
  courseForm.grade = course.grade || ''
  courseForm.gender = course.gender || '不限'

  dialogTitle.value = '编辑课程'
  isEditing.value = true
  dialogVisible.value = true
}

// 保存课程
const saveCourse = async () => {
  try {
    loading.value = true

    const courseData = {
      teacherId: courseForm.teacherId!,
      subjectId: courseForm.subjectId!,
      title: courseForm.title,
      description: courseForm.description,
      courseType: courseForm.courseType,
      durationMinutes: courseForm.durationMinutes,
      status: courseForm.status,
      grade: courseForm.grade,
      gender: courseForm.gender
    }

    let response: any
    if (isEditing.value && courseForm.id) {
      response = await courseAPI.update(courseForm.id, courseData)
    } else {
      response = await courseAPI.create(courseData)
    }

    if (response.success) {
      ElMessage.success(isEditing.value ? '课程更新成功' : '课程创建成功')
      dialogVisible.value = false
      await fetchCourses()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    console.error('保存课程失败:', error)
    ElMessage.error('保存课程失败')
  } finally {
    loading.value = false
  }
}

// 删除课程
const deleteCourse = async (course: Course) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除课程"${course.title}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    loading.value = true
    const response = await courseAPI.delete(course.id)

    if (response.success) {
      ElMessage.success('课程删除成功')
      await fetchCourses()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除课程失败:', error)
      ElMessage.error('删除课程失败')
    }
  } finally {
    loading.value = false
  }
}

// 更新课程状态
const updateCourseStatus = async (course: Course, newStatus: string) => {
  try {
    loading.value = true
    const response = await courseAPI.updateStatus(course.id, newStatus)

    if (response.success) {
      ElMessage.success('状态更新成功')
      await fetchCourses()
    } else {
      ElMessage.error(response.message || '状态更新失败')
    }
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('状态更新失败')
  } finally {
    loading.value = false
  }
}

// 搜索课程
const searchCourses = () => {
  pagination.current = 1
  fetchCourses()
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.subjectId = null
  searchForm.teacherId = null
  searchForm.status = ''
  searchForm.courseType = ''
  searchForm.grade = ''
  searchForm.gender = ''
  pagination.current = 1
  fetchCourses()
}

// 分页变化
const handlePageChange = (page: number) => {
  pagination.current = page
  fetchCourses()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.current = 1
  fetchCourses()
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  switch (status) {
    case 'active': return 'success'
    case 'inactive': return 'info'
    case 'full': return 'warning'
    default: return 'info'
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchCourses()
  fetchSubjects()
  fetchTeachers()
})
</script>

<template>
  <div class="admin-course-management">
    <div class="header">
      <h2>课程管理</h2>
    </div>

    <!-- 搜索筛选区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索课程标题或科目"
            :prefix-icon="Search"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="教师">
          <el-select v-model="searchForm.teacherId" placeholder="选择教师" clearable style="width: 150px">
            <el-option
              v-for="teacher in teachers"
              :key="teacher.id"
              :label="teacher.realName"
              :value="teacher.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科目">
          <el-select v-model="searchForm.subjectId" placeholder="选择科目" clearable style="width: 200px">
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="`${subject.name} (${subject.gradeLevels || '全年级'})`"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.courseType" placeholder="选择类型" clearable style="width: 120px">
            <el-option
              v-for="option in courseTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-input
            v-model="searchForm.grade"
            placeholder="搜索年级"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="searchForm.gender" placeholder="选择性别" clearable style="width: 120px">
            <el-option
              v-for="option in genderOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="searchCourses">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
        <el-form-item class="add-button-item">
          <el-button type="primary" :icon="Plus" @click="openAddDialog">
            新增课程
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 课程表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="courses"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="title" label="课程标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="授课教师" width="100" />
        <el-table-column prop="subjectName" label="科目" width="80" />
        <el-table-column prop="grade" label="适用年级" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.grade || '未设置' }}
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="适合性别" width="90">
          <template #default="{ row }">
            {{ row.gender || '未设置' }}
          </template>
        </el-table-column>
        <el-table-column prop="courseTypeDisplay" label="类型" width="80" />
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="90" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ row.statusDisplay }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="120">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleDateString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right" align="center">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button size="small" :icon="Edit" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-dropdown @command="(status) => updateCourseStatus(row, status)">
                <el-button size="small" type="primary">
                  状态<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="active" :disabled="row.status === 'active'">
                      设为可报名
                    </el-dropdown-item>
                    <el-dropdown-item command="inactive" :disabled="row.status === 'inactive'">
                      设为已下架
                    </el-dropdown-item>
                    <el-dropdown-item command="full" :disabled="row.status === 'full'">
                      设为已满员
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button size="small" type="danger" :icon="Delete" @click="deleteCourse(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          :current-page="pagination.current"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑课程对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="courseForm"
        :rules="formRules"
        ref="formRef"
        label-width="100px"
      >
        <el-form-item label="授课教师" prop="teacherId">
          <el-select v-model="courseForm.teacherId" placeholder="请选择教师" style="width: 100%">
            <el-option
              v-for="teacher in teachers"
              :key="teacher.id"
              :label="teacher.realName"
              :value="teacher.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="科目" prop="subjectId">
          <el-select v-model="courseForm.subjectId" placeholder="请选择科目" style="width: 100%">
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="`${subject.name} (${subject.gradeLevels || '全年级'})`"
              :value="subject.id"
            >
              <div style="display: flex; justify-content: space-between;">
                <span>{{ subject.name }}</span>
                <span style="color: #8492a6; font-size: 12px;">{{ subject.gradeLevels || '全年级' }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 年级字段 -->
        <el-form-item label="适用年级" prop="grade" required>
          <el-input
            v-model="courseForm.grade"
            placeholder="请输入适用年级，如：小学一年级,小学二年级"
            maxlength="100"
            show-word-limit
            clearable
            style="width: 100%;"
          />
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            多个年级请用逗号分隔，如：小学一年级,小学二年级
          </div>
        </el-form-item>

        <!-- 性别字段 -->
        <el-form-item label="适合性别">
          <el-radio-group v-model="courseForm.gender">
            <el-radio
              v-for="option in genderOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio>
          </el-radio-group>
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            选择课程适合的学生性别
          </div>
        </el-form-item>

        <el-form-item label="课程标题" prop="title">
          <el-input
            v-model="courseForm.title"
            placeholder="请输入课程标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="课程描述">
          <el-input
            v-model="courseForm.description"
            type="textarea"
            placeholder="请输入课程描述"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="课程类型" prop="courseType">
          <el-radio-group v-model="courseForm.courseType">
            <el-radio
              v-for="option in courseTypeOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="课程时长" prop="durationMinutes">
          <el-input-number
            v-model="courseForm.durationMinutes"
            :min="1"
            :max="480"
            :step="30"
            style="width: 200px"
          />
          <span style="margin-left: 10px; color: #909399;">分钟</span>
        </el-form-item>

        <el-form-item label="课程状态">
          <el-radio-group v-model="courseForm.status">
            <el-radio
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveCourse" :loading="loading">
            {{ isEditing ? '更新' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-course-management {
  padding: 20px;
  padding-top: 5px; /* 增加顶部间距，避免被导航栏遮挡 */
}

.header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  min-height: 400px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 统计卡片样式 */
.statistics-section {
  margin-bottom: 20px;
}

.statistics-cards {
  margin-bottom: 0;
}

.statistic-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.statistic-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.statistic-card.active {
  border-left: 4px solid #67c23a;
}

.statistic-card.inactive {
  border-left: 4px solid #909399;
}

.statistic-card.full {
  border-left: 4px solid #e6a23c;
}

.statistic-content {
  text-align: center;
  padding: 10px;
}

.statistic-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.statistic-label {
  font-size: 14px;
  color: #606266;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.add-button-item {
  margin-left: auto;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .admin-course-management {
    padding: 10px;
  }

  .header h2 {
    font-size: 18px;
  }

  .statistic-number {
    font-size: 24px;
  }

  .statistic-label {
    font-size: 12px;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-form .el-form-item {
    margin-bottom: 12px;
    width: 100%;
  }

  .search-form .el-form-item .el-input,
  .search-form .el-form-item .el-select {
    width: 100% !important;
  }

  .add-button-item {
    margin-left: 0;
    width: 100%;
  }

  .add-button-item .el-button {
    width: 100%;
  }

  .table-card {
    overflow-x: auto;
  }

  .el-table {
    min-width: 1200px;
  }

  .pagination-wrapper {
    padding: 10px;
  }

  .pagination-wrapper .el-pagination {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .statistic-number {
    font-size: 20px;
  }

  .search-form .el-form-item .el-form-item__label {
    width: 60px !important;
    font-size: 14px;
  }

  .el-table .el-button {
    padding: 4px 8px;
    font-size: 12px;
  }
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
</style>
