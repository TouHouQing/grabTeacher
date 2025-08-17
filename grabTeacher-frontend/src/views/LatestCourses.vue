<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import ContactUs from '../components/ContactUs.vue'
import { ElMessage } from 'element-plus'
import {
  Timer,
  User,
  Star,
  Calendar,
  Refresh,
  Promotion,
  GoodsFilled,
  Present,
  Reading,
  School
} from '@element-plus/icons-vue'
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

// 科目接口定义
interface Subject {
  id: number
  name: string
  iconUrl?: string
  isActive: boolean
}

// 年级接口定义
interface Grade {
  id: number
  gradeName: string
  description?: string
}

// 搜索过滤条件
const filter = reactive({
  subject: '',
  grade: ''
})

// 分页参数
const currentPage = ref(1)
const pageSize = ref(6)
const total = ref(0)

// 数据状态
const loading = ref(false)
const courses = ref<Course[]>([])
const subjects = ref<Subject[]>([])
const grades = ref<Grade[]>([])

// 获取课程列表
const fetchCourses = async () => {
  try {
    loading.value = true
    const params: any = {
      page: currentPage.value,
      size: pageSize.value
    }

    // 添加筛选条件
    if (filter.subject) {
      const selectedSubject = subjects.value.find(s => s.name === filter.subject)
      if (selectedSubject) {
        params.subjectId = selectedSubject.id
      }
    }

    if (filter.grade) {
      params.grade = filter.grade
    }

    const response = await courseAPI.getPublicLatestCourses(params)
    if (response.success && response.data) {
      courses.value = response.data.courses || []
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
    const response = await gradeApi.getAllPublic()
    if (response.success) {
      grades.value = response.data || []
    }
  } catch (error) {
    console.error('获取年级列表失败:', error)
  }
}

// 筛选方法
const handleFilter = () => {
  currentPage.value = 1
  fetchCourses()
}

// 重置筛选
const resetFilter = () => {
  filter.subject = ''
  filter.grade = ''
  currentPage.value = 1
  fetchCourses()
}

// 分页变化处理
const handlePageChange = () => {
  fetchCourses()
}

// 页面大小变化处理
const handleSizeChange = () => {
  currentPage.value = 1
  fetchCourses()
}

// 格式化时长显示
const formatDuration = (minutes: number) => {
  if (minutes < 60) {
    return `${minutes}分钟`
  } else {
    const hours = Math.floor(minutes / 60)
    const remainingMinutes = minutes % 60
    return remainingMinutes > 0 ? `${hours}小时${remainingMinutes}分钟` : `${hours}小时`
  }
}

// 格式化日期显示
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

// 判断是否为最新课程（7天内创建的）
const isNewCourse = (createdAt: string) => {
  const courseDate = new Date(createdAt)
  const now = new Date()
  const diffTime = now.getTime() - courseDate.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays <= 7
}

// 获取默认课程图片
const getDefaultCourseImage = (subjectName: string) => {
  const imageMap = {
    '数学': '@/assets/pictures/math1.jpeg',
    '英语': '@/assets/pictures/english1.jpeg',
    '物理': '@/assets/pictures/physics1.jpeg',
    '化学': '@/assets/pictures/chemistry1.jpeg',
    '生物': '@/assets/pictures/biology1.jpeg',
    '历史': '@/assets/pictures/history1.jpeg',
    '地理': '@/assets/pictures/courseBackground1.jpeg',
    '政治': '@/assets/pictures/courseBackground1.jpeg',
    '语文': '@/assets/pictures/courseBackground1.jpeg'
  }
  return imageMap[subjectName] || '@/assets/pictures/courseBackground1.jpeg'
}

// 获取默认教师头像
const getDefaultTeacherAvatar = () => {
  const avatars = [
    '@/assets/pictures/teacherGirl1.jpeg',
    '@/assets/pictures/teacherGirl2.jpeg',
    '@/assets/pictures/teacherGirl3.jpeg',
    '@/assets/pictures/teacherGirl4.jpeg',
    '@/assets/pictures/teacherBoy1.jpeg',
    '@/assets/pictures/teacherBoy2.jpeg'
  ]
  return avatars[Math.floor(Math.random() * avatars.length)]
}

// 组件挂载时获取数据
onMounted(() => {
  fetchSubjects()
  fetchGrades()
  fetchCourses()
})
</script>

<template>
  <div class="latest-courses">
    <div class="banner">
      <div class="banner-content">
        <h1>最新课程</h1>
        <p>发现最新上线的精品课程，紧跟学习潮流</p>
      </div>
    </div>

    <div class="container">
      <!-- 课程筛选 -->
      <div class="filter-section">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="科目">
            <el-select v-model="filter.subject" placeholder="选择科目" clearable>
              <el-option
                v-for="subject in subjects"
                :key="subject.id"
                :label="subject.name"
                :value="subject.name"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="年级">
            <el-select v-model="filter.grade" placeholder="选择年级" clearable>
              <el-option
                v-for="grade in grades"
                :key="grade.id"
                :label="grade.gradeName"
                :value="grade.gradeName"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleFilter" :loading="loading">筛选</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 最新课程标语 -->
      <div class="latest-highlight">
        <div class="highlight-content">
          <el-icon><Refresh /></el-icon>
          <span>以下课程均为近期更新，持续更新中...</span>
        </div>
      </div>

      <!-- 课程列表 -->
      <div class="courses-section" v-loading="loading">
        <div class="courses-grid" v-if="courses.length > 0">
          <div class="course-card" v-for="course in courses" :key="course.id">
            <div class="course-image">
              <img :src="$getImageUrl(getDefaultCourseImage(course.subjectName))" :alt="course.title">
              <div class="course-badge new" v-if="isNewCourse(course.createdAt)">最新</div>
              <div class="course-badge status" :class="course.status">{{ course.statusDisplay }}</div>
            </div>
            <div class="course-info">
              <h3>{{ course.title }}</h3>
              <div class="course-teacher">
                <img :src="$getImageUrl(getDefaultTeacherAvatar())" :alt="course.teacherName">
                <span>{{ course.teacherName }}</span>
              </div>
              <p class="course-description">{{ course.description || '暂无课程描述' }}</p>
              <div class="course-meta">
                <div class="meta-item">
                  <el-icon><Timer /></el-icon>
                  <span>{{ formatDuration(course.durationMinutes) }}</span>
                </div>
                <div class="meta-item">
                  <el-icon><Reading /></el-icon>
                  <span>{{ course.subjectName }}</span>
                </div>
                <div class="meta-item">
                  <el-icon><School /></el-icon>
                  <span>{{ course.courseTypeDisplay }}</span>
                </div>
                <div class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ formatDate(course.createdAt) }}</span>
                </div>
              </div>
              <div class="course-tags" v-if="course.grade">
                <el-tag size="small" class="course-tag">{{ course.grade }}</el-tag>
                <el-tag size="small" class="course-tag" type="success">{{ course.subjectName }}</el-tag>
              </div>
              <div class="course-actions">
                <span class="course-status">{{ course.statusDisplay }}</span>
                <el-button type="primary" size="small" :disabled="course.status !== 'active'">立即报名</el-button>
                <el-button type="info" size="small" plain>查看详情</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading" class="empty-state">
          <el-empty description="暂无课程数据">
            <el-button type="primary" @click="fetchCourses">刷新</el-button>
          </el-empty>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="total > 0">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[6, 12, 24]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>

      <!-- 促销信息 -->
      <div class="promotion-section">
        <div class="promotion-card">
          <div class="promotion-icon">
            <el-icon><Promotion /></el-icon>
          </div>
          <div class="promotion-content">
            <h3>限时优惠</h3>
            <p>新课程首周报名，额外赠送1次一对一辅导</p>
            <el-button type="danger">了解详情</el-button>
          </div>
        </div>
        <div class="promotion-card">
          <div class="promotion-icon">
            <el-icon><GoodsFilled /></el-icon>
          </div>
          <div class="promotion-content">
            <h3>新人特惠</h3>
            <p>首次注册用户，任选一门最新课程立减200元</p>
            <el-button type="danger">立即领取</el-button>
          </div>
        </div>
        <div class="promotion-card">
          <div class="promotion-icon">
            <el-icon><Present /></el-icon>
          </div>
          <div class="promotion-content">
            <h3>邀请有礼</h3>
            <p>邀请好友注册，双方均可获得100元课程抵扣券</p>
            <el-button type="danger">邀请好友</el-button>
          </div>
        </div>
      </div>
    </div>
    <!-- 联系我们 -->
    <ContactUs />
  </div>
