<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Timer, Refresh, Check, VideoCamera, InfoFilled, Clock, Collection, Reading, Loading, Star } from '@element-plus/icons-vue'
import { bookingAPI, rescheduleAPI, teacherAPI, studentAPI, apiRequest } from '../../../utils/api'

// 课程安排接口（基于后端ScheduleResponseDTO）
interface CourseSchedule {
  id: number;
  teacherId: number;
  teacherName: string;
  studentId: number;
  studentName: string;
  courseId: number;
  courseTitle: string;
  subjectName: string;
  scheduledDate: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  totalTimes: number;
  status: 'progressing' | 'completed' | 'cancelled';
  teacherNotes?: string;
  studentFeedback?: string;
  createdAt: string;
  bookingRequestId: number;
  bookingSource: string;
  isTrial: boolean;
  sessionNumber: number;
  courseType: string;
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

// 获取学生课程安排
const loadStudentSchedules = async () => {
  try {
    loading.value = true

    // 获取学生的所有课程安排，不限日期范围
    const result = await bookingAPI.getAllStudentSchedules()

    if (result.success && result.data) {
      schedules.value = result.data
      // 将课程安排转换为课程列表
      courses.value = convertSchedulesToCourses(result.data)
      // 加载调课申请状态
      await loadRescheduleStatus()
      // 加载课程评价状态
      await loadCourseEvaluationStatus()
    } else {
      ElMessage.error(result.message || '获取课程安排失败')
    }
  } catch (error) {
    console.error('获取课程安排失败:', error)
    ElMessage.error('获取课程安排失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 将课程安排转换为课程列表
const convertSchedulesToCourses = (scheduleList: CourseSchedule[]): Course[] => {
  // 按预约申请ID分组（每个预约申请对应一个课程系列）
  const courseGroups = new Map<number, CourseSchedule[]>()

  scheduleList.forEach((schedule, index) => {
    const groupKey = schedule.bookingRequestId
    if (!courseGroups.has(groupKey)) {
      courseGroups.set(groupKey, [])
    }
    courseGroups.get(groupKey)!.push(schedule)
  })

  // 转换为Course对象
  const courseList: Course[] = []

  courseGroups.forEach((scheduleGroup, bookingRequestId) => {
    const firstSchedule = scheduleGroup[0]

    // 过滤掉已取消的节次，只统计有效的节次
    const effectiveSchedules = scheduleGroup.filter(s => s.status !== 'cancelled')
    const completedCount = effectiveSchedules.filter(s => s.status === 'completed').length
    // 强制使用有效节次数量作为总节数，确保显示正确
    const totalCount = effectiveSchedules.length
    const progress = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0

    // 找到下一次课程
    const upcomingSchedules = scheduleGroup
      .filter(s => s.status === 'progressing' && new Date(s.scheduledDate) >= new Date())
      .sort((a, b) => new Date(a.scheduledDate).getTime() - new Date(b.scheduledDate).getTime())

    const nextSchedule = upcomingSchedules[0]
    const nextClass = nextSchedule
      ? `${nextSchedule.scheduledDate} ${nextSchedule.startTime}-${nextSchedule.endTime}`
      : '暂无安排'

    // 确定课程状态
    let status: 'active' | 'completed' | 'upcoming' = 'active'
    if (progress === 100) {
      status = 'completed'
    } else if (completedCount === 0 && scheduleGroup.every(s => new Date(s.scheduledDate) > new Date())) {
      // 只有当没有任何课程已完成且所有课程都在未来时，才设置为 upcoming
      status = 'upcoming'
    }

    courseList.push({
      id: bookingRequestId,
      title: firstSchedule.courseTitle,
      teacher: firstSchedule.teacherName,
      teacherAvatar: '@/assets/pictures/teacherBoy1.jpeg', // 默认头像
      subject: firstSchedule.subjectName,
      schedule: generateScheduleText(scheduleGroup),
      progress,
      nextClass,
      startDate: scheduleGroup[0]?.scheduledDate || '',
      endDate: scheduleGroup[scheduleGroup.length - 1]?.scheduledDate || '',
      totalLessons: totalCount,
      completedLessons: completedCount,
      remainingLessons: totalCount - completedCount,
      weeklySchedule: [],
      image: getSubjectImage(firstSchedule.subjectName),
      description: `${firstSchedule.courseTitle} - ${firstSchedule.subjectName}课程`,
      status,
      schedules: scheduleGroup,
      hasEvaluation: false // 默认未评价，后续会通过API检查
    })
  })

  return courseList
}

// 生成课程安排文本
const generateScheduleText = (schedules: CourseSchedule[]): string => {
  if (schedules.length === 0) return '暂无安排'

  // 简单处理：显示第一个安排的时间
  const first = schedules[0]
  return `${first.startTime}-${first.endTime}`
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
  loadStudentSchedules()
})

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

// 教师可用时间段（从后端获取）
interface TeacherTimeSlot {
  weekday: number
  timeSlots: string[]
}

const teacherAvailableTimeSlots = ref<TeacherTimeSlot[]>([])

// 获取教师可用时间
const loadTeacherAvailableTime = async (teacherId: number) => {
  try {
    const result = await teacherAPI.getAvailableTime(teacherId)
    if (result.success && result.data?.availableTimeSlots) {
      teacherAvailableTimeSlots.value = result.data.availableTimeSlots
    } else {
      teacherAvailableTimeSlots.value = []
    }
  } catch (error) {
    console.error('获取教师可用时间失败:', error)
    teacherAvailableTimeSlots.value = []
  }
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

  // 检查时间段是否在教师可用时间内
  return daySlot.timeSlots.includes(timeSlot)
}

// 获取可用的时间段（过滤掉不可用的）
// const getAvailableTimeSlotsForWeekday = (weekday: number): string[] => {
//   return availableTimeSlots.value.filter(timeSlot => isTimeSlotAvailable(weekday, timeSlot))
// }

// 获取所有时间段（用于周期性调课显示）
const getAllAvailableTimeSlots = computed(() => {
  // 始终返回完整的时间段列表，让用户看到所有选项
  return availableTimeSlots.value
})

// 检查时间段是否可选择（用于禁用不可用的时间段）
const isTimeSlotSelectable = (timeSlot: string): boolean => {
  if (teacherAvailableTimeSlots.value.length === 0) {
    // 如果教师未设置可用时间，默认所有时间可选
    return true
  }

  // 检查是否有任何星期几包含这个时间段
  return teacherAvailableTimeSlots.value.some(daySlot =>
    daySlot.timeSlots && daySlot.timeSlots.includes(timeSlot)
  )
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

  return schedules.find(s =>
    new Date(s.scheduledDate) > new Date() && s.status === 'progressing'
  )
}

// 课程调课状态映射
const courseRescheduleStatus = ref<Map<number, string>>(new Map())

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
      await loadStudentSchedules()
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

  // 找到下次课程（最近的未来课程）
  const nextSchedule = getNextSchedule(course.schedules)

  let originalDate = ''
  let originalTime = ''

  if (nextSchedule) {
    originalDate = nextSchedule.scheduledDate
    originalTime = `${nextSchedule.startTime}-${nextSchedule.endTime}`

    // 加载教师可用时间
    await loadTeacherAvailableTime(nextSchedule.teacherId)
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

// 检查日期是否可以调课（需要提前一天申请）
const canReschedule = (dateStr: string): boolean => {
  const targetDate = new Date(dateStr)
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  tomorrow.setHours(0, 0, 0, 0)

  return targetDate >= tomorrow
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

// 课程详情中的课程安排（过滤掉已取消的节次，后端已排序）
const sortedSchedules = computed(() => {
  if (!currentCourse.value?.schedules) return []
  return currentCourse.value.schedules.filter(s => s.status !== 'cancelled')
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

    // 检查时间限制
    if (!canReschedule(rescheduleForm.value.originalDate)) {
      ElMessage.error('调课需要提前一天申请')
      return
    }

    if (!canReschedule(rescheduleForm.value.newDate)) {
      ElMessage.error('新的上课时间需要是明天之后')
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
    const requestData = {
      scheduleId: nextSchedule.id,
      requestType: rescheduleType.value,
      reason: rescheduleForm.value.reason,
      urgencyLevel: 'medium' as const
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
      await loadStudentSchedules()
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

// 获取课程状态对应的CSS类
const getScheduleStatusClass = (status: string): string => {
  switch (status) {
    case 'completed':
      return 'schedule-completed'
    case 'cancelled':
      return 'schedule-cancelled'
    case 'progressing':
    default:
      return 'schedule-progressing'
  }
}

// 获取课程状态对应的标签类型
const getScheduleTagType = (status: string): string => {
  switch (status) {
    case 'completed':
      return 'success'
    case 'cancelled':
      return 'danger'
    case 'progressing':
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
    case 'progressing':
    default:
      return '进行中'
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

// 格式化日期时间
const formatDateTime = (dateTime: string): string => {
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
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
            <div v-if="loading" class="loading-container">
              <el-skeleton :rows="3" animated />
            </div>
            <el-empty v-else-if="filteredCourses.length === 0" description="暂无课程">
              <el-button type="primary" @click="loadStudentSchedules">刷新</el-button>
            </el-empty>
            <div v-else class="courses-grid">
              <el-card v-for="course in filteredCourses" :key="course.id" class="course-card" :body-style="{ padding: '0px' }">
                <div class="course-image">
                  <img :src="$getImageUrl(course.image)" :alt="course.title">
                  <div class="course-status">
                    <el-tag size="small" :type="course.status === 'active' ? 'success' : course.status === 'upcoming' ? 'warning' : 'info'">
                      {{ course.status === 'active' ? '进行中' : course.status === 'upcoming' ? '即将开始' : '已完成' }}
                    </el-tag>
                  </div>
                  <!-- 调课申请状态标签 -->
                  <div v-if="getRescheduleStatus(course.id)" class="reschedule-status-tag">
                    <el-tag type="warning" size="small">{{ getRescheduleStatus(course.id) }}</el-tag>
                  </div>
                </div>
                <div class="course-content">
                  <h3 class="course-title">{{ course.title }}</h3>
                  <div class="course-teacher">
                    <el-avatar :size="30" :src="$getImageUrl(course.teacherAvatar)"></el-avatar>
                    <span>{{ course.teacher }}</span>
                  </div>
                  <div class="course-progress">
                    <div class="progress-info">
                      <span class="progress-text">进度: {{ course.completedLessons }}/{{ course.totalLessons }} 课时</span>
                      <span v-if="course.remainingLessons" class="remaining-lessons">
                        剩余: {{ course.remainingLessons }} 课时
                      </span>
                    </div>
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
                    <el-button size="small" type="success" @click="showGradeChart(course)">
                      <el-icon><Reading /></el-icon> 查看成绩
                    </el-button>
                    <el-button
                      v-if="course.status === 'completed'"
                      size="small"
                      type="warning"
                      @click="showCourseEvaluation(course)"
                      :disabled="course.hasEvaluation"
                    >
                      <el-icon><Star /></el-icon> {{ course.hasEvaluation ? '已评价' : '评价课程' }}
                    </el-button>
                    <el-button
                      v-else-if="course.remainingLessons && course.remainingLessons > 0"
                      size="small"
                      type="warning"
                      @click="showReschedule(course)"
                      :disabled="!!getRescheduleStatus(course.id)"
                    >
                      <el-icon><Refresh /></el-icon> 调课
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
                    <el-button size="small" type="success" @click="showGradeChart(course)">
                      <el-icon><Reading /></el-icon> 查看成绩
                    </el-button>
                    <el-button
                      v-if="course.remainingLessons && course.remainingLessons > 0"
                      size="small"
                      type="warning"
                      @click="showReschedule(course)"
                      :disabled="!!getRescheduleStatus(course.id)"
                    >
                      <el-icon><Refresh /></el-icon> 调课
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
                    <el-button size="small" type="success" @click="showGradeChart(course)">
                      <el-icon><Reading /></el-icon> 查看成绩
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

    <!-- 调课申请弹窗 -->
    <el-dialog
      v-model="showRescheduleModal"
      :title="`调课申请 - ${currentRescheduleCourse?.title}`"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="reschedule-modal-content">
        <div class="reschedule-info">
          <el-alert
            title="调课须知"
            description="调课需要提前一天申请，老师确认后生效。剩余课时不变。"
            type="info"
            show-icon
            :closable="false"
          />
        </div>

        <div class="course-info">
          <div class="info-item">
            <span class="label">课程：</span>
            <span>{{ currentRescheduleCourse?.title }}</span>
          </div>
          <div class="info-item">
            <span class="label">教师：</span>
            <span>{{ currentRescheduleCourse?.teacher }}</span>
          </div>
          <div class="info-item">
            <span class="label">剩余课时：</span>
            <span class="remaining-highlight">{{ currentRescheduleCourse?.remainingLessons }}课时</span>
          </div>
          <div class="info-item" v-if="currentRescheduleCourse?.weeklySchedule">
            <span class="label">当前安排：</span>
            <div class="current-schedule">
              <el-tag v-for="schedule in currentRescheduleCourse.weeklySchedule" :key="schedule" size="small">
                {{ schedule }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="reschedule-type-selection">
          <el-radio-group v-model="rescheduleType" @change="switchRescheduleType">
            <el-radio-button label="single">
              <el-icon><Calendar /></el-icon>
              单次调课
            </el-radio-button>
            <el-radio-button label="recurring">
              <el-icon><Timer /></el-icon>
              周期性调课
            </el-radio-button>
          </el-radio-group>
        </div>

        <!-- 单次调课表单 -->
        <div v-if="rescheduleType === 'single'" class="single-reschedule-form">
          <el-form :model="rescheduleForm" label-width="120px">
            <el-form-item label="原定课程">
              <div class="original-schedule-display">
                <div class="schedule-info">
                  <el-tag type="info" size="large">
                    <el-icon><Calendar /></el-icon>
                    {{ rescheduleForm.originalDate ? new Date(rescheduleForm.originalDate).toLocaleDateString('zh-CN') : '未选择日期' }}
                  </el-tag>
                  <el-tag type="info" size="large">
                    <el-icon><Timer /></el-icon>
                    {{ rescheduleForm.originalTime || '未选择时间' }}
                  </el-tag>
                </div>
                <div class="schedule-note">
                  <el-text type="info" size="small">* 将调整下次即将到来的课程</el-text>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="调整到">
              <div class="new-schedule-group">
                <el-date-picker
                  v-model="rescheduleForm.newDate"
                  type="date"
                  placeholder="选择新日期"
                  :disabled-date="(date) => !canReschedule(date.toISOString().split('T')[0])"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 200px;"
                  @change="checkRescheduleTimeConflict"
                />
                <el-select
                  v-model="rescheduleForm.newTime"
                  placeholder="选择新时间"
                  style="width: 150px;"
                  @change="checkRescheduleTimeConflict"
                >
                  <el-option
                    v-for="time in availableTimeSlots"
                    :key="time"
                    :label="time"
                    :value="time"
                  />
                </el-select>
              </div>

              <!-- 时间冲突检查结果 -->
              <div v-if="timeConflictChecking || timeConflictResult" class="time-conflict-check">
                <div v-if="timeConflictChecking" class="checking">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>正在检查时间冲突...</span>
                </div>
                <div v-else-if="timeConflictResult" class="check-result">
                  <el-alert
                    :type="timeConflictResult.hasConflict ? 'error' : 'success'"
                    :title="timeConflictResult.message"
                    :closable="false"
                    show-icon
                  >
                    <template v-if="timeConflictResult.hasConflict && timeConflictResult.conflictType === 'teacher_unavailable'">
                      <div class="conflict-detail">
                        <p>教师在该时间段不可预约，建议：</p>
                        <ul>
                          <li>选择教师设置的可用时间段</li>
                          <li>或联系教师调整可用时间设置</li>
                        </ul>
                      </div>
                    </template>
                    <template v-else-if="timeConflictResult.hasConflict && timeConflictResult.conflictType === 'time_conflict'">
                      <div class="conflict-detail">
                        <p>该时间段已有其他课程安排，建议：</p>
                        <ul>
                          <li>选择其他可用时间段</li>
                          <li>或联系相关人员协调时间</li>
                        </ul>
                      </div>
                    </template>
                  </el-alert>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 周期性调课表单 -->
        <div v-if="rescheduleType === 'recurring'" class="recurring-reschedule-form">
          <el-form :model="rescheduleForm" label-width="120px">
            <el-form-item label="选择时间段">
              <div class="time-slot-selection">
                <div
                  v-for="time in getAllAvailableTimeSlots"
                  :key="time"
                  :class="[
                    'time-slot-card',
                    {
                      'selected': rescheduleForm.selectedTimeSlots.includes(time),
                      'disabled': !isTimeSlotSelectable(time)
                    }
                  ]"
                  @click="() => {
                    // 如果时间段不可选择，不允许点击
                    if (!isTimeSlotSelectable(time)) {
                      return
                    }

                    if (rescheduleForm.selectedTimeSlots.includes(time)) {
                      rescheduleForm.selectedTimeSlots = rescheduleForm.selectedTimeSlots.filter(t => t !== time)
                      // 清空不再可用的星期选择
                      rescheduleForm.selectedWeekdays = rescheduleForm.selectedWeekdays.filter(weekday =>
                        isWeekdaySelectable(weekday)
                      )
                    } else {
                      rescheduleForm.selectedTimeSlots.push(time)
                    }
                  }"
                >
                  <div class="slot-time">{{ time }}</div>
                  <div v-if="!isTimeSlotSelectable(time)" class="unavailable-overlay">
                    <span class="unavailable-text">不可用</span>
                  </div>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="选择星期" v-if="rescheduleForm.selectedTimeSlots.length > 0">
              <div class="weekday-selection">
                <el-checkbox-group v-model="rescheduleForm.selectedWeekdays">
                  <el-checkbox
                    v-for="weekday in [1, 2, 3, 4, 5, 6, 7]"
                    :key="weekday"
                    :label="weekday"
                    :disabled="!isWeekdaySelectable(weekday)"
                  >
                    <span class="weekday-label" :class="{ 'disabled': !isWeekdaySelectable(weekday) }">
                      {{ getWeekdayName(weekday) }}
                      <span v-if="!isWeekdaySelectable(weekday)" class="unavailable-hint">（不可用）</span>
                    </span>
                  </el-checkbox>
                </el-checkbox-group>
              </div>
              <div v-if="getAvailableWeekdays.length === 0" class="no-available-weekdays">
                <el-text type="warning">所选时间段在任何星期都不可用，请重新选择时间段</el-text>
              </div>
            </el-form-item>

            <el-form-item label="新的安排" v-if="rescheduleForm.selectedWeekdays.length > 0 && rescheduleForm.selectedTimeSlots.length > 0">
              <div class="schedule-preview">
                <div class="preview-content">
                  <span class="preview-label">每周：</span>
                  <div class="preview-tags">
                    <el-tag type="success" size="large">
                      {{ rescheduleForm.selectedWeekdays.map(day => getWeekdayName(day)).join('、') }}
                    </el-tag>
                    <div class="time-tags">
                      <el-tag
                        v-for="timeSlot in rescheduleForm.selectedTimeSlots"
                        :key="timeSlot"
                        type="primary"
                        size="large"
                      >
                        {{ timeSlot }}
                      </el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <el-form :model="rescheduleForm" label-width="120px">
          <el-form-item label="调课原因" required>
            <el-input
              v-model="rescheduleForm.reason"
              type="textarea"
              :rows="3"
              placeholder="请填写调课原因，以便老师了解情况"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showRescheduleModal = false">取消</el-button>
          <el-button
            type="primary"
            @click="submitReschedule"
            :disabled="rescheduleType === 'single' ?
              (!rescheduleForm.originalDate || !rescheduleForm.originalTime || !rescheduleForm.newDate || !rescheduleForm.newTime) :
              (rescheduleForm.selectedWeekdays.length === 0 || rescheduleForm.selectedTimeSlots.length === 0)"
          >
            <el-icon><Check /></el-icon>
            提交申请
          </el-button>
        </span>
      </template>
    </el-dialog>

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

.schedule-progressing .schedule-number span {
  background-color: #409eff;
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
</style>
