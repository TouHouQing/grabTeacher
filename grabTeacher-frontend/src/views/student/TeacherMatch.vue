<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Connection, Male, Female, Message, Loading, View, ArrowLeft, ArrowRight, InfoFilled, Refresh, Sunrise, Sunny, Moon, Lock, Check, Clock, Close, Warning, SuccessFilled, ArrowDown, Document, Star } from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { teacherAPI, subjectAPI, bookingAPI, publicTeacherLevelAPI, publicGradeAPI, publicTeachingLocationAPI } from '../../utils/api'
const route = useRoute()
const isTrialMode = computed(() => route.path.includes('/student-center/trial'))
const isRecurringMode = computed(() => route.path.includes('/student-center/match'))

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
  experience: number
  description: string
  avatar: string
  tags: string[]
  schedule: string[]
  recommendationScore?: number // 推荐度（新）
  matchScore?: number // 兼容旧字段
  gender: string
  hourlyRate?: number
  educationBackground?: string
  specialties?: string
  isVerified?: boolean
  timeMatchScore?: number // 时间匹配度
  level?: string // 教师级别
  subjects?: string[] // 教授的全部科目（卡片展示前若干个）
  subjectsCount?: number // 科目总数（用于 +N 和按需加载）
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
  time?: string // 兼容旧格式
  timeSlot?: string // 新格式
  available: boolean
  conflictDates?: string[] // 冲突的具体日期
  availableFromDate?: string // 从什么时候开始可用
  conflictEndDate?: string // 冲突结束日期
  conflictReason?: string // 冲突原因
  isTeacherAvailable?: boolean // 是否在教师可预约时间范围内
}

const matchForm = reactive({
  subject: '',
  grade: '',
  preferredWeekdays: [] as number[], // 偏好的星期几
  preferredTimeSlots: [] as string[], // 偏好的时间段（由 selectedPeriods 自动映射）
  gender: '', // 使用空字符串作为默认值
  teacherLevel: '' // 教师级别，空字符串表示不限级别
})

// 三段时间偏好：morning/afternoon/evening
const selectedPeriods = ref<string[]>([])

watch(selectedPeriods, (periods) => {
  const slots: string[] = []
  if (periods.includes('morning')) {
    slots.push('08:00-10:00', '10:00-12:00')
  }
  if (periods.includes('afternoon')) {
    slots.push('13:00-15:00', '15:00-17:00')
  }
  if (periods.includes('evening')) {
    slots.push('17:00-19:00', '19:00-21:00')
  }
  matchForm.preferredTimeSlots = slots
}, { immediate: true })

// 试听匹配前置参数
const trialSearchDate = ref<string>('')

// 是否从匹配页预设了试听日期与时间段
const isTrialPreset = computed(() => isTrialMode.value && !!trialSearchDate.value && selectedPeriods.value.length > 0)
//

//

//


//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//

//
//  
//  
//  
// 
// 

//  
const isInitializingTrialPreset = ref(false)

const loading = ref(false)
const showResults = ref(false)
const matchedTeachers = ref<Teacher[]>([])
const subjects = ref<any[]>([])
const teacherLevels = ref<any[]>([])
const grades = ref<any[]>([])
const loadingOptions = ref(false)

// 添加性别选择组件的引用和重置键
const genderRadioGroupRef = ref()
const genderResetKey = ref(0)
const formResetKey = ref(0)

// 新增课表相关数据
const showScheduleModal = ref(false)
const currentTeacher = ref<Teacher | null>(null)
const currentTeacherSchedule = ref<TeacherSchedule | null>(null)

// 授课地点选择
const activeTeachingLocations = ref<any[]>([])
const allowedTeachingLocations = ref<Array<{ value: string | number; label: string }>>([])
const selectedTeachingLocation = ref<string | number>('')

// 课程选择相关数据
const teacherCourses = ref<any[]>([])
const selectedCourse = ref<any | null>(null)
const loadingCourses = ref(false)

// 新增周期性选课数据
const recurringSchedules = ref<RecurringSchedule[]>([])
const availableTimeSlots = ref<AvailableTimeSlot[]>([])
const selectedRecurringSchedule = ref<RecurringSchedule | null>(null)

// 试听课时间段选择相关数据
const selectedTimePeriod = ref<string>('') // 选中的时间段类型：morning/afternoon/evening
const availableTrialSlotsByPeriod = ref<string[]>([]) // 当前选中时间段类型下的可用时间段
const trialPeriodOptions = computed<string[]>(() => {
  return selectedPeriods.value.length > 0
    ? [...selectedPeriods.value]
    : ['morning', 'afternoon', 'evening']
})

// 获取明天的日期作为默认开始日期
const getDefaultStartDate = () => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return tomorrow.toISOString().split('T')[0]
}

const scheduleForm = reactive({
  bookingType: 'recurring' as 'trial' | 'recurring', // 预约类型：试听或周期性
  selectedWeekdays: [] as number[],
  selectedTimeSlots: [] as string[], // 改为数组支持多选
  startDate: getDefaultStartDate(), // 默认设置为明天
  endDate: '',
  sessionCount: 12, // 默认12次课
  // 试听课相关字段
  trialDate: '',
  trialStartTime: '',
  trialEndTime: '',
  selectedDurationMinutes: 90 // 默认选择1.5小时
})

onMounted(() => {
  // 根据路由强制设置模式
  scheduleForm.bookingType = isTrialMode.value ? 'trial' : 'recurring'
})

// 准确的时间匹配度信息
const accurateMatchScoreInfo = ref<{ score: number, message: string, type: string, conflicts?: any[] } | null>(null)
const matchScoreLoading = ref(false)
const showMatchDetails = ref(false)
const showConflictDetails = ref(false)

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
// 工具：解析时间段开始小时并映射上午/下午/晚上
const getStartHourFromTime = (timeStr: string): number | null => {
  if (!timeStr) return null
  const start = timeStr.split('-')[0]
  if (!start) return null
  const h = parseInt(start.split(':')[0], 10)
  return Number.isFinite(h) ? h : null
}

const getPeriodByHour = (hour: number): 'morning' | 'afternoon' | 'evening' | null => {
  if (hour >= 8 && hour < 12) return 'morning'
  if (hour >= 13 && hour < 17) return 'afternoon'
  if (hour >= 17 && hour < 21) return 'evening'
  return null
}

