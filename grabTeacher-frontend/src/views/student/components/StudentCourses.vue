<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-unused-vars */
import { ref, computed, onMounted, defineAsyncComponent, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Timer, Refresh, VideoCamera, InfoFilled, Clock, Collection, Reading, Star } from '@element-plus/icons-vue'
import { bookingAPI, rescheduleAPI, teacherAPI, studentAPI, apiRequest, suspensionAPI, enrollmentAPI } from '../../../utils/api'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../../stores/user'

const RescheduleModal = defineAsyncComponent(() => import('../../../components/RescheduleModal.vue'))
const userStore = useUserStore()


// 课程安排接口（基于后端ScheduleVO）
interface CourseSchedule {
  id: number;
  teacherId: number;
  teacherName: string;
  studentId: number;
  studentName: string;
  courseId: number;
  courseTitle?: string;
  courseName?: string;
  subjectName: string;
  scheduledDate: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  totalTimes: number;
  status: 'scheduled' | 'progressing' | 'completed' | 'cancelled' | 'rescheduled';
  teacherNotes?: string;
  studentFeedback?: string;
  createdAt: string;
  bookingRequestId?: number;
  enrollmentId?: number;
  bookingSource?: string;
  isTrial?: boolean; // 后端统一字段
  trial?: boolean;  // 兼容旧字段（少量旧接口/历史数据）
  sessionNumber?: number;
  courseType?: string;
}

// 聚合后的课程信息（用于显示）
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
  remainingLessons?: number;
  weeklySchedule?: string[];
  image: string;
  description: string;
  status: 'active' | 'completed' | 'upcoming';
  schedules?: CourseSchedule[]; // 关联的课程安排
  hasEvaluation?: boolean; // 是否已评价
  durationMinutes?: number; // 课程时长（分钟）
  bookingRequestId?: number; // 占位课程映射时保存预约ID，用于查询报名ID
}

// 生成 1v1 课程显示标题：姓名+科目+年级+一对一课程（若subject已含年级则不重复）
const buildOneToOneTitle = (s: CourseSchedule): string | null => {
  const type = (s.courseType || '').toLowerCase()
  const isOneToOne = type.includes('one') || type.includes('1v1') || type.includes('one_to_one') || type.includes('one-on-one')
  if (!isOneToOne) return null
  const studentName = (s.studentName || '').trim()
  let subjectWithGrade = (s.subjectName || '').trim()
  const grade = (s as any)?.grade ? String((s as any).grade).trim() : ''
  if (grade && subjectWithGrade && !subjectWithGrade.includes(grade)) {
    subjectWithGrade = `${subjectWithGrade}${grade}`
  }
  if (studentName && subjectWithGrade) {
    return `${studentName}${subjectWithGrade}一对一课程`
  }
  return null
}


// 调课相关接口

// 成绩数据接口
interface GradeData {
  date: string;
  score: number;
  comment: string;
  lesson: string;
}

interface ScheduleGrade {
  id: number;
  scheduleId: number;
  studentId: number;
  score: number;
  teacherComment: string;
  gradedAt: string;
  createdAt: string;
  updatedAt: string;
  // 添加时间字段用于排序
  scheduledDate: string;
  startTime: string;
  endTime: string;
}

// 响应式数据
const courses = ref<Course[]>([])
const loading = ref(false)
const schedules = ref<CourseSchedule[]>([])

// 成绩图表相关
const gradeChartVisible = ref(false)
const currentCourseForGrade = ref<Course | null>(null)
const gradeData = ref<GradeData[]>([])
const gradeLoading = ref(false)

// 单节课成绩详情
const scheduleGradeVisible = ref(false)
const currentScheduleGrade = ref<{
  schedule: CourseSchedule;
  grade: ScheduleGrade;
  courseTitle: string;
  teacherName: string;
  subjectName: string;
} | null>(null)
const scheduleGrades = ref<Map<number, ScheduleGrade>>(new Map()) // 缓存课程安排的成绩

// 课程评价相关
const evaluationVisible = ref(false)
const currentCourseForEvaluation = ref<Course | null>(null)
const evaluationForm = ref({
  rating: 5,
  comment: ''
})
const evaluationLoading = ref(false)

// 课程调课状态映射
const courseRescheduleStatus = ref<Map<number, string>>(new Map())
// 课程停课状态映射
const courseSuspensionStatus = ref<Map<number, string>>(new Map())


const router = useRouter()

// 获取学生课程安排
const loadStudentSchedules = async () => {
  try {
    loading.value = true
    const result = await bookingAPI.getAllStudentSchedules()
    if (result.success) {
      if (result.data && result.data.length > 0) {
        schedules.value = result.data
        courses.value = convertSchedulesToCourses(result.data)
        // 简化首屏请求：只加载课表
      } else {
        schedules.value = []
        courses.value = []
      }
    } else {
      ElMessage.error(result.message || '获取课程安排失败')
      schedules.value = []
      courses.value = []
    }
  } catch (error) {
    ElMessage.error('获取课程安排失败，请稍后重试')
    schedules.value = []
    courses.value = []
  } finally {
    loading.value = false
  }
}

// 直接使用后端 V2 聚合接口（单一真相源）
const loadStudentCoursesV2 = async () => {
  try {
    loading.value = true
    const result = await bookingAPI.getStudentCoursesV2()
    if (!result.success) {
      ElMessage.error(result.message || '获取课程失败')
      courses.value = []
      schedules.value = []
      return
    }
    const cards = Array.isArray(result.data) ? result.data : []

    // 将 V2 卡片转换为前端 Course 结构，尽量复用现有渲染逻辑
    const mappedCourses: Course[] = cards.map((card: any) => {
      const total = Number(card.totalLessons || 0)
      const completed = Number(card.completedLessons || 0)
      const progress = total > 0 ? Math.round((completed / total) * 100) : 0

      const fmtTime = (t?: string) => t ? (t.length >= 5 ? t.slice(0, 5) : t) : ''
      const next = card.nextSchedule
        ? `${card.nextSchedule.scheduledDate} ${fmtTime(String(card.nextSchedule.startTime))}-${fmtTime(String(card.nextSchedule.endTime))}`
        : '暂无安排'

      const status: 'active' | 'completed' | 'upcoming' = progress === 100 ? 'completed' : 'active'

      // 映射节次
      const mappedSchedules: CourseSchedule[] = (card.schedules || []).map((s: any) => ({
        id: Number(s.id),
        teacherId: Number(s.teacherId || 0),
        teacherName: String(card.teacherName || ''),
        studentId: Number(s.studentId || 0),
        studentName: String(userStore.user?.realName || ''),
        courseId: Number(s.courseId || card.courseId || 0),
        courseTitle: String(card.title || ''),
        courseName: String(card.title || ''),
        subjectName: String(card.subjectName || ''),
        scheduledDate: String(s.scheduledDate || ''),
        startTime: fmtTime(String(s.startTime || '')),
        endTime: fmtTime(String(s.endTime || '')),
        durationMinutes: Number(s.durationMinutes || card.durationMinutes || 0) || undefined,
        totalTimes: Number(card.totalLessons || 0),
        status: String(s.status || 'scheduled') as any,
        teacherNotes: '',
        studentFeedback: '',
        createdAt: '',
        bookingRequestId: card.bookingRequestId ? Number(card.bookingRequestId) : (s.bookingRequestId ? Number(s.bookingRequestId) : undefined),
        enrollmentId: card.enrollmentId ? Number(card.enrollmentId) : (s.enrollmentId ? Number(s.enrollmentId) : undefined),
        isTrial: Boolean(s.isTrial ?? s.trial ?? card.isTrial ?? card.trial ?? false),
        trial: Boolean(s.isTrial ?? s.trial ?? card.isTrial ?? card.trial ?? false),
        sessionNumber: s.sessionNumber ? Number(s.sessionNumber) : undefined,
        courseType: String(card.courseType || '')
      }))

      // 课程首尾日期
      const startDate = String(card.startDate || (mappedSchedules[0]?.scheduledDate || ''))
      const endDate = String(card.endDate || (mappedSchedules[mappedSchedules.length - 1]?.scheduledDate || ''))

      return {
        id: Number(card.enrollmentId || card.bookingRequestId || card.courseId || Date.now()),
        title: String(card.title || '未指定课程'),
        teacher: String(card.teacherName || '未知教师'),
        teacherAvatar: '@/assets/pictures/teacherBoy1.jpeg',
        subject: String(card.subjectName || '未知科目'),
        schedule: card.nextSchedule ? `${fmtTime(String(card.nextSchedule.startTime))}-${fmtTime(String(card.nextSchedule.endTime))}` : (mappedSchedules[0] ? `${fmtTime(mappedSchedules[0].startTime)}-${fmtTime(mappedSchedules[0].endTime)}` : '时间待定'),
        progress,
        nextClass: next,
        startDate,
        endDate,
        totalLessons: total,
        completedLessons: completed,
        remainingLessons: Math.max(0, total - completed),
        weeklySchedule: [],
        image: getSubjectImage(String(card.subjectName || '')),
        description: `${String(card.title || '')} - ${String(card.subjectName || '')}课程`,
        status,
        schedules: mappedSchedules,
        hasEvaluation: false,
        durationMinutes: Number(card.durationMinutes || 0) || undefined,
        bookingRequestId: card.bookingRequestId ? Number(card.bookingRequestId) : undefined
      }
    })

    courses.value = mappedCourses
    // 扁平化 schedules，供部分功能（如调课回退匹配、成绩加载）使用
    schedules.value = mappedCourses.flatMap(c => c.schedules || [])
  } catch (e) {
    console.error('获取课程失败:', e)
    ElMessage.error('获取课程失败，请稍后重试')
    courses.value = []
    schedules.value = []
  } finally {
    loading.value = false
  }
}


