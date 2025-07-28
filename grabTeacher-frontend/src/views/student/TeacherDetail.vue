<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Phone, Message, Location, School, Trophy, VideoCamera, ArrowLeft, Loading } from '@element-plus/icons-vue'
import { teacherAPI } from '@/utils/api'
import teacherBoy1 from '@/assets/pictures/teacherBoy1.jpeg'
import teacherBoy2 from '@/assets/pictures/teacherBoy2.jpeg'
import teacherBoy3 from '@/assets/pictures/teacherBoy3.jpeg'
import teacherGirl1 from '@/assets/pictures/teacherGirl1.jpeg'
import teacherGirl2 from '@/assets/pictures/teacherGirl2.jpeg'
import teacherGirl3 from '@/assets/pictures/teacherGirl3.jpeg'
import teacherGirl4 from '@/assets/pictures/teacherGirl4.jpeg'
import studentGirl2 from '@/assets/pictures/studentGirl2.jpeg'
import teacherBoy1v from '@/assets/video/teacherInfo1.mp4'

const route = useRoute()
const router = useRouter()

// 获取路由传递的教师ID并转换为数字
const teacherId = parseInt(route.params.id as string)

// 教师信息状态
const teacher = ref(null)
const loading = ref(true)

// 默认教师数据库（作为后备数据）
const teachersData = {
  1: {
    id: 1,
    name: '张老师',
    subject: '数学',
    grade: '初中',
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
    avatar: teacherBoy1,
    videoIntro: teacherBoy1v,
    tags: ['趣味教学', '重点突破', '思维导图'],
    schedule: ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00'],
    gender: 'Male',
    teachingStyle: '幽默风趣',
    education: '北京师范大学数学教育专业硕士',
    location: '新加坡中部',
    contactPhone: '+65 8888 8888',
    contactEmail: 'zhang.teacher@grabteacher.com',
    achievements: [
      '2020年新加坡优秀教师奖',
      '2018年数学教学创新奖',
      '2016年学生最喜爱教师奖'
    ]
  },
  2: {
    id: 2,
    name: '李老师',
    subject: '英语',
    grade: '初中',
    experience: 8,
    rating: 4.9,
    description: '毕业于英国剑桥大学，拥有TESOL证书，擅长英语口语教学，注重学生的语言应用能力培养。',
    detailedDescription: `李老师毕业于英国剑桥大学，拥有TESOL证书，有8年丰富的教学经验。

教学特点：
1. 纯正英式发音，注重语音语调训练
2. 互动式教学，提高学生口语表达能力
3. 结合国际教材，培养学生国际视野
4. 个性化指导，针对学生薄弱环节重点突破

教学成果：
- 培养学生雅思托福高分率达90%
- 指导学生参加国际英语竞赛获奖
- 学生口语表达能力显著提升`,
    avatar: teacherBoy2,
    videoIntro: teacherBoy1v,
    tags: ['发音纠正', '口语强化', '语法精通'],
    schedule: ['周二 18:00-20:00', '周四 18:00-20:00', '周日 14:00-16:00'],
    gender: 'Male',
    teachingStyle: '互动式教学',
    education: '英国剑桥大学英语教育专业硕士',
    location: '新加坡东部',
    contactPhone: '+65 8888 8889',
    contactEmail: 'li.teacher@grabteacher.com',
    achievements: [
      '2021年国际英语教学奖',
      '2019年TESOL优秀教师奖',
      '2017年学生口语提升突出贡献奖'
    ]
  },
  3: {
    id: 3,
    name: '王老师',
    subject: '物理',
    grade: '初中',
    experience: 12,
    rating: 4.7,
    description: '物理学博士，有丰富的教学经验，能将复杂概念简单化，善于通过实验和演示帮助学生理解物理原理。',
    detailedDescription: `王老师是物理学博士，有12年丰富的教学经验。

教学特点：
1. 理论与实践相结合，注重实验教学
2. 将复杂物理概念简单化，易于理解
3. 启发式教学，培养学生科学思维
4. 针对考试重点，提高解题技巧

教学成果：
- 指导学生参加物理竞赛获得优异成绩
- 学生物理成绩平均提升40%
- 培养多名学生考入理工类名校`,
    avatar: teacherBoy3,
    videoIntro: teacherBoy1v,
    tags: ['概念解析', '解题技巧', '高考冲刺'],
    schedule: ['周一 16:00-18:00', '周三 16:00-18:00', '周六 14:00-16:00'],
    gender: 'Male',
    teachingStyle: '实验教学',
    education: '清华大学物理学博士',
    location: '新加坡西部',
    contactPhone: '+65 8888 8890',
    contactEmail: 'wang.teacher@grabteacher.com',
    achievements: [
      '2020年物理教学创新奖',
      '2018年优秀博士教师奖',
      '2016年学生竞赛指导奖'
    ]
  },
  4: {
    id: 4,
    name: '刘老师',
    subject: '化学',
    grade: '初中',
    experience: 15,
    rating: 4.9,
    description: '化学教育硕士，从事一线教学工作15年，教学方法灵活多样，注重培养学生的实验能力和科学思维。',
    detailedDescription: `刘老师是化学教育硕士，从事一线教学工作15年。

教学特点：
1. 实验教学丰富，激发学生学习兴趣
2. 系统性强，注重知识点之间的联系
3. 因材施教，根据学生特点调整教学方法
4. 注重培养学生的科学素养和实验技能

教学成果：
- 指导学生化学实验竞赛多次获奖
- 学生化学成绩提升率达95%
- 培养学生养成良好的科学思维习惯`,
    avatar: teacherGirl1,
    videoIntro: teacherBoy1v,
    tags: ['实验教学', '概念讲解', '解题方法'],
    schedule: ['周二 16:00-18:00', '周五 18:00-20:00', '周日 10:00-12:00'],
    gender: 'Female',
    teachingStyle: '实验导向',
    education: '华东师范大学化学教育硕士',
    location: '新加坡南部',
    contactPhone: '+65 8888 8891',
    contactEmail: 'liu.teacher@grabteacher.com',
    achievements: [
      '2021年化学教学优秀奖',
      '2019年实验教学创新奖',
      '2017年学生最喜爱女教师奖'
    ]
  },
  5: {
    id: 5,
    name: '陈老师',
    subject: '数学',
    grade: '初中',
    experience: 7,
    rating: 4.6,
    description: '数学教育专业毕业，擅长启发式教学，能够根据学生的特点制定个性化的学习计划。',
    detailedDescription: `陈老师数学教育专业毕业，有7年教学经验。

教学特点：
1. 启发式教学，培养学生独立思考能力
2. 个性化教学计划，因材施教
3. 注重基础夯实，循序渐进
4. 耐心细致，善于鼓励学生

教学成果：
- 帮助基础薄弱学生显著提升成绩
- 培养学生数学学习兴趣和自信心
- 学生数学思维能力明显改善`,
    avatar: teacherGirl2,
    videoIntro: teacherBoy1v,
    tags: ['基础夯实', '思维训练', '难题攻克'],
    schedule: ['周一 15:00-17:00', '周四 16:00-18:00', '周六 16:00-18:00'],
    gender: 'Female',
    teachingStyle: '启发式教学',
    education: '北京师范大学数学教育专业学士',
    location: '新加坡北部',
    contactPhone: '+65 8888 8892',
    contactEmail: 'chen.teacher@grabteacher.com',
    achievements: [
      '2020年青年教师优秀奖',
      '2018年学生进步指导奖',
      '2016年数学教学新秀奖'
    ]
  },
  6: {
    id: 6,
    name: '赵老师',
    subject: '生物',
    grade: '初中',
    experience: 9,
    rating: 4.8,
    description: '生物学硕士，有丰富的教学经验，擅长将生物学知识与日常生活相结合，让学习更加生动有趣。',
    detailedDescription: `赵老师是生物学硕士，有9年丰富的教学经验。

教学特点：
1. 生动有趣的教学方式，寓教于乐
2. 理论联系实际，与生活紧密结合
3. 注重培养学生的观察和思考能力
4. 善用多媒体教学，直观展示生物现象

教学成果：
- 学生生物科学素养显著提升
- 指导学生参加生物竞赛获得佳绩
- 激发学生对生命科学的浓厚兴趣`,
    avatar: teacherGirl3,
    videoIntro: teacherBoy1v,
    tags: ['实验演示', '概念讲解', '考点梳理'],
    schedule: ['周二 19:00-21:00', '周五 16:00-18:00', '周日 16:00-18:00'],
    gender: 'Female',
    teachingStyle: '生动有趣',
    education: '复旦大学生物学硕士',
    location: '新加坡中部',
    contactPhone: '+65 8888 8893',
    contactEmail: 'zhao.teacher@grabteacher.com',
    achievements: [
      '2021年生物教学创新奖',
      '2019年科普教育贡献奖',
      '2017年学生最受欢迎教师奖'
    ]
  },
  7: {
    id: 7,
    name: '杨老师',
    subject: '英语',
    grade: '小学',
    experience: 5,
    rating: 4.9,
    description: '英语专业毕业，有海外留学经验，擅长通过游戏、歌曲等形式激发孩子学习英语的兴趣。',
    detailedDescription: `杨老师英语专业毕业，有海外留学经验，专注小学英语教学5年。

教学特点：
1. 趣味教学法，通过游戏歌曲学英语
2. 注重培养孩子的英语学习兴趣
3. 纯正发音，从小培养正确语感
4. 耐心细致，善于与孩子沟通

教学成果：
- 学生英语学习兴趣浓厚，积极性高
- 发音标准，口语表达自然流畅
- 词汇量和语法基础扎实`,
    avatar: teacherGirl4,
    videoIntro: teacherBoy1v,
    tags: ['趣味教学', '语音纠正', '词汇积累'],
    schedule: ['周三 15:00-17:00', '周五 15:00-17:00', '周六 10:00-12:00'],
    gender: 'Female',
    teachingStyle: '趣味互动',
    education: '外国语大学英语专业学士',
    location: '新加坡东部',
    contactPhone: '+65 8888 8894',
    contactEmail: 'yang.teacher@grabteacher.com',
    achievements: [
      '2020年少儿英语教学奖',
      '2018年创意教学方法奖',
      '2016年最受学生喜爱奖'
    ]
  },
  8: {
    id: 8,
    name: '周老师',
    subject: '物理',
    grade: '初中',
    experience: 8,
    rating: 4.7,
    description: '物理教育专业毕业，擅长实验教学，能够通过实验激发学生的学习兴趣，培养学生的动手能力。',
    detailedDescription: `周老师物理教育专业毕业，有8年教学经验。

教学特点：
1. 实验教学为主，理论结合实践
2. 注重培养学生动手操作能力
3. 激发学生对物理的探索兴趣
4. 循循善诱，耐心解答学生疑问

教学成果：
- 学生动手实验能力强，理解深刻
- 物理学习兴趣和成绩双提升
- 培养学生科学探究精神`,
    avatar: studentGirl2,
    videoIntro: teacherBoy1v,
    tags: ['实验教学', '概念讲解', '题型分析'],
    schedule: ['周一 17:00-19:00', '周四 17:00-19:00', '周日 14:00-16:00'],
    gender: 'Female',
    teachingStyle: '实验导向',
    education: '华中师范大学物理教育专业学士',
    location: '新加坡西部',
    contactPhone: '+65 8888 8895',
    contactEmail: 'zhou.teacher@grabteacher.com',
    achievements: [
      '2019年实验教学优秀奖',
      '2017年青年教师成长奖',
      '2015年学生实践指导奖'
    ]
  }
}

