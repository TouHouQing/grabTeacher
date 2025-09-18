<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, School, Trophy, ArrowLeft, Loading, Clock, User, Document, Timer } from '@element-plus/icons-vue'
import { courseAPI, teacherAPI, enrollmentAPI, publicGradeAPI, publicTeachingLocationAPI, bookingAPI } from '../../utils/api'
import StudentBookingCalendar from '../../components/StudentBookingCalendar.vue'
import SimpleCourseCalendar from '../../components/SimpleCourseCalendar.vue'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 获取路由传递的课程ID并转换为数字
const courseId = parseInt(route.params.id as string)

// 课程信息状态
const course = ref<any>(null)
const loading = ref(true)
const enrolling = ref(false)
const enrolled = ref(false)
// 额外教师信息
const teacher = ref<{ avatarUrl?: string; teachingExperience?: number; educationBackground?: string; specialties?: string; rating?: number; level?: string } | null>(null)

// 年级选择相关状态
const showGradeModal = ref(false)
// 直接在本页弹“课程安排”弹窗（用于一对一）
const showScheduleModal = ref(false)
const calendarDlg = ref<InstanceType<typeof StudentBookingCalendar> | null>(null)
// 简洁课表日历状态（仅展示）
const showSimpleCalendar = ref(false)
const simpleSessions = ref<Array<{ date: string; startTime: string; endTime: string }>>([])
const simpleStart = ref<string>('')
const simpleEnd = ref<string>('')

const selectedTeachingLocation = ref<string | number>('')
const allowedTeachingLocations = ref<Array<{ value: string | number; label: string }>>([])
const grades = ref<{ id: string | number; name: string }[]>([])
const selectedGrade = ref<string | number>('')
const loadingGrades = ref(false)

// 科目选择：详情页固定为课程科目，不再二次选择

// 偏好上课时间选择
const selectedPeriods = ref<string[]>([])

// 偏好开始/结束日期
const preferredStartDate = ref<string>('')
const preferredEndDate = ref<string>('')

// 每周上课时间段类型（与后端 TimeSlotDTO 对齐）
interface TimeSlotDTO {
  weekday: number // 1=周一 ... 7=周日
  timeSlots: string[]
}

const getWeekdayName = (n: number) => {
  const map = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return map[n] || '未知'
}

const sortedCourseTimeSlots = computed(() => {
  const slots = (course.value?.courseTimeSlots || []) as TimeSlotDTO[]
  return [...slots]
    .filter(s => s && Array.isArray(s.timeSlots) && s.timeSlots.length > 0)
    .sort((a, b) => (a.weekday || 0) - (b.weekday || 0))
})



// 返回上一页
const goBack = () => {
  router.back()
}

// 获取课程详情数据
const fetchCourseDetail = async () => {
  try {
    loading.value = true
    const result = await courseAPI.getPublicCourseById(courseId)

    if (result.success && result.data) {
      course.value = result.data
      // 从课程详情接口推断“已报名”状态（若后端返回该字段）
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const anyData: any = result.data as any
      if (typeof anyData.enrolled !== 'undefined') {
        enrolled.value = Boolean(anyData.enrolled)
      }

      // 额外拉取教师信息（头像、年限、教育背景、特长）
      try {
        const tRes = await teacherAPI.getDetail(result.data.teacherId)
        if (tRes.success && tRes.data) {
          teacher.value = {
            avatarUrl: tRes.data.avatarUrl,
            teachingExperience: tRes.data.teachingExperience,
            educationBackground: tRes.data.educationBackground,
            specialties: tRes.data.specialties,
            rating: tRes.data.rating,
            level: tRes.data.level,
          }
          // 根据教师配置构建授课地点选项
          try {
            const locResp = await publicTeachingLocationAPI.getActive()
            const list = locResp?.success ? (locResp.data || []) : []
            const opts: Array<{ value: string | number; label: string }> = []
            if (tRes.data.supportsOnline) opts.push({ value: 'online', label: '线上' })
            const ids: number[] = Array.isArray(tRes.data.teachingLocationIds) ? tRes.data.teachingLocationIds : []
            ids.forEach((id: number) => {
              const item = list.find((x: any) => x.id === id)
              if (item) opts.push({ value: id, label: item.name })
            })
            allowedTeachingLocations.value = opts
            if (!selectedTeachingLocation.value && opts.length > 0) selectedTeachingLocation.value = opts[0].value
          } catch {
            allowedTeachingLocations.value = []
          }
        }
      } catch (e) { /* 静默失败，不影响课程详情 */ }
    } else {
      ElMessage.error(result.message || '获取课程详情失败')
      // 如果获取失败，3秒后返回上一页
      setTimeout(() => {
        goBack()
      }, 3000)
    }
  } catch (error) {
    console.error('获取课程详情失败:', error)
    ElMessage.error('获取课程详情失败，请稍后重试')
    // 如果获取失败，3秒后返回上一页
    setTimeout(() => {
      goBack()
    }, 3000)
  } finally {
    loading.value = false
  }
}

