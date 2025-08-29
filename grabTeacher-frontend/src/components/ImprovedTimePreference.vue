<template>
  <div class="improved-time-preference">
    <div class="time-preference-header" @click="toggleExpanded">
      <div class="header-content">
        <h4>选择您的偏好上课时间</h4>
        <el-icon class="expand-icon" :class="{ 'expanded': isExpanded }">
          <ArrowDown />
        </el-icon>
      </div>
      <p class="description" v-show="isExpanded">请选择您希望上课的时间段，系统将为您匹配相应时间可用的教师</p>
    </div>

    <!-- 可折叠内容 -->
    <div class="collapsible-content" v-show="isExpanded">
      <!-- 时间段选择 -->
      <div class="time-slots-section">
        <div class="time-slots-grid">
          <div
            v-for="timeSlot in availableTimeSlots"
            :key="timeSlot"
            :class="[
              'time-slot-card',
              {
                'selected': selectedTimeSlots.includes(timeSlot)
              }
            ]"
            @click="toggleTimeSlot(timeSlot)"
          >
            <div class="time-slot-time">{{ timeSlot }}</div>
          </div>
        </div>
      </div>

      <!-- 星期几选择 -->
      <div class="weekdays-section" v-if="selectedTimeSlots.length > 0">
        <div class="weekdays-grid">
          <div
            v-for="(weekday, index) in weekdayOptions"
            :key="index"
            :class="[
              'weekday-card',
              {
                'selected': selectedWeekdays.includes(index + 1),
                'weekend': index === 5 || index === 6
              }
            ]"
            @click="toggleWeekday(index)"
          >
            <div class="weekday-name">{{ weekday.name }}</div>
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
                v-for="weekdayValue in selectedWeekdays"
                :key="weekdayValue"
                type="success"
                size="small"
                closable
                @close="removeWeekday(weekdayValue)"
              >
                {{ weekdayOptions[weekdayValue - 1].name }}
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
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'

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
const isExpanded = ref(false) // 默认收起

// 可用时间段 - 固定为6个系统上课时间
const availableTimeSlots = [
  '08:00-10:00',
  '10:00-12:00',
  '13:00-15:00',
  '15:00-17:00',
  '17:00-19:00',
  '19:00-21:00'
]

// 星期几选项
const weekdayOptions = [
  { name: '周一'},
  { name: '周二'},
  { name: '周三'},
  { name: '周四'},
  { name: '周五'},
  { name: '周六'},
  { name: '周日'}
]

// 方法
const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

const toggleTimeSlot = (timeSlot: string) => {
  const index = selectedTimeSlots.value.indexOf(timeSlot)
  if (index > -1) {
    selectedTimeSlots.value.splice(index, 1)
  } else {
    selectedTimeSlots.value.push(timeSlot)
  }
}

const toggleWeekday = (weekdayIndex: number) => {
  // 将数组索引转换为后端使用的星期几数字 (周一=1, 周二=2, ..., 周日=7)
  const weekdayValue = weekdayIndex + 1
  const index = selectedWeekdays.value.indexOf(weekdayValue)
  if (index > -1) {
    selectedWeekdays.value.splice(index, 1)
  } else {
    selectedWeekdays.value.push(weekdayValue)
  }
}

const removeTimeSlot = (timeSlot: string) => {
  const index = selectedTimeSlots.value.indexOf(timeSlot)
  if (index > -1) {
    selectedTimeSlots.value.splice(index, 1)
  }
}

const removeWeekday = (weekdayIndex: number) => {
  // weekdayIndex 这里实际上是后端的星期几值 (1-7)，直接使用
  const index = selectedWeekdays.value.indexOf(weekdayIndex)
  if (index > -1) {
    selectedWeekdays.value.splice(index, 1)
  }
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
watch(selectedWeekdays, (newWeekdays: number[]) => {
  emit('update:weekdays', [...newWeekdays])
}, { deep: true })

watch(selectedTimeSlots, (newTimeSlots: string[]) => {
  emit('update:time-slots', [...newTimeSlots])
}, { deep: true })

// 监听props变化
watch(() => props.weekdays, (newWeekdays: number[]) => {
  selectedWeekdays.value = [...newWeekdays]
})

watch(() => props.timeSlots, (newTimeSlots: string[]) => {
  selectedTimeSlots.value = [...newTimeSlots]
})
</script>

<style scoped>
.improved-time-preference {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  min-height: 120px;
  width: 100%;
}

.time-preference-header {
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
  width: 100%;
  box-sizing: border-box;
}

.header-content:hover {
  border-color: #007bff;
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.1);
}

.time-preference-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  font-weight: 600;
}

.expand-icon {
  transition: transform 0.2s ease;
  color: #6c757d;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.description {
  margin: 8px 0 0 0;
  color: #6c757d;
  font-size: 14px;
  line-height: 1.5;
  padding: 0 16px;
}

.collapsible-content {
  margin-top: 16px;
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

.time-slot-time {
  font-weight: 600;
  color: #2c3e50;
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
