<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Timer, Refresh, Check, VideoCamera, ChatDotRound, InfoFilled, Download } from '@element-plus/icons-vue'
import { bookingAPI, rescheduleAPI } from '@/utils/api'

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
}

// 调课相关接口
interface RescheduleRequest {
  courseId: number;
  type: 'single' | 'recurring'; // 单次调课或周期性调课
  originalDate?: string; // 原定日期（单次调课）
  originalTime?: string; // 原定时间（单次调课）
  newDate?: string; // 新日期（单次调课）
  newTime?: string; // 新时间（单次调课）
  newWeeklySchedule?: string[]; // 新的每周时间安排（周期性调课）
  reason: string; // 调课原因
  applyDate: string; // 申请日期
  status: 'pending' | 'approved' | 'rejected'; // 待确认、已同意、已拒绝
}

// 响应式数据
const courses = ref<Course[]>([])
const loading = ref(false)
const schedules = ref<CourseSchedule[]>([])

// 获取学生课程安排
const loadStudentSchedules = async () => {
  try {
    loading.value = true

    // 获取未来3个月的课程安排
    const startDate = new Date().toISOString().split('T')[0]
    const endDate = new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]

    const result = await bookingAPI.getStudentSchedules({
      startDate,
      endDate
    })

    if (result.success && result.data) {
      schedules.value = result.data
      // 将课程安排转换为课程列表
      courses.value = convertSchedulesToCourses(result.data)
      // 加载调课申请状态
      await loadRescheduleStatus()
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

  scheduleList.forEach(schedule => {
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
    const completedCount = scheduleGroup.filter(s => s.status === 'completed').length
    const totalCount = firstSchedule.totalTimes || scheduleGroup.length
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
      schedules: scheduleGroup
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

// 可用时间段
const availableTimeSlots = ref<string[]>([
  '09:00-10:00', '10:00-11:00', '11:00-12:00',
  '14:00-15:00', '15:00-16:00', '16:00-17:00', '17:00-18:00',
  '18:00-19:00', '19:00-20:00', '20:00-21:00'
])

// 调课申请记录
const rescheduleRequests = ref<RescheduleRequest[]>([])

// 课程调课状态映射
const courseRescheduleStatus = ref<Map<number, string>>(new Map())

const showCourseDetail = (course: Course) => {
  currentCourse.value = course
  detailDialogVisible.value = true
}

// 显示调课弹窗
const showReschedule = (course: Course) => {
  currentRescheduleCourse.value = course
  rescheduleType.value = 'single'

  // 找到下次课程（最近的未来课程）
  const nextSchedule = course.schedules?.find(s =>
    new Date(s.scheduledDate) > new Date() && s.status === 'progressing'
  )

  let originalDate = ''
  let originalTime = ''

  if (nextSchedule) {
    originalDate = nextSchedule.scheduledDate
    originalTime = `${nextSchedule.startTime}-${nextSchedule.endTime}`
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

  // 清空表单
  if (type === 'single') {
    rescheduleForm.value.selectedWeekdays = []
    rescheduleForm.value.selectedTimeSlots = []
  } else {
    rescheduleForm.value.originalDate = ''
    rescheduleForm.value.originalTime = ''
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

// 获取星期几的中文名称
const getWeekdayName = (weekday: number): string => {
  const names = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return names[weekday] || '未知'
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
    const nextSchedule = currentRescheduleCourse.value.schedules?.find(s =>
      new Date(s.scheduledDate) > new Date() && s.status === 'progressing'
    )

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
      result.data.records.forEach((request: any) => {
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

// 格式化日期显示
const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
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
                    <el-button size="small" type="success" @click="contactTeacher(course.teacher)">
                      <el-icon><ChatDotRound /></el-icon> 联系教师
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
                />
                <el-select v-model="rescheduleForm.newTime" placeholder="选择新时间" style="width: 150px;">
                  <el-option
                    v-for="time in availableTimeSlots"
                    :key="time"
                    :label="time"
                    :value="time"
                  />
                </el-select>
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
                  v-for="time in availableTimeSlots"
                  :key="time"
                  :class="[
                    'time-slot-card',
                    { 'selected': rescheduleForm.selectedTimeSlots.includes(time) }
                  ]"
                  @click="() => {
                    if (rescheduleForm.selectedTimeSlots.includes(time)) {
                      rescheduleForm.selectedTimeSlots = rescheduleForm.selectedTimeSlots.filter(t => t !== time)
                    } else {
                      rescheduleForm.selectedTimeSlots.push(time)
                    }
                  }"
                >
                  <div class="slot-time">{{ time }}</div>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="选择星期" v-if="rescheduleForm.selectedTimeSlots.length > 0">
              <div class="weekday-selection">
                <el-checkbox-group v-model="rescheduleForm.selectedWeekdays">
                  <el-checkbox
                    v-for="weekday in [1, 2, 3, 4, 5, 6, 0]"
                    :key="weekday"
                    :label="weekday"
                  >
                    <span class="weekday-label">{{ getWeekdayName(weekday) }}</span>
                  </el-checkbox>
                </el-checkbox-group>
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
  background-color: #f8f8f8;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s;
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
</style>
