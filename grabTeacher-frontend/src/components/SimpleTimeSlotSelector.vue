<template>
  <div class="simple-time-slot-selector">
    <div class="selector-header">
      <h3>{{ title }}</h3>
      <p class="description">{{ description }}</p>
    </div>
    
    <div class="quick-select-section">
      <div class="quick-select-buttons">
        <el-button @click="selectWorkdays" size="small" type="primary" plain>
          选择工作日晚上
        </el-button>
        <el-button @click="selectWeekends" size="small" type="success" plain>
          选择周末全天
        </el-button>
        <el-button @click="selectAll" size="small" type="warning" plain>
          全选
        </el-button>
        <el-button @click="clearAll" size="small" type="danger" plain>
          清空
        </el-button>
      </div>
    </div>
    
    <div class="weekdays-grid">
      <div 
        v-for="weekday in weekdays" 
        :key="weekday.value"
        class="weekday-card"
        :class="{ 'selected': weekday.selected }"
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
        
        <div v-if="weekday.selected" class="time-slots-compact">
          <el-checkbox-group 
            v-model="weekday.selectedSlots"
            @change="onTimeSlotsChange"
            size="small"
          >
            <div class="time-slot-row">
              <el-checkbox label="08:00-09:00">08:00</el-checkbox>
              <el-checkbox label="09:00-10:00">09:00</el-checkbox>
              <el-checkbox label="10:00-11:00">10:00</el-checkbox>
              <el-checkbox label="11:00-12:00">11:00</el-checkbox>
            </div>
            <div class="time-slot-row">
              <el-checkbox label="14:00-15:00">14:00</el-checkbox>
              <el-checkbox label="15:00-16:00">15:00</el-checkbox>
              <el-checkbox label="16:00-17:00">16:00</el-checkbox>
              <el-checkbox label="17:00-18:00">17:00</el-checkbox>
            </div>
            <div class="time-slot-row">
              <el-checkbox label="18:00-19:00">18:00</el-checkbox>
              <el-checkbox label="19:00-20:00">19:00</el-checkbox>
              <el-checkbox label="20:00-21:00">20:00</el-checkbox>
            </div>
          </el-checkbox-group>
        </div>
      </div>
    </div>
    
    <div class="summary-section">
      <div class="summary-stats">
        <el-tag type="info" size="small">
          已选择 {{ totalSelectedSlots }} 个时间段
        </el-tag>
        <el-tag type="primary" size="small" v-if="weekdaySlots > 0">
          工作日 {{ weekdaySlots }} 个
        </el-tag>
        <el-tag type="success" size="small" v-if="weekendSlots > 0">
          周末 {{ weekendSlots }} 个
        </el-tag>
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
  title: '设置可上课时间',
  description: '请选择您可以上课的时间段',
  maxSlots: 30
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

// 快速选择工作日晚上
const selectWorkdays = () => {
  weekdays.value.forEach(weekday => {
    if (weekday.value >= 1 && weekday.value <= 5) {
      weekday.selected = true
      weekday.selectedSlots = ['18:00-19:00', '19:00-20:00', '20:00-21:00']
    }
  })
  emitChange()
}

// 快速选择周末
const selectWeekends = () => {
  weekdays.value.forEach(weekday => {
    if (weekday.value === 6 || weekday.value === 7) {
      weekday.selected = true
      weekday.selectedSlots = ['09:00-10:00', '10:00-11:00', '14:00-15:00', '15:00-16:00', '16:00-17:00']
    }
  })
  emitChange()
}

// 全选
const selectAll = () => {
  weekdays.value.forEach(weekday => {
    weekday.selected = true
    if (weekday.value >= 1 && weekday.value <= 5) {
      // 工作日选择晚上时间
      weekday.selectedSlots = ['18:00-19:00', '19:00-20:00', '20:00-21:00']
    } else {
      // 周末选择全天时间
      weekday.selectedSlots = ['09:00-10:00', '10:00-11:00', '14:00-15:00', '15:00-16:00', '16:00-17:00']
    }
  })
  emitChange()
}

// 清空所有选择
const clearAll = () => {
  weekdays.value.forEach(weekday => {
    weekday.selected = false
    weekday.selectedSlots = []
  })
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
const totalSelectedSlots = computed(() => {
  return weekdays.value.reduce((total, weekday) => {
    return total + (weekday.selected ? weekday.selectedSlots.length : 0)
  }, 0)
})

const weekdaySlots = computed(() => {
  return weekdays.value
    .filter(weekday => weekday.value >= 1 && weekday.value <= 5 && weekday.selected)
    .reduce((total, weekday) => total + weekday.selectedSlots.length, 0)
})

const weekendSlots = computed(() => {
  return weekdays.value
    .filter(weekday => (weekday.value === 6 || weekday.value === 7) && weekday.selected)
    .reduce((total, weekday) => total + weekday.selectedSlots.length, 0)
})
</script>

<style scoped>
.simple-time-slot-selector {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.selector-header {
  margin-bottom: 16px;
}

.selector-header h3 {
  margin: 0 0 6px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.description {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.quick-select-section {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.quick-select-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.weekdays-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.weekday-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px;
  transition: all 0.3s ease;
}

.weekday-card.selected {
  border-color: #409eff;
  background-color: #f0f9ff;
}

.weekday-header {
  margin-bottom: 8px;
}

.weekday-checkbox {
  font-weight: 600;
  color: #303133;
}

.time-slots-compact {
  margin-left: 20px;
}

.time-slot-row {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.time-slot-row .el-checkbox {
  margin: 0;
  font-size: 12px;
}

.summary-section {
  border-top: 1px solid #e4e7ed;
  padding-top: 12px;
}

.summary-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
