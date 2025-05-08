<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, List, Timer, VideoCamera, ChatDotRound,
         Download, Upload, Location, Document, Edit,
         ArrowLeft, ArrowRight, InfoFilled, Checked } from '@element-plus/icons-vue'

interface ScheduleClass {
  id: number;
  title: string;
  date: string;
  time: string;
  duration: number; // 分钟
  teacher: string;
  teacherAvatar: string;
  subject: string;
  location: string;
  content: string;
  status: 'upcoming' | 'ongoing' | 'completed' | 'cancelled';
  homework?: string;
  position?: number; // 用于防止重叠
}

// 模拟课程数据
const classes = ref<ScheduleClass[]>([
  {
    id: 1,
    title: '高中数学 - 函数与导数',
    date: '2023-07-15',
    time: '18:00-20:00',
    duration: 120,
    teacher: '张老师',
    teacherAvatar: 'https://img.syt5.com/2021/0908/20210908055012420.jpg.420.580.jpg',
    subject: '数学',
    location: '线上课堂',
    content: '函数与导数的应用题解题技巧',
    status: 'upcoming',
    homework: '完成课本P123-125练习题'
  },
  {
    id: 2,
    title: '高中物理 - 力学与电学',
    date: '2023-07-12',
    time: '16:00-18:00',
    duration: 120,
    teacher: '王老师',
    teacherAvatar: 'https://img.syt5.com/2021/0908/20210908055050886.jpg.420.580.jpg',
    subject: '物理',
    location: '线上课堂',
    content: '电磁感应现象及应用',
    status: 'upcoming'
  },
  {
    id: 3,
    title: '高中数学 - 函数与导数',
    date: '2023-07-08',
    time: '18:00-20:00',
    duration: 120,
    teacher: '张老师',
    teacherAvatar: 'https://img.syt5.com/2021/0908/20210908055012420.jpg.420.580.jpg',
    subject: '数学',
    location: '线上课堂',
    content: '导数的几何意义与物理意义',
    status: 'completed',
    homework: '完成课本P118-120练习题'
  },
  {
    id: 4,
    title: '高中物理 - 力学与电学',
    date: '2023-07-05',
    time: '16:00-18:00',
    duration: 120,
    teacher: '王老师',
    teacherAvatar: 'https://img.syt5.com/2021/0908/20210908055050886.jpg.420.580.jpg',
    subject: '物理',
    location: '线上课堂',
    content: '电场与磁场的关系',
    status: 'completed',
    homework: '完成课本P85-87练习题'
  },
  {
    id: 5,
    title: '高中生物 - 基因与遗传',
    date: '2023-07-21',
    time: '18:00-20:00',
    duration: 120,
    teacher: '陈老师',
    teacherAvatar: 'https://img.syt5.com/2021/0908/20210908055031962.jpg.420.580.jpg',
    subject: '生物',
    location: '线上课堂',
    content: '基因的概念与DNA结构',
    status: 'upcoming'
  }
])

// 当前视图模式
const viewMode = ref('calendar') // calendar 或 list

// 当前日期
const currentDate = ref(new Date())

// 课程详情
const selectedClass = ref<ScheduleClass | null>(null)
const detailDialogVisible = ref(false)

const showClassDetail = (scheduleClass: ScheduleClass) => {
  selectedClass.value = scheduleClass
  detailDialogVisible.value = true
}

// 根据状态获取不同类型的课程
const upcomingClasses = computed(() => {
  return classes.value.filter(c => c.status === 'upcoming')
      .sort((a, b) => new Date(a.date + ' ' + a.time.split('-')[0]).getTime() -
                     new Date(b.date + ' ' + b.time.split('-')[0]).getTime())
})

const completedClasses = computed(() => {
  return classes.value.filter(c => c.status === 'completed')
      .sort((a, b) => new Date(b.date + ' ' + b.time.split('-')[0]).getTime() -
                     new Date(a.date + ' ' + a.time.split('-')[0]).getTime())
})

