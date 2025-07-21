<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Connection, Timer, Male, Female, Message, Loading, View, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { teacherAPI, subjectAPI, bookingAPI } from '@/utils/api'

// 声明图片相对路径，使用getImageUrl方法加载
const teacherImages = {
  teacherBoy1: '@/assets/pictures/teacherBoy1.jpeg',
  teacherBoy2: '@/assets/pictures/teacherBoy2.jpeg',
  teacherBoy3: '@/assets/pictures/teacherBoy3.jpeg',
  teacherGirl1: '@/assets/pictures/teacherGirl1.jpeg',
  teacherGirl4: '@/assets/pictures/teacherGirl4.jpeg'
}

interface Teacher {
  id: number
  name: string
  subject: string
  grade: string
  experience: number
  description: string
  avatar: string
  tags: string[]
  schedule: string[]
  matchScore: number
  gender: string
  teachingStyle: string
  hourlyRate?: number
  educationBackground?: string
  specialties?: string
  isVerified?: boolean
}

// 新增课表相关接口
interface TimeSlot {
  date: string
  time: string
  available: boolean
  booked?: boolean
  selected?: boolean
}

interface WeekSchedule {
  weekStart: string
  weekEnd: string
  slots: TimeSlot[]
}

interface TeacherSchedule {
  teacherName: string
  yearSchedule: WeekSchedule[]
}

// 新增周期性课程选择接口
interface RecurringSchedule {
  id: string
  weekdays: number[] // 0=周日, 1=周一, ..., 6=周六
  timeSlot: string // 如 "14:00-15:00"
  startDate: string
  endDate: string
  totalSessions: number
  description: string
}

