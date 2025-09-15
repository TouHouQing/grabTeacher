<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Phone, Message, Location, School, Trophy, ArrowLeft, Loading, Star, Clock, Document, InfoFilled } from '@element-plus/icons-vue'
import { teacherAPI, evaluationAPI, bookingAPI, publicTeachingLocationAPI, publicGradeAPI } from '@/utils/api'
import StudentBookingCalendar from '@/components/StudentBookingCalendar.vue'
import StudentTrialCalendar from '@/components/StudentTrialCalendar.vue'
import { useUserStore } from '@/stores/user'
import teacherBoy1 from '@/assets/pictures/teacherBoy1.jpeg'
import teacherBoy2 from '@/assets/pictures/teacherBoy2.jpeg'
import teacherBoy3 from '@/assets/pictures/teacherBoy3.jpeg'
import teacherGirl1 from '@/assets/pictures/teacherGirl1.jpeg'
import teacherGirl2 from '@/assets/pictures/teacherGirl2.jpeg'
import teacherGirl3 from '@/assets/pictures/teacherGirl3.jpeg'
import teacherGirl4 from '@/assets/pictures/teacherGirl4.jpeg'
import studentGirl2 from '@/assets/pictures/studentGirl2.jpeg'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 获取路由传递的教师ID并转换为数字
const teacherId = parseInt(route.params.id as string)

// 教师信息状态
const teacher = ref(null)
const loading = ref(true)


// 教师可授课时间（精简展示相关）
const teacherAvailableSlots = ref<any[]>([])
const showAllAvailability = ref(false)
const weekdayNames = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const flattenedAvailability = computed(() => {
  // 将同一天的时间段合并为“一天一行”，形如：周一：08:00-10:00、10:00-12:00
  try {
    const map = new Map<number, Set<string>>()
    for (const slot of teacherAvailableSlots.value || []) {
      const w = Number(slot?.weekday) || 0
      if (!map.has(w)) map.set(w, new Set<string>())
      const set = map.get(w)!
      const times: string[] = Array.isArray(slot?.timeSlots) ? slot.timeSlots : []
      for (const t of times) set.add(t)
    }
    const res: string[] = []
    for (let w = 1; w <= 7; w++) {
      const set = map.get(w)
      if (set && set.size > 0) {
        const times = Array.from(set).sort((a, b) => a.localeCompare(b))
        res.push(`${weekdayNames[w]}：${times.join('、')}`)
      }
    }
    return res
  } catch (e) {
    return []
  }
})
const visibleAvailability = computed(() => {
  const all = flattenedAvailability.value
  return showAllAvailability.value ? all : all.slice(0, 12)
})
const totalAvailabilityCount = computed(() => flattenedAvailability.value.length)

// 学生评价状态
const reviews = ref([])
const reviewsLoading = ref(false)
const reviewsPage = ref(1)
const reviewsSize = ref(10)
const reviewsTotal = ref(0)

// 学生评价自动滚动（无穷循环）相关
const reviewsScrollRef = ref<HTMLElement | null>(null)
const isReviewsHovering = ref(false)
const scrollSpeed = ref(0.6) // px/frame
let reviewsRafId: number | null = null

const loopedReviews = computed(() => {
  const arr = reviews.value as any[]
  return arr && arr.length > 0 ? [...arr, ...arr] : []
})

const shouldAutoScroll = () => {
  const el = reviewsScrollRef.value
  if (!el) return false
  return el.scrollHeight > el.clientHeight + 10
}

const reviewsStep = () => {
  const el = reviewsScrollRef.value
  if (!el) {
    reviewsRafId = null
    return
  }
  if (!document.hidden && !isReviewsHovering.value) {
    const half = el.scrollHeight / 2
    el.scrollTop += scrollSpeed.value
    if (el.scrollTop >= half) {
      el.scrollTop = el.scrollTop - half
    }
  }
  reviewsRafId = window.requestAnimationFrame(reviewsStep)
}

const startReviewsAutoScroll = () => {
  if (reviewsRafId != null) return
  if (!shouldAutoScroll()) return
  reviewsRafId = window.requestAnimationFrame(reviewsStep)
}

const stopReviewsAutoScroll = () => {
  if (reviewsRafId != null) {
    window.cancelAnimationFrame(reviewsRafId)
    reviewsRafId = null
  }
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    stopReviewsAutoScroll()
  } else {
    startReviewsAutoScroll()
  }
}

watch(reviews, async () => {
  await nextTick()
  const el = reviewsScrollRef.value
  if (el) el.scrollTop = 0
  stopReviewsAutoScroll()
  startReviewsAutoScroll()
})

watch(isReviewsHovering, (hover) => {
  if (hover) {
    stopReviewsAutoScroll()
  } else {
    startReviewsAutoScroll()
  }
})

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  stopReviewsAutoScroll()
})
// 预约相关状态
const showBookingModal = ref(false)
const bookingType = ref<'trial' | 'formal' | ''>('') // 试听课或正式课
const trialDlg = ref<InstanceType<typeof StudentTrialCalendar> | null>(null)
const subjects = ref([]) // 科目列表
const selectedSubject = ref('')
const selectedDate = ref('')
const selectedTimeSlot = ref('')
const availableTimeSlots = ref([]) // 可用时间段
const loadingSubjects = ref(false)
// const loadingTimeSlots = ref(false) // 暂时注释，未使用


// 授课地点选择
const activeTeachingLocations = ref<any[]>([])
const allowedTeachingLocations = ref<Array<{ value: string | number; label: string }>>([])
const selectedTeachingLocation = ref<string | number>('')
// 年级选择
const grades = ref<Array<{ id: string | number; name: string }>>([])
const selectedGrade = ref<string | number>('')



// 试听课时间段选项（选择大时间段后，具体时间按半小时切分）
const timePeriods = [
  { value: 'morning', label: '上午（08:00-12:00）', range: ['08:00', '12:00'] },
  { value: 'afternoon', label: '下午（13:00-17:00）', range: ['13:00', '17:00'] },
  { value: 'evening', label: '晚上（17:00-21:00）', range: ['17:00', '21:00'] }
]
const selectedTimePeriod = ref('')

// 课程安排相关状态 - 从TeacherMatch.vue复制
const showCourseScheduleModal = ref(false)
const calendarDlg = ref<InstanceType<typeof StudentBookingCalendar> | null>(null)
const teacherCourses = ref([]) // 教师可选课程列表
const selectedCourse = ref(null) // 选中的课程
const loadingCourses = ref(false)
const scheduleForm = ref({
  bookingType: 'calendar', // 预约类型：calendar(按日历选择正式课/试听课)
  selectedCourse: null,
  selectedDurationMinutes: 90, // 课程时长（分钟）
  startDate: '',
  endDate: '',
  sessionCount: 10,
  trialDate: '',
  trialStartTime: '',
  trialEndTime: ''
})
const availableTimeSlotsForCourse = ref([]) // 课程可用时间段
const loadingCourseTimeSlots = ref(false)