// 跳转到教师详情
const goTeacherDetail = () => {
  const id = course.value?.teacherId
  if (id) router.push(`/teacher-detail/${id}`)
}

onMounted(() => {
  // 页面加载时滚动到顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })

  // 获取课程详情
  fetchCourseDetail()
})

// 加载年级列表
const loadGrades = async () => {
  try {
    loadingGrades.value = true
    const res = await publicGradeAPI.list()
    if (res.success && Array.isArray(res.data)) {
      grades.value = res.data.map((g: any) => ({ id: g.id ?? g.name, name: g.name ?? String(g) }))
    } else {
      grades.value = []
    }
  } catch (e) {
    grades.value = []
    ElMessage.error('加载年级列表失败')
  } finally {
    loadingGrades.value = false
  }
}


// 显示年级选择弹窗
const showGradeSelection = async () => {
  if (grades.value.length === 0) {
    await loadGrades()
  }
  selectedGrade.value = ''
  showGradeModal.value = true
}

// 确认年级选择并跳转
const confirmGradeSelection = () => {
  if (!selectedGrade.value) {
    ElMessage.warning('请选择年级')
    return
  }

  const teacherId = course.value?.teacherId
  if (!teacherId) {
    ElMessage.warning('未找到授课教师，暂无法预约')
    return
  }

  // 跳转并携带 teacherId、courseId 和选择的年级
  // 找到对应的年级名称
  const gradeItem = grades.value.find(g => g.id === selectedGrade.value)
  const gradeName = gradeItem ? gradeItem.name : String(selectedGrade.value)

  router.push({
    path: '/student-center/match',
    query: {
      teacherId: String(teacherId),
      courseId: String(courseId),
      grade: gradeName,
      openSchedule: '1'
    }
  })

  showGradeModal.value = false
}

// 立即报名：一对一课程先选择年级再跳转；小班课按原流程报名
const enrollCourse = async () => {
  try {
    if (!userStore.isLoggedIn) {
      await ElMessageBox.alert('请登录或联系客服预约课程', '提示', { confirmButtonText: '确定' })
      return
    }
    if (!userStore.isStudent) {
      ElMessage.warning('请使用学生账号登录后报名')
      return
    }
    if (!course.value) return

    // 一对一课程：直接在本页弹出课程安排（包含科目、年级、偏好时间、日期范围、授课地点选择）
    if (course.value.courseType === 'one_on_one') {
      if (allowedTeachingLocations.value.length === 0) {
        // 若尚未加载地点，尝试补载
        try {
          const opts: Array<{ value: string | number; label: string }> = []
          // 默认支持线上
          opts.push({ value: 'online', label: '线上' })
          allowedTeachingLocations.value = opts
        } catch {}
      }

      // 重置表单状态（科目固定为课程科目）
      selectedGrade.value = ''
      selectedPeriods.value = []
      preferredStartDate.value = ''
      preferredEndDate.value = ''
      selectedTeachingLocation.value = allowedTeachingLocations.value.length > 0 ? allowedTeachingLocations.value[0].value : ''

      showScheduleModal.value = true
      return
    }

    // 已报名则提示并返回（仅针对小班课）
    if (enrolled.value) {
      ElMessage.info('您已报名该课程')
      return
    }

    if (course.value.courseType !== 'large_class') {
      ElMessage.info('该课程请联系客服预约或选择合适时间')
      return
    }

    await ElMessageBox.confirm(`确认报名该课程：${course.value.title}？`, '确认报名', {
      type: 'warning',
      confirmButtonText: '确认报名',
      cancelButtonText: '取消'
    })

    enrolling.value = true
    const res = await enrollmentAPI.enrollCourse(courseId)
    if (res.success) {
      ElMessage.success('报名成功')
      enrolled.value = true
    } else {
      const msg = res.message || '报名失败，请稍后重试'
      if (msg.includes('已报名')) {
        ElMessage.info('您已报名该课程')
        enrolled.value = true
      } else if (msg.includes('余额不足')) {
        ElMessage.warning('您的M豆余额不足，请联系客服预约课程')
      } else {
        ElMessage.error(msg)
      }
    }
  } catch (err: unknown) {
    if (err === 'cancel') {
      ElMessage.info('已取消报名')
    } else {
      console.error('报名失败:', err)
      ElMessage.error('报名失败，请稍后重试')
    }
  } finally {
    enrolling.value = false
  }
}