</template>

<script lang="ts">
export default {
  name: 'LatestCoursesView'
}
</script>

<style scoped>
.latest-courses {
  width: 100%;
}

.banner {
  height: 300px;
  background-image: url('@/assets/pictures/courseBackground1.jpeg');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  text-align: center;
  position: relative;
}

.banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
}

.banner-content {
  position: relative;
  z-index: 10;
}

.banner h1 {
  font-size: 48px;
  margin-bottom: 15px;
}

.banner p {
  font-size: 20px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.filter-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.filter-form .el-select {
  min-width: 140px;
  width: 180px;
}

.latest-highlight {
  background-color: #f0f9eb;
  border-radius: 8px;
  padding: 15px 20px;
  margin-bottom: 30px;
}

.highlight-content {
  display: flex;
  align-items: center;
  color: #67c23a;
  font-size: 16px;
}

.highlight-content .el-icon {
  margin-right: 10px;
  font-size: 20px;
}

.courses-section {
  margin-bottom: 60px;
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 30px;
  margin-bottom: 40px;
}

.course-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  background-color: #fff;
  transition: transform 0.3s, box-shadow 0.3s;
}

.course-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

.course-image {
  height: 200px;
  overflow: hidden;
  position: relative;
}

.course-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.course-card:hover .course-image img {
  transform: scale(1.05);
}

