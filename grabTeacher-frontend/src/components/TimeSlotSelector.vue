<template>
  <div class="time-slot-selector">
    <div class="selector-header">
      <h3>{{ title }}</h3>
      <p class="description">{{ description }}</p>
    </div>

    <!-- 课程时长选择 -->
    <div class="duration-selection" v-if="showDurationSelection">
      <div class="duration-header">
        <h4>选择课程时长</h4>
        <p class="duration-tip">请选择您希望的课程时长，系统将显示对应的时间段选项</p>
      </div>

      <el-radio-group v-model="selectedDuration" @change="onDurationChange" class="duration-radio-group">
        <el-radio :label="90" class="duration-radio">
          <div class="duration-option">
            <div class="duration-label">1.5小时（90分钟）</div>
            <div class="duration-desc">可选择开始时间，如8:00、8:15、8:30等</div>
          </div>
        </el-radio>
        <el-radio :label="120" class="duration-radio">
          <div class="duration-option">
            <div class="duration-label">2小时（120分钟）</div>
            <div class="duration-desc">固定时间段，如8:00-10:00、10:00-12:00等</div>
          </div>
        </el-radio>
      </el-radio-group>
    </div>

    <div class="weekdays-container" v-if="selectedDuration">
      <div
        v-for="weekday in weekdays"
        :key="weekday.value"
        class="weekday-section"
      >
        <div class="weekday-header">
          <el-checkbox
            v-model="weekday.selected"
            @change="onWeekdayChange(weekday)"
            class="weekday-checkbox"
          >
            {{ weekday.label }}
          </el-checkbox>
        </div>

        <div v-if="weekday.selected" class="time-slots-grid">
          <el-checkbox-group
            v-model="weekday.selectedSlots"
            @change="onTimeSlotsChange"
            class="time-slots-group"
          >
            <el-checkbox
              v-for="slot in availableTimeSlots"
              :key="slot"
              :label="slot"
              class="time-slot-checkbox"
            >
              {{ slot }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
      </div>
    </div>

    <div class="summary-section" v-if="selectedDuration">
      <div class="summary-header">
        <h4>已选择的时间安排</h4>
        <el-button @click="clearAll" size="small" type="danger" plain>
          清空所有
        </el-button>
      </div>

      <div class="selected-summary">
        <div v-if="selectedSummary.length === 0" class="no-selection">
          暂未选择任何时间
        </div>
        <div v-else class="summary-list">
          <el-tag
            v-for="(item, index) in selectedSummary"
            :key="index"
            closable
            @close="removeTimeSlot(item)"
            class="summary-tag"
          >
            {{ item.weekdayName }} {{ item.timeSlot }}
          </el-tag>
        </div>
      </div>

      <div class="summary-stats">
        <span class="stat-item">
          总计: {{ totalSelectedSlots }} 个时间段
        </span>
        <span class="stat-item">
          工作日: {{ weekdaySlots }} 个
        </span>
        <span class="stat-item">
          周末: {{ weekendSlots }} 个
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

interface TimeSlot {
  weekday: number
  timeSlots: string[]
}

interface WeekdayOption {
  value: number
  label: string
  selected: boolean
  selectedSlots: string[]
}

interface Props {
  modelValue?: TimeSlot[]
  title?: string
  description?: string
  maxSlots?: number
  showDurationSelection?: boolean
  defaultDuration?: number
}

interface Emits {
  (e: 'update:modelValue', value: TimeSlot[]): void
  (e: 'change', value: TimeSlot[]): void
  (e: 'durationChange', duration: number): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  title: '选择可上课时间',
  description: '请选择您可以上课的时间段',
  maxSlots: 50,
  showDurationSelection: true,
  defaultDuration: 90
})

const emit = defineEmits<Emits>()

// 选中的课程时长
const selectedDuration = ref<number>(props.defaultDuration)

// 星期几选项
const weekdays = ref<WeekdayOption[]>([
  { value: 1, label: '周一', selected: false, selectedSlots: [] },
  { value: 2, label: '周二', selected: false, selectedSlots: [] },
  { value: 3, label: '周三', selected: false, selectedSlots: [] },
  { value: 4, label: '周四', selected: false, selectedSlots: [] },
  { value: 5, label: '周五', selected: false, selectedSlots: [] },
  { value: 6, label: '周六', selected: false, selectedSlots: [] },
  { value: 7, label: '周日', selected: false, selectedSlots: [] }
])

// 根据课程时长生成可用时间段
const availableTimeSlots = computed(() => {
  if (selectedDuration.value === 90) {
    // 1.5小时：在固定时间段基础上，可选择开始时间
    return generateFlexibleTimeSlots()
  } else if (selectedDuration.value === 120) {
    // 2小时：固定时间段
    return generateFixedTimeSlots()
  }
  return []
})

// 生成固定时间段（2小时）
const generateFixedTimeSlots = (): string[] => {
  return [
    '08:00-10:00', '10:00-12:00', '13:00-15:00',
    '15:00-17:00', '17:00-19:00', '19:00-21:00'
  ]
}

