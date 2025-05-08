<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

interface ScheduleEvent {
  id: number;
  title: string;
  start: string;
  end: string;
  student: string;
  course: string;
  type: 'class' | 'available' | 'other';
  dayOfWeek?: number; // 0-6 表示周日至周六
  startHour?: number;
  endHour?: number;
  column?: number; // 用于防止重叠的列位置
}

// 模拟课表数据
const scheduleEvents = ref<ScheduleEvent[]>([
  {
    id: 1,
    title: '高中数学 - 函数与导数',
    start: '2023-07-10T14:00:00',
    end: '2023-07-10T16:00:00',
    student: '张明',
    course: '高中数学 - 函数与导数',
    type: 'class'
  },
  {
    id: 2,
    title: '高中数学 - 三角函数',
    start: '2023-07-12T16:00:00',
    end: '2023-07-12T18:00:00',
    student: '李华',
    course: '高中数学 - 三角函数',
    type: 'class'
  },
  {
    id: 3,
    title: '可预约时间',
    start: '2023-07-11T09:00:00',
    end: '2023-07-11T12:00:00',
    student: '',
    course: '',
    type: 'available'
  },
  {
    id: 4,
    title: '会议',
    start: '2023-07-13T10:00:00',
    end: '2023-07-13T11:00:00',
    student: '',
    course: '',
    type: 'other'
  },
  {
    id: 5,
    title: '高中英语 - 阅读理解',
    start: '2023-07-10T14:30:00',
    end: '2023-07-10T16:30:00',
    student: '王小红',
    course: '高中英语 - 阅读理解',
    type: 'class'
  },
  {
    id: 6,
    title: '初中物理 - 力学基础',
    start: '2023-07-11T16:00:00',
    end: '2023-07-11T17:30:00',
    student: '刘强',
    course: '初中物理 - 力学基础',
    type: 'class'
  }
])

// 计算每个事件的周几和小时信息，用于在周视图中正确显示
const processedEvents = computed(() => {
  return scheduleEvents.value.map(event => {
    const startDate = new Date(event.start)
    const endDate = new Date(event.end)

    return {
      ...event,
      dayOfWeek: startDate.getDay(), // 0是周日，1是周一，以此类推
      startHour: startDate.getHours() + startDate.getMinutes() / 60,
      endHour: endDate.getHours() + endDate.getMinutes() / 60
    }
  })
})

// 获取特定一天的事件
const getEventsForDay = (dayIndex: number) => {
  // 将 index 转换为对应的 dayOfWeek (0-6)
  const dayOfWeek = (dayIndex + 1) % 7
  return processedEvents.value.filter(event => event.dayOfWeek === dayOfWeek)
}

// 计算事件在某一天中的列位置，防止重叠
const getEventColumn = (event: ScheduleEvent, dayEvents: ScheduleEvent[]) => {
  // 找出与当前事件时间有重叠的所有事件
  const overlappingEvents = dayEvents.filter(e =>
    e.id !== event.id &&
    ((e.startHour! <= event.startHour! && e.endHour! > event.startHour!) ||
     (e.startHour! < event.endHour! && e.endHour! >= event.endHour!) ||
     (e.startHour! >= event.startHour! && e.endHour! <= event.endHour!))
  )

  // 根据重叠事件数量决定列位置
  if (overlappingEvents.length === 0) {
    return 'column-0'
  } else {
    // 找出已被占用的列
    const occupiedColumns = overlappingEvents.map(e => {
      const match = (e.column !== undefined) ?
        e.column :
        Number((Array.from(document.querySelectorAll(`.schedule-event[data-id="${e.id}"]`)[0]?.classList || [])
          .find(c => c.startsWith('column-')) || 'column-0').replace('column-', ''))
      return match
    })

    // 找到第一个未被占用的列
    let column = 0
    while (occupiedColumns.includes(column)) {
      column++
    }

    // 更新事件的列位置
    event.column = column
    return `column-${column}`
  }
}

// 新增可预约时间表单
const availableTimeForm = ref({
  date: '',
  startTime: '',
  endTime: '',
  repeat: false,
  repeatDays: []
})

// 对话框控制
const availableTimeDialogVisible = ref(false)
const eventDetailDialogVisible = ref(false)
const selectedEvent = ref<ScheduleEvent | null>(null)

// 打开新增可预约时间对话框
const openAddAvailableTimeDialog = () => {
  availableTimeForm.value = {
    date: '',
    startTime: '',
    endTime: '',
    repeat: false,
    repeatDays: []
  }
  availableTimeDialogVisible.value = true
}

// 添加可预约时间
const addAvailableTime = () => {
  // 模拟处理表单提交
  ElMessage.success('可预约时间设置成功')
  availableTimeDialogVisible.value = false

  // 在实际应用中这里需要将数据添加到scheduleEvents中
  const newEvent: ScheduleEvent = {
    id: scheduleEvents.value.length + 1,
    title: '可预约时间',
    start: `${availableTimeForm.value.date}T${availableTimeForm.value.startTime}:00`,
    end: `${availableTimeForm.value.date}T${availableTimeForm.value.endTime}:00`,
    student: '',
    course: '',
    type: 'available'
  }

  scheduleEvents.value.push(newEvent)
}