// 详情页打开日历（用于一对一预约，在本页直接弹出可选日历）
const openCalendarFromDetail = () => {
  if (!selectedGrade.value) { ElMessage.warning('请先选择年级'); return }
  if (selectedPeriods.value.length === 0) { ElMessage.warning('请选择偏好上课时间段'); return }
  if (!preferredStartDate.value) { ElMessage.warning('请选择开始上课日期'); return }
  if (!preferredEndDate.value) { ElMessage.warning('请选择结束上课日期'); return }
  if (!selectedTeachingLocation.value) { ElMessage.warning('请选择授课地点'); return }

  // 验证日期范围
  if (preferredStartDate.value > preferredEndDate.value) {
    ElMessage.warning('结束日期不能早于开始日期')
    return
  }

  // 打开可选择的日历对话框（由 StudentBookingCalendar 控制显示）
  calendarDlg.value?.open?.()
}

// 小班课：查看课表（仅展示，禁止多选，基于课程已有/推导的 sessions 进行回显）
const openScheduleView = () => {
  if (!course.value) return
  if (course.value.courseType !== 'large_class') return

  const start = course.value.startDate
  const end = course.value.endDate

  // 优先使用后端 sessions，如无则根据每周安排推导
  const anyCourse: any = course.value as any
  let sessions: Array<{ date: string; startTime: string; endTime: string }> = []
  if (Array.isArray(anyCourse.sessions) && anyCourse.sessions.length > 0) {
    sessions = anyCourse.sessions.map((s: any) => ({ date: s.date, startTime: s.startTime, endTime: s.endTime }))
  } else if (start && end && Array.isArray(course.value.courseTimeSlots)) {
    const weekly: Array<{ weekday: number; timeSlots: string[] }> = course.value.courseTimeSlots as any
    const mapByWeekday: Record<number, string[]> = {}
    for (const w of weekly) {
      if (!mapByWeekday[w.weekday]) mapByWeekday[w.weekday] = []
      mapByWeekday[w.weekday].push(...w.timeSlots)
    }
    let cursor = dayjs(start)
    const endDay = dayjs(end)
    while (cursor.isBefore(endDay) || cursor.isSame(endDay, 'day')) {
      const jsDay = cursor.day() // 0(日) - 6(六)
      const weekday = jsDay === 0 ? 7 : jsDay // 1-7
      const slots = mapByWeekday[weekday] || []
      for (const t of slots) {
        const [st, et] = String(t).split('-')
        if (st && et) sessions.push({ date: cursor.format('YYYY-MM-DD'), startTime: st, endTime: et })
      }
      cursor = cursor.add(1, 'day')
    }
  }

  simpleStart.value = start || ''
  simpleEnd.value = end || ''
  simpleSessions.value = sessions
  showSimpleCalendar.value = true
}

