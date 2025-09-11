<script setup lang="ts">
import { ref, watch, onMounted, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { bookingAPI, teacherAPI, rescheduleAPI, suspensionAPI } from '../../utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'


import {
  HomeFilled,
  DocumentChecked,
  Calendar,
  Reading,
  Money,
  Setting,
  ChatDotRound,
  List,
  Refresh,
  Lock
} from '@element-plus/icons-vue'

// 导入消息组件
import TeacherMessages from './components/TeacherMessages.vue'
const RescheduleModal = defineAsyncComponent(() => import('../../components/RescheduleModal.vue'))
const TeacherHourDetails = defineAsyncComponent(() => import('./components/TeacherHourDetails.vue'))

// 课程安排接口定义
interface ScheduleItem {
  id: number
  scheduledDate: string
  startTime: string
  endTime: string
  courseName: string
  studentName: string
  status: string
  isTrial: boolean
  enrollmentId?: number
  courseType?: string
  teacherId?: number
  teacherName?: string
  studentId?: number
  courseId?: number
  durationMinutes?: number
  courseTitle?: string
  subjectName?: string
  grade?: string
}



// 课程标题：学生姓名+科目+年级+一对一课程
const buildOneToOneTitle = (s: ScheduleItem): string | null => {
  const student = (s.studentName || '').trim()
  const subject = (s.subjectName || '').trim()
  const grade = (s.grade || '').trim()

  if (student && subject) {
    // 组合科目和年级
    const subjectWithGrade = grade ? `${subject}${grade}` : subject
    return `${student}${subjectWithGrade}一对一课程`
  }
  return null
}

const userStore = useUserStore()
const route = useRoute()
const activeMenu = ref('dashboard')
const todayCourses = ref<ScheduleItem[]>([])
const loading = ref(false)
// 即将开始筛选范围（天）
const upcomingDays = ref(14)

// 当月日历数据
const calendarLoading = ref(false)
const monthDate = ref(new Date())
const monthSchedules = ref<ScheduleItem[]>([])

// 全部课程抽屉
const allSchedulesVisible = ref(false)
const allSchedulesLoading = ref(false)
const allSchedules = ref<ScheduleItem[]>([])

// 统计数据
const statistics = ref({
  rescheduleRequests: 0,
  totalCourses: 0,
  upcomingClasses: 0,
  bookingRequests: 0,
  currentHours: 0,
  lastHours: 0,
  monthlyRescheduleCount: 0
})
const statsLoading = ref(false)
// 工资统计（基于时薪 * 课时）
const salary = ref({ currentSalary: 0, lastSalary: 0 })

// 课时详情模态框
const showHourDetailsModal = ref(false)
const hourDetails = ref({
  normalHours: 0,        // 正常上课的课时数
  teacherDeduction: 0,   // 教师超出调课/请假次数后所扣课时
  studentCompensation: 0, // 学生超出调课/请假次数的补偿课时
  adminAdjustment: 0,    // 总Admin手动调整记录
  totalHours: 0          // 总课时
})
const hourDetailsLoading = ref(false)

// 调课弹窗
const showRescheduleModal = ref(false)
const rescheduleCourse = ref<any>(null)
// 课程详情弹窗
const showScheduleDetailModal = ref(false)

// 停课申请弹窗
const showSuspensionDialog = ref(false)
const suspensionForm = ref<{ startDate: string; endDate: string; reason: string }>({ startDate: '', endDate: '', reason: '' })
const currentEnrollmentId = ref<number | null>(null)

const selectedSchedule = ref<ScheduleItem | null>(null)

// 根据当前路由设置激活菜单
watch(() => route.path, (path: string) => {
  if (path.includes('/profile')) {
    activeMenu.value = 'profile'
  } else if (path.includes('/password')) {
    activeMenu.value = 'password'
  } else if (path.includes('/schedule')) {
    activeMenu.value = 'schedule'
  } else if (path.includes('/bookings')) {
    activeMenu.value = 'bookings'
  } else if (path.includes('/reschedule')) {
    activeMenu.value = 'reschedule'

  } else {
    activeMenu.value = 'dashboard'
  }
}, { immediate: true })

// 获取即将开始的课程（优先复用当月课表，避免重复请求）
const fetchUpcomingCourses = async () => {
  try {
    loading.value = true
    const today = new Date()
    const rangeDays = upcomingDays.value
    const end = new Date(today.getTime() + rangeDays * 24 * 60 * 60 * 1000)

    const d = new Date(monthDate.value)
    const monthStart = new Date(d.getFullYear(), d.getMonth(), 1)
    const monthEnd = new Date(d.getFullYear(), d.getMonth() + 1, 0)

    const inSameMonthRange = (today >= monthStart) && (end <= monthEnd)

    let source: ScheduleItem[] = []
    if (inSameMonthRange) {
      // 当月范围足够，直接使用已加载的当月课表
      source = monthSchedules.value || []
    } else {
      // 跨月时再按需请求一次较小范围
      const todayStr = today.toISOString().split('T')[0]
      const endStr = end.toISOString().split('T')[0]
      const response = await bookingAPI.getTeacherSchedules({ startDate: todayStr, endDate: endStr })
      source = (response.success && Array.isArray(response.data)) ? response.data : []
      if (!response.success) {
        ElMessage.error(response.message || '获取即将开始课程失败')
      }
    }

    const upcoming = source.filter((s: ScheduleItem) => s.status === 'progressing')
      .sort((a: ScheduleItem, b: ScheduleItem) => {
        const d = a.scheduledDate.localeCompare(b.scheduledDate)
        if (d !== 0) return d
        return a.startTime.localeCompare(b.startTime)
      }).slice(0, 10)

    todayCourses.value = upcoming
  } catch (error) {
    console.error('获取即将开始课程失败:', error)
    ElMessage.error('获取即将开始课程失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 格式化时间显示
const formatTimeRange = (startTime: string, endTime: string) => {
  return `${startTime}-${endTime}`
}
// 查看课程详情
const viewScheduleDetail = (schedule: ScheduleItem) => {
  selectedSchedule.value = schedule
  showScheduleDetailModal.value = true
}

// 课程状态 -> 中文
const getScheduleStatusText = (status?: string) => {
  switch (status) {
    case 'progressing':
      return '进行中'
    case 'completed':
      return '已完成'
    case 'cancelled':
    case 'canceled':
      return '已取消'
    case 'rescheduled':
      return '已改期'
    default:
      return status || '-'
  }
}

// 课程状态 -> 标签类型
const getScheduleStatusTag = (status?: string) => {
  switch (status) {
    case 'progressing':
      return 'primary'
    case 'completed':
      return 'success'
    case 'rescheduled':
      return 'warning'
    case 'cancelled':
    case 'canceled':
      return 'info'
    default:
      return 'info'
  }
}

// 加载两周内课表（用于日历渲染）
const loadMonthSchedules = async () => {
  try {
    calendarLoading.value = true
    const d = new Date(monthDate.value)
    const now = new Date()
    const isCurrentMonth = d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth()

    let start: Date
    let end: Date

    if (isCurrentMonth) {
      // 当前月：从今天起两周
      start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      end = new Date(start)
      end.setDate(start.getDate() + 13)
    } else {
      // 非当前月：从该月第一天起两周，最多到该月末
      start = new Date(d.getFullYear(), d.getMonth(), 1)
      end = new Date(start)
      end.setDate(start.getDate() + 13)
      const monthEnd = new Date(d.getFullYear(), d.getMonth() + 1, 0)
      if (end.getTime() > monthEnd.getTime()) end = monthEnd
    }

    const startStr = start.toISOString().split('T')[0]
    const endStr = end.toISOString().split('T')[0]
    const res = await bookingAPI.getTeacherSchedules({ startDate: startStr, endDate: endStr })
    if (res.success && res.data) {
      monthSchedules.value = res.data
    } else {
      ElMessage.error(res.message || '获取课表失败')
    }
  } catch (e) {
//      load all schedules for current teacher (very wide range)
const loadAllSchedules = async () => {
  try {
    allSchedulesLoading.value = true
    const res = await bookingAPI.getTeacherSchedules({ startDate: '1970-01-01', endDate: '2099-12-31' })
    if (res.success && Array.isArray(res.data)) {
      allSchedules.value = res.data.sort((a: any, b: any) => {
        const d = a.scheduledDate.localeCompare(b.scheduledDate)
        if (d !== 0) return d
        return a.startTime.localeCompare(b.startTime)
      })
    } else {
      allSchedules.value = []
      ElMessage.error(res.message || '获取全部课程失败')
    }
  } catch (e) {
    allSchedules.value = []
    ElMessage.error('获取全部课程失败，请稍后重试')
  } finally {
    allSchedulesLoading.value = false
  }
}

const openAllSchedulesDrawer = async () => {
  allSchedulesVisible.value = true
  if (allSchedules.value.length === 0) await loadAllSchedules()
}

    console.error('加载当月课表失败:', e)
  } finally {
    calendarLoading.value = false
  }
}

// 全部课程抽屉（独立实现，避免受上方函数内作用域影响）
const allVisible = ref(false)
const allLoading = ref(false)
const allList = ref<ScheduleItem[]>([])

const fetchAllSchedules = async () => {
  try {
    allLoading.value = true
    const res = await bookingAPI.getTeacherSchedules({ startDate: '1970-01-01', endDate: '2099-12-31' })
    if (res.success && Array.isArray(res.data)) {
      allList.value = res.data.sort((a: any, b: any) => {
        const d = a.scheduledDate.localeCompare(b.scheduledDate)
        if (d !== 0) return d
        return a.startTime.localeCompare(b.startTime)
      })
    } else {
      allList.value = []
      ElMessage.error(res.message || '获取全部课程失败')
    }
  } catch (e) {
    allList.value = []
    ElMessage.error('获取全部课程失败，请稍后重试')
  } finally {
    allLoading.value = false
  }
}

const openAllSchedules = async () => {
  allVisible.value = true
  if (allList.value.length === 0) await fetchAllSchedules()
}


const dateHasSchedule = (date: Date) => {
  const ds = date.toISOString().split('T')[0]
  return monthSchedules.value.some(s => s.scheduledDate === ds)
}

const dateScheduleStatus = (date: Date): 'none' | 'has' => {
  return dateHasSchedule(date) ? 'has' : 'none'
}

// 打开调课弹窗
const openRescheduleModal = (schedule: ScheduleItem) => {
  const enhancedSchedule = {
    id: schedule.id,
    teacherId: schedule.teacherId,
    teacherName: schedule.teacherName || '当前教师',
    studentId: schedule.studentId,
    studentName: schedule.studentName,
    courseId: schedule.courseId,
    courseTitle: schedule.courseTitle || schedule.courseName || '自定义课程',
    subjectName: schedule.subjectName || '未知科目',
    scheduledDate: schedule.scheduledDate,
    startTime: schedule.startTime,
    endTime: schedule.endTime,
    durationMinutes: schedule.durationMinutes || 120,
    totalTimes: 1,
    status: schedule.status || 'progressing',
    teacherNotes: undefined,
    studentFeedback: undefined,
    createdAt: undefined,
    bookingRequestId: undefined,
    bookingSource: undefined,
    isTrial: schedule.isTrial,
    sessionNumber: 1,
    courseType: 'regular'
  }

  rescheduleCourse.value = {
    id: schedule.courseId || schedule.id,
    title: buildOneToOneTitle(schedule) || schedule.courseTitle || schedule.courseName || '自定义课程',
    teacher: schedule.teacherName || '当前教师',
    teacherId: schedule.teacherId,
    subject: schedule.subjectName || '未知科目',
    remainingLessons: 1,
    weeklySchedule: [],
    schedules: [enhancedSchedule]
  }
  showRescheduleModal.value = true
}


// 打开停课弹窗（从即将开始/详情发起）
const openSuspensionDialog = (schedule: ScheduleItem) => {
  if (!schedule.enrollmentId) {
    ElMessage.error('缺少报名ID，无法发起停课申请')
    return
  }
  currentEnrollmentId.value = schedule.enrollmentId
  const today = new Date()
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 7)
  const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 13)
  const fmt = (d: Date) => d.toISOString().split('T')[0]
  suspensionForm.value.startDate = fmt(start)
  suspensionForm.value.endDate = fmt(end)
  suspensionForm.value.reason = ''
  showSuspensionDialog.value = true
}

const submitSuspension = async () => {
  if (!currentEnrollmentId.value) return
  try {
    // 基本前端校验：start ≥ 今日+7，end ≥ start+13
    const parse = (s: string) => new Date(s + 'T00:00:00')
    const today = new Date()
    const minStart = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 7)
    if (parse(suspensionForm.value.startDate) < minStart) {
      ElMessage.error('停课开始日期需从一周后开始')
      return
    }
    const minEnd = new Date(parse(suspensionForm.value.startDate).getTime() + 13 * 24 * 3600 * 1000)
    if (parse(suspensionForm.value.endDate) < minEnd) {
      ElMessage.error('停课时长不少于两周')
      return
    }

    // 验证区间内至少包含2次课（以该报名ID筛选）
    const listRes = await bookingAPI.getTeacherSchedules({ startDate: suspensionForm.value.startDate, endDate: suspensionForm.value.endDate })
    if (!(listRes.success && Array.isArray(listRes.data))) {
      ElMessage.error('无法获取课程安排，请稍后重试')
      return
    }
    const lessons = listRes.data.filter((s: any) => s.enrollmentId === currentEnrollmentId.value && (!s.status || s.status !== 'cancelled'))
    if (lessons.length < 2) {
      ElMessage.error('停课区间内需至少包含2次课')
      return
    }

    const res = await suspensionAPI.createRequest({
      enrollmentId: currentEnrollmentId.value,
      startDate: suspensionForm.value.startDate,
      endDate: suspensionForm.value.endDate,
      reason: suspensionForm.value.reason?.trim() || undefined,
    })
    if (res.success) {
      ElMessage.success('停课申请已提交，等待管理员审批')
      showSuspensionDialog.value = false
    } else {
      ElMessage.error(res.message || '停课申请提交失败')
    }
  } catch (e: any) {
    console.error('停课申请失败:', e)
    ElMessage.error(e?.message || '停课申请失败')
  }
}


const handleRescheduleSuccess = () => {
  fetchUpcomingCourses()
  loadMonthSchedules()
}

// 获取统计数据
const loadStatistics = async () => {
  try {
    statsLoading.value = true
    const [statsRes, profileRes] = await Promise.all([
      teacherAPI.getStatistics(),
      teacherAPI.getProfile()
    ])

    if (statsRes.success && statsRes.data) {
      statistics.value = {
        rescheduleRequests: statsRes.data.rescheduleRequests || 0,
        totalCourses: statsRes.data.totalCourses || 0,
        upcomingClasses: statsRes.data.upcomingClasses || 0,
        bookingRequests: statsRes.data.bookingRequests || 0,
        currentHours: statsRes.data.currentHours || 0,
        lastHours: statsRes.data.lastHours || 0,
        monthlyRescheduleCount: statsRes.data.monthlyRescheduleCount || 0
      }
    } else {
      ElMessage.error(statsRes.message || '获取统计数据失败')
    }

    // 基于资料中的时薪计算工资
    if (profileRes.success && profileRes.data) {
      const rate = Number(profileRes.data.hourlyRate || 0)
      const ch = Number(statistics.value.currentHours || 0)
      const lh = Number(statistics.value.lastHours || 0)
      salary.value.currentSalary = Math.round(rate * ch * 100) / 100
      salary.value.lastSalary = Math.round(rate * lh * 100) / 100
    } else {
      ElMessage.error(profileRes.message || '获取教师资料失败')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败，请稍后重试')
  } finally {
    statsLoading.value = false
  }
}

// 获取课时详情
const loadHourDetails = async () => {
  try {
    hourDetailsLoading.value = true
    const res = await teacherAPI.getHourDetails()
    if (res.success && res.data) {
      hourDetails.value = res.data
    } else {
      ElMessage.error(res.message || '获取课时详情失败')
    }
  } catch (error) {
    console.error('获取课时详情失败:', error)
    ElMessage.error('获取课时详情失败，请稍后重试')
  } finally {
    hourDetailsLoading.value = false
  }
}

// 组件挂载时获取数据（先加载当月课表，再计算即将开始，避免首屏重复请求）
onMounted(async () => {
  if (userStore.user && !userStore.user.avatarUrl) {
    await userStore.loadUserAvatar()
  }
  await loadMonthSchedules()
  await fetchUpcomingCourses()
  loadStatistics()
})
</script>

<template>
  <div class="teacher-center">
    <el-container>
      <el-aside width="250px">
        <div class="sidebar">
          <div class="user-info">
            <div class="avatar">
              <el-avatar :size="64" :src="userStore.user?.avatarUrl || $getImageUrl('@/assets/pictures/teacherBoy2.jpeg')" />
            </div>
            <div class="info">
              <h3>{{ userStore.user?.username }}</h3>
              <p>教师</p>
            </div>
          </div>
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical"
            @select="activeMenu = $event"
          >
            <el-menu-item index="dashboard" @click="$router.push('/teacher-center')">
              <el-icon><HomeFilled /></el-icon>
              <span>我的课表</span>
            </el-menu-item>
            <el-menu-item index="bookings" @click="$router.push('/teacher-center/bookings')">
              <el-icon><DocumentChecked /></el-icon>
              <span>调课记录</span>
            </el-menu-item>
            <el-menu-item index="grade-entry" @click="$router.push('/teacher-center/grade-entry')">
              <el-icon><Reading /></el-icon>
              <span>成绩录入</span>
            </el-menu-item>


            <el-menu-item index="my-hour-details">
              <el-icon><List /></el-icon>
              <span>上课记录</span>
            </el-menu-item>


            <el-menu-item index="messages">
              <el-icon><ChatDotRound /></el-icon>
              <span>消息中心</span>
            </el-menu-item>

            <el-menu-item index="profile" @click="$router.push('/teacher-center/profile')">
              <el-icon><Setting /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="password" @click="$router.push('/teacher-center/password')">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-aside>
      <el-main>
        <div v-if="activeMenu === 'dashboard'" class="dashboard">
          <h2>我的课表</h2>
          <div class="dashboard-stats">
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Refresh /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.monthlyRescheduleCount }}</div>
                <div class="stat-label">本月已申请调课/请假次数</div>
              </div>
            </div>
            <div class="stat-card clickable" @click="showHourDetailsModal = true">
              <div class="stat-icon">
                <el-icon><Money /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.currentHours }}</div>
                <div class="stat-label">本月累计课时(小时)</div>
                <div class="stat-hint">点击查看详情</div>
              </div>
            </div>
          </div>

          <div class="calendar-section">
            <div class="calendar-header">
              <h3>当月课表</h3>
              <div class="calendar-actions">
                <el-date-picker
                  v-model="monthDate"
                  type="month"
                  placeholder="选择月份"
                  format="YYYY-MM"
                  value-format="YYYY-MM-DD"
                  @change="loadMonthSchedules"
                  style="width: 160px;"
                />
                <el-button type="primary" @click="loadMonthSchedules" :loading="calendarLoading" style="margin-left: 8px;">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
                <el-button @click="openAllSchedules" style="margin-left: 8px;">查看全部课程</el-button>
              </div>


            </div>
            <el-calendar v-model="monthDate">
              <template #date-cell="{ data }">
                <div class="calendar-cell" :class="dateScheduleStatus(data.date) === 'has' ? 'has-schedule' : 'no-schedule'">
                  <span class="date-number">{{ data.day.split('-').slice(-1)[0] }}</span>
                  <span v-if="dateScheduleStatus(data.date) === 'has'" class="dot"></span>
                </div>
              </template>
            </el-calendar>
            <div class="calendar-legend">
              <span><i class="dot"></i> 有课程</span>
              <span class="muted">无课程</span>
            </div>
          </div>

          <div class="quick-actions">
            <h3>快捷操作</h3>
            <div class="action-buttons">

              <el-button type="warning" @click="$router.push({ path: '/about', hash: '#contact-us' })">
                <el-icon><ChatLineRound /></el-icon>
                联系客服
              </el-button>
            </div>
          </div>

          <div class="upcoming-courses">
            <div class="upcoming-header">
              <h3>即将开始课程</h3>
              <div class="upcoming-actions">

    <!-- 停课申请弹窗（教师） -->
    <el-dialog v-model="showSuspensionDialog" title="停课申请" width="520px">
      <el-form label-width="96px">
        <el-form-item label="开始日期">
          <el-date-picker v-model="suspensionForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择开始日期" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="suspensionForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择结束日期" />
        </el-form-item>
        <el-form-item label="原因（可选）">
          <el-input v-model="suspensionForm.reason" type="textarea" :rows="3" placeholder="可填写原因，便于管理员审核" />
        </el-form-item>
        <el-alert type="warning" :closable="false" show-icon>
          停课规则：开始日期需从一周后起，且覆盖时长不少于两周；审批通过仅对区间内未开始课节生效。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="showSuspensionDialog = false">取消</el-button>
        <el-button type="primary" @click="submitSuspension">提交申请</el-button>
      </template>
    </el-dialog>

                <el-select v-model="upcomingDays" style="width: 140px;" @change="fetchUpcomingCourses">
                  <el-option :value="14" label="14天内" />
                  <el-option :value="30" label="30天内" />
                </el-select>
              </div>
            </div>
            <div v-loading="loading">
              <el-table :data="todayCourses" style="width: 100%">
                <el-table-column prop="scheduledDate" label="日期" width="120" />
                <el-table-column label="时间" width="140">
                  <template #default="scope">
                    {{ formatTimeRange(scope.row.startTime, scope.row.endTime) }}
                  </template>
                </el-table-column>
                <el-table-column label="课程">
                  <template #default="scope">
                    {{ buildOneToOneTitle(scope.row) || scope.row.courseTitle || scope.row.courseName }}
                  </template>
                </el-table-column>
                <el-table-column prop="studentName" label="学生" width="120" />
                <el-table-column label="类型" width="80">
                  <template #default="scope">
                    <el-tag v-if="scope.row.isTrial" type="warning" size="small">试听</el-tag>
                    <el-tag v-else type="success" size="small">正式</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="260">
                  <template #default="scope">
                    <el-button type="primary" size="small" @click="viewScheduleDetail(scope.row)">详情</el-button>
                    <el-button type="warning" size="small" @click="openRescheduleModal(scope.row)">
                      <el-icon><Refresh /></el-icon>
                      申请调课
                    </el-button>

                    <el-button type="danger" size="small" @click="openSuspensionDialog(scope.row)">停课</el-button>

                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!loading && todayCourses.length === 0" class="empty-state">
                <el-empty description="暂无即将开始的课程" />
              </div>
            </div>
          </div>


        </div>
        <div v-else-if="activeMenu === 'messages'">
          <TeacherMessages />
        </div>

        <div v-else-if="activeMenu === 'my-hour-details'">
          <TeacherHourDetails />
        </div>

        <div v-else>
          <router-view></router-view>
        </div>
      </el-main>
    </el-container>

    <RescheduleModal
      v-model="showRescheduleModal"
      :course="rescheduleCourse"
      :is-teacher="true"
      @success="handleRescheduleSuccess"
    />

    <!-- 课程详情弹窗 -->
    <el-dialog
      v-model="showScheduleDetailModal"
      title="课程详情"
      width="640px"
    >
      <div v-if="selectedSchedule" class="schedule-detail">
        <div class="detail-header">
          <div class="course-title">{{ buildOneToOneTitle(selectedSchedule) || selectedSchedule.courseTitle || selectedSchedule.courseName }}</div>
          <div class="chips">
            <el-tag type="info" size="small">{{ selectedSchedule.scheduledDate }}</el-tag>
            <el-tag type="success" size="small">{{ formatTimeRange(selectedSchedule.startTime, selectedSchedule.endTime) }}</el-tag>
            <el-tag :type="selectedSchedule.isTrial ? 'warning' : 'success'" size="small">{{ selectedSchedule.isTrial ? '试听' : '正式' }}</el-tag>
          </div>
        </div>