// 将课程安排转换为课程列表
const convertSchedulesToCourses = (scheduleList: CourseSchedule[]): Course[] => {
  console.log('开始转换课程安排数据，总数:', scheduleList.length)

  if (!scheduleList || scheduleList.length === 0) {
    console.log('没有课程安排数据')
    return []
  }

  // 分组：优先使用 enrollmentId（新表），回退到 bookingRequestId（兼容旧数据）
  const courseGroups = new Map<number, CourseSchedule[]>()

  scheduleList.forEach((schedule, index) => {
    // 确保有有效的分组键
    const groupKey = schedule.enrollmentId || schedule.bookingRequestId
    if (!groupKey) {
      console.warn(`课程安排 ${index} 缺少分组键:`, schedule)
      return
    }

    console.log(`处理课程安排 ${index}:`, {
      scheduleId: schedule.id,
      enrollmentId: schedule.enrollmentId,
      bookingRequestId: schedule.bookingRequestId,
      groupKey: groupKey,
      courseTitle: schedule.courseTitle,
      teacherName: schedule.teacherName,
      subjectName: schedule.subjectName
    })

    if (!courseGroups.has(groupKey)) {
      courseGroups.set(groupKey, [])
    }
    courseGroups.get(groupKey)!.push(schedule)
  })

  console.log('分组后的课程组数量:', courseGroups.size)

  // 转换为Course对象
  const courseList: Course[] = []

  courseGroups.forEach((scheduleGroup, groupKey) => {
    const firstSchedule = scheduleGroup[0]

    if (!firstSchedule) {
      console.warn('课程组为空:', groupKey)
      return
    }

    // 过滤掉已取消的节次（仅用于已完成计数和“下次课”）
    const effectiveSchedules = scheduleGroup.filter(s => s.status !== 'cancelled')
    const completedCount = effectiveSchedules.filter(s => s.status === 'completed').length

    // 计算总课时：优先使用 enrollment/预约规划的总课时(totalTimes)
    // 其次使用课节编号(sessionNumber)的最大值；若编号异常（一直为1等），则退化为计划条目数
    const maxTotalTimes = Math.max(0, ...scheduleGroup.map(s => Number(s.totalTimes || 0)))
    const maxSession = Math.max(0, ...scheduleGroup.map(s => Number(s.sessionNumber || 0)))
    // 计划条目数包含已取消的节次，用于更接近真实规划
    const plannedEntryCount = scheduleGroup.length
    let totalCount = 0
    if (isFinite(maxTotalTimes) && maxTotalTimes > 0) {
      totalCount = maxTotalTimes
    } else {
      totalCount = Math.max(maxSession, plannedEntryCount, effectiveSchedules.length)
    }
    const progress = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0

    // 找到下一次课程
    const upcomingSchedules = scheduleGroup
      .filter(s => {
        if (s.status === 'cancelled') return false
        const startStr = (s.startTime && s.startTime.length === 5) ? `${s.startTime}:00` : (s.startTime || '00:00:00')
        const start = new Date(`${s.scheduledDate}T${startStr}`)
        return start.getTime() > Date.now()
      })
      .sort((a, b) => {
        const aStart = new Date(`${a.scheduledDate}T${(a.startTime && a.startTime.length === 5) ? `${a.startTime}:00` : (a.startTime || '00:00:00')}`).getTime()
        const bStart = new Date(`${b.scheduledDate}T${(b.startTime && b.startTime.length === 5) ? `${b.startTime}:00` : (b.startTime || '00:00:00')}`).getTime()
        return aStart - bStart
      })

    const nextSchedule = upcomingSchedules[0]
    const fmtTime = (t?: string) => t ? (t.length === 5 ? t : t.slice(0, 5)) : ''
    const nextClass = nextSchedule
      ? `${nextSchedule.scheduledDate} ${fmtTime(nextSchedule.startTime)}-${fmtTime(nextSchedule.endTime)}`
      : '暂无安排'

    // 确定课程状态
    let status: 'active' | 'completed' | 'upcoming' = 'active'
    if (progress === 100) {
      status = 'completed'
    } else if (completedCount === 0 && scheduleGroup.every(s => new Date(s.scheduledDate) > new Date())) {
      // 只有当没有任何课程已完成且所有课程都在未来时，才设置为 upcoming
      status = 'upcoming'
    }

    // 确保课程标题不为空；1v1 课程按“姓名+科目(+年级)+一对一课程”生成
    const rawTitle = firstSchedule.courseTitle || firstSchedule.courseName || '未指定课程'
    const generatedTitle = buildOneToOneTitle(firstSchedule)
    const courseTitle = generatedTitle || rawTitle
    const teacherName = firstSchedule.teacherName || '未知教师'
    const subjectName = firstSchedule.subjectName || '未知科目'

    console.log(`创建课程对象:`, {
      id: groupKey,
      title: courseTitle,
      teacher: teacherName,
      subject: subjectName,
      totalLessons: totalCount,
      completedLessons: completedCount,
      progress: progress,
      status: status
    })

    courseList.push({
      id: groupKey,
      title: courseTitle,
      teacher: teacherName,
      teacherAvatar: '@/assets/pictures/teacherBoy1.jpeg', // 默认头像
      subject: subjectName,
      schedule: nextSchedule ? `${fmtTime(nextSchedule.startTime)}-${fmtTime(nextSchedule.endTime)}` : generateScheduleText(scheduleGroup),
      progress,
      nextClass,
      startDate: scheduleGroup[0]?.scheduledDate || '',
      endDate: scheduleGroup[scheduleGroup.length - 1]?.scheduledDate || '',
      totalLessons: totalCount,
      completedLessons: completedCount,
      remainingLessons: totalCount - completedCount,
      weeklySchedule: [],
      image: getSubjectImage(subjectName),
      description: `${courseTitle} - ${subjectName}课程`,
      status,
      schedules: scheduleGroup,
      hasEvaluation: false, // 默认未评价，后续会通过API检查
      durationMinutes: firstSchedule?.durationMinutes // 从数据库获取真实时长
    })
  })

  console.log('转换后的课程列表:', courseList)
  return courseList
}

// 生成课程安排文本
const generateScheduleText = (schedules: CourseSchedule[]): string => {
  if (schedules.length === 0) return '暂无安排'

  // 简单处理：显示第一个安排的时间
  const first = schedules[0]
  if (!first || !first.startTime || !first.endTime) {
    return '时间待定'
  }

  return `${first.startTime}-${first.endTime}`
}
// 工具：按周几聚合的时间段，限显展示（最多3个分组）
const weekdayName = (i: number) => ['','周一','周二','周三','周四','周五','周六','周日'][i] || `周${i}`
const formatCalendarWeekdaySlotsLimited = (m: Record<string | number, string[]>, limit = 3): string => {
  const order = [1, 2, 3, 4, 5, 6, 7]
  const groups: string[] = []
  for (const i of order) {
    const t = (m as any)[i] || (m as any)[String(i)]
    if (t?.length) groups.push(`${weekdayName(i)}：${t.join('、')}`)
  }
  return groups.length <= limit
    ? groups.join('；')
    : groups.slice(0, limit).join('；') + ` 等${groups.length - limit}个`
}


// 根据科目获取图片
const getSubjectImage = (subject: string): string => {
  const imageMap: Record<string, string> = {
    '数学': '@/assets/pictures/math1.jpeg',
    '英语': '@/assets/pictures/english1.jpeg',
    '物理': '@/assets/pictures/physics1.jpeg',
    '化学': '@/assets/pictures/chemistry1.jpeg',
    '生物': '@/assets/pictures/biology1.jpeg'
  }
  return imageMap[subject] || '@/assets/pictures/math1.jpeg'
}

// 组件挂载时加载数据


onMounted(() => {
  loadStudentCoursesV2()
  // 延迟并行加载非关键数据，避免首屏并发四个请求
  setTimeout(async () => {
    try {
      await Promise.all([
        loadRescheduleStatus(),
        loadSuspensionStatus(),

        loadCourseEvaluationStatus()
      ])
    } catch (e) {
      // 非关键数据失败不影响主流程
      console.warn('附加数据加载失败', e)
    }
  }, 0)
})

// 课程过滤状态
const activeTab = ref('active')

// 预约相关接口
interface BookingRequest {
  id: number


  teacherName: string
  courseTitle?: string
  subjectName?: string
  grade?: string
  courseType?: string
  studentName?: string
  courseDurationMinutes?: number
  bookingType: 'single' | 'recurring' | 'calendar'
  // single
  requestedDate?: string
  requestedStartTime?: string
  requestedEndTime?: string
  // recurring（历史兼容）
  recurringWeekdays?: number[]
  recurringTimeSlots?: string[]
  startDate?: string
  endDate?: string
  // calendar
  calendarStartDate?: string
  calendarEndDate?: string
  calendarTimeSlots?: string[]
  calendarWeekdayTimeSlots?: Record<number | string, string[]>
  calendarSessionsCount?: number
  totalTimes?: number
  studentRequirements?: string
  teacherReply?: string
  status: 'pending' | 'approved' | 'rejected' | 'cancelled'
  isTrial?: boolean
  trialDurationMinutes?: number
  createdAt: string
}

// 预约相关数据
const bookings = ref<BookingRequest[]>([])
const bookingLoading = ref(false)
const statusFilter = ref('')

// 分页数据
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 弹窗相关
const showDetailModal = ref(false)
const selectedBooking = ref<BookingRequest | null>(null)

// 统计信息
const bookingStats = computed(() => {
  const total = bookings.value.length
  const pending = bookings.value.filter(b => b.status === 'pending').length
  const approved = bookings.value.filter(b => b.status === 'approved').length
  const trial = bookings.value.filter(b => b.isTrial).length

  return { total, pending, approved, trial }
})

// 根据标签筛选课程（V2：直接使用后端聚合结果，不再合并占位课程）
const filteredCourses = computed(() => {
  if (activeTab.value === 'suspended') return []
  if (activeTab.value === 'pending' || activeTab.value === 'approved') return []
  if (activeTab.value === 'active') return courses.value.filter(course => course.status === 'active')
  return courses.value.filter(course => course.status === activeTab.value)
})

// 根据标签筛选预约
const filteredBookings = computed(() => {
  if (activeTab.value === 'pending') {
    return bookings.value.filter(b => b.status === 'pending')
  } else if (activeTab.value === 'approved') {
    return bookings.value.filter(b => b.status === 'approved')
  }
  return []
})
// 懒加载预约列表：仅在切换到“待审核/已审核”标签时请求，避免与课程接口并发
const loadedBookingStatuses = ref<Set<string>>(new Set())
watch(activeTab, async (val: string) => {
  if (val === 'pending' || val === 'approved') {
    if (!loadedBookingStatuses.value.has(val)) {
      statusFilter.value = val
      await loadBookings()
      loadedBookingStatuses.value.add(val)
    }
  }
})


// 已审核且未开始的预约（用于“进行中”标签展示）
const isBookingNotStartedYet = (b: BookingRequest): boolean => {
  try {
    const now = new Date()
    if (b.bookingType === 'single' && b.requestedDate && b.requestedStartTime) {
      const startStr = (b.requestedStartTime.length === 5) ? `${b.requestedStartTime}:00` : b.requestedStartTime
      const start = new Date(`${b.requestedDate}T${startStr}+08:00`)
      return start.getTime() > now.getTime()
    }
    if (b.bookingType === 'recurring' && b.startDate) {
      const start = new Date(`${b.startDate}T00:00:00+08:00`)
      return start.getTime() > now.getTime()
    }
    if (b.bookingType === 'calendar' && b.calendarStartDate) {
      const start = new Date(`${b.calendarStartDate}T00:00:00+08:00`)
      return start.getTime() > now.getTime()
    }
    return false
  } catch {
    return false
  }
}

const approvedUpcomingBookingsInActive = computed(() =>
  bookings.value.filter(b => b.status === 'approved' && isBookingNotStartedYet(b))
)

// 工具：解析时间段字符串 "HH:mm-HH:mm"
const parseTimeRange = (timeRange?: string): { start: string; end: string } | null => {
  if (!timeRange || !timeRange.includes('-')) return null
  const [start, end] = timeRange.split('-')
  const norm = (s: string) => (s.length === 5 ? `${s}:00` : s)
  return { start: norm(start), end: norm(end) }
}