.course-badge {
  position: absolute;
  top: 15px;
  right: 15px;
  padding: 5px 10px;
  color: white;
  font-size: 12px;
  border-radius: 4px;
  font-weight: bold;
}

.course-badge.hot {
  background-color: #f56c6c;
  right: 15px;
}

.course-badge.new {
  background-color: #409eff;
  right: 70px;
}

.course-badge.status {
  right: 15px;
}

.course-badge.status.active {
  background-color: #67c23a;
}

.course-badge.status.inactive {
  background-color: #909399;
}

.course-badge.status.full {
  background-color: #f56c6c;
}

.course-badge.status.pending {
  background-color: #e6a23c;
}

.course-info {
  padding: 20px;
}

.course-info h3 {
  margin: 0 0 15px;
  font-size: 18px;
  color: #333;
}

.course-teacher {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.course-teacher img {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  margin-right: 10px;
}

.course-teacher span {
  color: #666;
}

.course-description {
  margin: 0 0 15px;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  height: 68px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  margin-right: 15px;
  margin-bottom: 5px;
  color: #666;
  font-size: 13px;
}

.meta-item .el-icon {
  margin-right: 5px;
  font-size: 16px;
}

.course-tags {
  margin-bottom: 15px;
}

.course-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

.course-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.course-price {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
}

.course-status {
  font-size: 16px;
  color: #67c23a;
  font-weight: bold;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.pagination {
  text-align: center;
}

.promotion-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.promotion-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.promotion-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #fef0f0;
  color: #f56c6c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  margin-right: 20px;
}

.promotion-content {
  flex: 1;
}

.promotion-content h3 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #333;
}

.promotion-content p {
  margin: 0 0 15px;
  color: #666;
}

@media (max-width: 768px) {
  .latest-courses {
    padding-top: 56px; /* 适配移动端导航高度 */
  }

  .banner {
    height: 200px;
    padding: 0 20px;
  }

  .banner h1 {
    font-size: 32px;
    margin-bottom: 10px;
  }

  .banner p {
    font-size: 16px;
  }

  .container {
    padding: 30px 16px;
  }

  .filter-section {
    margin-bottom: 30px;
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .filter-form .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
    width: 100%;
  }

  .filter-actions {
    margin-top: 16px;
    display: flex;
    gap: 12px;
  }

  .filter-actions .el-button {
    flex: 1;
  }

  .courses-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .course-card {
    padding: 16px;
  }

  .course-image {
    height: 180px;
    margin-bottom: 16px;
  }

  .course-info h3 {
    font-size: 16px;
    margin-bottom: 8px;
  }

  .course-teacher {
    margin-bottom: 12px;
  }

  .course-teacher img {
    width: 24px;
    height: 24px;
  }

  .course-teacher span {
    font-size: 13px;
  }

  .course-description {
    font-size: 13px;
    line-height: 1.5;
    margin-bottom: 12px;
  }

  .course-meta {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
    margin-bottom: 16px;
  }

  .meta-item {
    font-size: 12px;
  }

  .course-price {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .course-actions {
    flex-direction: column;
    gap: 8px;
  }

  .course-actions .el-button {
    width: 100%;
  }

  .promotion-section {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .promotion-card {
    padding: 16px;
    flex-direction: column;
    text-align: center;
  }

  .promotion-icon {
    width: 50px;
    height: 50px;
    font-size: 24px;
    margin-right: 0;
    margin-bottom: 12px;
  }

  .promotion-content h3 {
    font-size: 16px;
    margin-bottom: 8px;
  }

  .promotion-content p {
    font-size: 13px;
    margin-bottom: 12px;
  }

  .pagination {
    margin-top: 30px;
  }

  .el-pagination {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .latest-courses {
    padding-top: 52px;
  }

  .banner {
    height: 180px;
    padding: 0 16px;
  }

  .banner h1 {
    font-size: 28px;
  }

  .banner p {
    font-size: 14px;
  }

  .container {
    padding: 24px 12px;
  }

  .course-card {
    padding: 12px;
  }

  .course-image {
    height: 160px;
    margin-bottom: 12px;
  }

  .filter-actions {
    flex-direction: column;
  }

  .filter-actions .el-button {
    width: 100%;
    margin-bottom: 8px;
  }

  .promotion-card {
    padding: 12px;
  }

  .promotion-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }

  .el-pagination {
    flex-wrap: wrap;
  }

  .el-pagination .el-pagination__sizes,
  .el-pagination .el-pagination__jump {
    order: 3;
    margin-top: 12px;
  }
}
</style>
