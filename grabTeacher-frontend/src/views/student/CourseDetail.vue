<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, School, Trophy, ArrowLeft, Loading, Clock, User, Document, Timer } from '@element-plus/icons-vue'
import { courseAPI, teacherAPI, enrollmentAPI } from '../../utils/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 获取路由传递的课程ID并转换为数字
const courseId = parseInt(route.params.id as string)

// 课程信息状态
const course = ref<any>(null)
const loading = ref(true)
const enrolling = ref(false)
const enrolled = ref(false)
// 额外教师信息
const teacher = ref<{ avatarUrl?: string; teachingExperience?: number; educationBackground?: string; specialties?: string } | null>(null)

// 每周上课时间段类型（与后端 TimeSlotDTO 对齐）
interface TimeSlotDTO {
  weekday: number // 1=周一 ... 7=周日
  timeSlots: string[]
}

const getWeekdayName = (n: number) => {
  const map = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return map[n] || '未知'
}

const sortedCourseTimeSlots = computed(() => {
  const slots = (course.value?.courseTimeSlots || []) as TimeSlotDTO[]
  return [...slots]
    .filter(s => s && Array.isArray(s.timeSlots) && s.timeSlots.length > 0)
    .sort((a, b) => (a.weekday || 0) - (b.weekday || 0))
})



// 返回上一页
const goBack = () => {
  router.back()
}

// 获取课程详情数据
const fetchCourseDetail = async () => {
  try {
    loading.value = true
    const result = await courseAPI.getPublicCourseById(courseId)

    if (result.success && result.data) {
      course.value = result.data
      // 从课程详情接口推断“已报名”状态（若后端返回该字段）
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const anyData: any = result.data as any
      if (typeof anyData.enrolled !== 'undefined') {
        enrolled.value = Boolean(anyData.enrolled)
      }

      // 额外拉取教师信息（头像、年限、教育背景、特长）
      try {
        const tRes = await teacherAPI.getDetail(result.data.teacherId)
        if (tRes.success && tRes.data) {
          teacher.value = {
            avatarUrl: tRes.data.avatarUrl,
            teachingExperience: tRes.data.teachingExperience,
            educationBackground: tRes.data.educationBackground,
            specialties: tRes.data.specialties,
          }
        }
      } catch (e) { /* 静默失败，不影响课程详情 */ }
    } else {
      ElMessage.error(result.message || '获取课程详情失败')
      // 如果获取失败，3秒后返回上一页
      setTimeout(() => {
        goBack()
      }, 3000)
    }
  } catch (error) {
    console.error('获取课程详情失败:', error)
    ElMessage.error('获取课程详情失败，请稍后重试')
    // 如果获取失败，3秒后返回上一页
    setTimeout(() => {
      goBack()
    }, 3000)
  } finally {
    loading.value = false
  }
}

// 跳转到教师详情
const goTeacherDetail = () => {
  const id = course.value?.teacherId
  if (id) router.push(`/teacher-detail/${id}`)
}

onMounted(() => {
  // 页面加载时滚动到顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })

  // 获取课程详情
  fetchCourseDetail()
})

// 立即报名：一对一课程跳转到智能匹配页；大班课按原流程报名
const enrollCourse = async () => {
  try {
    if (!userStore.isLoggedIn) {
      await ElMessageBox.alert('请登录或联系客服预约课程', '提示', { confirmButtonText: '确定' })
      return
    }
    if (!userStore.isStudent) {
      ElMessage.warning('请使用学生账号登录后报名')
      return
    }
    if (!course.value) return

    // 一对一课程：跳转到 1v1 教师智能匹配页面并直达该老师
    if (course.value.courseType === 'one_on_one') {
      const teacherId = course.value.teacherId
      if (!teacherId) {
        ElMessage.warning('未找到授课教师，暂无法预约')
        return
      }
      // 跳转并携带 teacherId 和 courseId，便于目标页预选老师与课程
      router.push({
        path: '/student-center/match',
        query: { teacherId: String(teacherId), courseId: String(courseId) }
      })
      return
    }

    // 已报名则提示并返回（仅针对大班课）
    if (enrolled.value) {
      ElMessage.info('您已报名该课程')
      return
    }

    if (course.value.courseType !== 'large_class') {
      ElMessage.info('该课程请联系客服预约或选择合适时间')
      return
    }

    await ElMessageBox.confirm(`确认报名该课程：${course.value.title}？`, '确认报名', {
      type: 'warning',
      confirmButtonText: '确认报名',
      cancelButtonText: '取消'
    })

    enrolling.value = true
    const res = await enrollmentAPI.enrollCourse(courseId)
    if (res.success) {
      ElMessage.success('报名成功')
      enrolled.value = true
    } else {
      const msg = res.message || '报名失败，请稍后重试'
      if (msg.includes('已报名')) {
        ElMessage.info('您已报名该课程')
        enrolled.value = true
      } else if (msg.includes('余额不足')) {
        ElMessage.warning('您的M豆余额不足，请联系客服预约课程')
      } else {
        ElMessage.error(msg)
      }
    }
  } catch (err: unknown) {
    if (err === 'cancel') {
      ElMessage.info('已取消报名')
    } else {
      console.error('报名失败:', err)
      ElMessage.error('报名失败，请稍后重试')
    }
  } finally {
    enrolling.value = false
  }
}



