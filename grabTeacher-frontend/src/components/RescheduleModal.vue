<template>
  <el-dialog
    :model-value="visible"
    :title="`调课申请 - ${course?.title}`"
    width="800px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
    @update:model-value="updateVisible"
  >
    <div class="reschedule-modal-content">
      <div class="reschedule-info">
        <el-alert
          title="调课须知"
          description="调课须在开课前4小时之外发起，管理员审批同意后生效。"
          type="info"
          show-icon
          :closable="false"
        />

        <!-- 教师端超额提示 -->
        <div style="margin-top: 10px;">
          <el-alert
            v-if="!props.isTeacher"
            :title="studentOverQuota ? '超额调课提示' : '余额与扣费规则说明'"
            :description="studentOverQuota
              ? '您本月可调课次数已用完，若继续调课，系统将扣除该课程0.5小时对应的M豆，且余额不足将无法提交。'
              : '若本月可调课次数不足，将扣除该课程0.5小时对应的M豆，且余额不足将无法提交。'"
            :type="studentOverQuota ? 'warning' : 'info'"
            show-icon
            :closable="false"
          />
          <el-alert
            v-else
            title="教师超额调课说明"
            description="若本月可调课次数为0，教师仍可发起调课，但系统会扣除教师本月课时1小时，并自动补偿该学生0.5小时对应的M豆。"
            type="info"
            show-icon
            :closable="false"
          />
        </div>
      </div>

      <div class="course-info">
        <div class="info-item">
          <span class="label">课程：</span>
          <span>{{ course?.title }}</span>
        </div>
        <div class="info-item">
          <span class="label">教师：</span>
          <span>{{ course?.teacher }}</span>
        </div>
        <div class="info-item">
          <span class="label">剩余课时：</span>
          <span class="remaining-highlight">{{ course?.remainingLessons }}课时</span>
        </div>
        <div class="info-item" v-if="course?.weeklySchedule">
          <span class="label">当前安排：</span>
          <div class="current-schedule">
            <el-tag v-for="schedule in course.weeklySchedule" :key="schedule" size="small">
              {{ schedule }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="reschedule-type-selection"></div>

      <!-- 调课表单（仅单次，按具体节次与时间） -->
      <div class="single-reschedule-form">
        <el-form :model="rescheduleForm" label-width="120px">
          <el-form-item label="原定课程">
            <div class="original-schedule-display">
              <el-select v-model="selectedScheduleId" placeholder="选择要调整的节次" style="width: 360px; margin-bottom: 8px;">
                <el-option
                  v-for="(s, idx) in futureSchedules"
                  :key="s.id"
                  :label="`第${idx + 1}节 ${s.scheduledDate} ${s.startTime}-${s.endTime}${!isScheduleAdjustable(s) ? '（4小时内不可调）' : ''}`"
                  :value="s.id"
                  :disabled="!isScheduleAdjustable(s)"
                />
              </el-select>

            </div>
          </el-form-item>

          <el-form-item label="调整到">
            <div class="new-schedule-group">
              <el-button type="primary" @click="openCalendar">
                {{ props.isTeacher ? '按日历选择（可多选）' : '按日历选择' }}
              </el-button>
              <!-- 学生端显示已选择 -->
              <template v-if="!props.isTeacher && rescheduleForm.newDate && rescheduleForm.newTime">
                <el-tag type="success" effect="plain" style="margin-left: 12px;">
                  已选择：{{ rescheduleForm.newDate }} {{ rescheduleForm.newTime }}
                </el-tag>
              </template>

              <!-- 教师端显示候选列表 -->
              <div v-if="props.isTeacher && candidateSessions.length > 0" class="candidate-list" style="margin-top: 8px;">
                <el-tag
                  v-for="(c, idx) in candidateSessions.slice(0, 3)"
                  :key="idx"
                  type="success"
                  effect="plain"
                  style="margin-right: 8px; margin-bottom: 8px;"
                >
                  {{ c.date }} {{ c.startTime }}-{{ c.endTime }}
                </el-tag>
                <el-tag
                  v-if="candidateSessions.length > 3"
                  type="info"
                  effect="plain"
                  style="margin-right: 8px; margin-bottom: 8px;"
                >
                  +{{ candidateSessions.length - 3 }}
                </el-tag>
              </div>

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
        <el-button @click="handleClose">取消</el-button>
    <StudentScheduler
      v-if="calendarTeacherId > 0"
      ref="studentSchedulerRef"
      :teacher-id="calendarTeacherId"
      :months="12"
      :multi-select="props.isTeacher"
      @confirm="onCalendarConfirm"
    />

        <el-button
          type="primary"
          @click="submitReschedule"
          :disabled="submitDisabled"
        >
          <el-icon><Check /></el-icon>
          提交申请
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, defineAsyncComponent } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Loading } from '@element-plus/icons-vue'
const StudentScheduler = defineAsyncComponent(() => import('./scheduler/StudentScheduler.vue'))
import { rescheduleAPI, teacherAPI, studentAPI } from '../utils/api'

