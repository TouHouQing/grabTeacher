<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Phone, Message, Location, School, Trophy, VideoCamera } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 获取路由传递的教师数据
const teacherId = route.params.id

// 教师信息
const teacher = ref({
  id: teacherId,
  name: '张老师',
  subject: 'Mathematics',
  grade: 'Secondary 3',
  experience: 10,
  rating: 4.8,
  description: '数学教育专家，专注于中小学数学教学，善于激发学生学习兴趣，使用多种教学方法帮助学生理解数学概念。',
  detailedDescription: `张老师拥有北京师范大学数学教育专业硕士学位，有10年丰富的教学经验。

  教学特点：
  1. 善于调动学生学习热情，课堂氛围活跃
  2. 针对不同学生特点，采用个性化教学方法
  3. 注重培养学生的数学思维和解题能力
  4. 教学经验丰富，对升学考试有深入研究

  教学成果：
  - 指导多名学生在数学竞赛中获奖
  - 培养学生考入名校比例高达85%
  - 平均提分率达30%以上`,
  avatar: '@/assets/pictures/teacherBoy1.jpeg',
  videoIntro: '@/assets/video/teacherInfo1.mp4',
  tags: ['趣味教学', '重点突破', '思维导图'],
  schedule: ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00'],
  gender: 'Male',
  teachingStyle: 'Humorous and Witty Style',
  education: '北京师范大学数学教育专业硕士',
  location: '新加坡中部',
  contactPhone: '+65 8888 8888',
  contactEmail: 'zhang.teacher@grabteacher.com',
  achievements: [
    '2020年新加坡优秀教师奖',
    '2018年数学教学创新奖',
    '2016年学生最喜爱教师奖'
  ]
})

// 视频播放控制
const isPlaying = ref(false)
const videoRef = ref()

// 返回上一页
const goBack = () => {
  router.back()
}

// 在真实场景中，我们应该通过API获取教师详情
onMounted(() => {
  // 在这里可以发送API请求获取教师详情
  // 模拟API请求延迟
  setTimeout(() => {
    // 在实际应用中，应该向后端API请求数据
    ElMessage.success('教师详情加载完成')
  }, 500)
})

// 预约课程
const bookLesson = () => {
  ElMessage.success({
    message: `已成功预约${teacher.value.name}的课程！`,
    duration: 3000
  })
}

// 联系教师
const contactTeacher = () => {
  ElMessage.info({
    message: `即将打开与${teacher.value.name}的聊天窗口`,
    duration: 2000
  })
}
</script>

<template>
  <div class="teacher-detail">
    <div class="detail-header">
      <el-button @click="goBack" class="back-button" text bg size="large">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>教师详情</h2>
    </div>

    <div class="detail-content">
      <div class="teacher-profile">
        <div class="profile-header">
          <div class="profile-avatar">
            <img :src="$getImageUrl(teacher.avatar)" :alt="teacher.name">
          </div>
          <div class="profile-basic">
            <h3>{{ teacher.name }}</h3>
            <div class="teacher-rating">
              <el-rate v-model="teacher.rating" disabled text-color="#ff9900" />
              <span class="rating-text">{{ teacher.rating.toFixed(1) }}分</span>
            </div>
            <div class="teacher-tags">
              <div class="tag-group">
                <el-tag type="success" effect="dark" class="subject-tag">{{ teacher.subject }}</el-tag>
                <el-tag type="info" effect="plain" class="grade-tag">{{ teacher.grade }}</el-tag>
                <el-tag type="warning" effect="plain" class="experience-tag">{{ teacher.experience }}年教龄</el-tag>
                <el-tag type="info" effect="plain" class="gender-tag">
                  {{ teacher.gender === 'Male' ? '男' : '女' }}
                </el-tag>
              </div>
              <div class="tag-group">
                <el-tag v-for="(tag, i) in teacher.tags" :key="i" size="small" class="teacher-tag" effect="light">{{ tag }}</el-tag>
              </div>
            </div>
            <div class="profile-info-row">
              <div class="info-item">
                <el-icon><School /></el-icon>
                <span>{{ teacher.education }}</span>
              </div>
              <div class="info-item">
                <el-icon><Location /></el-icon>
                <span>{{ teacher.location }}</span>
              </div>
            </div>
            <div class="profile-info-row">
              <div class="info-item">
                <el-icon><Phone /></el-icon>
                <span>{{ teacher.contactPhone }}</span>
              </div>
              <div class="info-item">
                <el-icon><Message /></el-icon>
                <span>{{ teacher.contactEmail }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="profile-section">
          <h4 class="section-title"><el-icon><Document /></el-icon> 教师介绍</h4>
          <div class="section-content">
            <p v-for="(paragraph, index) in teacher.detailedDescription.split('\n\n')" :key="index" class="description-paragraph">
              {{ paragraph }}
            </p>
          </div>
        </div>

        <div class="profile-section">
          <h4 class="section-title"><el-icon><Trophy /></el-icon> 教学成就</h4>
          <div class="section-content">
            <ul class="achievements-list">
              <li v-for="(achievement, index) in teacher.achievements" :key="index">
                {{ achievement }}
              </li>
            </ul>
          </div>
        </div>

        <div class="profile-section">
          <h4 class="section-title"><el-icon><Calendar /></el-icon> 可授课时间</h4>
          <div class="section-content">
            <div class="schedule-times">
              <el-tag v-for="(time, i) in teacher.schedule" :key="i" class="schedule-tag" type="info" effect="plain">
                {{ time }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="profile-section">
          <h4 class="section-title"><el-icon><VideoCamera /></el-icon> 教师介绍视频</h4>
          <div class="video-container">
            <video
              ref="videoRef"
              :src="$getVideoUrl(teacher.videoIntro)"
              controls
              @play="isPlaying = true"
              @pause="isPlaying = false"
              class="teacher-video"
            ></video>
          </div>
        </div>

        <div class="action-buttons">
          <el-button type="primary" @click="bookLesson" size="large">
            <el-icon><Calendar /></el-icon> 预约课程
          </el-button>
          <el-button type="info" @click="contactTeacher" plain size="large">
            <el-icon><Message /></el-icon> 联系教师
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.teacher-detail {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.back-button {
  margin-right: 20px;
}

h2 {
  font-size: 28px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.detail-content {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  padding: 30px;
  border: 1px solid #f0f0f0;
}

.teacher-profile {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.profile-header {
  display: flex;
  gap: 30px;
  margin-bottom: 20px;
}

.profile-avatar {
  width: 180px;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border: 3px solid #fff;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-basic {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.profile-basic h3 {
  font-size: 24px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.teacher-rating {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-text {
  font-size: 16px;
  color: #ff9900;
  font-weight: 500;
}

.teacher-tags {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.profile-info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 5px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.profile-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 20px;
}

.section-title {
  font-size: 18px;
  color: #333;
  margin-bottom: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-content {
  color: #666;
  line-height: 1.6;
}

.description-paragraph {
  margin-bottom: 15px;
}

.achievements-list {
  padding-left: 20px;
  margin: 0;
}

.achievements-list li {
  margin-bottom: 10px;
}

.schedule-times {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.schedule-tag {
  padding: 8px 15px;
}

.video-container {
  margin-top: 15px;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  background-color: #000;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.teacher-video {
  width: 100%;
  max-height: 450px;
  display: block;
}

.action-buttons {
  display: flex;
  gap: 15px;
  margin-top: 30px;
}
</style>