// 生成“已审核未开始”的计划课时（占位节次），用于前端展示与选择
const generatePlannedSchedulesFromBooking = (b: BookingRequest): CourseSchedule[] => {
  const result: CourseSchedule[] = []
  const total = Number(b.calendarSessionsCount || b.totalTimes || 1)
  const subject = (b.subjectName || '').trim()
  const duration = Number(b.courseDurationMinutes || (b.isTrial ? b.trialDurationMinutes : 120) || 120)
  const baseId = Number(b.id || 0)

  const pushItem = (date: string, timeRange?: string, idx?: number) => {
    const t = parseTimeRange(timeRange || '')
    const startTime = t?.start ? t.start.slice(0, 5) : ''
    const endTime = t?.end ? t.end.slice(0, 5) : ''
    result.push({
      id: -1000000 - baseId * 1000 - (idx ?? result.length),
      teacherId: 0,
      teacherName: b.teacherName || '未知教师',
      studentId: 0,
      studentName: b.studentName || (userStore.user?.realName || ''),
      courseId: 0,
      courseTitle: b.courseTitle,
      courseName: b.courseTitle,
      subjectName: subject || '未知科目',
      scheduledDate: date,
      startTime: startTime,
      endTime: endTime,
      durationMinutes: duration,
      totalTimes: total,
      status: 'scheduled',
      isTrial: Boolean(b.isTrial),
      trial: Boolean(b.isTrial),
      sessionNumber: (idx ?? result.length) + 1
    } as unknown as CourseSchedule)
  }

  const dateStr = (d: Date) => {
    const y = d.getFullYear(); const m = String(d.getMonth() + 1).padStart(2, '0'); const dd = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${dd}`
  }

  if (b.bookingType === 'single') {
    const date = b.requestedDate || ''
    const tr = (b.requestedStartTime && b.requestedEndTime) ? `${b.requestedStartTime}-${b.requestedEndTime}` : ''
    pushItem(date, tr, 0)
    return result
  }

  // 生成多节次（calendar/recurring）
  const weekdaySlots: Record<string, string[]> = {}
  if (b.bookingType === 'calendar') {
    if (b.calendarWeekdayTimeSlots) {
      Object.keys(b.calendarWeekdayTimeSlots).forEach(k => {
        const arr = (b.calendarWeekdayTimeSlots as any)[k] || []
        weekdaySlots[String(k)] = Array.isArray(arr) ? arr as string[] : []
      })
    } else if (b.calendarTimeSlots && b.calendarTimeSlots.length) {
      // 没有按周分布，则默认周一到周日都用同一批时段
      for (let i = 1; i <= 7; i++) weekdaySlots[String(i)] = [...b.calendarTimeSlots]
    }
  } else if (b.bookingType === 'recurring') {
    const days = b.recurringWeekdays || []
    const slots = b.recurringTimeSlots || []
    days.forEach(d => { weekdaySlots[String(d)] = [...slots] })
  }

  const startDateStr = b.calendarStartDate || b.startDate || ''
  if (!startDateStr) return result
  const cursor = new Date(`${startDateStr}T00:00:00`)

  while (result.length < total) {
    const wd = cursor.getDay() // 0=周日 ... 6=周六
    const weekday = wd === 0 ? 7 : wd
    const key = String(weekday)
    const slots = weekdaySlots[key]
    if (slots && slots.length) {
      for (const slot of slots) {
        if (result.length >= total) break
        pushItem(dateStr(cursor), slot)
      }
    }
    cursor.setDate(cursor.getDate() + 1)
  }
  return result
}

// 将“已审核未开始的预约”映射为占位课程，合并到“进行中”列表
const mapApprovedBookingToCourse = (b: BookingRequest): Course => {
  const subject = (b.subjectName || '未知科目').trim()
  const isSingle = b.bookingType === 'single'
  const isCalendar = b.bookingType === 'calendar'
  const timeText = isSingle
    ? ((b.requestedStartTime && b.requestedEndTime) ? `${b.requestedStartTime}-${b.requestedEndTime}` : '时间待定')
    : (isCalendar
        ? (b.calendarWeekdayTimeSlots ? formatCalendarWeekdaySlotsLimited(b.calendarWeekdayTimeSlots, 3) : (b.calendarTimeSlots?.join('、') || '时间待定'))
        : formatRecurringTime(b.recurringWeekdays || [], b.recurringTimeSlots || [])
      )
  const nextText = isSingle
    ? ((b.requestedDate && b.requestedStartTime && b.requestedEndTime) ? `${b.requestedDate} ${b.requestedStartTime}-${b.requestedEndTime}` : (b.requestedDate || '待定'))
    : (isCalendar
        ? (b.calendarStartDate ? `${b.calendarStartDate} 起` : '待定')
        : (b.startDate ? `${b.startDate} 起` : '待定')
      )
  const totalTimes = Number(b.totalTimes || 1)

  // 1v1 预约命名：姓名+科目(+年级)+一对一课程
  const type = (b.courseType || '').toLowerCase()
  const likelyOneToOne = type ? (type.includes('one') || type.includes('1v1') || type.includes('one_to_one') || type.includes('one-on-one')) : true
  const studentName = (userStore.user?.realName || b.studentName || '').trim()
  const grade = (b.grade || '').trim()
  let subjectWithGrade = subject
  if (grade && !subjectWithGrade.includes(grade)) {
    subjectWithGrade = `${subjectWithGrade}${grade}`
  }
  const generatedTitle = likelyOneToOne && studentName && subjectWithGrade
    ? `${studentName}${subjectWithGrade}一对一课程`
    : (b.courseTitle || '自定义课程')

  const title = generatedTitle

  // 生成占位节次（显示所有课时）
  const plannedSchedules = generatePlannedSchedulesFromBooking(b)
  const nextFromPlanned = plannedSchedules[0]
    ? `${plannedSchedules[0].scheduledDate} ${plannedSchedules[0].startTime}-${plannedSchedules[0].endTime}`
    : nextText

  // 优先用可推导的计划节次数作为总课时，避免缺失 totalTimes 时退化为 1
  const derivedTotal = plannedSchedules.length > 0 ? plannedSchedules.length : totalTimes

  return {
    id: -1000000 - Number(b.id || 0),
    title,
    teacher: b.teacherName || '未知教师',
    teacherAvatar: '@/assets/pictures/teacherBoy1.jpeg',
    subject: subjectWithGrade,
    schedule: timeText || '时间待定',
    progress: 0,
    nextClass: nextFromPlanned,
    startDate: isSingle ? (b.requestedDate || '') : (b.startDate || b.calendarStartDate || ''),
    endDate: isSingle ? (b.requestedDate || '') : (b.endDate || ''),
    totalLessons: derivedTotal,
    completedLessons: 0,
    remainingLessons: derivedTotal,
    weeklySchedule: [],
    image: getSubjectImage(subjectWithGrade),
    description: `${title} - ${subjectWithGrade}课程`,
    status: 'active',
    schedules: plannedSchedules,
    hasEvaluation: false,
    durationMinutes: b.courseDurationMinutes || undefined,
    bookingRequestId: b.id
  }
}

const mergedActiveCourses = computed(() => {
  const base = courses.value.filter(c => c.status === 'active')
  // 仅在“该预约尚未生成真实课表”时，才加入占位课程，避免重复
  const realBookingIds = new Set<number>()
  base.forEach(c => {
    (c.schedules || []).forEach(s => {
      if (s.bookingRequestId) realBookingIds.add(Number(s.bookingRequestId))
    })
  })
  const placeholders = approvedUpcomingBookingsInActive.value
    .filter(b => !realBookingIds.has(Number(b.id)))
    .map(mapApprovedBookingToCourse)
  return [...base, ...placeholders]
})


// 当前查看的课程详情
const currentCourse = ref<Course | null>(null)
const detailDialogVisible = ref(false)

// 详情窗口：是否为试听课（任一节次为试听则视为试听课）
const isTrialCourse = computed(() => {
  const course = currentCourse.value
  if (!course) return false
  const list = course.schedules || []
  return list.some(s => Boolean(s.isTrial))
})

// 调课相关数据
const showRescheduleModal = ref(false)
const currentRescheduleCourse = ref<Course | null>(null)
const rescheduleType = ref<'single' | 'recurring'>('single')
const rescheduleForm = ref({
  originalDate: '',
  originalTime: '',
  newDate: '',
  newTime: '',
  selectedWeekdays: [] as number[],
  selectedTimeSlots: [] as string[],
  reason: '',
  type: 'single' as 'single' | 'recurring'
})

// 可用时间段 - 固定为6个系统上课时间
const availableTimeSlots = ref<string[]>([
  '08:00-10:00', '10:00-12:00', '13:00-15:00',
  '15:00-17:00', '17:00-19:00', '19:00-21:00'
])

// 根据课程时长动态生成可用时间段
const getAvailableTimeSlotsByDuration = (durationMinutes?: number): string[] => {
  if (!durationMinutes) {
    // 如果没有时长信息，默认返回2小时时间段
    return availableTimeSlots.value
  }

  if (durationMinutes === 90) {
    // 1.5小时：仅允许基础2小时段的中间90分钟（baseStart+15 ~ baseEnd-15）
    const baseTimeSlots = availableTimeSlots.value
    const centerSlots: string[] = []
    baseTimeSlots.forEach(baseSlot => {
      const [bs, be] = baseSlot.split('-')
      const [bsh, bsm] = bs.split(':').map(Number)
      const [beh, bem] = be.split(':').map(Number)
      const startMin = bsh * 60 + bsm + 15
      const endMin = beh * 60 + bem - 15
      const s = `${String(Math.floor(startMin / 60)).padStart(2,'0')}:${String(startMin % 60).padStart(2,'0')}`
      const e = `${String(Math.floor(endMin / 60)).padStart(2,'0')}:${String(endMin % 60).padStart(2,'0')}`
      centerSlots.push(`${s}-${e}`)
    })
    return centerSlots
  } else if (durationMinutes === 120) {
    // 2小时：使用固定的时间段，维持现状不变
    return availableTimeSlots.value
  }

  // 默认返回2小时时间段
  return availableTimeSlots.value
}

// 获取当前调课课程的时间段（根据课程时长动态生成）
const getCurrentRescheduleTimeSlots = computed(() => {
  if (!currentRescheduleCourse.value?.schedules || currentRescheduleCourse.value.schedules.length === 0) {
    return availableTimeSlots.value
  }

  // 获取第一个课程安排的时长信息
  const firstSchedule = currentRescheduleCourse.value.schedules[0]
  const durationMinutes = firstSchedule.durationMinutes

  return getAvailableTimeSlotsByDuration(durationMinutes)
})

// 教师可用时间段（从后端获取）
interface TeacherTimeSlot {
  weekday: number
  timeSlots: string[]
}

const teacherAvailableTimeSlots = ref<TeacherTimeSlot[]>([])

// 获取教师可用时间（周模板已移除，按日历选择）
const loadTeacherAvailableTime = async (_teacherId: number) => {
  console.info('[StudentCourses] 已切换按日历预约，周模板可用时间已移除')
  teacherAvailableTimeSlots.value = []
}

// 找到1.5小时时间段对应的基础2小时时间段（仅当该时间段是基础段的中间90分钟时返回）
const findBaseTimeSlotFor90Min = (timeSlot: string): string | null => {
  const [startTime, endTime] = timeSlot.split('-')
  const [sh, sm] = startTime.split(':').map(Number)
  const [eh, em] = endTime.split(':').map(Number)
  const sMin = sh * 60 + sm
  const eMin = eh * 60 + em

  const baseTimeSlots = availableTimeSlots.value
  for (const baseSlot of baseTimeSlots) {
    const [bs, be] = baseSlot.split('-')
    const [bsh, bsm] = bs.split(':').map(Number)
    const [beh, bem] = be.split(':').map(Number)
    const bsMin = bsh * 60 + bsm
    const beMin = beh * 60 + bem
    if (sMin === bsMin + 15 && eMin === beMin - 15) {
      return baseSlot
    }
  }
  return null
}

// 检查时间段是否可用（基于教师设置和预约逻辑）
const isTimeSlotAvailable = (weekday: number, timeSlot: string): boolean => {
  if (teacherAvailableTimeSlots.value.length === 0) {
    // 如果教师未设置可用时间，默认所有时间可用
    return true
  }

  // 查找对应星期几的时间段（现在统一使用1-7格式）
  const daySlot = teacherAvailableTimeSlots.value.find(slot => slot.weekday === weekday)
  if (!daySlot || !daySlot.timeSlots) {
    return false
  }

  // 获取当前调课课程的时长信息
  const currentDuration = currentRescheduleCourse.value?.schedules?.[0]?.durationMinutes

  // 如果当前时长是90分钟（1.5小时），需要映射到基础2小时时间段
  if (currentDuration === 90) {
    const baseTimeSlot = findBaseTimeSlotFor90Min(timeSlot)
    if (!baseTimeSlot) {
      return false
    }
    // 使用基础时间段来判断可用性
    return daySlot.timeSlots.includes(baseTimeSlot)
  }

  // 2小时课程，直接查找
  return daySlot.timeSlots.includes(timeSlot)
}


// ================= 停课申请（日期范围选择） =================
const suspensionDialogVisible = ref(false)
const currentSuspensionCourse = ref<Course | null>(null)
const currentSuspensionEnrollmentId = ref<number | null>(null)
const suspensionForm = ref({
  dateRange: [] as [Date, Date] | [],
  reason: '学生申请停课'
})

const initDefaultSuspensionRange = () => {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() + 7)
  const end = new Date(start)
  end.setDate(end.getDate() + 13)
  suspensionForm.value.dateRange = [start, end]
}

const getCourseDurationMinutes = (course?: Course | null): number => {
  // 优先使用课程直接设置的时长
  if (course?.durationMinutes && Number.isFinite(Number(course.durationMinutes)) && Number(course.durationMinutes) > 0) {
    return Number(course.durationMinutes)
  }
  // 回退到从课程安排中获取时长
  const dur = course?.schedules?.[0]?.durationMinutes
  return Number.isFinite(Number(dur)) && Number(dur) > 0 ? Number(dur) : 120
}




// 获取可选择的星期几（基于已选择的时间段）
const getAvailableWeekdays = computed(() => {
  if (rescheduleForm.value.selectedTimeSlots.length === 0) {
    return []
  }

  const availableWeekdays: number[] = []

  // 检查每个星期几
  for (const weekday of [1, 2, 3, 4, 5, 6, 7]) {
    // 检查该星期几是否有所有已选择的时间段可用
    const hasAllTimeSlots = rescheduleForm.value.selectedTimeSlots.every(timeSlot =>
      isTimeSlotAvailable(weekday, timeSlot)
    )

    if (hasAllTimeSlots) {
      availableWeekdays.push(weekday)
    }
  }

  return availableWeekdays
})

// 检查星期几是否可选择
const isWeekdaySelectable = (weekday: number): boolean => {
  return getAvailableWeekdays.value.includes(weekday)
}

// 获取下一节课（最近的未来课程）
// 由于后端已经按时间正序排序，直接找第一个符合条件的即可
const getNextSchedule = (schedules?: CourseSchedule[]): CourseSchedule | undefined => {
  if (!schedules) return undefined

  const nowTs = Date.now()
  return schedules.find(s => {
    if (s.status === 'cancelled') return false
    const startTime = (s.startTime && s.startTime.length === 5) ? `${s.startTime}:00` : (s.startTime || '00:00:00')
    const start = new Date(`${s.scheduledDate}T${startTime}`)
    return start.getTime() > nowTs
  })
}

const showCourseDetail = async (course: Course) => {
  currentCourse.value = course
  detailDialogVisible.value = true

  // 加载该课程所有课程安排的成绩
  if (course.schedules && course.schedules.length > 0) {
    await loadScheduleGrades(course.schedules)
  }
}

// 显示课程评价弹窗
const showCourseEvaluation = (course: Course) => {
  currentCourseForEvaluation.value = course
  // 重置评价表单
  evaluationForm.value = {
    rating: 5,
    comment: ''
  }
  evaluationVisible.value = true
}

// 提交课程评价
const submitCourseEvaluation = async () => {
  if (!currentCourseForEvaluation.value) return

  try {
    evaluationLoading.value = true

    // 获取课程信息
    const course = currentCourseForEvaluation.value
    const firstSchedule = course.schedules?.[0]

    if (!firstSchedule) {
      ElMessage.error('课程信息不完整，无法提交评价')
      return
    }

    // 验证ID的有效性
    if (!firstSchedule.teacherId || !firstSchedule.studentId || !firstSchedule.courseId ||
        isNaN(Number(firstSchedule.teacherId)) || isNaN(Number(firstSchedule.studentId)) || isNaN(Number(firstSchedule.courseId))) {
      ElMessage.error('课程信息不完整，ID无效，无法提交评价')
      return
    }

    // 构建评价数据
    const evaluationData = {
      teacherId: Number(firstSchedule.teacherId),
      studentId: Number(firstSchedule.studentId),
      courseId: Number(firstSchedule.courseId),
      teacherName: firstSchedule.teacherName,
      studentName: firstSchedule.studentName,
      courseName: firstSchedule.courseTitle,
      rating: evaluationForm.value.rating,
      studentComment: evaluationForm.value.comment
    }

    // 调用API创建评价
    const result = await studentAPI.createCourseEvaluation(evaluationData)

    if (result.success) {
      ElMessage.success('评价提交成功！')
      evaluationVisible.value = false
      // 更新当前课程的评价状态
      if (currentCourseForEvaluation.value) {
        currentCourseForEvaluation.value.hasEvaluation = true
      }
      // 刷新课程列表
      await loadStudentCoursesV2()
    } else {
      ElMessage.error(result.message || '评价提交失败')
    }
  } catch (error) {
    console.error('提交评价失败:', error)
    ElMessage.error('评价提交失败，请稍后重试')
  } finally {
    evaluationLoading.value = false
  }
}

// 显示调课弹窗
const showReschedule = async (course: Course) => {
  currentRescheduleCourse.value = course
  rescheduleType.value = 'single'
  // 
  // V2 
  // 
  if ((course.id <= 0 || (course.schedules || []).every(s => Number(s.id) <= 0)) && course.bookingRequestId) {
    const real = courses.value.find(c => Number(c.bookingRequestId) === Number(course.bookingRequestId)
      && Array.isArray(c.schedules) && c.schedules.some(s => Number(s.id) > 0))
    if (real) {
      currentRescheduleCourse.value = real
      course = real
    }
  }


  // 先取当前卡片的“下一节”
  let nextSchedule = getNextSchedule(course.schedules)

  // 如果下一节是占位（id<=0），尝试从全量真实课表中映射到真实节次
  if (!nextSchedule || !nextSchedule.id || Number(nextSchedule.id) <= 0) {
    const targetDate = nextSchedule?.scheduledDate || ''
    const targetStart = nextSchedule?.startTime || ''
    const targetEnd = nextSchedule?.endTime || ''
    const teacherId = nextSchedule?.teacherId || course.schedules?.[0]?.teacherId

    const realCandidates = schedules.value.filter(s => s.id && Number(s.id) > 0)

    // 先按“日期+时间段(+教师)”精确匹配
    const exact = realCandidates.find(s =>
      s.scheduledDate === targetDate &&
      s.startTime === targetStart &&
      s.endTime === targetEnd &&
      (!teacherId || s.teacherId === teacherId)
    )
    if (exact) {
      nextSchedule = exact as unknown as CourseSchedule
      // 确保传入弹窗的课程对象包含该真实节次，避免弹窗内仍选中占位节次
      if (Array.isArray(course.schedules)) {
        const exists = course.schedules.some(s => Number(s.id) === Number((exact as any).id))
        if (!exists) {
          course.schedules.push(exact as any)
        }
      }
    } else {
      // 回退：选择最近的未来节次（同教师优先）
      const future = realCandidates
        .filter(s => (!teacherId || s.teacherId === teacherId))
        .filter(s => {
          const startStr = (s.startTime && s.startTime.length === 5) ? `${s.startTime}:00` : (s.startTime || '00:00:00')
          const ts = new Date(`${s.scheduledDate}T${startStr}`).getTime()
          return ts > Date.now()
        })
        .sort((a, b) => {
          const aStr = (a.startTime && a.startTime.length === 5) ? `${a.startTime}:00` : (a.startTime || '00:00:00')
          const bStr = (b.startTime && b.startTime.length === 5) ? `${b.startTime}:00` : (b.startTime || '00:00:00')
          return new Date(`${a.scheduledDate}T${aStr}`).getTime() - new Date(`${b.scheduledDate}T${bStr}`).getTime()
        })[0]
      if (future) {
        nextSchedule = future as unknown as CourseSchedule
        if (Array.isArray(course.schedules)) {
          const exists = course.schedules.some(s => Number(s.id) === Number((future as any).id))
          if (!exists) {
            course.schedules.push(future as any)
          }
        }
      }
    }
  }

  let originalDate = ''
  let originalTime = ''

  if (nextSchedule && nextSchedule.id && Number(nextSchedule.id) > 0) {
    originalDate = nextSchedule.scheduledDate
    originalTime = `${nextSchedule.startTime}-${nextSchedule.endTime}`

    // 加载教师可用时间
    await loadTeacherAvailableTime(nextSchedule.teacherId)
  } else {
    ElMessage.warning('没有找到可调课的真实课表，请稍后刷新重试')
    return
  }

  // 重置表单并设置原定课程信息
  rescheduleForm.value = {
    originalDate,
    originalTime,
    newDate: '',
    newTime: '',
    selectedWeekdays: [],
    selectedTimeSlots: [],
    reason: '',
    type: 'single'
  }

  showRescheduleModal.value = true
}

// 切换调课类型
const switchRescheduleType = (type: 'single' | 'recurring') => {
  rescheduleType.value = type
  rescheduleForm.value.type = type

  // 保留原定课程信息，只清空新的安排
  if (type === 'single') {
    // 切换到单次调课时，保留原定课程信息，清空周期性调课的选择
    rescheduleForm.value.selectedWeekdays = []
    rescheduleForm.value.selectedTimeSlots = []
    rescheduleForm.value.newDate = ''
    rescheduleForm.value.newTime = ''

    // 重新设置原定课程信息（如果之前被清空了）
    if (!rescheduleForm.value.originalDate || !rescheduleForm.value.originalTime) {
      const nextSchedule = getNextSchedule(currentRescheduleCourse.value?.schedules)
      if (nextSchedule) {
        rescheduleForm.value.originalDate = nextSchedule.scheduledDate
        rescheduleForm.value.originalTime = `${nextSchedule.startTime}-${nextSchedule.endTime}`
      }
    }
  } else {
    // 切换到周期性调课时，清空单次调课的选择
    rescheduleForm.value.selectedWeekdays = []
    rescheduleForm.value.selectedTimeSlots = []
    rescheduleForm.value.newDate = ''
    rescheduleForm.value.newTime = ''
  }
}

// 判断是否为过去日期（禁止选择今天之前）
const isPastDate = (dateStr: string): boolean => {
  const target = new Date(dateStr)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return target < today
}

// 可选日期校验：当前仅禁止选择过去日期
const canReschedule = (dateStr: string): boolean => {
  return !isPastDate(dateStr)
}

// 避免 toISOString 引起的时区偏移（备用）
const canRescheduleDate = (date: Date): boolean => {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return d >= today
}

// 校验是否满足开课前4小时规则
const isAtLeastFourHoursBeforeOriginal = (): boolean => {
  if (!rescheduleForm.value.originalDate || !rescheduleForm.value.originalTime) return true
  try {
    const [startRaw] = rescheduleForm.value.originalTime.split('-')
    const start = startRaw.length === 5 ? `${startRaw}:00` : startRaw
    const originalStart = new Date(`${rescheduleForm.value.originalDate}T${start}`)
    if (isNaN(originalStart.getTime())) return true
    const nowPlus4 = new Date(Date.now() + 4 * 60 * 60 * 1000)
    return originalStart.getTime() > nowPlus4.getTime()
  } catch {
    return true
  }
}

// 时间冲突检查状态
const timeConflictChecking = ref(false)
const timeConflictResult = ref<{
  hasConflict: boolean;
  conflictType?: string;
  message?: string
} | null>(null)

// 检查调课时间冲突
const checkRescheduleTimeConflict = async () => {
  if (!rescheduleForm.value.newDate || !rescheduleForm.value.newTime) {
    timeConflictResult.value = null
    return
  }

  const nextSchedule = getNextSchedule(currentRescheduleCourse.value?.schedules)
  if (!nextSchedule) {
    timeConflictResult.value = { hasConflict: false, message: '无法获取课程信息' }
    return
  }

  try {
    timeConflictChecking.value = true

    // 若下一节课程ID无效（<=0），尝试从全量真实课表中映射
    if (!nextSchedule.id || Number(nextSchedule.id) <= 0) {
      const targetDate = nextSchedule.scheduledDate
      const targetStart = nextSchedule.startTime
      const targetEnd = nextSchedule.endTime
      const teacherId = nextSchedule.teacherId || currentRescheduleCourse.value?.schedules?.[0]?.teacherId

      const realCandidates = schedules.value.filter(s => s.id && Number(s.id) > 0)
      const exact = realCandidates.find(s =>
        s.scheduledDate === targetDate &&
        s.startTime === targetStart &&
        s.endTime === targetEnd &&
        (!teacherId || s.teacherId === teacherId)
      )
      if (exact) {
        // 用真实节次ID进行冲突检查
        nextSchedule.id = exact.id
      } else {
        timeConflictResult.value = { hasConflict: false, message: '未找到对应的真实课表，请稍后重试' }
        return
      }
    }

    // 解析时间段
    const [startTime, endTime] = rescheduleForm.value.newTime.split('-')

    const result = await rescheduleAPI.checkTimeConflict(
      nextSchedule.id,
      rescheduleForm.value.newDate,
      startTime,
      endTime
    )

    if (result.success) {
      timeConflictResult.value = {
        hasConflict: result.data.hasConflict,
        conflictType: result.data.conflictType,
        message: result.data.message
      }
    } else {
      timeConflictResult.value = { hasConflict: false, message: '检查失败，请重试' }
    }
  } catch (error) {
    console.error('检查时间冲突失败:', error)
    timeConflictResult.value = { hasConflict: false, message: '检查失败，请重试' }
  } finally {
    timeConflictChecking.value = false
  }
}

// 获取星期几的中文名称（统一使用后端格式：1-7）
const getWeekdayName = (weekday: number): string => {
  const names = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return names[weekday] || '未知'
}

// 课程详情中的课程安排（过滤掉已取消的节次，且在前端再次按日期+开始时间稳定排序，避免编号混乱）
const sortedSchedules = computed(() => {
  if (!currentCourse.value?.schedules) return []
  const list = currentCourse.value.schedules.filter(s => s.status !== 'cancelled')
  return list.slice().sort((a, b) => {
    const aStart = new Date(`${a.scheduledDate}T${(a.startTime && a.startTime.length === 5) ? `${a.startTime}:00` : (a.startTime || '00:00:00')}`).getTime()
    const bStart = new Date(`${b.scheduledDate}T${(b.startTime && b.startTime.length === 5) ? `${b.startTime}:00` : (b.startTime || '00:00:00')}`).getTime()
    return aStart - bStart
  })
})

// 为有效节次生成正确的编号（排除已取消的节次）
const getEffectiveSessionNumber = (schedule: CourseSchedule, index: number) => {
  if (schedule.sessionNumber && schedule.sessionNumber > 0) {
    return schedule.sessionNumber
  }
  // 如果没有后端编号，则基于有效节次的索引生成
  return index + 1
}

// 提交调课申请
const submitReschedule = async () => {
  if (!currentRescheduleCourse.value) return

  // 验证表单
  if (rescheduleType.value === 'single') {
    if (!rescheduleForm.value.originalDate || !rescheduleForm.value.originalTime ||
        !rescheduleForm.value.newDate || !rescheduleForm.value.newTime) {
      ElMessage.warning('请填写完整的调课信息')
      return
    }

    // 检查4小时限制（仅针对原定课程开始时间）
    if (!isAtLeastFourHoursBeforeOriginal()) {
      ElMessage.error('单次调课需在开课前4小时之外发起')
      return
    }
    // 新的上课日期不得早于今天
    if (isPastDate(rescheduleForm.value.newDate)) {
      ElMessage.error('新的上课日期不能早于今天')
      return
    }

    // 检查时间冲突
    if (timeConflictResult.value?.hasConflict) {
      ElMessage.error('所选时间与其他课程冲突，请选择其他时间')
      return
    }

    // 如果还没有检查过时间冲突，先检查
    if (!timeConflictResult.value) {
      await checkRescheduleTimeConflict()
      if (timeConflictResult.value?.hasConflict) {
        ElMessage.error('所选时间与其他课程冲突，请选择其他时间')
        return
      }
    }
  } else {
    if (rescheduleForm.value.selectedWeekdays.length === 0 ||
        rescheduleForm.value.selectedTimeSlots.length === 0) {
      ElMessage.warning('请选择新的每周上课时间')
      return
    }
  }

  if (!rescheduleForm.value.reason.trim()) {
    ElMessage.warning('请填写调课原因')
    return
  }

  try {
    // 获取最近的课程安排ID（用于调课申请）
    const nextSchedule = getNextSchedule(currentRescheduleCourse.value.schedules)

    if (!nextSchedule) {
      ElMessage.error('没有找到可调课的课程安排')
      return
    }

    // 构建调课申请数据
    const requestData: {
      scheduleId: number;
      requestType: 'reschedule';
      reason: string;
      urgencyLevel: 'low' | 'medium' | 'high';
      newDate?: string;
      newStartTime?: string;
      newEndTime?: string;
    } = {
      scheduleId: nextSchedule.id,
      requestType: 'reschedule',
      reason: rescheduleForm.value.reason,
      urgencyLevel: 'medium'
    }

    if (rescheduleType.value === 'single') {
      Object.assign(requestData, {
        newDate: rescheduleForm.value.newDate,
        newStartTime: rescheduleForm.value.newTime.split('-')[0],
        newEndTime: rescheduleForm.value.newTime.split('-')[1]
      })
    } else {
      Object.assign(requestData, {
        newRecurringWeekdays: rescheduleForm.value.selectedWeekdays,
        newRecurringTimeSlots: rescheduleForm.value.selectedTimeSlots
      })
    }

    // 调用API提交申请
    const result = await rescheduleAPI.createRequest(requestData)

    if (result.success) {
      ElMessage.success('调课申请已提交，等待老师确认')
      showRescheduleModal.value = false

      // 刷新课程列表以更新状态
      await loadStudentCoursesV2()
    } else {
      ElMessage.error(result.message || '提交调课申请失败')
    }
  } catch (error) {
    console.error('提交调课申请失败:', error)
    ElMessage.error('提交调课申请失败，请稍后重试')
  }
}

// 获取调课申请状态
const getRescheduleStatus = (courseId: number) => {
  return courseRescheduleStatus.value.get(courseId) || null
}

// 加载学生的调课申请状态
const loadRescheduleStatus = async () => {
  try {
    const result = await rescheduleAPI.getStudentRequests({
      page: 1,
      size: 100,
      status: 'pending'
    })

    if (result.success && result.data?.records) {
      courseRescheduleStatus.value.clear()
      result.data.records.forEach((request: { scheduleId: number }) => {
        // 根据scheduleId找到对应的课程
        const course = courses.value.find(c =>
          c.schedules?.some(s => s.id === request.scheduleId)
        )
        if (course) {
          courseRescheduleStatus.value.set(course.id, '调课申请中')
        }
      })
    }
  } catch (error) {
    console.error('加载调课申请状态失败:', error)
  }
}

// 获取停课申请状态
const getSuspensionStatus = (courseId: number) => {
  return courseSuspensionStatus.value.get(courseId) || null
}

// 加载学生的停课申请状态（pending）
const loadSuspensionStatus = async () => {
  try {
    const result = await suspensionAPI.getStudentRequests({ page: 1, size: 100, status: 'pending' })
    if (result.success && result.data?.records) {
      courseSuspensionStatus.value.clear()
      result.data.records.forEach((req: { enrollmentId: number }) => {
        const course = courses.value.find(c => {
          const eid = c.schedules?.[0]?.enrollmentId || Number(c.id)
          return Number(eid) === Number(req.enrollmentId)
        })
        if (course) {
          courseSuspensionStatus.value.set(course.id, '停课申请中')
        }
      })
    }
  } catch (e) {
    console.error('加载停课申请状态失败:', e)
  }
}



// 发起停课（试听课：直接确认并删除该节课；正式课：走停课申请流程）
const requestSuspension = async (course: Course) => {
  // 若下一节为试听课：无需选择日期，直接确认 -> 删除该节课并恢复试听次数
  const next = getNextSchedule(course.schedules)
  if (next && next.isTrial && next.id) {
    try {
      await ElMessageBox.confirm(
        '确认要停掉该试听课？此操作将删除该节课，并恢复您的试听次数。',
        '确认停课',
        { type: 'warning', confirmButtonText: '确认停课', cancelButtonText: '再想想' }
      )
      await bookingAPI.cancelTrialSchedule(Number(next.id))
      ElMessage.success('试听课已停课，试听次数已恢复')
      await loadStudentCoursesV2()
    } catch (e: any) {
      if (e !== 'cancel') {
        ElMessage.error(e?.message || '停课失败，请稍后重试')
      }
    }
    return
  }

  // 正式课：打开停课申请弹窗
  currentSuspensionEnrollmentId.value = null
  let eid: number | null = null
  const eidFromSchedule = course.schedules?.[0]?.enrollmentId
  if (eidFromSchedule && Number(eidFromSchedule) > 0) {
    eid = Number(eidFromSchedule)
  } else if (course.bookingRequestId) {
    try {
      const res = await enrollmentAPI.getByBookingRequestId(course.bookingRequestId)
      if (res?.success && res.data?.id) {
        eid = Number(res.data.id)
      }
    } catch (err) {
      console.warn('解析报名ID失败:', err)
    }
  }
  if (!eid) {
    ElMessage.info('正在准备报名信息，请先选择停课日期，提交时将自动解析。')
  }
  currentSuspensionEnrollmentId.value = eid || null
  currentSuspensionCourse.value = course
  initDefaultSuspensionRange()
  suspensionDialogVisible.value = true
}

const submitSuspension = async () => {
  try {
    if (!currentSuspensionCourse.value) return
    let eid: number | null = currentSuspensionEnrollmentId.value || null
    if (!eid) {
      const course = currentSuspensionCourse.value
      const eidFromSchedule = course.schedules?.[0]?.enrollmentId
      if (eidFromSchedule && Number(eidFromSchedule) > 0) {
        eid = Number(eidFromSchedule)

      } else if (course.bookingRequestId) {
        try {
          const res = await enrollmentAPI.getByBookingRequestId(course.bookingRequestId)
          if (res?.success && res.data?.id) {
            eid = Number(res.data.id)
          }
        } catch {}
      }
    }
    if (!eid) {
      ElMessage.error('无法获取报名信息，暂不能停课')
      return
    }
    const range = suspensionForm.value.dateRange
    if (!range || range.length !== 2) {
      ElMessage.warning('请选择停课起止日期')
      return
    }
    const [start, end] = range as [Date, Date]
    const today = new Date()
    today.setHours(0, 0, 0, 0)

    const minStart = new Date(today)
    minStart.setDate(minStart.getDate() + 7)
    const minEnd = new Date(start)
    minEnd.setDate(minEnd.getDate() + 13)

    if (start < minStart) {
      ElMessage.warning('停课开始日期需从一周后开始')
      return
    }
    if (end < minEnd) {
      ElMessage.warning('停课时长不可少于两周（14天）')
      return
    }

    const fmt = (d: Date) => {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const dd = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${dd}`
    }

    const payload = {
      enrollmentId: Number(eid),
      startDate: fmt(start),
      endDate: fmt(end),
      reason: suspensionForm.value.reason || '学生申请停课'
    }

    const result = await suspensionAPI.createRequest(payload)
    if (result.success) {
      ElMessage.success('停课申请已提交，等待管理员审批')
      suspensionDialogVisible.value = false
      await loadStudentCoursesV2()
    } else {
      ElMessage.error(result.message || '提交停课申请失败')
    }
  } catch (e: unknown) {
    console.error('提交停课申请失败:', e)
    const err = e as { response?: { message?: string }; message?: string }
    const msg = err?.response?.message || err?.message || '提交停课申请失败，请稍后重试'
    ElMessage.error(msg)
  }
}