// 课程接口
interface Course {
  id: number;
  title: string;
  teacher: string;
  teacherId: number;
  subject: string;
  remainingLessons?: number;
  weeklySchedule?: string[];
  schedules?: CourseSchedule[];
}

// 课程安排接口
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
  status: 'progressing' | 'completed' | 'cancelled' | 'rescheduled';
  teacherNotes?: string;
  studentFeedback?: string;
  createdAt: string;
  bookingRequestId: number;
  bookingSource: string;
  isTrial: boolean;
  sessionNumber: number;
  courseType: string;
}

// 教师可用时间接口
interface TeacherAvailableTime {
  weekday: number;
  timeSlots: string[];
}

// 时间冲突检查结果接口
interface TimeConflictResult {
  hasConflict: boolean;
  conflictType?: string;
  message?: string;
}

// Props
interface Props {
  modelValue: boolean;
  course: Course | null;
  isTeacher?: boolean; // 是否为教师端使用
  allSchedules?: CourseSchedule[]; // 全量真实课表（用于占位→真实映射）
}

// Emits
interface Emits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const visible = computed(() => props.modelValue)

// 更新visible的函数
const updateVisible = (value: boolean) => {
  emit('update:modelValue', value)
}

const rescheduleForm = ref({
  originalDate: '',
  originalTime: '',
  newDate: '',
  newTime: '',
  reason: ''
})

// 选择的原定节次ID
const selectedScheduleId = ref<number | null>(null)

// 未来可选的节次列表（仅保留“当前时间之后”的 scheduled/progressing 节次）
const futureSchedules = computed<CourseSchedule[]>(() => {
  const schedules = props.course?.schedules ?? []
  const nowTs = Date.now()
  const norm = (t?: string) => (t && t.length === 5 ? `${t}:00` : (t || '00:00:00'))

  // 1) 先按id去重：若同一id出现多条，保留“时间更新后”的那条（较新的日期+开始时间）
  const byId = new Map<number, CourseSchedule>()
  for (const s of schedules as CourseSchedule[]) {
    const id = Number((s as CourseSchedule).id)
    if (!id || isNaN(id)) continue
    const prev = byId.get(id)
    if (!prev) {
      byId.set(id, s)
    } else {
      const prevTs = new Date(`${prev.scheduledDate}T${norm(prev.startTime)}`).getTime()
      const currTs = new Date(`${s.scheduledDate}T${norm(s.startTime)}`).getTime()
      if (currTs >= prevTs) byId.set(id, s)
    }
  }

  const unique = Array.from(byId.values())

  // 2) 仅保留未来可调课的有效节次，并按时间升序
  return unique
    .filter((s: CourseSchedule) => {
      if (s.status === 'cancelled') return false
      const ts = new Date(`${s.scheduledDate}T${norm(s.startTime)}`).getTime()
      return ts > nowTs
    })
    .sort((a: CourseSchedule, b: CourseSchedule) => {
      const ta = new Date(`${a.scheduledDate}T${norm(a.startTime)}`).getTime()
      const tb = new Date(`${b.scheduledDate}T${norm(b.startTime)}`).getTime()
      return ta - tb
    })
})

