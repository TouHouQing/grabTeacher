<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed, defineAsyncComponent } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, ArrowDown } from '@element-plus/icons-vue'
import { courseAPI, subjectAPI, teacherAPI, fileAPI, publicTeachingLocationAPI, adminAPI } from '../../utils/api'
const StudentBookingCalendar = defineAsyncComponent(() => import('../../components/StudentBookingCalendar.vue'))

// 课程接口定义
interface Course {
  id: number
  teacherId: number
  teacherName: string
  subjectId: number
  subjectName: string
  title: string
  description?: string
  courseType: string
  courseTypeDisplay: string
  durationMinutes: number | null
  status: string
  statusDisplay: string
  createdAt: string
  featured: boolean
  price?: number
  teacherHourlyRate?: number | null
  startDate?: string
  endDate?: string
  personLimit?: number
  enrollmentCount?: number

  currentHours?: number | null
  lastHours?: number | null

  imageUrl?: string
  courseLocation?: string
  _localImageFile?: File | null
  _localPreviewUrl?: string
}

interface Subject {
  id: number
  name: string
  iconUrl?: string
  isActive: boolean
}

interface Teacher {
  id: number
  realName: string
  subjects?: string
  isVerified: boolean
}

// 响应式数据
const loading = ref(false)
const courses = ref<Course[]>([])
const subjects = ref<Subject[]>([])
const teachers = ref<Teacher[]>([])
const filteredTeacherOptions = ref<Teacher[]>([])
const availableSubjects = ref<Subject[]>([]) // 当前教师可教授的科目
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEditing = ref(false)
// 表单引用用于触发校验
const formRef = ref()

// 建议价格
const suggestedPrice = ref<number | null>(null)
const suggestedUnit = ref<'per_hour' | 'total' | null>(null)

// 分页数据
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
  pages: 0
})

// 搜索和筛选
const searchForm = reactive({
  keyword: '',
  subjectId: null as number | null,
  teacherId: null as number | null,
  status: '',
  courseType: '',
})

// 课程表单
const courseForm = reactive({
  id: null as number | null,
  teacherId: null as number | null,
  subjectId: null as number | null,
  title: '',
  description: '',
  courseType: 'one_on_one',
  durationMinutes: null as number | null,
  status: 'active',
  price: null as number | null,
  teacherHourlyRate: null as number | null,
  // 仅一对一：本月课时（编辑时可改，变动将记录明细）
  currentHours: null as number | null,

  startDate: '',
  endDate: '',
  personLimit: null as number | null,
  imageUrl: '' as string,
  courseLocation: '线上' as string,
  // 临时选择的本地文件与预览
  _localImageFile: null as File | null,
  courseTimeSlots: [] as Array<{ weekday: number; timeSlots: string[] }>,

  _localPreviewUrl: '' as string
})

// 授课方式/地点（小班课）
const activeLocations = ref<Array<{ id: number; name: string }>>([])
const locationMode = ref<'online' | 'offline'>('online')
const selectedOfflineLocationId = ref<number | null>(null)

const fetchActiveLocations = async () => {
  try {
    const resp = await publicTeachingLocationAPI.getActive()
    if (resp?.success) {
      activeLocations.value = resp.data || []
    }
  } catch (e) {
    console.warn('获取授课地点失败:', e)
  }
}

// 获取教师可教授的科目
const fetchTeacherSubjects = async (teacherId: number) => {
  try {
    const resp = await adminAPI.getTeacherSubjects(teacherId)
    if (resp?.success && resp.data) {
      // 根据教师科目ID过滤出可选的科目
      const teacherSubjectIds = resp.data
      availableSubjects.value = subjects.value.filter(subject =>
        teacherSubjectIds.includes(subject.id)
      )
    } else {
      availableSubjects.value = []
    }
  } catch (e) {
    console.warn('获取教师科目失败:', e)
    availableSubjects.value = []
  }
}


// 选择封面图片（仅本地预览，不立即上传）
const onSelectCover = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files && input.files[0]
  if (!file) return
  // 简单校验：限制类型与大小（<= 10MB）
  if (!/^image\//.test(file.type)) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return
  }
  courseForm._localImageFile = file
  // 生成本地预览
  const reader = new FileReader()
  reader.onload = () => {
    courseForm._localPreviewUrl = reader.result as string
  }
  reader.readAsDataURL(file)
}

// 保存时上传封面图片到 OSS，并更新 imageUrl
const uploadCoverIfNeeded = async () => {
  if (!courseForm._localImageFile) return null
  // 预签名直传
  return await fileAPI.presignAndPut(courseForm._localImageFile, 'course-cover')
}