// 获取状态类型
const getStatusType = (status: string) => {
  switch (status) {
    case 'active':
      return 'success'
    case 'inactive':
      return 'info'
    case 'full':
      return 'warning'
    case 'pending':
      return 'warning'
    default:
      return 'info'
  }
}

// 将特长字符串切分为标签数组
const parseSpecialties = (val?: string) => {
  if (!val) return []
  return val.split(/[，,\s]+/).filter(Boolean).slice(0, 6)
}

const specialtyChips = computed(() => {
  const arr = parseSpecialties(teacher.value?.specialties)
  const visible = arr.slice(0, 3)
  const rest = arr.length > 3 ? arr.length - 3 : 0
  return { visible, rest }
})

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'active':
      return '正在招生'
    case 'inactive':
      return '已下架'
    case 'full':
      return '已满员'
    case 'pending':
      return '待审批'
    default:
      return status
  }
}
</script>

<template>
  <div class="course-detail-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="loading-icon" :size="50"><Loading /></el-icon>
      <p>正在加载课程详情...</p>
    </div>

    <!-- 课程详情内容 -->
    <div v-else-if="course" class="course-detail-content">
      <!-- 返回按钮 -->
      <div class="back-button">
        <el-button @click="goBack" type="primary" plain>
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </div>

      <!-- 课程基本信息 -->
      <div class="course-header">
        <div class="course-image">
          <img :src="course.imageUrl || '/src/assets/pictures/courseBackground1.jpeg'" :alt="course.title">
        </div>
        <div class="course-info">
          <h1 class="course-title">{{ course.title }}</h1>
          <div class="course-meta">
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>{{ course.teacherName }}</span>
            </div>
            <div class="meta-item">
              <el-icon><School /></el-icon>
              <span>{{ course.subjectName }}</span>
            </div>
            <div class="meta-item" v-if="course.durationMinutes">
              <el-icon><Clock /></el-icon>
              <span>{{ Math.floor(course.durationMinutes / 60) }}小时</span>
            </div>
            <div class="meta-item" v-if="course.courseTypeDisplay">
              <el-icon><Trophy /></el-icon>
              <span>{{ course.courseTypeDisplay }}</span>
            </div>
          </div>
          <div class="course-price" v-if="course.price">
            <span class="price-label">课程价格：</span>
            <span class="price-value">{{ course.price }} M豆</span>
          </div>
          <div class="course-actions">
            <el-button type="primary" size="large"
                       :loading="enrolling"
                       :disabled="enrolling || enrolled"
                       @click="enrollCourse">
              <el-icon><Calendar /></el-icon>
              {{ enrolled ? '已报名' : '立即报名' }}
            </el-button>
            <el-button size="large" @click="goBack">返回</el-button>
          </div>
        </div>
      </div>

      <!-- 课程详细信息 -->
      <div class="course-details">
        <h2>课程详情</h2>
        <div class="details-grid">
          <div class="detail-item">
            <el-icon><Trophy /></el-icon>
            <span class="label">课程类型：</span>
            <span class="value">{{ course.courseType === 'one_on_one' ? '一对一' : course.courseType === 'large_class' ? '大班课' : course.courseType }}</span>
          </div>
          <div class="detail-item" v-if="course.startDate">
            <el-icon><Calendar /></el-icon>
            <span class="label">开始日期：</span>
            <span class="value">{{ course.startDate }}</span>
          </div>
          <div class="detail-item" v-if="course.endDate">
            <el-icon><Calendar /></el-icon>
            <span class="label">结束日期：</span>
            <span class="value">{{ course.endDate }}</span>
          </div>
          <div class="detail-item" v-if="course.personLimit">
            <el-icon><User /></el-icon>
            <span class="label">人数限制：</span>
            <span class="value">{{ course.personLimit }}人</span>
          </div>
          <div class="detail-item">
            <el-icon><Timer /></el-icon>
            <span class="label">课程时长：</span>
            <span class="value">{{ course.durationMinutes }}分钟</span>
          </div>
          <div class="detail-item">
            <el-icon><Document /></el-icon>
            <span class="label">课程状态：</span>
            <el-tag :type="getStatusType(course.status)">
              {{ getStatusText(course.status) }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 每周上课时间安排 -->
      <div class="weekly-schedule" v-if="course.courseTimeSlots && course.courseTimeSlots.length">
        <h2>上课时间安排</h2>
        <div class="weekday-grid">
          <div class="weekday-item" v-for="(s, idx) in sortedCourseTimeSlots" :key="idx">
            <div class="weekday-title">
              <el-icon><Calendar /></el-icon>
              <span>{{ getWeekdayName(s.weekday) }}</span>
            </div>
            <div class="time-chips">
              <el-tag v-for="(t, i) in s.timeSlots" :key="i" size="small" effect="light" type="info">{{ t }}</el-tag>
            </div>
          </div>
        </div>
      </div>


      <!-- 教师信息 -->
      <div class="teacher-info" v-if="course.teacherName">
        <h2>授课教师</h2>
        <div class="teacher-card" role="button" tabindex="0" @click="goTeacherDetail" @keyup.enter="goTeacherDetail">
          <div class="teacher-avatar">
            <img :src="teacher?.avatarUrl || $getImageUrl('@/assets/pictures/teacherBoy1.jpeg')" alt="教师头像" />
          </div>
          <div class="teacher-details">
            <h3>{{ course.teacherName }}</h3>
            <p class="teacher-subject">{{ course.subjectName }}专业教师</p>
            <div class="teacher-extra-grid">
              <div class="teacher-extra-item" v-if="teacher?.teachingExperience != null">
                <el-icon><Timer /></el-icon>
                <div class="kv">
                  <div class="label">教学年龄</div>
                  <div class="value">{{ teacher?.teachingExperience }}年</div>
                </div>
              </div>
              <div class="teacher-extra-item" v-if="teacher?.educationBackground">
                <el-icon><School /></el-icon>
                <div class="kv">
                  <div class="label">文凭背景</div>
                  <el-tooltip placement="top" :content="teacher?.educationBackground">
                    <div class="value ellipsis-1">{{ teacher?.educationBackground }}</div>
                  </el-tooltip>
                </div>
              </div>
              <div class="teacher-extra-item" v-if="teacher?.specialties">
                <el-icon><Document /></el-icon>
                <div class="kv">
                  <div class="label">特长</div>
                  <div class="chip-list">
                    <el-tag
                      v-for="(sp, idx) in specialtyChips.visible"
                      :key="idx"
                      size="small"
                      effect="light"
                      type="success"
                      class="chip"
                    >{{ sp }}</el-tag>
                    <el-tag v-if="specialtyChips.rest > 0" size="small" effect="plain" type="info" class="chip">+{{ specialtyChips.rest }}</el-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 课程详细描述 -->
      <div class="course-description" v-if="course.description">
        <h2>课程介绍</h2>
        <div class="description-content">
          {{ course.description }}
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error-container">
      <p>课程不存在或已被删除</p>
      <el-button @click="goBack" type="primary">返回</el-button>
    </div>
  </div>
</template>

<style scoped>
.course-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
}

