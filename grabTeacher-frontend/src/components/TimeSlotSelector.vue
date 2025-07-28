<template>
  <div class="time-slot-selector">
    <div class="selector-header">
      <h3>{{ title }}</h3>
      <p class="description">{{ description }}</p>
    </div>
    
    <div class="weekdays-container">
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
              v-for="slot in timeSlots"
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
    
    <div class="summary-section">
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
}

interface Emits {
  (e: 'update:modelValue', value: TimeSlot[]): void
  (e: 'change', value: TimeSlot[]): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  title: '选择可上课时间',
  description: '请选择您可以上课的时间段',
  maxSlots: 50
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

// 时间段选项
const timeSlots = [
  '08:00-09:00', '09:00-10:00', '10:00-11:00', '11:00-12:00',
  '14:00-15:00', '15:00-16:00', '16:00-17:00', '17:00-18:00',
  '18:00-19:00', '19:00-20:00', '20:00-21:00'
]

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
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
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
</style>