// 按学生勾选的上午/下午/晚上过滤月度课表时间段（多选/单选均支持；未勾选则不过滤）
const filterTimeSlotsBySelectedPeriods = (
  slots: Array<{ timeSlot?: string; time?: string; available?: boolean; booked?: boolean }>
) => {
  const allowed = selectedPeriods.value.length > 0 ? selectedPeriods.value : ['morning', 'afternoon', 'evening']
  return (slots || []).filter((s) => {
    const t = (s.timeSlot || s.time || '')
    const h = getStartHourFromTime(t)
    if (h === null) return false
    const p = getPeriodByHour(h)
    return p ? allowed.includes(p) : false
  })
}


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
  if (!matchForm.subject) {
    ElMessage.warning('请选择科目')
    return
  }
  if (!matchForm.grade) {
    ElMessage.warning('请选择年级')
    return
  }

  // 试听模式下，要求先选择日期与偏好时间段（上午/下午/晚上，可多选）
  if (isTrialMode.value) {
    if (!trialSearchDate.value) {
      ElMessage.warning('请先选择上课日期')
      return
    }
    if (selectedPeriods.value.length === 0) {
      ElMessage.warning('请选择偏好上课时间段（可多选上午/下午/晚上）')
      return
    }
  }

  loading.value = true
  showResults.value = false

  try {
    // 构建匹配请求参数
    // 试课模式强制限定日期与2小时间段集合
    let preferredTimeSlots = matchForm.preferredTimeSlots.length > 0 ? matchForm.preferredTimeSlots : undefined
    let preferredDateStart: string | undefined
    let preferredDateEnd: string | undefined
    if (isTrialMode.value) {
      const segSlots: string[] = []
      if (selectedPeriods.value.includes('morning')) segSlots.push('08:00-10:00', '10:00-12:00')
      if (selectedPeriods.value.includes('afternoon')) segSlots.push('13:00-15:00', '15:00-17:00')
      if (selectedPeriods.value.includes('evening')) segSlots.push('17:00-19:00', '19:00-21:00')
      preferredTimeSlots = segSlots
      preferredDateStart = trialSearchDate.value
      preferredDateEnd = trialSearchDate.value
    }

    const matchRequest = {
      subject: matchForm.subject,
      grade: matchForm.grade || undefined,
      preferredWeekdays: matchForm.preferredWeekdays.length > 0 ? matchForm.preferredWeekdays : undefined,
      preferredTimeSlots,
      preferredDateStart,
      preferredDateEnd,
      preferredGender: matchForm.gender || undefined,
      teacherLevel: matchForm.teacherLevel || undefined,
      limit: 5
    }

    // 调用后端API
    console.log('发送匹配请求:', matchRequest)

    // 添加超时处理
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => reject(new Error('请求超时，请检查后端服务是否正常运行')), 10000)
    })

    const result = await Promise.race([
      teacherAPI.matchTeachers(matchRequest),
      timeoutPromise
    ])

    // 详细的调试日志
    console.log('=== 匹配结果调试信息 ===')
    console.log('完整响应对象:', result)
    console.log('响应对象的所有键:', Object.keys(result))
    console.log('result.success:', result.success, '(类型:', typeof result.success, ')')
    console.log('result.data:', result.data)
    console.log('result.data 类型:', typeof result.data)
    console.log('result.data 是否为数组:', Array.isArray(result.data))
    if (result.data && Array.isArray(result.data)) {
      console.log('result.data 长度:', result.data.length)
      console.log('第一个教师数据:', result.data[0])
    }
    console.log('=== 调试信息结束 ===')

    // 检查响应格式和数据 - 修复条件判断
    if (result && result.success === true && result.data && Array.isArray(result.data)) {
      console.log('进入成功分支，数据长度:', result.data.length)

      if (result.data.length > 0) {
        console.log('开始转换教师数据')

        // 转换后端数据格式为前端需要的格式
        const teachers = result.data.map((teacher: any) => ({
          id: teacher.id,
          name: teacher.name,
          subject: teacher.subject,
          experience: teacher.experience,
          description: teacher.description,
          avatar: teacher.avatar || teacherImages.teacherBoy1, // 使用默认头像如果没有
          tags: Array.isArray(teacher.tags) ? teacher.tags : [], // 确保 tags 是数组
          schedule: Array.isArray(teacher.schedule) ? teacher.schedule : ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00'],
          recommendationScore: teacher.recommendationScore ?? teacher.matchScore,
          matchScore: teacher.matchScore, // 兼容展示
          timeMatchScore: teacher.timeMatchScore, // 时间匹配度
          hourlyRate: teacher.hourlyRate,
          educationBackground: teacher.educationBackground,
          specialties: teacher.specialties,
          isVerified: teacher.isVerified,
          gender: teacher.gender || '不愿透露', // 提供默认性别
          level: teacher.level,
          subjects: Array.isArray(teacher.subjects) ? teacher.subjects : (teacher.subject ? [teacher.subject] : []),
          subjectsCount: typeof teacher.subjectsCount === 'number' ? teacher.subjectsCount : (Array.isArray(teacher.subjects) ? teacher.subjects.length : (teacher.subject ? 1 : 0))
        }))

        console.log('转换后的教师数据:', teachers)

        // 先设置数据，再显示结果
        matchedTeachers.value = teachers

        // 使用 nextTick 确保数据更新完成后再显示结果
        nextTick(() => {
          showResults.value = true
          ElMessage.success(`为您匹配到了 ${teachers.length} 位适合的教师`)
        })
      } else {
        // 数据为空数组
        console.log('匹配到空数组')
        ElMessage.warning('您所选择的上课时间或教师级别无匹配教师，请重新输入筛选条件')
        showResults.value = false
      }
    } else {
      // 请求失败或其他错误情况
      console.log('匹配失败的详细信息:', result)
      ElMessage.error(result?.message || '匹配失败，请稍后重试')
      showResults.value = false
    }
  } catch (error) {
    console.error('匹配教师失败:', error)
    ElMessage.error(`匹配失败: ${error.message || '请检查网络连接'}`)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  console.log('开始重置表单')

  // 重置表单字段
  matchForm.subject = ''
  matchForm.preferredWeekdays = []
  matchForm.preferredTimeSlots = []
  selectedPeriods.value = []
  matchForm.gender = ''
  matchForm.teacherLevel = ''
  matchForm.grade = ''
  trialSearchDate.value = ''

  // 重置匹配结果和状态
  showResults.value = false
  matchedTeachers.value = []
  loading.value = false

  // 强制重新渲染表单组件
  formResetKey.value += 1

  console.log('表单重置完成，当前性别值:', matchForm.gender)
  ElMessage.success('表单已重置')
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

// 获取教师级别选项
const loadTeacherLevels = async () => {
  try {
    const result = await publicTeacherLevelAPI.list()
    if (result.success && result.data) {
      teacherLevels.value = result.data
    }
  } catch (error) {
    console.error('获取教师级别列表失败:', error)
  }
}

// 获取年级选项（公开）
const loadGrades = async () => {
  try {
    const result = await publicGradeAPI.list()
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
    await Promise.all([
      loadSubjects(),
      loadTeacherLevels(),
      loadGrades()
    ])
  } finally {
    loadingOptions.value = false
  }
}

// 根据路由参数预加载指定教师并直接展示
const splitToTags = (val?: string) => {
  if (!val) return []
  return val.split(/[，,\s]+/).filter(Boolean).slice(0, 10)
}

const preloadFromQuery = async () => {
  const teacherIdParam = route.query.teacherId as string | undefined
  const courseIdParam = route.query.courseId as string | undefined
  if (!teacherIdParam) return

  const teacherId = Number(teacherIdParam)
  if (!Number.isFinite(teacherId)) return

  try {
    const res = await teacherAPI.getDetail(teacherId)
    if (res.success && res.data) {
      const t = res.data
      const teacher: Teacher = {
        id: t.id,
        name: t.realName || t.username || '教师',
        subject: (t.subjects && t.subjects.length > 0 ? t.subjects[0] : ''),
        experience: t.teachingExperience || 0,
        description: t.introduction || '',
        avatar: t.avatarUrl || '@/assets/pictures/teacherBoy1.jpeg',
        tags: splitToTags(t.specialties) || (t.subjects || []),
        schedule: [],
        recommendationScore: 100,
        matchScore: 100,
        gender: t.gender || '不愿透露'
      }

      matchedTeachers.value = [teacher]
      showResults.value = true

      // 预选教师并加载课程
      currentTeacher.value = teacher
      await loadTeacherCourses(teacherId)

      if (courseIdParam) {
        const cid = Number(courseIdParam)
        if (Number.isFinite(cid)) {
          const found = teacherCourses.value.find((c: any) => c.id === cid)
          if (found) {
            selectedCourse.value = found
          }
        }
      }
    }
  } catch (e) {
    console.error('预加载教师失败:', e)
    ElMessage.error('无法加载指定教师，请稍后重试')
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

// 根据星期几返回可用的时间段（现在所有天都使用完整的时间段）
const getTimeSlotsForDay = (dayOfWeek: number): string[] => {
  // 所有天都使用完整的时间段列表（从早上9点到晚上9点）
  return getCompleteTimeSlots()
}

// 获取课程时长（小时）
const getCourseDurationHours = () => {
  if (!selectedCourse.value) return 0

    // 初始化试课预设，避免watch触发清空
    isInitializingTrialPreset.value = true
  // 优先使用课程设置的时长，如果没有则使用学生选择的时长
  const durationMinutes = selectedCourse.value.durationMinutes || scheduleForm.selectedDurationMinutes
  if (!durationMinutes) return 0

  return (durationMinutes / 60).toFixed(1)
}

// 计算总花费
const calculateTotalCost = () => {
  if (!selectedCourse.value || !selectedCourse.value.price) {
    return 0
  }

  const pricePerHour = selectedCourse.value.price // M豆/小时
  const durationMinutes = selectedCourse.value.durationMinutes || scheduleForm.selectedDurationMinutes
  if (!durationMinutes) {
    return 0 // 没有时长信息，无法计算
  }

  const durationHours = durationMinutes / 60 // 每次课的小时数
  const pricePerSession = pricePerHour * durationHours // 每次课的价格

  if (scheduleForm.bookingType === 'trial') {
    return 0 // 试听课免费
  } else {
    return pricePerSession * scheduleForm.sessionCount // 总价格
  }
}

// 格式化价格显示
const formatTotalCost = () => {
  const totalCost = calculateTotalCost()
  if (totalCost === 0) {
    return scheduleForm.bookingType === 'trial' ? '免费试听' : '价格面议'
  }
  return `${totalCost.toFixed(0)}M豆`
}

// 加载授课地点（根据教师配置）
async function loadAllowedTeachingLocations() {
  if (!currentTeacher.value) return
  try {
    // 先取公共可用的地点字典
    const res = await publicTeachingLocationAPI.getActive()
    const list = res?.success ? (res.data || []) : []
    activeTeachingLocations.value = list

    // 拉取教师详情，获取其支持的线上与线下地点ID
    const detail = await teacherAPI.getDetail(currentTeacher.value.id)
    const options: Array<{ value: string | number; label: string }> = []
    if (detail?.success && detail.data) {
      const data = detail.data
      if (data.supportsOnline) {
        options.push({ value: 'online', label: '线上' })
      }
      const ids: number[] = Array.isArray(data.teachingLocationIds) ? data.teachingLocationIds : []
      ids.forEach((id: number) => {
        const item = list.find((x: any) => x.id === id)
        if (item) options.push({ value: id, label: item.name })
      })
    }

    allowedTeachingLocations.value = options
    if (!selectedTeachingLocation.value && options.length > 0) {
      selectedTeachingLocation.value = options[0].value
    }
  } catch (e) {
    console.error('加载授课地点失败:', e)
  }
}

// 获取教师课程列表
const loadTeacherCourses = async (teacherId: number) => {
  try {
    loadingCourses.value = true
    const result = await teacherAPI.getPublicCourses(teacherId)
    if (result.success && result.data) {
      // 智能匹配功能只显示1对1课程
      const oneOnOneCourses = result.data.filter((course: any) => course.courseType === 'one_on_one')
      teacherCourses.value = oneOnOneCourses

      if (oneOnOneCourses.length === 0) {
        ElMessage.warning('该教师暂无1对1课程可预约')
      }
    } else {
      teacherCourses.value = []
      ElMessage.warning('该教师暂无可预约的课程')
    }
  } catch (error) {
    console.error('获取教师课程失败:', error)
    teacherCourses.value = []
    ElMessage.error('获取教师课程失败')
  } finally {
    loadingCourses.value = false
  }
}

// 显示教师课表
const showTeacherSchedule = async (teacher: Teacher) => {
  currentTeacher.value = teacher

  // 重置选课表单
  scheduleForm.bookingType = isTrialMode.value ? 'trial' : 'recurring'
  scheduleForm.selectedWeekdays = []
  scheduleForm.selectedTimeSlots = []
  // 设置开始日期为明天
  scheduleForm.startDate = getDefaultStartDate()
  scheduleForm.endDate = ''
  scheduleForm.sessionCount = 12
  // 重置试听课字段
  scheduleForm.trialDate = ''
  scheduleForm.trialStartTime = ''
  scheduleForm.trialEndTime = ''
  // 重置课程时长选择
  scheduleForm.selectedDurationMinutes = 120
  // 重置课程选择
  selectedCourse.value = null
  selectedTeachingLocation.value = ''

  if (isTrialMode.value) {
    // 试听：不需要课程列表，沿用匹配时选择的日期与上下午/晚上
    // 避免预设触发清空逻辑
    isInitializingTrialPreset.value = true
    scheduleForm.trialDate = trialSearchDate.value ? toYMD(trialSearchDate.value as unknown as string | Date) : ''
    selectedTimePeriod.value = (selectedPeriods.value[0] || '')
    if (scheduleForm.trialDate) {
      await loadTrialAvailability(scheduleForm.trialDate)
      if (selectedTimePeriod.value) {
        availableTrialSlotsByPeriod.value = getTrialTimeSlotsByPeriod(selectedTimePeriod.value)
    isInitializingTrialPreset.value = false
      }
    }
  } else {
    // 正式课：需要课程与周期性可用性
    await loadTeacherCourses(teacher.id)
    await generateAvailableTimeSlots()
  }

  await loadAllowedTeachingLocations()
  showScheduleModal.value = true
}

// 生成可用的周期性时间段（支持根据学生选择的开始/结束日期动态计算）
const generateAvailableTimeSlots = async (rangeStart?: string, rangeEnd?: string) => {
  if (!currentTeacher.value) return

  try {
    // 首先获取教师的可预约时间设置
    const teacherAvailableTimeResult = await teacherAPI.getAvailableTime(currentTeacher.value.id)
    let teacherAvailableSlots: any[] = []

    if (teacherAvailableTimeResult.success && teacherAvailableTimeResult.data && teacherAvailableTimeResult.data.availableTimeSlots) {
      teacherAvailableSlots = teacherAvailableTimeResult.data.availableTimeSlots
      console.log('获取教师可预约时间设置:', teacherAvailableSlots)
    } else {
      console.log('教师未设置可预约时间，默认所有时间可用')
    }

    // 计算查询时间范围：以学生选择的开始日期为基准，若未选择则从今天起，默认90天
    const baseStart = rangeStart || scheduleForm.startDate || new Date().toISOString().split('T')[0]
    const baseStartDate = new Date(baseStart)
    const startDate = baseStart
    const endDate = rangeEnd || scheduleForm.endDate || new Date(baseStartDate.getTime() + 90 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]

    // 调用后端API获取时间段冲突情况
    const result = await teacherAPI.checkAvailability(currentTeacher.value.id, {
      startDate,
      endDate
    })

    // 生成完整的时间段列表，结合教师可预约时间和冲突情况
    const allTimeSlots: any[] = []

    if (teacherAvailableSlots.length > 0) {
      // 如果教师设置了可预约时间，只显示这些时间段
      teacherAvailableSlots.forEach((slot: any) => {
        if (slot.timeSlots && slot.timeSlots.length > 0) {
          slot.timeSlots.forEach((timeSlot: string) => {
            // 查找该时间段的冲突情况
            const conflictInfo = result.success && result.data ?
              result.data.find((item: any) => item.weekday === slot.weekday && item.timeSlot === timeSlot) : null

            allTimeSlots.push({
              weekday: slot.weekday,
              time: timeSlot,
              timeSlot: timeSlot,
              available: conflictInfo ? conflictInfo.available : true,
              conflictDates: conflictInfo ? (conflictInfo.conflictDates || []) : [],
              availableFromDate: conflictInfo ? conflictInfo.availableFromDate : undefined,
              conflictEndDate: conflictInfo ? conflictInfo.conflictEndDate : undefined,
              conflictReason: conflictInfo ? conflictInfo.conflictReason : undefined,
              isTeacherAvailable: true // 标记为教师可预约时间
            })
          })
        }
      })
    } else {
      // 如果教师未设置可预约时间，显示所有默认时间段
      const defaultTimeSlots = getCompleteTimeSlots()

      for (let weekday = 1; weekday <= 7; weekday++) {
        defaultTimeSlots.forEach(timeSlot => {
          const conflictInfo = result.success && result.data ?
            result.data.find((item: any) => item.weekday === weekday && item.timeSlot === timeSlot) : null

          allTimeSlots.push({
            weekday: weekday,
            time: timeSlot,
            timeSlot: timeSlot,
            available: conflictInfo ? conflictInfo.available : true,
            conflictDates: conflictInfo ? (conflictInfo.conflictDates || []) : [],
            availableFromDate: conflictInfo ? conflictInfo.availableFromDate : undefined,
            conflictEndDate: conflictInfo ? conflictInfo.conflictEndDate : undefined,
            conflictReason: conflictInfo ? conflictInfo.conflictReason : undefined,
            isTeacherAvailable: true // 默认情况下都可预约
          })
        })
      }
    }

    availableTimeSlots.value = allTimeSlots
    console.log('生成的可用时间段:', allTimeSlots)

  } catch (error) {
    console.error('获取时间段可用性失败:', error)
    // 如果API调用失败，使用默认时间段
    generateDefaultTimeSlots()
  }
}

// 生成默认时间段（作为备用）- 当API调用失败时不显示任何时间段
const generateDefaultTimeSlots = () => {
  // 当API调用失败时，不生成任何默认时间段
  // 这样可以避免显示教师实际上不可用的时间段
  availableTimeSlots.value = []
  console.warn('无法获取教师可用时间段，课表为空。请检查网络连接或联系管理员。')
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

// 将开始时间映射到基础2小时区间
const getBaseSlotForStart = (startTime: string): string | null => {
  const baseSlots = getCompleteTimeSlots() // ['08:00-10:00', ...]
  const [sH, sM] = startTime.split(':').map(Number)
  const startMinutes = sH * 60 + sM
  for (const slot of baseSlots) {
    const [bStart, bEnd] = slot.split('-')
    const [bSH, bSM] = bStart.split(':').map(Number)
    const [bEH, bEM] = bEnd.split(':').map(Number)
    const bStartMin = bSH * 60 + bSM
    const bEndMin = bEH * 60 + bEM
    if (startMinutes >= bStartMin && startMinutes < bEndMin) {
      return slot
    }
  }
  return null
}

// 生成指定日期可选的30分钟试听时间段（在三个大区间内）
const getAvailableTrialTimeSlots = (): string[] => {
  if (!scheduleForm.trialDate) return []

  const makeRange = (start: string, end: string): string[] => {
    const [sH, sM] = start.split(':').map(Number)
    const [eH, eM] = end.split(':').map(Number)
    const startMin = sH * 60 + sM
    const endMin = eH * 60 + eM
    const slots: string[] = []
    for (let m = startMin; m + 30 <= endMin; m += 30) {
      const sh = Math.floor(m / 60).toString().padStart(2, '0')
      const sm = (m % 60).toString().padStart(2, '0')
      const eh = Math.floor((m + 30) / 60).toString().padStart(2, '0')
      const em = ((m + 30) % 60).toString().padStart(2, '0')
      slots.push(`${sh}:${sm}-${eh}:${em}`)
    }
    return slots
  }

  // 三个区间：08:00-12:00, 13:00-17:00, 17:00-21:00
  const ranges = [
    ...makeRange('08:00', '12:00'),
    ...makeRange('13:00', '17:00'),
    ...makeRange('17:00', '21:00')
  ]

  // 若教师设置了可预约时间，基于基础区间可用性过滤
  const trialDate = new Date(scheduleForm.trialDate)
  const weekday = trialDate.getDay() === 0 ? 7 : trialDate.getDay()
  const filtered = ranges.filter(t => {
    const start = t.split('-')[0]
    const baseSlot = getBaseSlotForStart(start)
    if (!baseSlot) return false
    const slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === baseSlot)
    if (!slot) return true // 无数据则默认可选
    return slot.isTeacherAvailable !== false && slot.available
  })

  return filtered
}

// 根据时间段类型获取对应的可用时间段
const getTrialTimeSlotsByPeriod = (period: string): string[] => {
  if (!scheduleForm.trialDate) return []

  // 优先使用后端返回的试听可用性数据（更准确）
  if (trialAvailabilityData.value?.trialSlots) {
    return getTrialOptionsByPeriod(period as 'morning' | 'afternoon' | 'evening')
  }

  // 兼容旧逻辑（基于教师可用时间段推导）
  const allSlots = getAvailableTrialTimeSlots()
  switch (period) {
    case 'morning':
      return allSlots.filter(slot => {
        const startHour = parseInt(slot.split('-')[0].split(':')[0])
        return startHour >= 8 && startHour < 10
      })
    case 'afternoon':
      return allSlots.filter(slot => {
        const startHour = parseInt(slot.split('-')[0].split(':')[0])
        return startHour >= 13 && startHour < 17
      })
    case 'evening':
      return allSlots.filter(slot => {
        const startHour = parseInt(slot.split('-')[0].split(':')[0])
        return startHour >= 17 && startHour < 21
      })
    default:
      return []
  }
}


const selectTimePeriod = (period: string) => {
  selectedTimePeriod.value = period
  availableTrialSlotsByPeriod.value = getTrialTimeSlotsByPeriod(period)

  // 清空已选择的时间段
  scheduleForm.trialStartTime = ''
  scheduleForm.trialEndTime = ''
}

// 获取时间段类型的显示名称
const getTimePeriodName = (period: string): string => {
  const names = {
    morning: '上午（8-12点）',
    afternoon: '下午（13-17点）',
    evening: '晚上（17-21点）'
  }
  return names[period] || ''
}

// 获取时间段类型的图标
const getTimePeriodIcon = (period: string) => {
  const icons = {
    morning: 'Sunrise',
    afternoon: 'Sunny',
    evening: 'Moon'
  }
  return icons[period] || 'Clock'
}

// 试听课可用性数据
const trialAvailabilityData = ref<any>(null)

// 加载试听课可用性数据
const loadTrialAvailability = async (date: string, segment?: string) => {
  if (!currentTeacher.value) return

  try {
    const result = await bookingAPI.getDayAvailability(currentTeacher.value.id, date, segment)
    if (result.success && result.data) {
      trialAvailabilityData.value = result.data
    }
  } catch (error) {
    console.error('加载试听课可用性失败:', error)
    trialAvailabilityData.value = null
  }
}

// 检查试听课时间段是否可用（30分钟）
const isTrialTimeSlotAvailable = (timeSlot: string): boolean => {
  if (!scheduleForm.trialDate) return false
  // 可用性数据未就绪时，先允许选择，后端仍会进行冲突校验
  if (!trialAvailabilityData.value) return true

  // 从新的可用性数据中查找对应的试听课时间段
  const trialSlot = trialAvailabilityData.value.trialSlots?.find((slot: any) => slot.slot === timeSlot)
  // 未提供该时间段的专门可用性，默认允许
  if (!trialSlot) return true

  return trialSlot.trialAvailable === true
}

// 下拉选择：上午/下午/晚上三个下拉，各自列出30分钟起始时间
const trialSelectMorning = ref<string | null>(null)
const trialSelectAfternoon = ref<string | null>(null)
const trialSelectEvening = ref<string | null>(null)

const getTrialOptionsByPeriod = (period: 'morning' | 'afternoon' | 'evening'): string[] => {
  if (!trialAvailabilityData.value?.trialSlots) return []

  // 从可用性数据中获取对应时间段的可选项
  const trialSlots = trialAvailabilityData.value.trialSlots
  const inRange = (t: string, startHour: number, endHour: number) => {
    const sh = parseInt(t.split('-')[0].split(':')[0])
    return sh >= startHour && sh < endHour
  }

  const availableSlots = trialSlots
    .filter((slot: any) => slot.trialAvailable === true)
    .map((slot: any) => slot.slot)

  if (period === 'morning') return availableSlots.filter((t: string) => inRange(t, 8, 12))
  if (period === 'afternoon') return availableSlots.filter((t: string) => inRange(t, 13, 17))
  return availableSlots.filter((t: string) => inRange(t, 17, 21))
}

const onTrialSelectChange = (period: 'morning' | 'afternoon' | 'evening', value: string | null) => {
  // 清空其他下拉，确保只选择一个时间
  if (period !== 'morning') trialSelectMorning.value = null
  if (period !== 'afternoon') trialSelectAfternoon.value = null
  if (period !== 'evening') trialSelectEvening.value = null

  if (!value) {
    scheduleForm.trialStartTime = ''
    scheduleForm.trialEndTime = ''
    return
  }

  // 使用统一的选择逻辑
  selectTrialTimeSlot(value)
}

// 切换日期时清空下拉与已选试听时间
watch(() => scheduleForm.trialDate, async (newDate) => {
  if (isInitializingTrialPreset.value) {
    // 预设阶段：不清空，直接根据已选时段刷新可选时间
    if (selectedTimePeriod.value) {
      availableTrialSlotsByPeriod.value = getTrialTimeSlotsByPeriod(selectedTimePeriod.value)
    }
    isInitializingTrialPreset.value = false
    return
  }
  trialSelectMorning.value = null
  trialSelectAfternoon.value = null
  trialSelectEvening.value = null
  scheduleForm.trialStartTime = ''
  scheduleForm.trialEndTime = ''
  // 清空时间段类型选择
  selectedTimePeriod.value = ''
  availableTrialSlotsByPeriod.value = []

  // 加载新日期的可用性数据
  if (newDate) {
    await loadTrialAvailability(newDate)
  }
})


// 选择试听课时间段
const selectTrialTimeSlot = (timeSlot: string) => {
  if (!isTrialTimeSlotAvailable(timeSlot)) {
    // 获取不可用的原因
    const reason = getTrialTimeSlotUnavailableReason(timeSlot)
    ElMessage.warning(reason)
    return
  }

  const [startTime] = timeSlot.split('-')
  scheduleForm.trialStartTime = startTime

  // 自动计算结束时间（30分钟后）
  const [hours, minutes] = startTime.split(':').map(Number)
  const start = new Date()
  start.setHours(hours, minutes, 0, 0)
  const end = new Date(start.getTime() + 30 * 60 * 1000)

  const endHours = end.getHours().toString().padStart(2, '0')
  const endMinutes = end.getMinutes().toString().padStart(2, '0')
  scheduleForm.trialEndTime = `${endHours}:${endMinutes}`
}

// 获取试听课时间段不可用的原因
const getTrialTimeSlotUnavailableReason = (timeSlot: string): string => {
  if (!scheduleForm.trialDate) return '请先选择日期'
  // 可用性未就绪时不给出“加载中”提示，避免误导
  if (!trialAvailabilityData.value) return '该时间段暂未校验，可尝试选择'

  // 从新的可用性数据中查找对应的试听课时间段
  const trialSlot = trialAvailabilityData.value.trialSlots?.find((slot: any) => slot.slot === timeSlot)
  if (!trialSlot) return '该时间段不在可预约范围内'

  if (trialSlot.trialAvailable === true) return ''

  // 根据冲突原因返回相应的提示信息
  const reasons = trialSlot.reasons || []
  if (reasons.includes('teacherUnavailable')) return '该时间段教师不可预约'
  if (reasons.includes('busyScheduled')) return '该时间段已被占用'
  if (reasons.includes('duplicateTrialSlot')) return '该时间段已有试听课预约'
  if (reasons.includes('pendingBooking')) return '该时间段已有待处理预约，暂不可选择'
  if (reasons.includes('baseSlotOccupiedByTrial')) return '该时间段与试听课冲突，暂不可选择'

  return '该时间段暂不可选择'
}

// 获取当前选择的试听课时间段
const getTrialTimeSlot = (): string => {
  if (!scheduleForm.trialStartTime || !scheduleForm.trialEndTime) {
    return ''
  }
  return `${scheduleForm.trialStartTime}-${scheduleForm.trialEndTime}`
}

// 检查指定日期是否有任何可用的试听时间段
const hasAvailableTrialSlots = (date: Date): boolean => {
  const weekday = date.getDay() === 0 ? 7 : date.getDay()

  // 检查三个时间段：上午(8-12)、下午(13-17)、晚上(17-21)
  const timeRanges = [
    { start: 8, end: 12, name: 'morning' },
    { start: 13, end: 17, name: 'afternoon' },
    { start: 17, end: 21, name: 'evening' }
  ]

  for (const range of timeRanges) {
    // 生成该时间段内的30分钟试听时间段
    const slots: string[] = []
    for (let hour = range.start; hour < range.end; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        const startTime = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`
        const endHour = minute + 30 >= 60 ? hour + 1 : hour
        const endMinute = minute + 30 >= 60 ? minute + 30 - 60 : minute + 30
        const endTime = `${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}`
        slots.push(`${startTime}-${endTime}`)
      }
    }

    // 检查是否有任何时间段可用
    for (const slot of slots) {
      const start = slot.split('-')[0]
      const baseSlot = getBaseSlotForStart(start)
      if (baseSlot && isTimeSlotAvailable(weekday, baseSlot)) {
        return true
      }
    }
  }

  return false
}

// 检查试听课预约是否会影响基础2小时区间的可用性
const hasTrialBookingConflict = (weekday: number, timeSlot: string): boolean => {
  // 检查该基础2小时区间是否已经有试听课预约
  // 逻辑说明：
  // 1. 如果8:00-8:30有试听课预约，那么8:00-10:00的基础区间应该被标记为不可用（用于正式课）
  // 2. 但是8:30-10:00之间还可以预约试听课
  // 3. 通过检查availableTimeSlots中的reasons字段来判断是否有试听课冲突

  const slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === timeSlot)
  if (!slot) {
    return false
  }

  // 检查是否有试听课相关的冲突原因
  // 注意：这里需要根据实际的数据结构来调整
  // 如果slot对象没有reasons属性，则通过其他方式判断
  if (slot.conflictReason) {
    return slot.conflictReason.includes('试听') ||
           slot.conflictReason.includes('trial') ||
           slot.conflictReason.includes('基础区间')
  }

  return false
}

// 获取时间段的样式类
const getTimePeriodClass = (timeSlot: string): string => {
  const startHour = parseInt(timeSlot.split('-')[0].split(':')[0])

  if (startHour >= 8 && startHour < 10) {
    return 'morning-slot'
  } else if (startHour >= 13 && startHour < 17) {
    return 'afternoon-slot'
  } else if (startHour >= 17 && startHour <= 21) {
    return 'evening-slot'
  }

  return ''
}

// 自动计算试听课结束时间（固定30分钟）- 保留原函数以防其他地方使用
const updateTrialEndTime = () => {
  if (scheduleForm.trialStartTime) {
    const [hours, minutes] = scheduleForm.trialStartTime.split(':').map(Number)
    const startTime = new Date()
    startTime.setHours(hours, minutes, 0, 0)

    // 加30分钟
    const endTime = new Date(startTime.getTime() + 30 * 60 * 1000)
    const endHours = endTime.getHours().toString().padStart(2, '0')
    const endMinutes = endTime.getMinutes().toString().padStart(2, '0')

    scheduleForm.trialEndTime = `${endHours}:${endMinutes}`
  }
}

// 创建预约申请（支持单次和周期性）
const createBookingRequest = async () => {
  if (!currentTeacher.value) {
    ElMessage.error('请先选择教师')
    return
  }

  // 验证表单数据
  if (scheduleForm.bookingType !== 'trial' && !selectedCourse.value) {
    ElMessage.warning('请选择要预约的课程')
    return
  }

  if (scheduleForm.bookingType === 'trial') {
    if (!scheduleForm.trialDate || !scheduleForm.trialStartTime || !scheduleForm.trialEndTime) {
      ElMessage.warning('请选择试听课日期和时间')
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
    // 验证课程时长选择（仅当课程时长为空时）


    if (!selectedCourse.value.durationMinutes && !scheduleForm.selectedDurationMinutes) {
      ElMessage.warning('请选择课程时长')
      return
    }
  }

  try {
    // 构建预约请求数据
    const bookingData: any = {
      teacherId: currentTeacher.value.id,
      bookingType: scheduleForm.bookingType === 'trial' ? 'single' : 'recurring', // 试听课作为单次预约处理
      studentRequirements: scheduleForm.bookingType === 'trial'
        ? `申请与${currentTeacher.value.name}老师进行${matchForm.subject}科目试听`
        : `希望预约${currentTeacher.value.name}老师的《${selectedCourse.value.title}》课程`,
      grade: matchForm.grade || undefined,
      isTrial: scheduleForm.bookingType === 'trial',
      trialDurationMinutes: scheduleForm.bookingType === 'trial' ? 30 : undefined,
      selectedDurationMinutes: scheduleForm.bookingType === 'recurring' ? (selectedCourse.value?.durationMinutes || scheduleForm.selectedDurationMinutes) : undefined
    }
    if (scheduleForm.bookingType !== 'trial') {
      bookingData.courseId = selectedCourse.value.id
    }

    if (scheduleForm.bookingType === 'trial') {
      // 试听课预约
      bookingData.requestedDate = scheduleForm.trialDate
      bookingData.requestedStartTime = scheduleForm.trialStartTime
      bookingData.requestedEndTime = scheduleForm.trialEndTime
    } else {
      // 周期性预约
      bookingData.recurringWeekdays = scheduleForm.selectedWeekdays
      bookingData.recurringTimeSlots = scheduleForm.selectedTimeSlots
      bookingData.startDate = scheduleForm.startDate
      bookingData.endDate = scheduleForm.endDate
      bookingData.totalTimes = scheduleForm.sessionCount
    }

    // 授课地点
    if (!selectedTeachingLocation.value) {
      ElMessage.warning('请选择授课地点')
      return
    }
    if (selectedTeachingLocation.value === 'online') {
      bookingData.teachingLocation = '线上'
    } else {
      bookingData.teachingLocationId = Number(selectedTeachingLocation.value)
    }

    // 调用后端API创建预约申请
    const result = await bookingAPI.createRequest(bookingData)

    if (result.success && result.data) {
      let successMessage = ''
      if (scheduleForm.bookingType === 'trial') {
        successMessage = `试听课申请提交成功！${currentTeacher.value?.name} - ${scheduleForm.trialDate} ${scheduleForm.trialStartTime}-${scheduleForm.trialEndTime}（30分钟免费试听）`
      } else {
        const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
        const selectedDays = scheduleForm.selectedWeekdays.map(day => weekdayNames[day]).join('、')
        const selectedTimes = scheduleForm.selectedTimeSlots.join('、')
        successMessage = `预约申请提交成功！${currentTeacher.value?.name} - 每周${selectedDays} ${selectedTimes}，共${scheduleForm.sessionCount}次课`
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
      // 显示后端返回的具体错误信息
      const errorMessage = result.message || '预约申请提交失败'
      ElMessage.error({
        message: errorMessage,
        duration: 8000, // 延长显示时间，让用户有足够时间阅读
        showClose: true
      })

      // 如果是余额不足，显示特殊提示
      if (errorMessage.includes('余额不足')) {
        ElMessage.warning({
          message: '请联系客服充值M豆后预约课程',
          duration: 10000,
          showClose: true
        })
      }
    }
  } catch (error) {
    console.error('创建预约申请失败:', error)

    // 尝试从错误对象中提取更详细的错误信息
    let errorMessage = '预约申请提交失败，请稍后重试'

    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.message) {
      errorMessage = error.message
    }

    ElMessage.error({
      message: errorMessage,
      duration: 8000,
      showClose: true
    })
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
  const names = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return names[weekday] || '未知'
}

// 获取完整的时间段列表 - 系统上课时间
const getCompleteTimeSlots = (): string[] => {
  return [
    '08:00-10:00', '10:00-12:00', '13:00-15:00',
    '15:00-17:00', '17:00-19:00', '19:00-21:00'
  ]
}

// 获取唯一的时间段列表（基于教师的可预约时间）
const getUniqueTimeSlots = (): string[] => {
  // 始终返回完整的时间段列表，让用户可以选择所有时间段
  // 具体的可用性检查在 isTimeSlotAvailable 函数中进行
  return getCompleteTimeSlots()
}

// 根据课程时长获取可选的时间段列表
const getAvailableTimeSlotsForSelection = (): string[] => {
  // 获取当前应该使用的时长（优先使用课程固定时长，其次使用学生选择的时长）
  const currentDuration = selectedCourse.value?.durationMinutes || scheduleForm.selectedDurationMinutes

  // 先按时长生成候选时间段
  let slots: string[]
  if (currentDuration) {
    slots = getAvailableTimeSlotsByDuration(currentDuration)
  } else {
    slots = getUniqueTimeSlots()
  }

  // 若学生勾选了上午/下午/晚上，则只展示对应区间内的时间按钮
  const allowed = selectedPeriods.value.length > 0 ? selectedPeriods.value : null
  if (!allowed) return slots

  return slots.filter((s) => {
    const h = getStartHourFromTime(s)
    if (h === null) return false
    const p = getPeriodByHour(h)
    return p ? allowed.includes(p) : false
  })
}

// 找到1.5小时时间段对应的基础2小时时间段
const findBaseTimeSlotFor90Min = (timeSlot: string): string | null => {
  const [startTime] = timeSlot.split('-')
  const [startHour, startMinute] = startTime.split(':').map(Number)

  // 获取基础2小时时间段
  const baseTimeSlots = getCompleteTimeSlots()

  // 找到包含这个开始时间的基础时间段
  for (const baseSlot of baseTimeSlots) {
    const [baseStart] = baseSlot.split('-')
    const [baseStartHour, baseStartMinute] = baseStart.split(':').map(Number)

    // 计算基础时间段的开始时间（分钟）
    const baseStartMinutes = baseStartHour * 60 + baseStartMinute
    // 计算当前开始时间（分钟）
    const currentStartMinutes = startHour * 60 + startMinute

    // 如果当前开始时间在基础时间段内，返回基础时间段
    if (currentStartMinutes >= baseStartMinutes && currentStartMinutes < baseStartMinutes + 120) {
      return baseSlot
    }
  }

  return null
}

// 检查时间段是否与已选择的时间段冲突（在同一基础区间内）
const isTimeSlotConflictWithSelected = (timeSlot: string): boolean => {
  const currentDuration = selectedCourse.value?.durationMinutes || scheduleForm.selectedDurationMinutes

  // 只有1.5小时课程需要检查区间冲突
  if (currentDuration !== 90) {
    return false
  }

  // 如果当前时间段已经被选中，则不算冲突（允许取消选择）
  if (scheduleForm.selectedTimeSlots.includes(timeSlot)) {
    return false
  }

  // 找到当前时间段对应的基础区间
  const currentBaseSlot = findBaseTimeSlotFor90Min(timeSlot)
  if (!currentBaseSlot) {
    return false
  }

  // 检查已选择的时间段中是否有与当前时间段在同一基础区间的
  return scheduleForm.selectedTimeSlots.some(selectedSlot => {
    const selectedBaseSlot = findBaseTimeSlotFor90Min(selectedSlot)
    return selectedBaseSlot === currentBaseSlot
  })
}

// 检查时间段是否可用于选择（正式课）

const isTimeSlotAvailableForSelection = (timeSlot: string): boolean => {
  // 检查是否有任何星期几可用
  const hasAvailableWeekday = [1, 2, 3, 4, 5, 6, 7].some(weekday =>
    isTimeSlotAvailable(weekday, timeSlot)
  )

  // 如果没有任何星期几可用，则时间段不可选择
  if (!hasAvailableWeekday) {
    return false
  }

  // 额外检查：确保时间段在所有星期几上都没有冲突
  // 如果某个星期几有冲突，但其他星期几可用，仍然允许选择
  // 但需要在UI上显示冲突信息

  return true
}

// 获取正式课时间段不可用的原因
const getTimeSlotUnavailableReason = (timeSlot: string): string => {
  // 检查是否有任何星期几可用
  const availableWeekdays = [1, 2, 3, 4, 5, 6, 7].filter(weekday =>
    isTimeSlotAvailable(weekday, timeSlot)
  )

  if (availableWeekdays.length === 0) {
    // 检查第一个星期几的不可用原因
    const firstWeekday = 1
    const currentDuration = selectedCourse.value?.durationMinutes || scheduleForm.selectedDurationMinutes

    if (currentDuration === 90) {
      const baseTimeSlot = findBaseTimeSlotFor90Min(timeSlot)
      if (!baseTimeSlot) return '该时间段不在可预约范围内'

      const slot = availableTimeSlots.value.find(s => s.weekday === firstWeekday && (s.time || s.timeSlot) === baseTimeSlot)
      if (!slot) return '该时间段教师不可预约'

      if (slot.isTeacherAvailable === false) return '该时间段教师不可预约'
      if (!slot.available) return '该时间段已被占用'

      if (slot.conflictReason && (slot.conflictReason.includes('待处理') || slot.conflictReason.includes('pending'))) {
        return '该时间段已有待处理预约，暂不可选择'
      }
    } else {
      const slot = availableTimeSlots.value.find(s => s.weekday === firstWeekday && (s.time || s.timeSlot) === timeSlot)
      if (!slot) return '该时间段教师不可预约'

      if (slot.isTeacherAvailable === false) return '该时间段教师不可预约'
      if (!slot.available) return '该时间段已被占用'

      if (slot.conflictReason && (slot.conflictReason.includes('待处理') || slot.conflictReason.includes('pending'))) {
        return '该时间段已有待处理预约，暂不可选择'
      }
    }

    return '该时间段教师不可预约'
  }

  if (availableWeekdays.length < 7) {
    const unavailableCount = 7 - availableWeekdays.length
    return `该时间段在${unavailableCount}个星期几不可用`
  }

  return ''
}

// 选择时间段
const selectTimeSlot = (time: string) => {
  // 如果时间段不可用，不允许选择
  if (!isTimeSlotAvailableForSelection(time)) {
    const reason = getTimeSlotUnavailableReason(time)
    ElMessage.warning(reason)
    return
  }

  // 如果时间段已经被选中，允许取消选择
  if (scheduleForm.selectedTimeSlots.includes(time)) {
    scheduleForm.selectedTimeSlots = scheduleForm.selectedTimeSlots.filter(t => t !== time)
    // 清空之前选择的星期，让用户重新选择
    scheduleForm.selectedWeekdays = []
    // 更新准确的匹配度信息
    updateAccurateMatchScore()
    return
  }

  // 检查是否与已选择的时间段冲突（同一区间内）
  if (isTimeSlotConflictWithSelected(time)) {
    ElMessage.warning('该时间段与已选择的时间段在同一时间区间内，1.5小时课程在同一区间只能选择一个时间段')
    return
  }

  // 添加新的时间段
  scheduleForm.selectedTimeSlots.push(time)
  // 清空之前选择的星期，让用户重新选择
  scheduleForm.selectedWeekdays = []
  // 更新准确的匹配度信息
  updateAccurateMatchScore()
}

// 获取星期几的短名称
const getWeekdayShort = (weekday: number): string => {
  const names = ['', '一', '二', '三', '四', '五', '六', '日']
  return names[weekday] || '未知'
}

// 判断某个星期几的时间段是否可用
const isTimeSlotAvailable = (weekday: number, time: string): boolean => {
  // 获取当前应该使用的时长（优先使用课程固定时长，其次使用学生选择的时长）
  const currentDuration = selectedCourse.value?.durationMinutes || scheduleForm.selectedDurationMinutes

  // 如果当前时长是90分钟（1.5小时），需要映射到基础2小时时间段
  if (currentDuration === 90) {
    const baseTimeSlot = findBaseTimeSlotFor90Min(time)
    if (!baseTimeSlot) {
      return false
    }
    // 使用基础时间段来判断可用性
    const slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === baseTimeSlot)
    if (!slot) {
      return false
    }

    // 检查基础时间段是否被试听课预约影响
    if (hasTrialBookingConflict(weekday, baseTimeSlot)) {
      return false
    }

    // 检查是否有待处理预约冲突
    if (slot.conflictReason && (slot.conflictReason.includes('待处理') || slot.conflictReason.includes('pending'))) {
      return false
    }

    return slot.isTeacherAvailable !== false && slot.available
  }

  // 2小时课程，直接查找
  const slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === time)
  if (!slot) {
    return false
  }

  // 检查2小时时间段是否被试听课预约影响
  if (hasTrialBookingConflict(weekday, time)) {
    return false
  }

  // 检查是否有待处理预约冲突
  if (slot.conflictReason && (slot.conflictReason.includes('待处理') || slot.conflictReason.includes('pending'))) {
    return false
  }

  return slot.isTeacherAvailable !== false && slot.available
}

// 判断某个时间段是否有可用的开始日期
const hasAvailableFromDate = (weekday: number, time: string): boolean => {
  // 获取当前应该使用的时长（优先使用课程固定时长，其次使用学生选择的时长）
  const currentDuration = selectedCourse.value?.durationMinutes || scheduleForm.selectedDurationMinutes

  // 如果当前时长是90分钟（1.5小时），需要映射到基础2小时时间段
  if (currentDuration === 90) {
    const baseTimeSlot = findBaseTimeSlotFor90Min(time)
    if (baseTimeSlot) {
      const slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === baseTimeSlot)
      return slot ? !!slot.availableFromDate : false
    }
  }

  const slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === time)
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
      // 获取当前应该使用的时长（优先使用课程固定时长，其次使用学生选择的时长）
      const currentDuration = selectedCourse.value?.durationMinutes || scheduleForm.selectedDurationMinutes

      // 如果当前时长是90分钟（1.5小时），需要映射到基础2小时时间段
      if (currentDuration === 90) {
        const baseTimeSlot = findBaseTimeSlotFor90Min(time)
        if (baseTimeSlot) {
          const slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === baseTimeSlot)
          return slot?.availableFromDate
        }
      }

      const slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === time)
      return slot?.availableFromDate
    })
    .filter(date => date)
    .sort()

  if (availableFromDates.length === 0) return ''

  return `(${formatDate(availableFromDates[0]!)}起部分可用)`
}

// 获取星期几的提示信息
const getWeekdayTooltip = (weekday: number, time: string): string => {
  // 如果学生选择了1.5小时，需要映射到基础2小时时间段
  let slot: any
  if (!selectedCourse.value?.durationMinutes && scheduleForm.selectedDurationMinutes === 90) {
    const baseTimeSlot = findBaseTimeSlotFor90Min(time)
    if (baseTimeSlot) {
      slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === baseTimeSlot)
    }
  }

  if (!slot) {
    slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === time)
  }

  if (!slot) return '暂无数据'

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
    return `冲突日期: ${slot.conflictDates.slice(0, 3).map((d: any) => formatDate(d)).join(', ')}${slot.conflictDates.length > 3 ? '...' : ''}`
  }

  return '暂不可用'
}

// 获取星期几禁用原因
const getWeekdayDisabledReason = (weekday: number, time: string): string => {
  // 如果学生选择了1.5小时，需要映射到基础2小时时间段
  let slot: any
  if (!selectedCourse.value?.durationMinutes && scheduleForm.selectedDurationMinutes === 90) {
    const baseTimeSlot = findBaseTimeSlotFor90Min(time)
    if (baseTimeSlot) {
      slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === baseTimeSlot)
    }
  }

  if (!slot) {
    slot = availableTimeSlots.value.find(s => s.weekday === weekday && (s.time || s.timeSlot) === time)
  }

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

// 规范化日期为 YYYY-MM-DD 字符串
const toYMD = (val: string | Date): string => {
  if (!val) return ''
  if (typeof val === 'string') {
    if (/^\d{4}-\d{2}-\d{2}$/.test(val)) return val
    const d = new Date(val)
    if (!isNaN(d.getTime())) {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const da = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${da}`
    }
    return val
  }
  const d = val
  if (!isNaN(d.getTime())) {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const da = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${da}`
  }
  return ''
}


// 判断某个时间段是否有冲突
const hasTimeSlotConflicts = (time: string): boolean => {
  // 如果学生选择了1.5小时，需要映射到基础2小时时间段
  if (!selectedCourse.value?.durationMinutes && scheduleForm.selectedDurationMinutes === 90) {
    const baseTimeSlot = findBaseTimeSlotFor90Min(time)
    if (baseTimeSlot) {
      return availableTimeSlots.value.some(slot =>
        (slot.time || slot.timeSlot) === baseTimeSlot &&
        ((slot.conflictDates && slot.conflictDates.length > 0) || slot.availableFromDate)
      )
    }
  }

  return availableTimeSlots.value.some(slot =>
    (slot.time || slot.timeSlot) === time &&
    ((slot.conflictDates && slot.conflictDates.length > 0) || slot.availableFromDate)
  )
}

// 计算时间匹配度 - 修复周期性预约的匹配度计算
const calculateTimeMatchScore = (): number => {
  // 周期性预约必须选择时间段和星期几才能计算匹配度
  if (scheduleForm.selectedTimeSlots.length === 0 || scheduleForm.selectedWeekdays.length === 0) {
    return 0
  }

  let totalCombinations = 0
  let availableCombinations = 0

  scheduleForm.selectedTimeSlots.forEach(timeSlot => {
    scheduleForm.selectedWeekdays.forEach(weekday => {
      totalCombinations++
      if (isTimeSlotAvailable(weekday, timeSlot)) {
        availableCombinations++
      }
    })
  })

  return totalCombinations > 0 ? Math.round((availableCombinations / totalCombinations) * 100) : 0
}

// 使用后端API获取准确的时间匹配度
const getAccurateTimeMatchScore = async (): Promise<number> => {
  const result = await getDetailedTimeValidation()
  return result.score
}

// 获取详细的时间验证信息
const getDetailedTimeValidation = async (): Promise<{ score: number, conflicts: any[] }> => {
  if (!currentTeacher.value || scheduleForm.selectedTimeSlots.length === 0 || scheduleForm.selectedWeekdays.length === 0) {
    return { score: 0, conflicts: [] }
  }

  try {
    // 构建请求数据，包含日期范围和课程次数
    const requestData: any = {
      weekdays: scheduleForm.selectedWeekdays,
      timeSlots: scheduleForm.selectedTimeSlots
    }

    // 如果学生选择了1.5小时课程，需要将时间段转换为基础2小时时间段进行后端验证
    if (!selectedCourse.value?.durationMinutes && scheduleForm.selectedDurationMinutes === 90) {
      const baseTimeSlots = scheduleForm.selectedTimeSlots.map(timeSlot => {
        const baseTimeSlot = findBaseTimeSlotFor90Min(timeSlot)
        return baseTimeSlot || timeSlot
      })
      requestData.timeSlots = baseTimeSlots
      // 添加课程时长信息，让后端知道这是1.5小时课程
      requestData.selectedDurationMinutes = scheduleForm.selectedDurationMinutes
    }

    // 如果有开始和结束日期，添加到请求中以获得更准确的匹配度
    if (scheduleForm.startDate && scheduleForm.endDate) {
      requestData.startDate = scheduleForm.startDate
      requestData.endDate = scheduleForm.endDate
      requestData.totalTimes = scheduleForm.sessionCount
    }

    const result = await teacherAPI.validateBookingTime(currentTeacher.value.id, requestData)

    if (result.success && result.data) {
      return {
        score: result.data.overallMatchScore || 0,
        conflicts: result.data.conflicts || []
      }
    }
  } catch (error) {
    console.error('获取详细时间验证信息失败:', error)
  }

  // 如果API调用失败，回退到本地计算
  return {
    score: calculateTimeMatchScore(),
    conflicts: []
  }
}

// 获取匹配度提示信息
const getMatchScoreInfo = (): { score: number, message: string, type: string } => {
  const score = calculateTimeMatchScore()

  if (score >= 80) {
    return {
      score,
      message: `匹配度很高 (${score}%)，大部分时间段都可预约`,
      type: 'success'
    }
  } else if (score >= 60) {
    return {
      score,
      message: `匹配度良好 (${score}%)，部分时间段可预约`,
      type: 'warning'
    }
  } else if (score > 0) {
    return {
      score,
      message: `匹配度较低 (${score}%)，建议调整时间选择`,
      type: 'danger'
    }
  } else {
    return {
      score,
      message: '所选时间段教师不可预约，请重新选择',
      type: 'danger'
    }
  }
}

// 生成冲突摘要信息
const getConflictSummary = (conflicts: any[]): string => {
  if (!conflicts || conflicts.length === 0) return ''

  const conflictsByType = conflicts.reduce((acc: Record<string, string[]>, conflict: any) => {
    const key = `${getWeekdayName(conflict.weekday)} ${conflict.timeSlot}`
    if (!acc[key]) {
      acc[key] = []
    }
    if (conflict.conflictDates && Array.isArray(conflict.conflictDates)) {
      acc[key].push(...conflict.conflictDates)
    }
    return acc
  }, {})

  const summaryParts: string[] = []
  Object.entries(conflictsByType).forEach(([timeKey, dates]: [string, string[]]) => {
    if (dates.length > 0) {
      const dateStr = dates.length > 3
        ? `${dates.slice(0, 3).map((d: string) => formatDate(d)).join('、')}等${dates.length}个日期`
        : dates.map((d: string) => formatDate(d)).join('、')
      summaryParts.push(`${timeKey}: ${dateStr}`)
    }
  })

  return summaryParts.length > 0
    ? `冲突时间: ${summaryParts.join('; ')}`
    : ''
}

// 获取准确的匹配度提示信息（异步版本）
const getAccurateMatchScoreInfo = async (): Promise<{ score: number, message: string, type: string, conflicts?: any[] }> => {
  const result = await getDetailedTimeValidation()
  const score = result.score
  const conflicts = result.conflicts || []

  let message = ''
  let type = ''

  if (score >= 80) {
    message = `匹配度很高 (${score}%)，大部分时间段都可预约`
    type = 'success'
  } else if (score >= 60) {
    message = `匹配度良好 (${score}%)，部分时间段可预约`
    type = 'warning'
  } else if (score > 0) {
    message = `匹配度较低 (${score}%)，建议调整时间选择`
    type = 'danger'
  } else {
    message = '所选时间段教师不可预约，请重新选择'
    type = 'danger'
  }

  // 如果有冲突，在消息中添加冲突信息
  if (conflicts.length > 0) {
    const conflictSummary = getConflictSummary(conflicts)
    if (conflictSummary) {
      message += `\n${conflictSummary}`
    }
  }

  return {
    score,
    message,
    type,
    conflicts
  }
}

// 更新准确的匹配度信息
const updateAccurateMatchScore = async () => {
  if (scheduleForm.selectedTimeSlots.length === 0 || scheduleForm.selectedWeekdays.length === 0) {
    accurateMatchScoreInfo.value = null
    return
  }

  // 如果没有设置开始日期和结束日期，不进行后端验证
  if (!scheduleForm.startDate || !scheduleForm.endDate) {
    accurateMatchScoreInfo.value = null
    return
  }

  matchScoreLoading.value = true
  try {
    accurateMatchScoreInfo.value = await getAccurateMatchScoreInfo()
    console.log('更新的匹配度信息:', accurateMatchScoreInfo.value)
  } catch (error) {
    console.error('更新匹配度信息失败:', error)
    // 回退到本地计算
    const localInfo = getMatchScoreInfo()
    accurateMatchScoreInfo.value = {
      score: localInfo.score,
      message: localInfo.message,
      type: localInfo.type,
      conflicts: []
    }
  } finally {
    matchScoreLoading.value = false
  }
}

// 切换匹配度详情展开/收起
const toggleMatchDetails = () => {
  showMatchDetails.value = !showMatchDetails.value
}

// 切换冲突详情展开/收起
const toggleConflictDetails = () => {
  showConflictDetails.value = !showConflictDetails.value
}

// 处理星期几选择变化
const onWeekdayChange = () => {
  // 当星期几选择发生变化时，重新计算结束日期
  calculateEndDate()
  // 更新匹配度信息
  updateAccurateMatchScore()
}

// 获取进度条颜色
const getProgressColor = (type: string) => {
  switch (type) {
    case 'success':
      return '#67c23a'
    case 'warning':
      return '#e6a23c'
    case 'danger':
      return '#f56c6c'
    default:
      return '#909399'
  }
}

// 获取冲突严重程度文本
const getSeverityText = (severity: string) => {
  switch (severity) {
    case 'HIGH':
      return '严重'
    case 'MEDIUM':
      return '中等'
    case 'LOW':
      return '轻微'
    default:
      return '未知'
  }
}

// 获取冲突严重程度提示
const getSeverityTooltip = (severity: string) => {
  switch (severity) {
    case 'HIGH':
      return '严重冲突：该时间段完全不可用，需要重新选择时间'
    case 'MEDIUM':
      return '中等冲突：部分时间可用，建议调整或协商'
    case 'LOW':
      return '轻微冲突：大部分时间可用，影响较小'
    default:
      return '冲突程度未知'
  }
}

// 格式化冲突日期显示
const formatConflictDate = (dateStr: string) => {
  try {
    const date = new Date(dateStr)
    const month = date.getMonth() + 1
    const day = date.getDate()
    const weekday = date.getDay()
    const weekdayNames = ['日', '一', '二', '三', '四', '五', '六']
    return `${month}/${day}(${weekdayNames[weekday]})`
  } catch (error) {
    return dateStr
  }
}

// 获取冲突摘要
const getTimeSlotConflictSummary = (time: string): string => {
  // 如果学生选择了1.5小时，需要映射到基础2小时时间段
  let searchTime = time
  if (!selectedCourse.value?.durationMinutes && scheduleForm.selectedDurationMinutes === 90) {
    const baseTimeSlot = findBaseTimeSlotFor90Min(time)
    if (baseTimeSlot) {
      searchTime = baseTimeSlot
    }
  }

  const conflictSlots = availableTimeSlots.value.filter(slot =>
    (slot.time || slot.timeSlot) === searchTime &&
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
    return `部分时段从 ${formatDate(earliestDate)} 开始可用`
  }

  if (conflictSlots2.length > 0) {
    const totalConflicts = conflictSlots2.reduce((sum, slot) => sum + (slot.conflictDates?.length || 0), 0)
    return `${totalConflicts} 个时段有冲突`
  }

  return ''
}



// 显示月度课表
const showMonthlySchedule = async (teacher: Teacher) => {
  currentMonthTeacher.value = teacher
  currentMonth.value = new Date().getMonth()
  currentYear.value = new Date().getFullYear()
  await generateMonthlyScheduleData()
  showMonthlyModal.value = true
}

// 生成月度课表数据
const generateMonthlyScheduleData = async () => {
  if (!currentMonthTeacher.value) return

  const year = currentYear.value
  const month = currentMonth.value
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const firstDayRaw = new Date(year, month, 1).getDay() // JavaScript格式：0=周日, 1=周一...6=周六

  // 头部定义是 [1, 2, 3, 4, 5, 6, 0]，对应 [周一, 周二, 周三, 周四, 周五, 周六, 周日]
  // 需要将JavaScript的getDay()结果映射到我们的列索引
  // JavaScript: 0=周日, 1=周一, 2=周二, 3=周三, 4=周四, 5=周五, 6=周六
  // 我们的列:   6=周日, 0=周一, 1=周二, 2=周三, 3=周四, 4=周五, 5=周六
  const firstDay = firstDayRaw === 0 ? 6 : firstDayRaw - 1

  monthlyScheduleData.value = []

  try {
    // 计算查询时间范围
    const startDate = new Date(year, month, 1).toISOString().split('T')[0]
    const endDate = new Date(year, month + 1, 0).toISOString().split('T')[0]

    // 调用API获取教师课表数据
    const result = await teacherAPI.getPublicSchedule(currentMonthTeacher.value.id, {
      startDate,
      endDate
    })

    const scheduleData: any = {}
    if (result.success && result.data) {
      // 将课表数据按日期分组，保留完整的日程信息
      result.data.daySchedules.forEach((daySchedule: any) => {
        scheduleData[daySchedule.date] = {
          timeSlots: daySchedule.timeSlots,
          availableCount: daySchedule.availableCount,
          bookedCount: daySchedule.bookedCount,
          dayOfWeek: daySchedule.dayOfWeek
        }


      })
    }

    // 生成日历数据
    for (let week = 0; week < 6; week++) {
      const weekData = []
      for (let day = 0; day < 7; day++) {
        const dayNumber = week * 7 + day - firstDay + 1

        if (dayNumber > 0 && dayNumber <= daysInMonth) {
          // 使用本地时间格式化日期，避免时区问题
          const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(dayNumber).padStart(2, '0')}`
          const date = new Date(year, month, dayNumber)
          const actualWeekday = date.getDay() // JavaScript格式：0=周日, 1=周一...6=周六
          const backendWeekday = actualWeekday === 0 ? 7 : actualWeekday // 转换为后端格式

          // 首先检查是否有该具体日期的数据（可能包含预订信息）
          const specificDateData = scheduleData[dateStr]

          if (specificDateData) {
            // 使用具体日期的数据（包含预订信息）
            const filteredSlots = filterTimeSlotsBySelectedPeriods(specificDateData.timeSlots)
            weekData.push({
              day: dayNumber,
              date: dateStr,
              isCurrentMonth: true,
              timeSlots: filteredSlots,
              availableCount: filteredSlots.filter(slot => slot.available).length,
              bookedCount: filteredSlots.filter(slot => slot.booked).length
            })
          } else {
            // 如果没有具体日期的数据，根据星期几生成基础时间段
            const weekdayTemplate = getWeekdayTemplate(backendWeekday, scheduleData)

            if (weekdayTemplate && weekdayTemplate.length > 0) {
              // 根据星期几模板生成可用时间段
              const timeSlots = weekdayTemplate.map((slot: any) => ({
                timeSlot: slot.timeSlot,
                available: true,  // 默认可用
                booked: false,
                studentName: null,
                courseTitle: null,
                status: null,
                isTrial: false
              }))

              const filteredSlots = filterTimeSlotsBySelectedPeriods(timeSlots)

              weekData.push({
                day: dayNumber,
                date: dateStr,
                isCurrentMonth: true,
                timeSlots: filteredSlots,
                availableCount: filteredSlots.filter(slot => slot.available).length,
                bookedCount: filteredSlots.filter(slot => slot.booked).length
              })
            } else {
              // 如果老师在这一天不排课，显示完全空的数据
              weekData.push({
                day: dayNumber,
                date: dateStr,
                isCurrentMonth: true,
                timeSlots: [],
                availableCount: 0,
                bookedCount: 0
              })
            }
          }
        } else {
          // 对于不在当月的日期，不显示或显示为空
          weekData.push({
            day: 0, // 设置为0表示不显示
            date: '',
            isCurrentMonth: false,
            timeSlots: [],
            availableCount: 0,
            bookedCount: 0
          })
        }
      }

      // 只有当这一周至少有一个当月日期时才添加到数据中
      if (weekData.some(day => day.isCurrentMonth)) {
        monthlyScheduleData.value.push(weekData)
      }
    }
  } catch (error) {
    console.error('获取教师课表失败:', error)
    // 如果API调用失败，生成默认数据
    generateDefaultMonthlyScheduleData()
  }
}

