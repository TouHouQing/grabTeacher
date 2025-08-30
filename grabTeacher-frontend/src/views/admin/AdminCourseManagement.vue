<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, ArrowDown } from '@element-plus/icons-vue'
import { courseAPI, subjectAPI, teacherAPI, gradeApi, fileAPI } from '../../utils/api'

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
  createdAt: string
  featured: boolean
  price?: number
  startDate?: string
  endDate?: string
  personLimit?: number
  imageUrl?: string
}

interface Subject {
  id: number
  name: string
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
const loadingGrades = ref(false)
const courses = ref<Course[]>([])
const subjects = ref<Subject[]>([])
const teachers = ref<Teacher[]>([])
const availableGrades = ref([])
const selectedGrades = ref([])
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
  grade: ''
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
  price: null as number | null,
  startDate: '',
  endDate: '',
  personLimit: null as number | null,
  imageUrl: '' as string,
  // 临时选择的本地文件与预览
  _localImageFile: null as File | null,
  _localPreviewUrl: '' as string
})

// 选择封面图片（仅本地预览，不立即上传）
const onSelectCover = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files && input.files[0]
  if (!file) return
  // 简单校验：限制类型与大小（<= 10MB）
  if (!/^image\//.test(file.type)) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return
  }
  courseForm._localImageFile = file
  // 生成本地预览
  const reader = new FileReader()
  reader.onload = () => {
    courseForm._localPreviewUrl = reader.result as string
  }
  reader.readAsDataURL(file)
}