// 从日历确认后的提交
const onCalendarConfirmFromDetail = async (sessions: Array<{ date: string; startTime: string; endTime: string }>, duration: 90|120) => {
  try {
    // 在提交前检查所有会话的时间冲突
    ElMessage.info('正在检查时间冲突...')
    const conflictSessions = []

    for (const session of sessions) {
      try {
        const conflictResult = await bookingAPI.checkFormalBookingConflict(
          course.value?.teacherId || 0,
          session.date,
          session.startTime,
          session.endTime
        )

        if (conflictResult.success && conflictResult.data === true) {
          conflictSessions.push(`${session.date} ${session.startTime}-${session.endTime}`)
        }
      } catch (error) {
        console.warn(`检查时间冲突失败 ${session.date} ${session.startTime}-${session.endTime}:`, error)
      }
    }

    if (conflictSessions.length > 0) {
      ElMessage.error(`以下时间段存在冲突，请重新选择：\n${conflictSessions.join('\n')}`)
      return
    }

    // 构建偏好时间段
    const preferredTimeSlots: string[] = []
    if (selectedPeriods.value.includes('morning')) {
      preferredTimeSlots.push('08:00-10:00', '10:00-12:00')
    }
    if (selectedPeriods.value.includes('afternoon')) {
      preferredTimeSlots.push('13:00-15:00', '15:00-17:00')
    }
    if (selectedPeriods.value.includes('evening')) {
      preferredTimeSlots.push('17:00-19:00', '19:00-21:00')
    }

    const bookingData: any = {
      teacherId: course.value?.teacherId,
      courseId: course.value?.id,
      subject: course.value?.subjectName,
      grade: grades.value.find(g => g.id === selectedGrade.value)?.name || String(selectedGrade.value),
      preferredTimeSlots: preferredTimeSlots,
      preferredDateStart: preferredStartDate.value,
      preferredDateEnd: preferredEndDate.value,
      bookingType: 'calendar' as const,
      isTrial: false,
      selectedDurationMinutes: duration,
      selectedSessions: sessions
    }
    if (selectedTeachingLocation.value === 'online') bookingData.teachingLocation = '线上'
    else bookingData.teachingLocationId = Number(selectedTeachingLocation.value)

    const result = await bookingAPI.createRequest(bookingData)
    if (result.success) {
      ElMessage.success(`已提交预约申请（${sessions.length}次）`)
      showScheduleModal.value = false
    } else {
      ElMessage.error(result.message || '预约提交失败')
    }
  } catch (e:any) {
    ElMessage.error(e?.message || '预约提交失败')
  }
}



// 获取状态类型
const getStatusType = (status: string) => {
  switch (status) {
    case 'active':
      return 'success'
    case 'inactive':
      return 'info'
    case 'full':
      return 'warning'
    case 'pending':
      return 'warning'
    default:
      return 'info'
  }
}

// 将特长字符串切分为标签数组
const parseSpecialties = (val?: string) => {
  if (!val) return []
  return val.split(/[，,\s]+/).filter(Boolean).slice(0, 6)
}

const specialtyChips = computed(() => {
  const arr = parseSpecialties(teacher.value?.specialties)
  const visible = arr.slice(0, 3)
  const rest = arr.length > 3 ? arr.length - 3 : 0
  return { visible, rest }
})

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'active':
      return '正在招生'
    case 'inactive':
      return '已下架'
    case 'full':
      return '已满员'
    case 'pending':
      return '待审批'
    default:
      return status
  }
}
</script>