// 加载课程评价状态
const loadCourseEvaluationStatus = async () => {
  try {
    // 为每个已完成的课程检查评价状态
    for (const course of courses.value) {
      if (course.status === 'completed' && course.schedules && course.schedules.length > 0) {
        const firstSchedule = course.schedules[0]

        // 验证ID的有效性
        if (!firstSchedule.studentId || !firstSchedule.courseId ||
            isNaN(Number(firstSchedule.studentId)) || isNaN(Number(firstSchedule.courseId))) {
          console.warn(`课程 ${course.id} 的ID无效: studentId=${firstSchedule.studentId}, courseId=${firstSchedule.courseId}`)
          course.hasEvaluation = false
          continue
        }

        try {
          const result = await studentAPI.checkCourseEvaluationStatus(
            Number(firstSchedule.studentId),
            Number(firstSchedule.courseId)
          )
          if (result.success) {
            course.hasEvaluation = result.data
          } else {
            course.hasEvaluation = false
          }
        } catch (error) {
          console.error(`检查课程 ${course.id} 评价状态失败:`, error)
          course.hasEvaluation = false
        }
      }
    }
  } catch (error) {
    console.error('加载课程评价状态失败:', error)
  }
}

// 格式化日期显示
const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// 格式化课程安排日期显示
const formatScheduleDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const weekday = weekdays[date.getDay()]

  return `${year}年${month}月${day}日 ${weekday}`
}

