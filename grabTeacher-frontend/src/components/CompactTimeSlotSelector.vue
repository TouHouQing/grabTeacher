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

// 时间段选项 - 固定为6个系统上课时间
const timeSlots = [
  { value: '08:00-10:00', display: '08:00-10:00', period: '' },
  { value: '10:00-12:00', display: '10:00-12:00', period: '' },
  { value: '13:00-15:00', display: '13:00-15:00', period: '' },
  { value: '15:00-17:00', display: '15:00-17:00', period: '' },
  { value: '17:00-19:00', display: '17:00-19:00', period: '' },
  { value: '19:00-21:00', display: '19:00-21:00', period: '' }
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

// 重置内部状态
const resetWeekdays = () => {
  weekdays.value.forEach(w => {
    w.selected = false
    w.selectedSlots = []
  })
  selectedWeekday.value = null
}

// 初始化/回显数据（确保不残留上一次编辑痕迹）
const initializeData = () => {
  const prevSelected = selectedWeekday.value?.value ?? null
  resetWeekdays()

  const mv = Array.isArray(props.modelValue) ? props.modelValue : []
  if (mv.length > 0) {
    mv.forEach(item => {
      const weekday = weekdays.value.find(w => w.value === item.weekday)
      if (weekday) {
        weekday.selected = true
        const slots = Array.isArray(item.timeSlots) ? item.timeSlots.filter(s => typeof s === 'string') : []
        // 去重，避免出现重复时段
        weekday.selectedSlots = Array.from(new Set(slots))
      }
    })
  }

  // 恢复先前选中的星期几，尽量保持用户操作上下文
  if (prevSelected) {
    const found = weekdays.value.find(w => w.value === prevSelected && w.selected)
    selectedWeekday.value = found || (selectedWeekdays.value[0] || null)
  } else {
    selectedWeekday.value = selectedWeekdays.value[0] || null
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
      weekday.selectedSlots = ['17:00-19:00', '19:00-21:00']
    }
  })
  emitChange()
}

// 快速选择周末
const selectWeekends = () => {
  weekdays.value.forEach(weekday => {
    if (weekday.value === 6 || weekday.value === 7) {
      weekday.selected = true
      weekday.selectedSlots = ['08:00-10:00', '10:00-12:00', '13:00-15:00', '15:00-17:00']
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
    .filter(weekday => weekday.selected)
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