// 生成灵活时间段（1.5小时）
const generateFlexibleTimeSlots = (): string[] => {
  const slots: string[] = []

  // 从8:00开始，每15分钟一个时间段，到20:00结束
  for (let hour = 8; hour <= 20; hour++) {
    for (let minute = 0; minute < 60; minute += 15) {
      const startTime = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`
      const endHour = hour + Math.floor((minute + 90) / 60)
      const endMinute = (minute + 90) % 60
      const endTime = `${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}`

      // 检查结束时间是否超过20:00
      if (endHour < 20 || (endHour === 20 && endMinute === 0)) {
        slots.push(`${startTime}-${endTime}`)
      }
    }
  }

  return slots
}

// 初始化数据
const initializeData = () => {
  if (props.modelValue && props.modelValue.length > 0) {
    weekdays.value.forEach(weekday => {
      const found = props.modelValue.find(item => item.weekday === weekday.value)
      if (found) {
        weekday.selected = true
        weekday.selectedSlots = [...found.timeSlots]
      }
    })
  }
}

// 监听props变化
watch(() => props.modelValue, () => {
  initializeData()
}, { immediate: true, deep: true })

// 课程时长变化
const onDurationChange = () => {
  // 清空之前的选择
  weekdays.value.forEach(weekday => {
    weekday.selected = false
    weekday.selectedSlots = []
  })

  emit('durationChange', selectedDuration.value)
  emitChange()
}

// 星期几选择变化
const onWeekdayChange = (weekday: WeekdayOption) => {
  if (!weekday.selected) {
    weekday.selectedSlots = []
  }
  emitChange()
}

// 时间段选择变化
const onTimeSlotsChange = () => {
  if (totalSelectedSlots.value > props.maxSlots) {
    ElMessage.warning(`最多只能选择 ${props.maxSlots} 个时间段`)
    return
  }
  emitChange()
}

// 发出变化事件
const emitChange = () => {
  const result: TimeSlot[] = weekdays.value
    .filter(weekday => weekday.selected && weekday.selectedSlots.length > 0)
    .map(weekday => ({
      weekday: weekday.value,
      timeSlots: [...weekday.selectedSlots]
    }))

  emit('update:modelValue', result)
  emit('change', result)
}

// 计算属性
const selectedSummary = computed(() => {
  const summary: Array<{ weekdayName: string; timeSlot: string; weekday: number }> = []

  weekdays.value.forEach(weekday => {
    if (weekday.selected && weekday.selectedSlots.length > 0) {
      weekday.selectedSlots.forEach(slot => {
        summary.push({
          weekdayName: weekday.label,
          timeSlot: slot,
          weekday: weekday.value
        })
      })
    }
  })

  return summary
})

const totalSelectedSlots = computed(() => selectedSummary.value.length)

const weekdaySlots = computed(() =>
  selectedSummary.value.filter(item => item.weekday >= 1 && item.weekday <= 5).length
)

const weekendSlots = computed(() =>
  selectedSummary.value.filter(item => item.weekday === 6 || item.weekday === 7).length
)

// 清空所有选择
const clearAll = () => {
  weekdays.value.forEach(weekday => {
    weekday.selected = false
    weekday.selectedSlots = []
  })
  emitChange()
}

// 移除特定时间段
const removeTimeSlot = (item: { weekdayName: string; timeSlot: string; weekday: number }) => {
  const weekday = weekdays.value.find(w => w.value === item.weekday)
  if (weekday) {
    const index = weekday.selectedSlots.indexOf(item.timeSlot)
    if (index > -1) {
      weekday.selectedSlots.splice(index, 1)
      if (weekday.selectedSlots.length === 0) {
        weekday.selected = false
      }
      emitChange()
    }
  }
}

// 暴露方法给父组件
defineExpose({
  selectedDuration,
  clearAll
})
</script>

<style scoped>
.time-slot-selector {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  background: #fff;
}

.selector-header {
  margin-bottom: 20px;
}

.selector-header h3 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.description {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

/* 课程时长选择样式 */
.duration-selection {
  margin-bottom: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.duration-header {
  margin-bottom: 16px;
}

.duration-header h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.duration-tip {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.duration-radio-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.duration-radio {
  margin: 0;
  padding: 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s;
  background: #fff;
}

.duration-radio:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.duration-radio.is-checked {
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

.weekdays-container {
  margin-bottom: 24px;
}

.weekday-section {
  margin-bottom: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
}

.weekday-header {
  margin-bottom: 12px;
}

.weekday-checkbox {
  font-weight: 600;
  color: #303133;
}

.time-slots-grid {
  margin-left: 24px;
}

.time-slots-group {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}

.time-slot-checkbox {
  margin: 0;
}

.summary-section {
  border-top: 1px solid #e4e7ed;
  padding-top: 16px;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.summary-header h4 {
  margin: 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.selected-summary {
  margin-bottom: 12px;
  min-height: 32px;
}

.no-selection {
  color: #909399;
  font-size: 14px;
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.summary-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-tag {
  margin: 0;
}

.summary-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.stat-item {
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .duration-radio-group {
    gap: 12px;
  }

  .duration-radio {
    padding: 12px;
  }

  .duration-label {
    font-size: 15px;
  }

  .duration-desc {
    font-size: 12px;
  }

  .time-slots-group {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 6px;
  }
}
</style>
