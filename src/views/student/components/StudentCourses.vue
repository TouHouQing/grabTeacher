<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

interface Course {
  id: number;
  title: string;
  teacher: string;
  teacherAvatar: string;
  subject: string;
  schedule: string;
  progress: number;
  nextClass: string;
  startDate: string;
  endDate: string;
  totalLessons: number;
  completedLessons: number;
  image: string;
  description: string;
  status: 'active' | 'completed' | 'upcoming';
}

// 模拟课程数据
const courses = ref<Course[]>([
  {
    id: 1,
    title: '初中数学 - 函数与导数',
    teacher: '张老师',
    teacherAvatar: '@/assets/pictures/teacherBoy1.jpeg',
    subject: '数学',
    schedule: '每周一、三 18:00-20:00',
    progress: 60,
    nextClass: '2023-07-15 18:00-20:00',
    startDate: '2023-05-15',
    endDate: '2023-08-15',
    totalLessons: 24,
    completedLessons: 14,
    image: '@/assets/pictures/math1.jpeg',
    description: '本课程深入浅出地讲解初中数学中的函数与导数知识点，适合初二、初三学生。通过系统讲解和大量练习，帮助学生掌握函数与导数的核心概念和解题技巧。',
    status: 'active'
  },
  {
    id: 2,
    title: '初中物理 - 力学与电学',
    teacher: '王老师',
    teacherAvatar: '@/assets/pictures/teacherBoy2.jpeg',
    subject: '物理',
    schedule: '每周二、四 16:00-18:00',
    progress: 45,
    nextClass: '2023-07-12 16:00-18:00',
    startDate: '2023-06-01',
    endDate: '2023-09-01',
    totalLessons: 20,
    completedLessons: 9,
    image: '@/assets/pictures/physics1.jpeg',
    description: '从基础概念到难点突破，全面讲解初中物理力学与电学知识。通过实验演示和题型分析，帮助学生理解物理概念和解题思路。',
    status: 'active'
  },
  {
    id: 3,
    title: '初中英语 - 语法精讲',
    teacher: '李老师',
    teacherAvatar: '@/assets/pictures/teacherBoy3.jpeg',
    subject: '英语',
    schedule: '每周六 10:00-12:00',
    progress: 100,
    nextClass: '已完成',
    startDate: '2023-04-08',
    endDate: '2023-06-24',
    totalLessons: 12,
    completedLessons: 12,
    image: '@/assets/pictures/english1.jpeg',
    description: '系统梳理初中英语语法知识，打牢语法基础，提高英语成绩。通过大量例句和练习，帮助学生掌握语法规则和写作技巧。',
    status: 'completed'
  },
  {
    id: 4,
    title: '初中生物 - 基因与遗传',
    teacher: '陈老师',
    teacherAvatar: '@/assets/pictures/teacherGirl1.jpeg',
    subject: '生物',
    schedule: '每周五 18:00-20:00',
    progress: 0,
    nextClass: '2023-07-21 18:00-20:00',
    startDate: '2023-07-21',
    endDate: '2023-10-20',
    totalLessons: 16,
    completedLessons: 0,
    image: '@/assets/pictures/biology1.jpeg',
    description: '本课程将深入讲解基因与遗传的相关知识，包括DNA结构、基因表达、遗传规律等内容，帮助学生理解生物学中的重要概念。',
    status: 'upcoming'
  }
])

// 课程过滤状态
const activeTab = ref('all')

// 根据标签筛选课程
const filteredCourses = computed(() => {
  if (activeTab.value === 'all') {
    return courses.value
  } else {
    return courses.value.filter(course => course.status === activeTab.value)
  }
})

// 当前查看的课程详情
const currentCourse = ref<Course | null>(null)
const detailDialogVisible = ref(false)

const showCourseDetail = (course: Course) => {
  currentCourse.value = course
  detailDialogVisible.value = true
}

// 下载学习资料
const downloadMaterials = (courseId: number) => {
  ElMessage.success('开始下载课程资料')
}

// 联系教师
const contactTeacher = (teacher: string) => {
  ElMessage.success(`正在连接 ${teacher} 的聊天窗口`)
}

