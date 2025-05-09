<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import studentBoy1 from '@/assets/pictures/studentBoy1.jpeg'
import studentBoy2 from '@/assets/pictures/studentBoy2.jpeg'
import studentGirl1 from '@/assets/pictures/studentGirl1.jpeg'
import studentGirl2 from '@/assets/pictures/studentGirl2.jpeg'

interface ScheduleClass {
  id: number;
  title: string;
  date: string;
  time: string;
  duration: number; // 分钟
  studentName: string;
  studentAvatar?: string;
  subject: string;
  location: string;
  content: string;
  status: 'upcoming' | 'ongoing' | 'completed' | 'cancelled';
  notes?: string;
}

// 模拟课程数据
const classes = ref<ScheduleClass[]>([
  {
    id: 1,
    title: '高中数学 - 函数与导数',
    date: '2023-07-15',
    time: '18:00-20:00',
    duration: 120,
    studentName: '张同学',
    studentAvatar: studentBoy1,
    subject: '数学',
    location: '线上课堂',
    content: '函数与导数的应用题解题技巧',
    status: 'upcoming',
    notes: '准备导数应用的例题'
  },
  {
    id: 2,
    title: '高中物理 - 力学与电学',
    date: '2023-07-12',
    time: '16:00-18:00',
    duration: 120,
    studentName: '李同学',
    studentAvatar: studentBoy2,
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
    studentName: '张同学',
    studentAvatar: studentGirl1,
    subject: '数学',
    location: '线上课堂',
    content: '导数的几何意义与物理意义',
    status: 'completed',
    notes: '下次课需要增加习题难度'
  },
  {
    id: 4,
    title: '高中物理 - 力学与电学',
    date: '2023-07-05',
    time: '16:00-18:00',
    duration: 120,
    studentName: '李同学',
    studentAvatar: studentBoy1,
    subject: '物理',
    location: '线上课堂',
    content: '电场与磁场的关系',
    status: 'completed',
    notes: '学生对矢量场概念理解有难度，下次详细讲解'
  },
  {
    id: 5,
    title: '高中生物 - 基因与遗传',
    date: '2023-07-21',
    time: '18:00-20:00',
    duration: 120,
    studentName: '王同学',
    studentAvatar: studentGirl2,
    subject: '生物',
    location: '线上课堂',
    content: '基因的概念与DNA结构',
    status: 'upcoming'
  },
  {
    id: 6,
    title: '高中英语 - 阅读理解技巧',
    date: new Date().toISOString().split('T')[0],
    time: '14:00-16:00',
    duration: 120,
    studentName: '赵同学',
    studentAvatar: studentGirl1,
    subject: '英语',
    location: '线上课堂',
    content: '高考英语阅读理解答题技巧与实战演练',
    status: 'upcoming',
    notes: '准备4篇阅读理解范文'
  },
  {
    id: 7,
    title: '高中化学 - 有机化学',
    date: new Date().toISOString().split('T')[0],
    time: '16:30-18:30',
    duration: 120,
    studentName: '钱同学',
    studentAvatar: studentBoy1,
    subject: '化学',
    location: '线上课堂',
    content: '有机化合物的结构与性质',
    status: 'upcoming',
    notes: '准备实验演示视频和分子模型'
  },
  {
    id: 8,
    title: '初中数学 - 几何证明',
    date: new Date().toISOString().split('T')[0],
    time: '10:00-12:00',
    duration: 120,
    studentName: '孙同学',
    studentAvatar: studentGirl1,
    subject: '数学',
    location: '线上课堂',
    content: '平行四边形的性质与证明',
    status: 'completed',
    notes: '学生理解良好，下次可以进行难度提升'
  }
])

// 当前视图模式
const viewMode = ref('calendar') // calendar, list, week, 或 timetable

// 当前日期
const currentDate = ref(new Date())

// 课程详情和编辑
const selectedClass = ref<ScheduleClass | null>(null)
const detailDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editingClass = reactive({
  id: 0,
  title: '',
  date: '',
  time: '',
  duration: 120,
  studentName: '',
  subject: '',
  location: '',
  content: '',
  status: 'upcoming' as const,
  notes: ''
})

// 显示课程详情
const showClassDetail = (scheduleClass: ScheduleClass) => {
  selectedClass.value = scheduleClass
  detailDialogVisible.value = true
}

// 编辑课程
const editClass = (scheduleClass: ScheduleClass) => {
  Object.assign(editingClass, scheduleClass)
  editDialogVisible.value = true
}