// 时间匹配度相关状态（暂时注释，未使用）
// const accurateMatchScoreInfo = ref(null)
// const matchScoreLoading = ref(false)
// const showMatchDetails = ref(false)
// const showConflictDetails = ref(false)

// 默认教师数据库（作为后备数据）
const teachersData = {
  1: {
    id: 1,
    name: '张老师',
    subject: '数学',
    grade: '初中',
    experience: 10,
    rating: 4.8,
    description: '数学教育专家，专注于中小学数学教学，善于激发学生学习兴趣，使用多种教学方法帮助学生理解数学概念。',
    detailedDescription: `张老师拥有北京师范大学数学教育专业硕士学位，有10年丰富的教学经验。

教学特点：
1. 善于调动学生学习热情，课堂氛围活跃
2. 针对不同学生特点，采用个性化教学方法
3. 注重培养学生的数学思维和解题能力
4. 教学经验丰富，对升学考试有深入研究

教学成果：
- 指导多名学生在数学竞赛中获奖
- 培养学生考入名校比例高达85%
- 平均提分率达30%以上`,
    avatar: teacherBoy1,
    tags: ['趣味教学', '重点突破', '思维导图'],
    schedule: ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00'],
    gender: 'Male',
    teachingStyle: '幽默风趣',
    education: '北京师范大学数学教育专业硕士',
    location: '新加坡中部',
    contactPhone: '+65 8888 8888',
    contactEmail: 'zhang.teacher@grabteacher.com',
    achievements: [
      '2020年新加坡优秀教师奖',
      '2018年数学教学创新奖',
      '2016年学生最喜爱教师奖'
    ]
  },
  2: {
    id: 2,
    name: '李老师',
    subject: '英语',
    grade: '初中',
    experience: 8,
    rating: 4.9,
    description: '毕业于英国剑桥大学，拥有TESOL证书，擅长英语口语教学，注重学生的语言应用能力培养。',
    detailedDescription: `李老师毕业于英国剑桥大学，拥有TESOL证书，有8年丰富的教学经验。

教学特点：
1. 纯正英式发音，注重语音语调训练
2. 互动式教学，提高学生口语表达能力
3. 结合国际教材，培养学生国际视野
4. 个性化指导，针对学生薄弱环节重点突破

教学成果：
- 培养学生雅思托福高分率达90%
- 指导学生参加国际英语竞赛获奖
- 学生口语表达能力显著提升`,
    avatar: teacherBoy2,
    tags: ['发音纠正', '口语强化', '语法精通'],
    schedule: ['周二 18:00-20:00', '周四 18:00-20:00', '周日 14:00-16:00'],
    gender: 'Male',
    teachingStyle: '互动式教学',
    education: '英国剑桥大学英语教育专业硕士',
    location: '新加坡东部',
    contactPhone: '+65 8888 8889',
    contactEmail: 'li.teacher@grabteacher.com',
    achievements: [
      '2021年国际英语教学奖',
      '2019年TESOL优秀教师奖',
      '2017年学生口语提升突出贡献奖'
    ]
  },
  3: {
    id: 3,
    name: '王老师',
    subject: '物理',
    grade: '初中',
    experience: 12,
    rating: 4.7,
    description: '物理学博士，有丰富的教学经验，能将复杂概念简单化，善于通过实验和演示帮助学生理解物理原理。',
    detailedDescription: `王老师是物理学博士，有12年丰富的教学经验。

教学特点：
1. 理论与实践相结合，注重实验教学
2. 将复杂物理概念简单化，易于理解
3. 启发式教学，培养学生科学思维
4. 针对考试重点，提高解题技巧

教学成果：
- 指导学生参加物理竞赛获得优异成绩
- 学生物理成绩平均提升40%
- 培养多名学生考入理工类名校`,
    avatar: teacherBoy3,
    tags: ['概念解析', '解题技巧', '高考冲刺'],
    schedule: ['周一 16:00-18:00', '周三 16:00-18:00', '周六 14:00-16:00'],
    gender: 'Male',
    teachingStyle: '实验教学',
    education: '清华大学物理学博士',
    location: '新加坡西部',
    contactPhone: '+65 8888 8890',
    contactEmail: 'wang.teacher@grabteacher.com',
    achievements: [
      '2020年物理教学创新奖',
      '2018年优秀博士教师奖',
      '2016年学生竞赛指导奖'
    ]
  },
  4: {
    id: 4,
    name: '刘老师',
    subject: '化学',
    grade: '初中',
    experience: 15,
    rating: 4.9,
    description: '化学教育硕士，从事一线教学工作15年，教学方法灵活多样，注重培养学生的实验能力和科学思维。',
    detailedDescription: `刘老师是化学教育硕士，从事一线教学工作15年。

教学特点：
1. 实验教学丰富，激发学生学习兴趣
2. 系统性强，注重知识点之间的联系
3. 因材施教，根据学生特点调整教学方法
4. 注重培养学生的科学素养和实验技能

教学成果：
- 指导学生化学实验竞赛多次获奖
- 学生化学成绩提升率达95%
- 培养学生养成良好的科学思维习惯`,
    avatar: teacherGirl1,
    tags: ['实验教学', '概念讲解', '解题方法'],
    schedule: ['周二 16:00-18:00', '周五 18:00-20:00', '周日 10:00-12:00'],
    gender: 'Female',
    teachingStyle: '实验导向',
    education: '华东师范大学化学教育硕士',
    location: '新加坡南部',
    contactPhone: '+65 8888 8891',
    contactEmail: 'liu.teacher@grabteacher.com',
    achievements: [
      '2021年化学教学优秀奖',
      '2019年实验教学创新奖',
      '2017年学生最喜爱女教师奖'
    ]
  },
  5: {
    id: 5,
    name: '陈老师',
    subject: '数学',
    grade: '初中',
    experience: 7,
    rating: 4.6,
    description: '数学教育专业毕业，擅长启发式教学，能够根据学生的特点制定个性化的学习计划。',
    detailedDescription: `陈老师数学教育专业毕业，有7年教学经验。

教学特点：
1. 启发式教学，培养学生独立思考能力
2. 个性化教学计划，因材施教
3. 注重基础夯实，循序渐进
4. 耐心细致，善于鼓励学生

教学成果：
- 帮助基础薄弱学生显著提升成绩
- 培养学生数学学习兴趣和自信心
- 学生数学思维能力明显改善`,
    avatar: teacherGirl2,
    tags: ['基础夯实', '思维训练', '难题攻克'],
    schedule: ['周一 15:00-17:00', '周四 16:00-18:00', '周六 16:00-18:00'],
    gender: 'Female',
    teachingStyle: '启发式教学',
    education: '北京师范大学数学教育专业学士',
    location: '新加坡北部',
    contactPhone: '+65 8888 8892',
    contactEmail: 'chen.teacher@grabteacher.com',
    achievements: [
      '2020年青年教师优秀奖',
      '2018年学生进步指导奖',
      '2016年数学教学新秀奖'
    ]
  },
  6: {
    id: 6,
    name: '赵老师',
    subject: '生物',
    grade: '初中',
    experience: 9,
    rating: 4.8,
    description: '生物学硕士，有丰富的教学经验，擅长将生物学知识与日常生活相结合，让学习更加生动有趣。',
    detailedDescription: `赵老师是生物学硕士，有9年丰富的教学经验。

教学特点：
1. 生动有趣的教学方式，寓教于乐
2. 理论联系实际，与生活紧密结合
3. 注重培养学生的观察和思考能力
4. 善用多媒体教学，直观展示生物现象

教学成果：
- 学生生物科学素养显著提升
- 指导学生参加生物竞赛获得佳绩
- 激发学生对生命科学的浓厚兴趣`,
    avatar: teacherGirl3,
    tags: ['实验演示', '概念讲解', '考点梳理'],
    schedule: ['周二 19:00-21:00', '周五 16:00-18:00', '周日 16:00-18:00'],
    gender: 'Female',
    teachingStyle: '生动有趣',
    education: '复旦大学生物学硕士',
    location: '新加坡中部',
    contactPhone: '+65 8888 8893',
    contactEmail: 'zhao.teacher@grabteacher.com',
    achievements: [
      '2021年生物教学创新奖',
      '2019年科普教育贡献奖',
      '2017年学生最受欢迎教师奖'
    ]
  },
  7: {
    id: 7,
    name: '杨老师',
    subject: '英语',
    grade: '小学',
    experience: 5,
    rating: 4.9,
    description: '英语专业毕业，有海外留学经验，擅长通过游戏、歌曲等形式激发孩子学习英语的兴趣。',
    detailedDescription: `杨老师英语专业毕业，有海外留学经验，专注小学英语教学5年。

教学特点：
1. 趣味教学法，通过游戏歌曲学英语
2. 注重培养孩子的英语学习兴趣
3. 纯正发音，从小培养正确语感
4. 耐心细致，善于与孩子沟通

教学成果：
- 学生英语学习兴趣浓厚，积极性高
- 发音标准，口语表达自然流畅
- 词汇量和语法基础扎实`,
    avatar: teacherGirl4,
    tags: ['趣味教学', '语音纠正', '词汇积累'],
    schedule: ['周三 15:00-17:00', '周五 15:00-17:00', '周六 10:00-12:00'],
    gender: 'Female',
    teachingStyle: '趣味互动',
    education: '外国语大学英语专业学士',
    location: '新加坡东部',
    contactPhone: '+65 8888 8894',
    contactEmail: 'yang.teacher@grabteacher.com',
    achievements: [
      '2020年少儿英语教学奖',
      '2018年创意教学方法奖',
      '2016年最受学生喜爱奖'
    ]
  },
  8: {
    id: 8,
    name: '周老师',
    subject: '物理',
    grade: '初中',
    experience: 8,
    rating: 4.7,
    description: '物理教育专业毕业，擅长实验教学，能够通过实验激发学生的学习兴趣，培养学生的动手能力。',
    detailedDescription: `周老师物理教育专业毕业，有8年教学经验。

教学特点：
1. 实验教学为主，理论结合实践
2. 注重培养学生动手操作能力
3. 激发学生对物理的探索兴趣
4. 循循善诱，耐心解答学生疑问

教学成果：
- 学生动手实验能力强，理解深刻
- 物理学习兴趣和成绩双提升
- 培养学生科学探究精神`,
    avatar: studentGirl2,
    tags: ['实验教学', '概念讲解', '题型分析'],
    schedule: ['周一 17:00-19:00', '周四 17:00-19:00', '周日 14:00-16:00'],
    gender: 'Female',
    teachingStyle: '实验导向',
    education: '华中师范大学物理教育专业学士',
    location: '新加坡西部',
    contactPhone: '+65 8888 8895',
    contactEmail: 'zhou.teacher@grabteacher.com',
    achievements: [
      '2019年实验教学优秀奖',
      '2017年青年教师成长奖',
      '2015年学生实践指导奖'
    ]
  }
}

