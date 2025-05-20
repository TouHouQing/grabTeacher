<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

// 搜索过滤条件
const filter = reactive({
  subject: '',
  grade: '',
  level: ''
})

// 分页参数
const currentPage = ref(1)
const pageSize = ref(6)

// 最新课程数据（用时间排序，最近的在前面）
const courses = [
  {
    id: 1,
    title: '初中物理 - 电磁学深入',
    teacher: '王老师',
    teacherAvatar: '@/assets/pictures/teacherGirl4.jpeg',
    description: '本课程深入讲解电磁感应、电磁波等初中物理重点知识，结合最新高考真题，帮助学生系统掌握电磁学知识体系。',
    image: '@/assets/pictures/physics1.jpeg',
    duration: '25课时',
    students: 42,
    rating: 5.0,
    price: 2100,
    subject: '物理',
    grade: '初中',
    level: '进阶',
    isHot: true,
    isNew: true,
    releaseDate: '2023-07-01',
    tags: ['电磁学', '中考重点', '最新']
  },
  {
    id: 2,
    title: '初中化学 - 无机化合物专题',
    teacher: '刘老师',
    teacherAvatar: '@/assets/pictures/teacherGirl3.jpeg',
    description: '系统讲解无机化学基础，深入浅出地介绍无机化合物的性质与反应。',
    image: '@/assets/pictures/chemistry1.jpeg',
    duration: '20课时',
    students: 35,
    rating: 4.9,
    price: 1800,
    subject: '化学',
    grade: '初中',
    level: '入门',
    isHot: false,
    isNew: true,
    releaseDate: '2023-06-28',
    tags: ['无机化学', '化学反应', '最新']
  },
  {
    id: 3,
    title: '初中数学 - 几何证明入门',
    teacher: '李老师',
    teacherAvatar: '@/assets/pictures/teacherGirl2.jpeg',
    description: '本课程从基础开始，教授初中几何证明的基本方法，包括三角形全等、相似、勾股定理等常用证明技巧。',
    image: '@/assets/pictures/math1.jpeg',
    duration: '18课时',
    students: 68,
    rating: 4.8,
    price: 1500,
    subject: '数学',
    grade: '初中',
    level: '入门',
    isHot: true,
    isNew: true,
    releaseDate: '2023-06-25',
    tags: ['几何', '证明', '中考']
  },
  {
    id: 4,
    title: '小学英语 - 情景对话进阶',
    teacher: '杨老师',
    teacherAvatar: '@/assets/pictures/teacherGirl1.jpeg',
    description: '通过日常生活情景对话，提高小学生英语口语表达能力，课程生动有趣，配合多媒体教学资源。',
    image: '@/assets/pictures/english1.jpeg',
    duration: '15课时',
    students: 82,
    rating: 5.0,
    price: 1200,
    subject: '英语',
    grade: '小学',
    level: '进阶',
    isHot: false,
    isNew: true,
    releaseDate: '2023-06-20',
    tags: ['口语', '对话', '情景教学']
  },
  {
    id: 5,
    title: '初中生物 - 生物学前沿',
    teacher: '赵老师',
    teacherAvatar: '@/assets/pictures/teacherBoy1.jpeg',
    description: '结合最新生物技术发展，讲解遗传学知识，从DNA到基因编辑，让学生了解现代生物科技与生物学前沿。',
    image: '@/assets/pictures/biology1.jpeg',
    duration: '22课时',
    students: 48,
    rating: 4.9,
    price: 1950,
    subject: '生物',
    grade: '初中',
    level: '高级',
    isHot: true,
    isNew: true,
    releaseDate: '2023-06-15',
    tags: ['遗传学', '基因', '前沿科技']
  },
  {
    id: 6,
    title: '初中历史 - 中国古代文明',
    teacher: '陈老师',
    teacherAvatar: '@/assets/pictures/teacherBoy2.jpeg',
    description: '带领学生领略中国古代文明的辉煌，从夏商周到明清，系统讲解中国历史发展脉络，深入浅出，图文并茂。',
    image: '@/assets/pictures/history1.jpeg',
    duration: '24课时',
    students: 56,
    rating: 4.8,
    price: 1600,
    subject: '历史',
    grade: '初中',
    level: '入门',
    isHot: false,
    isNew: true,
    releaseDate: '2023-06-10',
    tags: ['中国历史', '古代文明', '文化']
  }
]

