<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

// 定义组件名称
defineOptions({
  name: 'PremiumCoursesView'
})

// 导入图片资源
import teacherBoy1 from '@/assets/pictures/teacherBoy1.jpeg'
import teacherBoy2 from '@/assets/pictures/teacherBoy2.jpeg'
import teacherBoy3 from '@/assets/pictures/teacherBoy3.jpeg'
import teacherGirl1 from '@/assets/pictures/teacherGirl1.jpeg'
import teacherGirl2 from '@/assets/pictures/teacherGirl2.jpeg'
import teacherGirl3 from '@/assets/pictures/teacherGirl3.jpeg'
import teacherGirl4 from '@/assets/pictures/teacherGirl4.jpeg'
import studentGirl2 from '@/assets/pictures/studentGirl2.jpeg'
import math1 from '@/assets/pictures/math1.jpeg'
import physics1 from '@/assets/pictures/physics1.jpeg'
import chemistry1 from '@/assets/pictures/chemistry1.jpeg'
import biology1 from '@/assets/pictures/biology1.jpeg'
import english1 from '@/assets/pictures/english1.jpeg'

// 分页参数
const currentPage = ref(1)
const pageSize = ref(6)

// 过滤条件
const filter = reactive({
  subject: '',
  grade: '',
  level: ''
})

// 课程数据
const courses = ref([
  {
    title: '高中数学 - 函数与导数',
    teacher: '张老师',
    teacherAvatar: teacherBoy1,
    description: '本课程深入浅出地讲解高中数学中的函数与导数知识点，适合高二、高三学生。通过大量习题和实例，帮助学生掌握解题技巧。',
    image: math1,
    duration: '30课时',
    students: 1280,
    rating: 4.8,
    price: 1980,
    subject: '数学',
    grade: '高中',
    level: '进阶',
    isHot: true,
    tags: ['函数', '导数', '高考必考']
  },
  {
    title: '初中英语 - 语法精讲',
    teacher: '李老师',
    teacherAvatar: teacherBoy2,
    description: '系统梳理初中英语语法知识，打牢语法基础，提高英语成绩。课程包含大量例句和练习，帮助学生掌握语法规则。',
    image: english1,
    duration: '25课时',
    students: 958,
    rating: 4.7,
    price: 1480,
    subject: '英语',
    grade: '初中',
    level: '入门',
    isHot: true,
    tags: ['语法', '中考', '基础入门']
  },
  {
    title: '高中物理 - 力学与电学',
    teacher: '王老师',
    teacherAvatar: teacherBoy3,
    description: '从基础概念到难点突破，全面讲解高中物理力学与电学知识。通过实验演示和习题讲解，帮助学生理解物理原理。',
    image: physics1,
    duration: '28课时',
    students: 876,
    rating: 4.9,
    price: 1880,
    subject: '物理',
    grade: '高中',
    level: '进阶',
    isHot: false,
    tags: ['力学', '电学', '实验']
  },
  {
    title: '小学数学 - 思维训练',
    teacher: '陈老师',
    teacherAvatar: teacherGirl1,
    description: '培养孩子的数学思维能力，通过趣味数学题和游戏，激发学习兴趣，提高解题能力。适合小学3-6年级学生。',
    image: math1,
    duration: '20课时',
    students: 1056,
    rating: 4.9,
    price: 1280,
    subject: '数学',
    grade: '小学',
    level: '入门',
    isHot: true,
    tags: ['思维训练', '趣味数学', '奥数基础']
  },
  {
    title: '高中化学 - 有机化学',
    teacher: '刘老师',
    teacherAvatar: teacherGirl2,
    description: '系统讲解高中有机化学知识，从基础概念到重难点突破，配合大量习题和实验演示，帮助学生掌握有机化学知识。',
    image: chemistry1,
    duration: '26课时',
    students: 782,
    rating: 4.8,
    price: 1780,
    subject: '化学',
    grade: '高中',
    level: '高级',
    isHot: false,
    tags: ['有机化学', '实验', '高考重点']
  },
  {
    title: '初中物理 - 电学入门',
    teacher: '周老师',
    teacherAvatar: teacherGirl3,
    description: '通过实验和动画演示，讲解初中电学知识，帮助学生建立物理概念，掌握基本规律，提高解题能力。',
    image: physics1,
    duration: '22课时',
    students: 685,
    rating: 4.7,
    price: 1380,
    subject: '物理',
    grade: '初中',
    level: '入门',
    isHot: false,
    tags: ['电学', '实验', '初中物理']
  },
  {
    title: '高中生物 - 分子与细胞',
    teacher: '赵老师',
    teacherAvatar: teacherGirl4,
    description: '深入讲解高中生物分子与细胞部分，通过图解、模型和实验，帮助学生理解抽象概念，掌握重要知识点。',
    image: biology1,
    duration: '24课时',
    students: 592,
    rating: 4.8,
    price: 1680,
    subject: '生物',
    grade: '高中',
    level: '进阶',
    isHot: false,
    tags: ['分子生物学', '细胞', '高考重点']
  },
  {
    title: '小学英语 - 自然拼读',
    teacher: '杨老师',
    teacherAvatar: studentGirl2,
    description: '通过自然拼读法，帮助孩子掌握英语发音规则，建立拼读能力，提高阅读和拼写能力。适合小学1-3年级学生。',
    image: english1,
    duration: '18课时',
    students: 1123,
    rating: 4.9,
    price: 1180,
    subject: '英语',
    grade: '小学',
    level: '入门',
    isHot: true,
    tags: ['自然拼读', '英语启蒙', '趣味英语']
  }
])