<!--

-->


        <div class="detail-grid">
          <div class="detail-item">
            <div class="label">学生</div>
            <div class="value">{{ selectedSchedule.studentName || '-' }}</div>
          </div>
          <div class="detail-item">
            <div class="label">教师</div>
            <div class="value">{{ selectedSchedule.teacherName || '当前教师' }}</div>
          </div>
          <div class="detail-item">

    <!-- 全部课程抽屉 -->
    <el-drawer v-model="allVisible" title="全部课程" size="70%">
      <div v-loading="allLoading">
        <el-table :data="allList" style="width: 100%">
          <el-table-column prop="scheduledDate" label="日期" width="120" />
          <el-table-column label="时间" width="140">
            <template #default="scope">
              {{ scope.row.startTime }}-{{ scope.row.endTime }}
            </template>
          </el-table-column>
          <el-table-column label="课程">
            <template #default="scope">
              {{ buildOneToOneTitle(scope.row) || scope.row.courseTitle || scope.row.courseName }}
            </template>
          </el-table-column>
          <el-table-column prop="studentName" label="学生" width="140" />
          <el-table-column prop="subjectName" label="科目" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getScheduleStatusTag(scope.row.status)" size="small">{{ getScheduleStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!allLoading && allList.length === 0" class="empty-state">
          <el-empty description="暂无课程" />
        </div>
      </div>
    </el-drawer>

            <div class="label">学科</div>
            <div class="value">{{ selectedSchedule.subjectName || '-' }}</div>
          </div>
          <div class="detail-item">
            <div class="label">时长</div>
            <div class="value">{{ selectedSchedule.durationMinutes || 0 }} 分钟</div>
          </div>
          <div class="detail-item">
            <div class="label">状态</div>
            <div class="value">
              <el-tag :type="getScheduleStatusTag(selectedSchedule.status)" size="small">{{ getScheduleStatusText(selectedSchedule.status) }}</el-tag>
            </div>
          </div>
        </div>

        <el-alert type="info" :closable="false" class="hint">
          提示：如需调整本次课程，请使用“申请调课”；如需暂停接下来一段时间的课程，请使用“停课”。
        </el-alert>
      </div>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="showScheduleDetailModal = false">关闭</el-button>
          <el-button type="warning" @click="selectedSchedule && openRescheduleModal(selectedSchedule)">
            <el-icon><Refresh /></el-icon>
            申请调课
          </el-button>

          <el-button type="danger" @click="selectedSchedule && openSuspensionDialog(selectedSchedule)">停课</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 课时详情模态框 -->
    <el-dialog
      v-model="showHourDetailsModal"
      title="本月课时详情"
      width="600px"
      @open="loadHourDetails"
    >
      <div v-loading="hourDetailsLoading" class="hour-details-content">
        <div class="detail-item">
          <div class="detail-label">正常上课的课时数：</div>
          <div class="detail-value positive">{{ hourDetails.normalHours }}小时</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">教师超出调课/请假次数后所扣课时：</div>
          <div class="detail-value negative">-{{ hourDetails.teacherDeduction }}小时</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">学生超出调课/请假次数的补偿课时：</div>
          <div class="detail-value positive">+{{ hourDetails.studentCompensation }}小时</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Admin手动调整记录：</div>
          <div class="detail-value" :class="hourDetails.adminAdjustment >= 0 ? 'positive' : 'negative'">
            {{ hourDetails.adminAdjustment >= 0 ? '+' : '' }}{{ hourDetails.adminAdjustment }}小时
          </div>
        </div>
        <el-divider />
        <div class="detail-item total">
          <div class="detail-label">本月累计课时：</div>
          <div class="detail-value total-value">{{ hourDetails.totalHours }}小时</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showHourDetailsModal = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>



