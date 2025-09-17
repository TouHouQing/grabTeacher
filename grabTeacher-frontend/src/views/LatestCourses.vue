<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import ContactUs from '../components/ContactUs.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer, Star, Calendar, Reading, School } from '@element-plus/icons-vue'
import { courseAPI, subjectAPI, enrollmentAPI } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 课程接口定义
interface Course {
  id: number
  teacherId: number
  teacherName: string
  teacherLevel?: string
  subjectId: number
  subjectName: string
  title: string
  description?: string
  courseType: string
  courseTypeDisplay: string
  durationMinutes: number
  status: string
  statusDisplay: string
  createdAt: string
  price?: number
  startDate?: string
  endDate?: string
  personLimit?: number
  imageUrl?: string
  courseLocation?: string
  // 课程时间显示（后端组合好的显示文案）
  scheduleDisplay?: string
  // 每周上课时间安排（仅大班课），用于渲染“周几 + 时间段”
  courseTimeSlots?: TimeSlotDTO[]
  enrolled?: boolean
}

// 科目接口定义
interface Subject {
  id: number
  name: string
  iconUrl?: string
  isActive: boolean
}

// 每周上课时间段类型（与后端 TimeSlotDTO 对齐）
interface TimeSlotDTO {
  weekday: number // 1=周一 ... 7=周日
  timeSlots: string[]
}



// 搜索过滤条件
const filter = reactive({
  subject: '',
  courseType: '',
  courseLocation: '',
  teacherLevel: ''
})

// 分页参数
const currentPage = ref(1)
const pageSize = ref(6)
const total = ref(0)

// 数据状态
const loading = ref(false)
const courses = ref<Course[]>([])
const subjects = ref<Subject[]>([])
// 报名加载状态（用于禁用/加载按钮）
const enrollingCourseId = ref<number | null>(null)

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

    if (filter.courseType) {
      params.courseType = filter.courseType
    }
    if (filter.courseLocation) {
      params.courseLocation = filter.courseLocation
    }
    if (filter.teacherLevel) {
      params.teacherLevel = filter.teacherLevel
    }

    const response = await courseAPI.getPublicCourses(params)
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


// 筛选方法
const handleFilter = () => {
  currentPage.value = 1
  fetchCourses()
}

// 重置筛选
const resetFilter = () => {
  filter.subject = ''
  filter.courseType = ''
  filter.courseLocation = ''
  filter.teacherLevel = ''
  currentPage.value = 1
  fetchCourses()
}

// 分页变化处理
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchCourses()
}

// 页面大小变化处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchCourses()
}

