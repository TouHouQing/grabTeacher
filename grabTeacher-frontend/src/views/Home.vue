<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useLangStore } from '../stores/lang'
import { courseAPI, teacherAPI } from '../utils/api'
import { Timer, User } from '@element-plus/icons-vue'
import ContactUs from '../components/ContactUs.vue'

// 使用语言store和路由
const langStore = useLangStore()
const router = useRouter()

// 首页推荐教师：来自“精选课程”（courses.is_featured=1）的教师集合
interface RecommendedTeacherItem {
  teacherId: number
  name: string
  subject?: string
  teachingExperience?: number
  introduction?: string
  educationBackground?: string
  specialties?: string
  avatarUrl?: string
  image?: string // 课程封面图作为备用
}

const recommendedTeachers = ref<RecommendedTeacherItem[]>([])
const marqueeList = computed(() => recommendedTeachers.value.concat(recommendedTeachers.value))
const isMarqueeHovered = ref(false)

// 动态计算滚动动画时长，基于固定速度（像素/秒）
const teacherScrollSpeed = 25 // 每秒滚动25像素
const teacherAnimationDuration = ref('60s') // 默认值



const handleTeacherClick = (teacher: RecommendedTeacherItem) => {
  // 跳转到教师详情页面
  router.push(`/teacher-detail/${teacher.teacherId}`)
}

// 首页精选课程：来自"精选课程"（courses.is_featured=1）
interface FeaturedCourseItem {
  id: number
  title: string
  teacherName: string
  description?: string
  imageUrl?: string
  subjectName?: string
  price?: number
  durationMinutes?: number
  courseType?: string
}

const featuredCourses = ref<FeaturedCourseItem[]>([])
const courseMarqueeList = computed(() => featuredCourses.value.concat(featuredCourses.value))
const isCourseMarqueeHovered = ref(false)

// 动态计算课程滚动动画时长，基于固定速度（像素/秒）
const courseScrollSpeed = 20 // 每秒滚动20像素
const courseAnimationDuration = ref('50s') // 默认值

// 计算滚动动画时长的函数
const calculateAnimationDuration = (itemCount: number, itemWidth: number, gap: number, speed: number) => {
  if (itemCount === 0) return '60s'
  // 计算总宽度：项目数量 * 项目宽度 + 间隙数量 * 间隙宽度
  const totalWidth = itemCount * itemWidth + (itemCount - 1) * gap
  // 计算动画时长：总宽度 / 速度
  const duration = totalWidth / speed
  return `${Math.max(duration, 10)}s` // 最小10秒，避免过快
}

// 更新动画时长
const updateAnimationDurations = () => {
  nextTick(() => {
    // 教师卡片：假设每个卡片宽度380px，间隙20px
    teacherAnimationDuration.value = calculateAnimationDuration(
      recommendedTeachers.value.length, 380, 20, teacherScrollSpeed
    )

    // 课程卡片：假设每个卡片宽度320px，间隙20px
    courseAnimationDuration.value = calculateAnimationDuration(
      featuredCourses.value.length, 320, 20, courseScrollSpeed
    )
  })
}

// 共享的精选数据加载函数，课程走精选课程、教师走精选教师
const loadFeaturedData = async () => {
  try {
    // 1) 课程：获取所有精选课程（不分页）
    const respCourses = await courseAPI.getAllFeaturedCourses()
    if (respCourses.success && Array.isArray(respCourses.data)) {
      const courses = respCourses.data as Array<any>
      featuredCourses.value = courses.map((course: any) => ({
        id: course.id,
        title: course.title || '精选课程',
        teacherName: course.teacherName || '名师',
        description: course.description || '',
        imageUrl: course.imageUrl || '',
        subjectName: course.subjectName || '',
        price: course.price || 0,
        durationMinutes: course.durationMinutes || 0,
        courseType: course.courseTypeDisplay || course.courseType || ''
      }))
    }

    // 2) 教师：获取精选教师（不分页）
    const respTeachers = await teacherAPI.getFeaturedList()
    if (respTeachers.success && Array.isArray(respTeachers.data)) {
      const records = respTeachers.data
      recommendedTeachers.value = records.map((t: any) => ({
        teacherId: t.id,
        name: t.realName || '名师',
        subject: (t.subjects && t.subjects.length ? t.subjects[0] : ''),
        teachingExperience: t.teachingExperience || 0,
        introduction: t.introduction || '',
        educationBackground: t.educationBackground || '',
        specialties: t.specialties || '',
        avatarUrl: t.avatarUrl || '',
      }))
    }

    // 数据加载完成后更新动画时长
    updateAnimationDurations()
  } catch (e) {
    // 安静降级：不阻塞主页渲染
    console.warn('加载精选数据失败', e)
  }
}