// 日历相关
// 判断某天是否有课程
const isDateHasClass = (date: Date) => {
  const dateStr = date.toISOString().split('T')[0]
  return classes.value.some(c => c.date === dateStr)
}

// 获取某天的所有课程
const getDateClasses = (date: Date) => {
  const dateStr = date.toISOString().split('T')[0]
  return classes.value.filter(c => c.date === dateStr)
      .sort((a, b) => a.time.split('-')[0].localeCompare(b.time.split('-')[0]))
}

// 日历导航函数
const prevMonth = () => {
  const date = new Date(currentDate.value)
  date.setMonth(date.getMonth() - 1)
  currentDate.value = date
}

const nextMonth = () => {
  const date = new Date(currentDate.value)
  date.setMonth(date.getMonth() + 1)
  currentDate.value = date
}

const today = () => {
  currentDate.value = new Date()
}

// 格式化日历标题
const formatCalendarTitle = (date: Date) => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  return `${year}年${month}月`
}

// 获取某天的所有课程并计算位置防止重叠
const getDateClassesWithPosition = (date: Date) => {
  const classes = getDateClasses(date)

  // 按时间排序
  const sortedClasses = [...classes].sort((a, b) =>
    a.time.split('-')[0].localeCompare(b.time.split('-')[0])
  )

  // 计算位置以防止重叠
  let lastEndTime = ''
  let lastPosition = 0

  return sortedClasses.map((scheduleClass, index) => {
    const startTime = scheduleClass.time.split('-')[0]

    // 如果当前课程的开始时间晚于上一个课程的结束时间，可以使用相同位置
    if (lastEndTime && startTime >= lastEndTime) {
      lastPosition = 0
    }

    // 如果时间重叠，使用不同位置
    const position = lastPosition
    lastPosition = (lastPosition + 1) % 2 // 交替使用0和1两个位置
    lastEndTime = scheduleClass.time.split('-')[1]

    return {
      ...scheduleClass,
      position
    }
  })
}

// 进入课堂
const enterClassroom = (scheduleClass: ScheduleClass) => {
  if (scheduleClass.status === 'upcoming') {
    ElMessage.success(`正在进入 ${scheduleClass.title} 课堂`)
  } else if (scheduleClass.status === 'completed') {
    ElMessage.info('该课程已结束')
  }
}

// 下载学习资料
const downloadMaterials = (classId: number) => {
  ElMessage.success('开始下载课程资料')
}

// 联系教师
const contactTeacher = (teacher: string) => {
  ElMessage.success(`正在连接 ${teacher} 的聊天窗口`)
}

// 提交作业
const submitHomework = (classId: number) => {
  ElMessage.success('作业提交成功')
}
</script>

<script lang="ts">
export default {
  name: 'StudentSchedule'
}
</script>

