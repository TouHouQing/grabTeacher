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
          description="单次调课须在开课前4小时之外发起，老师确认后生效。剩余课时不变。"
          type="info"
          show-icon
          :closable="false"
        />
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

      <div class="reschedule-type-selection">
        <el-radio-group v-model="rescheduleType" @change="switchRescheduleType">
          <el-radio-button :value="'single'">
            <el-icon><Calendar /></el-icon>
            单次调课
          </el-radio-button>
          <el-radio-button :value="'recurring'">
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
                :disabled-date="(date) => {
                  const d = new Date(date)
                  d.setHours(0,0,0,0)
                  const today = new Date()
                  today.setHours(0,0,0,0)
                  return d < today
                }"
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
                  v-for="time in getCurrentRescheduleTimeSlots"
                  :key="time"
                  :label="time"
                  :value="time"
                  :disabled="!isTimeSlotSelectable(time)"
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
                v-for="time in getCurrentRescheduleTimeSlots"
                :key="time"
                :class="[
                  'time-slot-card',
                  {
                    'selected': rescheduleForm.selectedTimeSlots.includes(time),
                    'disabled': !isTimeSlotSelectable(time) && !rescheduleForm.selectedTimeSlots.includes(time),
                    'conflicted': rescheduleForm.selectedTimeSlots.includes(time) && !isTimeSlotSelectable(time)
                  }
                ]"
                @click="() => {
                  // 如果时间段已经被选中，允许取消选择
                  if (rescheduleForm.selectedTimeSlots.includes(time)) {
                    rescheduleForm.selectedTimeSlots = rescheduleForm.selectedTimeSlots.filter(t => t !== time)
                    // 清空不再可用的星期选择
                    rescheduleForm.selectedWeekdays = rescheduleForm.selectedWeekdays.filter(weekday =>
                      isWeekdaySelectable(weekday)
                    )
                    return
                  }

                  // 如果时间段不可选择且未被选中，不允许选择
                  if (!isTimeSlotSelectable(time)) {
                    return
                  }

                  // 添加新的时间段
                  rescheduleForm.selectedTimeSlots.push(time)
                }"
              >
                <div class="slot-time">{{ time }}</div>
                <div v-if="!isTimeSlotSelectable(time) && !rescheduleForm.selectedTimeSlots.includes(time)" class="unavailable-overlay">
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
        <el-button @click="handleClose">取消</el-button>
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
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Timer, Check, Loading } from '@element-plus/icons-vue'
import { rescheduleAPI, teacherAPI } from '../utils/api'

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

// 教师可用时间
const teacherAvailableTimeSlots = ref<TeacherAvailableTime[]>([])

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
    // 1.5小时：在2小时区间内选择开始时间，但确保每个时间段都是完整的1.5小时
    // 时间限制：七点最多到:30，不能有:45
    const baseTimeSlots = availableTimeSlots.value
    const flexibleSlots: string[] = []

    baseTimeSlots.forEach(baseSlot => {
      const [startTime] = baseSlot.split('-')
      const [startHour, startMinute] = startTime.split(':').map(Number)

      // 在2小时区间内，每15分钟一个开始时间选项，但限制时间格式
      for (let minute = 0; minute < 120; minute += 15) {
        const newStartHour = startHour + Math.floor(minute / 60)
        const newStartMinute = (startMinute + minute) % 60

        // 时间限制：七点最多到:30，不能有:45
        if (newStartHour === 7 && newStartMinute > 30) {
          continue // 跳过7点超过30分的时间
        }
        if (newStartMinute === 45) {
          continue // 跳过所有:45的时间
        }

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
          flexibleSlots.push(`${newStartTime}-${endTime}`)
        }
      }
    })

    return flexibleSlots
  } else if (durationMinutes === 120) {
    // 2小时：使用固定的时间段，维持现状不变
    return availableTimeSlots.value
  }

  // 默认返回2小时时间段
  return availableTimeSlots.value
}

// 获取当前调课可用的时间段（根据课程时长动态生成）
const getCurrentRescheduleTimeSlots = computed(() => {
  if (!props.course?.schedules || props.course.schedules.length === 0) {
    return availableTimeSlots.value
  }

  // 获取第一个课程安排的时长信息
  const firstSchedule = props.course.schedules[0]
  const durationMinutes = firstSchedule.durationMinutes

  return getAvailableTimeSlotsByDuration(durationMinutes)
})

// 监听弹窗显示状态
watch(() => props.modelValue, async (newVal) => {
  if (newVal && props.course) {
    await initializeReschedule()
  }
})

// 初始化调课
const initializeReschedule = async () => {
  if (!props.course) return

  rescheduleType.value = 'single'

  // 找到下次课程（最近的未来课程）
  const nextSchedule = getNextSchedule(props.course.schedules)

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

  // 重置时间冲突检查结果
  timeConflictResult.value = null
}

// 加载教师可用时间
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
      const nextSchedule = getNextSchedule(props.course?.schedules)
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