<template>
  <div class="course-detail-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="loading-icon" :size="50"><Loading /></el-icon>
      <p>正在加载课程详情...</p>
    </div>

    <!-- 课程详情内容 -->
    <div v-else-if="course" class="course-detail-content">
      <!-- 返回按钮 -->
      <div class="back-button">
        <el-button @click="goBack" type="primary" plain>
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </div>

      <!-- 课程基本信息 -->
      <div class="course-header">
        <div class="course-image">
          <img :src="course.imageUrl || '/src/assets/pictures/courseBackground1.jpeg'" :alt="course.title">
        </div>
        <div class="course-info">
          <h1 class="course-title">{{ course.title }}</h1>
          <div class="course-meta">
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>{{ course.teacherName }}</span>
            </div>
            <div class="meta-item">
              <el-icon><School /></el-icon>
              <span>{{ course.subjectName }}</span>
            </div>
            <div class="meta-item" v-if="course.durationMinutes && course.courseType !== 'one_on_one'">
              <el-icon><Clock /></el-icon>
              <span>{{ Math.floor(course.durationMinutes / 60) }}小时</span>
            </div>
            <div class="meta-item" v-if="course.courseTypeDisplay">
              <el-icon><Trophy /></el-icon>
              <span>{{ course.courseTypeDisplay }}</span>
            </div>
          </div>
          <div class="course-price" v-if="course.price">
            <span class="price-label">课程价格：</span>
            <span class="price-value">{{ course.price }} M豆/h</span>
          </div>
          <div class="course-actions">
            <el-button type="primary" size="large"
                       :loading="enrolling"
                       :disabled="enrolling"
                       @click="enrollCourse">
              <el-icon><Calendar /></el-icon>
              立即报名
            </el-button>
            <el-button
              v-if="course.courseType === 'large_class'"
              size="large"
              type="warning"
              @click="openScheduleView"
            >查看课表</el-button>
            <el-button size="large" @click="goBack">返回</el-button>
          </div>
        </div>
      </div>

      <!-- 课程详细信息 -->
      <div class="course-details">
        <h2>课程详情</h2>
        <div class="details-grid">
          <div class="detail-item">
            <el-icon><Trophy /></el-icon>
            <span class="label">课程类型：</span>
            <span class="value">{{ course.courseType === 'one_on_one' ? '一对一' : course.courseType === 'large_class' ? '小班课' : course.courseType }}</span>
          </div>
          <div class="detail-item" v-if="course.startDate">
            <el-icon><Calendar /></el-icon>
            <span class="label">开始日期：</span>
            <span class="value">{{ course.startDate }}</span>
          </div>
          <div class="detail-item" v-if="course.endDate">
            <el-icon><Calendar /></el-icon>
            <span class="label">结束日期：</span>
            <span class="value">{{ course.endDate }}</span>
          </div>
          <div class="detail-item" v-if="course.personLimit">
            <el-icon><User /></el-icon>
            <span class="label">人数限制：</span>
            <span class="value">{{ course.personLimit }}人</span>
          </div>
          <div class="detail-item" v-if="course.courseType !== 'one_on_one'">
            <el-icon><Timer /></el-icon>
            <span class="label">课程时长：</span>
            <span class="value">{{ course.durationMinutes }}分钟</span>
          </div>
          <div class="detail-item">
            <el-icon><Document /></el-icon>
            <span class="label">课程状态：</span>
            <el-tag :type="getStatusType(course.status)">
              {{ getStatusText(course.status) }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 教师信息 -->
      <div class="teacher-info" v-if="course.teacherName">
        <h2>授课教师</h2>
        <div class="teacher-card" role="button" tabindex="0" @click="goTeacherDetail" @keyup.enter="goTeacherDetail">
          <div class="teacher-avatar">
            <img :src="teacher?.avatarUrl || $getImageUrl('@/assets/pictures/teacherBoy1.jpeg')" alt="教师头像" />
          </div>
          <div class="teacher-details">
            <h3>{{ course.teacherName }}</h3>
            <p class="teacher-subject">{{ course.subjectName }}专业教师</p>
            <div class="teacher-extra-grid">
              <div class="teacher-extra-item" v-if="teacher?.rating != null">
                <el-icon><Trophy /></el-icon>
                <div class="kv">
                  <div class="label">教师评分</div>
                  <div class="value">{{ Number(teacher?.rating || 0).toFixed(1) }} 分</div>
                </div>
              </div>
              <div class="teacher-extra-item" v-if="teacher?.level">
                <el-icon><User /></el-icon>
                <div class="kv">
                  <div class="label">教师级别</div>
                  <div class="value">{{ teacher?.level }}</div>
                </div>
              </div>
              <div class="teacher-extra-item" v-if="teacher?.teachingExperience != null">
                <el-icon><Timer /></el-icon>
                <div class="kv">
                  <div class="label">教学年龄</div>
                  <div class="value">{{ teacher?.teachingExperience }}年</div>
                </div>
              </div>
              <div class="teacher-extra-item" v-if="teacher?.educationBackground">
                <el-icon><School /></el-icon>
                <div class="kv">
                  <div class="label">文凭背景</div>
                  <el-tooltip placement="top" :content="teacher?.educationBackground">
                    <div class="value ellipsis-1">{{ teacher?.educationBackground }}</div>
                  </el-tooltip>
                </div>
              </div>
              <div class="teacher-extra-item" v-if="teacher?.specialties">
                <el-icon><Document /></el-icon>
                <div class="kv">
                  <div class="label">特长</div>
                  <div class="chip-list">
                    <el-tag
                      v-for="(sp, idx) in specialtyChips.visible"
                      :key="idx"
                      size="small"
                      effect="light"
                      type="success"
                      class="chip"
                    >{{ sp }}</el-tag>
                    <el-tag v-if="specialtyChips.rest > 0" size="small" effect="plain" type="info" class="chip">+{{ specialtyChips.rest }}</el-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 课程详细描述 -->
      <div class="course-description" v-if="course.description">
        <h2>课程介绍</h2>
        <div class="description-content">
          {{ course.description }}
        </div>
      </div>

      <!-- 一对一课程安排弹窗 -->
      <el-dialog
        v-model="showScheduleModal"
        :title="`${course.teacherName} - 课程安排`"
        width="800px"
        :close-on-click-modal="false"
        destroy-on-close
      >
        <div class="schedule-modal-content">
          <el-form label-width="120px">
            <el-form-item label="科目" required>
              <el-input :model-value="course.subjectName" disabled />
            </el-form-item>

            <el-form-item label="年级" required>
              <el-select v-model="selectedGrade" placeholder="请选择年级" style="width: 100%" :loading="loadingGrades" @visible-change="(v)=>{ if(v && grades.length===0) loadGrades() }">
                <el-option v-for="g in grades" :key="g.id" :label="g.name" :value="g.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="偏好上课时间" required>
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

            <el-form-item label="开始上课日期" required>
              <el-date-picker
                v-model="preferredStartDate"
                type="date"
                placeholder="请选择开始日期"
                :disabled-date="(date) => date < new Date()"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="结束上课日期" required>
              <el-date-picker
                v-model="preferredEndDate"
                type="date"
                placeholder="请选择结束日期"
                :disabled="!preferredStartDate"
                :disabled-date="(date) => preferredStartDate ? date < new Date(preferredStartDate) : false"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="授课地点" required>
              <el-radio-group v-model="selectedTeachingLocation">
                <el-radio-button v-for="opt in allowedTeachingLocations" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-alert type="info" :closable="false" title="请完成所有必填项后，点击下方打开日历选择" />
          </el-form>
        </div>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="showScheduleModal = false">取消</el-button>
            <el-button type="primary" @click="openCalendarFromDetail" :disabled="!selectedGrade || selectedPeriods.length === 0 || !preferredStartDate || !preferredEndDate || !selectedTeachingLocation">
              <el-icon><Calendar /></el-icon> 打开日历选择
            </el-button>
          </span>
        </template>
      </el-dialog>

      <StudentBookingCalendar ref="calendarDlg" :teacher-id="course.teacherId" @confirm="onCalendarConfirmFromDetail" />
      <!-- 小班课查看课表：使用简洁只读日历 -->
      <el-dialog
        v-model="showSimpleCalendar"
        title="课表"
        width="900px"
        destroy-on-close
        :close-on-click-modal="true"
      >
        <SimpleCourseCalendar :sessions="simpleSessions" :start="simpleStart" :end="simpleEnd" />
      </el-dialog>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error-container">
      <p>课程不存在或已被删除</p>
      <el-button @click="goBack" type="primary">返回</el-button>
    </div>

    <!-- 年级选择弹窗 -->
    <el-dialog
      v-model="showGradeModal"
      title="选择年级"
      width="400px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="grade-selection">
        <p class="grade-tip">请选择您的年级，以便为您匹配合适的教师</p>
        <el-form :model="{}" label-width="80px">
          <el-form-item label="年级" required>
            <el-select
              v-model="selectedGrade"
              placeholder="请选择年级"
              style="width: 100%"
              :loading="loadingGrades"
            >
              <el-option
                v-for="grade in grades"
                :key="grade.id"
                :label="grade.name"
                :value="grade.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="showGradeModal = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmGradeSelection"
          :disabled="!selectedGrade"
        >
          确认并跳转
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.course-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
}