// 格式化时长显示
const formatDuration = (minutes: number | null) => {
  if (minutes === null || minutes === undefined) {
    return '1.5/2小时'
  }
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

// 周几中文名
const getWeekdayName = (n: number) => {
  const map = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return map[n] || '未知'
}

// 渲染卡片用：将 courseTimeSlots 压缩为单行短文案
const formatCourseTimeSlotsForList = (slots?: TimeSlotDTO[]) => {
  if (!slots || slots.length === 0) return '时间段待定'
  const parts: string[] = []
  // 过滤空并按 weekday 升序
  const sorted = [...slots]
    .filter(s => s && Array.isArray(s.timeSlots) && s.timeSlots.length > 0)
    .sort((a, b) => (a.weekday || 0) - (b.weekday || 0))
  for (const s of sorted) {
    const day = getWeekdayName(s.weekday)
    const times = s.timeSlots.join('、')
    parts.push(`${day} ${times}`)
  }
  return parts.join(' | ')
}

// Tooltip 使用：完整展开（含换行）
const formatCourseTimeSlotsTooltip = (slots?: TimeSlotDTO[]) => {
  if (!slots || slots.length === 0) return '时间段待定'
  const sorted = [...slots]
    .filter(s => s && Array.isArray(s.timeSlots) && s.timeSlots.length > 0)
    .sort((a, b) => (a.weekday || 0) - (b.weekday || 0))
  return sorted.map(s => `${getWeekdayName(s.weekday)}：${s.timeSlots.join('、')}`).join('\n')
}

// 过滤后的 tooltip 数据（避免 v-for + v-if 混用）
const filterTimeSlotsForTooltip = (slots?: TimeSlotDTO[]) => {
  if (!slots) return [] as TimeSlotDTO[]
  return [...slots]
    .filter(s => s && Array.isArray(s.timeSlots) && s.timeSlots.length > 0)
    .sort((a, b) => (a.weekday || 0) - (b.weekday || 0))
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

// 格式化价格显示
const formatPrice = (course: Course) => {
  if (course.courseType === 'large_class') {
    // 小班课价格采用“每小时”展示
    if (course.price && course.price > 0) {
      return `${course.price} M豆/h`
    } else {
      return '价格面议'
    }
  } else {
    // 一对一课程按小时
    if (course.price && course.price > 0) {
      return `${course.price} M豆/h`
    } else {
      return '价格面议'
    }
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchSubjects()
  fetchCourses()
})

// 点击立即报名
const handleEnroll = async (course: Course) => {
  try {
    // 未登录提示
    if (!userStore.isLoggedIn) {
      await ElMessageBox.alert('请登录或联系客服预约课程', '提示', { confirmButtonText: '确定' })
      return
    }
    // 非学生角色提示
    if (!userStore.isStudent) {
      ElMessage.warning('请使用学生账号登录后报名')
      return
    }
    // 已报名直接提示
    if (course.enrolled) {
      ElMessage.info('您已报名该课程')
      return
    }
    // 课程已满员提示
    if (course.status === 'full') {
      ElMessage.warning('该课程已满员，无法报名')
      return
    }
    // 仅支持小班课报名
    if (course.courseType !== 'large_class') {
      ElMessage.info('该课程请先进入详情或联系客服预约')
      return
    }

    // 二次确认
    await ElMessageBox.confirm(`确认报名该课程：${course.title}？`, '确认报名', {
      type: 'warning',
      confirmButtonText: '确认报名',
      cancelButtonText: '取消'
    })

    enrollingCourseId.value = course.id
    const res = await enrollmentAPI.enrollCourse(course.id)
    const msg: string = res.message || ''
    if (msg.includes('已报名')) {
      ElMessage.info('您已报名该课程')
    } else if (res.success) {
      ElMessage.success('报名成功')
      fetchCourses()
    } else if (msg.includes('余额不足')) {
      ElMessage.warning('您的M豆余额不足，请联系客服预约课程')
    } else {
      ElMessage.error(msg || '报名失败，请稍后重试')
    }
  } catch (e: any) {
    if (e === 'cancel') {
      ElMessage.info('已取消报名')
    } else {
      console.error('报名失败:', e)
      ElMessage.error('报名失败，请稍后重试')
    }
  } finally {
    enrollingCourseId.value = null
  }
}

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
          <el-form-item label="教师级别">
            <el-select v-model="filter.teacherLevel" placeholder="选择级别" clearable>
              <el-option label="王牌" value="王牌" />
              <el-option label="金牌" value="金牌" />
              <el-option label="银牌" value="银牌" />
              <el-option label="铜牌" value="铜牌" />
            </el-select>
          </el-form-item>
          <el-form-item label="授课方式">
            <el-select v-model="filter.courseLocation" placeholder="线上/线下" clearable>
              <el-option label="线上" value="线上" />
              <el-option label="线下" value="线下" />
            </el-select>
          </el-form-item>
          <el-form-item label="课程类型">
            <el-select v-model="filter.courseType" placeholder="类型" clearable>
              <el-option label="一对一" value="one_on_one" />
              <el-option label="大班课" value="large_class" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleFilter" :loading="loading">筛选</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 课程列表 -->
      <div class="courses-section" v-loading="loading">
        <div class="courses-grid" v-if="courses.length > 0">
          <div class="course-card" v-for="course in courses" :key="course.id">
            <div class="course-image" @click="$router.push(`/course/${course.id}`)" @keyup.enter="$router.push(`/course/${course.id}`)" role="button" tabindex="0">
              <img :src="course.imageUrl ? course.imageUrl : $getImageUrl(getDefaultCourseImage(course.subjectName))" :alt="course.title">
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
                  <el-icon><Calendar /></el-icon>
                  <span>{{ course.scheduleDisplay || (course.startDate && course.endDate ? (new Date(course.startDate).toLocaleDateString('zh-CN') + '-' + new Date(course.endDate).toLocaleDateString('zh-CN')) : '时间可协商') }}</span>
                </div>
                <!-- 每周上课时间段（卡片精简展示，提供 tooltip 查看完整） -->
                <div class="meta-item" v-if="course.courseTimeSlots && course.courseTimeSlots.length">
                  <el-icon><Timer /></el-icon>
                  <el-tooltip effect="light" placement="top">
                    <template #content>
                      <div class="schedule-tooltip">
                        <div v-for="(s, i) in filterTimeSlotsForTooltip(course.courseTimeSlots)" :key="i">
                          <strong>{{ getWeekdayName(s.weekday) }}：</strong>{{ s.timeSlots.join('、') }}
                        </div>
                      </div>
                    </template>
                    <span class="schedule-inline">{{ formatCourseTimeSlotsForList(course.courseTimeSlots) }}</span>
                  </el-tooltip>
                </div>

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
                <div class="meta-item" v-if="course.teacherLevel">
                  <el-icon><Star /></el-icon>
                  <span>{{ course.teacherLevel }}</span>
                </div>
              </div>
              <div class="course-tags">
                <el-tag size="small" class="course-tag" type="success">{{ course.subjectName }}</el-tag>
              </div>
              <div class="course-price-section">
                <div class="course-price">
                  {{ formatPrice(course) }}
                </div>
                <div class="course-status-text" :class="{
                  'status-active': course.status === 'active',
                  'status-full': course.status === 'full',
                  'status-pending': course.status === 'pending'
                }">{{ course.statusDisplay }}</div>
              </div>

              <div class="course-actions">
                <template v-if="course.courseType === 'large_class'">
                  <el-button v-if="course.status === 'full'"
                             type="info"
                             disabled>
                    已满员
                  </el-button>
                  <el-button v-else-if="course.enrolled"
                             type="success"
                             disabled>
                    已报名
                  </el-button>
                  <el-button v-else
                             type="primary"
                             :loading="enrollingCourseId === course.id"
                             :disabled="enrollingCourseId === course.id"
                             @click.stop="handleEnroll(course)">
                    立即报名
                  </el-button>
                  <el-button type="default" @click.stop="$router.push(`/course/${course.id}`)">查看详情</el-button>
                </template>
                <template v-else>
                  <el-button type="default" @click.stop="$router.push(`/course/${course.id}`)">查看详情</el-button>
                </template>
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
  line-clamp: 3;
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

/* 单行省略的时间段展示，避免卡片被挤爆 */
.schedule-inline {
  display: inline-block;
  max-width: 260px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
/* Tooltip 内容布局 */
.schedule-tooltip {
  font-size: 12px;
  line-height: 1.6;
  white-space: normal;
  max-width: 360px;
}

.course-tags {
  margin-bottom: 15px;
}

.course-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

.course-price-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
  padding: 10px 0;
  border-top: 1px solid #f0f0f0;
}

.course-price {
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
}

.course-price.customizable {
  color: #409eff;
  font-size: 16px;
}

.course-status-text {
  font-size: 14px;
  font-weight: 500;
}

.course-status-text.status-active {
  color: #67c23a;
}

.course-status-text.status-full {
  color: #f56c6c;
}

.course-status-text.status-pending {
  color: #e6a23c;
}

.course-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.course-actions .el-button {
  flex: 1;
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

  .schedule-inline { max-width: 70vw; }

  .meta-item {
    font-size: 12px;
  }

  .course-price-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 12px;
  }

  .course-price {
    font-size: 16px;
  }

  .course-status-text {
    font-size: 13px;
  }

  .course-actions {
    flex-direction: column;
    gap: 8px;
  }

  .course-actions .el-button {
    width: 100%;
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
