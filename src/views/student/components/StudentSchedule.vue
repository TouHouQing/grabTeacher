<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { CalendarDateType } from 'element-plus'

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

// 获取某天的课程数量
const getDateClassCount = (date: Date) => {
  const dateStr = date.toISOString().split('T')[0]
  return classes.value.filter(c => c.date === dateStr).length
}

// 获取某天的所有课程
const getDateClasses = (date: Date) => {
  const dateStr = date.toISOString().split('T')[0]
  return classes.value.filter(c => c.date === dateStr)
      .sort((a, b) => a.time.split('-')[0].localeCompare(b.time.split('-')[0]))
}

// 日历单元格的自定义渲染
const cellRender = (cell: CalendarDateType) => {
  const date = new Date(cell.date.toISOString())
  const hasClass = isDateHasClass(date)
  const count = getDateClassCount(date)

  return hasClass ? {
    type: 'success',
    badge: count > 0
  } : null
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
        <template #dateCell="{ data }">
          <div class="calendar-cell">
            <p :class="{ 'is-today': data.isToday, 'not-current-month': data.type !== 'current' }">
              {{ data.day.split('-').slice(2).join('') }}
            </p>
            <div v-if="isDateHasClass(data.date)" class="class-indicators">
              <div
                v-for="(scheduleClass, index) in getDateClasses(data.date)"
                :key="index"
                class="class-indicator"
                :class="[`subject-${scheduleClass.subject}`, `status-${scheduleClass.status}`]"
                :style="{ top: `${index * 26 + 30}px` }"
                @click.stop="showClassDetail(scheduleClass)"
              >
                <span class="indicator-time">{{ scheduleClass.time.split('-')[0] }}</span>
                <span class="indicator-title">{{ scheduleClass.title }}</span>
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
      title="课程详情"
      width="50%"
    >
      <div v-if="selectedClass" class="class-detail">
        <div class="detail-header">
          <div class="detail-status">
            <el-tag :type="selectedClass.status === 'upcoming' ? 'warning' : selectedClass.status === 'ongoing' ? 'success' : 'info'">
              {{ selectedClass.status === 'upcoming' ? '即将开始' : selectedClass.status === 'ongoing' ? '进行中' : '已结束' }}
            </el-tag>
          </div>
          <h3 class="detail-title">{{ selectedClass.title }}</h3>
          <div class="detail-meta">
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>{{ selectedClass.date }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Timer /></el-icon>
              <span>{{ selectedClass.time }} ({{ selectedClass.duration }}分钟)</span>
            </div>
            <div class="meta-item">
              <el-icon><Location /></el-icon>
              <span>{{ selectedClass.location }}</span>
            </div>
          </div>
        </div>

        <div class="detail-teacher">
          <div class="teacher-avatar">
            <el-avatar :size="60" :src="selectedClass.teacherAvatar"></el-avatar>
          </div>
          <div class="teacher-info">
            <h4>{{ selectedClass.teacher }}</h4>
            <p>{{ selectedClass.subject }} 教师</p>
          </div>
        </div>

        <div class="detail-content">
          <h4>课程内容</h4>
          <p>{{ selectedClass.content }}</p>
        </div>

        <div class="detail-homework" v-if="selectedClass.homework">
          <h4>课后作业</h4>
          <p>{{ selectedClass.homework }}</p>
        </div>

        <div class="detail-actions">
          <el-button type="primary" @click="enterClassroom(selectedClass)" v-if="selectedClass.status !== 'completed'">
            <el-icon><VideoCamera /></el-icon> 进入课堂
          </el-button>
          <el-button type="success" @click="contactTeacher(selectedClass.teacher)">
            <el-icon><ChatDotRound /></el-icon> 联系教师
          </el-button>
          <el-button type="warning" @click="submitHomework(selectedClass.id)" v-if="selectedClass.homework && selectedClass.status === 'completed'">
            <el-icon><Upload /></el-icon> 提交作业
          </el-button>
          <el-button type="info" @click="downloadMaterials(selectedClass.id)" v-if="selectedClass.status === 'completed'">
            <el-icon><Download /></el-icon> 下载资料
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-schedule {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
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
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 15px;
  margin-bottom: 30px;
}

.calendar-view :deep(.el-calendar-table) {
  table-layout: fixed;
}

.calendar-view :deep(.el-calendar-day) {
  height: 150px;
  padding: 0;
  position: relative;
}

.calendar-cell {
  height: 100%;
  position: relative;
  padding: 8px;
  overflow: visible;
}

.calendar-cell p {
  text-align: center;
  margin: 0;
  height: 24px;
  width: 24px;
  line-height: 24px;
  margin: 0 auto;
  position: relative;
  z-index: 2;
}

.calendar-cell .is-today {
  background-color: #409eff;
  color: #fff;
  border-radius: 50%;
}

.calendar-cell .not-current-month {
  color: #c0c4cc;
}

.class-indicators {
  position: absolute;
  width: 100%;
  left: 0;
  top: 0;
  height: 100%;
  padding: 0 5px;
}

.class-indicator {
  position: absolute;
  left: 5px;
  right: 5px;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
  transition: all 0.2s;
  z-index: 1;
  height: 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.class-indicator:hover {
  transform: translateY(-2px) scale(1.02);
  z-index: 10;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.indicator-time {
  margin-right: 4px;
  font-weight: bold;
  font-size: 11px;
}

.indicator-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 11px;
}

/* 科目颜色 */
.subject-数学 {
  background-color: #ecf5ff;
  color: #409eff;
  border-left: 3px solid #409eff;
}

.subject-物理 {
  background-color: #f0f9eb;
  color: #67c23a;
  border-left: 3px solid #67c23a;
}

.subject-生物 {
  background-color: #fdf6ec;
  color: #e6a23c;
  border-left: 3px solid #e6a23c;
}

.subject-英语 {
  background-color: #f5f7fa;
  color: #909399;
  border-left: 3px solid #909399;
}

/* 课程状态 */
.status-completed {
  opacity: 0.7;
}

/* 列表视图样式 */
.list-view {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.list-section {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  padding: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #333;
}

.section-title .el-icon {
  margin-right: 8px;
  color: #409eff;
}

.class-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.class-card {
  border-radius: 8px;
  transition: transform 0.3s;
}

.class-card:hover {
  transform: translateY(-5px);
}

.class-card.completed {
  opacity: 0.8;
}

.class-card-content {
  display: flex;
}

.class-time-info {
  padding-right: 15px;
  margin-right: 15px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
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
}

.class-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.class-main-info {
  flex: 1;
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
  color: #409eff;
}

.class-actions {
  display: flex;
  gap: 10px;
}

/* 详情弹窗样式 */
.class-detail {
  padding: 10px;
}

.detail-header {
  margin-bottom: 20px;
}

.detail-status {
  margin-bottom: 10px;
}

.detail-title {
  font-size: 22px;
  margin: 0 0 15px;
  color: #333;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
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

.detail-teacher {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
}

.teacher-avatar {
  margin-right: 15px;
}

.teacher-info h4 {
  margin: 0 0 5px;
  font-size: 18px;
  color: #333;
}

.teacher-info p {
  margin: 0;
  color: #666;
}

.detail-content, .detail-homework {
  margin-bottom: 20px;
}

.detail-content h4, .detail-homework h4 {
  margin: 0 0 10px;
  font-size: 16px;
  color: #333;
}

.detail-content p, .detail-homework p {
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.detail-homework {
  background-color: #fdf6ec;
  padding: 15px;
  border-radius: 8px;
  border-left: 4px solid #e6a23c;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .class-card-content {
    flex-direction: column;
  }

  .class-time-info {
    border-right: none;
    border-bottom: 1px solid #eee;
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

  .detail-meta {
    flex-direction: column;
    gap: 10px;
  }
}
</style>