// 课程套餐数据
const coursePackages = ref([
  {
    title: '初中英语+数学套餐',
    originalPrice: 2960,
    currentPrice: 2480,
    courses: [
      { title: '初中英语 - 语法精讲', teacher: '李老师', duration: '25课时' },
      { title: '初中数学 - 代数与几何', teacher: '陈老师', duration: '28课时' }
    ],
    features: [
      '双科联报优惠价',
      '赠送中考模拟题',
      '1年有效期',
      '定期学习规划指导'
    ]
  },
  {
    title: '小学全科启蒙套餐',
    originalPrice: 3760,
    currentPrice: 2980,
    courses: [
      { title: '小学数学 - 思维训练', teacher: '陈老师', duration: '20课时' },
      { title: '小学英语 - 自然拼读', teacher: '杨老师', duration: '18课时' },
      { title: '小学科学 - 趣味实验', teacher: '林老师', duration: '16课时' }
    ],
    features: [
      '三科联报优惠价',
      '赠送学习工具包',
      '2年有效期',
      '家长辅导指南'
    ]
  },
  {
    title: '高中数理化套餐',
    originalPrice: 5640,
    currentPrice: 4580,
    courses: [
      { title: '高中数学 - 函数与导数', teacher: '张老师', duration: '30课时' },
      { title: '高中物理 - 力学与电学', teacher: '王老师', duration: '28课时' },
      { title: '高中化学 - 有机化学', teacher: '刘老师', duration: '26课时' }
    ],
    features: [
      '三科联报优惠价',
      '赠送高考真题解析',
      '1年有效期',
      '免费答疑服务'
    ]
  }
])

