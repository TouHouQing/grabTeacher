<template>
  <div class="improved-time-preference">
    <div class="time-preference-header">
      <h4>选择您的偏好上课时间</h4>
      <p class="description">请选择您希望上课的时间段，系统将为您匹配相应时间可用的教师</p>
    </div>

    <!-- 时间段选择 -->
    <div class="time-slots-section">
      <h5>偏好时间段</h5>
      <div class="time-slots-grid">
        <div
          v-for="timeSlot in availableTimeSlots"
          :key="timeSlot"
          :class="[
            'time-slot-card',
            {
              'selected': selectedTimeSlots.includes(timeSlot),
              'morning': isMorningSlot(timeSlot),
              'afternoon': isAfternoonSlot(timeSlot),
              'evening': isEveningSlot(timeSlot)
            }
          ]"
          @click="toggleTimeSlot(timeSlot)"
        >
          <div class="time-slot-time">{{ timeSlot }}</div>
          <div class="time-slot-period">{{ getTimePeriod(timeSlot) }}</div>
        </div>
      </div>
    </div>

    <!-- 星期几选择 -->
    <div class="weekdays-section" v-if="selectedTimeSlots.length > 0">
      <h5>偏好星期几</h5>
      <div class="weekdays-grid">
        <div
          v-for="(weekday, index) in weekdayOptions"
          :key="index"
          :class="[
            'weekday-card',
            {
              'selected': selectedWeekdays.includes(index),
              'weekend': index === 0 || index === 6
            }
          ]"
          @click="toggleWeekday(index)"
        >
          <div class="weekday-name">{{ weekday.name }}</div>
          <div class="weekday-short">{{ weekday.short }}</div>
        </div>
      </div>
    </div>

    <!-- 选择预览 -->
    <div class="selection-preview" v-if="selectedTimeSlots.length > 0 || selectedWeekdays.length > 0">
      <h5>您的选择预览</h5>
      <div class="preview-content">
        <div class="preview-item" v-if="selectedTimeSlots.length > 0">
          <span class="preview-label">时间段：</span>
          <div class="preview-tags">
            <el-tag
              v-for="timeSlot in selectedTimeSlots"
              :key="timeSlot"
              type="primary"
              size="small"
              closable
              @close="removeTimeSlot(timeSlot)"
            >
              {{ timeSlot }}
            </el-tag>
          </div>
        </div>
        <div class="preview-item" v-if="selectedWeekdays.length > 0">
          <span class="preview-label">星期几：</span>
          <div class="preview-tags">
            <el-tag
              v-for="weekdayIndex in selectedWeekdays"
              :key="weekdayIndex"
              type="success"
              size="small"
              closable
              @close="removeWeekday(weekdayIndex)"
            >
              {{ weekdayOptions[weekdayIndex].name }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 匹配度提示 -->
    <div class="match-tips" v-if="selectedTimeSlots.length > 0 || selectedWeekdays.length > 0">
      <el-alert
        :title="getMatchTip()"
        type="info"
        show-icon
        :closable="false"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

// Props
interface Props {
  weekdays?: number[]
  timeSlots?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  weekdays: () => [],
  timeSlots: () => []
})

// Emits
const emit = defineEmits<{
  'update:weekdays': [weekdays: number[]]
  'update:time-slots': [timeSlots: string[]]
}>()

// 响应式数据
const selectedWeekdays = ref<number[]>([...props.weekdays])
const selectedTimeSlots = ref<string[]>([...props.timeSlots])

// 可用时间段
const availableTimeSlots = [
  '09:00-10:00', '10:00-11:00', '11:00-12:00',
  '14:00-15:00', '15:00-16:00', '16:00-17:00', '17:00-18:00',
  '18:00-19:00', '19:00-20:00', '20:00-21:00'
]

// 星期几选项
const weekdayOptions = [
  { name: '周日', short: '日' },
  { name: '周一', short: '一' },
  { name: '周二', short: '二' },
  { name: '周三', short: '三' },
  { name: '周四', short: '四' },
  { name: '周五', short: '五' },
  { name: '周六', short: '六' }
]

// 方法
const toggleTimeSlot = (timeSlot: string) => {
  const index = selectedTimeSlots.value.indexOf(timeSlot)
  if (index > -1) {
    selectedTimeSlots.value.splice(index, 1)
  } else {
    selectedTimeSlots.value.push(timeSlot)
  }
}

const toggleWeekday = (weekdayIndex: number) => {
  const index = selectedWeekdays.value.indexOf(weekdayIndex)
  if (index > -1) {
    selectedWeekdays.value.splice(index, 1)
  } else {
    selectedWeekdays.value.push(weekdayIndex)
  }
}