.loading-container, .error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
}

.loading-icon {
  animation: spin 1s linear infinite;
  color: #409eff;
  margin-bottom: 20px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.course-detail-content {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.back-button {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.course-header {
  display: flex;
  gap: 30px;
  padding: 30px;
}

.course-image {
  flex-shrink: 0;
  width: 400px;
  height: 300px;
  border-radius: 12px;
  overflow: hidden;
}

.course-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.course-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
}

.course-price {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
}

.price-label {
  color: #666;
}

.price-value {
  color: #e6a23c;
  font-weight: 600;
  font-size: 24px;
}

.course-actions {
  display: flex;
  gap: 15px;
  margin-top: auto;
}

.course-details, .teacher-info, .course-description {
  padding: 30px;
  border-top: 1px solid #eee;
}

.course-details h2, .teacher-info h2, .course-description h2 {
  font-size: 20px;
  color: #333;
  margin-bottom: 20px;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.detail-item .label {
  font-weight: 500;
  color: #333;
}

.detail-item .value {
  color: #666;
}

.teacher-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.1s ease, opacity 0.2s ease;
  -webkit-tap-highlight-color: transparent;
}
.teacher-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.teacher-card:active {
  transform: scale(0.98);
  opacity: 0.92;
}
.teacher-card:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.25);
}

