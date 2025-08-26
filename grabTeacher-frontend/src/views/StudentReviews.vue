<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { evaluationAPI } from '../utils/api'

type Review = {
  id: number
  teacherId: number
  studentId: number
  courseId: number
  studentComment: string
  rating: number
  createdAt: string
}

const loading = ref(true)
const reviews = ref<Review[]>([])
const page = ref(1)
const size = ref(9)
const total = ref(0)

// 可选筛选参数（如未来需要可从路由或UI传入）
const teacherId = ref<number | undefined>(undefined)
const courseId = ref<number | undefined>(undefined)
const minRating = ref<number | undefined>(undefined)

const loadReviews = async () => {
  loading.value = true
  try {
    const res = await evaluationAPI.listPublic({
      page: page.value,
      size: size.value,
      teacherId: teacherId.value,
      courseId: courseId.value,
      minRating: minRating.value
    })
    if (res?.success) {
      const data = res.data
      reviews.value = (data.records || []) as Review[]
      total.value = Number(data.total || 0)
      page.value = Number(data.current || 1)
      size.value = Number(data.size || 9)
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

const initialOf = (r: Review) => String(r.studentId || '').slice(0, 1) || '学'
</script>

<template>
  <div class="reviews-page">
    <div class="hero">
      <h1>学员评价</h1>
      <p>真实反馈，助你找到更合适的老师</p>
    </div>

    <div class="container">
      <el-skeleton v-if="loading" :rows="6" animated />

      <template v-else>
        <div class="grid">
          <div v-for="item in reviews" :key="item.id" class="card">
            <div class="card-header">
              <div class="avatar">{{ initialOf(item) }}</div>
              <div class="meta">
                <div class="name">学员 {{ item.studentId }}</div>
                <div class="date">{{ item.createdAt }}</div>
              </div>
              <div class="rating">
                <el-rate :model-value="Number(item.rating || 0)" disabled allow-half />
              </div>
            </div>
            <div class="content">{{ item.studentComment }}</div>
          </div>
        </div>

        <div class="pagination">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :total="total"
            :current-page="page"
            :page-size="size"
            :page-sizes="[6, 9, 12, 15]"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.reviews-page {
  width: 100%;
}

.hero {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  padding: 40px 20px;
  text-align: center;
}

.hero h1 {
  font-size: 28px;
  margin-bottom: 8px;
}

.hero p {
  opacity: 0.95;
}

.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
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

.meta {
  flex: 1;
}

.name {
  font-weight: 600;
}

.date {
  font-size: 12px;
  color: #909399;
}

.rating {
  display: flex;
  align-items: center;
}

.content {
  color: #303133;
  line-height: 1.8;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

@media (max-width: 992px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .hero {
    padding: 28px 16px;
  }
  .hero h1 {
    font-size: 22px;
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
