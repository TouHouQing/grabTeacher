<script setup lang="ts">
import { ref, watch, onMounted, defineAsyncComponent, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { bookingAPI, teacherAPI, rescheduleAPI, suspensionAPI } from '../../utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'


import {
  HomeFilled,
  DocumentChecked,
  Reading,
  Money,
  Setting,
  ChatDotRound,
  ChatLineRound,
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
  teacherHourlyRate?: number
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



// 将当月课表按日期分组，供日历单元格快速渲染“课程名 + 时间段”（O(n) 预聚合，避免每格扫描）
const schedulesMapByDate = computed<Record<string, Array<{ title: string; startTime: string; endTime: string; isTrial: boolean }>>>(() => {
  const map: Record<string, Array<{ title: string; startTime: string; endTime: string; isTrial: boolean }>> = {}
  for (const s of monthSchedules.value) {
    const ds = (s as any)?.scheduledDate
    const st = (s as any)?.startTime
    const et = (s as any)?.endTime
    if (!ds || !st || !et) continue
    const titleFull = buildOneToOneTitle(s as any) || (s as any)?.courseTitle || (s as any)?.courseName || '课程'
    ;(map[ds] = map[ds] || []).push({ title: titleFull, startTime: st, endTime: et, isTrial: !!(s as any)?.isTrial })
  }
  Object.keys(map).forEach(k => map[k].sort((a, b) => a.startTime.localeCompare(b.startTime)))
  return map
})

// 某天的条目数组：[{ title, timeText, isTrial }]
const getDayEntries = (date: Date): Array<{ title: string; timeText: string; isTrial: boolean }> => {
  const ds = date.toISOString().split('T')[0]
  const arr = schedulesMapByDate.value[ds] || []
  return arr.map((it: { title: string; startTime: string; endTime: string; isTrial: boolean }) => ({
    title: it.title,
    timeText: `${it.startTime}-${it.endTime}`,
    isTrial: it.isTrial
  }))
}

// 当日条目明细弹窗
const showDayEntriesDialog = ref(false)
const dayEntriesDialog = ref<{ date: string; entries: Array<{ title: string; timeText: string; isTrial: boolean }> }>({ date: '', entries: [] })

function openDayEntriesDialog(date: Date) {
  const ds = date.toISOString().split('T')[0]
  dayEntriesDialog.value.date = ds
  dayEntriesDialog.value.entries = getDayEntries(date)
  showDayEntriesDialog.value = true
}



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

// 课时详情模态框
const showHourDetailsModal = ref(false)
const hourDetails = ref({
  normalHours: 0,        // 正常上课的课时数
  teacherDeduction: 0,   // 教师超出调课/请假次数后所扣课时
  studentCompensation: 0, // 学生超出调课/请假次数的补偿课时
  adminAdjustment: 0,    // 总Admin手动调整记录
  totalHours: 0,         // 总课时
  // 新增：基于课程的本月/上月课时与薪资汇总
  courseSummaries: [] as Array<any>,
  totalCurrentHoursByCourses: 0,
  totalLastHoursByCourses: 0,
  totalCurrentAmount: 0,
  totalLastAmount: 0
})
const hourDetailsLoading = ref(false)

// 调课弹窗
const showRescheduleModal = ref(false)
const rescheduleCourse = ref<any>(null)
// 为调课弹窗传入教师全量课表，保障“原定课程”可选
const rescheduleAllSchedules = ref<ScheduleItem[]>([])
// 课程详情弹窗
const showScheduleDetailModal = ref(false)


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
  } else if (path.includes('/grade-entry')) {
    activeMenu.value = 'grade-entry'
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

  // 教师端请假/停课申请
  const showSuspensionDialog = ref(false)
  const currentSuspensionSchedule = ref<ScheduleItem | null>(null)
  const suspensionForm = ref<{ dateRange: string[]; reason: string }>({ dateRange: [], reason: '' })
  const dateRangeSelected = computed(() => {
    const r = suspensionForm.value.dateRange
    return !!r && r.length === 2 && !!r[0] && !!r[1]
  })
  const disablePastDate = (date: Date) => {
    const today = new Date(); today.setHours(0,0,0,0)
    return date.getTime() < today.getTime()
  }

  const deriveEnrollmentId = async (schedule: ScheduleItem): Promise<number | null> => {
    if (schedule.enrollmentId && Number(schedule.enrollmentId) > 0) return Number(schedule.enrollmentId)
    try {
      const todayStr = new Date().toISOString().split('T')[0]
      const res = await bookingAPI.getTeacherSchedules({ startDate: todayStr, endDate: '2099-12-31' })
      if (res.success && Array.isArray(res.data)) {
        const same = res.data.find((s: any) =>
          s && s.enrollmentId && s.teacherId === schedule.teacherId && s.studentId === schedule.studentId && s.courseId === schedule.courseId
        )
        if (same?.enrollmentId) return Number(same.enrollmentId)
      }
    } catch {}
    return null
  }

  const submitTeacherSuspension = async () => {
    if (!currentSuspensionSchedule.value) return
    const range = suspensionForm.value.dateRange
    if (!range || range.length !== 2) {
      ElMessage.warning('请选择请假起止日期')
      return
    }

    const eid = await deriveEnrollmentId(currentSuspensionSchedule.value)
    if (!eid) {
      ElMessage.error('未找到报名信息，暂无法提交请假申请')
      return
    }
    try {
      const payload = {
        enrollmentId: eid,
        startDate: range[0],
        endDate: range[1],
        reason: (suspensionForm.value.reason || '').trim() || undefined
      }
      const result = await suspensionAPI.createRequest(payload as any)
      if ((result as any)?.success) {
        ElMessage.success('请假申请已提交，等待管理员审批')
        showSuspensionDialog.value = false
        await fetchUpcomingCourses()
        await loadMonthSchedules()
      } else {
        ElMessage.error((result as any)?.message || '提交请假申请失败')
      }
    } catch (e: any) {
      ElMessage.error(e?.message || '提交请假申请失败，请稍后重试')
    }
  }

  // 入口：点击“请假”
  const handleSuspend = async (schedule: ScheduleItem) => {
    if (schedule.isTrial) {
      try {
        await ElMessageBox.confirm(
          '确认取消本次试听课？此操作将删除该节课并恢复学生的试听次数。',
          '确认取消试听',
          { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' }
        )
        await bookingAPI.cancelTrialSchedule(Number(schedule.id))
        ElMessage.success('已取消试听课并恢复学生试听次数')
        await fetchUpcomingCourses()
        await loadMonthSchedules()
      } catch (e: any) {
        if (e !== 'cancel') {
          ElMessage.error(e?.message || '操作失败，请稍后重试')
        }
      }
      return
    }
    currentSuspensionSchedule.value = schedule
    suspensionForm.value = { dateRange: [], reason: '' }
    showSuspensionDialog.value = true
  }

// 详情弹窗：计算显示的时长（试听课固定30分钟；否则取字段或按起止时间计算）
const getDetailDurationMinutes = (s: ScheduleItem | null | undefined): number => {
  if (!s) return 0
  if (s.isTrial) return 30
  if (typeof s.durationMinutes === 'number' && s.durationMinutes > 0) return s.durationMinutes
  try {
    const [sh, sm] = (s.startTime || '00:00').split(':').map((x)=>Number(x))
    const [eh, em] = (s.endTime || '00:00').split(':').map((x)=>Number(x))
    const start = sh*60 + sm
    const end = eh*60 + em
    return Math.max(0, end - start)
  } catch {
    return 0
  }
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
    // 改为整月范围：当月第一天 ~ 当月最后一天（确保当月日历完整填充）
    const start: Date = new Date(d.getFullYear(), d.getMonth(), 1)
    const end: Date = new Date(d.getFullYear(), d.getMonth() + 1, 0)

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

    console.error('加载当月课表失败:', e)
  } finally {
    calendarLoading.value = false
  }
}

// 全部课程抽屉（独立实现，避免受上方函数内作用域影响）


const dateHasSchedule = (date: Date) => {
  const ds = date.toISOString().split('T')[0]
  return monthSchedules.value.some(s => s.scheduledDate === ds)
}

const dateScheduleStatus = (date: Date): 'none' | 'has' => {
  return dateHasSchedule(date) ? 'has' : 'none'
}

// 打开调课弹窗（复用学生端弹窗逻辑，保证“原定课程”和“剩余课时”正确）
const openRescheduleModal = async (schedule: ScheduleItem) => {
  // 1) 准备全部课表（未来区间），用于弹窗内“原定课程”选择与映射
  try {
    const today = new Date()
    const startStr = today.toISOString().split('T')[0]
    const res = await bookingAPI.getTeacherSchedules({ startDate: startStr, endDate: '2099-12-31' })
    if (res.success && Array.isArray(res.data)) {
      rescheduleAllSchedules.value = res.data
    } else {
      rescheduleAllSchedules.value = []
    }
  } catch (e) {
    rescheduleAllSchedules.value = []
  }

  // 2) 计算“剩余课时”：同一报名(enrollmentId)下，未取消且日期>=今天的节次数
  const isFuture = (d: string) => d >= new Date().toISOString().split('T')[0]
  const sameEnrollment = (list: ScheduleItem[]) => {
    if (schedule.enrollmentId) {
      return list.filter(s => s.enrollmentId === schedule.enrollmentId && isFuture(s.scheduledDate) && s.status !== 'cancelled' && s.status !== 'canceled')
    }
    // 兜底：若无报名ID，用教师+学生+课程匹配
    return list.filter(s => s.teacherId === schedule.teacherId && s.studentId === schedule.studentId && s.courseId === schedule.courseId && isFuture(s.scheduledDate) && s.status !== 'cancelled' && s.status !== 'canceled')
  }
  const futureList = sameEnrollment(rescheduleAllSchedules.value)
  const remainingLessons = Math.max(futureList.length, 1)

  // 3) 构造弹窗所需课程与节次数据
  const toCourseSchedule = (s: ScheduleItem) => ({
    id: s.id,
    teacherId: s.teacherId!,
    teacherName: s.teacherName || '当前教师',
    studentId: s.studentId!,
    studentName: s.studentName,
    courseId: s.courseId!,
    courseTitle: s.courseTitle || s.courseName || '自定义课程',
    subjectName: s.subjectName || '',
    scheduledDate: s.scheduledDate,
    startTime: s.startTime,
    endTime: s.endTime,
    durationMinutes: s.durationMinutes || 120,
    totalTimes: 1,
    status: (s.status as any) || 'progressing',
    teacherNotes: undefined,
    studentFeedback: undefined,
    createdAt: undefined,
    bookingRequestId: undefined,
    bookingSource: undefined,
    isTrial: s.isTrial,
    sessionNumber: 1,
    courseType: s.courseType || 'regular'
  })

  const scheduleOptions = (futureList.length > 0 ? futureList : [schedule]).map(toCourseSchedule)

  rescheduleCourse.value = {
    id: schedule.courseId || schedule.id,
    title: buildOneToOneTitle(schedule) || schedule.courseTitle || schedule.courseName || '自定义课程',
    teacher: schedule.teacherName || '当前教师',
    teacherId: schedule.teacherId,
    subject: schedule.subjectName || '未知科目',
    remainingLessons,
    weeklySchedule: [],
    schedules: scheduleOptions
  }
  showRescheduleModal.value = true
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

    // 教师资料获取仍保留，但不再用于计算收入（收入已由后端精确计算）
    if (!profileRes.success) {
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
              <h3>{{ userStore.user?.realName || userStore.user?.username }}</h3>
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
            <el-menu-item index="grade-entry" @click="$router.push('/teacher-center/grade-entry')">
              <el-icon><Reading /></el-icon>
              <span>成绩录入</span>
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

              </div>


            <el-dialog v-model="showDayEntriesDialog" :title="`${dayEntriesDialog.date} 当日课程`" width="520px">
              <div v-if="dayEntriesDialog.entries.length===0" style="color:#909399">无课程</div>
              <div v-else class="day-dialog-list">
                <div class="day-dialog-row" v-for="(e,idx) in dayEntriesDialog.entries" :key="idx">
                  <div class="row-title" :class="e.isTrial ? 'trial' : 'regular'">{{ e.title }}</div>
                  <div class="row-time">{{ e.timeText }}</div>
                </div>
              </div>
              <template #footer>
                <el-button @click="showDayEntriesDialog=false">关闭</el-button>
              </template>
            </el-dialog>

            </div>
            <div class="calendar-scroll">
              <el-calendar v-model="monthDate">
                <template #date-cell="{ data }">
                  <div class="calendar-cell" :class="(schedulesMapByDate[data.date.toISOString().split('T')[0]]?.length ? 'has-schedule' : 'no-schedule')"
                       @click="getDayEntries(data.date).length && openDayEntriesDialog(data.date)">
                    <div class="date-number">{{ data.day.split('-').slice(-1)[0] }}</div>
                    <div class="day-entries" v-if="getDayEntries(data.date).length">
                      <div class="entry" v-for="(e, i) in getDayEntries(data.date)" :key="i" :title="`${e.title} ${e.timeText}`">
                        <div class="title-row">
                          <span class="title" :class="e.isTrial ? 'trial' : 'regular'">{{ e.title }}</span>
                        </div>
                        <div class="time-row">
                          <span class="time">{{ e.timeText }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
              </el-calendar>
            </div>
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


                <el-select v-model="upcomingDays" style="width: 140px;" @change="fetchUpcomingCourses">
                  <el-option :value="14" label="14天内" />
                  <el-option :value="30" label="30天内" />
                </el-select>
              </div>
            </div>
            <div v-loading="loading">
              <div class="table-wrap">
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
                      <el-button v-if="!scope.row.isTrial" type="warning" size="small" @click="openRescheduleModal(scope.row)">
                        <el-icon><Refresh /></el-icon>
                        申请调课
                      </el-button>
                      <el-button
                        v-if="!scope.row.isTrial"
                        type="danger"
                        size="small"
                        @click="handleSuspend(scope.row)"
                      >请假</el-button>


                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <div v-if="!loading && todayCourses.length === 0" class="empty-state">
                <el-empty description="暂无即将开始的课程" />
              </div>
            </div>
          </div>

    <!-- 教师端请假申请弹窗（复用停课逻辑） -->
    <el-dialog
      v-model="showSuspensionDialog"
      title="请假申请"
      width="520px"
    >
      <el-form label-width="96px">
        <el-form-item label="请假区间">
          <el-date-picker
            v-model="suspensionForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :unlink-panels="true"
            :disabled-date="disablePastDate"
          />
          <div class="el-form-item__tip" style="margin-left:8px;color:#909399;">提醒：开课前4小时内不可请假；仅支持一对一正式课</div>
        </el-form-item>
        <el-form-item label="请假原因">
          <el-input v-model="suspensionForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="可选，简单说明原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="showSuspensionDialog = false">取消</el-button>
          <el-button type="primary" :disabled="!dateRangeSelected" @click="submitTeacherSuspension">提交</el-button>
        </div>
      </template>
    </el-dialog>



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
      :all-schedules="rescheduleAllSchedules"
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


            <div class="label">学科</div>
            <div class="value">{{ selectedSchedule.subjectName || '-' }}</div>
          </div>
          <div class="detail-item">
            <div class="label">时长</div>
            <div class="value">{{ getDetailDurationMinutes(selectedSchedule) }} 分钟</div>
          </div>

          <div class="detail-item">
            <div class="label">状态</div>
            <div class="value">
              <el-tag :type="getScheduleStatusTag(selectedSchedule.status)" size="small">{{ getScheduleStatusText(selectedSchedule.status) }}</el-tag>
            </div>
          </div>
        </div>

        <el-alert type="info" :closable="false" class="hint">
          提示：如需调整本次课程，请使用“申请调课”。
        </el-alert>
      </div>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="showScheduleDetailModal = false">关闭</el-button>
          <el-button v-if="selectedSchedule && !selectedSchedule.isTrial" type="warning" @click="openRescheduleModal(selectedSchedule)">
            <el-icon><Refresh /></el-icon>
            申请调课
          </el-button>
          <el-button v-if="selectedSchedule && !selectedSchedule.isTrial" type="danger" @click="handleSuspend(selectedSchedule)">请假</el-button>
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
        <div style="color:#999;font-size:12px;margin-top:4px;">最近10条</div>
        <el-table :data="hourDetails.adminAdjustList || []" size="small" stripe style="width: 100%; margin-top: 6px">
          <el-table-column prop="createdAt" label="时间" width="180">
            <template #default="{ row }">{{ (row.createdAt || '').toString().replace('T',' ').slice(0,19) }}</template>
          </el-table-column>
          <el-table-column prop="hours" label="调整课时" width="120">
            <template #default="{ row }">
              <span :class="(Number(row.hours || 0) >= 0) ? 'positive' : 'negative'">{{ Number(row.hours || 0) >= 0 ? '+' : '' }}{{ row.hours }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="备注" min-width="200" show-overflow-tooltip />
        </el-table>

        <el-divider />
        <div class="detail-item total">
          <div class="detail-label">本月累计课时：</div>
          <div class="detail-value total-value">{{ hourDetails.totalHours }}小时</div>
        </div>

        <el-divider />
        <div class="course-salary-section">
          <div class="section-title">薪资明细（本月）</div>
          <el-table :data="hourDetails.courseSummaries || []" size="small" stripe style="width: 100%">
            <el-table-column prop="title" label="课程" min-width="200" show-overflow-tooltip />
            <el-table-column prop="teacherHourlyRate" label="时薪" width="120">
              <template #default="{ row }">{{ row.teacherHourlyRate || 0 }} M豆/小时</template>
            </el-table-column>
            <el-table-column prop="currentHours" label="本月课时" width="120">
              <template #default="{ row }">{{ row.currentHours || 0 }} 小时</template>
            </el-table-column>
            <el-table-column prop="currentAmount" label="本月薪资" width="140">
              <template #default="{ row }">{{ row.currentAmount || 0 }} M豆</template>
            </el-table-column>
          </el-table>

          <div class="detail-item total">
            <div class="detail-label">本月总薪资（估算）：</div>
            <div class="detail-value total-value">{{ hourDetails.totalCurrentAmount || 0 }} M豆</div>
          </div>
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
  border-radius: 6px;
}

.calendar-cell.has-schedule { background: #f7fbff; border: 1px solid #ecf5ff; }
.calendar-cell.no-schedule { background: #fff; border: 1px solid transparent; }

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

/* 表格横向滚动兜底，避免小屏列拥挤溢出 */
.table-wrap { width: 100%; overflow-x: auto; }
.table-wrap :deep(table) { min-width: 900px; }


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

/* 小屏弹窗/工具栏细节优化（独立块） */
@media (max-width: 768px) {
  :deep(.el-dialog){ width:100vw !important; max-width:100vw !important; margin:0 !important; }
  :deep(.el-dialog__body){ padding:12px; }
  :deep(.el-dialog .el-form .el-form-item__label){ float:none; display:block; padding-bottom:4px; }
  :deep(.el-dialog .el-form .el-form-item__content){ margin-left:0 !important; }
  .calendar-header, .upcoming-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  .calendar-actions, .upcoming-actions { width:100%; display:flex; flex-wrap:wrap; gap:8px }
  .calendar-actions :deep(.el-date-editor), .calendar-actions :deep(.el-button){ flex:1; min-width: 160px }
  .upcoming-actions :deep(.el-select){ width:100% !important }
}

    padding: 12px;
  }

  .user-info {
    padding: 10px 12px;
  }

  .dashboard-stats {
    grid-template-columns: 1fr;
    gap: 8px;
  }
.day-dialog-list { display:flex; flex-direction:column; gap:8px; }
.day-dialog-row { display:flex; flex-direction:column; }
.day-dialog-row .row-title { font-weight:600; color:#303133; }
.day-dialog-row .row-title.trial { color:#303133; }
.day-dialog-row .row-time { color:#606266; font-size:12px; }


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

/* 当月课表：在单元格内展示当天课程时间段 */
.calendar-cell .day-entries { margin-top: 4px; display: flex; flex-direction: column; gap: 4px; }
.calendar-cell .entry { display: flex; flex-direction: column; align-items: flex-start; gap: 2px; font-size: 11px; background: #f7f9fb; border-radius: 4px; padding: 2px 4px; white-space: nowrap; overflow: hidden; }
.calendar-cell .entry .title { color: #303133; font-weight: 500; max-width: 100%; overflow: hidden; text-overflow: ellipsis; }
.calendar-cell .entry .title.trial { color: #303133; }
.calendar-cell .entry .time { color: #606266; }
.calendar-cell .more { font-size: 11px; color: #909399; }

:deep(.el-calendar-table td) { vertical-align: top; }
:deep(.el-calendar-table .el-calendar-day) { height: auto !important; padding: 6px !important; }
.calendar-scroll { max-height: 72vh; overflow-y: auto; }
@media (max-width: 768px) { .calendar-scroll { max-height: 65vh; } }
.calendar-cell.has-schedule { cursor: pointer; }
</style>