.teacher-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.teacher-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.teacher-details h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #333;
}

.teacher-subject {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
}

.teacher-extra-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 16px;
  align-items: start;
}
.teacher-extra-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
}
.teacher-extra-item .kv .label { font-size: 12px; color: #999; }
.teacher-extra-item .kv .value { font-size: 14px; color: #333; }
.ellipsis-1 { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.chip-list { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { --el-tag-font-size: 12px; }





.description-content {
  color: #666;
  line-height: 1.6;
  font-size: 16px;
}

@media (max-width: 768px) {
  .course-header {
    flex-direction: column;
    gap: 20px;
  }

  .course-image {
    width: 100%;
    height: 250px;
  }

  .course-actions {
    flex-direction: column;
  }

  .details-grid {
    grid-template-columns: 1fr;
  }

  .teacher-card {
    flex-direction: column;
    text-align: center;
  }

  .teacher-extra-grid {
    grid-template-columns: 1fr;
  }
}

/* 小屏与小桌面进一步优化布局 */
@media (max-width: 992px) {
  .teacher-extra-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  /* 弹窗在小屏全宽 + 表单标签置顶 */
  :deep(.el-dialog) { width: 100vw !important; max-width: 100vw !important; margin: 0 !important; }
  :deep(.el-dialog__body) { padding: 12px; }
  :deep(.el-dialog .el-form .el-form-item__label) { float: none; display: block; padding-bottom: 4px; }
  :deep(.el-dialog .el-form .el-form-item__content) { margin-left: 0 !important; }
  :deep(.el-dialog .el-form .el-select),
  :deep(.el-dialog .el-form .el-input),
  :deep(.el-dialog .el-form .el-date-editor),
  :deep(.el-dialog .el-form .el-cascader) { width: 100% !important; }
}


/* 年级选择弹窗样式 */
.grade-selection {
  padding: 20px 0;
}

.grade-tip {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
  text-align: center;
}

/* 时间偏好选择样式 */
.time-preference-container {
  width: 100%;
}

.time-preference-checkbox-group {
  width: 100%;
}

.time-preference-row {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
}

.time-preference-row:last-child {
  margin-bottom: 0;
}
</style>