// 保存课程修改
const saveClassEdit = () => {
  const index = classes.value.findIndex(c => c.id === editingClass.id)
  if (index !== -1) {
    classes.value[index] = { ...editingClass }
    ElMessage.success('课程信息已更新')
    editDialogVisible.value = false
  }
}

// 取消课程
const cancelClass = (id: number) => {
  const index = classes.value.findIndex(c => c.id === id)
  if (index !== -1) {
    classes.value[index].status = 'cancelled'
    ElMessage.success('课程已取消')

    if (selectedClass.value && selectedClass.value.id === id) {
      selectedClass.value.status = 'cancelled'
    }
  }
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

// 日历相关函数
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

// 课程操作函数
// 进入课堂
const enterClassroom = (scheduleClass: ScheduleClass) => {
  if (scheduleClass.status === 'upcoming') {
    ElMessage.success(`正在进入 ${scheduleClass.title} 课堂`)
  } else if (scheduleClass.status === 'completed') {
    ElMessage.info('该课程已结束')
  }
}

// 联系学生
const contactStudent = (student: string) => {
  ElMessage.success(`正在连接 ${student} 的聊天窗口`)
}

// 添加新课程
const newClassDialogVisible = ref(false)
const newClass = reactive({
  title: '',
  date: '',
  time: '',
  duration: 120,
  studentName: '',
  subject: '',
  location: '线上课堂',
  content: '',
  status: 'upcoming' as const,
  notes: ''
})

// 打开添加课程对话框
const openNewClassDialog = () => {
  // 重置表单
  Object.assign(newClass, {
    title: '',
    date: '',
    time: '',
    duration: 120,
    studentName: '',
    subject: '',
    location: '线上课堂',
    content: '',
    status: 'upcoming',
    notes: ''
  })
  newClassDialogVisible.value = true
}

// 添加课程
const addNewClass = () => {
  // 简单验证
  if (!newClass.title || !newClass.date || !newClass.time || !newClass.studentName) {
    ElMessage.warning('请填写所有必填项')
    return
  }

  // 创建新课程
  const newId = Math.max(...classes.value.map(c => c.id), 0) + 1
  classes.value.push({
    id: newId,
    ...newClass,
    studentAvatar: 'https://img2.baidu.com/it/u=1003272215,1878984629&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500'  // 默认头像
  })

  ElMessage.success('新课程已添加')
  newClassDialogVisible.value = false
}

// 课程表相关函数
// 时间段
const timeSlots = [
  { id: 1, label: '08:00 - 10:00', start: '08:00', end: '10:00' },
  { id: 2, label: '10:00 - 12:00', start: '10:00', end: '12:00' },
  { id: 3, label: '14:00 - 16:00', start: '14:00', end: '16:00' },
  { id: 4, label: '16:00 - 18:00', start: '16:00', end: '18:00' },
  { id: 5, label: '18:00 - 20:00', start: '18:00', end: '20:00' },
  { id: 6, label: '20:00 - 22:00', start: '20:00', end: '22:00' }
]

// 获取周一
const getStartOfWeek = (date: Date) => {
  const d = new Date(date)
  const day = d.getDay() || 7 // 将周日视为7
  if (day !== 1) d.setHours(-24 * (day - 1))
  d.setHours(0, 0, 0, 0)
  return d
}

// 获取周日
const getEndOfWeek = (date: Date) => {
  const d = new Date(getStartOfWeek(date))
  d.setDate(d.getDate() + 6)
  return d
}

// 获取一周的日期
const weekDays = computed(() => {
  const startDay = getStartOfWeek(currentDate.value)
  const days = []
  for (let i = 0; i < 7; i++) {
    const day = new Date(startDay)
    day.setDate(day.getDate() + i)
    days.push(day)
  }
  return days
})

// 格式化日期范围
const formatWeekRange = (start: Date, end: Date) => {
  return `${start.getFullYear()}年${start.getMonth() + 1}月${start.getDate()}日 - ${end.getMonth() + 1}月${end.getDate()}日`
}

// 格式化日期
const formatDate = (date: Date) => {
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 格式化星期
const formatDay = (date: Date) => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[date.getDay()]
}

// 判断是否是今天
const isToday = (date: Date) => {
  const today = new Date()
  return date.getDate() === today.getDate() &&
         date.getMonth() === today.getMonth() &&
         date.getFullYear() === today.getFullYear()
}

// 上一周
const prevWeek = () => {
  const newDate = new Date(currentDate.value)
  newDate.setDate(newDate.getDate() - 7)
  currentDate.value = newDate
}

// 下一周
const nextWeek = () => {
  const newDate = new Date(currentDate.value)
  newDate.setDate(newDate.getDate() + 7)
  currentDate.value = newDate
}

// 判断某个时间段是否有课
const hasClass = (day: Date, timeSlot: typeof timeSlots[0]) => {
  const dateStr = day.toISOString().split('T')[0]
  return classes.value.some(c => {
    if (c.date !== dateStr) return false
    const classStartTime = c.time.split('-')[0]
    const classEndTime = c.time.split('-')[1]
    return classStartTime === timeSlot.start ||
           (classStartTime < timeSlot.start && classEndTime > timeSlot.start)
  })
}

// 获取某个时间段的课程
const getClassesByTimeSlot = (day: Date, timeSlot: typeof timeSlots[0]) => {
  const dateStr = day.toISOString().split('T')[0]
  return classes.value.filter(c => {
    if (c.date !== dateStr) return false
    const classStartTime = c.time.split('-')[0]
    const classEndTime = c.time.split('-')[1]
    return classStartTime === timeSlot.start ||
           (classStartTime < timeSlot.start && classEndTime > timeSlot.start)
  })
}
</script>

<template>
  <div class="teacher-schedule">
    <div class="schedule-header">
      <h2>课表管理</h2>
      <div class="schedule-actions">
        <el-button type="primary" @click="openNewClassDialog">
          <el-icon><Plus /></el-icon> 添加课程
        </el-button>
      </div>
    </div>

    <div class="schedule-container">
      <!-- 侧边栏 -->
      <div class="schedule-sidebar">
        <div class="view-toggle">
          <el-radio-group v-model="viewMode" size="large">
            <el-radio-button label="calendar">
              <el-icon><Calendar /></el-icon> 日历视图
            </el-radio-button>
            <el-radio-button label="list">
              <el-icon><List /></el-icon> 列表视图
            </el-radio-button>
            <el-radio-button label="timetable">
              <el-icon><Grid /></el-icon> 课程表视图
            </el-radio-button>
          </el-radio-group>
        </div>

        <div class="quick-stats">
          <div class="stat-item">
            <div class="stat-value">{{ upcomingClasses.length }}</div>
            <div class="stat-label">即将上课</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ completedClasses.length }}</div>
            <div class="stat-label">已完成</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ classes.filter(c => c.status === 'cancelled').length }}</div>
            <div class="stat-label">已取消</div>
          </div>
        </div>

        <div class="upcoming-preview">
          <h3>今日课程</h3>
          <div v-if="getDateClasses(new Date()).length === 0" class="no-classes">
            <el-empty description="今日无课程" :image-size="60" />
          </div>
          <div v-else class="today-classes">
            <div
              v-for="(scheduleClass, index) in getDateClasses(new Date())"
              :key="index"
              class="preview-class-item"
              :class="[`subject-${scheduleClass.subject}`, `status-${scheduleClass.status}`]"
              @click="showClassDetail(scheduleClass)"
            >
              <div class="preview-time">{{ scheduleClass.time }}</div>
              <div class="preview-title">{{ scheduleClass.title }}</div>
              <div class="preview-student">
                <el-avatar :size="20" :src="scheduleClass.studentAvatar"></el-avatar>
                <span>{{ scheduleClass.studentName }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="schedule-main">
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
        <div v-else-if="viewMode === 'list'" class="list-view">
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
                    <div class="class-student">
                      <el-avatar :size="24" :src="scheduleClass.studentAvatar"></el-avatar>
                      <span>{{ scheduleClass.studentName }}</span>
                    </div>
                    <div class="class-location">
                      <el-icon><Location /></el-icon>
                      <span>{{ scheduleClass.location }}</span>
                    </div>
                    <div class="class-actions">
                      <el-button size="small" type="primary" @click="enterClassroom(scheduleClass)">
                        <el-icon><VideoCamera /></el-icon> 进入课堂
                      </el-button>
                      <el-button size="small" type="success" @click="contactStudent(scheduleClass.studentName)">
                        <el-icon><ChatDotRound /></el-icon> 联系学生
                      </el-button>
                      <el-button size="small" type="warning" @click="editClass(scheduleClass)">
                        <el-icon><Edit /></el-icon> 编辑
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

        <!-- 课程表视图 -->
        <div v-else-if="viewMode === 'timetable'" class="timetable-view">
          <div class="timetable-header">
            <div class="timetable-nav">
              <el-button-group>
                <el-button size="small" @click="prevWeek">
                  <el-icon><ArrowLeft /></el-icon>
                </el-button>
                <el-button size="small" @click="currentDate = new Date()">本周</el-button>
                <el-button size="small" @click="nextWeek">
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </el-button-group>
              <div class="current-week">
                {{ formatWeekRange(getStartOfWeek(currentDate), getEndOfWeek(currentDate)) }}
              </div>
            </div>
          </div>

          <div class="timetable-container">
            <div class="timetable-sidebar">
              <div class="time-slot-header">时间段</div>
              <div
                v-for="timeSlot in timeSlots"
                :key="timeSlot.id"
                class="time-slot"
              >
                {{ timeSlot.label }}
              </div>
            </div>

            <div class="timetable-content">
              <div class="timetable-days">
                <div
                  v-for="(day, index) in weekDays"
                  :key="index"
                  class="day-header"
                  :class="{ 'today': isToday(day) }"
                >
                  <div class="day-name">{{ formatDay(day) }}</div>
                  <div class="day-date">{{ formatDate(day) }}</div>
                </div>
              </div>

              <div class="timetable-grid">
                <div
                  v-for="(day, dayIndex) in weekDays"
                  :key="dayIndex"
                  class="timetable-day"
                >
                  <div
                    v-for="timeSlot in timeSlots"
                    :key="`${dayIndex}-${timeSlot.id}`"
                    class="timetable-cell"
                    :class="{ 'has-class': hasClass(day, timeSlot) }"
                  >
                    <div
                      v-for="scheduleClass in getClassesByTimeSlot(day, timeSlot)"
                      :key="scheduleClass.id"
                      class="timetable-class"
                      :class="[`subject-${scheduleClass.subject}`, `status-${scheduleClass.status}`]"
                      @click="showClassDetail(scheduleClass)"
                    >
                      <div class="class-title">{{ scheduleClass.title }}</div>
                      <div class="class-time">{{ scheduleClass.time }}</div>
                      <div class="class-student">{{ scheduleClass.studentName }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
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
            <el-tag :type="selectedClass.status === 'upcoming' ? 'warning' : selectedClass.status === 'ongoing' ? 'success' : selectedClass.status === 'cancelled' ? 'danger' : 'info'">
              {{ selectedClass.status === 'upcoming' ? '即将开始' : selectedClass.status === 'ongoing' ? '进行中' : selectedClass.status === 'cancelled' ? '已取消' : '已结束' }}
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

        <div class="detail-student">
          <div class="student-avatar">
            <el-avatar :size="60" :src="selectedClass.studentAvatar"></el-avatar>
          </div>
          <div class="student-info">
            <h4>{{ selectedClass.studentName }}</h4>
            <p>{{ selectedClass.subject }} 课程学员</p>
          </div>
        </div>

        <div class="detail-content">
          <h4>课程内容</h4>
          <p>{{ selectedClass.content }}</p>
        </div>

        <div class="detail-notes" v-if="selectedClass.notes">
          <h4>教学笔记</h4>
          <p>{{ selectedClass.notes }}</p>
        </div>

        <div class="detail-actions">
          <el-button type="primary" @click="enterClassroom(selectedClass)" v-if="selectedClass.status === 'upcoming'">
            <el-icon><VideoCamera /></el-icon> 进入课堂
          </el-button>
          <el-button type="success" @click="contactStudent(selectedClass.studentName)">
            <el-icon><ChatDotRound /></el-icon> 联系学生
          </el-button>
          <el-button type="warning" @click="editClass(selectedClass)" v-if="selectedClass.status !== 'cancelled'">
            <el-icon><Edit /></el-icon> 编辑课程
          </el-button>
          <el-button type="danger" @click="cancelClass(selectedClass.id)" v-if="selectedClass.status === 'upcoming'">
            <el-icon><CircleClose /></el-icon> 取消课程
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 编辑课程对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑课程"
      width="50%"
    >
      <el-form :model="editingClass" label-position="top">
        <el-form-item label="课程标题" required>
          <el-input v-model="editingClass.title" placeholder="请输入课程标题"></el-input>
        </el-form-item>
        <el-form-item label="日期" required>
          <el-date-picker
            v-model="editingClass.date"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="时间" required>
              <el-input v-model="editingClass.time" placeholder="例如: 18:00-20:00"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长(分钟)" required>
              <el-input-number v-model="editingClass.duration" :min="30" :step="30" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学生姓名" required>
              <el-input v-model="editingClass.studentName" placeholder="请输入学生姓名"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科目" required>
              <el-select v-model="editingClass.subject" placeholder="请选择科目" style="width: 100%">
                <el-option label="数学" value="数学"></el-option>
                <el-option label="物理" value="物理"></el-option>
                <el-option label="化学" value="化学"></el-option>
                <el-option label="生物" value="生物"></el-option>
                <el-option label="英语" value="英语"></el-option>
                <el-option label="语文" value="语文"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="上课地点">
          <el-input v-model="editingClass.location" placeholder="请输入上课地点"></el-input>
        </el-form-item>
        <el-form-item label="课程内容" required>
          <el-input
            v-model="editingClass.content"
            type="textarea"
            :rows="3"
            placeholder="请输入课程内容"
          ></el-input>
        </el-form-item>
        <el-form-item label="教学笔记">
          <el-input
            v-model="editingClass.notes"
            type="textarea"
            :rows="3"
            placeholder="请输入教学笔记"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveClassEdit">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 添加新课程对话框 -->
    <el-dialog
      v-model="newClassDialogVisible"
      title="添加新课程"
      width="50%"
    >
      <el-form :model="newClass" label-position="top">
        <el-form-item label="课程标题" required>
          <el-input v-model="newClass.title" placeholder="请输入课程标题"></el-input>
        </el-form-item>
        <el-form-item label="日期" required>
          <el-date-picker
            v-model="newClass.date"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          ></el-date-picker>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="时间" required>
              <el-input v-model="newClass.time" placeholder="例如: 18:00-20:00"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长(分钟)" required>
              <el-input-number v-model="newClass.duration" :min="30" :step="30" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学生姓名" required>
              <el-input v-model="newClass.studentName" placeholder="请输入学生姓名"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科目" required>
              <el-select v-model="newClass.subject" placeholder="请选择科目" style="width: 100%">
                <el-option label="数学" value="数学"></el-option>
                <el-option label="物理" value="物理"></el-option>
                <el-option label="化学" value="化学"></el-option>
                <el-option label="生物" value="生物"></el-option>
                <el-option label="英语" value="英语"></el-option>
                <el-option label="语文" value="语文"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="上课地点">
          <el-input v-model="newClass.location" placeholder="请输入上课地点"></el-input>
        </el-form-item>
        <el-form-item label="课程内容" required>
          <el-input
            v-model="newClass.content"
            type="textarea"
            :rows="3"
            placeholder="请输入课程内容"
          ></el-input>
        </el-form-item>
        <el-form-item label="教学笔记">
          <el-input
            v-model="newClass.notes"
            type="textarea"
            :rows="3"
            placeholder="请输入教学笔记"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="newClassDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addNewClass">添加</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
export default {
  name: 'TeacherSchedule'
}
</script>

<style scoped>
.teacher-schedule {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.schedule-container {
  display: flex;
  gap: 20px;
  height: calc(100vh - 200px);
  min-height: 600px;
}

.schedule-sidebar {
  width: 280px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex-shrink: 0;
}

.view-toggle {
  margin-bottom: 10px;
}

.view-toggle :deep(.el-radio-group) {
  width: 100%;
  display: flex;
}

.view-toggle :deep(.el-radio-button) {
  flex: 1;
}

.view-toggle :deep(.el-radio-button__inner) {
  width: 100%;
}

.quick-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  background-color: #f9f9f9;
  padding: 10px;
  border-radius: 8px;
  flex: 1;
  margin: 0 5px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

.upcoming-preview {
  flex: 1;
  overflow-y: auto;
}

.upcoming-preview h3 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 16px;
  color: #333;
}

.today-classes {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-class-item {
  background-color: #f5f7fa;
  border-radius: 6px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid #ccc;
}

.preview-class-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.preview-time {
  font-size: 12px;
  color: #666;
  margin-bottom: 5px;
}

.preview-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-student {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #666;
}

.schedule-main {
  flex: 1;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 20px;
  overflow-y: auto;
}

/* 日历视图样式 */
.calendar-view {
  height: 100%;
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

/* 列表视图样式 */
.list-view {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.list-section {
  padding: 10px 0;
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
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
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

.class-student {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.class-student span {
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
  flex-wrap: wrap;
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

.detail-student {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
}

.student-avatar {
  margin-right: 15px;
}

.student-info h4 {
  margin: 0 0 5px;
  font-size: 18px;
  color: #333;
}

.student-info p {
  margin: 0;
  color: #666;
}

.detail-content, .detail-notes {
  margin-bottom: 20px;
}

.detail-content h4, .detail-notes h4 {
  margin: 0 0 10px;
  font-size: 16px;
  color: #333;
}

.detail-content p, .detail-notes p {
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.detail-notes {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 30px;
}

/* 科目颜色 */
.subject-数学 {
  background-color: #ecf5ff;
  color: #409eff;
  border-left-color: #409eff;
}

.subject-物理 {
  background-color: #f0f9eb;
  color: #67c23a;
  border-left-color: #67c23a;
}

.subject-生物 {
  background-color: #fdf6ec;
  color: #e6a23c;
  border-left-color: #e6a23c;
}

.subject-英语 {
  background-color: #f5f7fa;
  color: #909399;
  border-left-color: #909399;
}

.subject-化学 {
  background-color: #fef0f0;
  color: #f56c6c;
  border-left-color: #f56c6c;
}

.subject-语文 {
  background-color: #f0f2ff;
  color: #6366f1;
  border-left-color: #6366f1;
}

/* 课程状态 */
.status-completed {
  opacity: 0.7;
}

.status-cancelled {
  opacity: 0.5;
  text-decoration: line-through;
}

/* 课程表视图样式 */
.timetable-view {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.timetable-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.timetable-nav {
  display: flex;
  align-items: center;
  gap: 15px;
}

.current-week {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.timetable-container {
  display: flex;
  flex: 1;
  min-height: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.timetable-sidebar {
  width: 120px;
  min-width: 120px;
  border-right: 1px solid #ebeef5;
  background-color: #f5f7fa;
}

.time-slot-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  border-bottom: 1px solid #ebeef5;
  background-color: #f0f2f5;
}

.time-slot {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  text-align: center;
  font-size: 13px;
  border-bottom: 1px solid #ebeef5;
}

.timetable-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.timetable-days {
  display: flex;
  height: 60px;
  border-bottom: 1px solid #ebeef5;
}

.day-header {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #ebeef5;
  background-color: #f0f2f5;
}

.day-header:last-child {
  border-right: none;
}

.day-header.today {
  background-color: #ecf5ff;
  color: #409eff;
}

.day-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.day-date {
  font-size: 12px;
  color: #606266;
}

.timetable-grid {
  display: flex;
  flex: 1;
}

.timetable-day {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
}

.timetable-day:last-child {
  border-right: none;
}

.timetable-cell {
  height: 80px;
  border-bottom: 1px solid #ebeef5;
  padding: 5px;
  position: relative;
}

.timetable-cell.has-class {
  background-color: rgba(64, 158, 255, 0.05);
}

.timetable-class {
  position: absolute;
  top: 5px;
  left: 5px;
  right: 5px;
  bottom: 5px;
  padding: 8px;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.timetable-class:hover {
  transform: scale(1.02);
  z-index: 10;
}

.class-title {
  font-weight: bold;
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
}

.class-time {
  font-size: 11px;
  margin-bottom: 5px;
}

.class-student {
  font-size: 11px;
  color: #606266;
}

@media (max-width: 992px) {
  .schedule-container {
    flex-direction: column;
    height: auto;
  }

  .schedule-sidebar {
    width: 100%;
    margin-bottom: 20px;
  }

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

  .timetable-container {
    flex-direction: column;
  }

  .timetable-sidebar {
    width: 100%;
    min-width: 0;
    display: flex;
    border-right: none;
    border-bottom: 1px solid #ebeef5;
  }

  .time-slot-header {
    width: 120px;
    height: 50px;
    border-bottom: none;
    border-right: 1px solid #ebeef5;
  }

  .time-slot {
    flex: 1;
    height: 50px;
    border-bottom: none;
    border-right: 1px solid #ebeef5;
  }

  .time-slot:last-child {
    border-right: none;
  }

  .timetable-cell {
    height: 60px;
  }

  .class-title {
    font-size: 11px;
    margin-bottom: 2px;
  }

  .class-time, .class-student {
    font-size: 10px;
    margin-bottom: 2px;
  }
}
</style>
