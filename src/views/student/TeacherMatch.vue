<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Connection, Timer, Male, Female, Message, Loading, View } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

// 声明图片相对路径，使用getImageUrl方法加载
const teacherImages = {
  teacherBoy1: '@/assets/pictures/teacherBoy1.jpeg',
  teacherBoy2: '@/assets/pictures/teacherBoy2.jpeg',
  teacherBoy3: '@/assets/pictures/teacherBoy3.jpeg',
  teacherGirl1: '@/assets/pictures/teacherGirl1.jpeg',
  teacherGirl4: '@/assets/pictures/teacherGirl4.jpeg'
}

interface Teacher {
  name: string
  subject: string
  grade: string
  experience: number
  rating: number
  description: string
  avatar: string
  tags: string[]
  schedule: string[]
  matchScore: number
  gender: string
  teachingStyle: string
}

const matchForm = reactive({
  subject: '',
  grade: '',
  preferredTime: '',
  preferredDateRange: [], // 修改为日期范围
  gender: '',
  teachingStyle: ''
})

const loading = ref(false)
const showResults = ref(false)
const matchedTeachers = ref<Teacher[]>([])

const router = useRouter()

const handleMatch = () => {
  // 验证必填项
  if (!matchForm.subject || !matchForm.grade) {
    ElMessage.warning('请至少选择科目和年级')
    return
  }

  loading.value = true
  showResults.value = false

  // 模拟API请求延迟
  setTimeout(() => {
    // 在真实环境中，这里应该是向后端发送请求
    // 这里使用预设数据模拟匹配结果
    const allTeachers = [
      {
        name: '张老师',
        subject: 'Mathematics',
        grade: 'Secondary 3',
        experience: 10,
        rating: 4.8,
        description: '数学教育专家，专注于中小学数学教学，善于激发学生学习兴趣，使用多种教学方法帮助学生理解数学概念。',
        avatar: teacherImages.teacherBoy1,
        tags: ['趣味教学', '重点突破', '思维导图'],
        schedule: ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00'],
        matchScore: 98,
        gender: 'Male',
        teachingStyle: 'Humorous and Witty Style'
      },
      {
        name: '王老师',
        subject: 'Science',
        grade: 'Secondary 2',
        experience: 12,
        rating: 4.7,
        description: '物理学博士，有丰富的教学经验，能将复杂概念简单化，善于通过实验和演示帮助学生理解物理原理。',
        avatar: teacherImages.teacherBoy2,
        tags: ['概念解析', '解题技巧', '实验教学'],
        schedule: ['周一 16:00-18:00', '周三 16:00-18:00', '周六 14:00-16:00'],
        matchScore: 92,
        gender: 'Male',
        teachingStyle: 'Rigorous Teaching Style'
      },
      {
        name: '李老师',
        subject: 'English',
        grade: 'Primary 5',
        experience: 8,
        rating: 4.9,
        description: '英语教育专家，拥有海外留学背景，擅长英语口语教学和写作指导，教学方法生动有趣。',
        avatar: teacherImages.teacherGirl1,
        tags: ['口语练习', '阅读指导', '写作提升'],
        schedule: ['周二 16:00-18:00', '周五 18:00-20:00', '周日 10:00-12:00'],
        matchScore: 95,
        gender: 'Female',
        teachingStyle: 'Both Strict and Benevolent Style'
      },
      {
        name: '陈老师',
        subject: 'Mathematics',
        grade: 'O-level',
        experience: 15,
        rating: 4.6,
        description: '资深数学教师，擅长应付考试，指导学生掌握解题技巧和考试策略，帮助学生提高考试成绩。',
        avatar: teacherImages.teacherBoy3,
        tags: ['考试技巧', '题型分析', '快速提分'],
        schedule: ['周一 19:00-21:00', '周四 19:00-21:00', '周六 16:00-18:00'],
        matchScore: 90,
        gender: 'Male',
        teachingStyle: 'Rigorous Teaching Style'
      },
      {
        name: '赵老师',
        subject: 'Science',
        grade: 'Primary 6',
        experience: 7,
        rating: 4.5,
        description: '理科综合教师，擅长自然科学启蒙教育，通过趣味实验和互动方式让孩子爱上科学。',
        avatar: teacherImages.teacherGirl4,
        tags: ['趣味实验', '科学启蒙', '思维培养'],
        schedule: ['周二 14:00-16:00', '周四 14:00-16:00', '周日 10:00-12:00'],
        matchScore: 88,
        gender: 'Female',
        teachingStyle: 'Humorous and Witty Style'
      }
    ];

    // 根据用户选择的条件过滤教师
    let filtered = [...allTeachers];

    if (matchForm.subject) {
      filtered = filtered.filter(teacher => teacher.subject === matchForm.subject);
    }

    if (matchForm.grade) {
      filtered = filtered.filter(teacher => teacher.grade === matchForm.grade);
    }

    if (matchForm.gender) {
      filtered = filtered.filter(teacher => teacher.gender === matchForm.gender);
    }

    if (matchForm.teachingStyle) {
      filtered = filtered.filter(teacher => teacher.teachingStyle === matchForm.teachingStyle);
    }

    // 动态计算匹配分数 - 在实际应用中应由后端计算
    filtered.forEach(teacher => {
      // 基础匹配分数
      let score = 85 + Math.floor(Math.random() * 15);

      // 根据偏好微调分数
      if (matchForm.preferredTime && matchForm.preferredDateRange && matchForm.preferredDateRange.length === 2) {
        // 日期和时间都选择了，提高分数
        score += 8;
      } else if (matchForm.preferredTime || (matchForm.preferredDateRange && matchForm.preferredDateRange.length === 2)) {
        // 只选择了一项，小幅提高分数
        score += 5;
      }

      if (matchForm.gender && teacher.gender === matchForm.gender) {
        score += 3;
      }

      if (matchForm.teachingStyle && teacher.teachingStyle === matchForm.teachingStyle) {
        score += 7;
      }

      // 限制最高分为99
      teacher.matchScore = Math.min(score, 99);
    });

    // 排序结果，匹配分数高的排在前面
    filtered.sort((a, b) => b.matchScore - a.matchScore);

    // 如果筛选结果少于3个，添加更多教师以确保至少有3个结果
    if (filtered.length < 3) {
      // 找出不在筛选结果中的教师
      const remainingTeachers = allTeachers.filter(
        teacher => !filtered.some(t => t.name === teacher.name)
      );

      // 按匹配分数排序
      remainingTeachers.sort((a, b) => b.matchScore - a.matchScore);

      // 添加足够的教师，确保总数为3
      while (filtered.length < 3 && remainingTeachers.length > 0) {
        const nextTeacher = remainingTeachers.shift();
        if (nextTeacher) {
          // 降低匹配分数，表示这是额外推荐的
          nextTeacher.matchScore = Math.max(70, nextTeacher.matchScore - 15);
          filtered.push(nextTeacher);
        }
      }
    }

    // 更新匹配结果，最多显示3个教师
    matchedTeachers.value = filtered.slice(0, 3);

    loading.value = false;
    showResults.value = true;

    ElMessage.success(`为您匹配到了 3 位适合的教师`);
  }, 1500)
}