// 表单验证规则
const formRules = {
  teacherId: [
    { required: true, message: '请选择教师', trigger: 'change' }
  ],
  subjectId: [
    { required: true, message: '请选择科目', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入课程标题', trigger: 'blur' },
    { min: 2, max: 200, message: '课程标题长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  courseType: [
    { required: true, message: '请选择课程类型', trigger: 'change' }
  ],
  durationMinutes: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        const need = courseForm.courseType === 'large_class'
        if (!need) return callback()
        if (value !== 90 && value !== 120) return callback(new Error('小班课课程时长只能选择90分钟或120分钟'))
        return callback()
      },
      trigger: 'change'
    }
  ],
  price: [
    {
      validator: (_rule: any, value: number | null | undefined, callback: (err?: Error) => void) => {
        // 所有课程类型都必须设置“每小时价格”
        if (value === null || value === undefined) {
          callback(new Error('课程价格为必填（按每小时）'))
          return
        }
        if (value <= 0) {
          callback(new Error('课程价格必须大于0'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  teacherHourlyRate: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (courseForm.courseType !== 'one_on_one' && courseForm.courseType !== 'large_class') return callback()
        if (value === null || value === undefined) {
          callback(new Error('教师时薪为必填'))
          return
        }
        if (value <= 0) {
          callback(new Error('教师时薪必须大于0'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],

  startDate: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (courseForm.courseType === 'large_class') {
          if (!value) {
            callback(new Error('小班课必须设置开始日期'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  endDate: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (courseForm.courseType === 'large_class') {
          if (!value) {
            callback(new Error('小班课必须设置结束日期'))
          } else if (courseForm.startDate && value <= courseForm.startDate) {
            callback(new Error('结束日期必须晚于开始日期'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  personLimit: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (courseForm.courseType === 'large_class') {
          if (value !== null && value !== undefined && value <= 0) {
            callback(new Error('人数限制必须大于0（不填表示不限制）'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
  courseTimeSlots: [
    {
      validator: (_rule: any, _value: any, callback: any) => {
        if (courseForm.courseType === 'large_class') {
          const slots = Array.isArray(courseForm.courseTimeSlots) ? courseForm.courseTimeSlots : []
          const hasAnySlot = slots.some(d => Array.isArray(d?.timeSlots) && d.timeSlots.length > 0)
          if (!hasAnySlot) {
            callback(new Error('请至少为某一天选择一个具体时间段'))
            return
          }
        }
        callback()
      },
      trigger: 'change'
    }
  ],

      trigger: 'blur'
    }
  ]
}

// 课程类型选项
const courseTypeOptions = [
  { label: '一对一', value: 'one_on_one' },
  { label: '小班课', value: 'large_class' }
]

// 课程状态选项
const statusOptions = [
  { label: '待审批', value: 'pending' },
  { label: '可报名', value: 'active' },
  { label: '已下架', value: 'inactive' },
  { label: '已满员', value: 'full' }
]

// 获取课程列表
const fetchCourses = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.current,
      size: pagination.size,
      ...searchForm
    }

    const response = await courseAPI.getList(params)
    if (response.success && response.data) {
      courses.value = response.data.courses || []
      pagination.total = response.data.total || 0
      pagination.current = response.data.current || 1
      pagination.size = response.data.size || 10
      pagination.pages = response.data.pages || 0
    } else {
      ElMessage.error(response.message || '获取课程列表失败')
    }
  } catch (error) {
    console.error('获取课程列表失败:', error)
    ElMessage.error('获取课程列表失败')
  } finally {
    loading.value = false
  }
}

// 获取科目列表
const fetchSubjects = async () => {
  try {
    const response = await subjectAPI.getActiveSubjects()
    if (response.success) {
      subjects.value = response.data || []
    }
  } catch (error) {
    console.error('获取科目列表失败:', error)
  }
}

// 获取教师列表
const fetchTeachers = async () => {
  try {
    const response = await teacherAPI.getList({ page: 1, size: 1000, isVerified: true })
    if (response.success && response.data) {
      teachers.value = response.data.records || []
      filteredTeacherOptions.value = [...teachers.value]
    }
  } catch (error) {
    console.error('获取教师列表失败:', error)
  }
}

// 远程模糊过滤
const filterTeacherOptions = (query: string) => {
  const q = (query || '').trim().toLowerCase()
  if (!q) { filteredTeacherOptions.value = [...teachers.value]; return }
  filteredTeacherOptions.value = teachers.value.filter(t => (t.realName || '').toLowerCase().includes(q))
}

const getSelectedTeacher = (teacherId: number | null) => teachers.value.find(t => t.id === teacherId) as any

const calcWeeksBetween = (start: string, end: string): number => {
  if (!start || !end) return 0
  const s = new Date(start + 'T00:00:00')
  const e = new Date(end + 'T00:00:00')
  if (isNaN(s.getTime()) || isNaN(e.getTime())) return 0
  if (e < s) return 0
  const diffMs = e.getTime() - s.getTime()
  return Math.floor(diffMs / (7 * 24 * 60 * 60 * 1000)) + 1
}

const calcWeeklySessions = (): number => {
  const slots = courseForm.courseTimeSlots || []
  let count = 0
  for (const day of slots) count += (day?.timeSlots?.length || 0)
  return count
}

const recomputeSuggestedPrice = () => {
  suggestedPrice.value = null
  suggestedUnit.value = null
  const t = getSelectedTeacher(courseForm.teacherId)
  const hourly = (t as any)?.hourlyRate
  if (!hourly || hourly <= 0) return

  // 按每小时计价，建议价直接取教师每小时价格；无需基于单次课时长换算
  if (courseForm.courseType === 'one_on_one') {
    // 一对一：建议为按级别每小时价格
    suggestedPrice.value = Number(hourly.toFixed(2))
    suggestedUnit.value = 'per_hour'
    return
  }
  if (courseForm.courseType === 'large_class') {
    // 小班课：建议价格也按“每小时”显示
    suggestedPrice.value = Number(hourly.toFixed(2))
    suggestedUnit.value = 'per_hour'
    return
  }
}

// —— 小班课：按日历选择每周上课时间（自动基于教师可用与既有课程做冲突检测） ——
type AdminCalendarExpose = { open: (opts?: { defaultDuration?: 90|120; dateStart?: string; dateEnd?: string; preselectSessions?: Array<{ date: string; startTime: string; endTime: string }> }) => void }
const calendarRef = ref<AdminCalendarExpose | null>(null)
const selectedSessions = ref<Array<{ date: string; startTime: string; endTime: string }>>([])
const BASE_SLOTS = ['08:00-10:00','10:00-12:00','13:00-15:00','15:00-17:00','17:00-19:00','19:00-21:00']
const toMin = (t: string) => { const [h, m] = (t||'').split(':').map(Number); return (h||0) * 60 + (m||0) }
const inferBaseSlot = (startTime: string, endTime: string): string | null => {
  for (const base of BASE_SLOTS) {
    const [bs, be] = base.split('-'); const bsMin = toMin(bs); const beMin = toMin(be)
    const s = toMin(startTime); const e = toMin(endTime)
    if (s === bsMin && e === beMin) return base // 2小时
    if ((e - s) === 90 && s === bsMin + 15 && e === beMin - 15) return base // 1.5小时
  }
  return null
}

const openCalendarPicker = () => {
  if (courseForm.courseType !== 'large_class') return
  if (!courseForm.teacherId) { ElMessage.error('请先选择教师'); return }
  const d: 90|120 = (courseForm.durationMinutes === 120 ? 120 : 90)
  calendarRef.value?.open?.({ defaultDuration: d, preselectSessions: selectedSessions.value })
}

function onCalendarConfirm(sessions: Array<{ date: string; startTime: string; endTime: string }>, duration: 90|120) {
  // 自动填充开始/结束日期（按所选会话的首尾日期）
  if (sessions && sessions.length > 0) {
    const dates = sessions.map(s => s.date).sort()
    courseForm.startDate = dates[0]
    courseForm.endDate = dates[dates.length - 1]
  }
  // 保存会话以便二次打开日历回填
  selectedSessions.value = Array.isArray(sessions) ? [...sessions] : []
  const map: Record<number, Set<string>> = {}
  for (const it of (courseForm.courseTimeSlots || [])) {
    const set = map[it.weekday] || new Set<string>()
    for (const s of (it.timeSlots || [])) set.add(s)
    map[it.weekday] = set
  }
  for (const s of sessions || []) {
    const dt = new Date(s.date + 'T00:00:00'); let w = dt.getDay(); if (w === 0) w = 7
    const base = inferBaseSlot(s.startTime, s.endTime); if (!base) continue
    const set = map[w] || new Set<string>(); set.add(base); map[w] = set
  }
  const ordered = (Object.keys(map).map(k => Number(k)).sort((a,b)=>a-b))
  const sorted = (arr: string[]) => {
    const order = new Map(BASE_SLOTS.map((s,i)=>[s,i]))
    return [...arr].sort((a,b)=> (order.get(a) || 0) - (order.get(b) || 0))
  }
  courseForm.courseTimeSlots = ordered.map(w => ({ weekday: w, timeSlots: sorted(Array.from(map[w] || [])) }))
  courseForm.durationMinutes = duration
}

const weekdayLabel = (w: number) => ['周一','周二','周三','周四','周五','周六','周日'][w-1] || `周${w}`
const getWeekdayCount = (w: number) => {
  const it = (courseForm.courseTimeSlots || []).find(d => d.weekday === w)
  return it ? (it.timeSlots?.length || 0) : 0
}




// 重置表单
const resetForm = () => {
  courseForm.id = null
  courseForm.teacherId = null
  courseForm.subjectId = null
  courseForm.title = ''
  courseForm.description = ''
  courseForm.courseType = 'one_on_one'
  courseForm.durationMinutes = null
  courseForm.status = 'active'
  courseForm.price = null
  courseForm.teacherHourlyRate = null
  courseForm.currentHours = null
  courseForm.startDate = ''
  courseForm.endDate = ''
  courseForm.courseTimeSlots = []

  courseForm.personLimit = null
  courseForm.courseLocation = '线上'
  // 重置授课方式与地点（仅小班课使用）
  locationMode.value = 'online'
  selectedOfflineLocationId.value = null
  // 清空已选会话（用于二次打开日历回填）
  selectedSessions.value = []

  // 清空可选科目
  availableSubjects.value = []
}

// 监听教师选择变化
watch(() => courseForm.teacherId, async (newTeacherId, oldTeacherId) => {
  if (newTeacherId) {
    // 获取该教师可教授的科目
    await fetchTeacherSubjects(newTeacherId)

    // 在编辑模式下，如果教师发生变化，需要检查当前科目是否还可用
    if (isEditing.value && oldTeacherId !== newTeacherId && courseForm.subjectId) {
      const isSubjectAvailable = availableSubjects.value.some(subject => subject.id === courseForm.subjectId)
      if (!isSubjectAvailable) {
        // 如果当前科目不在新教师的科目范围内，清空科目选择
        courseForm.subjectId = null
        ElMessage.warning('切换教师后，原科目不在该教师的教授范围内，已清空科目选择')
      }
    } else if (!isEditing.value) {
      // 新增模式下，清空科目选择
      courseForm.subjectId = null
    }
  } else {
    // 如果没有选择教师，清空可选科目
    availableSubjects.value = []
    courseForm.subjectId = null
  }
})

// 打开新增对话框
const openAddDialog = () => {
  resetForm()
  dialogTitle.value = '新增课程'
  isEditing.value = false
  dialogVisible.value = true
  // 打开时计算一次
  recomputeSuggestedPrice()
}
// 打开编辑对话框（确保从详情接口获取最新的每周时间周期用于回显）
const openEditDialog = async (course: Course) => {
  // 先用列表数据快速填充，提升响应速度
  courseForm.id = course.id
  courseForm.teacherId = course.teacherId
  courseForm.subjectId = course.subjectId
  courseForm.title = course.title
  courseForm.description = course.description || ''
  courseForm.courseType = course.courseType
  courseForm.durationMinutes = course.durationMinutes ?? null
  courseForm.status = course.status
  ;(courseForm as any).teacherHourlyRate = (course as any).teacherHourlyRate ?? null
  courseForm.currentHours = (course as any).currentHours ?? null

  courseForm.price = course.price || null
  courseForm.startDate = course.startDate || ''
  courseForm.endDate = course.endDate || ''
  courseForm.personLimit = course.personLimit || null
  courseForm.courseLocation = (course.courseLocation as any) || '线上'
  const listSlots = (course as unknown as { courseTimeSlots?: Array<{ weekday: number; timeSlots: string[] }> }).courseTimeSlots || []
  courseForm.courseTimeSlots = listSlots

  // 封面回显与清理本地预览
  courseForm.imageUrl = course.imageUrl || ''
  courseForm._localPreviewUrl = ''
  courseForm._localImageFile = null

  // 打开编辑时清空上一个课程残留的会话选择，防止回填到别的课程
  selectedSessions.value = []

  // 获取该教师可教授的科目
  if (courseForm.teacherId) {
    await fetchTeacherSubjects(courseForm.teacherId)
  }

  dialogTitle.value = '编辑课程'
  isEditing.value = true
  dialogVisible.value = true

  // 仅当为小班课且列表数据缺少周期信息时，再拉取详情，避免不必要请求
  const needFetchDetail = course.courseType === 'large_class' && (!listSlots || listSlots.length === 0)
  // 回显授课方式/地点（单选：线上 或 线下）
  if (courseForm.courseType === 'large_class') {
    const locStr = courseForm.courseLocation || ''
    if (locStr === '线上') {
      locationMode.value = 'online'
      selectedOfflineLocationId.value = null
    } else {
      locationMode.value = 'offline'
      const match = activeLocations.value.find(l => l.name === locStr)
      selectedOfflineLocationId.value = match ? match.id : null
    }
  } else {
    locationMode.value = 'online'
    selectedOfflineLocationId.value = null
  }
  if (!needFetchDetail) return

  // 仅当为小班课且列表数据缺少周期信息时，再拉取详情，避免不必要请求
  //  a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0
  //  a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0
  //  a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0
  //  a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0
  //  a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0 a0
  //  a0 a0 a0
  //  a0 a0 a0 a0
  //  a0 a0
  //  a0
  //  a0
  //  a0
  //  a0
  //  a0
  //  a0
  //  a0

  try {
    const resp = await courseAPI.getById(course.id)
    if (resp?.success && resp.data) {
      const detail = resp.data as any
      if (detail.courseType) courseForm.courseType = detail.courseType
      if (detail.teacherHourlyRate !== undefined) (courseForm as any).teacherHourlyRate = detail.teacherHourlyRate ?? null

      if (detail.durationMinutes) courseForm.durationMinutes = detail.durationMinutes
      if (detail.status) courseForm.status = detail.status
      if (detail.price !== undefined) courseForm.price = detail.price ?? null
      if (detail.startDate) courseForm.startDate = detail.startDate || ''
      if (detail.endDate) courseForm.endDate = detail.endDate || ''
      if (detail.personLimit !== undefined) courseForm.personLimit = detail.personLimit ?? null
      if (detail.courseLocation) courseForm.courseLocation = detail.courseLocation
      // 关键：使用详情返回的每周上课时间进行回显
      courseForm.courseTimeSlots = (detail.courseTimeSlots as Array<{ weekday: number; timeSlots: string[] }>) || []
      // 回显当前本月课时（仅一对一课程有意义）
      if (detail.currentHours !== undefined) courseForm.currentHours = detail.currentHours ?? courseForm.currentHours
    }
  } catch (e) {
    console.warn('获取课程详情失败，使用列表数据回显:', e)
  }
}

// 保存课程
const saveCourse = async () => {
  try {
    loading.value = true

    // 触发表单基础校验
    if (formRef.value && typeof formRef.value.validate === 'function') {
      const valid = await formRef.value.validate().catch(() => false)
      if (!valid) {
        loading.value = false
        return
      }
    }

    // 业务前置校验：小班课需通过日历至少选择一次上课时间
    if (courseForm.courseType === 'large_class') {
      if (!courseForm.courseTimeSlots || courseForm.courseTimeSlots.length === 0) {
        ElMessage.error('小班课必须通过日历选择至少一次上课时间')
        loading.value = false
        return
      }

    }

    // 若选择了新封面，先上传获取 URL
    let coverUrl: string | null = null
    try {
      coverUrl = await uploadCoverIfNeeded()
    } catch (e) {
      console.error('封面上传失败', e)
      ElMessage.error('封面上传失败，请重试')
      loading.value = false
      return
    }

    const courseData: any = {
      teacherId: courseForm.teacherId!,
      subjectId: courseForm.subjectId!,
      title: courseForm.title,
      description: courseForm.description,
      courseType: courseForm.courseType,
      // durationMinutes: handled in type-specific block
      status: isEditing.value ? courseForm.status : 'active',
      price: courseForm.price, // 所有课程类型都可以设置价格
      teacherHourlyRate: courseForm.teacherHourlyRate,
      ...(coverUrl ? { imageUrl: coverUrl } : {})
    }

    if (courseForm.courseType === 'large_class') {
      courseData.durationMinutes = courseForm.durationMinutes

      courseData.startDate = courseForm.startDate
      courseData.endDate = courseForm.endDate
      courseData.personLimit = courseForm.personLimit
      courseData.courseTimeSlots = courseForm.courseTimeSlots

      // 授课方式：单选（线上 或 线下）
      if (locationMode.value === 'online') {
        (courseData as any).supportsOnline = true
        courseData.courseLocation = '线上'
      } else {
        if (!selectedOfflineLocationId.value) {
          ElMessage.error('请选择一个线下授课地点')
          loading.value = false
          return
        }
        (courseData as any).offlineLocationId = selectedOfflineLocationId.value
        const loc = activeLocations.value.find(l => l.id === selectedOfflineLocationId.value!)
        courseData.courseLocation = loc ? loc.name : '线下'
      }
    }

    let response: any
    if (isEditing.value && courseForm.id) {
      response = await courseAPI.update(courseForm.id, courseData)
    } else {
      response = await courseAPI.create(courseData)
    }

    if (response.success) {
      // 若编辑一对一课程且填写了本月课时，调用管理员接口进行差值入账并记录明细
      if (isEditing.value && courseForm.id && courseForm.teacherId && courseForm.courseType === 'one_on_one' && courseForm.currentHours !== null && courseForm.currentHours !== undefined) {
        try {
          const items = [{ courseId: courseForm.id as number, currentHours: Number(courseForm.currentHours) }]
          const res2: any = await teacherAPI.adminUpdateOneOnOneMetrics(courseForm.teacherId as number, items)
          if (!res2?.success) {
            ElMessage.error(res2?.message || '更新本月课时失败')
            loading.value = false
            return
          }
          ElMessage.success('本月课时已更新并记录明细')
        } catch (e) {
          console.error('更新本月课时失败:', e)
          ElMessage.error('更新本月课时失败')
          loading.value = false
          return
        }
      }
      ElMessage.success(isEditing.value ? '课程更新成功' : '课程创建成功')
      // 回显最新封面
      if (response.data?.imageUrl) {
        courseForm.imageUrl = response.data.imageUrl
        courseForm._localPreviewUrl = ''
        courseForm._localImageFile = null
      }
      dialogVisible.value = false
      await fetchCourses()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    console.error('保存课程失败:', error)
    const msg = (error as any)?.response?.message || (error as any)?.message || '保存课程失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

// 删除课程
const deleteCourse = async (course: Course) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除课程"${course.title}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    loading.value = true
    const response = await courseAPI.delete(course.id)

    if (response.success) {
      ElMessage.success('课程删除成功')
      await fetchCourses()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除课程失败:', error)
      ElMessage.error('删除课程失败')
    }
  } finally {
    loading.value = false
  }
}

// 更新课程状态
const updateCourseStatus = async (course: Course, newStatus: string) => {
  try {
    loading.value = true
    const response = await courseAPI.updateStatus(course.id, newStatus)

    if (response.success) {
      ElMessage.success('状态更新成功')
      await fetchCourses()
    } else {
      ElMessage.error(response.message || '状态更新失败')
    }
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('状态更新失败')
  } finally {
    loading.value = false
  }
}

// 切换精选状态
const onToggleFeatured = async (course: Course, val: any) => {
  const next = !!val
  const prev = !!course.featured
  course.featured = next
  try {
    const response = await courseAPI.setCourseAsFeatured(course.id, next)
    if (response.success) {
      ElMessage.success('已更新精选状态')
    } else {
      course.featured = prev
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error: any) {
    course.featured = prev
    ElMessage.error(error?.message || '操作失败')
  }
}

// 批准课程
const approveCourse = async (course: Course) => {
  try {
    await ElMessageBox.confirm(
      `确定要批准课程"${course.title}"吗？批准后课程将变为可报名状态。`,
      '确认批准',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success',
      }
    )

    await updateCourseStatus(course, 'active')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批准课程失败:', error)
    }
  }
}

// 拒绝课程
const rejectCourse = async (course: Course) => {
  try {
    await ElMessageBox.confirm(
      `确定要拒绝课程"${course.title}"吗？拒绝后课程将变为已下架状态。`,
      '确认拒绝',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    await updateCourseStatus(course, 'inactive')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拒绝课程失败:', error)
    }
  }
}

// 搜索课程
const searchCourses = () => {
  pagination.current = 1
  fetchCourses()
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.subjectId = null
  searchForm.teacherId = null
  searchForm.status = ''
  searchForm.courseType = ''
  pagination.current = 1
  fetchCourses()
}

// 分页变化
const handlePageChange = (page: number) => {
  pagination.current = page
  fetchCourses()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.current = 1
  fetchCourses()
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  switch (status) {
    case 'active': return 'success'
    case 'inactive': return 'info'
    case 'full': return 'warning'
    case 'pending': return 'warning'
    default: return 'info'
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchCourses()
  fetchSubjects()
  fetchTeachers()
  fetchActiveLocations()
})

// 相关字段变化时重算建议价格
watch(() => [courseForm.teacherId, courseForm.courseType, courseForm.durationMinutes, courseForm.startDate, courseForm.endDate, courseForm.personLimit, JSON.stringify(courseForm.courseTimeSlots)], () => {
  recomputeSuggestedPrice()
})
</script>

<template>
  <div class="admin-course-management">
    <div class="header">
      <h2>课程管理</h2>
    </div>

    <!-- 搜索筛选区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索课程标题或科目"
            :prefix-icon="Search"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="教师">
          <el-select
            v-model="searchForm.teacherId"
            placeholder="选择教师"
            clearable
            filterable
            remote
            :remote-method="filterTeacherOptions"
            :loading="loading"
            style="width: 220px"
          >
            <el-option
              v-for="teacher in filteredTeacherOptions"
              :key="teacher.id"
              :label="teacher.realName"
              :value="teacher.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科目">
          <el-select v-model="searchForm.subjectId" placeholder="选择科目" clearable style="width: 200px">
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.courseType" placeholder="选择类型" clearable style="width: 120px">
            <el-option
              v-for="option in courseTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="searchCourses">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
        <el-form-item class="add-button-item">
          <el-button type="primary" :icon="Plus" @click="openAddDialog">
            新增课程
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 课程表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="courses"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="title" label="课程标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="授课教师" width="100" />
        <el-table-column label="教师时薪" width="180">
          <template #default="{ row }">
            <template v-if="row.courseType === 'one_on_one'">
              <span v-if="row.teacherHourlyRate !== null && row.teacherHourlyRate !== undefined">{{ row.teacherHourlyRate }}M豆/时</span>
              <span v-else>按老师默认</span>
            </template>
            <template v-else-if="row.courseType === 'large_class'">
              <span v-if="row.teacherHourlyRate !== null && row.teacherHourlyRate !== undefined">1人：{{ row.teacherHourlyRate }}M豆/时（+5/人）</span>
              <span v-else class="text-muted">未设置</span>
            </template>
            <template v-else>
              <span class="text-muted">-</span>
            </template>
          </template>
        </el-table-column>

        <el-table-column prop="subjectName" label="科目" width="80" />
        <el-table-column prop="courseTypeDisplay" label="类型" width="80" />
        <el-table-column prop="durationMinutes" label="时长" width="90">
          <template #default="{ row }">
            {{ row.durationMinutes ? (row.durationMinutes + '分钟') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="价格" width="120">
          <template #default="{ row }">
            <span v-if="row.price">
              <template v-if="row.courseType === 'one_on_one'">
                {{ row.price }}M豆/时
              </template>
              <template v-else>
                {{ row.price }}M豆/时
              </template>
            </span>
            <span v-else class="text-muted">价格面议</span>
          </template>
        </el-table-column>
        <el-table-column label="开课时间" width="100">
          <template #default="{ row }">
            <span v-if="row.courseType === 'large_class'">
              {{ row.startDate || '未设置' }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="结课时间" width="100">
          <template #default="{ row }">
            <span v-if="row.courseType === 'large_class'">
              {{ row.endDate || '未设置' }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="报名人数" width="80">
          <template #default="{ row }">
            <span v-if="row.courseType === 'large_class'">
              {{ typeof row.enrollmentCount === 'number' ? row.enrollmentCount + '人' : '-' }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column label="人数限制" width="80">
          <template #default="{ row }">
            <span v-if="row.courseType === 'large_class'">
              {{ row.personLimit ? `${row.personLimit}人` : '不限制' }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ row.statusDisplay }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="精选" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.featured" @change="val => onToggleFeatured(row, val)" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="120">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleDateString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right" align="center">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button size="small" :icon="Edit" @click="openEditDialog(row)">
                编辑
              </el-button>

              <!-- 待审批状态显示审批按钮 -->
              <template v-if="row.status === 'pending'">
                <el-button size="small" type="success" @click="approveCourse(row)">
                  批准
                </el-button>
                <el-button size="small" type="warning" @click="rejectCourse(row)">
                  拒绝
                </el-button>
              </template>

              <!-- 其他状态显示状态切换下拉菜单 -->
              <template v-else>
                <el-dropdown @command="(status) => updateCourseStatus(row, status)">
                  <el-button size="small" type="primary">
                    状态<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="active" :disabled="row.status === 'active'">
                        设为可报名
                      </el-dropdown-item>
                      <el-dropdown-item command="inactive" :disabled="row.status === 'inactive'">
                        设为已下架
                      </el-dropdown-item>
                      <el-dropdown-item command="full" :disabled="row.status === 'full'">
                        设为已满员
                      </el-dropdown-item>
                      <el-dropdown-item command="pending" :disabled="row.status === 'pending'">
                        设为待审批
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>

              <el-button size="small" type="danger" :icon="Delete" @click="deleteCourse(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          :current-page="pagination.current"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑课程对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="courseForm"
        :rules="formRules"
        ref="formRef"
        label-width="100px"
      >
        <el-form-item label="授课教师" prop="teacherId">
          <el-select v-model="courseForm.teacherId" placeholder="请选择教师" style="width: 100%">
            <el-option
              v-for="teacher in teachers"
              :key="teacher.id"
              :label="teacher.realName"
              :value="teacher.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="科目" prop="subjectId">
          <el-select
            v-model="courseForm.subjectId"
            :placeholder="courseForm.teacherId ? '请选择科目' : '请先选择教师'"
            :disabled="!courseForm.teacherId"
            style="width: 100%"
          >
            <el-option
              v-for="subject in availableSubjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>


        <el-form-item label="课程标题" prop="title">
          <el-input
            v-model="courseForm.title"
            placeholder="请输入课程标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="课程描述">
          <el-input
            v-model="courseForm.description"
            type="textarea"
            placeholder="请输入课程描述"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 课程封面（本地预览，保存时上传） -->
        <el-form-item label="课程封面">
          <div class="cover-upload">
            <div class="cover-preview" v-if="courseForm._localPreviewUrl || courseForm.imageUrl">
              <img :src="courseForm._localPreviewUrl || courseForm.imageUrl" alt="课程封面预览" />
            </div>
            <div class="cover-actions">
              <input type="file" accept="image/*" @change="onSelectCover" />
              <div class="tip">支持 jpg/png，最大10MB。</div>
            </div>
          </div>
        </el-form-item>


        <el-form-item label="课程类型" prop="courseType">
          <el-radio-group v-model="courseForm.courseType">
            <el-radio
              v-for="option in courseTypeOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="courseForm.courseType==='large_class'" label="课程时长" prop="durationMinutes">
          <el-select
            v-model="courseForm.durationMinutes"
            placeholder="请选择课程时长"
            style="width: 200px"
          >
            <el-option
              :value="90"
              label="90分钟（一个半小时）"
            />
            <el-option
              :value="120"
              label="120分钟（俩小时）"
            />
          </el-select>
        </el-form-item>

        <!-- 小班课专用字段 -->
        <template v-if="courseForm.courseType === 'large_class'">
          <el-divider content-position="left">
            <span style="color: #409eff; font-weight: 500;">小班课设置</span>
          </el-divider>


          <el-form-item label="人数限制" prop="personLimit">
            <el-input-number
              v-model="courseForm.personLimit"
              :min="1"
              :max="1000"
              :step="1"
              style="width: 200px"
              placeholder="不填表示不限制"
              clearable
            />
            <span style="margin-left: 10px; color: #909399;">人（不填表示不限制人数）</span>
          </el-form-item>

          <el-form-item label="上课时间" prop="courseTimeSlots" required v-if="courseForm.courseType === 'large_class'">
            <div class="weekly-picker" style="display:flex; flex-direction:column; gap:8px;">
              <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
                <el-button size="small" type="primary" plain @click="openCalendarPicker">
                  按日历选择（自动避开冲突）
                </el-button>
                <span style="color:#909399;">仅可选择教师可用且不与已排课程冲突的时间</span>
              </div>
            </div>
            <StudentBookingCalendar
              ref="calendarRef"
              :teacher-id="courseForm.teacherId || 0"
              :multi-select="true"
              @confirm="onCalendarConfirm"
            />
          </el-form-item>
        </template>

        <!-- 新增课程不允许选择状态；仅编辑时显示可选状态 -->
        <el-form-item label="课程状态" v-if="isEditing">
          <el-radio-group v-model="courseForm.status">
            <el-radio
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="课程状态" v-else>
          <el-tag type="success">默认：可报名</el-tag>
        </el-form-item>

        <el-form-item label="课程价格" prop="price">
          <el-input-number
            v-model="courseForm.price"
            :min="0"
            :precision="2"
            :step="10"
            style="width: 200px"
            placeholder="请输入每小时价格"
            clearable
          />


          <span style="margin-left: 10px; color: #909399;">
            M豆/小时（必填）
          </span>
          <template v-if="suggestedPrice !== null">
            <el-divider direction="vertical" />
            <span style="color:#67C23A;">
              建议：
              <template v-if="suggestedUnit === 'per_hour'">
                {{ suggestedPrice }} M豆/小时
              </template>
              <template v-else-if="suggestedUnit === 'total'">
                {{ suggestedPrice }} M豆（总价）
              </template>
            </span>
          </template>
        </el-form-item>

        <el-form-item v-if="courseForm.courseType === 'one_on_one'" label="教师时薪" prop="teacherHourlyRate" required>
          <el-input-number
            v-model="courseForm.teacherHourlyRate"
            :min="0"
            :precision="2"
            :step="5"
            style="width: 200px"
            placeholder="请输入老师时薪"
            clearable
          />
          <span style="margin-left: 10px; color: #909399;">
            M豆/小时（必填，仅用于计算教师收入，可与学生价不一致）
          </span>
        </el-form-item>
        <el-form-item v-if="courseForm.courseType === 'large_class'" label="教师时薪" prop="teacherHourlyRate" required>
          <el-input-number
            v-model="courseForm.teacherHourlyRate"
            :min="0"
            :precision="2"
            :step="5"
            style="width: 200px"
            placeholder="填1人报名时的时薪"
            clearable
          />
          <span style="margin-left: 10px; color: #909399;">
            M豆/小时（1人报名时的老师时薪；每+1人，时薪+5）
          </span>
        </el-form-item>

        <!-- 一对一：编辑时可直接调整本月课时（变动会记录到课时变动明细） -->
        <el-form-item v-if="isEditing && courseForm.courseType === 'one_on_one'" label="本月课时">
          <el-input-number
            v-model="courseForm.currentHours"
            :min="0"
            :precision="2"
            :step="0.5"
            style="width: 200px"
            placeholder="填写新的本月课时"
            clearable
          />
          <span style="margin-left: 10px; color: #909399;">小时（按差值自动入账，并写入课时变动明细表）</span>
        </el-form-item>



        <!-- 小班课：授课方式与地点选择（单选：线上 或 线下） -->
        <template v-if="courseForm.courseType === 'large_class'">
          <el-form-item label="授课方式" required>
            <el-radio-group v-model="locationMode">
              <el-radio label="online">线上</el-radio>
              <el-radio label="offline">线下</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="locationMode === 'offline'" label="线下地点" required>
            <el-select v-model="selectedOfflineLocationId" placeholder="请选择线下授课地点" style="width: 100%">
              <el-option v-for="loc in activeLocations" :key="loc.id" :label="loc.name" :value="loc.id" />
            </el-select>
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveCourse" :loading="loading">
            {{ isEditing ? '更新' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-course-management {
  padding: 20px;
  padding-top: 5px; /* 增加顶部间距，避免被导航栏遮挡 */
}

.header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  min-height: 400px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 统计卡片样式 */
.statistics-section {
  margin-bottom: 20px;
}

.statistics-cards {
  margin-bottom: 0;
}

.statistic-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.statistic-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.statistic-card.active {
  border-left: 4px solid #67c23a;
}

.statistic-card.inactive {
  border-left: 4px solid #909399;
}

.statistic-card.full {
  border-left: 4px solid #e6a23c;
}

.statistic-content {
  text-align: center;
  padding: 10px;
}

.statistic-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.statistic-label {
  font-size: 14px;
  color: #606266;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.add-button-item {
  margin-left: auto;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .admin-course-management {
    padding: 10px;
  }

  .header h2 {
    font-size: 18px;
  }

  .statistic-number {
    font-size: 24px;
  }

  .statistic-label {
    font-size: 12px;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-form .el-form-item {
    margin-bottom: 12px;
    width: 100%;
  }

  .search-form .el-form-item .el-input,
  .search-form .el-form-item .el-select {
    width: 100% !important;
  }


  /* 课程封面上传样式 */
  .cover-upload { display: flex; align-items: center; gap: 12px; }
  .cover-preview img { width: 120px; height: 80px; object-fit: cover; border-radius: 6px; border: 1px solid #ebeef5; }
  .cover-actions .tip { color: #909399; font-size: 12px; margin-top: 6px; }

  .add-button-item {
    margin-left: 0;
    width: 100%;
  }

  .add-button-item .el-button {
    width: 100%;
  }

  .table-card {
    overflow-x: auto;
  }

  .el-table {
    min-width: 1200px;
  }

  .pagination-wrapper {
    padding: 10px;
  }

  .pagination-wrapper .el-pagination {
    justify-content: center;
  }

  /* 表单项标签置顶 + 控件全宽（仅本页的对话框内） */
  :deep(.el-dialog .el-form .el-form-item__label) { float: none; display: block; padding-bottom: 4px; }
  :deep(.el-dialog .el-form .el-form-item__content) { margin-left: 0 !important; }
  :deep(.el-dialog .el-form .el-select),
  :deep(.el-dialog .el-form .el-input),
  :deep(.el-dialog .el-form .el-date-editor),
  :deep(.el-dialog .el-form .el-cascader) { width: 100% !important; }

  /* 新增/编辑课程对话框小屏铺满 */
  :deep(.el-dialog) { width: 100vw !important; max-width: 100vw !important; margin: 0 !important; }
  :deep(.el-dialog__body) { padding: 12px; }
}

@media (max-width: 480px) {
  .statistic-number {
    font-size: 20px;
  }

  .search-form .el-form-item .el-form-item__label {
    width: 60px !important;
    font-size: 14px;
  }

  .el-table .el-button {
    padding: 4px 8px;
    font-size: 12px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  width: 100%;
  padding: 0 8px;
}

.operation-buttons .el-button {
  margin: 0;
  min-width: 60px;
  text-align: center;
}

/* 确保操作列标题居中 */
:deep(.el-table__header-wrapper .el-table__header th:last-child .cell) {
  text-align: center;
}

.text-muted {
  color: #c0c4cc;
  font-style: italic;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .operation-buttons {
    flex-wrap: wrap;
    gap: 4px;
    justify-content: center;
  }

  .operation-buttons .el-button {
    font-size: 12px;
    padding: 4px 8px;
    min-width: 50px;
  }
}
</style>
