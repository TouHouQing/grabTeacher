<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface ScheduleEvent {
  id: number;
  title: string;
  start: string;
  end: string;
  student: string;
  course: string;
  type: 'class' | 'available' | 'other';
}

// 模拟课表数据
const scheduleEvents = ref<ScheduleEvent[]>([
  {
    id: 1,
    title: '初中数学 - 函数与导数',
    start: '2023-07-10T14:00:00',
    end: '2023-07-10T16:00:00',
    student: '张明',
    course: '初中数学 - 函数与导数',
    type: 'class'
  },
  {
    id: 2,
    title: '初中数学 - 三角函数',
    start: '2023-07-12T16:00:00',
    end: '2023-07-12T18:00:00',
    student: '李华',
    course: '初中数学 - 三角函数',
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
  }
])

// 当前周课表
const currentWeekSchedule = computed(() => {
  // 这里应该根据当前日期计算本周的课程，这里简化处理
  return scheduleEvents.value
})

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
</script>

<template>
  <div class="teacher-schedule">
    <h2>课表管理</h2>

    <div class="schedule-actions">
      <el-button type="primary" @click="openAddAvailableTimeDialog">
        <el-icon><Plus /></el-icon>
        添加可预约时间
      </el-button>
    </div>

    <!-- 周视图 -->
    <div class="week-view">
      <div class="week-header">
        <div class="time-column"></div>
        <div class="day-column">周一</div>
        <div class="day-column">周二</div>
        <div class="day-column">周三</div>
        <div class="day-column">周四</div>
        <div class="day-column">周五</div>
        <div class="day-column">周六</div>
        <div class="day-column">周日</div>
      </div>

      <div class="week-body">
        <div class="time-slots">
          <div v-for="hour in 12" :key="hour" class="time-slot">
            {{ hour + 7 }}:00
          </div>
        </div>

        <div class="day-slots">
          <div v-for="day in 7" :key="day" class="day-slot">
            <div
              v-for="event in currentWeekSchedule"
              :key="event.id"
              class="schedule-event"
              :class="getEventClass(event.type)"
              @click="viewEventDetail(event)"
            >
              <div class="event-time">{{ formatDateTime(event.start).split(' ')[1] }} - {{ formatDateTime(event.end).split(' ')[1] }}</div>
              <div class="event-title">{{ event.title }}</div>
              <div v-if="event.student" class="event-student">学生: {{ event.student }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <div class="list-view">
      <h3>近期课程安排</h3>
      <el-table :data="currentWeekSchedule" style="width: 100%">
        <el-table-column label="日期" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.start).split(' ')[0] }}
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="scope">
            {{ formatDateTime(scope.row.start).split(' ')[1] }} - {{ formatDateTime(scope.row.end).split(' ')[1] }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="student" label="学生" width="100" />
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-tag
              :type="scope.row.type === 'class' ? 'primary' : scope.row.type === 'available' ? 'success' : 'info'"
            >
              {{
                scope.row.type === 'class' ? '上课' :
                scope.row.type === 'available' ? '可预约' : '其他'
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              @click="viewEventDetail(scope.row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加可预约时间对话框 -->
    <el-dialog
      v-model="availableTimeDialogVisible"
      title="添加可预约时间"
      width="500px"
    >
      <el-form :model="availableTimeForm" label-width="100px">
        <el-form-item label="日期" required>
          <el-date-picker
            v-model="availableTimeForm.date"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-time-picker
            v-model="availableTimeForm.startTime"
            placeholder="选择时间"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-time-picker
            v-model="availableTimeForm.endTime"
            placeholder="选择时间"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="是否重复">
          <el-switch v-model="availableTimeForm.repeat" />
        </el-form-item>
        <el-form-item label="重复日期" v-if="availableTimeForm.repeat">
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
          <el-button type="primary" @click="addAvailableTime">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 日程详情对话框 -->
    <el-dialog
      v-model="eventDetailDialogVisible"
      title="日程详情"
      width="500px"
    >
      <div v-if="selectedEvent" class="event-detail">
        <div class="detail-item">
          <span class="label">标题：</span>
          <span>{{ selectedEvent.title }}</span>
        </div>
        <div class="detail-item">
          <span class="label">类型：</span>
          <span>
            {{ selectedEvent.type === 'class' ? '上课' : selectedEvent.type === 'available' ? '可预约' : '其他' }}
          </span>
        </div>
        <div class="detail-item">
          <span class="label">开始时间：</span>
          <span>{{ formatDateTime(selectedEvent.start) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">结束时间：</span>
          <span>{{ formatDateTime(selectedEvent.end) }}</span>
        </div>
        <div v-if="selectedEvent.student" class="detail-item">
          <span class="label">学生：</span>
          <span>{{ selectedEvent.student }}</span>
        </div>
        <div v-if="selectedEvent.course" class="detail-item">
          <span class="label">课程：</span>
          <span>{{ selectedEvent.course }}</span>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button
            v-if="selectedEvent && selectedEvent.type !== 'class'"
            type="danger"
            @click="deleteEvent(selectedEvent?.id)"
          >
            删除
          </el-button>
          <el-button @click="eventDetailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.teacher-schedule {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

h3 {
  margin: 30px 0 20px;
  font-size: 18px;
  color: #333;
}

.schedule-actions {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

.week-view {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.week-header {
  display: flex;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.time-column {
  width: 60px;
  border-right: 1px solid #ebeef5;
}

.day-column {
  flex: 1;
  padding: 12px;
  text-align: center;
  font-weight: bold;
  color: #606266;
  border-right: 1px solid #ebeef5;
}

.week-body {
  display: flex;
  height: 400px;
  position: relative;
}

.time-slots {
  width: 60px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.time-slot {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #909399;
  border-bottom: 1px solid #ebeef5;
}

.day-slots {
  flex: 1;
  display: flex;
}

.day-slot {
  flex: 1;
  position: relative;
  border-right: 1px solid #ebeef5;
}

.schedule-event {
  position: absolute;
  width: 90%;
  left: 5%;
  border-radius: 4px;
  padding: 6px;
  font-size: 12px;
  color: #fff;
  overflow: hidden;
  cursor: pointer;
}

.class-event {
  background-color: #409EFF;
}

.available-event {
  background-color: #67C23A;
}

.other-event {
  background-color: #909399;
}

.event-time {
  font-weight: bold;
  margin-bottom: 5px;
}

.event-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 5px;
}

.event-student {
  font-size: 11px;
}

.detail-item {
  margin-bottom: 15px;
  display: flex;
}

.label {
  font-weight: bold;
  color: #606266;
  width: 80px;
  text-align: right;
  margin-right: 10px;
}

@media (max-width: 768px) {
  .week-view {
    display: none;
  }
}
</style>