// 生成默认月度课表数据（作为备用）
const generateDefaultMonthlyScheduleData = () => {
  const year = currentYear.value
  const month = currentMonth.value
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const firstDayRaw = new Date(year, month, 1).getDay()
  // 转换为周一开始的格式：周日(0)变为6，周一(1)变为0，...，周六(6)变为5
  const firstDay = firstDayRaw === 0 ? 6 : firstDayRaw - 1

  monthlyScheduleData.value = []

  // 生成日历数据
  for (let week = 0; week < 6; week++) {
    const weekData = []
    for (let day = 0; day < 7; day++) {
      const dayNumber = week * 7 + day - firstDay + 1

      if (dayNumber > 0 && dayNumber <= daysInMonth) {
        const date = new Date(year, month, dayNumber)
        const dateStr = date.toISOString().split('T')[0]

        // 生成该日的默认时间段数据
        const timeSlots = generateDefaultDayTimeSlots()
        const filteredSlots = filterTimeSlotsBySelectedPeriods(timeSlots)

        weekData.push({
          day: dayNumber,
          date: dateStr,
          isCurrentMonth: true,
          timeSlots: filteredSlots,
          availableCount: filteredSlots.filter(slot => slot.available).length,
          bookedCount: filteredSlots.filter(slot => slot.booked).length
        })
      } else {
        // 对于不在当月的日期，不显示或显示为空
        weekData.push({
          day: 0, // 设置为0表示不显示
          date: '',
          isCurrentMonth: false,
          timeSlots: [],
          availableCount: 0,
          bookedCount: 0
        })
      }
    }

    // 只有当这一周至少有一个当月日期时才添加到数据中
    if (weekData.some(day => day.isCurrentMonth)) {
      monthlyScheduleData.value.push(weekData)
    }
  }
}

