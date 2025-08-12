<template>
  <div class="wide-time-selector">
    <div class="selector-header" @click="toggleExpanded">
      <div class="header-content">
        <h4>{{ title }}</h4>
        <el-icon class="expand-icon" :class="{ 'expanded': isExpanded }">
          <ArrowDown />
        </el-icon>
      </div>
      <div v-show="isExpanded" class="quick-actions">
        <el-button @click.stop="selectWorkdays" size="small" type="primary" plain>
          工作日晚上
        </el-button>
        <el-button @click.stop="selectWeekends" size="small" type="success" plain>
          周末全天
        </el-button>
        <el-button @click.stop="clearAll" size="small" plain>
          清空
        </el-button>
      </div>
    </div>

    <!-- 可折叠的内容区域 -->
    <div class="collapsible-content" v-show="isExpanded">
      <!-- 时间选择网格 - 新设计 -->
      <div class="time-grid-container">
        <div class="weekdays-header">
          <div class="time-label">时间段</div>
          <div
            v-for="weekday in weekdays"
            :key="weekday.value"
            class="weekday-header"
          >
            {{ weekday.short }}
          </div>
        </div>

        <div class="time-rows">
          <div
            v-for="timeSlot in timeSlots"
            :key="timeSlot.value"
            class="time-row"
          >
            <div class="time-label">{{ timeSlot.display }}</div>
            <div
              v-for="weekday in weekdays"
              :key="`${weekday.value}-${timeSlot.value}`"
              class="time-cell"
              :class="{ 'selected': isSlotSelected(weekday.value, timeSlot.value) }"
              @click="toggleSlot(weekday.value, timeSlot.value)"
            >
              <div class="cell-content">
                <el-icon v-if="isSlotSelected(weekday.value, timeSlot.value)">
                  <Check />
                </el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 总结 -->
      <div class="summary-section">
        <div class="summary-text">
          已选择 <span class="highlight">{{ totalSelectedSlots }}</span> 个时间段
        </div>
        <div v-if="totalSelectedSlots > 0" class="selected-preview">
          <div v-for="(slots, weekdayId) in selectedSlots" :key="weekdayId" class="weekday-summary">
            <span class="weekday-name">{{ getWeekdayName(Number(weekdayId)) }}:</span>
            <el-tag
              v-for="slot in slots"
              :key="slot"
              size="small"
              type="primary"
              closable
              @close="removeSlot(Number(weekdayId), slot)"
            >
              {{ slot }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Check, ArrowDown } from '@element-plus/icons-vue'