const removeTimeSlot = (timeSlot: string) => {
  const index = selectedTimeSlots.value.indexOf(timeSlot)
  if (index > -1) {
    selectedTimeSlots.value.splice(index, 1)
  }
}

const removeWeekday = (weekdayIndex: number) => {
  const index = selectedWeekdays.value.indexOf(weekdayIndex)
  if (index > -1) {
    selectedWeekdays.value.splice(index, 1)
  }
}

// 判断时间段类型
const isMorningSlot = (timeSlot: string) => {
  const hour = parseInt(timeSlot.split(':')[0])
  return hour >= 9 && hour < 12
}

const isAfternoonSlot = (timeSlot: string) => {
  const hour = parseInt(timeSlot.split(':')[0])
  return hour >= 14 && hour < 18
}

const isEveningSlot = (timeSlot: string) => {
  const hour = parseInt(timeSlot.split(':')[0])
  return hour >= 18
}

const getTimePeriod = (timeSlot: string) => {
  if (isMorningSlot(timeSlot)) return '上午'
  if (isAfternoonSlot(timeSlot)) return '下午'
  if (isEveningSlot(timeSlot)) return '晚上'
  return ''
}

const getMatchTip = () => {
  const timeCount = selectedTimeSlots.value.length
  const dayCount = selectedWeekdays.value.length

  if (timeCount > 0 && dayCount > 0) {
    return `您选择了 ${timeCount} 个时间段和 ${dayCount} 天，这将帮助系统为您找到最匹配的教师`
  } else if (timeCount > 0) {
    return `您选择了 ${timeCount} 个时间段，建议也选择偏好的星期几以获得更精准的匹配`
  } else if (dayCount > 0) {
    return `您选择了 ${dayCount} 天，建议也选择具体的时间段以获得更精准的匹配`
  }
  return '请选择您的偏好时间以获得更好的匹配结果'
}

// 监听变化并发出事件
watch(selectedWeekdays, (newWeekdays) => {
  emit('update:weekdays', [...newWeekdays])
}, { deep: true })

watch(selectedTimeSlots, (newTimeSlots) => {
  emit('update:time-slots', [...newTimeSlots])
}, { deep: true })

// 监听props变化
watch(() => props.weekdays, (newWeekdays) => {
  selectedWeekdays.value = [...newWeekdays]
})

watch(() => props.timeSlots, (newTimeSlots) => {
  selectedTimeSlots.value = [...newTimeSlots]
})
</script>

<style scoped>
.improved-time-preference {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.time-preference-header {
  margin-bottom: 24px;
}

.time-preference-header h4 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
}

.description {
  margin: 0;
  color: #6c757d;
  font-size: 14px;
  line-height: 1.5;
}

.time-slots-section,
.weekdays-section {
  margin-bottom: 24px;
}

.time-slots-section h5,
.weekdays-section h5 {
  margin: 0 0 12px 0;
  color: #495057;
  font-size: 14px;
  font-weight: 600;
}

.time-slots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.time-slot-card {
  padding: 12px;
  background: white;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
}

.time-slot-card:hover {
  border-color: #007bff;
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.1);
}

.time-slot-card.selected {
  border-color: #007bff;
  background: #e7f3ff;
}

.time-slot-card.morning.selected {
  border-color: #28a745;
  background: #e8f5e8;
}

.time-slot-card.afternoon.selected {
  border-color: #ffc107;
  background: #fff8e1;
}

.time-slot-card.evening.selected {
  border-color: #6f42c1;
  background: #f3e8ff;
}

.time-slot-time {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.time-slot-period {
  font-size: 12px;
  color: #6c757d;
}

.weekdays-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.weekday-card {
  padding: 12px 8px;
  background: white;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
}

.weekday-card:hover {
  border-color: #28a745;
  box-shadow: 0 2px 8px rgba(40, 167, 69, 0.1);
}

.weekday-card.selected {
  border-color: #28a745;
  background: #e8f5e8;
}

.weekday-card.weekend {
  background: #fff5f5;
}

.weekday-card.weekend.selected {
  border-color: #dc3545;
  background: #ffe6e6;
}

.weekday-name {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 2px;
  font-size: 14px;
}

.weekday-short {
  font-size: 12px;
  color: #6c757d;
}

.selection-preview {
  margin-bottom: 16px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #dee2e6;
}

.selection-preview h5 {
  margin: 0 0 12px 0;
  color: #495057;
  font-size: 14px;
  font-weight: 600;
}

.preview-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
}

.preview-item:last-child {
  margin-bottom: 0;
}

.preview-label {
  min-width: 60px;
  color: #6c757d;
  font-size: 14px;
  margin-right: 12px;
  margin-top: 4px;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.match-tips {
  margin-top: 16px;
}
</style>