const handleCourseClick = (course: FeaturedCourseItem) => {
  // 跳转到课程详情页面
  router.push(`/course/${course.id}`)
}

onMounted(() => {
  loadFeaturedData()
})

// 根据当前语言获取评价数据
const testimonials = computed(() => {
  if (langStore.currentLang === 'zh') {
    return [
      {
        name: '张明',
        role: '初三学生',
        content: '通过平台找到了非常适合我的数学老师，半年时间数学成绩从80分提高到了120分，非常感谢！',
        avatar: '@/assets/pictures/studentBoy1.jpeg'
      },
      {
        name: '李华',
        role: '初中生家长',
        content: '孩子在这里学习了三个月，英语成绩有了明显提高，老师很负责任，教学方法也很适合孩子。',
        avatar: '@/assets/pictures/studentGirl2.jpeg'
      },
      {
        name: '王芳',
        role: '小学二年级学生',
        content: '老师教学很有耐心，会根据我的弱点定制学习计划，学习效率比自己学习提高了很多。',
        avatar: '@/assets/pictures/studentGirl1.jpeg'
      }
    ]
  } else {
    return [
      {
        name: 'Zhang Ming',
        role: 'Grade 9 Student',
        content: 'Through the platform, I found a math teacher who is very suitable for me. In half a year, my math score increased from 80 to 120 points. Thank you very much!',
        avatar: '@/assets/pictures/studentBoy1.jpeg'
      },
      {
        name: 'Li Hua',
        role: 'Middle School Parent',
        content: 'My child has been studying here for three months, and his English scores have improved significantly. The teacher is very responsible, and the teaching method is also suitable for children.',
        avatar: '@/assets/pictures/studentGirl2.jpeg'
      },
      {
        name: 'Wang Fang',
        role: 'Grade 2 Student',
        content: 'The teacher is very patient in teaching and will customize a learning plan according to my weaknesses. The learning efficiency is much higher than self-study.',
        avatar: '@/assets/pictures/studentGirl1.jpeg'
      }
    ]
  }
})

// 定义组件名称
defineOptions({
  name: 'HomePage'
})
</script>

