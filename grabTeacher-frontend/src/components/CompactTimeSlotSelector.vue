<template>
  <div class="elegant-time-selector">
    <div class="selector-header">
      <h4>{{ title }}</h4>
      <div class="quick-actions">
        <el-button @click="selectWorkdays" size="small" type="primary" plain>
          工作日晚上
        </el-button>
        <el-button @click="selectWeekends" size="small" type="success" plain>
          周末全天
        </el-button>
        <el-button @click="clearAll" size="small" plain>
          清空
        </el-button>
      </div>
    </div>

    <!-- 星期几选择 -->
    <div class="weekdays-section">
      <div class="weekdays-grid">
        <div
          v-for="weekday in weekdays"
          :key="weekday.value"
          class="weekday-card"
          :class="{ 'selected': weekday.selected }"
          @click="toggleWeekday(weekday)"
        >
          <div class="weekday-name">{{ weekday.label }}</div>
          <div class="weekday-count">{{ weekday.selectedSlots.length }}</div>
        </div>
      </div>
    </div>

    <!-- 时间段选择 -->
    <div v-if="selectedWeekday" class="time-slots-section">
      <div class="section-title">
        选择 {{ selectedWeekday.label }} 的上课时间
      </div>
      <div class="time-grid">
        <div
          v-for="slot in timeSlots"
          :key="slot.value"
          class="time-slot"
          :class="{ 'selected': selectedWeekday.selectedSlots.includes(slot.value) }"
          @click="toggleTimeSlot(slot.value)"
        >
          <div class="time-display">{{ slot.display }}</div>
          <div class="time-period">{{ slot.period }}</div>
        </div>
      </div>
    </div>

    <!-- 总结 -->
    <div class="summary-section">
      <div class="summary-text">
        已选择 <span class="highlight">{{ totalSelectedSlots }}</span> 个时间段
      </div>
      <div v-if="totalSelectedSlots > 0" class="selected-preview">
        <el-tag
          v-for="(weekday, index) in selectedWeekdays"
          :key="index"
          size="small"
          type="primary"
          class="weekday-tag"
        >
          {{ weekday.label }}({{ weekday.selectedSlots.length }})
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

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
}

interface Emits {
  (e: 'update:modelValue', value: TimeSlot[]): void
  (e: 'change', value: TimeSlot[]): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  title: '设置可上课时间'
})

const emit = defineEmits<Emits>()

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

// 完整的时间段选项（9:00-21:00，每小时）
const timeSlots = [
  { value: '09:00-10:00', display: '9:00', period: '' },
  { value: '10:00-11:00', display: '10:00', period: '' },
  { value: '11:00-12:00', display: '11:00', period: '' },
  { value: '12:00-13:00', display: '12:00', period: '' },
  { value: '13:00-14:00', display: '13:00', period: '' },
  { value: '14:00-15:00', display: '14:00', period: '' },
  { value: '15:00-16:00', display: '15:00', period: '' },
  { value: '16:00-17:00', display: '16:00', period: '' },
  { value: '17:00-18:00', display: '17:00', period: '' },
  { value: '18:00-19:00', display: '18:00', period: '' },
  { value: '19:00-20:00', display: '19:00', period: '' },
  { value: '20:00-21:00', display: '20:00', period: '' }
]

// 当前选中的星期几
const selectedWeekday = ref<WeekdayOption | null>(null)

// 计算属性
const selectedWeekdays = computed(() =>
  weekdays.value.filter(w => w.selected && w.selectedSlots.length > 0)
)

const totalSelectedSlots = computed(() =>
  weekdays.value.reduce((total, w) => total + w.selectedSlots.length, 0)
)

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

// 切换星期几选择
const toggleWeekday = (weekday: WeekdayOption) => {
  if (weekday.selected) {
    // 如果已选中，取消选择
    weekday.selected = false
    weekday.selectedSlots = []
    if (selectedWeekday.value === weekday) {
      selectedWeekday.value = null
    }
  } else {
    // 选中该星期几，并显示时间段选择
    weekday.selected = true
    selectedWeekday.value = weekday
  }
  emitChange()
}

// 切换时间段选择
const toggleTimeSlot = (timeSlotValue: string) => {
  if (!selectedWeekday.value) return

  const index = selectedWeekday.value.selectedSlots.indexOf(timeSlotValue)
  if (index > -1) {
    selectedWeekday.value.selectedSlots.splice(index, 1)
  } else {
    selectedWeekday.value.selectedSlots.push(timeSlotValue)
  }
  emitChange()
}

// 快速选择工作日
const selectWorkdays = () => {
  weekdays.value.forEach(weekday => {
    if (weekday.value >= 1 && weekday.value <= 5) {
      weekday.selected = true
      weekday.selectedSlots = ['18:00-18:30', '18:30-19:00', '19:00-19:30', '19:30-20:00', '20:00-20:30', '20:30-21:00']
    }
  })
  emitChange()
}

// 快速选择周末
const selectWeekends = () => {
  weekdays.value.forEach(weekday => {
    if (weekday.value === 6 || weekday.value === 7) {
      weekday.selected = true
      weekday.selectedSlots = ['09:00-09:30', '09:30-10:00', '10:00-10:30', '10:30-11:00', '14:00-14:30', '14:30-15:00', '15:00-15:30', '15:30-16:00']
    }
  })
  emitChange()
}

// 清空所有
const clearAll = () => {
  weekdays.value.forEach(weekday => {
    weekday.selected = false
    weekday.selectedSlots = []
  })
  selectedWeekday.value = null
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
</script>

<style scoped>
.elegant-time-selector {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.selector-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.quick-actions {
  display: flex;
  gap: 8px;
}

/* 星期几选择区域 */
.weekdays-section {
  margin-bottom: 24px;
}

.weekdays-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.weekday-card {
  background: white;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  min-height: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.weekday-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.weekday-card.selected {
  border-color: #409eff;
  background: linear-gradient(135deg, #ecf5ff 0%, #e1f3ff 100%);
  color: #409eff;
}

.weekday-name {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.weekday-count {
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  border-radius: 12px;
  padding: 2px 8px;
  display: inline-block;
  min-width: 20px;
}

.weekday-card.selected .weekday-count {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

/* 时间段选择区域 */
.time-slots-section {
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  text-align: center;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.time-slot {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 8px 4px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.time-slot:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.time-slot.selected {
  border-color: #409eff;
  background: linear-gradient(135deg, #409eff 0%, #66b3ff 100%);
  color: white;
}

.time-display {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 2px;
}

.time-period {
  font-size: 10px;
  opacity: 0.8;
}

/* 总结区域 */
.summary-section {
  text-align: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.summary-text {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
}

.highlight {
  color: #409eff;
  font-weight: 600;
  font-size: 16px;
}

.selected-preview {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px;
}

.weekday-tag {
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .selector-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .quick-actions {
    justify-content: center;
  }

  .weekdays-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .time-grid {
    grid-template-columns: repeat(auto-fill, minmax(70px, 1fr));
  }
}
</style>