// 获取某个星期几的时间段模板
const getWeekdayTemplate = (backendWeekday: number, scheduleData: any) => {
  // 从已有数据中找到这个星期几的时间段模板
  const templateData = Object.values(scheduleData).find((dayData: any) =>
    dayData.dayOfWeek === backendWeekday
  ) as any

  if (templateData && templateData.timeSlots.length > 0) {
    // 返回时间段模板
    return templateData.timeSlots
  }

  return []
}

// 生成默认的某一天时间段数据
const generateDefaultDayTimeSlots = () => {
  const timeSlots = getCompleteTimeSlots()

  return timeSlots.map(time => ({
    timeSlot: time,
    available: true, // 默认都可用
    booked: false,   // 默认未预订
    studentName: null,
    courseTitle: null,
    status: null,
    isTrial: false
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

// 获取时间匹配度标签类型
const getTimeMatchScoreType = (score: number) => {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

// 判断教师性别是否为男性
const isMaleTeacher = (gender: string) => {
  return gender === 'Male' || gender === '男'
}

// 获取性别显示文本
const getGenderText = (gender: string) => {
  if (gender === 'Male' || gender === '男') return '男'
  if (gender === 'Female' || gender === '女') return '女'
  return '不愿透露'
}

// 级别标签颜色映射（Element Plus 支持: success/warning/info/danger/''）
const getLevelTagType = (level?: string): '' | 'success' | 'warning' | 'info' | 'danger' => {
  if (!level) return ''
  if (level.includes('王牌')) return 'success'
  if (level.includes('金牌')) return 'warning'
  if (level.includes('银牌')) return 'info'
  if (level.includes('铜牌')) return ''
  return 'info'
}

// 科目完整列表缓存与按需加载
const fullSubjectsMap = reactive<{ [id: number]: string[] }>({})
const fullSubjectsLoading = reactive<{ [id: number]: boolean }>({})

const subjectsDisplayText = (t: Teacher) => {
  const base = Array.isArray(t.subjects) ? t.subjects : []
  const total = typeof t.subjectsCount === 'number' ? t.subjectsCount : base.length
  const extra = Math.max(total - base.length, 0)
  return base.join('、') + (extra > 0 ? ` +${extra}` : '')
}

const getSubjectsTooltip = (t: Teacher) => {
  const full = fullSubjectsMap[t.id]
  if (Array.isArray(full) && full.length) return full.join('、')
  const base = Array.isArray(t.subjects) ? t.subjects : []
  return base.join('、')
}

const onSubjectsTooltipShow = async (t: Teacher) => {
  const baseLen = Array.isArray(t.subjects) ? t.subjects.length : 0
  const total = typeof t.subjectsCount === 'number' ? t.subjectsCount : baseLen
  if (total <= baseLen) return
  if (fullSubjectsMap[t.id] || fullSubjectsLoading[t.id]) return
  try {
    fullSubjectsLoading[t.id] = true
    const res = await teacherAPI.getDetail(t.id)
    const full = Array.isArray(res?.data?.subjects) ? res.data.subjects : []
    fullSubjectsMap[t.id] = full
  } finally {
    fullSubjectsLoading[t.id] = false
  }
}

// 安全的时间选择更新处理
const handleWeekdaysUpdate = (weekdays: number[]) => {
  nextTick(() => {
    try {
      if (Array.isArray(weekdays)) {
        const filteredWeekdays = weekdays.filter(w => typeof w === 'number')
        matchForm.preferredWeekdays.splice(0, matchForm.preferredWeekdays.length, ...filteredWeekdays)
      } else {
        matchForm.preferredWeekdays.splice(0, matchForm.preferredWeekdays.length)
      }
    } catch (error) {
      console.error('更新星期几选择时发生错误:', error)
      matchForm.preferredWeekdays.splice(0, matchForm.preferredWeekdays.length)
    }
  })
}

const handleTimeSlotsUpdate = (timeSlots: string[]) => {
  nextTick(() => {
    try {
      if (Array.isArray(timeSlots)) {
        const filteredTimeSlots = timeSlots.filter(t => typeof t === 'string')
        matchForm.preferredTimeSlots.splice(0, matchForm.preferredTimeSlots.length, ...filteredTimeSlots)
      } else {
        matchForm.preferredTimeSlots.splice(0, matchForm.preferredTimeSlots.length)
      }
    } catch (error) {
      console.error('更新时间段选择时发生错误:', error)
      matchForm.preferredTimeSlots.splice(0, matchForm.preferredTimeSlots.length)
    }
  })
}

// 监听时间选择变化，更新匹配度
watch(
  () => [scheduleForm.selectedWeekdays, scheduleForm.selectedTimeSlots, scheduleForm.startDate, scheduleForm.endDate, scheduleForm.sessionCount],
  () => {
    updateAccurateMatchScore()
  },
  { deep: true }
)

// 当开始日期变更时，重新获取该区间的可用时间与冲突数据（减少误导性的冲突数量）
watch(
  () => scheduleForm.startDate,
  async (newStart) => {
    if (!newStart || !currentTeacher.value) return
    try {
      await generateAvailableTimeSlots(newStart, scheduleForm.endDate)
    } catch (err) {


      console.error('根据开始日期刷新可用时间段失败:', err)
    }
  }
)

// 组件挂载时初始化数据
onMounted(() => {
  initOptions()
  // 如果从课程详情页带来了 teacherId/courseId，预加载并直接展示该老师
  preloadFromQuery()

// 当学生勾选的上午/下午/晚上变化时，若月度课表弹窗已打开则重新生成以应用过滤
watch(selectedPeriods, () => {
  if (showMonthlyModal.value) {
    generateMonthlyScheduleData()
  }
})

})

// 处理课程时长变化
const onDurationChange = () => {
  // 清空已选择的时间段，因为时长变化会影响可用时间段
  scheduleForm.selectedTimeSlots = []
  scheduleForm.selectedWeekdays = []
  scheduleForm.endDate = ''

  console.log('课程时长变化:', scheduleForm.selectedDurationMinutes)
}

// 根据选择的课程时长生成可选时间段
const getAvailableTimeSlotsByDuration = (durationMinutes?: number) => {
  // 如果没有传入时长参数，使用scheduleForm中的时长
  const currentDuration = durationMinutes || scheduleForm.selectedDurationMinutes
  const availableSlots: string[] = []

  if (currentDuration === 90) {
    // 1.5小时：在2小时区间内选择开始时间，但确保每个时间段都是完整的1.5小时
    const baseTimeSlots = getCompleteTimeSlots() // 获取基础2小时时间段

    baseTimeSlots.forEach(baseSlot => {
      const [startTime] = baseSlot.split('-')
      const [startHour, startMinute] = startTime.split(':').map(Number)

      // 在2小时区间内，每15分钟一个开始时间选项
      for (let minute = 0; minute < 120; minute += 15) {
        const newStartHour = startHour + Math.floor(minute / 60)
        const newStartMinute = (startMinute + minute) % 60

        // 计算结束时间（1.5小时后）
        const totalMinutes = newStartHour * 60 + newStartMinute + 90
        const endHour = Math.floor(totalMinutes / 60)
        const endMinute = totalMinutes % 60

        // 检查是否在基础时间段内
        const endTime = `${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}`
        const baseEndTime = baseSlot.split('-')[1]

        // 确保结束时间不超过基础时间段的结束时间
        if (endTime <= baseEndTime) {
          const newStartTime = `${newStartHour.toString().padStart(2, '0')}:${newStartMinute.toString().padStart(2, '0')}`
          availableSlots.push(`${newStartTime}-${endTime}`)
        }
      }
    })
  } else if (currentDuration === 120) {
    // 2小时：使用固定的时间段
    availableSlots.push(...getCompleteTimeSlots())
  }

  return availableSlots
}

// 监听课程选择变化，自动设置课程时长
watch(selectedCourse, (newCourse) => {
  if (newCourse && newCourse.durationMinutes) {
    // 如果选择的课程有固定时长，自动设置为对应的时长
    scheduleForm.selectedDurationMinutes = newCourse.durationMinutes
  } else {
    // 如果课程没有固定时长，保持默认的2小时
    scheduleForm.selectedDurationMinutes = 120
  }
}, { immediate: true })

</script>

<template>
  <div class="teacher-match">
    <h2>{{ isTrialMode ? '预约免费试听课' : '智能匹配教师' }}</h2>
    <p class="description">{{ isTrialMode ? '选择合适的日期与时间，提交30分钟免费试听申请' : '根据您的学习需求和偏好，系统将为您匹配最合适的1对1教师' }}</p>

    <div class="match-container">


      <div class="match-form-section">
        <el-form :model="matchForm" label-position="top" class="match-form" :key="formResetKey">
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


          <el-form-item label="年级 Grade" required>
            <el-select v-model="matchForm.grade" placeholder="请选择年级" clearable :loading="loadingOptions">
              <el-option
                v-for="g in grades"
                :key="g.id || g.name"
                :label="g.name || g"
                :value="g.name || g" />
            </el-select>
          </el-form-item>


          <!-- 试听前置：日期与上下午/晚上选择 -->
          <el-form-item v-if="isTrialMode" label="上课日期">
            <el-date-picker v-model="trialSearchDate" type="date" placeholder="请选择上课日期" :disabled-date="(date) => date < new Date()" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item v-if="isTrialMode" label="偏好上课时间 Preferred Schedule">
            <div class="time-preference-container">
              <el-checkbox-group v-model="selectedPeriods" class="time-preference-checkbox-group">
                <div class="time-preference-row">
                  <el-checkbox label="morning">上午（8-12点）</el-checkbox>
                  <el-checkbox label="afternoon">下午（13-17点）</el-checkbox>
                </div>
                <div class="time-preference-row">
                  <el-checkbox label="evening">晚上（17-21点）</el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
          </el-form-item>

          <el-form-item v-if="!isTrialMode" label="偏好上课时间 Preferred Schedule">
            <div class="time-preference-container">
              <el-checkbox-group v-model="selectedPeriods" class="time-preference-checkbox-group">
                <div class="time-preference-row">
                  <el-checkbox label="morning">上午（8-12点）</el-checkbox>
                  <el-checkbox label="afternoon">下午（13-17点）</el-checkbox>
                </div>
                <div class="time-preference-row">
                  <el-checkbox label="evening">晚上（17-21点）</el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
          </el-form-item>
          <el-form-item label="教师级别 Teacher Level">
            <el-select v-model="matchForm.teacherLevel" placeholder="不限级别（可选）" clearable :loading="loadingOptions">
              <el-option
                v-for="level in teacherLevels"
                :key="level.id"
                :label="level.name"
                :value="level.name">
                <div class="option-with-icon">
                  <el-icon><Star /></el-icon>
                  <span>{{ level.name }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>


          <el-form-item>
            <el-radio-group v-model="matchForm.gender" size="large" ref="genderRadioGroupRef">
              <el-radio label="">
                不限
              </el-radio>
              <el-radio label="Male">
                <el-icon><Male /></el-icon> 男 Male
              </el-radio>
              <el-radio label="Female">
                <el-icon><Female /></el-icon> 女 Female
              </el-radio>
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
                <div class="score">{{ teacher.recommendationScore }}%</div>
                <div class="match-text">推荐度</div>
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
                <el-tag v-if="teacher.level" :type="getLevelTagType(teacher.level)" effect="dark" class="level-tag">{{ teacher.level }}</el-tag>
                <el-tag type="warning" effect="plain" class="experience-tag">{{ teacher.experience }}年教龄</el-tag>
                <el-tag type="info" effect="plain" class="gender-tag">
                  <el-icon v-if="isMaleTeacher(teacher.gender)"><Male /></el-icon>
                  <el-icon v-else><Female /></el-icon>
                  {{ getGenderText(teacher.gender) }}
                </el-tag>
                <!-- 时间匹配度显示（仅在智能匹配页展示，试听预约页隐藏） -->
                <el-tag v-if="!isTrialMode && teacher.timeMatchScore !== undefined"
                        :type="getTimeMatchScoreType(teacher.timeMatchScore)"
                        effect="plain"
                        class="time-match-tag">
                  <el-icon><Calendar /></el-icon>
                  时间匹配 {{ teacher.timeMatchScore }}%
                </el-tag>
              </div>
              <div class="teacher-subjects-all" v-if="teacher.subjects && teacher.subjects.length">
                <span class="label">教授科目：</span>
                <el-tooltip :content="getSubjectsTooltip(teacher)" placement="top" @show="onSubjectsTooltipShow(teacher)">
                  <span class="subjects-text">{{ subjectsDisplayText(teacher) }}</span>
                </el-tooltip>
              </div>
              <p class="teacher-description">{{ teacher.description }}</p>
              <div class="teacher-tags">
                <el-tag v-for="(tag, i) in teacher.tags" :key="i" size="small" class="teacher-tag" effect="light">{{ tag }}</el-tag>
              </div>

              <div class="selected-time" v-if="matchForm.preferredWeekdays.length > 0 || matchForm.preferredTimeSlots.length > 0">
                <div class="selected-time-title">您的预约时间：</div>
                <div class="selected-time-value">
                  <el-tag v-if="matchForm.preferredWeekdays.length > 0" type="success">
                    <el-icon><Calendar /></el-icon>
                    星期: {{ matchForm.preferredWeekdays.map(day => getWeekdayName(day)).join(', ') }}
                  </el-tag>
                  <el-tag v-if="matchForm.preferredTimeSlots.length > 0" type="primary" style="margin-left: 8px;">
                    时间: {{ matchForm.preferredTimeSlots.join(', ') }}
                  </el-tag>
                </div>
              </div>
              <div class="teacher-actions">
                <template v-if="!isTrialMode">
                  <el-button type="primary" @click="showTeacherSchedule(teacher)" size="large">
                    <el-icon><Calendar /></el-icon> 预约课程
                  </el-button>
                  <el-button type="warning" @click="showMonthlySchedule(teacher)" size="large">
                    <el-icon><Calendar /></el-icon> 查看课表
                  </el-button>
                </template>
                <template v-else>
                  <el-button type="primary" @click="showTeacherSchedule(teacher)" size="large">
                    <el-icon><Calendar /></el-icon> 预约老师
                  </el-button>
                </template>
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
          <el-form-item v-if="scheduleForm.bookingType === 'recurring'" label="选择课程" required>
            <div v-if="loadingCourses" class="loading-courses">
              <el-skeleton :rows="2" animated />
            </div>
            <div v-else-if="teacherCourses.length === 0" class="no-courses">
              <el-empty description="该教师暂无1对1课程可预约" :image-size="80" />
            </div>
            <div v-else class="course-selection">
              <el-radio-group v-model="selectedCourse" size="large" class="course-radio-group">
                <el-radio
                  v-for="course in teacherCourses"
                  :key="course.id"
                  :label="course"
                  class="course-radio-item"
                >
                  <div class="course-info">
                    <div class="course-title">{{ course.title }}</div>
                    <div class="course-details">
                      <el-tag size="small" type="primary">{{ course.subjectName }}</el-tag>
                      <el-tag size="small" type="warning" v-if="course.price">
                        {{ course.price }}M豆/小时
                      </el-tag>
                      <el-tag size="small" type="info" v-else>
                        价格面议
                      </el-tag>
                      <!-- 添加课程时长显示 -->
                      <el-tag size="small" type="success" v-if="course.durationMinutes">
                        {{ course.durationMinutes }}分钟
                      </el-tag>
                      <el-tag size="small" type="info" v-else>
                        时长可选
                      </el-tag>
                    </div>
                  </div>
                </el-radio>
              </el-radio-group>
            </div>
            <div class="form-item-tip">
              <el-icon><InfoFilled /></el-icon>
              请选择要预约的1对1课程，选择后可继续设置上课时间
            </div>
          </el-form-item>

          <el-form-item v-if="!isTrialMode && !isRecurringMode" label="预约类型">
            <el-radio-group v-model="scheduleForm.bookingType" size="large">
              <el-radio-button label="trial">试听课</el-radio-button>
              <el-radio-button label="recurring">正式课</el-radio-button>
            </el-radio-group>
            <div class="form-item-tip">
              <div>试听课：30分钟免费试听，每人仅限一次机会，如需增加机会请联系管理员</div>
            </div>
          </el-form-item>

          <!-- 课程时长选择（仅当课程时长为空时显示） -->
          <el-form-item v-if="selectedCourse && !selectedCourse.durationMinutes && scheduleForm.bookingType === 'recurring'" label="选择课程时长">
            <el-radio-group v-model="scheduleForm.selectedDurationMinutes" size="large" class="course-duration-selection" @change="onDurationChange">
              <el-radio :label="90">
                <div class="duration-option">
                  <div class="duration-label">1.5小时</div>
                </div>
              </el-radio>
              <el-radio :label="120">
                <div class="duration-option">
                  <div class="duration-label">2小时</div>
                </div>
              </el-radio>
            </el-radio-group>
            <div class="form-item-tip">
              <el-icon><InfoFilled /></el-icon>
              由于该课程未设置固定时长，请选择您希望的课程时长
            </div>
          </el-form-item>

          <!-- 授课地点选择 -->
          <el-form-item label="授课地点" required>
            <el-radio-group v-model="selectedTeachingLocation">
              <el-radio-button
                v-for="opt in allowedTeachingLocations"
                :key="opt.value"
                :label="opt.value"
              >
                {{ opt.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <!-- 周期性预约的课程安排设置 -->
          <template v-if="scheduleForm.bookingType === 'recurring'">
            <el-form-item label="课程安排">
              <div class="course-schedule-row">
                <div class="schedule-item">
                  <label class="schedule-label">开始日期</label>
                  <el-date-picker
                    v-model="scheduleForm.startDate"
                    type="date"
                    placeholder="选择开始日期"
                    :disabled-date="(date) => date < new Date()"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    @change="calculateEndDate"
                    style="width: 160px"
                  />
                </div>

                <div class="schedule-item">
                  <label class="schedule-label">课程次数</label>
                  <div class="session-count-wrapper">
                    <el-input-number
                      v-model="scheduleForm.sessionCount"
                      :min="1"
                      :max="50"
                      @change="calculateEndDate"
                      style="width: 120px"
                    />
                    <span class="session-unit">次课</span>
                  </div>
                </div>

                <div class="schedule-item">
                  <label class="schedule-label">结束日期</label>
                  <el-input
                    v-model="scheduleForm.endDate"
                    placeholder="自动计算"
                    readonly
                    style="width: 160px"
                  />
                </div>
              </div>
            </el-form-item>
          </template>

          <!-- 试听课表单 -->
          <template v-if="scheduleForm.bookingType === 'trial'">
            <el-alert
              title="试听课说明"
              description="试听课固定30分钟，试听课次数有限。请选择合适的日期和开始时间，系统将自动设置为30分钟时长。"
              type="info"
              show-icon
              :closable="false"
              style="margin-bottom: 20px;"
            />

            <el-form-item label="试听日期">
              <template v-if="!isTrialPreset">
                <el-date-picker
                  v-model="scheduleForm.trialDate"
                  type="date"
                  placeholder="选择试听日期"
                  :disabled-date="(date) => date < new Date() || !hasAvailableTrialSlots(date)"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 200px"
                />
              </template>
              <template v-else>
                <div class="trial-date-display">
                  <el-tag type="primary" size="large">
                    <el-icon><Calendar /></el-icon>
                    {{ scheduleForm.trialDate }}
                  </el-tag>

                </div>
              </template>
            </el-form-item>

            <el-form-item label="试听时间">
              <div class="form-item-tip">{{ isTrialPreset ? '请选择具体时间进行试听（30分钟免费体验）' : '请先选择时间段类型，然后选择具体时间进行试听（30分钟免费体验）' }}</div>

              <!-- 时间段类型选择 -->
              <div class="time-period-selection" v-if="scheduleForm.trialDate">
                <div class="period-buttons">
                  <el-button
                    v-for="period in trialPeriodOptions"
                    :key="period"
                    :type="selectedTimePeriod === period ? 'primary' : 'default'"
                    :icon="getTimePeriodIcon(period)"
                    size="large"
                    class="period-button"
                    @click="selectTimePeriod(period)"
                  >
                    {{ getTimePeriodName(period) }}
                  </el-button>
                </div>
              </div>

              <!-- 具体时间段选择 -->
              <div class="trial-time-slots" v-if="selectedTimePeriod && availableTrialSlotsByPeriod.length > 0">
                <div class="slots-header">
                  <el-icon><Clock /></el-icon>
                  <span>{{ getTimePeriodName(selectedTimePeriod) }} - 可选时间段</span>
                </div>
                <div class="time-slots-grid">
                  <div
                    v-for="time in availableTrialSlotsByPeriod"
                    :key="time"
                    :class="[
                      'trial-time-slot-card',
                      {
                        'selected': getTrialTimeSlot() === time,
                        'disabled': !isTrialTimeSlotAvailable(time)
                      }
                    ]"
                    @click="isTrialTimeSlotAvailable(time) ? selectTrialTimeSlot(time) : null"
                  >
                    <div class="time-display">{{ time }}</div>
                    <div class="time-duration">30分钟</div>
                    <div class="time-status-indicator">
                      <el-icon v-if="!isTrialTimeSlotAvailable(time)" class="status-icon disabled"><Lock /></el-icon>
                      <el-icon v-else-if="getTrialTimeSlot() === time" class="status-icon selected"><Check /></el-icon>
                      <span v-else class="status-text">可选</span>
                    </div>
                    <div v-if="!isTrialTimeSlotAvailable(time)" class="unavailable-reason">
                      {{ getTrialTimeSlotUnavailableReason(time) }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 无可用时间段提示 -->
              <div v-else-if="selectedTimePeriod && availableTrialSlotsByPeriod.length === 0" class="no-slots-message">
                <el-alert
                  title="该时间段暂无可用时间"
                  description="请选择其他时间段或日期"
                  type="warning"
                  show-icon
                  :closable="false"
                />
              </div>

              <!-- 选择结果显示 -->
              <div v-if="scheduleForm.trialStartTime && scheduleForm.trialEndTime" class="selected-trial-result">
                <el-alert
                  type="success"
                  :closable="false"
                  show-icon
                >
                  <template #title>
                    <span class="result-title">已选择试听时间</span>
                  </template>
                  <div class="result-content">
                    <div class="result-time">
                      <el-icon><Clock /></el-icon>
                      {{ scheduleForm.trialStartTime }} - {{ scheduleForm.trialEndTime }}
                    </div>
                    <div class="result-duration">30分钟免费试听</div>
                  </div>
                </el-alert>
              </div>
            </el-form-item>
          </template>

          <!-- 周期性预约表单 -->
          <template v-else>
                      <el-form-item label="选择时间段">
            <div class="form-item-tip">
              点击选择您偏好的上课时间段 (可多选)
              <template v-if="!selectedCourse?.durationMinutes && scheduleForm.selectedDurationMinutes">
                <br>当前选择：{{ scheduleForm.selectedDurationMinutes === 90 ? '1.5小时' : '2小时' }}课程
                <template v-if="scheduleForm.selectedDurationMinutes === 90">
                  <br><span style="color: #e6a23c;">注意：1.5小时课程在同一时间区间内只能选择一个时间段</span>
                </template>
              </template>
            </div>
            <div class="time-slot-selection">
              <div
                v-for="time in getAvailableTimeSlotsForSelection()"
                :key="time"
                :class="[
                  'time-slot-card',
                  {
                    'selected': scheduleForm.selectedTimeSlots.includes(time),
                    'has-conflicts': hasTimeSlotConflicts(time),
                    'conflicted': isTimeSlotConflictWithSelected(time),
                    'disabled': !isTimeSlotAvailableForSelection(time)
                  }
                ]"
                @click="isTimeSlotAvailableForSelection(time) ? selectTimeSlot(time) : null"
              >
                <div class="slot-time">{{ time }}</div>
                <div class="slot-duration-info" v-if="!selectedCourse?.durationMinutes">
                  <el-tag size="small" type="info">
                    {{ scheduleForm.selectedDurationMinutes === 90 ? '1.5小时' : '2小时' }}
                  </el-tag>
                </div>
                <div class="slot-weekdays">
                  <span
                    v-for="weekday in [1, 2, 3, 4, 5, 6, 7]"
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
                <div class="slot-availability-info" v-if="hasTimeSlotConflicts(time) || !isTimeSlotAvailableForSelection(time)">
                  <div class="conflict-summary" v-if="hasTimeSlotConflicts(time)">
                    {{ getTimeSlotConflictSummary(time) }}
                  </div>
                  <div class="unavailable-summary" v-if="!isTimeSlotAvailableForSelection(time)">
                    {{ getTimeSlotUnavailableReason(time) }}
                  </div>
                </div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="选择星期" v-if="scheduleForm.selectedTimeSlots.length > 0">
            <div class="form-item-tip">选择每周的哪几天上课 (可横向滑动查看所有选项)</div>
            <div class="weekday-selection">
              <el-checkbox-group v-model="scheduleForm.selectedWeekdays" @change="onWeekdayChange">
                <el-checkbox
                  v-for="weekday in [1, 2, 3, 4, 5, 6, 7]"
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

          <!-- 时间匹配度分析 - 紧凑设计 -->
          <el-form-item v-if="!isTrialMode && scheduleForm.selectedTimeSlots.length > 0 && scheduleForm.selectedWeekdays.length > 0" label="时间匹配度分析">
            <div class="compact-match-analysis">
              <!-- 紧凑的匹配度显示 -->
              <div class="compact-match-card" :class="(accurateMatchScoreInfo || getMatchScoreInfo()).type">
                <div class="compact-match-header" @click="toggleMatchDetails">
                  <div class="match-info-inline">
                    <div class="match-status-indicator">
                      <el-icon v-if="matchScoreLoading" class="loading-icon"><Loading /></el-icon>
                      <el-icon v-else-if="(accurateMatchScoreInfo || getMatchScoreInfo()).type === 'success'"><Check /></el-icon>
                      <el-icon v-else-if="(accurateMatchScoreInfo || getMatchScoreInfo()).type === 'warning'"><InfoFilled /></el-icon>
                      <el-icon v-else><Close /></el-icon>
                    </div>
                    <span class="match-description">{{ (accurateMatchScoreInfo || getMatchScoreInfo()).message.split('\n')[0] }}</span>
                  </div>
                  <div class="match-score-inline">
                    <span class="score-value">{{ matchScoreLoading ? '...' : (accurateMatchScoreInfo || getMatchScoreInfo()).score }}%</span>
                    <el-icon class="expand-icon" :class="{ 'rotated': showMatchDetails }"><ArrowDown /></el-icon>
                  </div>
                </div>

                <!-- 紧凑的详细内容 -->
                <el-collapse-transition>
                  <div v-show="showMatchDetails" class="compact-details-content">


                    <!-- 详细的冲突信息 -->
                    <div v-if="accurateMatchScoreInfo && accurateMatchScoreInfo.conflicts && accurateMatchScoreInfo.conflicts.length > 0" class="conflicts-compact">
                      <div class="conflicts-header" @click="toggleConflictDetails">
                        <el-icon><Warning /></el-icon>
                        <span>发现 {{ accurateMatchScoreInfo.conflicts.length }} 个时间冲突</span>
                        <el-icon class="conflict-expand-icon" :class="{ 'rotated': showConflictDetails }"><ArrowDown /></el-icon>
                      </div>
                      <div class="conflicts-details-compact">
                        <!-- 始终显示前3个冲突 -->
                        <div
                          v-for="conflict in accurateMatchScoreInfo.conflicts.slice(0, 3)"
                          :key="`${conflict.weekday}-${conflict.timeSlot}`"
                          class="conflict-item-compact"
                        >
                          <div class="conflict-time-info">
                            <el-tag type="warning" size="small" class="conflict-tag-compact">
                              <span v-if="conflict.conflictDates && conflict.conflictDates.length > 0" class="conflict-dates-inline">
                                {{ conflict.conflictDates.slice(0, 1).map(d => formatConflictDate(d))[0] }}
                              </span>
                              {{ getWeekdayName(conflict.weekday) }} {{ conflict.timeSlot }}
                            </el-tag>
                            <span class="conflict-reason-text">{{ conflict.conflictReason }}</span>
                          </div>
                        </div>

                        <!-- 可展开的其他冲突 -->
                        <el-collapse-transition>
                          <div v-show="showConflictDetails && accurateMatchScoreInfo.conflicts.length > 3">
                            <div
                              v-for="conflict in accurateMatchScoreInfo.conflicts.slice(3)"
                              :key="`${conflict.weekday}-${conflict.timeSlot}-extra`"
                              class="conflict-item-compact"
                            >
                              <div class="conflict-time-info">
                                <el-tag type="warning" size="small" class="conflict-tag-compact">
                                  <span v-if="conflict.conflictDates && conflict.conflictDates.length > 0" class="conflict-dates-inline">
                                    {{ conflict.conflictDates.slice(0, 1).map(d => formatConflictDate(d))[0] }}
                                  </span>
                                  {{ getWeekdayName(conflict.weekday) }} {{ conflict.timeSlot }}
                                </el-tag>
                                <span class="conflict-reason-text">{{ conflict.conflictReason }}</span>
                              </div>
                            </div>
                          </div>
                        </el-collapse-transition>

                        <!-- 展开提示 -->
                        <div v-if="accurateMatchScoreInfo.conflicts.length > 3 && !showConflictDetails" class="more-conflicts-toggle" @click="toggleConflictDetails">
                          <el-icon><ArrowDown /></el-icon>
                          <span>查看其他 {{ accurateMatchScoreInfo.conflicts.length - 3 }} 个冲突</span>
                        </div>
                      </div>
                    </div>

                    <!-- 智能建议 -->
                    <div v-if="(accurateMatchScoreInfo || getMatchScoreInfo()).score < 80" class="suggestion-compact">
                      <el-icon><InfoFilled /></el-icon>
                      <span v-if="(accurateMatchScoreInfo || getMatchScoreInfo()).score === 0">
                        💡 当前时间段不可用，请尝试其他时间
                      </span>
                      <span v-else-if="(accurateMatchScoreInfo || getMatchScoreInfo()).score < 50">
                        ⚠️ 冲突较多，建议重新规划时间安排
                      </span>
                      <span v-else>
                        ✨ 可以微调时间或与教师沟通协商
                      </span>
                    </div>
                  </div>
                </el-collapse-transition>
              </div>
            </div>
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
              <div class="cost-summary" v-if="selectedCourse">
                <div class="cost-item">
                  <span class="cost-label">总花费：</span>
                  <span class="cost-value">{{ formatTotalCost() }}</span>
                </div>
                <div class="cost-breakdown" v-if="selectedCourse.price && scheduleForm.bookingType === 'recurring'">
                  <span class="cost-detail">
                    {{ selectedCourse.price }}M豆/小时 × {{ getCourseDurationHours() }}小时/次 × {{ scheduleForm.sessionCount }}次
                  </span>
                </div>
                <!-- 显示课程时长信息 -->
                <div class="course-duration-info" v-if="scheduleForm.bookingType === 'recurring'">
                  <span class="duration-label">课程时长：</span>
                  <span class="duration-value">
                    <template v-if="selectedCourse.durationMinutes">
                      {{ selectedCourse.durationMinutes }}分钟（固定）
                    </template>
                    <template v-else-if="scheduleForm.selectedDurationMinutes">
                      {{ scheduleForm.selectedDurationMinutes }}分钟（学生选择）
                    </template>
                    <template v-else>
                      请选择课程时长
                    </template>
                  </span>
                </div>
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
              (!selectedTeachingLocation) ||
              (scheduleForm.bookingType === 'trial'
                ? (!scheduleForm.trialDate || !scheduleForm.trialStartTime || !scheduleForm.trialEndTime)
                : (!selectedCourse || scheduleForm.selectedTimeSlots.length === 0 || scheduleForm.selectedWeekdays.length === 0)
              )
            "
            size="large"
          >
            <el-icon><Calendar /></el-icon>
            {{ scheduleForm.bookingType === 'trial' ? '申请试听' : '确认预约' }}
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
            <div v-for="day in [1, 2, 3, 4, 5, 6, 0]" :key="day" class="week-header-day">
              {{ getDayName(day) }}
            </div>
          </div>

          <div class="calendar-grid">
            <div v-for="(week, weekIndex) in monthlyScheduleData" :key="weekIndex" class="week-row">
              <div v-for="(day, dayIndex) in week" :key="dayIndex"
                   :class="['day-cell', { 'other-month': !day.isCurrentMonth, 'clickable': day.isCurrentMonth, 'empty-day': day.day === 0 }]"
                   @click="day.isCurrentMonth && day.day > 0 ? showDayDetail(day) : null">
                <div class="day-header" v-if="day.day > 0">
                  <span class="day-number">{{ day.day }}</span>
                </div>
                <div class="day-content" v-if="day.isCurrentMonth && day.day > 0">
                  <div class="day-summary">
                    <span class="available-count">{{ day.availableCount }}可用</span>
                    <span class="booked-count">{{ day.bookedCount }}已订</span>
                  </div>
                  <div class="time-slots-preview">
                    <div
                      v-for="slot in day.timeSlots.slice(0, 3)"
                      :key="slot.timeSlot || slot.time"
                      :class="['mini-slot', { 'available': slot.available, 'booked': slot.booked }]"
                      :title="`${slot.timeSlot || slot.time} - ${slot.booked ? '已预订' : '可用'}`"
                    >
                      {{ (slot.timeSlot || slot.time || '').split('-')[0] }}
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
              :key="slot.timeSlot || slot.time"
              :class="['time-slot-detail', { 'available': slot.available, 'booked': slot.booked }]"
            >
              <div class="slot-time">{{ slot.timeSlot || slot.time }}</div>
              <div class="slot-status">
                <span v-if="slot.booked" class="status-booked">已预订</span>
                <span v-else-if="slot.available" class="status-available">可预约</span>
                <span v-else class="status-unavailable">不可用</span>
              </div>
              <div v-if="slot.studentName || slot.student" class="slot-student">
                学生：{{ slot.studentName || slot.student }}
              </div>
              <div v-if="slot.courseTitle" class="slot-course">
                课程：{{ slot.courseTitle }}
              </div>
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

.level-tag {
  margin-left: 8px;
}

.teacher-subjects-all {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}
.teacher-subjects-all .label {
  color: #666;
  font-size: 12px;
  flex: 0 0 auto;
}
.teacher-subjects-all .subjects-text {
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
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
  display: flex;
  align-items: center;
  gap: 4px;
}

.form-item-tip .el-icon {
  color: #409eff;
  font-size: 14px;
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

.time-slot-card.conflicted {
  border-color: #ff7875;
  background-color: #fff2f0;
  cursor: not-allowed;
  opacity: 0.6;
}

.time-slot-card.disabled {
  border-color: #dcdfe6;
  background-color: #f5f7fa;
  color: #c0c4cc;
  cursor: not-allowed;
  opacity: 0.6;
}

.time-slot-card.conflicted:hover {
  transform: none;
  box-shadow: none;
  background-color: #fff2f0;
}

.time-slot-card.disabled {
  background-color: #f5f5f5;
  border-color: #d9d9d9;
  cursor: not-allowed;
  opacity: 0.5;
  color: #999;
}

.time-slot-card.disabled:hover {
  transform: none;
  box-shadow: none;
  background-color: #f5f5f5;
  border-color: #d9d9d9;
}

.time-slot-card.disabled .slot-time {
  color: #999;
}

.time-slot-card.disabled .weekday-indicator {
  color: #ccc;
}

.unavailable-reason {
  font-size: 11px;
  color: #f56c6c;
  margin-top: 4px;
  text-align: center;
  line-height: 1.2;
}

.unavailable-summary {
  font-size: 11px;
  color: #f56c6c;
  text-align: center;
  margin-top: 4px;
}

.slot-time {
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  font-size: 14px;
}

.slot-duration-info {
  margin-bottom: 8px;
  display: flex;
  justify-content: center;
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

/* 冲突详情样式 */
.conflict-details {
  margin-top: 15px;
  padding: 15px;
  background-color: #fef7e6;
  border: 1px solid #ffd666;
  border-radius: 8px;
}

.conflict-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 12px;
  font-size: 14px;
}

.conflict-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.conflict-item {
  padding: 12px;
  background-color: #fff;
  border: 1px solid #ffd666;
  border-radius: 6px;
  font-size: 13px;
}

.conflict-time {
  margin-bottom: 6px;
}

.conflict-reason {
  color: #e6a23c;
  font-weight: 500;
  margin-bottom: 4px;
}

.conflict-dates {
  color: #666;
  font-size: 12px;
  margin-bottom: 4px;
}

.conflict-suggestion {
  color: #409eff;
  font-size: 12px;
  font-style: italic;
}

/* 课程安排行样式 */
.course-schedule-row {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

.schedule-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schedule-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 0;
}

.session-count-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-unit {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .course-schedule-row {
    flex-direction: column;
    gap: 15px;
  }

  .schedule-item {
    width: 100%;
  }

  .schedule-item .el-date-picker,
  .schedule-item .el-input-number,
  .schedule-item .el-input {
    width: 100% !important;
  }
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

/* 费用显示样式 */
.cost-summary {
  margin-top: 16px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.cost-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.cost-label {
  font-weight: 500;
  color: #333;
  font-size: 16px;
}

.cost-value {
  font-weight: 600;
  color: #e6a23c;
  font-size: 18px;
}

.cost-breakdown {
  text-align: center;
  margin-top: 4px;
}

.cost-detail {
  font-size: 12px;
  color: #909399;
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

/* 课程选择样式 */
.loading-courses {
  padding: 20px;
}

.no-courses {
  padding: 20px;
  text-align: center;
}

.course-selection {
  width: 100%;
}

.course-radio-group {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.course-radio-group .el-radio {
  margin-right: 0 !important;
}

.course-radio-item {
  width: 100%;
  margin: 0 0 16px 0 !important;
  padding: 0 !important;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  background-color: #fafafa;
  transition: all 0.3s;
  display: flex !important;
  align-items: flex-start !important;
  min-height: auto !important;
  height: auto !important;
}

.course-radio-item:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.course-radio-item.is-checked {
  border-color: #409eff;
  background-color: #e6f7ff;
}

.course-radio-item .el-radio__input {
  margin-top: 20px;
  margin-left: 16px;
  margin-right: 12px;
}

.course-radio-item .el-radio__label {
  padding: 16px 16px 16px 0;
  width: 100%;
  font-size: 14px;
  line-height: 1.5;
}

.course-info {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.course-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
  word-break: break-word;
}

.course-details {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.course-details .el-tag {
  margin: 0;
  font-size: 12px;
  height: 24px;
  line-height: 22px;
}

.course-description {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  word-break: break-word;
  margin-top: 4px;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .course-radio-item .el-radio__input {
    margin-top: 16px;
    margin-left: 12px;
    margin-right: 8px;
  }

  .course-radio-item .el-radio__label {
    padding: 12px 12px 12px 0;
  }

  .course-title {
    font-size: 15px;
  }

  .course-details .el-tag {
    font-size: 11px;
    height: 22px;
    line-height: 20px;
  }
}

/* 日期范围选择器样式 */
.date-range-container {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  width: 100%;
}

.date-input-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.date-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.date-separator {
  font-size: 16px;
  color: #909399;
  font-weight: 500;
  margin-bottom: 8px;
  padding: 0 8px;
}

/* 空日期单元格样式 */
.empty-day {
  visibility: hidden;
}

.day-cell.empty-day {
  pointer-events: none;
  background: transparent;
}

/* 响应式优化 - 日期选择器 */
@media (max-width: 768px) {
  .date-range-container {
    flex-direction: column;
    gap: 12px;
  }

  .date-separator {
    align-self: center;
    margin-bottom: 0;
  }
}

/* 时间匹配度提示样式 */
.time-match-info {
  margin: 16px 0;
}

.match-details {
  margin-top: 12px;
}

.match-score {
  margin-bottom: 12px;
}

.match-suggestion {
  font-size: 14px;
  color: #666;
}

.match-suggestion p {
  margin: 0 0 8px 0;
  font-weight: 500;
}

.match-suggestion ul {
  margin: 0;
  padding-left: 20px;
}

.match-suggestion li {
  margin: 4px 0;
  line-height: 1.4;
}

/* 紧凑的匹配度分析样式 */
.compact-match-analysis {
  width: 100%;
}

.compact-match-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  transition: all 0.3s ease;
}

.compact-match-card.success {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
}

.compact-match-card.warning {
  border-color: #e6a23c;
  background: linear-gradient(135deg, #fdf6ec 0%, #fef0e6 100%);
}

.compact-match-card.danger {
  border-color: #f56c6c;
  background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
}

.compact-match-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.compact-match-header:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.match-info-inline {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.match-status-indicator {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.compact-match-card.success .match-status-indicator {
  color: #67c23a;
}

.compact-match-card.warning .match-status-indicator {
  color: #e6a23c;
}

.compact-match-card.danger .match-status-indicator {
  color: #f56c6c;
}

.loading-icon {
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.match-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.4;
  flex: 1;
}

.match-score-inline {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-value {
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
}

.compact-match-card.success .score-value {
  color: #67c23a;
}

.compact-match-card.warning .score-value {
  color: #e6a23c;
}

.compact-match-card.danger .score-value {
  color: #f56c6c;
}

.expand-icon {
  font-size: 14px;
  color: #909399;
  transition: transform 0.3s ease;
}

.expand-icon.rotated {
  transform: rotate(180deg);
}

/* 紧凑详细内容样式 */
.compact-details-content {
  padding: 12px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(0, 0, 0, 0.02);
}

/* 统计信息行样式 */
.stats-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.stat-item-compact {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.stat-label-compact {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.stat-value-compact {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 冲突信息紧凑样式 */
.conflicts-compact {
  margin-bottom: 12px;
}

.conflicts-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #e6a23c;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
  padding: 4px 8px;
  border-radius: 4px;
}

.conflicts-header:hover {
  background-color: rgba(230, 162, 60, 0.1);
}

.conflict-expand-icon {
  margin-left: auto;
  font-size: 12px;
  transition: transform 0.3s ease;
}

.conflict-expand-icon.rotated {
  transform: rotate(180deg);
}

.conflicts-details-compact {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conflict-item-compact {
  background: rgba(230, 162, 60, 0.05);
  border-radius: 6px;
  padding: 8px;
  border-left: 3px solid #e6a23c;
}

.conflict-time-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.conflict-tag-compact {
  font-size: 12px;
  font-weight: 500;
}

.conflict-dates-inline {
  margin-right: 4px;
  font-weight: 600;
  color: #f56c6c;
}

.conflict-reason-text {
  font-size: 12px;
  color: #e6a23c;
  font-weight: 500;
}

.more-conflicts-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  padding: 6px 8px;
  margin-top: 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
  justify-content: center;
  background: rgba(0, 0, 0, 0.02);
}

.more-conflicts-toggle:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #606266;
}

.conflict-dates-compact {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #909399;
}

.date-icon {
  font-size: 12px;
}

.dates-text {
  line-height: 1.2;
}

.more-dates {
  color: #c0c4cc;
  font-style: italic;
}

.more-conflicts-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #909399;
  font-style: italic;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 4px;
}

/* 建议信息紧凑样式 */
.suggestion-compact {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #409eff;
  padding: 8px 12px;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 6px;
  border-left: 3px solid #409eff;
}



/* 区域标题样式 */
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.section-icon {
  color: #e6a23c;
  font-size: 18px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  flex: 1;
}

/* 冲突详情样式 */
.conflicts-section {
  margin-bottom: 24px;
}

.conflicts-grid {
  display: grid;
  gap: 12px;
}

.conflict-card {
  background: #fff;
  border: 1px solid #ffd666;
  border-radius: 8px;
  padding: 16px;
}

.conflict-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.conflict-time-tag {
  font-weight: 500;
}

.conflict-severity {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.conflict-severity.high {
  background: #fef0f0;
  color: #f56c6c;
}

.conflict-severity.medium {
  background: #fdf6ec;
  color: #e6a23c;
}

.conflict-severity.low {
  background: #f0f9ff;
  color: #409eff;
}

.severity-text {
  font-weight: 600;
}

.conflict-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conflict-reason {
  font-weight: 500;
  color: #e6a23c;
}

.conflict-dates,
.conflict-suggestion {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.more-dates {
  color: #909399;
  font-style: italic;
}

/* 建议区域样式 */
.suggestions-section {
  margin-bottom: 16px;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid;
}

.suggestion-item.high {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.suggestion-item.medium {
  background: #fdf6ec;
  border-left-color: #e6a23c;
}

.suggestion-item.low {
  background: #f0f9ff;
  border-left-color: #409eff;
}

.suggestion-icon {
  margin-top: 2px;
  font-size: 16px;
}

.suggestion-item.high .suggestion-icon {
  color: #f56c6c;
}

.suggestion-item.medium .suggestion-icon {
  color: #e6a23c;
}

.suggestion-item.low .suggestion-icon {
  color: #409eff;
}

.suggestion-content {
  flex: 1;
}

.suggestion-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.suggestion-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-row {
    flex-wrap: wrap;
    gap: 12px;
  }

  .stat-item-compact {
    min-width: calc(50% - 6px);
  }

  .compact-match-header {
    padding: 10px 12px;
  }

  .compact-details-content {
    padding: 10px 12px;
  }

  .match-description {
    font-size: 13px;
  }

  .score-value {
    font-size: 16px;
  }
}

.match-score-card.warning {
  border-color: #e6a23c;
  background: linear-gradient(135deg, #fffbf0 0%, #fef7e6 100%);
}

.match-score-card.danger {
  border-color: #f56c6c;
  background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
}

.match-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f5f5f5;
}

.match-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.match-score-card.success .match-icon {
  background: #67c23a;
  color: white;
}

.match-score-card.warning .match-icon {
  background: #e6a23c;
  color: white;
}

.match-score-card.danger .match-icon {
  background: #f56c6c;
  color: white;
}

.match-title h4 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.match-subtitle {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.match-content {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.match-progress-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.circular-progress {
  position: relative;
}

.progress-content {
  text-align: center;
}

.percentage {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}

.progress-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  margin-top: 4px;
}

.match-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  min-width: 80px;
}

.stat-number {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  margin-top: 4px;
}

.match-description {
  flex: 1;
}

.description-text {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

.desc-icon {
  font-size: 18px;
  color: #409eff;
}

.match-suggestions {
  margin-top: 16px;
}

.suggestions-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.suggestions-title .el-icon {
  font-size: 18px;
  color: #e6a23c;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  border-left: 4px solid #e6a23c;
  font-size: 14px;
  line-height: 1.5;
}

.suggestion-icon {
  font-size: 16px;
  color: #e6a23c;
  margin-top: 2px;
  flex-shrink: 0;
}

.match-success-info {
  margin-top: 16px;
}

.success-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: rgba(103, 194, 58, 0.1);
  border-radius: 12px;
  border: 2px solid #67c23a;
  font-size: 16px;
  font-weight: 600;
  color: #67c23a;
}

.success-icon {
  font-size: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .match-content {
    flex-direction: column;
    gap: 20px;
  }

  .match-stats {
    justify-content: center;
  }

  .stat-item {
    min-width: 70px;
    padding: 10px 12px;
  }

  .stat-number {
    font-size: 18px;
  }
}

/* 时间偏好选择容器样式 */
.time-preference-container,
.trial-time-preference-container {
  width: 100%;
  max-width: 100%;
  overflow: hidden;
}

.time-preference-checkbox-group,
.trial-time-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.time-preference-row {
  display: flex;
  gap: 12px;
  justify-content: flex-start;
  flex-wrap: wrap;
}

.time-preference-row:last-child {
  justify-content: flex-start;
}

/* 试听页：将上午/下午/晚上排列为两列，晚上正下方对齐上午 */
.trial-time-radio-group .time-preference-row {
  width: 100%;
}

.trial-time-radio-group .time-preference-row:first-child .el-radio-button,
.trial-time-radio-group .time-preference-row:last-child .el-radio-button {
  flex: 0 0 calc((100% - 12px) / 2);
  max-width: calc((100% - 12px) / 2);
}

/* 时间偏好按钮样式 */
.time-preference-checkbox-group .el-checkbox,
.trial-time-radio-group .el-radio-button {
  flex: 0 0 auto;
  min-width: 120px;
  max-width: 200px;
  margin: 0;
}

.time-preference-checkbox-group .el-checkbox {
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: #fff;
  transition: all 0.3s;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
}

.time-preference-checkbox-group .el-checkbox:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.time-preference-checkbox-group .el-checkbox.is-checked {
  border-color: #52c41a;
  background-color: #f6ffed;
}

.trial-time-radio-group .el-radio-button {
  flex: 0 0 auto;
  min-width: 120px;
  max-width: 200px;
}

.trial-time-radio-group .el-radio-button__inner {
  padding: 12px 16px;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  line-height: 1.2;
}

/* 试听页时间偏好按钮风格与智能匹配一致（仅作用于试听页单选按钮） */
.trial-time-radio-group .el-radio-button__inner {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: #fff;
  transition: all 0.3s;
  box-shadow: none;
}

.trial-time-radio-group .el-radio-button {
  margin: 0;
}

.trial-time-radio-group .el-radio-button:first-child .el-radio-button__inner,
.trial-time-radio-group .el-radio-button + .el-radio-button .el-radio-button__inner {
  border-left: 1px solid #e0e0e0;
}

.trial-time-radio-group .el-radio-button:hover .el-radio-button__inner {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.trial-time-radio-group .el-radio-button.is-active .el-radio-button__inner {
  border-color: #52c41a;
  background-color: #f6ffed;
  box-shadow: none;
  color: inherit;
}

/* 试听课时间选择样式 - 统一网格布局 */
.trial-time-container {
  margin: 20px 0;
}

.time-periods-header {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
  padding: 0 10px;
}

.period-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding: 12px;
  border-radius: 8px;
  background: #f8f9fa;
}

.period-header.morning {
  background: linear-gradient(135deg, #fff7e6 0%, #ffeaa7 100%);
  color: #e17055;
}

.period-header.afternoon {
  background: linear-gradient(135deg, #e6f3ff 0%, #74b9ff 100%);
  color: #0984e3;
}

.period-header.evening {
  background: linear-gradient(135deg, #f0e6ff 0%, #a29bfe 100%);
  color: #6c5ce7;
}

.period-header .el-icon {
  font-size: 18px;
}

.trial-time-unified-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 0 10px;
}

.trial-time-card-unified {
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
  position: relative;
  height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
}

.trial-time-card-unified:hover:not(.disabled) {
  border-color: #409eff;
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.15);
  transform: translateY(-3px);
}

.trial-time-card-unified.selected {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  box-shadow: 0 6px 16px rgba(103, 194, 58, 0.2);
  transform: translateY(-3px);
}

.trial-time-card-unified.disabled {
  border-color: #dcdfe6;
  background: #f5f7fa;
  color: #c0c4cc;
  cursor: not-allowed;
  opacity: 0.6;
}

/* 时间段特定样式 */
.trial-time-card-unified.morning-slot {
  border-left: 4px solid #e17055;
}

.trial-time-card-unified.afternoon-slot {
  border-left: 4px solid #0984e3;
}

.trial-time-card-unified.evening-slot {
  border-left: 4px solid #6c5ce7;
}

.trial-time-card-unified.morning-slot.selected {
  border-color: #e17055;
  background: linear-gradient(135deg, #fff7e6 0%, #ffeaa7 100%);
}

.trial-time-card-unified.afternoon-slot.selected {
  border-color: #0984e3;
  background: linear-gradient(135deg, #e6f3ff 0%, #ddeeff 100%);
}

.trial-time-card-unified.evening-slot.selected {
  border-color: #6c5ce7;
  background: linear-gradient(135deg, #f0e6ff 0%, #e6ddff 100%);
}

.time-display {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
  margin-bottom: 8px;
}

.time-duration {
  font-size: 13px;
  color: #909399;
  font-weight: 500;
  margin-bottom: 8px;
}

.time-status-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  min-height: 20px;
}

.status-text {
  color: #909399;
  font-weight: 500;
  font-size: 12px;
}

.status-icon {
  font-size: 16px;
}

.status-icon.selected {
  color: #67c23a;
}

.status-icon.disabled {
  color: #c0c4cc;
}

/* 选中状态的文字颜色 */
.trial-time-card-unified.selected .time-display {
  color: #303133;
  font-weight: 800;
}

.trial-time-card-unified.selected .time-duration {
  color: #606266;
  font-weight: 600;
}

.trial-time-card-unified.selected .status-text {
  color: #67c23a;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .trial-time-unified-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .trial-time-unified-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .time-periods-header {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .trial-time-card-unified {
    height: 100px;
    padding: 16px 12px;
  }

  .time-display {
    font-size: 16px;
  }
}

.selected-trial-result {
  margin-top: 24px;
}

.result-title {
  font-weight: 600;
  font-size: 16px;
}

.result-content {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}

.result-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #67c23a;
}

.result-duration {
  background: #67c23a;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

/* 课程时长选择样式 */
.course-duration-selection .el-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-duration-selection .el-radio {
  margin-right: 0;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s;
}

.course-duration-selection .el-radio:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.course-duration-selection .el-radio.is-checked {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.duration-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.duration-label {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.duration-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.4;
}

/* 课程时长信息显示样式 */
.course-duration-info {
  margin-top: 12px;
  padding: 8px 12px;
  background-color: #f0f9ff;
  border-radius: 6px;
  border: 1px solid #b3d8ff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.duration-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.duration-value {
  font-size: 14px;
  color: #409eff;
  font-weight: 600;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
}

.preview-slots {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.preview-slot {
  margin: 0;
}

.more-slots {
  color: #909399;
  font-size: 12px;
  font-style: italic;
}

/* 新的时间段选择样式 */
.time-period-selection {
  margin: 20px 0;
}

.period-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
  max-width: 600px;
  margin: 0 auto;
}

.period-button {
  flex: 1;
  min-width: 150px;
  max-width: 180px;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.period-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.period-button.is-active {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.3);
}

.trial-time-slots {
  margin-top: 24px;
}

.slots-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.time-slots-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  max-width: 100%;
}

/* 时间偏好选择响应式样式 */
@media (max-width: 768px) {
  .time-preference-checkbox-group .el-checkbox,
  .trial-time-radio-group .el-radio-button {
    min-width: 100px;
    max-width: 140px;
    padding: 10px 12px;
  }

  .trial-time-radio-group .el-radio-button__inner {
    padding: 10px 12px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .time-preference-checkbox-group .el-checkbox,
  .trial-time-radio-group .el-radio-button {
    min-width: 80px;
    max-width: 120px;
    padding: 8px 10px;
  }

  .trial-time-radio-group .el-radio-button__inner {
    padding: 8px 10px;
    font-size: 12px;
  }

  .time-preference-row {
    gap: 8px;
  }
}

.trial-time-slot-card {
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 16px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
  position: relative;
  height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.trial-time-slot-card:hover:not(.disabled) {
  border-color: #409eff;
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.15);
  transform: translateY(-3px);
}

.trial-time-slot-card.selected {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  box-shadow: 0 6px 16px rgba(103, 194, 58, 0.2);
  transform: translateY(-3px);
}

.trial-time-slot-card.disabled {
  border-color: #dcdfe6;
  background: #f5f7fa;
  color: #c0c4cc;
  cursor: not-allowed;
  opacity: 0.6;
}

.no-slots-message {
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .period-buttons {
    flex-direction: column;
    align-items: center;
    max-width: 300px;
  }

  .period-button {
    width: 100%;
    max-width: 250px;
    font-size: 14px;
    height: 45px;
  }

  .time-slots-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
  }

  .trial-time-slot-card {
    height: 90px;
    padding: 12px 8px;
  }
}

@media (max-width: 480px) {
  .period-buttons {
    max-width: 250px;
  }

  .period-button {
    max-width: 200px;
  }

  .time-slots-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }

  .trial-time-slot-card {
    height: 80px;
    padding: 10px 6px;
  }
}

</style>


