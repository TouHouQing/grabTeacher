  <script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'

// 使用语言store
const langStore = useLangStore()

// 根据当前语言获取教师数据
const recommendedTeachers = computed(() => {
  if (langStore.currentLang === 'zh') {
    return [
      {
        name: '张老师',
        subject: '数学',
        experience: 10,
        rating: 4.8,
        description: '数学教育专家，专注于中小学数学教学，善于激发学生学习兴趣。',
        avatar: '@/assets/pictures/teacherBoy1.jpeg',
        schedule: ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00']
      },
      {
        name: '李老师',
        subject: '英语',
        experience: 8,
        rating: 4.9,
        description: '毕业于英国剑桥大学，拥有TESOL证书，擅长英语口语教学。',
        avatar: '@/assets/pictures/teacherBoy2.jpeg',
        schedule: ['周二 18:00-20:00', '周四 18:00-20:00', '周日 14:00-16:00']
      },
      {
        name: '王老师',
        subject: '物理',
        experience: 12,
        rating: 4.7,
        description: '物理学博士，有丰富的教学经验，能将复杂概念简单化。',
        avatar: '@/assets/pictures/teacherBoy3.jpeg',
        schedule: ['周一 16:00-18:00', '周三 16:00-18:00', '周六 14:00-16:00']
      }
    ]
  } else {
    return [
      {
        name: 'Mr. Zhang',
        subject: 'Mathematics',
        experience: 10,
        rating: 4.8,
        description: 'Math education expert, focusing on primary and secondary school mathematics teaching, good at stimulating students\' interest in learning.',
        avatar: '@/assets/pictures/teacherBoy1.jpeg',
        schedule: ['Mon 18:00-20:00', 'Wed 18:00-20:00', 'Sat 10:00-12:00']
      },
      {
        name: 'Mr. Li',
        subject: 'English',
        experience: 8,
        rating: 4.9,
        description: 'Graduated from Cambridge University, UK, with a TESOL certificate, specializing in English speaking teaching.',
        avatar: '@/assets/pictures/teacherBoy2.jpeg',
        schedule: ['Tue 18:00-20:00', 'Thu 18:00-20:00', 'Sun 14:00-16:00']
      },
      {
        name: 'Mr. Wang',
        subject: 'Physics',
        experience: 12,
        rating: 4.7,
        description: 'PhD in Physics, with rich teaching experience, able to simplify complex concepts.',
        avatar: '@/assets/pictures/teacherBoy3.jpeg',
        schedule: ['Mon 16:00-18:00', 'Wed 16:00-18:00', 'Sat 14:00-16:00']
      }
    ]
  }
})