// 视频播放控制

// 返回上一页
const goBack = () => {
  router.back()
}

// 获取教师详情数据
// 加载可选择的授课地点（根据教师配置）
async function loadAllowedTeachingLocations() {
  try {
    const res = await publicTeachingLocationAPI.getActive()
    const list = res?.success ? (res.data || []) : []
    activeTeachingLocations.value = list

    const options: Array<{ value: string | number; label: string }> = []
    if (teacher.value?.supportsOnline) {
      options.push({ value: 'online', label: '线上' })
    }
    const ids: number[] = Array.isArray((teacher.value as any)?.teachingLocationIds) ? (teacher.value as any).teachingLocationIds : []
    ids.forEach((id) => {
      const item = list.find((x: any) => x.id === id)
      if (item) options.push({ value: id, label: item.name })
    })

    allowedTeachingLocations.value = options
    if (!selectedTeachingLocation.value && options.length > 0) {
      selectedTeachingLocation.value = options[0].value
    }
  } catch (e) {
    console.error('加载授课地点失败:', e)
  }
}

const fetchTeacherDetail = async () => {
  try {
    loading.value = true
    const result = await teacherAPI.getDetail(teacherId)

    if (result.success && result.data) {
      // 转换后端数据格式为前端需要的格式
      const teacherData = result.data
      teacher.value = {
        id: teacherData.id,
        name: teacherData.realName,
        subject: teacherData.subjects && teacherData.subjects.length > 0 ? teacherData.subjects[0] : '未设置',
        subjects: teacherData.subjects || [], // 保存完整的科目列表
        experience: teacherData.teachingExperience || 0,
        rating: 4.8, // 暂时使用默认值
        description: teacherData.introduction || '暂无介绍',
        detailedDescription: teacherData.introduction || '暂无详细介绍',
        avatar: teacherData.avatarUrl || teacherBoy1, // 使用用户头像或默认头像
        tags: teacherData.specialties ? teacherData.specialties.split(',') : [],
        schedule: ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00'], // 暂时使用默认值
        gender: teacherData.gender || 'Male',
        teachingStyle: '个性化教学',
        education: teacherData.educationBackground || '暂无信息',
        location: '新加坡',
        contactPhone: teacherData.phone || '未设置',
        contactEmail: teacherData.email || '未设置',
        achievements: ['优秀教师奖'],
        level: teacherData.level || '未设置'
      }


      // 授课地点能力
      teacher.value.supportsOnline = !!teacherData.supportsOnline
      teacher.value.teachingLocationIds = Array.isArray(teacherData.teachingLocationIds) ? teacherData.teachingLocationIds : []

      // 载入可选授课地点
      await loadAllowedTeachingLocations()

    } else {
      // 接口返回失败：使用安全兜底，保留页面，不再跳转
      if (teachersData[teacherId]) {
        teacher.value = teachersData[teacherId]
        ElMessage.warning('使用默认教师数据')
      } else {
        teacher.value = {
          id: teacherId,
          name: '该教师',
          subjects: [],
          level: '未设置',
          description: '暂无详细资料',
          avatar: '',
          tags: []
        }
        ElMessage.warning('暂未获取到教师详情，已使用最小信息展示')
      }
    }
  } catch (error) {
    console.error('获取教师详情失败:', error)
    // 如果API失败，使用默认数据
    if (teachersData[teacherId]) {
      teacher.value = teachersData[teacherId]
      ElMessage.warning('网络异常，使用默认教师数据')
    } else {
      teacher.value = {
        id: teacherId,
        name: '该教师',
        subjects: [],
        level: '未设置',
        description: '暂无详细资料',
        avatar: '',
        tags: []
      }
      ElMessage.warning('暂未获取到教师详情，已使用最小信息展示')
    }
  } finally {
    loading.value = false
  }
}