// 视频播放控制
const isPlaying = ref(false)
const videoRef = ref()

// 返回上一页
const goBack = () => {
  router.back()
}

// 获取教师详情数据
const fetchTeacherDetail = async () => {
  try {
    loading.value = true
    const result = await teacherAPI.getDetail(teacherId)

    if (result.success && result.data) {
      // 转换后端数据格式为前端需要的格式
      const teacherData = result.data
      teacher.value = {
        id: teacherData.id,
        name: teacherData.realName,
        subject: teacherData.subjects && teacherData.subjects.length > 0 ? teacherData.subjects[0] : '未设置',
        grade: teacherData.grades && teacherData.grades.length > 0 ? teacherData.grades.join('、') : '未设置',
        experience: teacherData.teachingExperience || 0,
        rating: 4.8, // 暂时使用默认值
        description: teacherData.introduction || '暂无介绍',
        detailedDescription: teacherData.introduction || '暂无详细介绍',
        avatar: teacherData.avatarUrl || teacherBoy1, // 使用用户头像或默认头像
        videoIntro: teacherData.videoIntroUrl || teacherBoy1v,
        tags: teacherData.specialties ? teacherData.specialties.split(',') : [],
        schedule: ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00'], // 暂时使用默认值
        gender: teacherData.gender || 'Male',
        teachingStyle: '个性化教学',
        education: teacherData.educationBackground || '暂无信息',
        location: '新加坡',
        contactPhone: teacherData.phone || '未设置',
        contactEmail: teacherData.email || '未设置',
        achievements: ['优秀教师奖']
      }
      ElMessage.success(`${teacher.value.name}的详情加载完成`)
    } else {
      // 如果API失败，使用默认数据
      if (teachersData[teacherId]) {
        teacher.value = teachersData[teacherId]
        ElMessage.warning('使用默认教师数据')
      } else {
        ElMessage.error('教师信息不存在')
        router.push('/famous-teachers')
        return
      }
    }
  } catch (error) {
    console.error('获取教师详情失败:', error)
    // 如果API失败，使用默认数据
    if (teachersData[teacherId]) {
      teacher.value = teachersData[teacherId]
      ElMessage.warning('网络异常，使用默认教师数据')
    } else {
      ElMessage.error('教师信息不存在')
      router.push('/famous-teachers')
      return
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 页面加载时滚动到顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })

  // 获取教师详情
  fetchTeacherDetail()
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
      <div v-if="loading" class="loading-container">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <p>正在加载教师详情...</p>
      </div>
      <div v-else-if="teacher" class="teacher-profile">
        <div class="profile-header">
          <div class="profile-avatar">
            <img :src="teacher.avatar" :alt="teacher.name">
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
          <h4 class="section-title"><el-icon><VideoCamera /></el-icon> 教师介绍视频</h4>
          <div class="video-container">
            <video
              ref="videoRef"
              :src="teacher.videoIntro"
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
      <div v-else class="error-container">
        <p>教师信息加载失败</p>
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

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #666;
}

.loading-icon {
  font-size: 48px;
  color: #409eff;
  animation: rotate 2s linear infinite;
  margin-bottom: 20px;
}

.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #999;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
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