<template>
    <div class="home-page">
        <!-- 主横幅 -->
        <div class="banner">
            <div class="banner-content">
                <h1 class="banner-title">Mindrift</h1>
                <div class="banner-subtitle">{{ $t('home.banner.subtitle') }}</div>
                <div class="banner-desc">{{ $t('home.banner.desc') }}</div>
            </div>
        </div>

        <!-- 主要内容区 -->
        <div class="container">
            <!-- 智能匹配系统介绍 -->
            <div class="section">
                <h2 class="section-title">{{ $t('home.sections.findTeacher.title') }}</h2>
                <div class="section-subtitle">{{ $t('home.sections.findTeacher.subtitle') }}</div>
                <div class="feature-grid">
                    <div class="feature-item">
                        <el-icon class="feature-icon"><Medal /></el-icon>
                        <h3>{{ $t('home.sections.findTeacher.features.quality.title') }}</h3>
                        <p>{{ $t('home.sections.findTeacher.features.quality.desc') }}</p>
                    </div>
                    <div class="feature-item">
                        <el-icon class="feature-icon"><Aim /></el-icon>
                        <h3>{{ $t('home.sections.findTeacher.features.matching.title') }}</h3>
                        <p>{{ $t('home.sections.findTeacher.features.matching.desc') }}</p>
                    </div>
                    <div class="feature-item">
                        <el-icon class="feature-icon"><Opportunity /></el-icon>
                        <h3>{{ $t('home.sections.findTeacher.features.personalized.title') }}</h3>
                        <p>{{ $t('home.sections.findTeacher.features.personalized.desc') }}</p>
                    </div>
                    <div class="feature-item">
                        <el-icon class="feature-icon"><Clock /></el-icon>
                        <h3>{{ $t('home.sections.findTeacher.features.flexible.title') }}</h3>
                        <p>{{ $t('home.sections.findTeacher.features.flexible.desc') }}</p>
                    </div>
                </div>
            </div>

            <!-- 推荐教师（来自精选课程 is_featured=1），横向无缝右向左滚动 -->
            <div class="section">
                <h2 class="section-title">{{ $t('home.sections.recommendedTeachers.title') }}</h2>
                <div class="section-subtitle">{{ $t('home.sections.recommendedTeachers.subtitle') }}</div>
                <div class="marquee" v-if="marqueeList.length">
                  <div
                    class="marquee-track"
                    :class="{ 'paused': isMarqueeHovered }"
                    :style="{ '--animation-duration': teacherAnimationDuration }"
                    @mouseenter="isMarqueeHovered = true"
                    @mouseleave="isMarqueeHovered = false"
                  >
                    <div
                      class="marquee-item"
                      v-for="(item, idx) in marqueeList"
                      :key="idx"
                      @click="handleTeacherClick(item)"
                    >
                      <div class="mi-avatar">
                        <img :src="item.avatarUrl ? $getImageUrl(item.avatarUrl) : $getImageUrl('@/assets/pictures/teacherBoy3.jpeg')" :alt="item.name">
                      </div>
                      <div class="mi-info">
                        <div class="mi-name">{{ item.name }}</div>
                        <div class="mi-subject" v-if="item.subject">{{ item.subject }}</div>
                        <div class="mi-experience" v-if="item.teachingExperience">{{ item.teachingExperience }}年教学经验</div>
                        <div class="mi-education" v-if="item.educationBackground">{{ item.educationBackground }}</div>
                        <div class="mi-specialties" v-if="item.specialties">{{ item.specialties }}</div>
                        <div class="mi-intro" v-if="item.introduction">{{ item.introduction }}</div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="view-more">
                    <el-button type="primary" plain @click="$router.push('/famous-teachers')">{{ $t('home.sections.hotCourses.viewMore') }}</el-button>
                </div>
            </div>

            <!-- 热门课程（精选课程 is_featured=1），横向无缝右向左滚动 -->
            <div class="section">
                <h2 class="section-title">{{ $t('home.sections.hotCourses.title') }}</h2>
                <div class="section-subtitle">{{ $t('home.sections.hotCourses.subtitle') }}</div>
                <div class="course-marquee" v-if="courseMarqueeList.length">
                  <div
                    class="course-marquee-track"
                    :class="{ 'paused': isCourseMarqueeHovered }"
                    :style="{ '--animation-duration': courseAnimationDuration }"
                    @mouseenter="isCourseMarqueeHovered = true"
                    @mouseleave="isCourseMarqueeHovered = false"
                  >
                    <div
                      class="course-marquee-item"
                      v-for="(course, idx) in courseMarqueeList"
                      :key="idx"
                      @click="handleCourseClick(course)"
                    >
                      <div class="course-image">
                        <img :src="course.imageUrl ? $getImageUrl(course.imageUrl) : $getImageUrl('@/assets/pictures/courseBackground1.jpeg')" :alt="course.title">
                        <div class="course-price" v-if="course.price">¥{{ course.price }}</div>
                      </div>
                      <div class="course-info">
                        <h3 class="course-title">{{ course.title }}</h3>
                        <p class="course-teacher">{{ course.teacherName }}</p>
                        <p class="course-subject" v-if="course.subjectName">{{ course.subjectName }}</p>
                        <p class="course-description">{{ course.description }}</p>
                        <div class="course-meta">
                          <span v-if="course.durationMinutes">
                            <el-icon><Timer /></el-icon> {{ Math.floor(course.durationMinutes / 60) }}小时
                          </span>
                          <span v-if="course.courseType">
                            <el-icon><User /></el-icon> {{ course.courseType }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="view-more">
                    <el-button type="primary" plain @click="$router.push('/latest-courses')">{{ $t('home.sections.hotCourses.viewMore') }}</el-button>
                </div>
            </div>

            <!-- 用户评价 -->
            <div class="section testimonials">
                <h2 class="section-title">{{ $t('home.sections.testimonials.title') }}</h2>
                <div class="section-subtitle">{{ $t('home.sections.testimonials.subtitle') }}</div>
                <el-carousel :interval="4000" type="card" height="300px">
                    <el-carousel-item v-for="(testimonial, index) in testimonials" :key="index">
                        <div class="testimonial-card">
                            <div class="testimonial-avatar">
                                <img :src="$getImageUrl(testimonial.avatar)" :alt="testimonial.name">
                            </div>
                            <div class="testimonial-content">
                                <p class="testimonial-text">"{{ testimonial.content }}"</p>
                                <div class="testimonial-author">
                                    <h4>{{ testimonial.name }}</h4>
                                    <p>{{ testimonial.role }}</p>
                                </div>
                            </div>
                        </div>
                    </el-carousel-item>
                </el-carousel>
            </div>


        </div>
        <!-- 联系我们 -->
        <ContactUs />
    </div>
</template>

<style>
/* 重置一些基础样式 */
body {
    margin: 0;
    padding: 0;
    width: 100%;
}

#app {
    width: 100%;
}