// 根据当前语言获取课程数据
const hotCourses = computed(() => {
  if (langStore.currentLang === 'zh') {
    return [
      {
        title: '初中数学 - 函数与导数',
        teacher: '张老师',
        description: '本课程深入浅出地讲解初中数学中的函数与导数知识点，适合初二、初三学生。',
        image: '@/assets/pictures/teacherGirl1.jpeg',
        duration: '30课时',
        students: 1280
      },
      {
        title: '小学英语 - 语法精讲',
        teacher: '李老师',
        description: '系统梳理小学英语语法知识，打牢语法基础，提高英语成绩。',
        image: '@/assets/pictures/teacherGirl2.jpeg',
        duration: '25课时',
        students: 958
      },
      {
        title: '初中物理 - 力学与电学',
        teacher: '王老师',
        description: '从基础概念到难点突破，全面讲解初中物理力学与电学知识。',
        image: '@/assets/pictures/teacherGirl3.jpeg',
        duration: '28课时',
        students: 876
      }
    ]
  } else {
    return [
      {
        title: 'Middle School Math - Functions and Derivatives',
        teacher: 'Mr. Zhang',
        description: 'This course explains the knowledge points of functions and derivatives in middle school mathematics in a simple way, suitable for students in grades 8-9.',
        image: '@/assets/pictures/teacherGirl1.jpeg',
        duration: '30 lessons',
        students: 1280
      },
      {
        title: 'Primary School English - Grammar Intensive',
        teacher: 'Mr. Li',
        description: 'Systematically combs through primary school English grammar knowledge, lays a solid foundation in grammar, and improves English scores.',
        image: '@/assets/pictures/teacherGirl2.jpeg',
        duration: '25 lessons',
        students: 958
      },
      {
        title: 'Middle School Physics - Mechanics and Electricity',
        teacher: 'Mr. Wang',
        description: 'From basic concepts to difficult breakthroughs, comprehensive explanation of middle school physics mechanics and electricity knowledge.',
        image: '@/assets/pictures/teacherGirl3.jpeg',
        duration: '28 lessons',
        students: 876
      }
    ]
  }
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
                <h1 class="banner-title">GrabTeacher</h1>
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

            <!-- 推荐教师 -->
            <div class="section">
                <h2 class="section-title">{{ $t('home.sections.recommendedTeachers.title') }}</h2>
                <div class="section-subtitle">{{ $t('home.sections.recommendedTeachers.subtitle') }}</div>
                <div class="teachers-grid">
                    <div class="teacher-card" v-for="(teacher, index) in recommendedTeachers" :key="index">
                        <div class="teacher-avatar">
                            <img :src="$getImageUrl(teacher.avatar)" :alt="teacher.name">
                            <div class="teacher-rating">
                                <el-rate v-model="teacher.rating" disabled text-color="#ff9900"></el-rate>
                            </div>
                        </div>
                        <div class="teacher-info">
                            <h3>{{ teacher.name }}</h3>
                            <p>{{ teacher.subject }} | {{ teacher.experience }}{{ $t('home.sections.recommendedTeachers.experience') }}</p>
                            <p class="teacher-description">{{ teacher.description }}</p>
                            <div class="teacher-schedule">
                                <span v-for="(time, i) in teacher.schedule" :key="i" class="schedule-tag">{{ time }}</span>
                            </div>
                            <el-button type="primary" size="small" class="booking-btn">{{ $t('home.sections.recommendedTeachers.bookingBtn') }}</el-button>
                        </div>
                    </div>
                </div>
                <div class="view-more">
                    <el-button type="primary" plain @click="$router.push('/famous-teachers')">{{ $t('home.sections.hotCourses.viewMore') }}</el-button>
                </div>
            </div>

            <!-- 热门课程 -->
            <div class="section">
                <h2 class="section-title">{{ $t('home.sections.hotCourses.title') }}</h2>
                <div class="section-subtitle">{{ $t('home.sections.hotCourses.subtitle') }}</div>
                <div class="courses-grid">
                    <div class="course-card" v-for="(course, index) in hotCourses" :key="index">
                        <div class="course-image">
                            <img :src="$getImageUrl(course.image)" :alt="course.title">
                        </div>
                        <div class="course-info">
                            <h3>{{ course.title }}</h3>
                            <p class="course-teacher">{{ course.teacher }}</p>
                            <p class="course-description">{{ course.description }}</p>
                            <div class="course-meta">
                                <span><el-icon><Timer /></el-icon> {{ course.duration }}</span>
                                <span><el-icon><User /></el-icon> {{ course.students }}{{ $t('home.sections.hotCourses.students') }}</span>
                            </div>
                            <el-button type="primary" size="small" class="course-btn">{{ $t('home.sections.hotCourses.detailBtn') }}</el-button>
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

            <!-- 开始学习引导 -->
            <div class="section cta">
                <h2 class="cta-title">{{ $t('home.sections.cta.title') }}</h2>
                <p class="cta-subtitle">{{ $t('home.sections.cta.subtitle') }}</p>
                <el-button type="primary" size="large" @click="$router.push('/login')">{{ $t('home.sections.cta.button') }}</el-button>
            </div>
        </div>
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

.cta {
    text-align: center;
    background-color: #f0f7ff;
    padding: 60px;
    border-radius: 8px;
}

.cta-title {
    font-size: 32px;
    margin-bottom: 20px;
    color: #333;
}

.cta-subtitle {
    font-size: 18px;
    color: #666;
    margin-bottom: 30px;
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

    .cta {
        padding: 40px 20px;
        text-align: center;
    }

    .cta-title {
        font-size: 24px;
        margin-bottom: 16px;
    }

    .cta-subtitle {
        font-size: 16px;
        margin-bottom: 24px;
        line-height: 1.5;
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

    .cta {
        padding: 32px 16px;
    }

    .cta-title {
        font-size: 20px;
    }

    .cta-subtitle {
        font-size: 14px;
    }
}
</style>