interface AvailableTimeSlot {
  weekday: number
  time: string
  available: boolean
  conflictDates?: string[] // 冲突的具体日期
  availableFromDate?: string // 从什么时候开始可用
  conflictEndDate?: string // 冲突结束日期
  conflictReason?: string // 冲突原因
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
const subjects = ref<any[]>([])
const grades = ref<string[]>([])
const loadingOptions = ref(false)

// 新增课表相关数据
const showScheduleModal = ref(false)
const currentTeacher = ref<Teacher | null>(null)
const currentTeacherSchedule = ref<TeacherSchedule | null>(null)

// 新增周期性选课数据
const recurringSchedules = ref<RecurringSchedule[]>([])
const availableTimeSlots = ref<AvailableTimeSlot[]>([])
const selectedRecurringSchedule = ref<RecurringSchedule | null>(null)
const scheduleForm = reactive({
  bookingType: 'recurring' as 'single' | 'recurring', // 预约类型
  selectedWeekdays: [] as number[],
  selectedTimeSlots: [] as string[], // 改为数组支持多选
  startDate: '',
  endDate: '',
  sessionCount: 12, // 默认12次课
  // 单次预约相关字段
  singleDate: '',
  singleStartTime: '',
  singleEndTime: ''
})

// 月度课表查看相关数据
const showMonthlyModal = ref(false)
const currentMonthTeacher = ref<Teacher | null>(null)
const currentMonth = ref(new Date().getMonth())
const currentYear = ref(new Date().getFullYear())
const monthlyScheduleData = ref<Array<Array<{
  day: number
  date: string
  isCurrentMonth: boolean
  timeSlots: Array<{
    time: string
    available: boolean
    booked: boolean
    student: string | null
  }>
  availableCount: number
  bookedCount: number
}>>>([])

// 日期详情查看相关数据
const showDayDetailModal = ref(false)
const selectedDayData = ref<{
  date: string
  day: number
  timeSlots: Array<{
    time: string
    available: boolean
    booked: boolean
    student: string | null
  }>
} | null>(null)

const router = useRouter()

const handleMatch = async () => {
  // 验证必填项
  if (!matchForm.subject || !matchForm.grade) {
    ElMessage.warning('请至少选择科目和年级')
    return
  }

  loading.value = true
  showResults.value = false

  try {
    // 构建匹配请求参数
    const matchRequest = {
      subject: matchForm.subject,
      grade: matchForm.grade,
      preferredTime: matchForm.preferredTime || undefined,
      preferredDateStart: matchForm.preferredDateRange && matchForm.preferredDateRange.length > 0
        ? matchForm.preferredDateRange[0] : undefined,
      preferredDateEnd: matchForm.preferredDateRange && matchForm.preferredDateRange.length > 1
        ? matchForm.preferredDateRange[1] : undefined,
      limit: 3
    }

    // 调用后端API
    const result = await teacherAPI.matchTeachers(matchRequest)

    if (result.success && result.data) {
      // 转换后端数据格式为前端需要的格式
      const teachers = result.data.map((teacher: any) => ({
        id: teacher.id,
        name: teacher.name,
        subject: teacher.subject,
        grade: teacher.grade,
        experience: teacher.experience,
        description: teacher.description,
        avatar: teacher.avatar || teacherImages.teacherBoy1, // 使用默认头像如果没有
        tags: teacher.tags || [],
        schedule: teacher.schedule || ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00'],
        matchScore: teacher.matchScore,
        hourlyRate: teacher.hourlyRate,
        educationBackground: teacher.educationBackground,
        specialties: teacher.specialties,
        isVerified: teacher.isVerified,
        gender: teacher.gender || 'Male', // 提供默认性别
        teachingStyle: teacher.teachingStyle || '个性化教学' // 提供默认教学风格
      }))

      matchedTeachers.value = teachers
      showResults.value = true

      ElMessage.success(`为您匹配到了 ${teachers.length} 位适合的教师`)
    } else {
      ElMessage.error('匹配失败，请稍后重试')
    }
  } catch (error) {
    console.error('匹配教师失败:', error)
    ElMessage.error('匹配失败，请检查网络连接')
  } finally {
    loading.value = false
  }
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

// 获取科目选项
const loadSubjects = async () => {
  try {
    const result = await subjectAPI.getActiveSubjects()
    if (result.success && result.data) {
      subjects.value = result.data
    }
  } catch (error) {
    console.error('获取科目列表失败:', error)
  }
}

// 获取年级选项
const loadGrades = async () => {
  try {
    const result = await teacherAPI.getAvailableGrades()
    if (result.success && result.data) {
      grades.value = result.data
    }
  } catch (error) {
    console.error('获取年级列表失败:', error)
  }
}

// 初始化选项数据
const initOptions = async () => {
  loadingOptions.value = true
  try {
    await Promise.all([loadSubjects(), loadGrades()])
  } finally {
    loadingOptions.value = false
  }
}

// 生成未来一年的课表数据
const generateYearSchedule = (teacherName: string): TeacherSchedule => {
  const yearSchedule: WeekSchedule[] = []
  const currentDate = new Date()

  // 生成52周的课表
  for (let week = 0; week < 52; week++) {
    const weekStart = new Date(currentDate)
    weekStart.setDate(currentDate.getDate() + week * 7 - currentDate.getDay())

    const weekEnd = new Date(weekStart)
    weekEnd.setDate(weekStart.getDate() + 6)

    const slots: TimeSlot[] = []

    // 每周生成时间段
    for (let day = 0; day < 7; day++) {
      const slotDate = new Date(weekStart)
      slotDate.setDate(weekStart.getDate() + day)

      // 根据星期几生成不同的时间段
      const timeSlots = getTimeSlotsForDay(slotDate.getDay())

      timeSlots.forEach(time => {
        // 随机设置一些时间段为已预订
        const isBooked = Math.random() < 0.3
        slots.push({
          date: slotDate.toISOString().split('T')[0],
          time: time,
          available: !isBooked,
          booked: isBooked,
          selected: false
        })
      })
    }

    yearSchedule.push({
      weekStart: weekStart.toISOString().split('T')[0],
      weekEnd: weekEnd.toISOString().split('T')[0],
      slots: slots
    })
  }

  return {
    teacherName: teacherName,
    yearSchedule: yearSchedule
  }
}

// 根据星期几返回可用的时间段
const getTimeSlotsForDay = (dayOfWeek: number): string[] => {
  // 0 = 周日, 1 = 周一, ..., 6 = 周六
  const weekdaySlots = ['16:00-17:00', '17:00-18:00', '18:00-19:00', '19:00-20:00', '20:00-21:00']
  const weekendSlots = ['09:00-10:00', '10:00-11:00', '11:00-12:00', '14:00-15:00', '15:00-16:00', '16:00-17:00', '17:00-18:00']

  if (dayOfWeek === 0 || dayOfWeek === 6) {
    return weekendSlots
  } else {
    return weekdaySlots
  }
}

// 显示教师课表
const showTeacherSchedule = (teacher: Teacher) => {
  currentTeacher.value = teacher
  currentTeacherSchedule.value = generateYearSchedule(teacher.name)

  // 生成可用的周期性时间段
  generateAvailableTimeSlots()

  // 重置选课表单
  scheduleForm.bookingType = 'recurring'
  scheduleForm.selectedWeekdays = []
  scheduleForm.selectedTimeSlots = []
  scheduleForm.startDate = new Date().toISOString().split('T')[0]
  scheduleForm.endDate = ''
  scheduleForm.sessionCount = 12
  // 重置单次预约字段
  scheduleForm.singleDate = ''
  scheduleForm.singleStartTime = ''
  scheduleForm.singleEndTime = ''

  showScheduleModal.value = true
}

// 生成可用的周期性时间段
const generateAvailableTimeSlots = () => {
  availableTimeSlots.value = []

  const weekdays = [1, 2, 3, 4, 5, 6, 0] // 周一到周日
  const timeSlots = [
    '09:00-10:00', '10:00-11:00', '11:00-12:00',
    '14:00-15:00', '15:00-16:00', '16:00-17:00', '17:00-18:00',
    '18:00-19:00', '19:00-20:00', '20:00-21:00'
  ]

  weekdays.forEach(weekday => {
    timeSlots.forEach(time => {
      // 检查该时间段在未来3个月内的可用性
      const { conflicts, availableFromDate, conflictEndDate, conflictReason } = checkTimeSlotConflicts(weekday, time)
      const availabilityRate = 1 - (conflicts.length / 12) // 假设检查12周

      availableTimeSlots.value.push({
        weekday,
        time,
        available: availabilityRate > 0.7, // 70%以上可用才显示为可选
        conflictDates: conflicts,
        availableFromDate,
        conflictEndDate,
        conflictReason
      })
    })
  })
}

// 检查时间段冲突
const checkTimeSlotConflicts = (weekday: number, time: string): { conflicts: string[], availableFromDate?: string, conflictEndDate?: string, conflictReason?: string } => {
  const conflicts: string[] = []
  const currentDate = new Date()
  let availableFromDate: string | undefined
  let conflictEndDate: string | undefined
  let conflictReason: string | undefined

  // 检查未来12周的该时间段
  for (let week = 0; week < 12; week++) {
    const checkDate = new Date(currentDate)
    checkDate.setDate(currentDate.getDate() + week * 7 + (weekday - currentDate.getDay()))

    const dateStr = checkDate.toISOString().split('T')[0]

    // 检查该日期该时间段是否已被预订
    if (currentTeacherSchedule.value) {
      for (const weekSchedule of currentTeacherSchedule.value.yearSchedule) {
        const slot = weekSchedule.slots.find(s => s.date === dateStr && s.time === time)
        if (slot && slot.booked) {
          conflicts.push(dateStr)

          // 模拟结课逻辑：假设有些课程会在特定日期结束
          if (Math.random() < 0.3) { // 30%的概率是结课
            const endDate = new Date(checkDate)
            endDate.setDate(checkDate.getDate() + Math.floor(Math.random() * 28) + 7) // 1-4周后结课
            conflictEndDate = endDate.toISOString().split('T')[0]
            conflictReason = '学生课程结束'

            // 结课后的下一周就可以选择
            const nextAvailableDate = new Date(endDate)
            nextAvailableDate.setDate(endDate.getDate() + 7)
            if (!availableFromDate || nextAvailableDate < new Date(availableFromDate)) {
              availableFromDate = nextAvailableDate.toISOString().split('T')[0]
            }
          } else {
            conflictReason = '已有其他学生预约'
          }
          break
        }
      }
    }
  }

  return { conflicts, availableFromDate, conflictEndDate, conflictReason }
}

// 检查是否为试听课申请
const checkIfTrialRequest = async (): Promise<boolean> => {
  try {
    const result = await bookingAPI.checkTrialEligibility()
    if (result.success && result.data) {
      // 如果用户可以使用免费试听，询问是否要申请试听课
      const confirmed = await ElMessageBox.confirm(
        '您可以申请30分钟免费试听课，是否要申请试听课？',
        '免费试听课',
        {
          confirmButtonText: '申请试听课',
          cancelButtonText: '申请正式课程',
          type: 'info'
        }
      )
      return confirmed === 'confirm'
    }
    return false
  } catch (error) {
    console.error('检查试听资格失败:', error)
    return false
  }
}

// 创建预约申请（支持单次和周期性）
const createBookingRequest = async () => {
  if (!currentTeacher.value) {
    ElMessage.error('请先选择教师')
    return
  }

  // 验证表单数据
  if (scheduleForm.bookingType === 'single') {
    if (!scheduleForm.singleDate || !scheduleForm.singleStartTime || !scheduleForm.singleEndTime) {
      ElMessage.warning('请选择上课日期和时间')
      return
    }
  } else {
    if (scheduleForm.selectedWeekdays.length === 0 || scheduleForm.selectedTimeSlots.length === 0) {
      ElMessage.warning('请选择上课时间')
      return
    }
    if (!scheduleForm.startDate || !scheduleForm.endDate) {
      ElMessage.warning('请选择开始和结束日期')
      return
    }
  }

  try {
    // 检查是否为试听课申请
    const isTrial = await checkIfTrialRequest()

    // 构建预约请求数据
    const bookingData: any = {
      teacherId: currentTeacher.value.id,
      courseId: null, // 可以根据需要设置课程ID
      bookingType: scheduleForm.bookingType,
      studentRequirements: `希望预约${currentTeacher.value.name}老师的${currentTeacher.value.subject}课程`,
      isTrial: isTrial,
      trialDurationMinutes: isTrial ? 30 : undefined
    }

    if (scheduleForm.bookingType === 'single') {
      // 单次预约
      bookingData.requestedDate = scheduleForm.singleDate
      bookingData.requestedStartTime = scheduleForm.singleStartTime
      bookingData.requestedEndTime = scheduleForm.singleEndTime
    } else {
      // 周期性预约
      bookingData.recurringWeekdays = scheduleForm.selectedWeekdays
      bookingData.recurringTimeSlots = scheduleForm.selectedTimeSlots
      bookingData.startDate = scheduleForm.startDate
      bookingData.endDate = scheduleForm.endDate
      bookingData.totalTimes = scheduleForm.sessionCount
    }

    // 调用后端API创建预约申请
    const result = await bookingAPI.createRequest(bookingData)

    if (result.success && result.data) {
      let successMessage = ''
      if (scheduleForm.bookingType === 'single') {
        successMessage = `预约申请提交成功！${currentTeacher.value?.name} - ${scheduleForm.singleDate} ${scheduleForm.singleStartTime}-${scheduleForm.singleEndTime}${isTrial ? '（30分钟免费试听）' : ''}`
      } else {
        const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
        const selectedDays = scheduleForm.selectedWeekdays.map(day => weekdayNames[day]).join('、')
        const selectedTimes = scheduleForm.selectedTimeSlots.join('、')
        successMessage = `预约申请提交成功！${currentTeacher.value?.name} - 每周${selectedDays} ${selectedTimes}，共${scheduleForm.sessionCount}次课${isTrial ? '（包含30分钟免费试听）' : ''}`
      }

      ElMessage.success({
        message: successMessage,
        duration: 5000
      })

      showScheduleModal.value = false

      // 跳转到学生预约管理页面
      setTimeout(() => {
        ElMessage.info('即将跳转到预约管理页面...')
        // router.push('/student/bookings') // 暂时注释，因为这个页面可能还不存在
      }, 2000)
    } else {
      ElMessage.error(result.message || '预约申请提交失败')
    }
  } catch (error) {
    console.error('创建预约申请失败:', error)
    ElMessage.error('预约申请提交失败，请稍后重试')
  }
}

// 保持向后兼容
const createRecurringSchedule = createBookingRequest

// 计算课程结束日期
const calculateEndDate = () => {
  if (!scheduleForm.startDate || scheduleForm.selectedWeekdays.length === 0) return

  const startDate = new Date(scheduleForm.startDate)
  const sessionsPerWeek = scheduleForm.selectedWeekdays.length
  const totalWeeks = Math.ceil(scheduleForm.sessionCount / sessionsPerWeek)

  const endDate = new Date(startDate)
  endDate.setDate(startDate.getDate() + (totalWeeks - 1) * 7)

  scheduleForm.endDate = endDate.toISOString().split('T')[0]
}

// 获取星期几的中文名称
const getWeekdayName = (weekday: number): string => {
  const names = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return names[weekday]
}

// 获取唯一的时间段列表
const getUniqueTimeSlots = (): string[] => {
  const timeSlots = [
    '09:00-10:00', '10:00-11:00', '11:00-12:00',
    '14:00-15:00', '15:00-16:00', '16:00-17:00', '17:00-18:00',
    '18:00-19:00', '19:00-20:00', '20:00-21:00'
  ]
  return timeSlots
}

// 选择时间段
const selectTimeSlot = (time: string) => {
  if (scheduleForm.selectedTimeSlots.includes(time)) {
    scheduleForm.selectedTimeSlots = scheduleForm.selectedTimeSlots.filter(t => t !== time)
  } else {
    scheduleForm.selectedTimeSlots.push(time)
  }
  // 清空之前选择的星期，让用户重新选择
  scheduleForm.selectedWeekdays = []
  // 重新计算结束日期
  calculateEndDate()
}

// 获取星期几的短名称
const getWeekdayShort = (weekday: number): string => {
  const names = ['日', '一', '二', '三', '四', '五', '六']
  return names[weekday]
}

// 判断某个星期几是否可用（检查所有选中的时间段）
const isTimeSlotAvailable = (weekday: number, time: string): boolean => {
  const slot = availableTimeSlots.value.find(s => s.weekday === weekday && s.time === time)
  return slot ? slot.available : true // 默认可用，除非有冲突
}

// 判断某个时间段是否有可用的开始日期
const hasAvailableFromDate = (weekday: number, time: string): boolean => {
  const slot = availableTimeSlots.value.find(s => s.weekday === weekday && s.time === time)
  return slot ? !!slot.availableFromDate : false
}

// 判断某个星期几对所有选中时间段是否可用
const isWeekdayAvailableForAllSlots = (weekday: number): boolean => {
  if (scheduleForm.selectedTimeSlots.length === 0) return true

  return scheduleForm.selectedTimeSlots.every(time =>
    isTimeSlotAvailable(weekday, time)
  )
}

// 获取星期几的综合禁用原因
const getWeekdayDisabledReasonForAllSlots = (weekday: number): string => {
  if (scheduleForm.selectedTimeSlots.length === 0) return ''

  const unavailableSlots = scheduleForm.selectedTimeSlots.filter(time =>
    !isTimeSlotAvailable(weekday, time)
  )

  if (unavailableSlots.length === 0) return ''

  if (unavailableSlots.length === 1) {
    return getWeekdayDisabledReason(weekday, unavailableSlots[0])
  }

  return `(${unavailableSlots.length}个时间段冲突)`
}

// 判断某个星期几是否有未来可用的时间段
const hasAvailableFromDateForAllSlots = (weekday: number): boolean => {
  if (scheduleForm.selectedTimeSlots.length === 0) return false

  return scheduleForm.selectedTimeSlots.some(time =>
    hasAvailableFromDate(weekday, time)
  )
}

// 获取星期几的综合可用开始时间
const getWeekdayAvailableFromDateForAllSlots = (weekday: number): string => {
  if (scheduleForm.selectedTimeSlots.length === 0) return ''

  const availableFromDates = scheduleForm.selectedTimeSlots
    .map(time => {
      const slot = availableTimeSlots.value.find(s => s.weekday === weekday && s.time === time)
      return slot?.availableFromDate
    })
    .filter(date => date)
    .sort()

  if (availableFromDates.length === 0) return ''

  return `(${formatDate(availableFromDates[0]!)}起部分可用)`
}

// 获取星期几的提示信息
const getWeekdayTooltip = (weekday: number, time: string): string => {
  const slot = availableTimeSlots.value.find(s => s.weekday === weekday && s.time === time)
  if (!slot) return ''

  if (slot.available) {
    return '可用'
  }

  if (slot.availableFromDate) {
    return `从 ${formatDate(slot.availableFromDate)} 开始可用`
  }

  if (slot.conflictReason) {
    return `冲突原因: ${slot.conflictReason}`
  }

  if (slot.conflictDates && slot.conflictDates.length > 0) {
    return `冲突日期: ${slot.conflictDates.slice(0, 3).map(d => formatDate(d)).join(', ')}${slot.conflictDates.length > 3 ? '...' : ''}`
  }

  return '暂不可用'
}

// 获取星期几禁用原因
const getWeekdayDisabledReason = (weekday: number, time: string): string => {
  const slot = availableTimeSlots.value.find(s => s.weekday === weekday && s.time === time)
  if (!slot) return '(暂不可用)'

  if (slot.conflictReason === '学生课程结束') {
    return '(学生即将结课)'
  }

  return '(该时间段冲突较多)'
}



// 格式化日期显示
const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// 判断某个时间段是否有冲突
const hasTimeSlotConflicts = (time: string): boolean => {
  return availableTimeSlots.value.some(slot =>
    slot.time === time &&
    ((slot.conflictDates && slot.conflictDates.length > 0) || slot.availableFromDate)
  )
}

// 获取冲突摘要
const getTimeSlotConflictSummary = (time: string): string => {
  const conflictSlots = availableTimeSlots.value.filter(slot =>
    slot.time === time &&
    ((slot.conflictDates && slot.conflictDates.length > 0) || slot.availableFromDate)
  )

  if (conflictSlots.length === 0) return ''

  const availableFromSlots = conflictSlots.filter(slot => slot.availableFromDate)
  const conflictSlots2 = conflictSlots.filter(slot => slot.conflictDates && slot.conflictDates.length > 0)

  if (availableFromSlots.length > 0) {
    const earliestDate = availableFromSlots.reduce((earliest, slot) => {
      return !earliest || (slot.availableFromDate && slot.availableFromDate < earliest)
        ? slot.availableFromDate
        : earliest
    }, '')
    return `部分时段从 ${earliestDate} 开始可用`
  }

  if (conflictSlots2.length > 0) {
    const totalConflicts = conflictSlots2.reduce((sum, slot) => sum + (slot.conflictDates?.length || 0), 0)
    return `${totalConflicts} 个时段有冲突`
  }

  return ''
}



// 显示月度课表
const showMonthlySchedule = (teacher: Teacher) => {
  currentMonthTeacher.value = teacher
  currentMonth.value = new Date().getMonth()
  currentYear.value = new Date().getFullYear()
  generateMonthlyScheduleData()
  showMonthlyModal.value = true
}

// 生成月度课表数据
const generateMonthlyScheduleData = () => {
  const year = currentYear.value
  const month = currentMonth.value
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const firstDay = new Date(year, month, 1).getDay()

  monthlyScheduleData.value = []

  // 生成日历数据
  for (let week = 0; week < 6; week++) {
    const weekData = []
    for (let day = 0; day < 7; day++) {
      const dayNumber = week * 7 + day - firstDay + 1

      if (dayNumber > 0 && dayNumber <= daysInMonth) {
        const date = new Date(year, month, dayNumber)
        const dateStr = date.toISOString().split('T')[0]

        // 生成该日的时间段数据
        const timeSlots = generateDayTimeSlots()

        weekData.push({
          day: dayNumber,
          date: dateStr,
          isCurrentMonth: true,
          timeSlots: timeSlots,
          availableCount: timeSlots.filter(slot => slot.available).length,
          bookedCount: timeSlots.filter(slot => slot.booked).length
        })
      } else {
        weekData.push({
          day: dayNumber > 0 ? dayNumber : dayNumber + daysInMonth,
          date: '',
          isCurrentMonth: false,
          timeSlots: [],
          availableCount: 0,
          bookedCount: 0
        })
      }
    }
    monthlyScheduleData.value.push(weekData)
  }
}

// 生成某一天的时间段数据
const generateDayTimeSlots = () => {
  const timeSlots = [
    '09:00-10:00', '10:00-11:00', '11:00-12:00',
    '14:00-15:00', '15:00-16:00', '16:00-17:00', '17:00-18:00',
    '18:00-19:00', '19:00-20:00', '20:00-21:00'
  ]

  return timeSlots.map(time => ({
    time,
    available: Math.random() > 0.3, // 70%概率可用
    booked: Math.random() < 0.2,    // 20%概率已预订
    student: Math.random() < 0.2 ? `学生${Math.floor(Math.random() * 10) + 1}` : null
  }))
}

// 切换到上个月
const previousMonth = () => {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
  generateMonthlyScheduleData()
}

// 切换到下个月
const nextMonth = () => {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
  generateMonthlyScheduleData()
}

// 获取月份名称
const getMonthName = (month: number): string => {
  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  return months[month]
}

// 获取星期名称
const getDayName = (dayIndex: number): string => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[dayIndex]
}

// 点击日期查看详情
const showDayDetail = (dayData: {
  day: number
  date: string
  isCurrentMonth: boolean
  timeSlots: Array<{
    time: string
    available: boolean
    booked: boolean
    student: string | null
  }>
  availableCount: number
  bookedCount: number
}) => {
  if (!dayData.isCurrentMonth || !dayData.date) return

  selectedDayData.value = {
    date: dayData.date,
    day: dayData.day,
    timeSlots: dayData.timeSlots
  }
  showDayDetailModal.value = true
}

// 格式化完整日期显示
const formatFullDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekday = getDayName(date.getDay())
  return `${year}年${month}月${day}日 ${weekday}`
}