// 获取学生评价数据
const fetchTeacherReviews = async () => {
  try {
    reviewsLoading.value = true
    const pageSize = 50
    reviewsSize.value = pageSize

    let all: any[] = []
    let total = 0

    // 第一次请求，获取总数
    const first = await evaluationAPI.listPublic({
      teacherId: teacherId,
      page: 1,
      size: pageSize
    })

    if (first.success && first.data) {
      const records = first.data.records || []
      total = Number(first.data.total || records.length || 0)
      all = records

      const totalPages = Math.ceil(total / pageSize)
      for (let p = 2; p <= totalPages; p++) {
        const res = await evaluationAPI.listPublic({ teacherId: teacherId, page: p, size: pageSize })
        if (res.success && res.data && res.data.records) {
          all = all.concat(res.data.records)
        }
      }

      reviews.value = all
      reviewsTotal.value = total
    } else {
      reviews.value = []
      reviewsTotal.value = 0
    }
  } catch (error) {
    console.error('获取学生评价失败:', error)
    reviews.value = []
    reviewsTotal.value = 0
  } finally {
    reviewsLoading.value = false
  }
}

// 格式化日期时间
const formatDateTime = (iso: string) => {
  if (!iso) return '未知时间'
  const date = new Date(iso)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取学生姓名首字母
const getStudentInitial = (name: string) => {
  if (!name) return '学'
  return name.charAt(0).toUpperCase()
}

onMounted(() => {
  // 页面加载时滚动到顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })

  // 获取教师详情
  fetchTeacherDetail()

  // 获取学生评价
  fetchTeacherReviews()

  // 若带有 autoBooking 参数，则自动触发预约
  const autoBooking = (route.query as any)?.autoBooking
  if (autoBooking) {
    nextTick(() => {
      handleBooking()
      // 清理 autoBooking，避免再次触发
      const q: any = { ...(route.query || {}) }
      delete q.autoBooking
      router.replace({ path: route.path, query: q })
    })
  }
})

// 立即预约按钮点击事件
const handleBooking = async () => {

  // 检查登录状态
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再预约')
    router.push({ path: '/login' })
    return
  }

  // 已登录，显示课程类型选择
  showBookingTypeSelection()
}

// 显示课程类型选择
const showBookingTypeSelection = () => {
  ElMessageBox.confirm('请选择预约类型', '选择课程类型', {
    confirmButtonText: '试听课',
    cancelButtonText: '正式课',
    type: 'info',
    center: true,
    distinguishCancelAndClose: true
  }).then(async () => {
    // 选择试听课：弹出科目/年级/地点选择窗口
    bookingType.value = 'trial'
    await showTrialBookingModal()
  }).catch((action) => {
    if (action === 'cancel') {
      // 选择正式课
      bookingType.value = 'formal'
      showFormalBookingModal()
    }
  })
}

// 显示试听课预约弹窗（切换为日历预约）
const showTrialBookingModal = async () => {
  await Promise.all([loadSubjects(), loadGrades()])
  showBookingModal.value = true
}

const openTrialCalendar = () => {
  if (!selectedSubject.value) { ElMessage.warning('请先选择科目'); return }
  if (!selectedGrade.value) { ElMessage.warning('请先选择年级'); return }
  if (!selectedTeachingLocation.value) { ElMessage.warning('请先选择授课地点'); return }
  // 关闭当前信息收集弹窗，打开日历
  showBookingModal.value = false
  trialDlg.value?.open()
}


// 显示正式课预约弹窗（显示预约课程按钮页面）
// 加载年级列表（公开）
const loadGrades = async () => {
  try {
    const res = await publicGradeAPI.list()
    if (res.success && Array.isArray(res.data)) {
      grades.value = res.data.map((g: any) => ({ id: g.id ?? g.name, name: g.name ?? String(g) }))
    } else {
      grades.value = []
    }
  } catch (e) {
    grades.value = []
  }
}

const showFormalBookingModal = async () => {
  showBookingModal.value = false
  showCourseScheduleModal.value = true
  await Promise.all([loadTeacherCourses(), loadGrades()])
}

// 加载科目列表
const loadSubjects = async () => {
  try {
    loadingSubjects.value = true

    // 如果教师详情已经加载，直接使用教师的科目
    if (teacher.value && teacher.value.subjects) {
      // 将科目名称转换为科目对象格式
      subjects.value = teacher.value.subjects.map((subjectName: string) => ({
        id: subjectName, // 使用科目名称作为ID
        name: subjectName,
        active: true
      }))
    } else {
      // 如果教师详情未加载，重新获取教师详情
      const result = await teacherAPI.getDetail(teacherId)
      if (result.success && result.data && result.data.subjects) {
        subjects.value = result.data.subjects.map((subjectName: string) => ({
          id: subjectName,
          name: subjectName,
          active: true
        }))
      } else {
        ElMessage.error('该教师暂无可授课科目')
        subjects.value = []
      }
    }
  } catch (error) {
    console.error('加载教师科目失败:', error)
    ElMessage.error('加载教师科目失败')
  } finally {
    loadingSubjects.value = false
  }
}

