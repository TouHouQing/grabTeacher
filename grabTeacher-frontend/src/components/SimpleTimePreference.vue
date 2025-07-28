<template>
  <div class="flexible-time-preference">
    <!-- 快速选择按钮 -->
    <div class="quick-actions">
      <el-button @click="selectWorkdayEvening" size="small" type="primary" plain>
        工作日晚上
      </el-button>
      <el-button @click="selectWeekendDay" size="small" type="success" plain>
        周末白天
      </el-button>
      <el-button @click="clearAll" size="small" plain>
        清空
      </el-button>
    </div>

    <!-- 星期几选择 -->
    <div class="weekdays-section">

      <div class="weekdays-grid">
        <div
          v-for="day in weekdayData"
          :key="day.value"
          class="weekday-card"
          :class="{ 'active': day.selected }"
          @click="toggleWeekday(day.value)"
        >
          <div class="weekday-name">{{ day.label }}</div>
          <div class="weekday-count">{{ day.timeSlots.length }}</div>
        </div>
      </div>
    </div>

    <!-- 时间段选择 -->
    <div v-if="selectedWeekday" class="timeslots-section">
      <div class="timeslots-header" @click="toggleTimeslotsExpanded">
        <div class="section-title">
          选择 {{ selectedWeekday.label }} 的时间段
          <span v-if="selectedWeekday.timeSlots.length > 0" class="selected-count">
            (已选{{ selectedWeekday.timeSlots.length }}项)
          </span>
        </div>
        <el-icon class="expand-icon" :class="{ 'expanded': timeslotsExpanded }">
          <ArrowDown />
        </el-icon>
      </div>

      <div v-show="timeslotsExpanded" class="timeslots-content">
        <div class="timeslots-grid">
          <div
            v-for="slot in timeSlotOptions"
            :key="slot.value"
            class="timeslot-item"
            :class="{ 'selected': selectedWeekday.timeSlots.includes(slot.value) }"
            @click="toggleTimeSlot(slot.value)"
          >
            <div class="time-display">{{ slot.label }}</div>
            <div class="time-range" v-if="slot.range">{{ slot.range }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 选择总结 -->
    <div class="selection-summary">
      <div class="summary-header" @click="toggleSummaryExpanded">
        <div class="summary-title">
          已选择的时间偏好
          <span v-if="totalSelections > 0" class="selection-count">({{ totalSelections }}项)</span>
        </div>
        <el-icon class="expand-icon" :class="{ 'expanded': summaryExpanded }">
          <ArrowDown />
        </el-icon>
      </div>

      <div v-show="summaryExpanded" class="summary-content">
        <div v-if="totalSelections > 0" class="selected-items">
          <div
            v-for="day in selectedWeekdayData"
            :key="day.value"
            class="selected-day"
          >
            <span class="day-name">{{ day.label }}</span>
            <div class="day-times">
              <el-tag
                v-for="timeSlot in day.timeSlots"
                :key="timeSlot"
                size="small"
                type="primary"
              >
                {{ getTimeSlotLabel(timeSlot) }}
              </el-tag>
            </div>
          </div>
        </div>
        <div v-else class="no-selection">
          暂未选择任何时间偏好
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'

interface WeekdayData {
  value: number
  label: string
  selected: boolean
  timeSlots: string[]
}

interface Props {
  weekdays?: number[]
  timeSlots?: string[]
}

interface Emits {
  (e: 'update:weekdays', value: number[]): void
  (e: 'update:timeSlots', value: string[]): void
}

const props = withDefaults(defineProps<Props>(), {
  weekdays: () => [],
  timeSlots: () => []
})

const emit = defineEmits<Emits>()

// 星期几数据（每个星期几都有自己的时间段）
const weekdayData = ref<WeekdayData[]>([
  { value: 1, label: '周一', selected: false, timeSlots: [] },
  { value: 2, label: '周二', selected: false, timeSlots: [] },
  { value: 3, label: '周三', selected: false, timeSlots: [] },
  { value: 4, label: '周四', selected: false, timeSlots: [] },
  { value: 5, label: '周五', selected: false, timeSlots: [] },
  { value: 6, label: '周六', selected: false, timeSlots: [] },
  { value: 7, label: '周日', selected: false, timeSlots: [] }
])

// 当前选中的星期几
const selectedWeekday = ref<WeekdayData | null>(null)

// 总结区域展开状态
const summaryExpanded = ref(false)

// 时间段选择区域展开状态
const timeslotsExpanded = ref(false)

const timeSlotOptions = [
  { value: '08:00-08:30', label: '08:00', range: '' },
  { value: '08:30-09:00', label: '08:30', range: '' },
  { value: '09:00-09:30', label: '09:00', range: '' },
  { value: '09:30-10:00', label: '09:30', range: '' },
  { value: '10:00-10:30', label: '10:00', range: '' },
  { value: '10:30-11:00', label: '10:30', range: '' },
  { value: '11:00-11:30', label: '11:00', range: '' },
  { value: '11:30-12:00', label: '11:30', range: '' },
  { value: '14:00-14:30', label: '14:00', range: '' },
  { value: '14:30-15:00', label: '14:30', range: '' },
  { value: '15:00-15:30', label: '15:00', range: '' },
  { value: '15:30-16:00', label: '15:30', range: '' },
  { value: '16:00-16:30', label: '16:00', range: '' },
  { value: '16:30-17:00', label: '16:30', range: '' },
  { value: '17:00-17:30', label: '17:00', range: '' },
  { value: '17:30-18:00', label: '17:30', range: '' },
  { value: '18:00-18:30', label: '18:00', range: '' },
  { value: '18:30-19:00', label: '18:30', range: '' },
  { value: '19:00-19:30', label: '19:00', range: '' },
  { value: '19:30-20:00', label: '19:30', range: '' },
  { value: '20:00-20:30', label: '20:00', range: '' },
  { value: '20:30-21:00', label: '20:30', range: '' }
]

// 计算属性
const selectedWeekdayData = computed(() =>
  weekdayData.value.filter(day => day.selected && day.timeSlots.length > 0)
)

const totalSelections = computed(() =>
  selectedWeekdayData.value.reduce((total, day) => total + day.timeSlots.length, 0)
)

// 监听props变化，初始化数据
watch(() => [props.weekdays, props.timeSlots], ([newWeekdays, newTimeSlots]) => {
  // 重置所有数据
  weekdayData.value.forEach(day => {
    day.selected = (newWeekdays as number[]).includes(day.value)
    day.timeSlots = (newWeekdays as number[]).includes(day.value) ? [...(newTimeSlots as string[])] : []
  })
}, { immediate: true, deep: true })

// 监听weekdayData变化，发出事件
watch(weekdayData, () => {
  const selectedWeekdays = weekdayData.value
    .filter(day => day.selected && day.timeSlots.length > 0)
    .map(day => day.value)

  const allTimeSlots = weekdayData.value
    .filter(day => day.selected && day.timeSlots.length > 0)
    .flatMap(day => day.timeSlots)
    .filter((slot, index, arr) => arr.indexOf(slot) === index) // 去重

  emit('update:weekdays', selectedWeekdays)
  emit('update:timeSlots', allTimeSlots)
}, { deep: true })

// 切换星期几选择
const toggleWeekday = (weekdayValue: number) => {
  const weekday = weekdayData.value.find(day => day.value === weekdayValue)
  if (!weekday) return

  if (weekday.selected) {
    // 如果已选中，取消选择并清空时间段
    weekday.selected = false
    weekday.timeSlots = []
    if (selectedWeekday.value === weekday) {
      selectedWeekday.value = null
      timeslotsExpanded.value = false
    }
  } else {
    // 选中该星期几，并显示时间段选择
    weekday.selected = true
    selectedWeekday.value = weekday
    timeslotsExpanded.value = true // 自动展开时间段选择
  }
}

// 切换时间段选择
const toggleTimeSlot = (timeSlotValue: string) => {
  if (!selectedWeekday.value) return

  const index = selectedWeekday.value.timeSlots.indexOf(timeSlotValue)
  if (index > -1) {
    selectedWeekday.value.timeSlots.splice(index, 1)
  } else {
    selectedWeekday.value.timeSlots.push(timeSlotValue)
  }

  // 如果该星期几没有选择任何时间段，则取消选中状态
  if (selectedWeekday.value.timeSlots.length === 0) {
    selectedWeekday.value.selected = false
  } else {
    selectedWeekday.value.selected = true
  }
}

// 获取时间段标签
const getTimeSlotLabel = (timeSlotValue: string) => {
  const slot = timeSlotOptions.find(s => s.value === timeSlotValue)
  return slot ? slot.label : timeSlotValue
}

// 切换总结区域展开状态
const toggleSummaryExpanded = () => {
  summaryExpanded.value = !summaryExpanded.value
}

// 切换时间段选择区域展开状态
const toggleTimeslotsExpanded = () => {
  timeslotsExpanded.value = !timeslotsExpanded.value
}

// 快速选择方法
const selectWorkdayEvening = () => {
  const eveningSlots = ['18:00-18:30', '18:30-19:00', '19:00-19:30', '19:30-20:00', '20:00-20:30', '20:30-21:00']
  weekdayData.value.forEach(day => {
    if (day.value >= 1 && day.value <= 5) {
      day.selected = true
      day.timeSlots = [...eveningSlots]
    }
  })
  // 自动展开总结区域
  summaryExpanded.value = true
}

const selectWeekendDay = () => {
  const daySlots = ['09:00-09:30', '09:30-10:00', '10:00-10:30', '10:30-11:00', '14:00-14:30', '14:30-15:00', '15:00-15:30', '15:30-16:00']
  weekdayData.value.forEach(day => {
    if (day.value === 6 || day.value === 7) {
      day.selected = true
      day.timeSlots = [...daySlots]
    }
  })
  // 自动展开总结区域
  summaryExpanded.value = true
}

const clearAll = () => {
  weekdayData.value.forEach(day => {
    day.selected = false
    day.timeSlots = []
  })
  selectedWeekday.value = null
  timeslotsExpanded.value = false
  summaryExpanded.value = false
}
</script>

<style scoped>
.flexible-time-preference {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  width: 100%;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

/* 快速操作按钮 */
.quick-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

/* 星期几选择区域 */
.weekdays-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  text-align: center;
}

.weekdays-grid {
  display: flex;
  justify-content: center;
  gap: 6px;
  flex-wrap: wrap;
  max-width: 100%;
  margin: 0 auto;
}

.weekday-card {
  background: white;
  border: 2px solid #e4e7ed;
  border-radius: 6px;
  padding: 8px 6px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 50px;
  width: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
}

.weekday-card:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.weekday-card.active {
  border-color: #409eff;
  background: linear-gradient(135deg, #409eff 0%, #66b3ff 100%);
  color: white;
}

.weekday-name {
  font-weight: 600;
  font-size: 12px;
  margin-bottom: 3px;
}

.weekday-count {
  font-size: 10px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  padding: 1px 4px;
  min-width: 14px;
  display: inline-block;
}

.weekday-card:not(.active) .weekday-count {
  background: #f5f7fa;
  color: #909399;
}

/* 时间段选择区域 */
.timeslots-section {
  background: white;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  margin-bottom: 20px;
  overflow: hidden;
}

.timeslots-header {
  padding: 12px 16px;
  background: #f8f9fa;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.3s ease;
}

.timeslots-header:hover {
  background: #e9ecef;
}

.timeslots-header .section-title {
  margin-bottom: 0;
  text-align: left;
  display: flex;
  align-items: center;
  gap: 8px;
}

.selected-count {
  font-size: 12px;
  color: #409eff;
  font-weight: 500;
}

.timeslots-content {
  padding: 16px;
}

.timeslots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(70px, 1fr));
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 4px;
}