const resetForm = () => {
  // 分别处理不同类型的字段
  matchForm.subject = '';
  matchForm.grade = '';
  matchForm.preferredTime = '';
  matchForm.preferredDateRange = []; // 数组类型单独重置
  matchForm.gender = '';
  matchForm.teachingStyle = '';

  showResults.value = false;
}

const createOrder = (teacher: Teacher) => {
  // 拼接预约时间信息
  let appointmentInfo = '';
  if (matchForm.preferredDateRange && matchForm.preferredDateRange.length === 2 && matchForm.preferredTime) {
    appointmentInfo = `${matchForm.preferredDateRange[0]} 至 ${matchForm.preferredDateRange[1]} ${matchForm.preferredTime}`;
  }

  // 在真实环境中，这里应该是向后端发送创建订单的请求
  ElMessage.success({
    message: `预约成功！${teacher.name}将在${appointmentInfo ? appointmentInfo : '您选择的时间'}与您联系`,
    duration: 3000
  });

  // 模拟跳转到订单页面
  setTimeout(() => {
    ElMessage.info('即将跳转到订单页面...');
  }, 2000);
}
</script>

<template>
  <div class="teacher-match">
    <h2>智能匹配教师</h2>
    <p class="description">根据您的学习需求和偏好，系统将为您匹配最合适的教师</p>

    <div class="match-container">
      <div class="match-form-section">
        <el-form :model="matchForm" label-position="top" class="match-form">
          <el-form-item label="科目 Subjects" required>
            <el-select v-model="matchForm.subject" placeholder="请选择科目">
              <el-option label="科学 Science" value="Science">
                <div class="option-with-icon">
                  <el-icon><Aim /></el-icon>
                  <span>科学 Science</span>
                </div>
              </el-option>
              <el-option label="数学 Mathematics" value="Mathematics">
                <div class="option-with-icon">
                  <el-icon><DataLine /></el-icon>
                  <span>数学 Mathematics</span>
                </div>
              </el-option>
              <el-option label="英文 English" value="English">
                <div class="option-with-icon">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>英文 English</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="年级 Grades" required>
            <el-select v-model="matchForm.grade" placeholder="请选择年级">
              <el-option-group label="小学 Primary">
                <el-option label="Primary 1" value="Primary 1" />
                <el-option label="Primary 2" value="Primary 2" />
                <el-option label="Primary 3" value="Primary 3" />
                <el-option label="Primary 4" value="Primary 4" />
                <el-option label="Primary 5" value="Primary 5" />
                <el-option label="Primary 6" value="Primary 6" />
              </el-option-group>
              <el-option-group label="中学 Secondary">
                <el-option label="Secondary 1" value="Secondary 1" />
                <el-option label="Secondary 2" value="Secondary 2" />
                <el-option label="Secondary 3" value="Secondary 3" />
                <el-option label="Secondary 4" value="Secondary 4" />
              </el-option-group>
              <el-option-group label="考试 Exam">
                <el-option label="O-level" value="O-level" />
                <el-option label="A-level" value="A-level" />
              </el-option-group>
            </el-select>
          </el-form-item>

          <el-form-item label="期望授课日期 Preferred Date">
            <el-date-picker
              v-model="matchForm.preferredDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :disabled-date="(date) => date < new Date()"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="期望授课时间 Preferred Time">
            <el-time-select
              v-model="matchForm.preferredTime"
              start="08:00"
              step="00:30"
              end="22:00"
              placeholder="选择期望授课时间"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="教师性别 Genders">
            <el-radio-group v-model="matchForm.gender" size="large">
              <el-radio-button label="Male">
                <el-icon><Male /></el-icon> 男 Male
              </el-radio-button>
              <el-radio-button label="Female">
                <el-icon><Female /></el-icon> 女 Female
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="教师授课风格 Teacher's Styles">
            <el-radio-group v-model="matchForm.teachingStyle" size="large">
              <el-radio border label="Humorous and Witty Style">幽默风趣型</el-radio>
              <el-radio border label="Rigorous Teaching Style">严谨教学型</el-radio>
              <el-radio border label="Both Strict and Benevolent Style">严慈相济型</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item>
            <div class="form-buttons">
              <el-button type="primary" :loading="loading" @click="handleMatch" size="large">
                <el-icon><Connection /></el-icon> 开始匹配
              </el-button>
              <el-button @click="resetForm" size="large">
                <el-icon><Refresh /></el-icon> 重置
              </el-button>
            </div>
          </el-form-item>
        </el-form>

        <div class="match-tips">
          <el-alert
            title="匹配小提示"
            type="info"
            description="精确填写您的偏好信息，可以获得更精准的教师匹配结果。选择具体日期和时间会提高匹配精度。"
            show-icon
            :closable="false"
          />
        </div>
      </div>

      <div v-if="showResults" class="match-results-section">
        <h3>匹配结果</h3>
        <p class="results-desc">根据您的需求，我们为您找到了以下最匹配的教师</p>

        <div class="teachers-list">
          <div v-for="(teacher, index) in matchedTeachers" :key="index" class="teacher-card">
            <div class="teacher-avatar">
              <img :src="$getImageUrl(teacher.avatar)" :alt="teacher.name">
              <div class="match-score">
                <div class="score">{{ teacher.matchScore }}%</div>
                <div class="match-text">匹配度</div>
              </div>
            </div>
            <div class="teacher-info">
              <div class="teacher-header">
                <h4>{{ teacher.name }}</h4>
                <div class="teacher-rating">
                  <el-rate v-model="teacher.rating" disabled text-color="#ff9900" />
                  <span class="rating-text">{{ teacher.rating.toFixed(1) }}分</span>
                </div>
              </div>
              <div class="teacher-subject">
                <el-tag type="success" effect="dark" class="subject-tag">{{ teacher.subject }}</el-tag>
                <el-tag type="info" effect="plain" class="grade-tag">{{ teacher.grade }}</el-tag>
                <el-tag type="warning" effect="plain" class="experience-tag">{{ teacher.experience }}年教龄</el-tag>
                <el-tag type="info" effect="plain" class="gender-tag">
                  <el-icon v-if="teacher.gender === 'Male'"><Male /></el-icon>
                  <el-icon v-else><Female /></el-icon>
                  {{ teacher.gender === 'Male' ? '男' : '女' }}
                </el-tag>
              </div>
              <p class="teacher-description">{{ teacher.description }}</p>
              <div class="teacher-tags">
                <el-tag v-for="(tag, i) in teacher.tags" :key="i" size="small" class="teacher-tag" effect="light">{{ tag }}</el-tag>
              </div>
              <div class="teacher-schedule">
                <div class="schedule-title">可授课时间：</div>
                <div class="schedule-times">
                  <span v-for="(time, i) in teacher.schedule" :key="i" class="schedule-tag">
                    <el-icon><Timer /></el-icon> {{ time }}
                  </span>
                </div>
              </div>
              <div class="selected-time" v-if="matchForm.preferredDateRange && matchForm.preferredDateRange.length === 2 && matchForm.preferredTime">
                <div class="selected-time-title">您的预约时间：</div>
                <div class="selected-time-value">
                  <el-tag type="success">
                    <el-icon><Calendar /></el-icon>
                    {{ matchForm.preferredDateRange[0] }} 至 {{ matchForm.preferredDateRange[1] }} {{ matchForm.preferredTime }}
                  </el-tag>
                </div>
              </div>
              <div class="teacher-actions">
                <el-button type="primary" @click="createOrder(teacher)" size="large">
                  <el-icon><Calendar /></el-icon> 预约课程
                </el-button>
                <el-button type="info" plain size="large">
                  <el-icon><Message /></el-icon> 联系教师
                </el-button>
                <el-button type="success" plain size="large" @click="router.push(`/student/teacher-detail/${teacher.name}`)">
                  <el-icon><View /></el-icon> 查看详情
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="loading" class="match-loading-section">
        <div class="loading-content">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p class="loading-text">正在为您匹配最合适的教师，请稍候...</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.teacher-match {
  padding: 20px;
}