// 筛选课程
const filteredCourses = computed(() => {
  return courses.filter(course => {
    let match = true
    if (filter.subject && course.subject !== filter.subject) {
      match = false
    }
    if (filter.grade && course.grade !== filter.grade) {
      match = false
    }
    if (filter.level && course.level !== filter.level) {
      match = false
    }
    return match
  })
})

// 分页显示课程
const displayCourses = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredCourses.value.slice(start, end)
})

// 筛选方法
const handleFilter = () => {
  currentPage.value = 1
}

// 重置筛选
const resetFilter = () => {
  filter.subject = ''
  filter.grade = ''
  filter.level = ''
  currentPage.value = 1
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
              <el-option label="数学" value="数学" />
              <el-option label="英语" value="英语" />
              <el-option label="物理" value="物理" />
              <el-option label="化学" value="化学" />
              <el-option label="生物" value="生物" />
              <el-option label="历史" value="历史" />
            </el-select>
          </el-form-item>
          <el-form-item label="年级">
            <el-select v-model="filter.grade" placeholder="选择年级" clearable>
              <el-option label="小学" value="小学" />
              <el-option label="初中" value="初中" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="filter.level" placeholder="课程难度" clearable>
              <el-option label="入门" value="入门" />
              <el-option label="进阶" value="进阶" />
              <el-option label="高级" value="高级" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleFilter">筛选</el-button>
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
      <div class="courses-section">
        <div class="courses-grid">
          <div class="course-card" v-for="(course, index) in displayCourses" :key="index">
            <div class="course-image">
              <img :src="$getImageUrl(course.image)" :alt="course.title">
              <div class="course-badge new" v-if="course.isNew">最新</div>
              <div class="course-badge hot" v-if="course.isHot">热门</div>
            </div>
            <div class="course-info">
              <h3>{{ course.title }}</h3>
              <div class="course-teacher">
                <img :src="$getImageUrl(course.teacherAvatar)" :alt="course.teacher">
                <span>{{ course.teacher }}</span>
              </div>
              <p class="course-description">{{ course.description }}</p>
              <div class="course-meta">
                <div class="meta-item">
                  <el-icon><Timer /></el-icon>
                  <span>{{ course.duration }}</span>
                </div>
                <div class="meta-item">
                  <el-icon><User /></el-icon>
                  <span>{{ course.students }}人学习</span>
                </div>
                <div class="meta-item">
                  <el-icon><Star /></el-icon>
                  <span>{{ course.rating }}分</span>
                </div>
                <div class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ course.releaseDate }}</span>
                </div>
              </div>
              <div class="course-tags">
                <el-tag v-for="(tag, i) in course.tags" :key="i" size="small" class="course-tag">{{ tag }}</el-tag>
              </div>
              <div class="course-actions">
                <span class="course-price">￥{{ course.price }}</span>
                <el-button type="primary" size="small">立即报名</el-button>
                <el-button type="info" size="small" plain>免费试听</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[6, 12, 24]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="filteredCourses.length"
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
  .banner {
    height: 200px;
  }

  .banner h1 {
    font-size: 32px;
  }

  .banner p {
    font-size: 16px;
  }

  .courses-grid {
    grid-template-columns: 1fr;
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-form .el-form-item {
    margin-right: 0;
    margin-bottom: 15px;
  }

  .promotion-section {
    grid-template-columns: 1fr;
  }
}
</style>