// 查看日程详情
const viewEventDetail = (event: ScheduleEvent) => {
  selectedEvent.value = event
  eventDetailDialogVisible.value = true
}

// 删除日程
const deleteEvent = (eventId: number) => {
  ElMessageBox.confirm('确认删除此日程？', '确认信息', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = scheduleEvents.value.findIndex(e => e.id === eventId)
    if (index !== -1) {
      scheduleEvents.value.splice(index, 1)
      ElMessage.success('日程已删除')
      eventDetailDialogVisible.value = false
    }
  }).catch(() => {
    // 用户取消删除
  })
}

// 获取事件类型对应的样式类
const getEventClass = (type: string): string => {
  switch (type) {
    case 'class':
      return 'class-event'
    case 'available':
      return 'available-event'
    case 'other':
      return 'other-event'
    default:
      return ''
  }
}

// 获取事件类型对应的标签类型
const getEventTagType = (type: string): string => {
  switch (type) {
    case 'class':
      return 'success'
    case 'available':
      return 'info'
    case 'other':
      return 'warning'
    default:
      return 'info'
  }
}

// 获取事件类型名称
const getEventTypeName = (type: string): string => {
  switch (type) {
    case 'class':
      return '课程'
    case 'available':
      return '可预约'
    case 'other':
      return '其他'
    default:
      return '未知'
  }
}

// 计算事件在时间表中的位置和大小
const calculateEventStyle = (event: ScheduleEvent) => {
  if (event.startHour === undefined || event.endHour === undefined) return {}

  const startTime = event.startHour - 8 // 8:00 作为起始时间
  const duration = event.endHour - event.startHour

  // 从时间计算顶部位置和高度
  const topPercentage = (startTime / 12) * 100 // 总共显示12小时
  const heightPercentage = (duration / 12) * 100

  return {
    top: `${topPercentage}%`,
    height: `${heightPercentage}%`,
    zIndex: Math.floor(event.startHour) // 确保早开始的事件在下层
  }
}

// 格式化事件时间
const formatEventTime = (start: string, end: string): string => {
  const startTime = new Date(start)
  const endTime = new Date(end)

  const formatTime = (date: Date) => {
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes}`
  }

  return `${formatTime(startTime)} - ${formatTime(endTime)}`
}

// 格式化日期时间
const formatDateTime = (dateTime: string): string => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 星期几映射
const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

// 生成时间段数组
const timeSlots = computed(() => {
  const slots = []
  for (let i = 8; i <= 20; i++) {
    slots.push(`${i}:00`)
  }
  return slots
})

// 当前日期
const currentDate = ref(new Date())
const currentWeekDays = computed(() => {
  const days = []
  const current = new Date(currentDate.value)
  const day = current.getDay() // 0是周日，1是周一

  // 调整到本周一
  current.setDate(current.getDate() - (day === 0 ? 6 : day - 1))

  // 生成本周七天的日期
  for (let i = 0; i < 7; i++) {
    const date = new Date(current)
    date.setDate(date.getDate() + i)
    days.push(date)
  }

  return days
})

// 格式化日期显示
const formatDayHeader = (date: Date) => {
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}/${day}`
}

// 检查日期是否是今天
const isToday = (date: Date) => {
  const today = new Date()
  return date.getDate() === today.getDate() &&
         date.getMonth() === today.getMonth() &&
         date.getFullYear() === today.getFullYear()
}
</script>