h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
  font-weight: 600;
}

.description {
  font-size: 16px;
  color: #666;
  margin-bottom: 30px;
}

.match-container {
  display: flex;
  gap: 30px;
}

.match-form-section {
  flex: 1;
  background-color: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  max-width: 400px;
  position: sticky;
  top: 20px;
  height: fit-content;
  border: 1px solid #f0f0f0;
}

.match-form {
  margin-bottom: 30px;
}

.match-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #333;
}

.match-form :deep(.el-select) {
  width: 100%;
}

.form-buttons {
  display: flex;
  gap: 15px;
  margin-top: 30px;
}

.match-results-section {
  flex: 2;
  background-color: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
}

.match-results-section h3 {
  font-size: 22px;
  color: #333;
  margin-bottom: 10px;
  font-weight: 600;
}

.results-desc {
  font-size: 16px;
  color: #666;
  margin-bottom: 30px;
}

.teachers-list {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.teacher-card {
  display: flex;
  background-color: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s, box-shadow 0.3s;
  border: 1px solid #f0f0f0;
}

.teacher-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.teacher-avatar {
  width: 200px;
  height: 240px;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

.teacher-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.match-score {
  position: absolute;
  top: 15px;
  right: 15px;
  width: 60px;
  height: 60px;
  background-color: rgba(64, 158, 255, 0.9);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.4);
}

.score {
  font-size: 18px;
  line-height: 1;
}

.match-text {
  font-size: 12px;
  opacity: 0.9;
}

.teacher-info {
  flex: 1;
  padding: 25px;
  display: flex;
  flex-direction: column;
}

.teacher-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.teacher-header h4 {
  font-size: 22px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.teacher-rating {
  display: flex;
  align-items: center;
}

.teacher-subject {
  color: #606266;
  margin-bottom: 15px;
  font-size: 15px;
  border-bottom: 1px dashed #eee;
  padding-bottom: 15px;
}

.teacher-description {
  color: #333;
  margin-bottom: 20px;
  line-height: 1.6;
  flex: 1;
}

.teacher-tags {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.teacher-tag {
  background-color: #ecf5ff;
  color: #409eff;
  border: none;
}

.teacher-schedule {
  margin-bottom: 15px;
}

.schedule-title {
  font-weight: 500;
  color: #333;
  margin-bottom: 10px;
}

.schedule-times {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.schedule-tag {
  display: inline-block;
  padding: 6px 12px;
  background-color: #f8f8f8;
  color: #666;
  border-radius: 4px;
  font-size: 13px;
}

.selected-time {
  margin-bottom: 20px;
  background-color: #f0f9eb;
  padding: 10px;
  border-radius: 8px;
}

.selected-time-title {
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.teacher-actions {
  display: flex;
  gap: 15px;
}

.match-loading-section {
  flex: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background-color: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.loading-content {
  text-align: center;
}

.loading-icon {
  font-size: 48px;
  color: #409eff;
  animation: rotate 2s linear infinite;
}

.loading-text {
  margin-top: 20px;
  font-size: 16px;
  color: #666;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1200px) {
  .match-container {
    flex-direction: column;
  }

  .match-form-section {
    max-width: 100%;
    position: static;
  }
}

@media (max-width: 768px) {
  .teacher-card {
    flex-direction: column;
  }

  .teacher-avatar {
    width: 100%;
    height: 200px;
  }

  .match-score {
    top: 10px;
    right: 10px;
    width: 50px;
    height: 50px;
  }

  .score {
    font-size: 16px;
  }

  .match-text {
    font-size: 10px;
  }
}
</style>