// 生成半小时切分的时间段
const generateHalfHourSlots = (start: string, end: string) => {
  const result: string[] = []

  const [startH, startM] = start.split(':').map(n => parseInt(n))
  const [endH, endM] = end.split(':').map(n => parseInt(n))

  let startMinutes = startH * 60 + startM
  const endMinutes = endH * 60 + endM

  while (startMinutes + 30 <= endMinutes) {
    const sH = Math.floor(startMinutes / 60).toString().padStart(2, '0')
    const sM = (startMinutes % 60).toString().padStart(2, '0')
    const eMinutes = startMinutes + 30
    const eH = Math.floor(eMinutes / 60).toString().padStart(2, '0')
    const eM = (eMinutes % 60).toString().padStart(2, '0')
    result.push(`${sH}:${sM}-${eH}:${eM}`)
    startMinutes += 30
  }
  return result
}

// 选择时间段类型
const selectTimePeriod = (period: string) => {
  selectedTimePeriod.value = period
  const periodData = timePeriods.find(p => p.value === period)
  if (periodData) {
    availableTimeSlots.value = generateHalfHourSlots(periodData.range[0], periodData.range[1])
  } else {
    availableTimeSlots.value = []
  }
  selectedTimeSlot.value = ''
}

// 选择时间段
const selectTimeSlot = (timeSlot: string) => {
  selectedTimeSlot.value = timeSlot
}