// 保存时上传封面图片到 OSS，并更新 imageUrl
const uploadCoverIfNeeded = async () => {
  if (!courseForm._localImageFile) return null
  // 预签名直传
  return await fileAPI.presignAndPut(courseForm._localImageFile, 'course-cover')
}

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
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (value !== null && value !== undefined) {
          if (value !== 90 && value !== 120) {
            callback(new Error('课程时长只能选择90分钟（一个半小时）或120分钟（俩小时），或者留空表示灵活时间'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  grade: [
    { required: true, message: '请输入适用年级', trigger: 'blur' }
  ],
  price: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        // 所有课程类型都可以设置价格
        if (value !== null && value !== undefined && value <= 0) {
          callback(new Error('价格必须大于0（不填表示价格面议）'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  startDate: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (courseForm.courseType === 'large_class') {
          if (!value) {
            callback(new Error('大班课必须设置开始日期'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  endDate: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (courseForm.courseType === 'large_class') {
          if (!value) {
            callback(new Error('大班课必须设置结束日期'))
          } else if (courseForm.startDate && value <= courseForm.startDate) {
            callback(new Error('结束日期必须晚于开始日期'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  personLimit: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (courseForm.courseType === 'large_class') {
          if (value !== null && value !== undefined && value <= 0) {
            callback(new Error('人数限制必须大于0（不填表示不限制）'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 课程类型选项
const courseTypeOptions = [
  { label: '一对一', value: 'one_on_one' },
  { label: '大班课', value: 'large_class' }
]

// 课程状态选项
const statusOptions = [
  { label: '待审批', value: 'pending' },
  { label: '可报名', value: 'active' },
  { label: '已下架', value: 'inactive' },
  { label: '已满员', value: 'full' }
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

// 获取年级列表
const fetchGrades = async () => {
  try {
    loadingGrades.value = true
    const response = await gradeApi.getAll()
    if (response.success && response.data) {
      availableGrades.value = response.data
    }
  } catch (error) {
    console.error('获取年级列表失败:', error)
  } finally {
    loadingGrades.value = false
  }
}

// 监听年级选择变化，同步到表单数据
watch(selectedGrades, (newGrades: string[]) => {
  courseForm.grade = newGrades.join(',')
}, { deep: true })

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
  courseForm.price = null
  courseForm.startDate = ''
  courseForm.endDate = ''
  courseForm.personLimit = null

  // 清空选中的年级
  selectedGrades.value = []
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
  courseForm.price = course.price || null
  courseForm.startDate = course.startDate || ''
  courseForm.endDate = course.endDate || ''
  courseForm.personLimit = course.personLimit || null

  // 将年级字符串转换为数组
  selectedGrades.value = course.grade ? course.grade.split(',').map(g => g.trim()) : []

  // 封面回显与清理本地预览
  courseForm.imageUrl = course.imageUrl || ''
  courseForm._localPreviewUrl = ''
  courseForm._localImageFile = null

  dialogTitle.value = '编辑课程'
  isEditing.value = true
  dialogVisible.value = true
}

// 保存课程
const saveCourse = async () => {
  try {
    loading.value = true

    // 若选择了新封面，先上传获取 URL
    let coverUrl: string | null = null
    try {
      coverUrl = await uploadCoverIfNeeded()
    } catch (e) {
      console.error('封面上传失败', e)
      ElMessage.error('封面上传失败，请重试')
      loading.value = false
      return
    }

    const courseData = {
      teacherId: courseForm.teacherId!,
      subjectId: courseForm.subjectId!,
      title: courseForm.title,
      description: courseForm.description,
      courseType: courseForm.courseType,
      durationMinutes: courseForm.durationMinutes,
      status: courseForm.status,
      grade: selectedGrades.value.join(','), // 将选中的年级转换为逗号分隔的字符串
      price: courseForm.price, // 所有课程类型都可以设置价格
      ...(courseForm.courseType === 'large_class' && {
        startDate: courseForm.startDate,
        endDate: courseForm.endDate,
        personLimit: courseForm.personLimit
      }),
      ...(coverUrl ? { imageUrl: coverUrl } : {})
    }

    let response: any
    if (isEditing.value && courseForm.id) {
      response = await courseAPI.update(courseForm.id, courseData)
    } else {
      response = await courseAPI.create(courseData)
    }

    if (response.success) {
      ElMessage.success(isEditing.value ? '课程更新成功' : '课程创建成功')
      // 回显最新封面
      if (response.data?.imageUrl) {
        courseForm.imageUrl = response.data.imageUrl
        courseForm._localPreviewUrl = ''
        courseForm._localImageFile = null
      }
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

// 切换精选状态
const onToggleFeatured = async (course: Course, val: any) => {
  const next = !!val
  const prev = !!course.featured
  course.featured = next
  try {
    const response = await courseAPI.setCourseAsFeatured(course.id, next)
    if (response.success) {
      ElMessage.success('已更新精选状态')
    } else {
      course.featured = prev
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error: any) {
    course.featured = prev
    ElMessage.error(error?.message || '操作失败')
  }
}

// 批准课程
const approveCourse = async (course: Course) => {
  try {
    await ElMessageBox.confirm(
      `确定要批准课程"${course.title}"吗？批准后课程将变为可报名状态。`,
      '确认批准',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success',
      }
    )

    await updateCourseStatus(course, 'active')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批准课程失败:', error)
    }
  }
}

// 拒绝课程
const rejectCourse = async (course: Course) => {
  try {
    await ElMessageBox.confirm(
      `确定要拒绝课程"${course.title}"吗？拒绝后课程将变为已下架状态。`,
      '确认拒绝',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    await updateCourseStatus(course, 'inactive')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拒绝课程失败:', error)
    }
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
    case 'pending': return 'warning'
    default: return 'info'
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchCourses()
  fetchSubjects()
  fetchTeachers()
  fetchGrades()
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
              :label="subject.name"
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
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="title" label="课程标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="授课教师" width="100" />
        <el-table-column prop="subjectName" label="科目" width="80" />
        <el-table-column prop="grade" label="适用年级" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.grade || '未设置' }}
          </template>
        </el-table-column>
        <el-table-column prop="courseTypeDisplay" label="类型" width="80" />
        <el-table-column prop="durationMinutes" label="时长" width="90">
          <template #default="{ row }">
            <template v-if="row.durationMinutes">
              {{ row.durationMinutes }}分钟
            </template>
            <template v-else>
              1.5/2小时
            </template>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="120">
          <template #default="{ row }">
            <span v-if="row.price">
              <template v-if="row.courseType === 'one_on_one'">
                {{ row.price }}M豆/时
              </template>
              <template v-else>
                {{ row.price }}M豆
              </template>
            </span>
            <span v-else class="text-muted">价格面议</span>
          </template>
        </el-table-column>
        <el-table-column label="开课时间" width="100">
          <template #default="{ row }">
            <span v-if="row.courseType === 'large_class'">
              {{ row.startDate || '未设置' }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="结课时间" width="100">
          <template #default="{ row }">
            <span v-if="row.courseType === 'large_class'">
              {{ row.endDate || '未设置' }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="人数限制" width="80">
          <template #default="{ row }">
            <span v-if="row.courseType === 'large_class'">
              {{ row.personLimit ? `${row.personLimit}人` : '不限制' }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ row.statusDisplay }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="精选" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.featured" @change="val => onToggleFeatured(row, val)" />
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

              <!-- 待审批状态显示审批按钮 -->
              <template v-if="row.status === 'pending'">
                <el-button size="small" type="success" @click="approveCourse(row)">
                  批准
                </el-button>
                <el-button size="small" type="warning" @click="rejectCourse(row)">
                  拒绝
                </el-button>
              </template>

              <!-- 其他状态显示状态切换下拉菜单 -->
              <template v-else>
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
                      <el-dropdown-item command="pending" :disabled="row.status === 'pending'">
                        设为待审批
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>

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
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>

        <!-- 年级字段 -->
        <el-form-item label="适用年级" prop="grade" required>
          <el-select
            v-model="selectedGrades"
            multiple
            placeholder="请选择适用年级"
            style="width: 100%;"
            :loading="loadingGrades"
          >
            <el-option
              v-for="grade in availableGrades"
              :key="grade.id"
              :label="grade.gradeName"
              :value="grade.gradeName"
            />
          </el-select>
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            可以选择多个年级，系统会自动保存关联关系
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

        <!-- 课程封面（本地预览，保存时上传） -->
        <el-form-item label="课程封面">
          <div class="cover-upload">
            <div class="cover-preview" v-if="courseForm._localPreviewUrl || courseForm.imageUrl">
              <img :src="courseForm._localPreviewUrl || courseForm.imageUrl" alt="课程封面预览" />
            </div>
            <div class="cover-actions">
              <input type="file" accept="image/*" @change="onSelectCover" />
              <div class="tip">支持 jpg/png，最大10MB。</div>
            </div>
          </div>
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
          <el-select
            v-model="courseForm.durationMinutes"
            placeholder="90或120分钟"
            style="width: 200px"
            clearable
          >
            <el-option
              :value="null"
              label="90或120分钟"
            />
            <el-option
              :value="90"
              label="90分钟"
            />
            <el-option
              :value="120"
              label="120分钟"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="课程价格" prop="price">
          <el-input-number
            v-model="courseForm.price"
            :min="0"
            :precision="2"
            :step="10"
            style="width: 200px"
            placeholder="不填表示可定制价格"
            clearable
          />
          <span style="margin-left: 10px; color: #909399;" v-if="courseForm.courseType === 'one_on_one'">
            M豆/小时（不填表示价格面议）
          </span>
          <span style="margin-left: 10px; color: #909399;" v-else>
            M豆/总课程（不填表示价格面议）
          </span>
        </el-form-item>

        <!-- 大班课专用字段 -->
        <template v-if="courseForm.courseType === 'large_class'">
          <el-divider content-position="left">
            <span style="color: #409eff; font-weight: 500;">大班课设置</span>
          </el-divider>

          <el-form-item label="开始日期" prop="startDate" required>
            <el-date-picker
              v-model="courseForm.startDate"
              type="date"
              placeholder="请选择开始日期"
              style="width: 200px"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="(time) => time.getTime() < Date.now() - 24 * 60 * 60 * 1000"
            />
            <span style="margin-left: 10px; color: #909399;">大班课必须设置开始日期</span>
          </el-form-item>

          <el-form-item label="结束日期" prop="endDate" required>
            <el-date-picker
              v-model="courseForm.endDate"
              type="date"
              placeholder="请选择结束日期"
              style="width: 200px"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="(time) => {
                const today = Date.now() - 24 * 60 * 60 * 1000
                const startTime = courseForm.startDate ? new Date(courseForm.startDate).getTime() : 0
                return time.getTime() < Math.max(today, startTime)
              }"
            />
            <span style="margin-left: 10px; color: #909399;">必须晚于开始日期</span>
          </el-form-item>

          <el-form-item label="人数限制" prop="personLimit">
            <el-input-number
              v-model="courseForm.personLimit"
              :min="1"
              :max="1000"
              :step="1"
              style="width: 200px"
              placeholder="不填表示不限制"
              clearable
            />
            <span style="margin-left: 10px; color: #909399;">人（不填表示不限制人数）</span>
          </el-form-item>
        </template>

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


  /* 课程封面上传样式 */
  .cover-upload { display: flex; align-items: center; gap: 12px; }
  .cover-preview img { width: 120px; height: 80px; object-fit: cover; border-radius: 6px; border: 1px solid #ebeef5; }
  .cover-actions .tip { color: #909399; font-size: 12px; margin-top: 6px; }

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

.text-muted {
  color: #c0c4cc;
  font-style: italic;
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
