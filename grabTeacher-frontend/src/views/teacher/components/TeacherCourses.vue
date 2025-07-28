<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, View, Search, Refresh } from '@element-plus/icons-vue'
import { courseAPI, subjectAPI, gradeApi } from '@/utils/api'

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
}

interface Subject {
  id: number
  name: string
  iconUrl?: string
  isActive: boolean
}

// 响应式数据
const loading = ref(false)
const courses = ref<Course[]>([])
const subjects = ref<Subject[]>([])
const availableGrades = ref<any[]>([])
const loadingGrades = ref(false)
const selectedGrades = ref<string[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEditing = ref(false)

// 搜索和筛选
const searchForm = reactive({
  keyword: '',
  subjectId: null as number | null,
  status: '',
  courseType: ''
})

// 课程表单
const courseForm = reactive({
  id: null as number | null,
  subjectId: null as number | null,
  title: '',
  description: '',
  courseType: 'one_on_one',
  durationMinutes: 120,
  status: 'active',
  grade: ''
})

// 表单验证规则
const formRules = {
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
    {
      required: true,
      message: '请选择适用年级',
      trigger: 'change',
      validator: (rule: any, value: any, callback: any) => {
        if (selectedGrades.value.length === 0) {
          callback(new Error('请选择适用年级'))
        } else {
          callback()
        }
      }
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

// 计算属性
const filteredCourses = computed(() => {
  let result = courses.value

  if (searchForm.keyword) {
    result = result.filter(course =>
      course.title.toLowerCase().includes(searchForm.keyword.toLowerCase()) ||
      course.subjectName.toLowerCase().includes(searchForm.keyword.toLowerCase())
    )
  }

  if (searchForm.subjectId) {
    result = result.filter(course => course.subjectId === searchForm.subjectId)
  }

  if (searchForm.status) {
    result = result.filter(course => course.status === searchForm.status)
  }

  if (searchForm.courseType) {
    result = result.filter(course => course.courseType === searchForm.courseType)
  }

  return result
})

// 获取课程列表
const fetchCourses = async () => {
  try {
    loading.value = true
    const response = await courseAPI.getMyCourses()
    if (response.success) {
      courses.value = response.data || []
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

// 获取年级列表
const fetchGrades = async () => {
  try {
    loadingGrades.value = true
    const response = await gradeApi.getAllPublic()
    if (response.success && response.data) {
      availableGrades.value = response.data
    }
  } catch (error) {
    console.error('获取年级列表失败:', error)
  } finally {
    loadingGrades.value = false
  }
}

// 重置表单
const resetForm = () => {
  courseForm.id = null
  courseForm.subjectId = null
  courseForm.title = ''
  courseForm.description = ''
  courseForm.courseType = 'one_on_one'
  courseForm.durationMinutes = 120
  courseForm.status = 'pending' // 教师创建的课程始终为待审批状态
  courseForm.grade = ''

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
  courseForm.subjectId = course.subjectId
  courseForm.title = course.title
  courseForm.description = course.description || ''
  courseForm.courseType = course.courseType
  courseForm.durationMinutes = course.durationMinutes
  courseForm.status = 'pending' // 教师编辑课程时状态重置为待审批
  courseForm.grade = course.grade || ''

  // 将年级字符串转换为数组
  selectedGrades.value = course.grade ? course.grade.split(',').map(g => g.trim()) : []

  dialogTitle.value = '编辑课程'
  isEditing.value = true
  dialogVisible.value = true
}

// 保存课程
const saveCourse = async () => {
  try {
    loading.value = true

    const courseData = {
      subjectId: courseForm.subjectId!,
      title: courseForm.title,
      description: courseForm.description,
      courseType: courseForm.courseType,
      durationMinutes: courseForm.durationMinutes,
      status: 'pending', // 教师创建/编辑的课程始终为待审批状态
      grade: selectedGrades.value.join(',') // 将选中的年级转换为逗号分隔的字符串
    }

    let response
    if (isEditing.value && courseForm.id) {
      response = await courseAPI.update(courseForm.id, courseData)
    } else {
      response = await courseAPI.create(courseData)
    }

    if (response.success) {
      const message = isEditing.value
        ? '课程更新成功，已提交审批，请等待管理员审核'
        : '课程创建成功，已提交审批，请等待管理员审核'
      ElMessage.success(message)
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

// 教师不能直接更新课程状态，已移除相关功能

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.subjectId = null
  searchForm.status = ''
  searchForm.courseType = ''
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
  fetchGrades()
})
</script>

<template>
  <div class="teacher-courses">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>我的课程</h2>
    </div>

    <!-- 搜索筛选区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索课程标题或科目"
            :prefix-icon="Search"
            clearable
            style="width: 200px"
          />
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
        <el-form-item>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
        <el-form-item class="add-button-item">
          <el-button type="primary" :icon="Plus" @click="openAddDialog">
            新增课程
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 课程列表 -->
    <el-card class="course-list" shadow="never">
      <div v-loading="loading">
        <div v-if="filteredCourses.length === 0" class="empty-state">
          <el-empty description="暂无课程数据" />
        </div>
        <div v-else class="course-grid">
          <div
            v-for="course in filteredCourses"
            :key="course.id"
            class="course-card"
          >
            <el-card shadow="hover">
              <div class="course-header">
                <h3 class="course-title">{{ course.title }}</h3>
                <div class="course-status">
                  <el-tag :type="getStatusTagType(course.status)">
                    {{ course.statusDisplay }}
                  </el-tag>
                </div>
              </div>

              <div class="course-info">
                <div class="info-item">
                  <span class="label">科目：</span>
                  <span class="value">{{ course.subjectName }}</span>
                </div>
                <div class="info-item" v-if="course.grade">
                  <span class="label">年级：</span>
                  <span class="value">{{ course.grade }}</span>
                </div>
                <div class="info-item">
                  <span class="label">类型：</span>
                  <span class="value">{{ course.courseTypeDisplay }}</span>
                </div>
                <div class="info-item">
                  <span class="label">时长：</span>
                  <span class="value">{{ course.durationMinutes }}分钟</span>
                </div>
                <div class="info-item">
                  <span class="label">创建时间：</span>
                  <span class="value">{{ new Date(course.createdAt).toLocaleDateString() }}</span>
                </div>
                <div class="info-item">
                  <span class="label">状态：</span>
                  <el-tag :type="getStatusTagType(course.status)" size="small">
                    {{ course.statusDisplay }}
                  </el-tag>
                  <span v-if="course.status === 'pending'" class="status-tip">
                    （等待管理员审批）
                  </span>
                </div>
              </div>

              <div v-if="course.description" class="course-description">
                <p>{{ course.description }}</p>
              </div>

              <div class="course-actions">
                <el-button size="small" :icon="Edit" @click="openEditDialog(course)">
                  编辑
                </el-button>
                <!-- 教师不能直接管理课程状态，需要通过管理员审批 -->
                <el-button size="small" type="danger" :icon="Delete" @click="deleteCourse(course)">
                  删除
                </el-button>
              </div>
            </el-card>
          </div>
        </div>
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

        <el-form-item label="适用年级" prop="grade">
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

        <!-- 教师不能选择课程状态，新增和编辑时都会自动设置为待审批状态 -->
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
.teacher-courses {
  padding: 0;
  height: 100%;
  overflow: hidden;
}

.page-header {
  padding: 20px 20px 0 20px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.search-card {
  margin: 0 20px 20px 20px;
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

.course-list {
  margin: 0 20px 20px 20px;
  min-height: 400px;
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  padding: 20px;
}

.course-card {
  height: 100%;
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.course-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  margin-right: 10px;
}

.course-info {
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
}

.info-item .label {
  width: 80px;
  color: #909399;
  font-size: 14px;
}

.info-item .value {
  color: #606266;
  font-size: 14px;
  flex: 1;
}

.course-description {
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.course-description p {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.status-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}

.course-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
