<template>
  <div class="featured-course-management">
    <div class="header">
      <h2>精选课程管理</h2>
      <p class="description">管理在最新课程页面展示的精选课程</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button
        type="primary"
        @click="batchSetFeatured(true)"
        :disabled="selectedCourses.length === 0"
      >
        批量设为精选
      </el-button>
      <el-button
        type="warning"
        @click="batchSetFeatured(false)"
        :disabled="selectedCourses.length === 0"
      >
        批量取消精选
      </el-button>
      <el-button @click="refreshData">刷新</el-button>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="科目">
          <el-select v-model="filter.subjectId" placeholder="选择科目" clearable @change="fetchCourses">
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="选择状态" clearable @change="fetchCourses">
            <el-option label="活跃" value="active" />
            <el-option label="已下架" value="inactive" />
            <el-option label="已满员" value="full" />
            <el-option label="待审批" value="pending" />
          </el-select>
        </el-form-item>
        <el-form-item label="精选状态">
          <el-select v-model="filter.featured" placeholder="精选状态" clearable @change="fetchCourses">
            <el-option label="已精选" :value="true" />
            <el-option label="未精选" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="info" @click="resetFilter">重置筛选</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 课程列表 -->
    <div class="course-table">
      <el-table
        :data="courses"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="课程标题" min-width="200" />
        <el-table-column prop="teacherName" label="教师" width="120" />
        <el-table-column prop="subjectName" label="科目" width="100" />
        <el-table-column prop="courseTypeDisplay" label="类型" width="100" />
        <el-table-column prop="statusDisplay" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ row.statusDisplay }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="精选状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isFeatured ? 'success' : 'info'">
              {{ row.isFeatured ? '已精选' : '未精选' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.isFeatured"
              type="primary"
              size="small"
              @click="toggleFeatured(row)"
            >
              设为精选
            </el-button>
            <el-button
              v-else
              type="warning"
              size="small"
              @click="toggleFeatured(row)"
            >
              取消精选
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 精选课程预览 -->
    <div class="featured-preview">
      <h3>当前精选课程预览</h3>
      <div class="featured-courses-grid">
        <div
          v-for="course in featuredCourses"
          :key="course.id"
          class="featured-course-card"
        >
          <div class="course-title">{{ course.title }}</div>
          <div class="course-info">
            <span>{{ course.teacherName }}</span> |
            <span>{{ course.subjectName }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseAPI, subjectAPI } from '@/utils/api'

// 接口定义
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
  isFeatured: boolean
  createdAt: string
}

interface Subject {
  id: number
  name: string
}

// 响应式数据
const loading = ref(false)
const courses = ref<Course[]>([])
const featuredCourses = ref<Course[]>([])
const subjects = ref<Subject[]>([])
const selectedCourses = ref<Course[]>([])

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 筛选条件
const filter = reactive({
  subjectId: null as number | null,
  status: '',
  featured: null as boolean | null
})

// 获取课程列表
const fetchCourses = async () => {
  try {
    loading.value = true
    const params: any = {
      page: currentPage.value,
      size: pageSize.value
    }

    if (filter.subjectId) {
      params.subjectId = filter.subjectId
    }
    if (filter.status) {
      params.status = filter.status
    }

    const response = await courseAPI.getCourseList(params)
    if (response.success && response.data) {
      let courseList = response.data.courses || []

      // 根据精选状态筛选
      if (filter.featured !== null) {
        courseList = courseList.filter(course => course.isFeatured === filter.featured)
      }

      courses.value = courseList
      total.value = response.data.total || 0
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

// 获取精选课程预览
const fetchFeaturedCourses = async () => {
  try {
    const response = await courseAPI.getFeaturedCourses({ page: 1, size: 6 })
    if (response.success && response.data) {
      featuredCourses.value = response.data.courses || []
    }
  } catch (error) {
    console.error('获取精选课程失败:', error)
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

// 切换精选状态
const toggleFeatured = async (course: Course) => {
  try {
    const action = course.isFeatured ? '取消精选' : '设为精选'
    await ElMessageBox.confirm(
      `确定要${action}课程"${course.title}"吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await courseAPI.setCourseAsFeatured(course.id, !course.isFeatured)
    if (response.success) {
      ElMessage.success(`${action}成功`)
      await refreshData()
    } else {
      ElMessage.error(response.message || `${action}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('切换精选状态失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 批量设置精选状态
const batchSetFeatured = async (featured: boolean) => {
  try {
    const action = featured ? '设为精选' : '取消精选'
    await ElMessageBox.confirm(
      `确定要批量${action}选中的 ${selectedCourses.value.length} 个课程吗？`,
      '确认批量操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const courseIds = selectedCourses.value.map(course => course.id)
    const response = await courseAPI.batchSetFeaturedCourses(courseIds, featured)

    if (response.success) {
      ElMessage.success(`批量${action}成功`)
      await refreshData()
    } else {
      ElMessage.error(response.message || `批量${action}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量操作失败:', error)
      ElMessage.error('批量操作失败')
    }
  }
}

// 处理选择变化
const handleSelectionChange = (selection: Course[]) => {
  selectedCourses.value = selection
}

// 分页处理
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchCourses()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchCourses()
}

// 刷新数据
const refreshData = async () => {
  await Promise.all([
    fetchCourses(),
    fetchFeaturedCourses()
  ])
}

// 重置筛选条件
const resetFilter = () => {
  filter.subjectId = null
  filter.status = ''
  filter.featured = null
  currentPage.value = 1
  fetchCourses()
}

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  switch (status) {
    case 'active': return 'success'
    case 'inactive': return 'info'
    case 'full': return 'warning'
    case 'pending': return 'danger'
    default: return 'info'
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchSubjects()
  refreshData()
})
</script>

<style scoped>
.featured-course-management {
  padding: 20px;
}

.header {
  margin-bottom: 30px;
}

.header h2 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 24px;
}

.description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.action-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.filter-section {
  background-color: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.filter-form .el-select {
  width: 180px;
}

.course-table {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.featured-preview {
  margin-top: 30px;
  background-color: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
}

.featured-preview h3 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 18px;
}

.featured-courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
}

.featured-course-card {
  background-color: #fff;
  padding: 15px;
  border-radius: 6px;
  border-left: 4px solid #409eff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.course-title {
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  font-size: 14px;
}

.course-info {
  color: #666;
  font-size: 12px;
}

@media (max-width: 768px) {
  .featured-course-management {
    padding: 15px;
  }

  .action-bar {
    flex-direction: column;
    gap: 8px;
  }

  .filter-form {
    flex-direction: column;
    gap: 15px;
  }

  .filter-form .el-select {
    width: 100%;
  }

  .course-table {
    padding: 15px;
    overflow-x: auto;
  }

  .featured-courses-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
}
</style>