interface TimeSlot {
  weekday: number
  timeSlots: string[]
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

// 响应式数据 - 使用简单的二维结构
const selectedSlots = ref<Record<number, string[]>>({})
const isExpanded = ref(false) // 默认收起状态

// 星期几选项
const weekdays = [
  { value: 1, label: '周一', short: '一' },
  { value: 2, label: '周二', short: '二' },
  { value: 3, label: '周三', short: '三' },
  { value: 4, label: '周四', short: '四' },
  { value: 5, label: '周五', short: '五' },
  { value: 6, label: '周六', short: '六' },
  { value: 7, label: '周日', short: '日' }
]

// 时间段选项
const timeSlots = [
  { value: '09:00-10:00', display: '09:00' },
  { value: '10:00-11:00', display: '10:00' },
  { value: '11:00-12:00', display: '11:00' },
  { value: '12:00-13:00', display: '12:00' },
  { value: '13:00-14:00', display: '13:00' },
  { value: '14:00-15:00', display: '14:00' },
  { value: '15:00-16:00', display: '15:00' },
  { value: '16:00-17:00', display: '16:00' },
  { value: '17:00-18:00', display: '17:00' },
  { value: '18:00-19:00', display: '18:00' },
  { value: '19:00-20:00', display: '19:00' },
  { value: '20:00-21:00', display: '20:00' }
]

// 计算属性
const totalSelectedSlots = computed(() =>
  Object.values(selectedSlots.value).reduce((total, slots) => total + slots.length, 0)
)

// 核心方法
const isSlotSelected = (weekdayId: number, timeSlot: string): boolean => {
  const weekdaySlots = selectedSlots.value[weekdayId] || []
  return weekdaySlots.includes(timeSlot)
}

const toggleSlot = (weekdayId: number, timeSlot: string) => {
  if (!selectedSlots.value[weekdayId]) {
    selectedSlots.value[weekdayId] = []
  }

  const slots = selectedSlots.value[weekdayId]
  const index = slots.indexOf(timeSlot)

  if (index > -1) {
    // 移除时间段
    slots.splice(index, 1)
    // 如果该星期几没有时间段了，删除整个键
    if (slots.length === 0) {
      delete selectedSlots.value[weekdayId]
    }
  } else {
    // 添加时间段
    slots.push(timeSlot)
  }

  emitChange()
}

const removeSlot = (weekdayId: number, timeSlot: string) => {
  if (selectedSlots.value[weekdayId]) {
    const slots = selectedSlots.value[weekdayId]
    const index = slots.indexOf(timeSlot)
    if (index > -1) {
      slots.splice(index, 1)
      if (slots.length === 0) {
        delete selectedSlots.value[weekdayId]
      }
      emitChange()
    }
  }
}

const getWeekdayName = (weekdayId: number): string => {
  const weekday = weekdays.find(w => w.value === weekdayId)
  return weekday ? weekday.label : ''
}

// 切换展开/收起状态
const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

// 初始化数据
const initializeData = () => {
  selectedSlots.value = {}

  // 根据 modelValue 设置选择
  if (props.modelValue && props.modelValue.length > 0) {
    console.log('WideTimeSlotSelector: 初始化数据', props.modelValue)
    props.modelValue.forEach((item: TimeSlot) => {
      if (item.timeSlots && item.timeSlots.length > 0) {
        selectedSlots.value[item.weekday] = [...item.timeSlots]
        console.log(`WideTimeSlotSelector: 设置星期${item.weekday}的时间段:`, item.timeSlots)
      }
    })
    console.log('WideTimeSlotSelector: 最终selectedSlots:', selectedSlots.value)
  } else {
    console.log('WideTimeSlotSelector: 没有初始数据或数据为空')
  }
}

// 监听props变化
watch(() => props.modelValue, () => {
  initializeData()
}, { immediate: true, deep: true })

// 快速选择工作日
const selectWorkdays = () => {
  selectedSlots.value = {}
  weekdays.forEach(weekday => {
    if (weekday.value >= 1 && weekday.value <= 5) {
      selectedSlots.value[weekday.value] = ['18:00-19:00', '19:00-20:00', '20:00-21:00']
    }
  })
  emitChange()
}

// 快速选择周末
const selectWeekends = () => {
  selectedSlots.value = {}
  weekdays.forEach(weekday => {
    if (weekday.value === 6 || weekday.value === 7) {
      selectedSlots.value[weekday.value] = ['09:00-10:00', '10:00-11:00', '11:00-12:00', '14:00-15:00', '15:00-16:00', '16:00-17:00']
    }
  })
  emitChange()
}

// 清空所有
const clearAll = () => {
  selectedSlots.value = {}
  emitChange()
}

// 发出变化事件
const emitChange = () => {
  const result: TimeSlot[] = Object.entries(selectedSlots.value)
    .filter(([_, slots]) => slots.length > 0)
    .map(([weekdayStr, slots]) => ({
      weekday: parseInt(weekdayStr),
      timeSlots: [...slots]
    }))

  emit('update:modelValue', result)
  emit('change', result)
}
</script>

<style scoped>
.wide-time-selector {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.time-grid-container {
  margin: 20px 0;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.weekdays-header {
  display: grid;
  grid-template-columns: 100px repeat(7, 1fr);
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.time-label {
  padding: 12px 8px;
  font-weight: 600;
  color: #606266;
  text-align: center;
  border-right: 1px solid #e4e7ed;
  background: #fafbfc;
}

.weekday-header {
  padding: 12px 8px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  border-right: 1px solid #e4e7ed;
}

.weekday-header:last-child {
  border-right: none;
}

.time-rows {
  background: white;
}

.time-row {
  display: grid;
  grid-template-columns: 100px repeat(7, 1fr);
  border-bottom: 1px solid #f0f2f5;
}

.time-row:last-child {
  border-bottom: none;
}

.time-cell {
  padding: 8px;
  border-right: 1px solid #f0f2f5;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.time-cell:last-child {
  border-right: none;
}

.time-cell:hover {
  background: #f0f9ff;
}

.time-cell.selected {
  background: #e7f3ff;
  color: #409eff;
}

.cell-content {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.summary-section {
  margin-top: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.summary-text {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
}

.highlight {
  color: #409eff;
  font-weight: 600;
}

.selected-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.weekday-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.weekday-name {
  font-weight: 600;
  color: #303133;
  min-width: 60px;
}

.selector-header {
  margin-bottom: 16px;
  cursor: pointer;
  user-select: none;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.header-content:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.selector-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.expand-icon {
  transition: transform 0.2s ease;
  color: #6c757d;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.quick-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding: 0 16px;
}

.collapsible-content {
  margin-top: 16px;
}

/* 星期几选择区域 */
.weekdays-section {
  margin-bottom: 24px;
}

.weekdays-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
}

.weekday-card {
  background: white;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  min-height: 70px;
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
  font-size: 16px;
  margin-bottom: 6px;
}

.weekday-count {
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  border-radius: 12px;
  padding: 3px 10px;
  display: inline-block;
  min-width: 24px;
}

.weekday-card.selected .weekday-count {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

/* 时间段选择区域 */
.time-slots-section {
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  text-align: center;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.time-slot {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 60px;
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
  font-size: 14px;
  margin-bottom: 3px;
}

.time-period {
  font-size: 11px;
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
</style>