// 格式化日期时间
const formatDateTime = (dateTimeStr: string) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取课程状态对应的CSS类
const getScheduleStatusClass = (status: string): string => {
  switch (status) {
    case 'completed':
      return 'schedule-completed'
    case 'cancelled':
      return 'schedule-cancelled'
    case 'rescheduled':
      return 'schedule-rescheduled'
    case 'scheduled':
    default:
      return 'schedule-scheduled'
  }
}

// 获取课程状态对应的标签类型
const getScheduleTagType = (status: string): string => {
  switch (status) {
    case 'completed':
      return 'success'
    case 'cancelled':
      return 'danger'
    case 'rescheduled':
      return 'warning'
    case 'scheduled':
    default:
      return 'primary'
  }
}

// 获取课程状态显示文本
const getScheduleStatusText = (status: string): string => {
  switch (status) {
    case 'completed':
      return '已完成'
    case 'cancelled':
      return '已取消'
    case 'rescheduled':
      return '已调课'
    case 'scheduled':
      return '未开始'
    case 'progressing':
      return '进行中'
    default:
      return '未知'
  }
}







// 显示成绩图表
const showGradeChart = async (course: Course) => {
  currentCourseForGrade.value = course
  gradeChartVisible.value = true

  // 加载该课程的成绩数据
  await loadGradeData(course)
}

// 加载课程成绩数据
const loadGradeData = async (course: Course) => {
  if (!course.schedules || course.schedules.length === 0) {
    ElMessage.warning('该课程暂无成绩记录')
    return
  }

  gradeLoading.value = true
  try {
    const studentId = course.schedules[0].studentId // 从课程安排中获取学生ID
    const subjectName = course.subject || course.schedules[0].subjectName

    // 检查科目名称是否有效
    if (!subjectName || subjectName === 'null' || subjectName.trim() === '') {
      ElMessage.warning('课程科目信息缺失，无法查询成绩')
      gradeData.value = []
      return
    }

    // 使用项目的apiRequest方法，确保包含认证token
    const result = await apiRequest(`/api/lesson-grades/student/${studentId}/subject/${encodeURIComponent(subjectName)}/chart-data`)

    if (result.success && result.data) {
      // 按日期和时间排序后分配课节编号
      const sortedData = result.data.sort((a: ScheduleGrade, b: ScheduleGrade) => {
        const dateA = new Date(`${a.scheduledDate} ${a.startTime}`)
        const dateB = new Date(`${b.scheduledDate} ${b.startTime}`)
        return dateA.getTime() - dateB.getTime()
      })

      gradeData.value = sortedData.map((item: ScheduleGrade, index: number) => ({
        date: item.scheduledDate,
        score: item.score,
        comment: item.teacherComment || '',
        lesson: `第${index + 1}课`
      }))

      // 数据加载完成后初始化图表
      setTimeout(() => {
        initGradeChart()
      }, 200)
    } else {
      ElMessage.warning('暂无成绩数据')
      gradeData.value = []
    }
  } catch (error) {
    console.error('加载成绩数据失败:', error)
    ElMessage.error('加载成绩数据失败，请稍后重试')
    gradeData.value = []
  } finally {
    gradeLoading.value = false
  }
}