// 组件挂载时初始化数据
onMounted(() => {
  initOptions()
})

</script>

<template>
  <div class="teacher-match">
    <h2>智能匹配教师</h2>
    <p class="description">根据您的学习需求和偏好，系统将为您匹配最合适的教师</p>

    <div class="match-container">
      <div class="match-form-section">
        <el-form :model="matchForm" label-position="top" class="match-form">
          <el-form-item label="科目 Subjects" required>
            <el-select v-model="matchForm.subject" placeholder="请选择科目" :loading="loadingOptions">
              <el-option
                v-for="subject in subjects"
                :key="subject.id"
                :label="subject.name"
                :value="subject.name">
                <div class="option-with-icon">
                  <el-icon><Aim /></el-icon>
                  <span>{{ subject.name }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="年级 Grades" required>
            <el-select v-model="matchForm.grade" placeholder="请选择年级" :loading="loadingOptions">
              <el-option
                v-for="grade in grades"
                :key="grade"
                :label="grade"
                :value="grade">
              </el-option>
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
                <div class="teacher-experience">
                  <span class="experience-text">{{ teacher.experience }}年教学经验</span>
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
                <el-button type="primary" @click="showTeacherSchedule(teacher)" size="large">
                  <el-icon><Calendar /></el-icon> 预约课程
                </el-button>
                <el-button type="warning" @click="showMonthlySchedule(teacher)" size="large">
                  <el-icon><Calendar /></el-icon> 查看课表
                </el-button>
                <el-button type="info" plain size="large">
                  <el-icon><Message /></el-icon> 联系教师
                </el-button>
                <el-button type="success" plain size="large" @click="router.push(`/teacher-detail/${teacher.id}`)">
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

    <!-- 教师课表弹窗 -->
    <el-dialog
      v-model="showScheduleModal"
      :title="`${currentTeacher?.name} - 课程安排`"
      width="900px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="schedule-modal-content">
        <div class="schedule-tip">
          <el-alert
            title="选课说明"
            description="选择固定的上课时间段，系统会自动安排周期性课程"
            type="info"
            show-icon
            :closable="false"
          />
        </div>

        <el-form :model="scheduleForm" label-width="120px" class="schedule-form">
          <el-form-item label="预约类型">
            <el-radio-group v-model="scheduleForm.bookingType" size="large">
              <el-radio-button label="single">单次预约</el-radio-button>
              <el-radio-button label="recurring">周期性预约</el-radio-button>
            </el-radio-group>
            <div class="form-item-tip">选择单次预约或周期性预约</div>
          </el-form-item>

          <!-- 单次预约表单 -->
          <template v-if="scheduleForm.bookingType === 'single'">
            <el-form-item label="上课日期">
              <el-date-picker
                v-model="scheduleForm.singleDate"
                type="date"
                placeholder="选择上课日期"
                :disabled-date="(date) => date < new Date()"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 200px"
              />
            </el-form-item>

            <el-form-item label="上课时间">
              <div style="display: flex; gap: 10px; align-items: center;">
                <el-time-select
                  v-model="scheduleForm.singleStartTime"
                  start="08:00"
                  step="00:30"
                  end="22:00"
                  placeholder="开始时间"
                  style="width: 120px"
                />
                <span>至</span>
                <el-time-select
                  v-model="scheduleForm.singleEndTime"
                  start="08:00"
                  step="00:30"
                  end="22:00"
                  placeholder="结束时间"
                  style="width: 120px"
                />
              </div>
            </el-form-item>
          </template>

          <!-- 周期性预约表单 -->
          <template v-else>
            <el-form-item label="选择时间段">
              <div class="form-item-tip">点击选择您偏好的上课时间段 (可多选)</div>
            <div class="time-slot-selection">
              <div
                v-for="time in getUniqueTimeSlots()"
                :key="time"
                :class="[
                  'time-slot-card',
                  {
                    'selected': scheduleForm.selectedTimeSlots.includes(time),
                    'has-conflicts': hasTimeSlotConflicts(time)
                  }
                ]"
                @click="selectTimeSlot(time)"
              >
                <div class="slot-time">{{ time }}</div>
                <div class="slot-weekdays">
                  <span
                    v-for="weekday in [1, 2, 3, 4, 5, 6, 0]"
                    :key="weekday"
                    :class="[
                      'weekday-indicator',
                      {
                        'available': isTimeSlotAvailable(weekday, time),
                        'unavailable': !isTimeSlotAvailable(weekday, time),
                        'future-available': hasAvailableFromDate(weekday, time)
                      }
                    ]"
                    :title="getWeekdayTooltip(weekday, time)"
                  >
                    {{ getWeekdayShort(weekday) }}
                  </span>
                </div>
                <div class="slot-availability-info" v-if="hasTimeSlotConflicts(time)">
                  <div class="conflict-summary">
                    {{ getTimeSlotConflictSummary(time) }}
                  </div>
                </div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="选择星期" v-if="scheduleForm.selectedTimeSlots.length > 0">
            <div class="form-item-tip">选择每周的哪几天上课 (可横向滑动查看所有选项)</div>
            <div class="weekday-selection">
              <el-checkbox-group v-model="scheduleForm.selectedWeekdays">
                <el-checkbox
                  v-for="weekday in [1, 2, 3, 4, 5, 6, 0]"
                  :key="weekday"
                  :label="weekday"
                  :disabled="!isWeekdayAvailableForAllSlots(weekday)"
                >
                  <span class="weekday-label">{{ getWeekdayName(weekday) }}</span>
                  <span v-if="!isWeekdayAvailableForAllSlots(weekday)" class="disabled-reason">
                    {{ getWeekdayDisabledReasonForAllSlots(weekday) }}
                  </span>
                  <span v-else-if="hasAvailableFromDateForAllSlots(weekday)" class="available-from-date">
                    {{ getWeekdayAvailableFromDateForAllSlots(weekday) }}
                  </span>
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </el-form-item>

          <el-form-item label="开始日期">
            <el-date-picker
              v-model="scheduleForm.startDate"
              type="date"
              placeholder="选择开始日期"
              :disabled-date="(date) => date < new Date()"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="calculateEndDate"
              style="width: 200px"
            />
          </el-form-item>

          <el-form-item label="课程次数">
            <el-input-number
              v-model="scheduleForm.sessionCount"
              :min="1"
              :max="50"
              @change="calculateEndDate"
              style="width: 200px"
            />
            <span class="session-info">次课</span>
          </el-form-item>

          <el-form-item label="结束日期">
            <el-input
              v-model="scheduleForm.endDate"
              placeholder="自动计算"
              readonly
              style="width: 200px"
            />
          </el-form-item>

          <el-form-item label="课程预览" v-if="scheduleForm.selectedWeekdays.length > 0 && scheduleForm.selectedTimeSlots.length > 0">
            <div class="schedule-preview">
              <div class="preview-title">课程安排：</div>
              <div class="preview-content">
                <div class="preview-weekdays">
                  <span class="preview-label">上课时间：</span>
                  <el-tag type="success" size="large" class="schedule-tag">
                    每周{{ scheduleForm.selectedWeekdays.map(day => getWeekdayName(day)).join('、') }}
                  </el-tag>
                </div>
                <div class="preview-timeslots">
                  <span class="preview-label">时间段：</span>
                  <div class="timeslots-container">
                    <el-tag
                      v-for="timeSlot in scheduleForm.selectedTimeSlots"
                      :key="timeSlot"
                      type="primary"
                      size="large"
                      class="timeslot-tag"
                    >
                      {{ timeSlot }}
                    </el-tag>
                  </div>
                </div>
              </div>
              <div class="preview-details">
                <span>共 {{ scheduleForm.sessionCount }} 次课</span>
                <span>每周 {{ scheduleForm.selectedWeekdays.length }} 天</span>
                <span>每天 {{ scheduleForm.selectedTimeSlots.length }} 个时间段</span>
                <span>约 {{ Math.ceil(scheduleForm.sessionCount / (scheduleForm.selectedWeekdays.length * scheduleForm.selectedTimeSlots.length)) }} 周完成</span>
              </div>
            </div>
          </el-form-item>
          </template>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showScheduleModal = false">取消</el-button>
          <el-button
            type="primary"
            @click="createBookingRequest"
            :disabled="
              scheduleForm.bookingType === 'single'
                ? !scheduleForm.singleDate || !scheduleForm.singleStartTime || !scheduleForm.singleEndTime
                : scheduleForm.selectedTimeSlots.length === 0 || scheduleForm.selectedWeekdays.length === 0
            "
          >
            确认预约
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 月度课表弹窗 -->
    <el-dialog
      v-model="showMonthlyModal"
      :title="`${currentMonthTeacher?.name} - ${getMonthName(currentMonth)} ${currentYear}年课表`"
      width="900px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="monthly-schedule-content">
        <div class="monthly-header">
          <el-button-group>
            <el-button @click="previousMonth">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <el-button>{{ getMonthName(currentMonth) }} {{ currentYear }}年</el-button>
            <el-button @click="nextMonth">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-button-group>
        </div>

        <div class="monthly-calendar">
          <div class="week-header">
            <div v-for="day in [0, 1, 2, 3, 4, 5, 6]" :key="day" class="week-header-day">
              {{ getDayName(day) }}
            </div>
          </div>

          <div class="calendar-grid">
            <div v-for="(week, weekIndex) in monthlyScheduleData" :key="weekIndex" class="week-row">
              <div v-for="(day, dayIndex) in week" :key="dayIndex"
                   :class="['day-cell', { 'other-month': !day.isCurrentMonth, 'clickable': day.isCurrentMonth }]"
                   @click="showDayDetail(day)">
                <div class="day-header">
                  <span class="day-number">{{ day.day }}</span>
                </div>
                <div class="day-content" v-if="day.isCurrentMonth">
                  <div class="day-summary">
                    <span class="available-count">{{ day.availableCount }}可用</span>
                    <span class="booked-count">{{ day.bookedCount }}已订</span>
                  </div>
                  <div class="time-slots-preview">
                    <div
                      v-for="slot in day.timeSlots.slice(0, 3)"
                      :key="slot.time"
                      :class="['mini-slot', { 'available': slot.available, 'booked': slot.booked }]"
                      :title="`${slot.time} - ${slot.booked ? '已预订' : '可用'}`"
                    >
                      {{ slot.time.split('-')[0] }}
                    </div>
                    <div v-if="day.timeSlots.length > 3" class="more-slots">
                      +{{ day.timeSlots.length - 3 }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showMonthlyModal = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 日期详情弹窗 -->
    <el-dialog
      v-model="showDayDetailModal"
      :title="selectedDayData ? `${selectedDayData.day}日 - ${formatFullDate(selectedDayData.date)}` : '日期详情'"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="day-detail-content" v-if="selectedDayData">
        <div class="day-stats">
          <div class="stat-item">
            <span class="stat-label">可用时段：</span>
            <span class="stat-value available">{{ selectedDayData.timeSlots.filter(slot => slot.available).length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">已预订：</span>
            <span class="stat-value booked">{{ selectedDayData.timeSlots.filter(slot => slot.booked).length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">总时段：</span>
            <span class="stat-value total">{{ selectedDayData.timeSlots.length }}</span>
          </div>
        </div>

        <div class="time-slots-detail">
          <h4>详细时段安排</h4>
          <div class="slots-grid">
            <div
              v-for="slot in selectedDayData.timeSlots"
              :key="slot.time"
              :class="['time-slot-detail', { 'available': slot.available, 'booked': slot.booked }]"
            >
              <div class="slot-time">{{ slot.time }}</div>
              <div class="slot-status">
                <span v-if="slot.booked" class="status-booked">已预订</span>
                <span v-else-if="slot.available" class="status-available">可预约</span>
                <span v-else class="status-unavailable">不可用</span>
              </div>
              <div v-if="slot.student" class="slot-student">{{ slot.student }}</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDayDetailModal = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
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

.teacher-experience {
  display: flex;
  align-items: center;
}

.experience-text {
  color: #666;
  font-size: 14px;
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
  .time-slot-selection {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .time-slot-card {
    padding: 12px;
    min-height: 50px;
  }

  .slot-time {
    font-size: 13px;
    margin-bottom: 6px;
  }

  .weekday-indicator {
    font-size: 9px;
    padding: 1px 4px;
    min-width: 14px;
  }

  .weekday-selection {
    padding: 15px;
  }

  .el-checkbox-group {
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
  }

  .el-checkbox-group .el-checkbox {
    padding: 8px 4px;
    min-height: 36px;
  }

  .weekday-label {
    font-size: 12px;
  }

  .disabled-reason {
    font-size: 9px;
  }

  .preview-details {
    flex-direction: column;
    gap: 10px;
  }

  .schedule-form .el-form-item {
    margin-bottom: 20px;
  }
}

@media (max-width: 480px) {
  .time-slot-selection {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .time-slot-card {
    padding: 12px;
    min-height: 50px;
  }

  .slot-time {
    font-size: 12px;
  }

  .weekday-indicator {
    font-size: 8px;
    padding: 1px 3px;
    min-width: 12px;
  }

  .weekday-selection {
    padding: 12px;
  }

  .el-checkbox-group {
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
  }

  .el-checkbox-group .el-checkbox {
    padding: 8px 4px;
    min-height: 34px;
  }

  .weekday-label {
    font-size: 11px;
  }

  .disabled-reason {
    font-size: 8px;
    margin-top: 1px;
  }

  .schedule-modal-content {
    padding: 15px;
  }
}

/* 优化表单项间距 */
.schedule-form .el-form-item {
  margin-bottom: 25px;
}

/* 图标对齐 */
.el-icon + span {
  margin-left: 5px;
}

/* 优化按钮组样式 */
.el-button-group {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 课表弹窗样式 */
.schedule-modal-content {
  padding: 25px;
}

.schedule-tip {
  margin-bottom: 30px;
}

.schedule-form {
  margin-top: 20px;
}

.form-item-tip {
  font-size: 13px;
  color: #999;
  margin-bottom: 10px;
}

.time-slot-selection {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  margin-top: 15px;
}

.time-slot-card {
  background-color: #f8f8f8;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 85px;
  justify-content: center;
}

.time-slot-card:hover {
  background-color: #e0e0e0;
  border-color: #ccc;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.time-slot-card.selected {
  background-color: #f6ffed;
  border-color: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
  position: relative;
}

.time-slot-card.selected::after {
  content: '✓';
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: #52c41a;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.time-slot-card.has-conflicts {
  border-color: #ffd666;
  background-color: #fffbe6;
}

.slot-time {
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  font-size: 14px;
}

.slot-weekdays {
  display: flex;
  justify-content: center;
  gap: 4px;
  font-size: 11px;
  flex-wrap: wrap;
}

.weekday-indicator {
  padding: 2px 6px;
  border-radius: 3px;
  background-color: #f0f0f0;
  color: #666;
  font-size: 10px;
  font-weight: 500;
  min-width: 16px;
  text-align: center;
  line-height: 1.2;
}

.weekday-indicator.available {
  background-color: #e6f7ff;
  color: #1890ff;
}

.weekday-indicator.unavailable {
  background-color: #f5f5f5;
  color: #bbb;
  text-decoration: line-through;
}

.weekday-indicator.future-available {
  background-color: #e1f3d8; /* 浅绿色背景 */
  color: #67c23a; /* 深绿色文字 */
}

.slot-availability-info {
  margin-top: 8px;
  font-size: 11px;
  color: #999;
  text-align: center;
  max-width: 100%;
  word-wrap: break-word;
}

.conflict-summary {
  color: #e6a23c;
  font-weight: 500;
  line-height: 1.3;
}

.weekday-selection {
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 20px;
  margin-top: 10px;
  border: 1px solid #e8e8e8;
  overflow-x: auto;
  overflow-y: hidden;
}

/* 自定义滚动条样式 */
.weekday-selection::-webkit-scrollbar {
  height: 6px;
}

.weekday-selection::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.weekday-selection::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.weekday-selection::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.session-info {
  margin-left: 10px;
  color: #666;
}

/* 优化复选框组样式 */
.el-checkbox-group {
  display: flex;
  gap: 12px;
  margin-top: 10px;
  min-width: 700px;
  padding-bottom: 5px;
}

.el-checkbox-group .el-checkbox {
  margin-right: 0;
  white-space: nowrap;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 12px 16px;
  transition: all 0.3s;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  flex: 0 0 auto;
  min-width: 90px;
}

.el-checkbox-group .el-checkbox:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.el-checkbox-group .el-checkbox.is-checked {
  border-color: #52c41a;
  background-color: #f6ffed;
}

.el-checkbox-group .el-checkbox.is-disabled {
  background-color: #f5f5f5;
  border-color: #d9d9d9;
  opacity: 0.6;
}

.el-checkbox-group .el-checkbox .el-checkbox__label {
  padding-left: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.2;
}

.weekday-label {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.disabled-reason {
  color: #999;
  font-size: 10px;
  font-weight: normal;
  margin-top: 2px;
  text-align: center;
}

.available-from-date {
  font-size: 10px;
  color: #67c23a;
  margin-top: 2px;
  text-align: center;
  font-weight: 500;
}

.schedule-preview {
  background-color: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 8px;
  padding: 20px;
  margin-top: 10px;
}

.preview-title {
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
}

.preview-content {
  margin-bottom: 15px;
}

.preview-weekdays {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.preview-timeslots {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
}

.preview-label {
  font-weight: 500;
  color: #333;
  font-size: 14px;
  white-space: nowrap;
}

.timeslots-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.schedule-tag {
  font-size: 16px;
  padding: 8px 16px;
}

.timeslot-tag {
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 6px;
  background-color: #e1f3d8;
  color: #67c23a;
  border: 1px solid #d9f7be;
}

.preview-details {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.preview-details span {
  background-color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .time-slot-selection {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
  }

  .el-checkbox-group {
    gap: 10px;
    min-width: 600px;
  }

  .el-checkbox-group .el-checkbox {
    min-width: 80px;
    padding: 10px 14px;
  }

  .weekday-selection {
    padding: 18px;
  }
}

@media (max-width: 768px) {
  .time-slot-selection {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .time-slot-card {
    padding: 15px;
    min-height: 55px;
  }

  .slot-time {
    font-size: 13px;
    margin-bottom: 6px;
  }

  .weekday-indicator {
    font-size: 9px;
    padding: 1px 4px;
    min-width: 14px;
  }

  .weekday-selection {
    padding: 15px;
  }

  .el-checkbox-group {
    gap: 8px;
    min-width: 500px;
  }

  .el-checkbox-group .el-checkbox {
    padding: 8px 12px;
    min-height: 38px;
    min-width: 70px;
  }

  .weekday-label {
    font-size: 12px;
  }

  .disabled-reason {
    font-size: 9px;
  }

  .preview-details {
    flex-direction: column;
    gap: 10px;
  }

  .schedule-form .el-form-item {
    margin-bottom: 20px;
  }
}

@media (max-width: 480px) {
  .time-slot-selection {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .time-slot-card {
    padding: 12px;
    min-height: 50px;
  }

  .slot-time {
    font-size: 12px;
  }

  .weekday-indicator {
    font-size: 8px;
    padding: 1px 3px;
    min-width: 12px;
  }

  .weekday-selection {
    padding: 12px;
  }

  .el-checkbox-group {
    gap: 6px;
    min-width: 420px;
  }

  .el-checkbox-group .el-checkbox {
    padding: 6px 10px;
    min-height: 34px;
    min-width: 60px;
  }

  .weekday-label {
    font-size: 11px;
  }

  .disabled-reason {
    font-size: 8px;
    margin-top: 1px;
  }

  .schedule-modal-content {
    padding: 15px;
  }
}

/* 月度课表弹窗样式 */
.monthly-schedule-content {
  padding: 20px;
}

.monthly-header {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.monthly-header .el-button-group {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.monthly-calendar {
  width: 100%;
}

.week-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
  margin-bottom: 15px;
  padding: 10px 0;
  border-bottom: 2px solid #f0f0f0;
}

.week-header-day {
  font-weight: 600;
  color: #333;
  text-align: center;
  font-size: 14px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}

.week-row {
  display: contents;
}

.day-cell {
  background-color: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: all 0.3s;
}

.day-cell:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.day-cell.other-month {
  background-color: #f8f8f8;
  color: #ccc;
  opacity: 0.5;
}

.day-cell.clickable {
  cursor: pointer;
}

.day-cell.clickable:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.day-header {
  position: absolute;
  top: 8px;
  left: 8px;
}

.day-number {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.day-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  margin-top: 25px;
}

.day-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding: 4px 8px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.available-count {
  font-size: 11px;
  color: #67c23a;
  font-weight: 500;
}

.booked-count {
  font-size: 11px;
  color: #faad14;
  font-weight: 500;
}

.time-slots-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.mini-slot {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 3px;
  background-color: #f0f0f0;
  color: #666;
  font-weight: 500;
  min-width: 20px;
  text-align: center;
}

.mini-slot.available {
  background-color: #e1f3d8;
  color: #67c23a;
}

.mini-slot.booked {
  background-color: #fffbe6;
  color: #faad14;
}

.more-slots {
  font-size: 10px;
  color: #999;
  font-weight: 500;
  padding: 2px 4px;
  background-color: #f0f0f0;
  border-radius: 3px;
}

.day-detail-content {
  padding: 20px;
}

.day-stats {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.stat-item {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
}

.stat-value.available {
  color: #67c23a;
}

.stat-value.booked {
  color: #faad14;
}

.stat-value.total {
  color: #409eff;
}

.time-slots-detail h4 {
  margin-bottom: 20px;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.slots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.time-slot-detail {
  background-color: #fff;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  padding: 15px;
  text-align: center;
  transition: all 0.3s;
}

.time-slot-detail:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.time-slot-detail.available {
  border-color: #67c23a;
  background-color: #f6ffed;
}

.time-slot-detail.booked {
  border-color: #faad14;
  background-color: #fffbe6;
}

.slot-time {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.slot-status {
  margin-bottom: 8px;
}

.status-available {
  color: #67c23a;
  font-weight: 500;
  background-color: #e1f3d8;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-booked {
  color: #faad14;
  font-weight: 500;
  background-color: #ffe58f;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-unavailable {
  color: #999;
  font-weight: 500;
  background-color: #f0f0f0;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.slot-student {
  font-size: 12px;
  color: #666;
  font-style: italic;
}
</style>