<template>
  <div class="teacher-schedule">
    <div class="schedule-header">
      <h2>课表管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="openAddAvailableTimeDialog">
          <el-icon><Plus /></el-icon>设置可预约时间
        </el-button>
      </div>
    </div>

    <!-- 周视图 -->
    <div class="week-view">
      <div class="time-labels">
        <div class="day-header empty-header"></div>
        <div v-for="time in timeSlots" :key="time" class="time-label">
          {{ time }}
        </div>
      </div>

      <div class="days-container">
        <div v-for="(day, index) in currentWeekDays" :key="index" class="day-column">
          <div class="day-header" :class="{ 'today': isToday(day) }">
            {{ dayNames[(index + 1) % 7] }}
            <div class="day-date">{{ formatDayHeader(day) }}</div>
          </div>

          <div class="time-grid">
            <div
              v-for="event in getEventsForDay(index)"
              :key="event.id"
              class="schedule-event"
              :class="[getEventClass(event.type), getEventColumn(event, getEventsForDay(index))]"
              :style="calculateEventStyle(event)"
              @click="viewEventDetail(event)"
            >
              <div class="event-content">
                <div class="event-title">{{ event.title }}</div>
                <div class="event-time">{{ formatEventTime(event.start, event.end) }}</div>
                <div v-if="event.student" class="event-student">学员: {{ event.student }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 可预约时间对话框 -->
    <el-dialog
      v-model="availableTimeDialogVisible"
      title="设置可预约时间"
      width="500px"
    >
      <el-form :model="availableTimeForm" label-width="100px">
        <el-form-item label="日期">
          <el-date-picker
            v-model="availableTimeForm.date"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker
            v-model="availableTimeForm.startTime"
            format="HH:mm"
            placeholder="选择时间"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker
            v-model="availableTimeForm.endTime"
            format="HH:mm"
            placeholder="选择时间"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="重复">
          <el-switch v-model="availableTimeForm.repeat" />
        </el-form-item>
        <el-form-item v-if="availableTimeForm.repeat" label="重复日">
          <el-checkbox-group v-model="availableTimeForm.repeatDays">
            <el-checkbox label="1">周一</el-checkbox>
            <el-checkbox label="2">周二</el-checkbox>
            <el-checkbox label="3">周三</el-checkbox>
            <el-checkbox label="4">周四</el-checkbox>
            <el-checkbox label="5">周五</el-checkbox>
            <el-checkbox label="6">周六</el-checkbox>
            <el-checkbox label="0">周日</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="availableTimeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addAvailableTime">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 日程详情对话框 -->
    <el-dialog
      v-model="eventDetailDialogVisible"
      title="日程详情"
      width="400px"
      destroy-on-close
    >
      <div v-if="selectedEvent" class="event-detail">
        <div class="detail-row">
          <span class="detail-label">日程类型:</span>
          <el-tag :type="getEventTagType(selectedEvent.type)">
            {{ getEventTypeName(selectedEvent.type) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">标题:</span>
          <span>{{ selectedEvent.title }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">开始时间:</span>
          <span>{{ formatDateTime(selectedEvent.start) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">结束时间:</span>
          <span>{{ formatDateTime(selectedEvent.end) }}</span>
        </div>
        <div v-if="selectedEvent.student" class="detail-row">
          <span class="detail-label">学员:</span>
          <span>{{ selectedEvent.student }}</span>
        </div>
        <div v-if="selectedEvent.course" class="detail-row">
          <span class="detail-label">课程:</span>
          <span>{{ selectedEvent.course }}</span>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="eventDetailDialogVisible = false">关闭</el-button>
          <el-button
            v-if="selectedEvent && selectedEvent.type === 'available'"
            type="danger"
            @click="deleteEvent(selectedEvent.id)">
            删除
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.teacher-schedule {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.schedule-header h2 {
  font-size: 24px;
  color: var(--el-color-primary);
  margin: 0;
  border-left: 4px solid var(--el-color-primary);
  padding-left: 10px;
}

.week-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
}

.time-labels {
  display: flex;
  height: 60px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.empty-header {
  width: 80px;
  border-right: 1px solid var(--el-border-color-light);
}

.time-label {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.days-container {
  display: flex;
  flex: 1;
  overflow-y: auto;
  min-height: 600px;
}

.day-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  border-right: 1px solid var(--el-border-color-light);
  min-width: 120px;
}

.day-column:last-child {
  border-right: none;
}

.day-header {
  height: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: var(--el-color-info-light-9);
  color: #606266;
  font-weight: bold;
  border-bottom: 1px solid var(--el-border-color-light);
  position: sticky;
  top: 0;
  z-index: 2;
}

.day-header.today {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.day-date {
  font-size: 12px;
  font-weight: normal;
  margin-top: 4px;
}

.time-grid {
  position: relative;
  flex: 1;
  height: 720px; /* 12小时 * 60px */
}

.schedule-event {
  position: absolute;
  left: 4px;
  right: 4px;
  border-radius: 4px;
  padding: 8px;
  color: white;
  font-size: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  word-break: break-word;
}

.schedule-event:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
  z-index: 10;
}

/* 处理事件重叠 */
.column-0 {
  left: 4px;
  right: 50%;
}

.column-1 {
  left: 50%;
  right: 4px;
}

.column-2 {
  left: 33%;
  right: 33%;
}

.class-event {
  background-color: var(--el-color-success);
  border-left: 4px solid var(--el-color-success-dark-2);
}

.available-event {
  background-color: var(--el-color-info);
  border-left: 4px solid var(--el-color-info-dark-2);
}

.other-event {
  background-color: var(--el-color-warning);
  border-left: 4px solid var(--el-color-warning-dark-2);
}

.event-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.event-title {
  font-weight: bold;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-time {
  font-size: 11px;
  margin-bottom: 4px;
}

.event-student {
  font-size: 11px;
}

.event-detail {
  padding: 10px;
}

.detail-row {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}

.detail-label {
  width: 80px;
  font-weight: bold;
  color: #606266;
}

@media (max-width: 768px) {
  .days-container {
    overflow-x: auto;
  }

  .day-column {
    min-width: 150px;
  }

  .schedule-event {
    padding: 6px;
  }

  .event-time, .event-student {
    font-size: 10px;
  }
}
</style>