// 关闭成绩图表
const closeGradeChart = () => {
  gradeChartVisible.value = false
  currentCourseForGrade.value = null
  gradeData.value = []

  // 销毁ECharts实例
  const chartDom = document.getElementById('gradeChart')
  if (chartDom) {
    // 如果有ECharts实例需要销毁
    const echarts = (window as { echarts?: { dispose: (dom: HTMLElement) => void } }).echarts
    if (echarts) {
      echarts.dispose(chartDom)
    }
  }
}

// 成绩统计方法
const getAverageScore = () => {
  if (gradeData.value.length === 0) return 0
  const sum = gradeData.value.reduce((acc, item) => acc + (item.score || 0), 0)
  return (sum / gradeData.value.length).toFixed(1)
}

const getMaxScore = () => {
  if (gradeData.value.length === 0) return 0
  return Math.max(...gradeData.value.map(item => item.score || 0))
}

const getMinScore = () => {
  if (gradeData.value.length === 0) return 0
  return Math.min(...gradeData.value.map(item => item.score || 0))
}

// 获取成绩显示样式
const getScoreClass = (score: number) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 70) return 'score-average'
  return 'score-poor'
}

// 初始化ECharts图表
const initGradeChart = () => {
  // 延迟执行以确保DOM已渲染
  setTimeout(() => {
    const chartDom = document.getElementById('gradeChart')
    if (!chartDom || gradeData.value.length === 0) return

    // 检查是否有ECharts，如果没有则动态加载
    const echarts = (window as { echarts?: unknown }).echarts
    if (!echarts) {
      console.warn('ECharts 未加载，将显示表格形式的成绩数据')
      return
    }

    const myChart = (echarts as { init: (dom: HTMLElement) => { setOption: (option: unknown) => void; resize: () => void } }).init(chartDom)

    const option = {
      title: {
        text: '成绩趋势图',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: { dataIndex: number }[]) => {
          const data = params[0]
          const gradeItem = gradeData.value[data.dataIndex]
          return `
            <div>
              <strong>${gradeItem.lesson}</strong><br/>
              日期: ${gradeItem.date}<br/>
              成绩: ${gradeItem.score}分<br/>
              ${gradeItem.comment ? `评价: ${gradeItem.comment}` : ''}
            </div>
          `
        }
      },
      xAxis: {
        type: 'category',
        data: gradeData.value.map(item => item.lesson),
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: {
          formatter: '{value}分'
        }
      },
      series: [
        {
          name: '成绩',
          type: 'line',
          data: gradeData.value.map(item => item.score),
          smooth: true,
          lineStyle: {
            color: '#409eff',
            width: 3
          },
          itemStyle: {
            color: '#409eff',
            borderColor: '#409eff',
            borderWidth: 2
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                {
                  offset: 0,
                  color: 'rgba(64, 158, 255, 0.3)'
                },
                {
                  offset: 1,
                  color: 'rgba(64, 158, 255, 0.1)'
                }
              ]
            }
          },
          markLine: {
            data: [
              { yAxis: 60, name: '及格线', lineStyle: { color: '#f56c6c' } },
              { yAxis: 80, name: '良好线', lineStyle: { color: '#e6a23c' } },
              { yAxis: 90, name: '优秀线', lineStyle: { color: '#67c23a' } }
            ]
          }
        }
      ]
    }

    myChart.setOption(option)

    // 响应式调整
    window.addEventListener('resize', () => {
      myChart.resize()
    })
  }, 100)
}



// 获取课程安排的成绩
const getScheduleGrade = (scheduleId: number) => {
  return scheduleGrades.value.get(scheduleId)
}

// 加载课程安排的成绩
const loadScheduleGrades = async (schedules: CourseSchedule[]) => {
  for (const schedule of schedules) {
    if (schedule.status === 'completed') {
      try {
        // 使用项目的apiRequest方法，确保包含认证token
        const result = await apiRequest(`/api/lesson-grades/schedule/${schedule.id}/student/${schedule.studentId}`)
        if (result.success && result.data) {
          scheduleGrades.value.set(schedule.id, result.data)
        }
      } catch (error) {
        console.error(`加载课程安排 ${schedule.id} 的成绩失败:`, error)
      }
    }
  }
}

// 显示单节课成绩详情
const showScheduleGradeDetail = (schedule: CourseSchedule) => {
  const grade = getScheduleGrade(schedule.id)
  if (grade) {
    currentScheduleGrade.value = {
      schedule,
      grade,
      courseTitle: schedule.courseTitle,
      teacherName: schedule.teacherName,
      subjectName: schedule.subjectName
    }
    scheduleGradeVisible.value = true
  }
}

// 关闭单节课成绩详情
const closeScheduleGradeDetail = () => {
  scheduleGradeVisible.value = false
  currentScheduleGrade.value = null
}

// 获取成绩等级文本
const getGradeLevel = (score: number): string => {
  if (score >= 90) return '优秀'
  if (score >= 80) return '良好'
  if (score >= 70) return '中等'
  if (score >= 60) return '及格'
  return '不及格'
}


// 进入课堂
const enterClassroom = (course: Course) => {
  if (course.status === 'active') {
    // 跳转到课堂页面
    router.push(`/classroom/${course.id}`)
  } else if (course.status === 'upcoming') {
    ElMessage.warning('该课程还未开始')
  } else {
    ElMessage.info('该课程已结束')
  }
}

// ================= 预约相关方法 =================

// 加载预约申请
const loadBookings = async () => {
  bookingLoading.value = true
  try {
    const result = await bookingAPI.getStudentRequests({
      page: pagination.current,
      size: pagination.size,
      status: statusFilter.value
    })

    if (result.success && result.data) {
      bookings.value = result.data.records || []
      pagination.total = result.data.total || 0
    } else {
      ElMessage.error(result.message || '获取预约申请失败')
    }
  } catch (error) {
    console.error('获取预约申请失败:', error)
    ElMessage.error('获取预约申请失败，请稍后重试')
  } finally {
    bookingLoading.value = false
  }
}

// 查看预约详情
const viewBookingDetail = (booking: BookingRequest) => {
  selectedBooking.value = booking
  showDetailModal.value = true
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedBooking.value = null
}

// 取消预约
const cancelBooking = async (booking: BookingRequest) => {
  try {
    await ElMessageBox.confirm(
      '确定要取消这个预约申请吗？取消后无法恢复。',
      '确认取消',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '我再想想',
        type: 'warning'
      }
    )

    const result = await bookingAPI.cancel(booking.id)
    if (result.success) {
      ElMessage.success('预约申请已取消')
      loadBookings()
      if (showDetailModal.value) {
        closeDetailModal()
      }
    } else {
      ElMessage.error(result.message || '取消失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消预约失败:', error)
      ElMessage.error('取消失败，请稍后重试')
    }
  }
}

// 工具函数

const formatRecurringTime = (weekdays: number[], timeSlots: string[]) => {
  if (!weekdays || !timeSlots) return ''

  const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const dayNames = weekdays.map(day => weekdayNames[day]).join('、')
  const times = timeSlots.join('、')

  return `每周${dayNames} ${times}`
}

