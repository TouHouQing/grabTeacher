<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { evaluationAPI } from '../utils/api'

type Review = {
  id: number
  teacherId: number
  studentId: number
  courseId: number
  teacherName: string
  studentName: string
  courseName: string
  studentComment: string
  rating: number
  createdAt: string
}

const loading = ref(true)
const reviews = ref<Review[]>([])
const page = ref(1)
const size = ref(6)
const total = ref(0)

// 查询条件：课程名、教师名、最低评分
const teacherName = ref('')
const courseName = ref('')
const minRating = ref<number | null>(null)

const loadReviews = async () => {
  loading.value = true
  try {
    const res = await evaluationAPI.listPublic({
      page: page.value,
      size: size.value,
      teacherName: teacherName.value,
      courseName: courseName.value,
      minRating: minRating.value ?? undefined
    })
    if (res?.success) {
      const data = res.data
      reviews.value = (data.records || []) as Review[]
      total.value = Number(data.total || 0)
      page.value = Number(data.current || 1)
      size.value = Number(data.size || 6)
    } else {
      reviews.value = []
      total.value = 0
    }
  } catch {
    reviews.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadReviews()
})

const handlePageChange = (p: number) => {
  page.value = p
  loadReviews()
}

const handleSizeChange = (s: number) => {
  size.value = s
  page.value = 1
  loadReviews()
}

const handleSearch = () => {
  page.value = 1
  loadReviews()
}

const resetFilter = () => {
  teacherName.value = ''
  courseName.value = ''
  minRating.value = null
  page.value = 1
  loadReviews()
}

const formatDateTime = (iso: string) => {
  const d = new Date(iso)
  // 与最新课程页面风格一致，展示本地化日期
  const date = d.toLocaleDateString('zh-CN')
  const time = d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return `${date} ${time}`
}

const initialOf = (r: Review) => (r.studentName?.slice(0, 1) || '学')
</script>

<template>
  <div class="student-reviews">
    <div class="banner">
      <div class="banner-content">
        <h1>学员评价</h1>
        <p>真实反馈，助你找到更合适的老师</p>
      </div>
    </div>

    <div class="container">
      <!-- 筛选区域：与最新课程风格一致 -->
      <div class="filter-section">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="课程名称">
            <el-input v-model="courseName" placeholder="输入课程名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="教师名称">
            <el-input v-model="teacherName" placeholder="输入教师名称" clearable @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="最低评分">
            <el-rate v-model="minRating" allow-half clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="reviews-section" v-loading="loading">
        <div class="reviews-grid" v-if="reviews.length > 0">
          <div class="review-card" v-for="item in reviews" :key="item.id">
            <div class="review-header">
              <div class="avatar">{{ initialOf(item) }}</div>
              <div class="header-meta">
                <div class="title-row">
                  <span class="student-name ellipsis">{{ item.studentName || '学员' }}</span>
                </div>
                <div class="info-row">
                  <el-tag size="small" effect="light" class="pill ellipsis">教师：{{ item.teacherName }}</el-tag>
                </div>
                <div class="course-row">
                  <el-tag size="small" type="success" effect="light" class="pill ellipsis">课程：{{ item.courseName }}</el-tag>
                </div>
              </div>
              <div class="rating">
                <el-rate :model-value="Number(item.rating || 0)" disabled allow-half />
                <span class="created-at">{{ formatDateTime(item.createdAt) }}</span>
              </div>
            </div>
            <div class="comment-box">
              <p>{{ item.studentComment || '暂无评论内容' }}</p>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading" class="empty-state">
          <el-empty description="暂无评价数据">
            <el-button type="primary" @click="loadReviews">刷新</el-button>
          </el-empty>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="total > 0">
          <el-pagination
            :current-page="page"
            :page-size="size"
            :page-sizes="[6, 9, 12, 15]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>
  </div>
  </template>

<style scoped>
.student-reviews {
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

.filter-form .el-input {
  width: 220px;
}

.reviews-section {
  margin-bottom: 40px;
}

.reviews-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 30px;
  margin-bottom: 30px;
}

.review-card {
  background: #fff;
  border-radius: 8px;
  padding: 18px 18px 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f2f5;
  transition: transform .2s ease, box-shadow .2s ease;
}

.review-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.08);
}

.review-header {
  position: relative; /* 为绝对定位的评分容器提供参考 */
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.header-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0; /* 防止flex子项溢出 */
}

.title-row {
  display: grid;
  grid-template-columns: 1fr; /* 仅显示姓名，时间移至右上角 */
  gap: 8px;
  align-items: baseline;
  min-width: 0; /* 防止网格内容溢出 */
}

.student-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.dot {
  color: #c0c4cc;
}

.created-at {
  font-size: 12px;
  color: #909399;
  justify-self: end;
}

.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
}

.course-row {
  margin-top: 6px;
}

.pill {
  border-radius: 999px;
}

.names {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  color: #333;
}

.date {
  font-size: 12px;
  color: #909399;
}

.rating {
  position: absolute; /* 绝对定位到右上角 */
  top: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  width: 130px; /* 固定宽度，防止被挤压 */
  z-index: 1;
}

/* 限制星星尺寸，避免溢出 */
.rating :deep(.el-rate__icon) {
  font-size: 18px;
}

.comment-box {
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  padding: 12px 14px;
  margin-top: 8px;
}

.comment-box p {
  color: #303133;
  line-height: 1.8;
  margin: 0;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.pagination {
  text-align: center;
}

@media (max-width: 768px) {
  .student-reviews { padding-top: 56px; }
  .banner { height: 200px; padding: 0 20px; }
  .banner h1 { font-size: 32px; margin-bottom: 10px; }
  .banner p { font-size: 16px; }
  .container { padding: 30px 16px; }
  .filter-form { flex-direction: column; align-items: stretch; gap: 16px; }
  .filter-form .el-form-item { width: 100%; margin-right: 0; margin-bottom: 0; }
  .reviews-grid { grid-template-columns: 1fr; gap: 20px; }

  .review-card { padding: 16px; }
  .review-header { gap: 10px; }
  .rating { width: 110px; }
  .rating :deep(.el-rate__icon) { font-size: 16px; }
  .avatar { width: 36px; height: 36px; font-size: 14px; }
  .student-name { font-size: 15px; }
  .created-at { font-size: 12px; }
  .header-meta { gap: 6px; }
  .info-row { gap: 6px; }
  .comment-box { padding: 10px 12px; }
}

@media (max-width: 480px) {
  .student-reviews { padding-top: 52px; }
  .banner { height: 180px; padding: 0 16px; }
  .banner h1 { font-size: 28px; }
  .banner p { font-size: 14px; }
  .container { padding: 24px 12px; }

  /* 移动端：调整评分容器宽度 */
  .rating { width: 96px; }
  .rating :deep(.el-rate__icon) { font-size: 15px; }
  .title-row { width: 100%; }
}
</style>