<style scoped>
.teacher-center {
  height: calc(100vh - 60px);
}

.el-container {
  height: 100%;
}

.el-aside {
  background-color: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.user-info {
  padding: 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #eee;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 15px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info h3 {
  margin: 0 0 5px 0;
  font-size: 16px;
}

.info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.el-menu-vertical {
  border-right: none;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}

.dashboard h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  background-color: #ecf5ff;
  color: #409EFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.quick-actions, .upcoming-courses {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.calendar-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.calendar-cell {
  position: relative;
  padding: 4px 6px;
}

.calendar-cell .date-number {
  font-size: 12px;
}

.calendar-cell .dot {
  position: absolute;
  right: 6px;
  bottom: 6px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #67c23a;
}

.calendar-legend {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.calendar-legend .dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  margin-right: 6px;
}

.calendar-legend .muted {
  color: #999;
}

.upcoming-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.quick-actions h3, .upcoming-courses h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.action-buttons {
  display: flex;
  gap: 15px;
}

/* 课时详情模态框样式 */
.clickable {
  cursor: pointer;
  transition: all 0.3s ease;
}

.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.1);
}

.stat-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.hour-details-content {
  padding: 20px 0;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item.total {
  font-weight: bold;
  font-size: 16px;
  padding: 16px 0;
}

.detail-label {
  color: #666;
  flex: 1;
}

.detail-value {
  font-weight: 600;
  font-size: 16px;
}

.detail-value.positive {
  color: #67c23a;
}

.detail-value.negative {
  color: #f56c6c;
}

.detail-value.total-value {
  color: #409eff;
  font-size: 18px;
}

/* 课程详情弹窗样式 */
.schedule-detail {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.course-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.chips {
  display: flex;
  gap: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  padding: 8px 0 12px;
}

.detail-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  border-left: 4px solid #409eff20;
}

.detail-item .label {
  color: #606266;
  font-weight: 500;
  margin-bottom: 6px;
}

.detail-item .value {
  color: #303133;
}

.hint {
  margin-top: 8px;
}

.dialog-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* 响应式布局 */
@media (max-width: 992px) {
  .el-aside {
    width: 200px !important;
  }

  .stat-card {
    width: calc(50% - 20px);
  }
}

@media (max-width: 768px) {
  .teacher-center {
    padding-top: 56px; /* 适配移动端导航高度 */
  }

  .el-container {
    flex-direction: column;
    height: calc(100vh - 56px);
  }

  .el-aside {
    width: 100% !important;
    height: auto;
    order: 2; /* 将侧边栏移到底部 */
    box-shadow: 0 -2px 12px 0 rgba(0, 0, 0, 0.05);
  }

  .el-main {
    padding: 16px;
    order: 1;
    overflow-y: auto;
    flex: 1;
  }

  .sidebar {
    height: auto;
  }

  .user-info {
    flex-direction: row;
    padding: 12px 16px;
    align-items: center;
  }

  .avatar {
    margin-right: 12px;
    margin-bottom: 0;
  }

  .info h3 {
    font-size: 14px;
    margin-bottom: 2px;
  }

  .info p {
    font-size: 12px;
    color: #999;
  }

  .el-menu-vertical {
    display: flex;
    overflow-x: auto;
    border-right: none;
    border-bottom: 1px solid #e6e6e6;
  }

  .el-menu-vertical .el-menu-item {
    flex-shrink: 0;
    white-space: nowrap;
    padding: 0 16px;
    height: 48px;
    line-height: 48px;
    border-bottom: none;
    border-right: 1px solid #e6e6e6;
  }

  .el-menu-vertical .el-menu-item:last-child {
    border-right: none;
  }

  .el-menu-vertical .el-menu-item span {
    margin-left: 8px;
    font-size: 13px;
  }

  .dashboard {
    padding: 0;
  }

  .dashboard h2 {
    font-size: 20px;
    margin-bottom: 16px;
  }

  .dashboard-stats {
    grid-template-columns: 1fr;
    gap: 12px;
    margin-bottom: 20px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-card h3 {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 24px;
  }

  .quick-actions h3,
  .upcoming-courses h3 {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 8px;
  }

  .action-buttons .el-button {
    width: 100%;
    margin-bottom: 0;
  }
}

@media (max-width: 480px) {
  .teacher-center {
    padding-top: 52px;
  }

  .el-container {
    height: calc(100vh - 52px);
  }

  .el-main {
    padding: 12px;
  }

  .user-info {
    padding: 10px 12px;
  }

  .dashboard-stats {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .stat-card {
    width: 100%;
    padding: 12px;
  }

  .el-menu-vertical .el-menu-item {
    padding: 0 12px;
    height: 44px;
    line-height: 44px;
  }

  .el-menu-vertical .el-menu-item span {
    font-size: 12px;
  }
}
</style>