<template>
  <div class="student-schedule">
    <h2>上课安排</h2>

    <div class="view-toggle">
      <el-radio-group v-model="viewMode" size="large">
        <el-radio-button label="calendar">
          <el-icon><Calendar /></el-icon> 日历视图
        </el-radio-button>
        <el-radio-button label="list">
          <el-icon><List /></el-icon> 列表视图
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 日历视图 -->
    <div v-if="viewMode === 'calendar'" class="calendar-view">
      <el-calendar v-model="currentDate">
        <template #header="{ date }">
          <div class="calendar-header">
            <el-button-group>
              <el-button type="primary" size="small" @click="prevMonth">
                <el-icon><ArrowLeft /></el-icon>
              </el-button>
              <el-button type="primary" size="small" @click="today">今天</el-button>
              <el-button type="primary" size="small" @click="nextMonth">
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </el-button-group>
            <span class="calendar-title">{{ formatCalendarTitle(date) }}</span>
          </div>
        </template>
        <template #dateCell="{ data }">
          <div class="calendar-cell">
            <p :class="{ 'is-today': data.isToday, 'not-current-month': data.type !== 'current' }">
              {{ data.day.split('-').slice(2).join('') }}
            </p>
            <div v-if="isDateHasClass(data.date)" class="class-indicators">
              <div
                v-for="(scheduleClass, index) in getDateClassesWithPosition(data.date)"
                :key="index"
                class="class-indicator"
                :class="[
                  `subject-${scheduleClass.subject}`,
                  `status-${scheduleClass.status}`,
                  `position-${scheduleClass.position || 0}`
                ]"
                @click.stop="showClassDetail(scheduleClass)"
              >
                <div class="indicator-content">
                  <span class="indicator-time">{{ scheduleClass.time.split('-')[0] }}</span>
                  <span class="indicator-title">{{ scheduleClass.title }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </el-calendar>
    </div>

    <!-- 列表视图 -->
    <div v-else class="list-view">
      <div class="list-section">
        <h3 class="section-title">
          <el-icon><Timer /></el-icon> 即将进行的课程
        </h3>
        <div class="class-list">
          <el-empty v-if="upcomingClasses.length === 0" description="暂无安排的课程" />
          <el-card
            v-for="scheduleClass in upcomingClasses"
            :key="scheduleClass.id"
            class="class-card"
            shadow="hover"
          >
            <div class="class-card-content">
              <div class="class-time-info">
                <div class="class-date">{{ scheduleClass.date }}</div>
                <div class="class-time">{{ scheduleClass.time }}</div>
                <div class="class-badge" :class="`subject-${scheduleClass.subject}`">
                  {{ scheduleClass.subject }}
                </div>
              </div>
              <div class="class-main-info">
                <h4 class="class-title">{{ scheduleClass.title }}</h4>
                <div class="class-teacher">
                  <el-avatar :size="24" :src="scheduleClass.teacherAvatar"></el-avatar>
                  <span>{{ scheduleClass.teacher }}</span>
                </div>
                <div class="class-location">
                  <el-icon><Location /></el-icon>
                  <span>{{ scheduleClass.location }}</span>
                </div>
                <div class="class-actions">
                  <el-button size="small" type="primary" @click="enterClassroom(scheduleClass)">
                    <el-icon><VideoCamera /></el-icon> 进入课堂
                  </el-button>
                  <el-button size="small" type="info" @click="showClassDetail(scheduleClass)">
                    <el-icon><InfoFilled /></el-icon> 详情
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <div class="list-section">
        <h3 class="section-title">
          <el-icon><Checked /></el-icon> 已完成的课程
        </h3>
        <div class="class-list">
          <el-empty v-if="completedClasses.length === 0" description="暂无已完成的课程" />
          <el-card
            v-for="scheduleClass in completedClasses"
            :key="scheduleClass.id"
            class="class-card completed"
            shadow="hover"
          >
            <div class="class-card-content">
              <div class="class-time-info">
                <div class="class-date">{{ scheduleClass.date }}</div>
                <div class="class-time">{{ scheduleClass.time }}</div>
                <div class="class-badge" :class="`subject-${scheduleClass.subject}`">
                  {{ scheduleClass.subject }}
                </div>
              </div>
              <div class="class-main-info">
                <h4 class="class-title">{{ scheduleClass.title }}</h4>
                <div class="class-teacher">
                  <el-avatar :size="24" :src="scheduleClass.teacherAvatar"></el-avatar>
                  <span>{{ scheduleClass.teacher }}</span>
                </div>
                <div class="class-location">
                  <el-icon><Location /></el-icon>
                  <span>{{ scheduleClass.location }}</span>
                </div>
                <div class="class-actions">
                  <el-button size="small" type="success" @click="submitHomework(scheduleClass.id)" v-if="scheduleClass.homework">
                    <el-icon><Upload /></el-icon> 提交作业
                  </el-button>
                  <el-button size="small" type="primary" @click="downloadMaterials(scheduleClass.id)">
                    <el-icon><Download /></el-icon> 下载资料
                  </el-button>
                  <el-button size="small" type="info" @click="showClassDetail(scheduleClass)">
                    <el-icon><InfoFilled /></el-icon> 详情
                  </el-button>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 课程详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="selectedClass?.title || '课程详情'"
      width="500px"
      destroy-on-close
      align-center
    >
      <div v-if="selectedClass" class="class-detail">
        <div class="detail-header" :class="`subject-bg-${selectedClass.subject}`">
          <el-avatar :size="64" :src="selectedClass.teacherAvatar" class="teacher-avatar"></el-avatar>
          <div class="header-info">
            <h3>{{ selectedClass.title }}</h3>
            <div class="teacher-name">{{ selectedClass.teacher }}</div>
          </div>
          <el-tag :type="selectedClass.status === 'upcoming' ? 'success' : 'info'" effect="dark" class="status-tag">
            {{ selectedClass.status === 'upcoming' ? '即将开始' : '已结束' }}
          </el-tag>
        </div>

        <div class="detail-content">
          <div class="detail-item">
            <div class="item-label"><el-icon><Calendar /></el-icon> 上课时间</div>
            <div class="item-value">{{ selectedClass.date }} {{ selectedClass.time }}</div>
          </div>
          <div class="detail-item">
            <div class="item-label"><el-icon><Timer /></el-icon> 课时</div>
            <div class="item-value">{{ selectedClass.duration }}分钟</div>
          </div>
          <div class="detail-item">
            <div class="item-label"><el-icon><Location /></el-icon> 地点</div>
            <div class="item-value">{{ selectedClass.location }}</div>
          </div>
          <div class="detail-item">
            <div class="item-label"><el-icon><Document /></el-icon> 内容</div>
            <div class="item-value">{{ selectedClass.content }}</div>
          </div>
          <div v-if="selectedClass.homework" class="detail-item">
            <div class="item-label"><el-icon><Edit /></el-icon> 作业</div>
            <div class="item-value">{{ selectedClass.homework }}</div>
          </div>
        </div>

        <div class="detail-actions">
          <el-button
            v-if="selectedClass.status === 'upcoming'"
            type="primary"
            @click="enterClassroom(selectedClass)"
          >
            <el-icon><VideoCamera /></el-icon> 进入课堂
          </el-button>
          <el-button @click="contactTeacher(selectedClass.teacher)">
            <el-icon><ChatDotRound /></el-icon> 联系老师
          </el-button>
          <el-button @click="downloadMaterials(selectedClass.id)">
            <el-icon><Download /></el-icon> 下载资料
          </el-button>
          <el-button
            v-if="selectedClass.homework"
            type="success"
            @click="submitHomework(selectedClass.id)"
          >
            <el-icon><Upload /></el-icon> 提交作业
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-schedule {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  height: 100%;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
  font-weight: 600;
  border-left: 4px solid var(--el-color-primary);
  padding-left: 10px;
}

.view-toggle {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 日历视图样式 */
.calendar-view {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #ebeef5;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.calendar-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.calendar-cell {
  height: 100%;
  position: relative;
  padding: 8px;
  min-height: 120px;
}

.calendar-cell p {
  text-align: center;
  margin: 0;
  height: 24px;
  width: 24px;
  line-height: 24px;
  margin: 0 auto;
  font-weight: 500;
}

.calendar-cell .is-today {
  background-color: #409eff;
  color: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
}

.calendar-cell .not-current-month {
  color: #c0c4cc;
}

.class-indicators {
  margin-top: 4px;
}

.class-indicator {
  border-radius: 4px;
  margin-bottom: 4px;
  padding: 4px 6px;
  cursor: pointer;
  font-size: 12px;
  color: #fff;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  position: relative;
}

/* 根据位置调整样式，防止重叠 */
.class-indicator.position-0 {
  margin-right: 50%;
}

.class-indicator.position-1 {
  margin-left: 50%;
}

.class-indicator:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transform: translateY(-1px);
}

.indicator-content {
  display: flex;
  flex-direction: column;
}

.indicator-time {
  font-weight: 600;
  display: block;
  margin-bottom: 2px;
}

.indicator-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 不同科目的颜色 */
.subject-数学 {
  background-color: #409eff;
}

.subject-物理 {
  background-color: #67c23a;
}

.subject-化学 {
  background-color: #ff9900;
}

.subject-生物 {
  background-color: #f56c6c;
}

.subject-英语 {
  background-color: #9254de;
}

.subject-语文 {
  background-color: #e6a23c;
}

.subject-历史 {
  background-color: #8e44ad;
}

.subject-地理 {
  background-color: #16a085;
}

.subject-政治 {
  background-color: #95a5a6;
}

/* 不同状态的透明度 */
.status-completed {
  opacity: 0.7;
}

/* 课程详情对话框样式 */
.class-detail {
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: center;
  padding: 15px;
  margin: -20px -20px 20px -20px;
  position: relative;
  color: white;
  border-radius: 8px 8px 0 0;
  overflow: hidden;
}

.header-info {
  margin-left: 15px;
  flex: 1;
}

.teacher-avatar {
  border: 3px solid rgba(255, 255, 255, 0.5);
}

.teacher-name {
  font-size: 14px;
  opacity: 0.9;
  margin-top: 4px;
}

.status-tag {
  position: absolute;
  top: 15px;
  right: 15px;
}

.detail-content {
  padding: 0 10px;
}

.detail-item {
  margin-bottom: 15px;
  display: flex;
  align-items: flex-start;
}

.item-label {
  width: 100px;
  color: #606266;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.item-label .el-icon {
  margin-right: 6px;
}

.item-value {
  flex: 1;
  color: #303133;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
  justify-content: center;
}

/* 不同科目的详情背景色 */
.subject-bg-数学 {
  background: linear-gradient(135deg, #409eff, #1890ff);
}

.subject-bg-物理 {
  background: linear-gradient(135deg, #67c23a, #52af11);
}

.subject-bg-化学 {
  background: linear-gradient(135deg, #ff9900, #ff7700);
}

.subject-bg-生物 {
  background: linear-gradient(135deg, #f56c6c, #e24c4c);
}

.subject-bg-英语 {
  background: linear-gradient(135deg, #9254de, #722ed1);
}

.subject-bg-语文 {
  background: linear-gradient(135deg, #e6a23c, #d48806);
}

.subject-bg-历史 {
  background: linear-gradient(135deg, #8e44ad, #6d279e);
}

.subject-bg-地理 {
  background: linear-gradient(135deg, #16a085, #006755);
}

.subject-bg-政治 {
  background: linear-gradient(135deg, #95a5a6, #7f8c8d);
}

/* 列表视图样式增强 */
.list-view {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #ebeef5;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 15px;
  color: var(--el-color-primary);
}

.section-title .el-icon {
  margin-right: 8px;
}

.class-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
}

.list-section {
  margin-bottom: 30px;
}

.class-card {
  border-radius: 8px;
  transition: all 0.3s;
  overflow: hidden;
  margin-bottom: 15px;
}

.class-card-content {
  display: flex;
}

.class-time-info {
  padding: 15px;
  margin-right: 15px;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 100px;
}

.class-date {
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.class-time {
  color: #666;
  margin-bottom: 10px;
  font-size: 14px;
}

.class-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
}

.class-main-info {
  flex: 1;
  padding: 15px 15px 15px 0;
}

.class-title {
  margin: 0 0 10px;
  font-size: 18px;
  color: #333;
}

.class-teacher {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.class-teacher span {
  margin-left: 8px;
  color: #666;
}

.class-location {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  color: #666;
}

.class-location .el-icon {
  margin-right: 5px;
  color: var(--el-color-primary);
}

.class-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 768px) {
  .class-card-content {
    flex-direction: column;
  }

  .class-time-info {
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
    padding-right: 0;
    padding-bottom: 10px;
    margin-right: 0;
    margin-bottom: 10px;
    flex-direction: row;
    gap: 15px;
    justify-content: flex-start;
  }

  .class-date, .class-time {
    margin-bottom: 0;
  }

  .class-actions {
    flex-wrap: wrap;
  }
}
</style>