// 当前选中的节次对象
const selectedSchedule = computed<CourseSchedule | undefined>(() => {
  if (!futureSchedules.value.length) return undefined
  if (selectedScheduleId.value != null) {
    return futureSchedules.value.find(s => s.id === selectedScheduleId.value) || futureSchedules.value[0]
  }
  return futureSchedules.value[0]
})

// 当切换选中节次时，同步原定课程显示与可用时间
watch(selectedScheduleId, async () => {
  let s = selectedSchedule.value
  // 若选择的是占位节次，尝试映射到当前课程中的真实节次（按日期+时间(+教师)）
  if (s && (!s.id || Number(s.id) <= 0) && Array.isArray(props.course?.schedules)) {
    const realCandidates = (props.course!.schedules! as CourseSchedule[]).filter((x: CourseSchedule) => x.id && Number(x.id) > 0)
    const norm = (t: string | undefined | null) => t ? (t.length === 5 ? `${t}:00` : t) : ''
    const exact = realCandidates.find((x: CourseSchedule) =>
      x.scheduledDate === s!.scheduledDate &&
      norm(x.startTime) === norm(s!.startTime) &&
      norm(x.endTime) === norm(s!.endTime) &&
      (!s!.teacherId || x.teacherId === s!.teacherId)
    )
    if (exact) {
      // 确保该真实节次出现在选项中
      if (Array.isArray(props.course?.schedules)) {
        const exists = (props.course!.schedules! as CourseSchedule[]).some(x => Number(x.id) === Number(exact.id))
        if (!exists) {
          (props.course!.schedules! as CourseSchedule[]).push(exact as CourseSchedule)
        }
      }
      selectedScheduleId.value = exact.id
      s = exact
    }
  }

  if (s) {
    rescheduleForm.value.originalDate = s.scheduledDate
    rescheduleForm.value.originalTime = `${s.startTime}-${s.endTime}`
    timeConflictResult.value = null
    await loadTeacherAvailableTime()
  } else {
    rescheduleForm.value.originalDate = ''
    rescheduleForm.value.originalTime = ''
  }
})


// 教师可用时间
const teacherAvailableTimeSlots = ref<TeacherAvailableTime[]>([])

// 本月剩余调课次数（来自用户资料）
const userAdjustmentTimes = ref<number | null>(null)
const studentOverQuota = computed(() => !props.isTeacher && (userAdjustmentTimes.value === null ? false : (userAdjustmentTimes.value <= 0)))

// 日历选择（学生/教师）
// 计算用于日历查询的教师ID（优先取所选节次的教师ID）
const calendarTeacherId = computed(() => (selectedSchedule.value?.teacherId || props.course?.teacherId || 0))

const studentSchedulerRef = ref<any>(null)
const candidateSessions = ref<Array<{ date: string; startTime: string; endTime: string }>>([])

const submitDisabled = computed(() => {
  const s = selectedSchedule.value
  if (!s || !isScheduleAdjustable(s)) return true
  const reasonOk = !!rescheduleForm.value.reason.trim()
  const hasSingle = !!(rescheduleForm.value.newDate && rescheduleForm.value.newTime)
  // 教师端：可多选候选，只要有候选且填写了原因即可提交
  if (props.isTeacher && candidateSessions.value.length > 0) return !reasonOk
  return !(hasSingle && reasonOk)
})

const openCalendar = () => {
  if (!calendarTeacherId.value || Number(calendarTeacherId.value) <= 0) {
    ElMessage.error('未获取到授课教师，无法打开日历')
    return
  }
  const s = selectedSchedule.value
  const defaultDuration = (s?.durationMinutes === 120 ? 120 : 90) as 90 | 120
  const today = new Date()
  const startStr = today.toISOString().slice(0, 10)
  let endStr: string | undefined = undefined
  if (!props.isTeacher) {
    const end = new Date(today)
    end.setDate(end.getDate() + 13)
    endStr = end.toISOString().slice(0, 10)
  }
  studentSchedulerRef.value?.open({ defaultDuration, dateStart: startStr, dateEnd: endStr })
}