.loading-container, .error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
}

.loading-icon {
  animation: spin 1s linear infinite;
  color: #409eff;
  margin-bottom: 20px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.course-detail-content {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.back-button {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.course-header {
  display: flex;
  gap: 30px;
  padding: 30px;
}

.course-image {
  flex-shrink: 0;
  width: 400px;
  height: 300px;
  border-radius: 12px;
  overflow: hidden;
}

.course-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.course-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
}

.course-price {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
}

.price-label {
  color: #666;
}

.price-value {
  color: #e6a23c;
  font-weight: 600;
  font-size: 24px;
}

.course-actions {
  display: flex;
  gap: 15px;
  margin-top: auto;
}

.course-details, .teacher-info, .course-description {
  padding: 30px;
  border-top: 1px solid #eee;
}

.course-details h2, .teacher-info h2, .course-description h2 {
  font-size: 20px;
  color: #333;
  margin-bottom: 20px;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.detail-item .label {
  font-weight: 500;
  color: #333;
}

.detail-item .value {
  color: #666;
}

.teacher-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.1s ease, opacity 0.2s ease;
  -webkit-tap-highlight-color: transparent;
}
.teacher-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.teacher-card:active {
  transform: scale(0.98);
  opacity: 0.92;
}
.teacher-card:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.25);
}

.teacher-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.teacher-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.teacher-details h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #333;
}

.teacher-subject {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
}

.teacher-extra-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 16px;
  align-items: start;
}
.teacher-extra-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
}
.teacher-extra-item .kv .label { font-size: 12px; color: #999; }
.teacher-extra-item .kv .value { font-size: 14px; color: #333; }
.ellipsis-1 { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.chip-list { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { --el-tag-font-size: 12px; }


/* 每周上课时间安排布局 */
.weekly-schedule { padding: 30px; border-top: 1px solid #eee; }
.weekly-schedule h2 { font-size: 20px; color: #333; margin-bottom: 16px; }
.weekday-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.weekday-item { background: #f8f9fa; border-radius: 8px; padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.weekday-title { display: flex; align-items: center; gap: 6px; font-weight: 600; color: #333; }
.time-chips { display: flex; flex-wrap: wrap; gap: 8px; }

@media (max-width: 768px) {
  .weekly-schedule { padding: 20px; }
  .weekday-grid { grid-template-columns: 1fr; }
}



.description-content {
  color: #666;
  line-height: 1.6;
  font-size: 16px;
}

@media (max-width: 768px) {
  .course-header {
    flex-direction: column;
    gap: 20px;
  }

  .course-image {
    width: 100%;
    height: 250px;
  }

  .course-actions {
    flex-direction: column;
  }

  .details-grid {
    grid-template-columns: 1fr;
  }

  .teacher-card {
    flex-direction: column;
    text-align: center;
  }

  .teacher-extra-grid {
    grid-template-columns: 1fr;
  }
}
</style>