.el-main {
    padding: 0 !important;
    width: 100%;
}
</style>

<style scoped>
.home-page {
    width: 100%;
}

.banner {
    height: 600px;
    background-image: v-bind('`url(${$getImageUrl("@/assets/pictures/headBackground1.jpeg")})`');
    background-size: cover;
    background-position: center;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    text-align: center;
    position: relative;
}

/* 推荐教师横向滚动 */
.marquee {
  overflow: hidden;
  width: 100%;
  padding: 20px 0;
}
.marquee-track {
  display: flex;
  gap: 20px;
  width: max-content;
  animation: marquee-left var(--animation-duration, 60s) linear infinite;
  will-change: transform;
}
.marquee-track.paused {
  animation-play-state: paused;
}
.marquee-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  min-width: 320px;
  max-width: 380px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}
.marquee-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  border-color: #409eff;
}
.mi-avatar {
  flex-shrink: 0;
}
.mi-avatar img {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #f0f0f0;
}
.mi-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-width: 0;
}
.mi-name {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
  margin-bottom: 4px;
}
.mi-subject {
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
}
.mi-experience {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}
.mi-education {
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mi-specialties {
  font-size: 12px;
  color: #e6a23c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mi-intro {
  font-size: 11px;
  color: #909399;
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  text-overflow: ellipsis;
}

@keyframes marquee-left {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

/* 热门课程横向滚动 */
.course-marquee {
  overflow: hidden;
  width: 100%;
  padding: 20px 0;
}
.course-marquee-track {
  display: flex;
  gap: 20px;
  width: max-content;
  animation: marquee-left var(--animation-duration, 50s) linear infinite;
  will-change: transform;
}
.course-marquee-track.paused {
  animation-play-state: paused;
}
.course-marquee-item {
  display: flex;
  flex-direction: column;
  min-width: 280px;
  max-width: 320px;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}
.course-marquee-item:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  border-color: #409eff;
}
.course-marquee-item .course-image {
  position: relative;
  height: 160px;
  overflow: hidden;
}
.course-marquee-item .course-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.course-marquee-item:hover .course-image img {
  transform: scale(1.05);
}
.course-price {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(64, 158, 255, 0.9);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.course-marquee-item .course-info {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.course-marquee-item .course-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.course-marquee-item .course-teacher {
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
  margin: 0;
}
.course-marquee-item .course-subject {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
  margin: 0;
}
.course-marquee-item .course-description {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  text-overflow: ellipsis;
  flex: 1;
}
.course-marquee-item .course-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}
.course-marquee-item .course-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.banner::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
}

.banner-content {
    position: relative;
    z-index: 10;
}

.banner-title {
    font-size: 64px;
    font-weight: bold;
    margin-bottom: 20px;
}

.banner-subtitle {
    font-size: 36px;
    margin-bottom: 20px;
}

.banner-desc {
    font-size: 20px;
    max-width: 600px;
    margin: 0 auto;
}

.container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 40px 20px;
}

.section {
    margin-bottom: 80px;
}

.section-title {
    font-size: 36px;
    font-weight: bold;
    text-align: center;
    margin-bottom: 10px;
    color: #333;
}

.section-subtitle {
    font-size: 18px;
    text-align: center;
    margin-bottom: 40px;
    color: #666;
}

.feature-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 30px;
}

.feature-item {
    text-align: center;
    padding: 30px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    transition: transform 0.3s ease;
}

.feature-item:hover {
    transform: translateY(-10px);
}

.feature-icon {
    font-size: 48px;
    color: #409EFF;
    margin-bottom: 20px;
}

.feature-item h3 {
    font-size: 20px;
    margin-bottom: 10px;
    color: #333;
}

.feature-item p {
    color: #666;
    line-height: 1.6;
}

.teachers-grid,
.courses-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 30px;
    margin-bottom: 30px;
}

.teacher-card,
.course-card {
    background-color: #fff;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    transition: transform 0.3s ease;
}

.teacher-card:hover,
.course-card:hover {
    transform: translateY(-10px);
}

.teacher-avatar {
    position: relative;
}

.teacher-avatar img {
    width: 100%;
    height: 260px;
    object-fit: cover;
}

.teacher-rating {
    position: absolute;
    bottom: 10px;
    right: 10px;
    background-color: rgba(255, 255, 255, 0.9);
    border-radius: 20px;
    padding: 4px 10px;
}

.teacher-info,
.course-info {
    padding: 20px;
}

.teacher-info h3,
.course-info h3 {
    font-size: 18px;
    margin-bottom: 8px;
    color: #333;
}