// 旧试听课预约入口保留，但默认不再使用（改为日历）
// 确认试听课预约
const confirmTrialBooking = async () => {
  if (!selectedSubject.value || !selectedGrade.value || !selectedDate.value || !selectedTimeSlot.value) {
    ElMessage.warning('请完整填写预约信息')
    return
  }

  if (!selectedTeachingLocation.value) {
    ElMessage.warning('请选择授课地点')
    return
  }


  try {
    // 构建试听课预约请求数据
    const bookingData: any = {
      teacherId: teacherId,
      bookingType: 'single' as const,
      studentRequirements: `申请与${teacher.value?.name}老师进行${selectedSubject.value}科目试听`,
      grade: String(selectedGrade.value),

      isTrial: true,
      trialDurationMinutes: 30,
      requestedDate: selectedDate.value,
      requestedStartTime: selectedTimeSlot.value.split('-')[0],
      requestedEndTime: selectedTimeSlot.value.split('-')[1]
    }

    // 选择授课地点
    if (selectedTeachingLocation.value === 'online') {
      bookingData.teachingLocation = '线上'
    } else {
      bookingData.teachingLocationId = Number(selectedTeachingLocation.value)
    }

    // 调用后端API创建试听课预约申请
    const result = await bookingAPI.createRequest(bookingData)

    if (result.success && result.data) {
      const successMessage = `试听课申请提交成功！${teacher.value?.name} - ${selectedDate.value} ${selectedTimeSlot.value}（30分钟免费试听）`

      ElMessage.success({
        message: successMessage,
        duration: 5000
      })

      showBookingModal.value = false
      resetBookingForm()

      // 跳转到学生预约管理页面
      setTimeout(() => {
        ElMessage.info('即将跳转到预约管理页面...')
        // router.push('/student/bookings') // 暂时注释，因为这个页面可能还不存在
      }, 2000)
    } else {
      // 显示后端返回的具体错误信息
      const errorMessage = result.message || '试听课预约申请提交失败'
      ElMessage.error({
        message: errorMessage,
        duration: 8000,
        showClose: true
      })

      // 如果是余额不足或试听次数不足，显示特殊提示
      if (errorMessage.includes('余额不足')) {
        ElMessage.warning({
          message: '您的账户余额不足，请联系管理员充值',
          duration: 10000,
          showClose: true
        })
      } else if (errorMessage.includes('试听次数')) {
        ElMessage.warning({
          message: '您的免费试听次数已用完，如需增加机会请联系管理员',
          duration: 10000,
          showClose: true
        })
      }
    }
  } catch (error) {
    console.error('试听课预约失败:', error)

    // 尝试从错误对象中提取更详细的错误信息
    let errorMessage = '试听课预约申请提交失败，请稍后重试'

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

// 确认正式课预约
const confirmFormalBooking = () => {
  // 显示课程安排页面
  showFormalBookingModal()
}

// 重置预约表单
const resetBookingForm = () => {
  selectedSubject.value = ''
  selectedDate.value = ''
  selectedTimeSlot.value = ''
  selectedTimePeriod.value = ''
  availableTimeSlots.value = []
  selectedTeachingLocation.value = ''
  selectedGrade.value = ''

  bookingType.value = ''
}

// 关闭预约弹窗
const closeBookingModal = () => {
  showBookingModal.value = false
  resetBookingForm()
}

// 加载教师课程列表
const loadTeacherCourses = async () => {
  try {
    loadingCourses.value = true
    const result = await teacherAPI.getPublicCourses(teacherId)
    if (result.success && result.data) {
      // 智能匹配功能只显示1对1课程
      const oneOnOneCourses = result.data.filter((course: any) => course.courseType === 'one_on_one')
      teacherCourses.value = oneOnOneCourses

      if (oneOnOneCourses.length === 0) {
        ElMessage.warning('该教师暂无1对1课程可预约')
      } else {
        // 如果有课程，自动选择第一个课程
        if (oneOnOneCourses.length > 0) {
          selectedCourse.value = oneOnOneCourses[0]
        }
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

// 选择课程
const selectCourse = (course: any) => {
  selectedCourse.value = course
  scheduleForm.value.selectedCourse = course

  loadCourseTimeSlots()
}

// 加载课程可用时间段
const loadCourseTimeSlots = async () => {
  if (!selectedCourse.value) return

  try {
    loadingCourseTimeSlots.value = true
    // 这里应该调用API获取该课程的时间段
    // 根据课程时长生成不同的时间段
    let timeSlots = []

    if (selectedCourse.value.durationMinutes === 90) {
      // 90分钟课程的时间段
      timeSlots = [
        '08:00-10:00', '10:00-12:00',
        '13:00-15:00', '15:00-17:00',
        '17:00-19:00', '19:00-21:00'
      ]
    } else if (selectedCourse.value.durationMinutes === 120) {
      // 120分钟课程的时间段
      timeSlots = [
        '08:00-10:00', '10:00-12:00',
        '13:00-15:00', '15:00-17:00',
        '17:00-19:00', '19:00-21:00'
      ]
    } else {
      // 默认90分钟时间段
      timeSlots = [
        '08:00-10:00', '10:00-12:00',
        '13:00-15:00', '15:00-17:00',
        '17:00-19:00', '19:00-21:00'
      ]
    }

    // 为每个星期几生成时间段数据
    const timeSlotData = []
    for (let weekday = 1; weekday <= 7; weekday++) {
      timeSlots.forEach(timeSlot => {
        timeSlotData.push({
          weekday,
          timeSlot,
          available: true
        })
      })
    }

    availableTimeSlotsForCourse.value = timeSlotData
  } catch (error) {
    console.error('加载时间段失败:', error)
    ElMessage.error('加载时间段失败')
  } finally {
    loadingCourseTimeSlots.value = false
  }
}



// 确认课程预约
const openCalendarBooking = () => {
  if (!selectedCourse.value) { ElMessage.warning('请先选择课程'); return }
  if (!selectedGrade.value) { ElMessage.warning('请先选择年级'); return }
  if (!selectedTeachingLocation.value) { ElMessage.warning('请先选择授课地点'); return }
  const defaultDuration = (selectedCourse.value as any)?.durationMinutes || 90
  calendarDlg.value?.open({ defaultDuration })
}


const onTrialConfirm = async (payload: { date: string; startTime: string; endTime: string }) => {
  try {
    const bookingData: any = {
      teacherId: teacherId,
      bookingType: 'single' as const,
      studentRequirements: `申请与${teacher.value?.name}老师进行${selectedSubject.value}科目试听`,
      grade: String(selectedGrade.value),
      isTrial: true,
      trialDurationMinutes: 30,
      requestedDate: payload.date,
      requestedStartTime: payload.startTime,
      requestedEndTime: payload.endTime
    }
    if (selectedTeachingLocation.value === 'online') {
      bookingData.isOnline = true
    } else if (selectedTeachingLocation.value) {
      bookingData.teachingLocationId = Number(selectedTeachingLocation.value)
    }
    const result = await bookingAPI.createRequest(bookingData)
    if (result.success && result.data) {
      ElMessage.success(`试听课申请提交成功！${teacher.value?.name} - ${payload.date} ${payload.startTime}-${payload.endTime}`)
    } else {
      ElMessage.error(result.message || '试听课预约申请提交失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '试听课预约申请提交失败，请稍后重试')
  }
}

const onCalendarConfirm = async (sessions: Array<{ date: string; startTime: string; endTime: string }>, duration: 90|120) => {
  try {
    const bookingData: any = {
      teacherId: teacherId,
      courseId: (selectedCourse.value as any)?.id,
      grade: String(selectedGrade.value),
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
      showCourseScheduleModal.value = false
      resetCourseForm()
    } else {
      ElMessage.error(result.message || '预约提交失败')
    }
  } catch (e:any) {
    ElMessage.error(e?.message || '预约提交失败')
  }
}


// 重置课程表单
const resetCourseForm = () => {
  selectedCourse.value = null
  scheduleForm.value.selectedCourse = null
  availableTimeSlotsForCourse.value = []
  selectedGrade.value = ''
}

// 关闭课程安排弹窗
const closeCourseScheduleModal = () => {
  showCourseScheduleModal.value = false
  resetCourseForm()
}


// 获取可用时间段
const getAvailableTimeSlots = () => {
  const timeSlots = new Set()
  availableTimeSlotsForCourse.value.forEach(slot => {
    if (slot.available) {
      timeSlots.add(slot.timeSlot)
    }
  })
  return Array.from(timeSlots)
}

// 判断时间段是否可用于选择
const isTimeSlotAvailableForSelection = (timeSlot: string) => {
  return availableTimeSlotsForCourse.value.some(slot =>
    slot.timeSlot === timeSlot && slot.available
  )
}

// 监听课程选择变化
watch(selectedCourse, (newCourse) => {
  if (newCourse) {
    scheduleForm.value.selectedCourse = newCourse

    loadCourseTimeSlots()
  }
})




</script>

<template>
  <div class="teacher-detail">
    <div class="detail-header">
      <el-button @click="goBack" class="back-button" text bg size="large">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>教师详情</h2>
    </div>

    <div class="detail-content">
      <div v-if="loading" class="loading-container">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <p>正在加载教师详情...</p>
      </div>
      <div v-else-if="teacher" class="teacher-profile">
        <div class="profile-header">
          <div class="profile-avatar">
            <img :src="teacher.avatar" :alt="teacher.name">
          </div>
          <div class="profile-basic">
            <h3>{{ teacher.name }}</h3>
            <div class="teacher-rating">
              <el-rate v-model="teacher.rating" disabled text-color="#ff9900" />
              <span class="rating-text">{{ teacher.rating.toFixed(1) }}分</span>
            </div>
            <div class="teacher-tags">
              <div class="tag-group">
                <el-tag v-for="(s, i) in teacher.subjects" :key="'sub-'+i" type="success" effect="dark" class="subject-tag">{{ s }}</el-tag>
                <el-tag type="warning" effect="plain" class="experience-tag">{{ teacher.experience }}年教龄</el-tag>
                <el-tag type="danger" effect="plain" class="level-tag">{{ teacher.level }}</el-tag>
                <el-tag type="info" effect="plain" class="gender-tag">
                  {{ teacher.gender === 'Male' ? '男' : '女' }}
                </el-tag>
              </div>
              <div class="tag-group">
                <el-tag v-for="(tag, i) in teacher.tags" :key="i" size="small" class="teacher-tag" effect="light">{{ tag }}</el-tag>
              </div>
            </div>
            <div class="profile-info-row">
              <div class="info-item">
                <el-icon><School /></el-icon>
                <span>{{ teacher.education }}</span>
              </div>
              <div class="info-item">
                <el-icon><Location /></el-icon>
                <span>{{ teacher.location }}</span>
              </div>
            </div>
            <div class="profile-info-row">
              <div class="info-item">
                <el-icon><Phone /></el-icon>
                <span>{{ teacher.contactPhone }}</span>
              </div>
              <div class="info-item">
                <el-icon><Message /></el-icon>
                <span>{{ teacher.contactEmail }}</span>
              </div>
            </div>
            <div class="cta-row">
              <el-button type="primary" size="large" @click="handleBooking">
                <el-icon><Calendar /></el-icon>
                立即预约
              </el-button>
            </div>
          </div>
        </div>


        <!-- 可授课时间（精简展示，支持展开/收起） -->

        <div class="profile-section">
          <h4 class="section-title"><el-icon><Document /></el-icon> 教师介绍</h4>
          <div class="section-content">
            <p v-for="(paragraph, index) in teacher.detailedDescription.split('\n\n')" :key="index" class="description-paragraph">
              {{ paragraph }}
            </p>
          </div>
        </div>

        <div class="profile-section">
          <h4 class="section-title"><el-icon><Trophy /></el-icon> 教学成就</h4>
          <div class="section-content">
            <ul class="achievements-list">
              <li v-for="(achievement, index) in teacher.achievements" :key="index">
                {{ achievement }}
              </li>
            </ul>
          </div>
        </div>





        <!-- 学生评价滚动展示 -->
        <div class="profile-section reviews-section">
          <h4 class="section-title">
            <el-icon><Star /></el-icon>
            学生评价
            <span class="reviews-count">({{ reviewsTotal }}条评价)</span>
          </h4>

          <div v-loading="reviewsLoading" class="reviews-container">
            <div
              v-if="reviews.length > 0"
              class="reviews-scroll"
              ref="reviewsScrollRef"
              @mouseenter="isReviewsHovering = true"
              @mouseleave="isReviewsHovering = false"
            >
              <div
                v-for="(review, idx) in loopedReviews"
                :key="(review.id || idx) + '-' + idx"
                class="review-item"
              >
                <div class="review-header">
                  <div class="student-avatar">
                    {{ getStudentInitial(review.studentName) }}
                  </div>
                  <div class="review-meta">
                    <div class="student-name">{{ review.studentName || '学员' }}</div>
                    <div class="course-name">{{ review.courseName }}</div>
                    <div class="review-time">{{ formatDateTime(review.createdAt) }}</div>
                  </div>
                  <div class="review-rating">
                    <el-rate
                      :model-value="Number(review.rating || 0)"
                      disabled
                      allow-half
                      size="small"
                    />
                  </div>
                </div>
                <div class="review-content">
                  <p>{{ review.studentComment || '暂无评论内容' }}</p>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else-if="!reviewsLoading" class="empty-reviews">
              <el-empty description="暂无学生评价">
                <el-button type="primary" @click="fetchTeacherReviews">刷新</el-button>
              </el-empty>
            </div>
          </div>
        </div>

      </div>
      <div v-else class="error-container">
        <p>教师信息加载失败</p>
      </div>
    </div>

    <!-- 预约弹窗 -->
    <el-dialog
      v-model="showBookingModal"
      :title="bookingType === 'trial' ? '预约试听课' : '预约正式课'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
      @close="closeBookingModal"
    >
      <!-- 试听课预约表单 -->
      <div v-if="bookingType === 'trial'" class="booking-form">
        <el-form :model="{}" label-width="100px">
          <el-form-item label="选择科目" required>
            <el-select
              v-model="selectedSubject"
              placeholder="请选择科目"
              style="width: 100%"
              :loading="loadingSubjects"
            >
              <el-option
                v-for="subject in subjects"
                :key="subject.id"
                :label="subject.name"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="选择年级" required>
            <el-select
              v-model="selectedGrade"
              placeholder="请选择年级"
              style="width: 100%"
            >
              <el-option
                v-for="g in grades"
                :key="g.id"
                :label="g.name"
                :value="g.id"
              />
            </el-select>
          </el-form-item>


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


          <el-form-item label="上课日期" required>
            <el-date-picker
              v-model="selectedDate"
              type="date"
              placeholder="选择日期"
              style="width: 100%"
              :disabled-date="(time) => time.getTime() < Date.now() - 8.64e7"
            />
          </el-form-item>

          <el-form-item label="时间段类型" required>
            <el-radio-group v-model="selectedTimePeriod" @change="selectTimePeriod">
              <el-radio
                v-for="period in timePeriods"
                :key="period.value"
                :label="period.value"
              >
                {{ period.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="selectedTimePeriod" label="具体时间" required>
            <div class="time-slots">
              <el-button
                v-for="slot in availableTimeSlots"
                :key="slot"
                :type="selectedTimeSlot === slot ? 'primary' : 'default'"
                :plain="selectedTimeSlot !== slot"
                @click="selectTimeSlot(slot)"
                class="time-slot-btn"
              >
                {{ slot }}
              </el-button>
  </div>
          </el-form-item>
        </el-form>

        <div class="booking-actions">
          <el-button @click="closeBookingModal">取消</el-button>
          <el-button type="primary" @click="confirmTrialBooking" :disabled="!selectedSubject || !selectedGrade || !selectedDate || !selectedTimeSlot || !selectedTeachingLocation">
            确认预约
          </el-button>
        </div>
      </div>

      <!-- 正式课预约页面 -->
      <div v-else-if="bookingType === 'formal'" class="formal-booking">
        <div class="booking-info">
          <el-icon class="info-icon"><Clock /></el-icon>
          <h3>预约正式课程</h3>
          <p>您将跳转到1v1智能教师匹配页面进行正式课程预约</p>
        </div>

        <div class="teacher-info-card">
          <div class="teacher-avatar">
            <img :src="teacher?.avatar" :alt="teacher?.name" />
          </div>
          <div class="teacher-details">
            <h4>{{ teacher?.name }}</h4>
            <p>{{ teacher?.subject }} · {{ teacher?.experience }}年教龄</p>
            <div class="teacher-tags">
              <el-tag v-for="tag in teacher?.tags" :key="tag" size="small" class="teacher-tag">
                {{ tag }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="booking-actions">
          <el-button @click="closeBookingModal">取消</el-button>
          <el-button type="primary" @click="confirmFormalBooking">
            <el-icon><Calendar /></el-icon>
            预约课程
          </el-button>
        </div>
      </div>
    </el-dialog>

    <StudentBookingCalendar ref="calendarDlg" :teacher-id="teacherId" @confirm="onCalendarConfirm" />
<StudentTrialCalendar ref="trialDlg" :teacher-id="teacherId" @confirm="onTrialConfirm" />

    <!-- 课程安排弹窗 -->
    <el-dialog
      v-model="showCourseScheduleModal"
      :title="`${teacher?.name} - 课程安排`"
      width="900px"
      :close-on-click-modal="false"
      destroy-on-close
      @close="closeCourseScheduleModal"
    >
      <div class="schedule-modal-content">
        <div class="schedule-tip">
          <el-alert
            title="预约说明"
            description="请选择课程、年级与授课地点，然后点击“使用日历预约”在日历上选择具体日期与时间段；系统将为每个选择生成独立的一对一课程安排"
            type="info"
            show-icon
            :closable="false"
          />
        </div>

        <div style="margin: 8px 0 12px 0; text-align: right;">
          <el-button type="success" plain size="small" @click="openCalendarBooking">
            使用日历预约
          </el-button>
        </div>


        <el-form :model="scheduleForm" label-width="120px" class="schedule-form">

          <el-form-item label="选择年级" required>
            <el-select
              v-model="selectedGrade"
              placeholder="请选择年级"
              style="width: 100%"
            >
              <el-option
                v-for="g in grades"
                :key="g.id"
                :label="g.name"
                :value="g.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="选择课程" required>
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

          <!-- 课程时长选择（仅当课程时长为空时显示） -->
          <el-form-item v-if="selectedCourse" label="选择课程时长">
            <el-radio-group v-model="scheduleForm.selectedDurationMinutes" size="large" class="course-duration-selection">
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


          <!-- 课程安排：已改为按月日历预约 -->
          <el-form-item label="课程安排">
            <el-alert type="info" :closable="false" show-icon title="本系统已改为按月日历预约，请点击上方“使用日历预约”选择具体日期与时间段" />
          </el-form-item>

          <!-- 已移除按星期的时间段/星期选择与课程预览，改为使用上方“使用日历预约”选择具体日期与时间段 -->
        </el-form>

        <!-- 操作按钮：引导使用日历预约 -->
        <div class="schedule-actions">
          <el-button @click="closeCourseScheduleModal">关闭</el-button>
          <el-button
            type="primary"
            @click="openCalendarBooking"
            :disabled="!selectedCourse || !selectedGrade || !selectedTeachingLocation"
          >
            使用日历预约
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.teacher-detail {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.back-button {
  margin-right: 20px;
}

h2 {
  font-size: 28px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.detail-content {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  padding: 30px;
  border: 1px solid #f0f0f0;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #666;
}

.loading-icon {
  font-size: 48px;
  color: #409eff;
  animation: rotate 2s linear infinite;
  margin-bottom: 20px;
}

.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #999;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.teacher-profile {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.profile-header {
  display: flex;
  gap: 30px;
  margin-bottom: 20px;
}

.profile-avatar {
  width: 180px;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border: 3px solid #fff;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-basic {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.profile-basic h3 {
  font-size: 24px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.teacher-rating {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-text {
  font-size: 16px;
  color: #ff9900;
  font-weight: 500;
}

.teacher-tags {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.profile-info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 5px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.profile-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 20px;
}

.section-title {
  font-size: 18px;
  color: #333;
  margin-bottom: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-content {
  color: #666;
  line-height: 1.6;
}

.description-paragraph {
  margin-bottom: 15px;
}

.achievements-list {
  padding-left: 20px;
  margin: 0;
}

.achievements-list li {
  margin-bottom: 10px;
}

.schedule-times {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.schedule-tag {
  padding: 8px 15px;
}


/* 可授课时间（精简展示） */
.availability-section .availability-summary {
  margin-left: 6px;
  font-weight: 400;
  color: #666;
  font-size: 13px;
}
.availability-section .chips-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  position: relative;
}
.availability-section .chips-container.collapsed {
  max-height: 84px; /* 控制折叠高度，约两行标签 */
  overflow: hidden;
}
.availability-section .chips-container.collapsed::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0), #fff);
  pointer-events: none;
}
.availability-section .time-chip {
  border-radius: 14px;
}
.availability-section .toggle-more {
  text-align: right;
  margin-top: 6px;
}


.action-buttons {
  display: flex;
  gap: 15px;
  margin-top: 30px;
}

/* 学生评价样式 */
.reviews-section {
  margin-top: 30px;
}

.reviews-count {
  font-size: 14px;
  color: #999;
  font-weight: normal;
  margin-left: 10px;
}

.reviews-container {
  margin-top: 20px;
}

.reviews-scroll {
  height: 320px;
  overflow: hidden;
  padding-right: 0;
}

.reviews-scroll::-webkit-scrollbar {
  width: 6px;
}

.reviews-scroll::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.reviews-scroll::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.reviews-scroll::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.review-item {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 15px;
  transition: all 0.3s ease;
}

.review-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #e0e0e0;
}

.review-header {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  margin-bottom: 15px;
}

.student-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
  flex-shrink: 0;
}

.review-meta {
  flex: 1;
  min-width: 0;
}

.student-name {
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.course-name {
  font-size: 12px;
  color: #666;
  background: #e8f4fd;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
  margin-bottom: 4px;
}

.review-time {
  font-size: 12px;
  color: #999;
}

.review-rating {
  flex-shrink: 0;
}

.review-content {
  color: #555;
  line-height: 1.6;
  font-size: 14px;
}

.review-content p {
  margin: 0;
  word-break: break-word;
}

.empty-reviews {
  text-align: center;
  padding: 40px 20px;
}

/* 预约弹窗样式 */
.booking-form {
  padding: 20px 0;
}

.time-slots {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.time-slot-btn {
  min-width: 100px;
  margin: 0;
}

.booking-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.formal-booking {
  padding: 20px 0;
}

.booking-info {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.info-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 15px;
}

.booking-info h3 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 20px;
}

.booking-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.teacher-info-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 30px;
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

.teacher-details {
  flex: 1;
}

.teacher-details h4 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 18px;
}

.teacher-details p {
  margin: 0 0 10px 0;
  color: #666;
  font-size: 14px;
}

.teacher-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.teacher-tag {
  margin: 0;
}

/* 课程安排弹窗样式 - 完全复制自TeacherMatch.vue */
.schedule-modal-content {
  padding: 20px 0;
}

.schedule-tip {
  margin-bottom: 20px;
}

.schedule-form {
  margin-top: 20px;
}

.form-item-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

.form-item-tip .el-icon {
  color: #409eff;
  font-size: 14px;
}

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
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.course-duration-selection {
  display: flex;
  gap: 20px;
}

.duration-option {
  padding: 10px 20px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  text-align: center;
  min-width: 100px;
}

.duration-label {
  font-weight: 500;
  color: #333;
}

.course-schedule-row {
  display: flex;
  gap: 20px;
  align-items: end;
  flex-wrap: wrap;
}

.schedule-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schedule-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.session-count-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-unit {
  font-size: 14px;
  color: #666;
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

.time-slot-card.disabled {
  border-color: #dcdfe6;
  background-color: #f5f7fa;
  color: #c0c4cc;
  cursor: not-allowed;
  opacity: 0.6;
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

.slot-time {
  font-weight: 600;
  color: #333;
  font-size: 14px;
  margin-bottom: 8px;
  display: flex;
  justify-content: center;
}

.slot-duration-info {
  margin-bottom: 10px;
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

.weekday-selection {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  padding: 18px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.weekday-label {
  margin-left: 8px;
  font-size: 14px;
}

.schedule-preview {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  margin-top: 10px;
}

.preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
}

.preview-content {
  margin-bottom: 15px;
}

.preview-weekdays,
.preview-timeslots {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.preview-label {
  font-size: 14px;
  color: #666;
  min-width: 80px;
}

.schedule-tag {
  margin: 0;
}

.timeslots-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.timeslot-tag {
  margin: 0;
}

.preview-details {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.schedule-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .time-slot-selection {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
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
}
</style>