const onCalendarConfirm = (sessions: Array<{ date: string; startTime: string; endTime: string }>, _duration: 90 | 120) => {
  if (!props.isTeacher) {
    if (!sessions || sessions.length === 0) return
    const first = sessions[0]
    rescheduleForm.value.newDate = first.date
    rescheduleForm.value.newTime = `${first.startTime}-${first.endTime}`
    if (sessions.length > 1) {
      ElMessage.info('学生端仅支持单次调课，已为你选取第一个时间')
    } else {
      ElMessage.success('已选择新的上课时间')
    }
    // 学生端：选择完成后触发一次冲突检查
    void checkRescheduleTimeConflict()
  } else {
    // 教师端：支持多选候选
    candidateSessions.value = Array.isArray(sessions) ? [...sessions] : []
    if (candidateSessions.value.length > 0) {
      const first = candidateSessions.value[0]
      rescheduleForm.value.newDate = first.date
      rescheduleForm.value.newTime = `${first.startTime}-${first.endTime}`
    }
    ElMessage.success(`已选择候选时间 ${candidateSessions.value.length} 个`)
    // 教师端候选由管理员最终选择，当前不强制做冲突校验
  }
}

// 时间冲突检查状态
const timeConflictChecking = ref(false)
const timeConflictResult = ref<TimeConflictResult | null>(null)

// 可用时间段 - 固定为6个系统上课时间（与学生端保持一致）
const availableTimeSlots = ref<string[]>([
  '08:00-10:00', '10:00-12:00', '13:00-15:00',
  '15:00-17:00', '17:00-19:00', '19:00-21:00'
])

// 根据课程时长动态生成可用时间段（与学生端逻辑完全一致）
const getAvailableTimeSlotsByDuration = (durationMinutes?: number): string[] => {
  if (!durationMinutes) {
    // 如果没有时长信息，默认返回2小时时间段
    return availableTimeSlots.value
  }

  if (durationMinutes === 90) {
    // 1.5小时：只显示每两小时时间段的中间1.5小时，如8:00-10:00只显示8:15-9:45
    const baseTimeSlots = availableTimeSlots.value
    const middleSlots: string[] = []

    baseTimeSlots.forEach(baseSlot => {
      const [startTime, endTime] = baseSlot.split('-')
      const [startHour, startMinute] = startTime.split(':').map(Number)
      const [endHour, endMinute] = endTime.split(':').map(Number)

      // 计算中间1.5小时的时间段（开始时间+15分钟，结束时间-15分钟）
      const middleStartHour = startHour
      const middleStartMinute = startMinute + 15
      const middleEndHour = endHour
      const middleEndMinute = endMinute - 15

      // 处理分钟进位
      let finalStartHour = middleStartHour
      let finalStartMinute = middleStartMinute
      if (finalStartMinute >= 60) {
        finalStartHour += 1
        finalStartMinute -= 60
      }

      let finalEndHour = middleEndHour
      let finalEndMinute = middleEndMinute
      if (finalEndMinute < 0) {
        finalEndHour -= 1
        finalEndMinute += 60
      }

      // 格式化时间并添加到结果中
      const formattedStartTime = `${finalStartHour.toString().padStart(2, '0')}:${finalStartMinute.toString().padStart(2, '0')}`
      const formattedEndTime = `${finalEndHour.toString().padStart(2, '0')}:${finalEndMinute.toString().padStart(2, '0')}`

      middleSlots.push(`${formattedStartTime}-${formattedEndTime}`)
    })

    return middleSlots
  } else if (durationMinutes === 120) {
    // 2小时：使用固定的时间段，维持现状不变
    return availableTimeSlots.value
  }

  // 默认返回2小时时间段
  return availableTimeSlots.value
}

// 获取当前调课可用的时间段（根据所选节次的课程时长动态生成）
const getCurrentRescheduleTimeSlots = computed(() => {
  const s = selectedSchedule.value
  return getAvailableTimeSlotsByDuration(s?.durationMinutes)
})