.teacher-description,
.course-description {
    color: #666;
    font-size: 14px;
    margin: 10px 0;
    line-height: 1.6;
    height: 70px;
    overflow: hidden;
}

.teacher-schedule {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 15px;
}

.schedule-tag {
    font-size: 12px;
    color: #409EFF;
    background-color: #ecf5ff;
    padding: 4px 8px;
    border-radius: 4px;
}

.booking-btn,
.course-btn {
    width: 100%;
}

.course-image img {
    width: 100%;
    height: 180px;
    object-fit: cover;
}

.course-teacher {
    color: #409EFF;
    font-size: 14px;
}

.course-meta {
    display: flex;
    justify-content: space-between;
    margin: 15px 0;
    color: #666;
    font-size: 14px;
}

.course-meta span {
    display: flex;
    align-items: center;
    gap: 5px;
}

.view-more {
    text-align: center;
}

.testimonials .el-carousel__container {
    height: 300px;
}

.testimonial-card {
    display: flex;
    height: 100%;
    background-color: #fff;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.testimonial-avatar {
    width: 150px;
    flex-shrink: 0;
}

.testimonial-avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.testimonial-content {
    flex: 1;
    padding: 30px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.testimonial-text {
    font-size: 16px;
    font-style: italic;
    color: #333;
    line-height: 1.8;
    margin-bottom: 20px;
}

.testimonial-author h4 {
    font-size: 18px;
    margin-bottom: 5px;
    color: #333;
}

.testimonial-author p {
    color: #666;
}



@media (max-width: 1200px) {
    .feature-grid {
        grid-template-columns: repeat(2, 1fr);
    }

    .teachers-grid,
    .courses-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 768px) {
    .home-page {
        padding-top: 56px; /* 为移动端导航留出空间 */
    }

    .banner {
        height: 400px;
        padding: 0 20px;
    }

    .banner-title {
        font-size: 36px;
        margin-bottom: 16px;
    }

    .banner-subtitle {
        font-size: 24px;
        margin-bottom: 16px;
    }

    .banner-desc {
        font-size: 16px;
        line-height: 1.5;
    }

    .container {
        padding: 30px 16px;
    }

    .section {
        margin-bottom: 40px;
    }

    .section-title {
        font-size: 24px;
        margin-bottom: 12px;
    }

    .section-subtitle {
        font-size: 14px;
        margin-bottom: 24px;
    }

    .feature-grid {
        grid-template-columns: 1fr;
        gap: 20px;
    }

    .feature-item {
        text-align: center;
        padding: 24px 16px;
    }

    .feature-icon {
        font-size: 40px;
        margin-bottom: 16px;
    }

    .feature-item h3 {
        font-size: 18px;
        margin-bottom: 12px;
    }

    .feature-item p {
        font-size: 14px;
        line-height: 1.6;
    }

    .teachers-grid,
    .courses-grid {
        grid-template-columns: 1fr;
        gap: 20px;
    }

    .teacher-card,
    .course-card {
        margin-bottom: 0;
    }

    .teacher-info h3,
    .course-info h3 {
        font-size: 16px;
        margin-bottom: 8px;
    }

    .teacher-info p,
    .course-info p {
        font-size: 13px;
        line-height: 1.5;
    }

    .course-meta {
        flex-direction: column;
        gap: 8px;
        align-items: flex-start;
    }

    .course-meta span {
        font-size: 12px;
    }

    .testimonial-card {
        flex-direction: column;
        text-align: center;
        padding: 20px;
    }

    .testimonial-avatar {
        width: 80px;
        height: 80px;
        margin: 0 auto 16px;
    }

    .testimonial-content {
        margin-left: 0;
    }

    .testimonial-text {
        font-size: 14px;
        line-height: 1.6;
        margin-bottom: 16px;
    }

    .testimonial-author h4 {
        font-size: 16px;
        margin-bottom: 4px;
    }

    .testimonial-author p {
        font-size: 13px;
    }


}

@media (max-width: 480px) {
    .banner {
        height: 350px;
        padding: 0 16px;
    }

    .banner-title {
        font-size: 28px;
        margin-bottom: 12px;
    }

    .banner-subtitle {
        font-size: 20px;
        margin-bottom: 12px;
    }

    .banner-desc {
        font-size: 14px;
    }

    .container {
        padding: 24px 12px;
    }

    .section-title {
        font-size: 20px;
    }

    .feature-item {
        padding: 20px 12px;
    }

    .feature-icon {
        font-size: 36px;
    }

    .teacher-avatar img,
    .course-image img {
        height: 200px;
    }


}
</style>