.timeslot-item {
  background: #f8f9fa;
  border: 2px solid #e4e7ed;
  border-radius: 6px;
  padding: 8px 4px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  min-height: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.timeslot-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.15);
}

.timeslot-item.selected {
  border-color: #409eff;
  background: linear-gradient(135deg, #409eff 0%, #66b3ff 100%);
  color: white;
}

.time-display {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 2px;
}

.time-range {
  font-size: 10px;
  opacity: 0.8;
}

/* 选择总结 */
.selection-summary {
  background: white;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.summary-header {
  padding: 12px 16px;
  background: #f8f9fa;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.3s ease;
}

.summary-header:hover {
  background: #e9ecef;
}

.summary-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.selection-count {
  font-size: 12px;
  color: #409eff;
  font-weight: 500;
}

.expand-icon {
  transition: transform 0.3s ease;
  color: #909399;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.summary-content {
  padding: 16px;
}

.selected-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selected-day {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 12px;
  border-left: 4px solid #409eff;
}

.day-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  display: block;
}

.day-times {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.no-selection {
  text-align: center;
  color: #909399;
  font-style: italic;
  padding: 20px;
}

/* 滚动条样式 */
.timeslots-grid::-webkit-scrollbar {
  width: 4px;
}

.timeslots-grid::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.timeslots-grid::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.timeslots-grid::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .weekdays-grid {
    gap: 4px;
  }

  .weekday-card {
    width: 50px;
    min-height: 45px;
    padding: 6px 4px;
  }

  .weekday-name {
    font-size: 11px;
  }

  .weekday-count {
    font-size: 9px;
    padding: 1px 3px;
  }

  .timeslots-grid {
    grid-template-columns: repeat(auto-fill, minmax(60px, 1fr));
    max-height: 160px;
  }

  .selected-day {
    padding: 8px;
  }

  .day-times {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .weekdays-grid {
    gap: 3px;
  }

  .weekday-card {
    width: 45px;
    min-height: 40px;
    padding: 4px 2px;
  }

  .weekday-name {
    font-size: 10px;
  }
}
</style>