const getStatusTagType = (status: string) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'approved': return 'success'
    case 'rejected': return 'danger'
    case 'cancelled': return 'info'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'pending': return '待审批'
    case 'approved': return '已通过'
    case 'rejected': return '已拒绝'
    case 'cancelled': return '已取消'
    default: return '未知'
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
                      <el-icon><Timer /></el-icon>
                      <span>下次课程: {{ course.nextClass }}</span>
                    </div>
                  </div>
                  <div class="course-actions">
                    <el-button size="small" type="success" @click="showGradeChart(course)">
                      <el-icon><Reading /></el-icon> 查看成绩
                    </el-button>
                    <el-button
                      v-if="course.remainingLessons && course.remainingLessons > 0 && !(getNextSchedule(course.schedules)?.isTrial)"
                      size="small"
                      type="warning"
                      @click="showReschedule(course)"
                      :disabled="!!getRescheduleStatus(course.id)"
                    >
                      <el-icon><Refresh /></el-icon> 调课
                    </el-button>
                    <el-button
                      v-if="course.status === 'active' && course.remainingLessons && course.remainingLessons > 0"
                      size="small"
                      type="danger"
                      @click="requestSuspension(course)"
                      :disabled="!!getSuspensionStatus(course.id)"
                    >
                      停课
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
                      <el-icon><Timer /></el-icon>
                      <span>已完成</span>
                    </div>
                  </div>
                  <div class="course-actions">
                    <el-button size="small" type="success" @click="showGradeChart(course)">
                      <el-icon><Reading /></el-icon> 查看成绩
                    </el-button>
                    <el-button size="small" type="warning" @click="showCourseEvaluation(course)">
                      <el-icon><Star /></el-icon> 评价课程
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
        <el-tab-pane label="待审核" name="pending">
          <div class="tab-content">
            <div v-if="bookingLoading" class="loading-container">
              <el-skeleton :rows="3" animated />
            </div>
            <el-empty v-else-if="filteredBookings.length === 0" description="暂无待审核的预约" />
            <div v-else class="booking-list">
              <div v-for="booking in filteredBookings" :key="booking.id" class="booking-item">
                <div class="booking-header">
                  <div class="teacher-info">
                    <el-avatar :size="48" style="margin-right: 12px;">
                      {{ booking.teacherName?.charAt(0) || 'T' }}
                    </el-avatar>
                    <div class="teacher-details">
                      <div class="teacher-name">{{ booking.teacherName }}</div>
                      <div class="course-info">
                        <div class="course-title">{{ booking.courseTitle || '自定义课程' }}</div>
                        <div class="course-meta" v-if="booking.subjectName || booking.courseDurationMinutes">
                          <el-tag v-if="booking.subjectName" size="small" type="primary">{{ booking.subjectName }}</el-tag>
                          <el-tag v-if="booking.courseDurationMinutes" size="small" type="info">{{ booking.courseDurationMinutes }}分钟</el-tag>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="booking-status">
                    <el-tag :type="getStatusTagType(booking.status)" size="large">
                      {{ getStatusText(booking.status) }}
                    </el-tag>
                    <el-tag v-if="booking.isTrial" type="warning" size="small" style="margin-left: 8px;">
                      试听课
                    </el-tag>
                  </div>
                </div>

                <div class="booking-content">
                  <div class="booking-details">
                    <div class="detail-row">
                      <span class="label">预约类型：</span>
                      <span class="value">{{ booking.bookingType === 'single' ? '单次预约' : '周期性预约' }}</span>
                    </div>

                    <div v-if="booking.bookingType === 'single'" class="detail-row">
                      <span class="label">上课时间：</span>
                      <span class="value">
                        {{ booking.requestedDate }} {{ booking.requestedStartTime }}-{{ booking.requestedEndTime }}
                      </span>
                    </div>

                    <div v-else class="detail-row">
                      <span class="label">上课时间：</span>
                      <span class="value">
                        {{ formatRecurringTime(booking.recurringWeekdays, booking.recurringTimeSlots) }}
                      </span>
                    </div>

                    <div v-if="booking.bookingType === 'recurring'" class="detail-row">
                      <span class="label">课程周期：</span>
                      <span class="value">
                        {{ booking.startDate }} 至 {{ booking.endDate }}
                        <span v-if="booking.totalTimes">（共{{ booking.totalTimes }}次课）</span>
                      </span>
                    </div>

                    <div v-if="booking.studentRequirements" class="detail-row">
                      <span class="label">学习需求：</span>
                      <span class="value">{{ booking.studentRequirements }}</span>
                    </div>

                    <div class="detail-row">
                      <span class="label">申请时间：</span>
                      <span class="value">{{ formatDateTime(booking.createdAt) }}</span>
                    </div>

                    <div v-if="booking.teacherReply" class="detail-row">
                      <span class="label">教师回复：</span>
                      <span class="value teacher-reply">{{ booking.teacherReply }}</span>
                    </div>
                  </div>

                  <div class="booking-actions">
                    <el-button size="small" @click="viewBookingDetail(booking)">
                      详情
                    </el-button>
                    <el-button
                      v-if="booking.status === 'pending'"
                      type="danger"
                      size="small"
                      @click="cancelBooking(booking)"
                    >
                      取消预约
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="已审核" name="approved">
          <div class="tab-content">
            <div v-if="bookingLoading" class="loading-container">
              <el-skeleton :rows="3" animated />
            </div>
            <el-empty v-else-if="filteredBookings.length === 0" description="暂无已审核的预约" />
            <div v-else class="booking-list">
              <div v-for="booking in filteredBookings" :key="booking.id" class="booking-item">
                <div class="booking-header">
                  <div class="teacher-info">
                    <el-avatar :size="48" style="margin-right: 12px;">
                      {{ booking.teacherName?.charAt(0) || 'T' }}
                    </el-avatar>
                    <div class="teacher-details">
                      <div class="teacher-name">{{ booking.teacherName }}</div>
                      <div class="course-info">
                        <div class="course-title">{{ booking.courseTitle || '自定义课程' }}</div>
                        <div class="course-meta" v-if="booking.subjectName || booking.courseDurationMinutes">
                          <el-tag v-if="booking.subjectName" size="small" type="primary">{{ booking.subjectName }}</el-tag>
                          <el-tag v-if="booking.courseDurationMinutes" size="small" type="info">{{ booking.courseDurationMinutes }}分钟</el-tag>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="booking-status">
                    <el-tag :type="getStatusTagType(booking.status)" size="large">
                      {{ getStatusText(booking.status) }}
                    </el-tag>
                    <el-tag v-if="booking.isTrial" type="warning" size="small" style="margin-left: 8px;">
                      试听课
                    </el-tag>
                  </div>
                </div>

                <div class="booking-content">
                  <div class="booking-details">
                    <div class="detail-row">
                      <span class="label">预约类型：</span>
                      <span class="value">{{ booking.bookingType === 'single' ? '单次预约' : '周期性预约' }}</span>
                    </div>

                    <div v-if="booking.bookingType === 'single'" class="detail-row">
                      <span class="label">上课时间：</span>
                      <span class="value">
                        {{ booking.requestedDate }} {{ booking.requestedStartTime }}-{{ booking.requestedEndTime }}
                      </span>
                    </div>

                    <div v-else class="detail-row">
                      <span class="label">上课时间：</span>
                      <span class="value">
                        {{ formatRecurringTime(booking.recurringWeekdays, booking.recurringTimeSlots) }}
                      </span>
                    </div>

                    <div v-if="booking.bookingType === 'recurring'" class="detail-row">
                      <span class="label">课程周期：</span>
                      <span class="value">
                        {{ booking.startDate }} 至 {{ booking.endDate }}
                        <span v-if="booking.totalTimes">（共{{ booking.totalTimes }}次课）</span>
                      </span>
                    </div>

                    <div v-if="booking.studentRequirements" class="detail-row">
                      <span class="label">学习需求：</span>
                      <span class="value">{{ booking.studentRequirements }}</span>
                    </div>

                    <div class="detail-row">
                      <span class="label">申请时间：</span>
                      <span class="value">{{ formatDateTime(booking.createdAt) }}</span>
                    </div>

                    <div v-if="booking.teacherReply" class="detail-row">
                      <span class="label">教师回复：</span>
                      <span class="value teacher-reply">{{ booking.teacherReply }}</span>
                    </div>
                  </div>

                  <div class="booking-actions">
                    <el-button size="small" @click="viewBookingDetail(booking)">
                      详情
                    </el-button>
                  </div>
                </div>
              </div>
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
              <div class="meta-item" v-if="!isTrialCourse">
                <el-icon><Collection /></el-icon>
                <span>{{ currentCourse.subject }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Calendar /></el-icon>
                <span>{{ currentCourse.schedule }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Timer /></el-icon>
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

        <!-- 课程时间安排 -->
        <div class="detail-schedule" v-if="currentCourse.schedules && currentCourse.schedules.length > 0">
          <h4>课程时间安排</h4>
          <div class="schedule-list">
            <div
              v-for="(schedule, index) in sortedSchedules"
              :key="schedule.id"
              class="schedule-item-detail"
              :class="getScheduleStatusClass(schedule.status)"
            >
              <div class="schedule-number">
                <span>第{{ getEffectiveSessionNumber(schedule, index) }}节</span>
              </div>
              <div class="schedule-info">
                <div class="schedule-date">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ formatScheduleDate(schedule.scheduledDate) }}</span>
                </div>
                <div class="schedule-time">
                  <el-icon><Clock /></el-icon>
                  <span>{{ schedule.startTime }} - {{ schedule.endTime }}</span>
                </div>
              </div>
              <div class="schedule-status">
                <el-tag
                  :type="getScheduleTagType(schedule.status)"
                  size="small"
                >
                  {{ getScheduleStatusText(schedule.status) }}
                </el-tag>
              </div>
              <div class="schedule-grade" v-if="schedule.status === 'completed'">
                <div class="grade-info" v-if="getScheduleGrade(schedule.id)">
                  <span class="grade-label">本节成绩:</span>
                  <span :class="getScoreClass(getScheduleGrade(schedule.id).score)" class="grade-score">
                    {{ getScheduleGrade(schedule.id).score }}分
                  </span>
                  <el-button size="small" type="text" @click="showScheduleGradeDetail(schedule)">
                    详情
                  </el-button>
                </div>
                <div class="no-grade" v-else>
                  <span class="no-grade-text">待评分</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-actions">
          <el-button type="primary" @click="enterClassroom(currentCourse)">
            <el-icon><VideoCamera /></el-icon> 进入课堂
          </el-button>
          <el-button type="success" @click="showGradeChart(currentCourse)">
            <el-icon><Reading /></el-icon> 查看成绩
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 调课申请弹窗（重构为公用组件） -->
    <RescheduleModal
      v-model="showRescheduleModal"
      :course="currentRescheduleCourse"
      :isTeacher="false"
      :all-schedules="schedules"
      @success="loadStudentSchedules"
    />

    <!-- 成绩图表弹窗 -->
    <el-dialog
      v-model="gradeChartVisible"
      title="课程成绩分析"
      width="80%"
      :before-close="closeGradeChart"
    >
      <div v-if="currentCourseForGrade" class="grade-chart-content">
        <div class="course-info-header">
          <div class="course-title">
            <h3>{{ currentCourseForGrade.title }}</h3>
            <p>{{ currentCourseForGrade.subject }} | {{ currentCourseForGrade.teacher }}</p>
          </div>
          <div class="grade-stats" v-if="gradeData.length > 0">
            <div class="stat-item">
              <span class="stat-label">平均分</span>
              <span class="stat-value">{{ getAverageScore() }}分</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最高分</span>
              <span class="stat-value">{{ getMaxScore() }}分</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最低分</span>
              <span class="stat-value">{{ getMinScore() }}分</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已评分课程</span>
              <span class="stat-value">{{ gradeData.length }}节</span>
            </div>
          </div>
        </div>

        <div v-if="gradeLoading" class="loading-container">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="gradeData.length === 0" class="no-grade-data">
          <el-empty description="暂无成绩数据">
            <el-button type="primary" @click="loadGradeData(currentCourseForGrade)">
              刷新数据
            </el-button>
          </el-empty>
        </div>

        <div v-else class="grade-content">
          <!-- ECharts图表 -->
          <div class="chart-container">
            <div id="gradeChart" style="width: 100%; height: 400px;"></div>
          </div>

          <!-- 成绩详细列表 -->
          <div class="grade-detail-list">
            <h4>课程详细成绩</h4>
            <el-table :data="gradeData" style="width: 100%">
              <el-table-column prop="lesson" label="课程" width="100">
                <template #default="{ row }">
                  <el-tag size="small" type="primary">{{ row.lesson }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="date" label="日期" width="120">
                <template #default="{ row }">
                  {{ formatDate(row.date) }}
                </template>
              </el-table-column>
              <el-table-column prop="score" label="成绩" width="100">
                <template #default="{ row }">
                  <span :class="getScoreClass(row.score)">{{ row.score }}分</span>
                </template>
              </el-table-column>
              <el-table-column prop="comment" label="教师评价" min-width="200">
                <template #default="{ row }">
                  <span v-if="row.comment" class="comment-text">{{ row.comment }}</span>
                  <span v-else class="no-comment">暂无评价</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeGradeChart">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 课程评价弹窗 -->
    <el-dialog
      v-model="evaluationVisible"
      title="课程评价"
      width="500px"
      :before-close="() => evaluationVisible = false"
    >
      <div v-if="currentCourseForEvaluation" class="evaluation-content">
        <div class="evaluation-course-info">
          <h4>{{ currentCourseForEvaluation.title }}</h4>
          <p>{{ currentCourseForEvaluation.subject }} | {{ currentCourseForEvaluation.teacher }}</p>
        </div>

        <el-form :model="evaluationForm" label-width="80px">
          <el-form-item label="课程评分" required>
            <el-rate
              v-model="evaluationForm.rating"
              :max="5"
              :step="0.5"
              show-score
              score-template="{value}分"
              :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
            />
          </el-form-item>

          <el-form-item label="评价内容" required>
            <el-input
              v-model="evaluationForm.comment"
              type="textarea"
              :rows="4"
              placeholder="请分享您对这门课程的感受和建议..."
              maxlength="255"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="evaluationVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="submitCourseEvaluation"
          :loading="evaluationLoading"
        >
          提交评价
        </el-button>
      </template>
    </el-dialog>

    <!-- 单节课成绩详情弹窗 -->
    <el-dialog
      v-model="scheduleGradeVisible"
      title="课程成绩详情"
      width="50%"
      :before-close="closeScheduleGradeDetail"
    >
      <div v-if="currentScheduleGrade" class="schedule-grade-detail">
        <div class="grade-header">
          <div class="course-info">
            <h3>{{ currentScheduleGrade.courseTitle }}</h3>
            <p>{{ currentScheduleGrade.subjectName }} | {{ currentScheduleGrade.teacherName }}</p>
            <p>{{ formatScheduleDate(currentScheduleGrade.schedule.scheduledDate) }} {{ currentScheduleGrade.schedule.startTime }}-{{ currentScheduleGrade.schedule.endTime }}</p>
          </div>
          <div class="grade-display">
            <div class="grade-score-large" :class="getScoreClass(currentScheduleGrade.grade.score)">
              {{ currentScheduleGrade.grade.score }}分
            </div>
            <div class="grade-level">
              {{ getGradeLevel(currentScheduleGrade.grade.score) }}
            </div>
          </div>
        </div>

        <div class="grade-details">
          <div class="detail-item">
            <label>课程编号:</label>
            <span>第{{ currentScheduleGrade.schedule.sessionNumber || 1 }}课</span>
          </div>
          <div class="detail-item">
            <label>成绩录入时间:</label>
            <span>{{ formatDateTime(currentScheduleGrade.grade.gradedAt) }}</span>
          </div>
          <div class="detail-item" v-if="currentScheduleGrade.grade.teacherComment">
            <label>教师评价:</label>
            <div class="comment-content">{{ currentScheduleGrade.grade.teacherComment }}</div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeScheduleGradeDetail">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 停课申请弹窗（日期范围选择） -->
    <el-dialog
      v-model="suspensionDialogVisible"
      title="申请停课"
      width="560px"
    >
      <div class="suspension-modal">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="规则说明：停课需从一周后开始，且连续时长不少于两周。"
          style="margin-bottom: 12px;"
        />
        <el-form label-width="96px">
          <el-form-item label="停课区间" required>
            <el-date-picker
              v-model="suspensionForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              unlink-panels
              :disabled-date="(date) => date.getTime() < new Date(new Date().setHours(0,0,0,0) + 7 * 24 * 3600 * 1000).getTime()"
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="suspensionForm.reason" type="textarea" :rows="3" placeholder="可填写停课原因"></el-input>
          </el-form-item>
        </el-form>
        <div class="hint-text">提示：提交后将进入管理员审批，审批通过后，所选区间内的未开始课节将暂停。</div>
      </div>
      <template #footer>
        <el-button @click="suspensionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSuspension">提交申请</el-button>
      </template>
    </el-dialog>


    <!-- 预约详情弹窗 -->
    <el-dialog
      v-model="showDetailModal"
      title="预约详情"
      width="600px"
      :before-close="closeDetailModal"
    >
      <div v-if="selectedBooking" class="booking-detail">
        <div class="detail-section">
          <h3>基本信息</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <label>教师姓名：</label>
              <span>{{ selectedBooking.teacherName }}</span>
            </div>
            <div class="detail-item">
              <label>课程类型：</label>
              <span>{{ selectedBooking.courseTitle || '自定义课程' }}</span>
            </div>
            <div class="detail-item">
              <label>预约类型：</label>
              <span>{{ selectedBooking.bookingType === 'single' ? '单次预约' : '周期性预约' }}</span>
            </div>
            <div class="detail-item">
              <label>申请状态：</label>
              <el-tag :type="getStatusTagType(selectedBooking.status)">
                {{ getStatusText(selectedBooking.status) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>是否试听：</label>
              <span>
                {{ selectedBooking.isTrial ? '是' : '否' }}
                <el-tag v-if="selectedBooking.isTrial" type="warning" size="small" style="margin-left: 8px;">
                  {{ selectedBooking.trialDurationMinutes }}分钟
                </el-tag>
              </span>
            </div>
            <div class="detail-item">
              <label>申请时间：</label>
              <span>{{ formatDateTime(selectedBooking.createdAt) }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h3>时间安排</h3>
          <div v-if="selectedBooking.bookingType === 'single'" class="time-info">
            <div class="time-item">
              <label>上课日期：</label>
              <span>{{ selectedBooking.requestedDate }}</span>
            </div>
            <div class="time-item">
              <label>上课时间：</label>
              <span>{{ selectedBooking.requestedStartTime }} - {{ selectedBooking.requestedEndTime }}</span>
            </div>
          </div>
          <div v-else class="time-info">
            <div class="time-item">
              <label>上课时间：</label>
              <span>{{ formatRecurringTime(selectedBooking.recurringWeekdays, selectedBooking.recurringTimeSlots) }}</span>
            </div>
            <div class="time-item">
              <label>课程周期：</label>
              <span>{{ selectedBooking.startDate }} 至 {{ selectedBooking.endDate }}</span>
            </div>
            <div v-if="selectedBooking.totalTimes" class="time-item">
              <label>总课程数：</label>
              <span>{{ selectedBooking.totalTimes }}次</span>
            </div>
          </div>
        </div>

        <div v-if="selectedBooking.studentRequirements" class="detail-section">
          <h3>学习需求</h3>
          <div class="requirements-content">
            {{ selectedBooking.studentRequirements }}
          </div>
        </div>

        <div v-if="selectedBooking.teacherReply" class="detail-section">
          <h3>教师回复</h3>
          <div class="reply-content">
            {{ selectedBooking.teacherReply }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeDetailModal">关闭</el-button>
        <el-button
          v-if="selectedBooking && selectedBooking.status === 'pending'"
          type="danger"
          @click="cancelBooking(selectedBooking)"
        >
          取消预约
        </el-button>
      </template>
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

.reschedule-status-tag {
  position: absolute;
  top: 45px;
  right: 15px;
  z-index: 2;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.remaining-lessons {
  color: #e6a23c;
  font-weight: 500;
  font-size: 12px;
}

.reschedule-modal-content {
  padding: 20px;
}

.reschedule-info {
  margin-bottom: 20px;
}

.course-info {
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.info-item .label {
  width: 80px;
  font-weight: 500;
  color: #333;
}

.remaining-highlight {
  color: #e6a23c;
  font-weight: 600;
}

.current-schedule {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.reschedule-type-selection {
  text-align: center;
  margin-bottom: 30px;
}

.original-schedule-group,
.new-schedule-group {
  display: flex;
  gap: 15px;
  align-items: center;
}

.time-slot-selection {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 10px;
}

.time-slot-card {
  position: relative;
  background-color: #f8f8f8;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.time-slot-card:hover {
  background-color: #e0e0e0;
  border-color: #ccc;
}

.time-slot-card.selected {
  background-color: #f6ffed;
  border-color: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
}

.slot-time {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.weekday-selection {
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
  margin-top: 10px;
}

.weekday-selection .el-checkbox-group {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.weekday-selection .el-checkbox {
  margin-right: 0;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 8px 12px;
  transition: all 0.3s;
}

.weekday-selection .el-checkbox:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.weekday-selection .el-checkbox.is-checked {
  border-color: #52c41a;
  background-color: #f6ffed;
}

.weekday-label {
  font-weight: 500;
  color: #333;
}

.schedule-preview {
  background-color: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 8px;
  padding: 15px;
}

.preview-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.preview-label {
  font-weight: 500;
  color: #333;
}

.preview-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* 课程时间安排样式 */
.detail-schedule {
  margin-top: 20px;
}

.detail-schedule h4 {
  margin-bottom: 15px;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.schedule-list {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background-color: #fafafa;
}

.schedule-item-detail {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  transition: background-color 0.3s ease;
}

.schedule-item-detail:last-child {
  border-bottom: none;
}

.schedule-item-detail:hover {
  background-color: #f0f9ff;
}

.schedule-number {
  flex-shrink: 0;
  width: 60px;
  margin-right: 15px;
}

.schedule-number span {
  display: inline-block;
  padding: 4px 8px;
  background-color: #409eff;
  color: white;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.schedule-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.schedule-date,
.schedule-time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.schedule-date .el-icon,
.schedule-time .el-icon {
  font-size: 16px;
  color: #909399;
}

.schedule-status {
  flex-shrink: 0;
  margin-left: 15px;
}

/* 不同状态的样式 */
.schedule-completed .schedule-number span {
  background-color: #67c23a;
}

.schedule-cancelled .schedule-number span {
  background-color: #f56c6c;
}

.schedule-scheduled .schedule-number span {
  background-color: #409eff;
}

.schedule-rescheduled .schedule-number span {
  background-color: #e6a23c;
}

.time-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 原定课程显示样式 */
.original-schedule-display {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.schedule-info {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.schedule-info .el-tag {
  padding: 8px 12px;
  font-size: 14px;
}

/* 不可用选项样式 */
.weekday-label.disabled {
  color: #c0c4cc;
  text-decoration: line-through;
}

.unavailable-hint {
  font-size: 12px;
  color: #f56c6c;
  margin-left: 4px;
}

.no-available-slots,
.no-available-weekdays {
  text-align: center;
  padding: 20px;
  background-color: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  margin-top: 10px;
}

/* 禁用状态的时间段卡片样式 */
.time-slot-card.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.time-slot-card.disabled:hover {
  transform: none;
  box-shadow: none;
  background-color: #f8f8f8;
}

/* 已选中但被冲突的时间段样式 - 仍然可点击用于取消选择 */
.time-slot-card.selected.conflicted {
  cursor: pointer;
  opacity: 1;
  background-color: #f6ffed;
  border-color: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
}

.time-slot-card.selected.conflicted:hover {
  background-color: #d9f7be;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
}

.unavailable-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: inherit;
}

.unavailable-text {
  font-size: 12px;
  color: #f56c6c;
  font-weight: 500;
}

/* 时间冲突检查样式 */
.time-conflict-check {
  margin-top: 15px;
  padding: 10px;
  border-radius: 6px;
  background-color: #f8f9fa;
}

.time-conflict-check .checking {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.time-conflict-check .checking .el-icon {
  font-size: 16px;
}

.time-conflict-check .check-result {
  margin: 0;
}

.conflict-detail {
  margin-top: 10px;
  font-size: 13px;
}

.conflict-detail p {
  margin: 0 0 8px 0;
  font-weight: 500;
}

.conflict-detail ul {
  margin: 0;
  padding-left: 20px;
}

.conflict-detail li {
  margin-bottom: 4px;
  color: #666;
}

.schedule-info .el-icon {
  margin-right: 6px;
}

.schedule-note {
  margin-top: 4px;
}

@media (max-width: 768px) {
  .courses-grid {
    grid-template-columns: 1fr;
  }

  .time-slot-selection {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .original-schedule-group,
  .new-schedule-group {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .original-schedule-group > *,
  .new-schedule-group > * {
    width: 100%;
  }

  .weekday-selection .el-checkbox-group {
    gap: 10px;
  }

  .preview-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
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

/* 加载状态样式 */
.loading-container {
  padding: 40px 20px;
  text-align: center;
}

/* 成绩图表弹窗样式 */
.grade-chart-content {
  padding: 20px 0;
}

.course-info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 12px;
}

.course-title h3 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.course-title p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.grade-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  text-align: center;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 80px;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.grade-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.chart-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.grade-detail-list {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.grade-detail-list h4 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 2px solid #f0f2f5;
  padding-bottom: 10px;
}

.no-grade-data {
  padding: 60px 20px;
  text-align: center;
}

/* 成绩等级样式 */
.score-excellent {
  color: #67c23a;
  font-weight: 600;
}

.score-good {
  color: #409eff;
  font-weight: 600;
}

.score-average {
  color: #e6a23c;
  font-weight: 600;
}

.score-poor {
  color: #f56c6c;
  font-weight: 600;
}

.comment-text {
  color: #606266;
  line-height: 1.4;
}

.no-comment {
  color: #c0c4cc;
  font-style: italic;
}


/* 时间段选择提示样式 */
.time-slot-tip {
  margin-top: 10px;
  padding: 8px 12px;
  background-color: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #409eff;
}

.time-slot-tip .el-icon {
  font-size: 14px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .course-info-header {
    flex-direction: column;
    gap: 20px;
  }

  .grade-stats {
    flex-wrap: wrap;
    gap: 12px;
  }

  .stat-item {
    min-width: 70px;
    padding: 8px 12px;
  }

  .stat-value {
    font-size: 16px;
  }
}

/* 课程安排成绩显示样式 */
.schedule-item-detail {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  background: white;
  transition: all 0.3s;
}

.schedule-item-detail:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.schedule-grade {
  margin-left: 15px;
  min-width: 120px;
}

.grade-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.grade-label {
  font-size: 12px;
  color: #909399;
}

.grade-score {
  font-weight: 600;
  font-size: 14px;
}

.no-grade {
  display: flex;
  align-items: center;
}

.no-grade-text {
  font-size: 12px;
  color: #c0c4cc;
  font-style: italic;
}

/* 课程评价弹窗样式 */
.evaluation-content {
  padding: 20px 0;
}

.evaluation-course-info {
  margin-bottom: 24px;
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8px;
  text-align: center;
}

.evaluation-course-info h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.evaluation-course-info p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

/* 单节课成绩详情弹窗样式 */
.schedule-grade-detail {
  padding: 20px;
}

.grade-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
}

.grade-header .course-info h3 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.grade-header .course-info p {
  margin: 4px 0;
  color: #606266;
  font-size: 14px;
}

.grade-display {
  text-align: center;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.grade-score-large {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 8px;
}

.grade-level {
  font-size: 14px;
  color: #606266;
  padding: 4px 12px;
  background: #f5f7fa;
  border-radius: 16px;
  display: inline-block;
}

.grade-details {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.grade-details .detail-item {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.grade-details .detail-item:last-child {
  margin-bottom: 0;
}

.grade-details .detail-item label {
  font-weight: 600;
  color: #303133;
  min-width: 120px;
  margin-right: 12px;
}

.comment-content {
  flex: 1;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  line-height: 1.6;
  color: #606266;
}

/* 预约相关样式 */
.booking-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.booking-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  background-color: #fff;
  transition: all 0.3s ease;
}

.booking-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.teacher-info {
  display: flex;
  align-items: center;
}

.teacher-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.teacher-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.course-info {
  font-size: 14px;
  color: #909399;
}

.booking-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.booking-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.booking-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.detail-row .label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
  flex-shrink: 0;
}

.detail-row .value {
  color: #303133;
  line-height: 1.5;
}

.teacher-reply {
  background-color: #f0f9ff;
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.booking-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.booking-detail {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h3 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.detail-item label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
  margin-right: 8px;
}

.detail-item span {
  color: #303133;
}

.time-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.time-item {
  display: flex;
  align-items: center;
}

.time-item label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
  margin-right: 8px;
}

.time-item span {
  color: #303133;
}

.requirements-content,
.reply-content {
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  line-height: 1.6;
  color: #303133;
}

.reply-content {
  border-left-color: #67c23a;
}

.course-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.course-title {
  font-size: 14px;
  color: #606266;
}
</style>