// 监听弹窗显示状态
watch(() => props.modelValue, async (newVal: boolean) => {
  if (newVal && props.course) {
    await initializeReschedule()
    await loadCurrentUserAdjustmentTimes()
  }
})

// 初始化调课
const initializeReschedule = async () => {
  if (!props.course) return

  // 默认选中最近的一节未来课程
  if (futureSchedules.value.length > 0) {
    const first = futureSchedules.value.find(s => isScheduleAdjustable(s))
    selectedScheduleId.value = first ? first.id : null
  } else {
    selectedScheduleId.value = null
  }

  const s = selectedSchedule.value

  // 重置表单并设置原定课程信息
  rescheduleForm.value = {
    originalDate: s ? s.scheduledDate : '',
    originalTime: s ? `${s.startTime}-${s.endTime}` : '',
    newDate: '',
    newTime: '',
    reason: ''
  }

  // 加载教师可用时间
  if (s) {
    await loadTeacherAvailableTime()
  }

  // 重置时间冲突检查结果
  timeConflictResult.value = null
}

// 加载当前用户本月可调课次数
const loadCurrentUserAdjustmentTimes = async () => {
  try {
    if (props.isTeacher) {
      const p = await teacherAPI.getProfile()
      if (p?.success && p?.data?.adjustmentTimes !== undefined) {
        userAdjustmentTimes.value = Number(p.data.adjustmentTimes)
      }
    } else {
      const p = await studentAPI.getProfile()
      if (p?.success && p?.data?.adjustmentTimes !== undefined) {
        userAdjustmentTimes.value = Number(p.data.adjustmentTimes)
      }
    }
  } catch {
    // 忽略读取失败，不阻塞弹窗
    userAdjustmentTimes.value = userAdjustmentTimes.value ?? null
  }
}

// 加载教师可用时间（周模板已移除，按日历选择）
const loadTeacherAvailableTime = async () => {
  console.info('[RescheduleModal] 已切换按日历调课，周模板可用时间已移除')
  teacherAvailableTimeSlots.value = []
}