// 检查调课时间冲突
const checkRescheduleTimeConflict = async () => {
  if (!rescheduleForm.value.newDate || !rescheduleForm.value.newTime) {
    timeConflictResult.value = null
    return
  }

  console.log('开始检查时间冲突，课程数据:', props.course)

  const nextSchedule = getNextSchedule(props.course?.schedules)
  if (!nextSchedule) {
    console.error('无法获取下一节课信息')
    timeConflictResult.value = { hasConflict: false, message: '无法获取课程信息' }
    return
  }

  try {
    timeConflictChecking.value = true

    // 解析时间段
    const [startTime, endTime] = rescheduleForm.value.newTime.split('-')

    if (!startTime || !endTime) {
      console.error('时间段格式错误:', rescheduleForm.value.newTime)
      timeConflictResult.value = { hasConflict: false, message: '时间段格式错误' }
      return
    }

    console.log('检查时间冲突参数:', {
      scheduleId: nextSchedule.id,
      newDate: rescheduleForm.value.newDate,
      startTime,
      endTime,
      nextSchedule
    })

    // 教师/学生分别调用各自的API，避免权限冲突
    const apiCall = props.isTeacher ? rescheduleAPI.checkTeacherTimeConflict : rescheduleAPI.checkTimeConflict

    const result = await apiCall(
      nextSchedule.id,
      rescheduleForm.value.newDate,
      startTime,
      endTime
    )

    console.log('时间冲突检查API返回结果:', result)

    if (result.success) {
      timeConflictResult.value = {
        hasConflict: result.data.hasConflict,
        conflictType: result.data.conflictType,
        message: result.data.message
 || '检查完成'
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

// 获取星期几的中文名称（统一使用后端格式：1-7）
const getWeekdayName = (weekday: number): string => {
  const names = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return names[weekday] || '未知'
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
  const currentDuration = props.course?.schedules?.[0]?.durationMinutes

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

// 检查时间段是否与已选择的时间段冲突（在同一基础区间内）
const isTimeSlotConflictWithSelected = (timeSlot: string): boolean => {
  const currentDuration = props.course?.schedules?.[0]?.durationMinutes

  // 只有1.5小时课程需要检查区间冲突
  if (currentDuration !== 90) {
    return false
  }

  // 如果当前时间段已经被选中，则不算冲突（允许取消选择）
  if (rescheduleForm.value.selectedTimeSlots.includes(timeSlot)) {
    return false
  }

  // 找到当前时间段对应的基础区间
  const currentBaseSlot = findBaseTimeSlotFor90Min(timeSlot)
  if (!currentBaseSlot) {
    return false
  }

  // 检查已选择的时间段中是否有与当前时间段在同一基础区间的
  return rescheduleForm.value.selectedTimeSlots.some(selectedSlot => {
    const selectedBaseSlot = findBaseTimeSlotFor90Min(selectedSlot)
    return selectedBaseSlot === currentBaseSlot
  })
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
const getNextSchedule = (schedules?: CourseSchedule[]): CourseSchedule | undefined => {
  if (!schedules) return undefined

  const nextSchedule = schedules.find(s =>
    new Date(s.scheduledDate) > new Date() && s.status === 'progressing'
  )

  console.log('获取下一节课:', {
    schedules,
    nextSchedule,
    currentDate: new Date()
  })

  return nextSchedule
}

// 提交调课申请
const submitReschedule = async () => {
  if (!props.course) return

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

    // 验证时间格式
    if (!validateTimeFormat(rescheduleForm.value.selectedWeekdays, rescheduleForm.value.selectedTimeSlots)) {
      ElMessage.error('时间格式无效，请重新选择')
      return
    }
  }

  if (!rescheduleForm.value.reason.trim()) {
    ElMessage.warning('请填写调课原因')
    return
  }

  try {
    // 获取最近的课程安排ID（用于调课申请）
    const nextSchedule = getNextSchedule(props.course.schedules)

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
      // 周期性调课：构建更规范的数据格式
      const weekdays = rescheduleForm.value.selectedWeekdays.sort((a, b) => a - b)
      const timeSlots = rescheduleForm.value.selectedTimeSlots.sort()

      console.log('周期性调课数据:', { weekdays, timeSlots });

      Object.assign(requestData, {
        newRecurringWeekdays: weekdays,
        newRecurringTimeSlots: timeSlots,
        // 添加格式化的每周安排字符串，便于后端处理和显示
        newWeeklySchedule: formatWeeklyScheduleString(weekdays, timeSlots)
      })
    }

    // 根据用户类型选择API
    const apiCall = props.isTeacher ? rescheduleAPI.createTeacherRequest : rescheduleAPI.createRequest

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
    selectedWeekdays: [],
    selectedTimeSlots: [],
    reason: '',
    type: 'single'
  }
  timeConflictResult.value = null
}

// 格式化每周安排字符串
const formatWeeklyScheduleString = (weekdays: number[], timeSlots: string[]): string => {
  if (weekdays.length === 0 || timeSlots.length === 0) {
    return '';
  }

  const weekdayNames = weekdays.map(day => getWeekdayName(day));

  // 如果只有一个时间段，使用多星期单时间段格式
  if (timeSlots.length === 1) {
    return `${weekdayNames.join('、')} ${timeSlots[0]}`;
  }

  // 如果多个时间段，使用单星期多时间段格式
  // 为每个星期几分配所有时间段
  const formattedSlots: string[] = [];

  for (const weekday of weekdays) {
    const weekdayName = getWeekdayName(weekday);
    const timeSlotString = timeSlots.join(',');
    formattedSlots.push(`${weekdayName} ${timeSlotString}`);
  }

  return formattedSlots.join(';');
};

// 验证时间格式（仅用于周期性调课）
const validateTimeFormat = (weekdays: number[], timeSlots: string[]): boolean => {
  if (weekdays.length === 0 || timeSlots.length === 0) {
    return false;
  }

  // 检查每个星期几是否都包含至少一个时间段
  for (const weekday of weekdays) {
    if (!isWeekdaySelectable(weekday)) {
      return false;
    }
  }

  // 检查每个时间段是否都可用
  for (const timeSlot of timeSlots) {
    if (!isTimeSlotSelectable(timeSlot)) {
      return false;
    }
  }

  return true;
};
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