// 筛选课程
const filteredCourses = computed(() => {
  return courses.value.filter(course => {
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

// 分页显示
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
  <div class="premium-courses">
    <div class="banner">
      <div class="banner-content">
        <h1>金牌课程</h1>
        <p>精心打造的优质课程，助您学习进步</p>
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
            </el-select>
          </el-form-item>
          <el-form-item label="年级">
            <el-select v-model="filter.grade" placeholder="选择年级" clearable>
              <el-option label="小学" value="小学" />
              <el-option label="初中" value="初中" />
              <el-option label="高中" value="高中" />
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

      <!-- 课程列表 -->
      <div class="courses-section">
        <div class="courses-grid">
          <div class="course-card" v-for="(course, index) in displayCourses" :key="index">
            <div class="course-image">
              <img :src="course.image" :alt="course.title">
              <div class="course-badge" v-if="course.isHot">热门</div>
              <div class="price-tag">¥{{ course.price }}</div>
            </div>
            <div class="course-info">
              <h3>{{ course.title }}</h3>
              <div class="course-teacher">
                <img :src="course.teacherAvatar" :alt="course.teacher">
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

      <!-- 热门课程包 -->
      <div class="course-packages">
        <h2 class="section-title">精选课程套餐</h2>
        <p class="section-subtitle">更多优惠，更高性价比</p>
        <div class="packages-grid">
          <div class="package-card" v-for="(pkg, index) in coursePackages" :key="index">
            <div class="package-header">
              <h3>{{ pkg.title }}</h3>
              <div class="package-price">
                <span class="original-price">￥{{ pkg.originalPrice }}</span>
                <span class="current-price">￥{{ pkg.currentPrice }}</span>
              </div>
            </div>
            <div class="package-content">
              <div class="package-courses">
                <div class="package-course-item" v-for="(course, i) in pkg.courses" :key="i">
                  <div class="course-icon">
                    <el-icon><Document /></el-icon>
                  </div>
                  <div class="course-detail">
                    <h4>{{ course.title }}</h4>
                    <p>{{ course.teacher }} | {{ course.duration }}</p>
                  </div>
                </div>
              </div>
              <div class="package-features">
                <div class="feature-item" v-for="(feature, i) in pkg.features" :key="i">
                  <el-icon><Check /></el-icon>
                  <span>{{ feature }}</span>
                </div>
              </div>
              <el-button type="primary" class="package-btn">立即购买</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.premium-courses {
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

.filter-form .el-select {
  min-width: 140px;
  width: 180px;
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

.courses-section {
  margin-bottom: 60px;
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
  margin-bottom: 30px;
}

.course-card {
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.course-card:hover {
  transform: translateY(-10px);
}

.course-image {
  position: relative;
}

.course-image img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.course-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: #f56c6c;
  color: white;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.price-tag {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: bold;
}

.course-info {
  padding: 20px;
}

.course-info h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
  height: 50px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-teacher {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.course-teacher img {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 10px;
}

.course-teacher span {
  color: #409EFF;
  font-size: 14px;
}

.course-description {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 15px;
  height: 90px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #666;
  font-size: 14px;
}

.course-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
}

.course-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.course-price {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
  margin-right: auto;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.section-title {
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 10px;
  color: #333;
}

.section-subtitle {
  font-size: 16px;
  text-align: center;
  margin-bottom: 30px;
  color: #666;
}

.packages-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.package-card {
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.package-card:hover {
  transform: translateY(-10px);
}

.package-header {
  background-color: #409EFF;
  color: white;
  padding: 20px;
  text-align: center;
}

.package-header h3 {
  font-size: 20px;
  margin-bottom: 10px;
}

.package-price {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.original-price {
  font-size: 16px;
  text-decoration: line-through;
  opacity: 0.8;
}

.current-price {
  font-size: 26px;
  font-weight: bold;
}

.package-content {
  padding: 20px;
}

.package-courses {
  margin-bottom: 20px;
}

.package-course-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.package-course-item:last-child {
  border-bottom: none;
}

.course-icon {
  font-size: 24px;
  color: #409EFF;
}

.course-detail {
  flex: 1;
}

.course-detail h4 {
  font-size: 16px;
  margin-bottom: 5px;
  color: #333;
}

.course-detail p {
  font-size: 14px;
  color: #666;
}

.package-features {
  margin-bottom: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: #666;
}

.feature-item .el-icon {
  color: #67c23a;
}

.package-btn {
  width: 100%;
}

@media (max-width: 1200px) {
  .courses-grid,
  .packages-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .courses-grid,
  .packages-grid {
    grid-template-columns: 1fr;
  }

  .filter-form {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-form .el-form-item {
    margin-right: 0;
    width: 100%;
  }

  .course-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