// 进入课堂
const enterClassroom = (course: Course) => {
  if (course.status === 'active') {
    ElMessage.success(`正在进入 ${course.title} 课堂`)
  } else if (course.status === 'upcoming') {
    ElMessage.warning('该课程还未开始')
  } else {
    ElMessage.info('该课程已结束')
  }
}
</script>

<script lang="ts">
export default {
  name: 'StudentCourses'
}
</script>

<template>
  <div class="student-courses">
    <h2>我的课程</h2>

    <!-- 课程分类标签 -->
    <div class="course-tabs">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="全部课程" name="all">
          <div class="tab-content">
            <el-empty v-if="filteredCourses.length === 0" description="暂无课程" />
            <div v-else class="courses-grid">
              <el-card v-for="course in filteredCourses" :key="course.id" class="course-card" :body-style="{ padding: '0px' }">
                <div class="course-image">
                  <img :src="$getImageUrl(course.image)" :alt="course.title">
                  <div class="course-status">
                    <el-tag size="small" :type="course.status === 'active' ? 'success' : course.status === 'upcoming' ? 'warning' : 'info'">
                      {{ course.status === 'active' ? '进行中' : course.status === 'upcoming' ? '即将开始' : '已完成' }}
                    </el-tag>
                  </div>
                </div>
                <div class="course-content">
                  <h3 class="course-title">{{ course.title }}</h3>
                  <div class="course-teacher">
                    <el-avatar :size="30" :src="$getImageUrl(course.teacherAvatar)"></el-avatar>
                    <span>{{ course.teacher }}</span>
                  </div>
                  <div class="course-progress">
                    <span class="progress-text">进度: {{ course.completedLessons }}/{{ course.totalLessons }} 课时</span>
                    <el-progress :percentage="course.progress" :status="course.progress === 100 ? 'success' : ''"></el-progress>
                  </div>
                  <div class="course-schedule">
                    <div class="schedule-item">
                      <el-icon><Calendar /></el-icon>
                      <span>{{ course.schedule }}</span>
                    </div>
                    <div class="schedule-item">
                      <el-icon><Timer /></el-icon>
                      <span>下次课程: {{ course.nextClass }}</span>
                    </div>
                  </div>
                  <div class="course-actions">
                    <el-button size="small" type="primary" @click="enterClassroom(course)">
                      <el-icon><VideoCamera /></el-icon> 进入课堂
                    </el-button>
                    <el-button size="small" type="success" @click="contactTeacher(course.teacher)">
                      <el-icon><ChatDotRound /></el-icon> 联系教师
                    </el-button>
                    <el-button size="small" type="info" @click="showCourseDetail(course)">
                      <el-icon><InfoFilled /></el-icon> 详情
                    </el-button>
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="进行中" name="active">
          <div class="tab-content">
            <el-empty v-if="filteredCourses.length === 0" description="暂无进行中的课程" />
            <div v-else class="courses-grid">
              <el-card v-for="course in filteredCourses" :key="course.id" class="course-card" :body-style="{ padding: '0px' }">
                <div class="course-image">
                  <img :src="$getImageUrl(course.image)" :alt="course.title">
                  <div class="course-status">
                    <el-tag size="small" type="success">进行中</el-tag>
                  </div>
                </div>
                <div class="course-content">
                  <h3 class="course-title">{{ course.title }}</h3>
                  <div class="course-teacher">
                    <el-avatar :size="30" :src="$getImageUrl(course.teacherAvatar)"></el-avatar>
                    <span>{{ course.teacher }}</span>
                  </div>
                  <div class="course-progress">
                    <span class="progress-text">进度: {{ course.completedLessons }}/{{ course.totalLessons }} 课时</span>
                    <el-progress :percentage="course.progress" :status="course.progress === 100 ? 'success' : ''"></el-progress>
                  </div>
                  <div class="course-schedule">
                    <div class="schedule-item">
                      <el-icon><Calendar /></el-icon>
                      <span>{{ course.schedule }}</span>
                    </div>
                    <div class="schedule-item">
                      <el-icon><Timer /></el-icon>
                      <span>下次课程: {{ course.nextClass }}</span>
                    </div>
                  </div>
                  <div class="course-actions">
                    <el-button size="small" type="primary" @click="enterClassroom(course)">
                      <el-icon><VideoCamera /></el-icon> 进入课堂
                    </el-button>
                    <el-button size="small" type="success" @click="contactTeacher(course.teacher)">
                      <el-icon><ChatDotRound /></el-icon> 联系教师
                    </el-button>
                    <el-button size="small" type="info" @click="showCourseDetail(course)">
                      <el-icon><InfoFilled /></el-icon> 详情
                    </el-button>
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="已完成" name="completed">
          <div class="tab-content">
            <el-empty v-if="filteredCourses.length === 0" description="暂无已完成的课程" />
            <div v-else class="courses-grid">
              <el-card v-for="course in filteredCourses" :key="course.id" class="course-card" :body-style="{ padding: '0px' }">
                <div class="course-image">
                  <img :src="$getImageUrl(course.image)" :alt="course.title">
                  <div class="course-status">
                    <el-tag size="small" type="info">已完成</el-tag>
                  </div>
                </div>
                <div class="course-content">
                  <h3 class="course-title">{{ course.title }}</h3>
                  <div class="course-teacher">
                    <el-avatar :size="30" :src="$getImageUrl(course.teacherAvatar)"></el-avatar>
                    <span>{{ course.teacher }}</span>
                  </div>
                  <div class="course-progress">
                    <span class="progress-text">进度: {{ course.completedLessons }}/{{ course.totalLessons }} 课时</span>
                    <el-progress :percentage="course.progress" status="success"></el-progress>
                  </div>
                  <div class="course-schedule">
                    <div class="schedule-item">
                      <el-icon><Calendar /></el-icon>
                      <span>{{ course.schedule }}</span>
                    </div>
                    <div class="schedule-item">
                      <el-icon><Timer /></el-icon>
                      <span>已完成</span>
                    </div>
                  </div>
                  <div class="course-actions">
                    <el-button size="small" type="primary" @click="downloadMaterials(course.id)">
                      <el-icon><Download /></el-icon> 下载资料
                    </el-button>
                    <el-button size="small" type="success" @click="contactTeacher(course.teacher)">
                      <el-icon><ChatDotRound /></el-icon> 联系教师
                    </el-button>
                    <el-button size="small" type="info" @click="showCourseDetail(course)">
                      <el-icon><InfoFilled /></el-icon> 详情
                    </el-button>
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="即将开始" name="upcoming">
          <div class="tab-content">
            <el-empty v-if="filteredCourses.length === 0" description="暂无即将开始的课程" />
            <div v-else class="courses-grid">
              <el-card v-for="course in filteredCourses" :key="course.id" class="course-card" :body-style="{ padding: '0px' }">
                <div class="course-image">
                  <img :src="$getImageUrl(course.image)" :alt="course.title">
                  <div class="course-status">
                    <el-tag size="small" type="warning">即将开始</el-tag>
                  </div>
                </div>
                <div class="course-content">
                  <h3 class="course-title">{{ course.title }}</h3>
                  <div class="course-teacher">
                    <el-avatar :size="30" :src="$getImageUrl(course.teacherAvatar)"></el-avatar>
                    <span>{{ course.teacher }}</span>
                  </div>
                  <div class="course-progress">
                    <span class="progress-text">进度: {{ course.completedLessons }}/{{ course.totalLessons }} 课时</span>
                    <el-progress :percentage="course.progress"></el-progress>
                  </div>
                  <div class="course-schedule">
                    <div class="schedule-item">
                      <el-icon><Calendar /></el-icon>
                      <span>{{ course.schedule }}</span>
                    </div>
                    <div class="schedule-item">
                      <el-icon><Timer /></el-icon>
                      <span>首次课程: {{ course.nextClass }}</span>
                    </div>
                  </div>
                  <div class="course-actions">
                    <el-button size="small" type="warning" disabled>
                      <el-icon><VideoCamera /></el-icon> 未开始
                    </el-button>
                    <el-button size="small" type="success" @click="contactTeacher(course.teacher)">
                      <el-icon><ChatDotRound /></el-icon> 联系教师
                    </el-button>
                    <el-button size="small" type="info" @click="showCourseDetail(course)">
                      <el-icon><InfoFilled /></el-icon> 详情
                    </el-button>
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 课程详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="课程详情"
      width="60%"
    >
      <div v-if="currentCourse" class="course-detail">
        <div class="detail-header">
          <div class="detail-image">
            <img :src="$getImageUrl(currentCourse.image)" :alt="currentCourse.title">
          </div>
          <div class="detail-info">
            <h3>{{ currentCourse.title }}</h3>
            <div class="detail-teacher">
              <el-avatar :size="40" :src="$getImageUrl(currentCourse.teacherAvatar)"></el-avatar>
              <span>{{ currentCourse.teacher }}</span>
            </div>
            <div class="detail-meta">
              <div class="meta-item">
                <el-icon><Collection /></el-icon>
                <span>{{ currentCourse.subject }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Calendar /></el-icon>
                <span>{{ currentCourse.schedule }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Date /></el-icon>
                <span>{{ currentCourse.startDate }} 至 {{ currentCourse.endDate }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Reading /></el-icon>
                <span>{{ currentCourse.totalLessons }} 课时</span>
              </div>
            </div>
            <div class="detail-progress">
              <span>学习进度:</span>
              <el-progress :percentage="currentCourse.progress" :status="currentCourse.progress === 100 ? 'success' : ''"></el-progress>
              <span class="progress-text">{{ currentCourse.completedLessons }}/{{ currentCourse.totalLessons }} 课时</span>
            </div>
            <div class="detail-status">
              <el-tag :type="currentCourse.status === 'active' ? 'success' : currentCourse.status === 'upcoming' ? 'warning' : 'info'">
                {{ currentCourse.status === 'active' ? '进行中' : currentCourse.status === 'upcoming' ? '即将开始' : '已完成' }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="detail-description">
          <h4>课程介绍</h4>
          <p>{{ currentCourse.description }}</p>
        </div>
        <div class="detail-actions">
          <el-button type="primary" @click="enterClassroom(currentCourse)">
            <el-icon><VideoCamera /></el-icon> 进入课堂
          </el-button>
          <el-button type="success" @click="contactTeacher(currentCourse.teacher)">
            <el-icon><ChatDotRound /></el-icon> 联系教师
          </el-button>
          <el-button type="info" @click="downloadMaterials(currentCourse.id)">
            <el-icon><Download /></el-icon> 下载资料
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-courses {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.course-tabs {
  margin-bottom: 20px;
}

.tab-content {
  padding: 20px 0;
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.course-card {
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  height: 100%;
}

.course-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.course-image {
  height: 180px;
  position: relative;
  overflow: hidden;
}

.course-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.course-card:hover .course-image img {
  transform: scale(1.1);
}

.course-status {
  position: absolute;
  top: 15px;
  right: 15px;
  z-index: 2;
}

.course-content {
  padding: 15px;
}

.course-title {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-teacher {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.course-teacher span {
  margin-left: 10px;
  color: #666;
}

.course-progress {
  margin-bottom: 15px;
}

.progress-text {
  display: block;
  margin-bottom: 5px;
  font-size: 14px;
  color: #666;
}

.course-schedule {
  margin-bottom: 15px;
}

.schedule-item {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
  font-size: 14px;
  color: #666;
}

.schedule-item .el-icon {
  margin-right: 5px;
  color: #409eff;
}

.course-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* 课程详情样式 */
.course-detail {
  padding: 10px;
}

.detail-header {
  display: flex;
  margin-bottom: 20px;
}

.detail-image {
  width: 250px;
  height: 180px;
  border-radius: 8px;
  overflow: hidden;
  margin-right: 20px;
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-info {
  flex: 1;
}

.detail-info h3 {
  font-size: 22px;
  margin-bottom: 15px;
  color: #333;
}

.detail-teacher {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.detail-teacher span {
  margin-left: 10px;
  font-size: 16px;
  color: #666;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  color: #666;
}

.meta-item .el-icon {
  margin-right: 5px;
  color: #409eff;
}

.detail-progress {
  margin-bottom: 15px;
}

.detail-progress span {
  display: inline-block;
  margin-right: 10px;
  color: #666;
}

.detail-status {
  margin-bottom: 20px;
}

.detail-description {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.detail-description h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
}

.detail-description p {
  color: #666;
  line-height: 1.6;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .courses-grid {
    grid-template-columns: 1fr;
  }

  .detail-header {
    flex-direction: column;
  }

  .detail-image {
    width: 100%;
    margin-right: 0;
    margin-bottom: 15px;
  }

  .detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>