// 判断是否为过去日期（禁止选择今天之前）
const isPastDate = (dateStr: string): boolean => {
  const target = new Date(dateStr)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return target < today
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

// 判断某节次是否满足“距离当前时间≥4小时”
const isScheduleAdjustable = (s: CourseSchedule): boolean => {
  try {
    const startRaw = s.startTime
    const start = startRaw.length === 5 ? `${startRaw}:00` : startRaw
    const originalStart = new Date(`${s.scheduledDate}T${start}`)
    if (isNaN(originalStart.getTime())) return false
    const nowPlus4 = new Date(Date.now() + 4 * 60 * 60 * 1000)
    return originalStart.getTime() > nowPlus4.getTime()
  } catch {
    return false
  }
}


// 检查调课时间冲突
const checkRescheduleTimeConflict = async () => {
  if (!rescheduleForm.value.newDate || !rescheduleForm.value.newTime) {
    timeConflictResult.value = null
    return
  }

  console.log('开始检查时间冲突，课程数据:', props.course)

  const s = selectedSchedule.value
  if (!s) {
    console.error('无可选择的未来课程')
    timeConflictResult.value = { hasConflict: false, message: '无可调整的未来课程' }
    return
  }

  try {
    timeConflictChecking.value = true

    // 若选中为占位节次，尝试在当前课程内映射到真实节次
    let scheduleId = s.id
    if (!scheduleId || Number(scheduleId) <= 0) {
      if (Array.isArray(props.course?.schedules)) {
        const merged = [
          ...((props.course!.schedules! as CourseSchedule[]) || []),
          ...((props.allSchedules || []) as CourseSchedule[])
        ]
        const seen = new Set<number>()
        const realCandidates = merged.filter((x: CourseSchedule) => {
          const ok = x.id && Number(x.id) > 0 && !seen.has(Number(x.id))
          if (ok) seen.add(Number(x.id))
          return ok
        })
        const norm = (t: string | undefined | null) => {
          if (!t) return ''
          return t.length === 5 ? `${t}:00` : t
        }
        const exact = realCandidates.find((x: CourseSchedule) =>
          x.scheduledDate === s!.scheduledDate &&
          norm(x.startTime) === norm(s!.startTime) &&
          norm(x.endTime) === norm(s!.endTime) &&
          (!s!.teacherId || x.teacherId === s!.teacherId)
        )
        if (exact) {
          // 确保该真实节次出现在选项中
          if (Array.isArray(props.course?.schedules)) {
            const exists = (props.course!.schedules! as CourseSchedule[]).some(x => Number(x.id) === Number(exact.id))
            if (!exists) {
              (props.course!.schedules! as CourseSchedule[]).push(exact as CourseSchedule)
            }
          }
          selectedScheduleId.value = exact.id
          scheduleId = exact.id
        }
      }
    }

    if (!scheduleId || Number(scheduleId) <= 0) {
      timeConflictResult.value = { hasConflict: false, message: '未找到对应的真实课表，请刷新后再试' }
      return
    }

    // 解析时间段
    const [startTime, endTime] = rescheduleForm.value.newTime.split('-')

    if (!startTime || !endTime) {
      console.error('时间段格式错误:', rescheduleForm.value.newTime)
      timeConflictResult.value = { hasConflict: false, message: '时间段格式错误' }
      return
    }

    console.log('检查时间冲突参数:', {
      scheduleId,
      newDate: rescheduleForm.value.newDate,
      startTime,
      endTime,
      selected: s
    })

    // 教师/学生分别调用各自的API，避免权限冲突
    const apiCall = props.isTeacher ? rescheduleAPI.checkTeacherTimeConflict : rescheduleAPI.checkTimeConflict

    const result = await apiCall(
      Number(scheduleId),
      rescheduleForm.value.newDate,
      startTime,
      endTime
    )

    console.log('时间冲突检查API返回结果:', result)

    if (result.success) {
      timeConflictResult.value = {
        hasConflict: result.data.hasConflict,
        conflictType: result.data.conflictType,
        message: result.data.message || '检查完成'
      }
    } else {
      console.error('API返回失败:', result)
      timeConflictResult.value = { hasConflict: false, message: result.message || '检查失败，请重试' }
    }
  } catch (error) {
    console.error('检查时间冲突失败:', error)
    timeConflictResult.value = { hasConflict: false, message: '网络错误，请重试' }
  } finally {
    timeConflictChecking.value = false
  }
}


// 找到1.5小时时间段对应的基础2小时时间段
const findBaseTimeSlotFor90Min = (timeSlot: string): string | null => {
  const [startTime] = timeSlot.split('-')
  const [startHour, startMinute] = startTime.split(':').map(Number)

  // 获取基础2小时时间段
  const baseTimeSlots = availableTimeSlots.value

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
  void timeSlot
  // 仅保留单次调课，已不涉及多选时间段的区间冲突
  return false
}

// 检查时间段是否可选择（用于禁用不可用的时间段）
const isTimeSlotSelectable = (timeSlot: string): boolean => {
  if (teacherAvailableTimeSlots.value.length === 0) {
    // 如果教师未设置可用时间，默认所有时间可选
    return true
  }

  // 检查是否与已选择的时间段冲突（同一区间内）
  if (isTimeSlotConflictWithSelected(timeSlot)) {
    return false
  }

  // 获取当前调课课程的时长信息
  const currentDuration = props.course?.schedules?.[0]?.durationMinutes

  // 如果当前时长是90分钟（1.5小时），需要映射到基础2小时时间段
  if (currentDuration === 90) {
    const baseTimeSlot = findBaseTimeSlotFor90Min(timeSlot)
    if (!baseTimeSlot) {
      return false
    }
    // 使用基础时间段来检查是否有任何星期几可用
    const isAvailable = teacherAvailableTimeSlots.value.some(daySlot =>
      daySlot.timeSlots && daySlot.timeSlots.includes(baseTimeSlot)
    )
    return isAvailable
  }

  // 2小时课程，直接检查
  const isAvailable = teacherAvailableTimeSlots.value.some(daySlot =>
    daySlot.timeSlots && daySlot.timeSlots.includes(timeSlot)
  )
  return isAvailable
}



// 提交调课申请
const submitReschedule = async () => {
  if (!props.course) return

  // 验证表单
  if (props.isTeacher) {
    // 教师端：允许多选候选；若无候选，则必须提供单个新时间
    if (!rescheduleForm.value.originalDate || !rescheduleForm.value.originalTime) {
      ElMessage.warning('请填写完整的调课信息')
      return
    }
    if (candidateSessions.value.length === 0 && (!rescheduleForm.value.newDate || !rescheduleForm.value.newTime)) {
      ElMessage.warning('请选择候选时间或填写一个新的上课时间')
      return
    }
  } else {
    // 学生端：必须提供单个新时间
    if (!rescheduleForm.value.originalDate || !rescheduleForm.value.originalTime ||
        !rescheduleForm.value.newDate || !rescheduleForm.value.newTime) {
      ElMessage.warning('请填写完整的调课信息')
      return
    }
  }

  // 检查4小时限制（仅针对原定课程开始时间）
  if (!isAtLeastFourHoursBeforeOriginal()) {
    ElMessage.error('调课需在开课前4小时之外发起')
    return
  }
  // 新的上课日期不得早于今天（仅在单选场景校验；教师多候选由管理员选取时再校验）
  if (!props.isTeacher || candidateSessions.value.length === 0) {
    if (isPastDate(rescheduleForm.value.newDate)) {
      ElMessage.error('新的上课日期不能早于今天')
      return
    }

    // 检查时间冲突（单选场景校验）
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
  }

  if (!rescheduleForm.value.reason.trim()) {
    ElMessage.warning('请填写调课原因')
    return
  }

  try {
    // 使用用户选择的未来节次ID
    const s = selectedSchedule.value

    if (!s) {
      ElMessage.error('没有找到可调课的课程安排')
      return
    }

    // 构建调课申请数据（学生单选 / 教师可多选候选）
    const requestData: any = {
      scheduleId: s.id,
      requestType: 'reschedule' as const,
      reason: rescheduleForm.value.reason,
      urgencyLevel: 'medium' as const
    }
    if (rescheduleForm.value.newDate && rescheduleForm.value.newTime) {
      requestData.newDate = rescheduleForm.value.newDate
      requestData.newStartTime = rescheduleForm.value.newTime.split('-')[0]
      requestData.newEndTime = rescheduleForm.value.newTime.split('-')[1]
    }

    if (props.isTeacher && candidateSessions.value.length > 0) {
      requestData.candidateSessions = candidateSessions.value
    }

    // 根据用户类型选择API
    const apiCall = props.isTeacher ? rescheduleAPI.createTeacherRequest : rescheduleAPI.createRequest

    // 学生端在提交前额外校验一次后端 canApply（用于余额不足等前置阻断）
    if (!props.isTeacher) {
      const can = await rescheduleAPI.canApply(s.id)
      if (can && can.success === true && can.data === false) {
        ElMessage.error('当前无法申请调课（可能因余额不足或时间限制）')
        return
      }
    }

    const result = await apiCall(requestData)

    if (result.success) {
      const successMessage = props.isTeacher ? '调课申请已提交，等待管理员确认' : '调课申请已提交，等待管理员确认'
      ElMessage.success(successMessage)
      updateVisible(false)
      emit('success')
    } else {
      ElMessage.error(result.message || '提交调课申请失败')
    }
  } catch (error) {
    console.error('提交调课申请失败:', error)
    ElMessage.error('提交调课申请失败，请稍后重试')
  }
}

// 关闭弹窗
const handleClose = () => {
  updateVisible(false)
  // 重置表单
  rescheduleForm.value = {
    originalDate: '',
    originalTime: '',
    newDate: '',
    newTime: '',
    reason: ''
  }
  timeConflictResult.value = null
}


</script>

<style scoped>
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

.schedule-note {
  margin-top: 4px;
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

.time-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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

/* 响应式调整 */
@media (max-width: 768px) {
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
}
</style>
