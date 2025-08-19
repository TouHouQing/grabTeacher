<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, School, Trophy, ArrowLeft, Loading, Clock, User, Document, Timer } from '@element-plus/icons-vue'
import { courseAPI, teacherAPI } from '../../utils/api'

const route = useRoute()
const router = useRouter()

// 获取路由传递的课程ID并转换为数字
const courseId = parseInt(route.params.id as string)

// 课程信息状态
const course = ref(null)
const loading = ref(true)
// 额外教师信息
const teacher = ref<{ avatarUrl?: string; teachingExperience?: number; educationBackground?: string; specialties?: string } | null>(null)


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

// 预约课程 - 跳转到联系我们页面
const bookCourse = () => {
  router.push({ path: '/about', hash: '#contact-us' })
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
            <el-button type="primary" size="large" @click="bookCourse">
              <el-icon><Calendar /></el-icon>
              立即预约
            </el-button>
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
