<template>
  <div class="teacher-schedule">
    <div class="page-header">
      <h1>我的课表</h1>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="loadSchedules"
          style="width: 300px; margin-right: 16px;"
        />
        <el-button type="primary" @click="loadSchedules" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-number">{{ scheduleStats.total }}</div>
          <div class="stat-label">总课程数</div>
        </div>
        <el-icon class="stat-icon"><Calendar /></el-icon>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-number">{{ scheduleStats.upcoming }}</div>
          <div class="stat-label">即将上课</div>
        </div>
        <el-icon class="stat-icon"><Clock /></el-icon>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-number">{{ scheduleStats.completed }}</div>
          <div class="stat-label">已完成</div>
        </div>
        <el-icon class="stat-icon"><Check /></el-icon>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-number">{{ scheduleStats.trial }}</div>
          <div class="stat-label">试听课</div>
        </div>
        <el-icon class="stat-icon"><Star /></el-icon>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-number">{{ teacherAdjustmentTimes ?? '-' }}</div>
          <div class="stat-label">本月调课剩余</div>
        </div>
        <el-icon class="stat-icon"><Refresh /></el-icon>
      </el-card>
    </div>

    <!-- 课表列表 -->
    <el-card class="schedule-list-card">
      <template #header>
        <div class="card-header">
          <span>课程安排</span>
          <el-select v-model="statusFilter" placeholder="筛选状态" style="width: 120px;" @change="loadSchedules">
            <el-option label="全部" value="" />
            <el-option label="进行中" value="progressing" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </div>
      </template>

      <el-table
        :data="schedules"
        v-loading="loading"
        stripe
        style="width: 100%"
        :default-sort="{ prop: 'scheduledDate', order: 'ascending' }"
      >
        <el-table-column prop="scheduledDate" label="上课日期" width="120" sortable>
          <template #default="{ row }">
            <div class="date-cell">
              <el-tag :type="getDateTagType(row.scheduledDate)" size="small">
                {{ formatDate(row.scheduledDate) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="startTime" label="上课时间" width="180">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Clock /></el-icon>
              {{ row.startTime }} - {{ row.endTime }}
              <el-tag v-if="row.isTrial" type="warning" size="small" style="margin-left: 8px;">
                试听课
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="studentName" label="学生" width="120">
          <template #default="{ row }">
            <div class="student-cell">
              <el-avatar :size="32" style="margin-right: 8px;">
                {{ row.studentName?.charAt(0) || 'S' }}
              </el-avatar>
              {{ row.studentName }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="courseTitle" label="课程" min-width="200">
          <template #default="{ row }">
            <div class="course-cell">
              <div class="course-title">{{ row.courseTitle || '自定义课程' }}</div>
              <div class="course-subject">{{ row.subjectName }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="sessionNumber" label="课程进度" width="100">
          <template #default="{ row }">
            <div v-if="row.totalTimes">
              {{ row.sessionNumber || 1 }} / {{ row.totalTimes }}
            </div>
            <div v-else>单次课程</div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="primary"
                size="small"
                @click="viewScheduleDetail(row)"
              >
                详情
              </el-button>
              <el-button
                v-if="row.status === 'progressing'"
                type="warning"
                size="small"
                @click="openRescheduleModal(row)"
              >
                <el-icon><Refresh /></el-icon>
                调课
              </el-button>
              <el-button
                v-if="row.status === 'progressing' && canAddNotes(row)"
                type="success"
                size="small"
                @click="addTeacherNotes(row)"
              >
                课后记录
              </el-button>
              <el-button
                v-if="row.status === 'completed' || (row.status === 'progressing' && canAddNotes(row))"
                type="warning"
                size="small"
                @click="openGradeModal(row)"
              >
                录入成绩
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="schedules.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无课程安排" />
      </div>
    </el-card>

    <!-- 课程详情弹窗 -->
    <el-dialog
      v-model="showDetailModal"
      title="课程详情"
      width="600px"
      :before-close="closeDetailModal"
    >
      <div v-if="selectedSchedule" class="schedule-detail">
        <div class="detail-section">
          <h3>基本信息</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <label>上课日期：</label>
              <span>{{ formatDate(selectedSchedule.scheduledDate) }}</span>
            </div>
            <div class="detail-item">
              <label>上课时间：</label>
              <span>{{ selectedSchedule.startTime }} - {{ selectedSchedule.endTime }}</span>
            </div>
            <div class="detail-item">
              <label>课程时长：</label>
              <span>{{ selectedSchedule.durationMinutes }}分钟</span>
            </div>
            <div class="detail-item">
              <label>学生姓名：</label>
              <span>{{ selectedSchedule.studentName }}</span>
            </div>
            <div class="detail-item">
              <label>课程类型：</label>
              <span>
                {{ selectedSchedule.isTrial ? '试听课' : '正式课程' }}
                <el-tag v-if="selectedSchedule.isTrial" type="warning" size="small" style="margin-left: 8px;">
                  免费试听
                </el-tag>
              </span>
            </div>
            <div class="detail-item">
              <label>课程状态：</label>
              <el-tag :type="getStatusTagType(selectedSchedule.status)">
                {{ getStatusText(selectedSchedule.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="selectedSchedule.courseTitle">
          <h3>课程信息</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <label>课程标题：</label>
              <span>{{ selectedSchedule.courseTitle }}</span>
            </div>
            <div class="detail-item">
              <label>科目：</label>
              <span>{{ selectedSchedule.subjectName }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="selectedSchedule.teacherNotes">
          <h3>课后记录</h3>
          <div class="notes-content">
            {{ selectedSchedule.teacherNotes }}
          </div>
        </div>

        <div class="detail-section" v-if="selectedSchedule.studentFeedback">
          <h3>学生反馈</h3>
          <div class="feedback-content">
            {{ selectedSchedule.studentFeedback }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeDetailModal">关闭</el-button>
        <el-button
          v-if="selectedSchedule && selectedSchedule.status === 'progressing' && canAddNotes(selectedSchedule)"
          type="primary"
          @click="addTeacherNotes(selectedSchedule)"
        >
          添加课后记录
        </el-button>
      </template>
    </el-dialog>

    <!-- 调课弹窗 -->
    <RescheduleModal
      v-model="showRescheduleModal"
      :course="rescheduleCourse"
      :is-teacher="true"
      @success="handleRescheduleSuccess"
    />

    <!-- 课后记录弹窗 -->
    <el-dialog
      v-model="showNotesModal"
      title="课后记录"
      width="500px"
      :before-close="closeNotesModal"
    >
      <el-form :model="notesForm" label-width="100px">
        <el-form-item label="课后记录">
          <el-input
            v-model="notesForm.teacherNotes"
            type="textarea"
            :rows="6"
            placeholder="请输入课后记录和反馈..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeNotesModal">取消</el-button>
        <el-button type="primary" @click="saveTeacherNotes" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 录入成绩弹窗 -->
    <el-dialog
      v-model="showGradeModal"
      title="录入课程成绩"
      width="500px"
      :before-close="closeGradeModal"
    >
      <div v-if="selectedSchedule" class="grade-form-header">
        <div class="course-info">
          <h4>{{ selectedSchedule.courseTitle || '自定义课程' }}</h4>
          <p>{{ selectedSchedule.studentName }} | {{ selectedSchedule.subjectName }}</p>
          <p>{{ formatDate(selectedSchedule.scheduledDate) }} {{ selectedSchedule.startTime }}-{{ selectedSchedule.endTime }}</p>
        </div>
      </div>

      <el-form :model="gradeForm" :rules="gradeRules" ref="gradeFormRef" label-width="100px">
        <el-form-item label="课程成绩" prop="score">
          <el-input-number
            v-model="gradeForm.score"
            :min="0"
            :max="100"
            :precision="1"
            :step="0.5"
            placeholder="请输入成绩 (0-100)"
            style="width: 100%"
          />
          <div class="score-hint">成绩范围：0-100分，支持一位小数</div>
        </el-form-item>

        <el-form-item label="评价建议" prop="teacherComment">
          <el-input
            v-model="gradeForm.teacherComment"
            type="textarea"
            :rows="4"
            placeholder="请输入对学生本节课的评价和建议..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeGradeModal">取消</el-button>
        <el-button type="primary" @click="saveGrade" :loading="saving">
          {{ gradeForm.isEdit ? '更新成绩' : '录入成绩' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Clock, Check, Star, Refresh } from '@element-plus/icons-vue'
import { bookingAPI, apiRequest, teacherAPI } from '@/utils/api'
import RescheduleModal from '@/components/RescheduleModal.vue'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const schedules = ref<any[]>([])
const dateRange = ref<[string, string]>([
  new Date().toISOString().split('T')[0],
  new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0] // 30天后
])
const statusFilter = ref('')
const teacherAdjustmentTimes = ref<number | null>(null)

// 弹窗相关
const showDetailModal = ref(false)
const showNotesModal = ref(false)
const showGradeModal = ref(false)
const showRescheduleModal = ref(false)
const selectedSchedule = ref<any>(null)
const gradeFormRef = ref<any>(null)
const rescheduleCourse = ref<any>(null)

// 表单数据
const notesForm = reactive({
  teacherNotes: ''
})

const gradeForm = reactive({
  scheduleId: null as number | null,
  studentId: null as number | null,
  score: null as number | null,
  teacherComment: '',
  isEdit: false,
  gradeId: null as number | null
})

// 表单验证规则
const gradeRules = {
  score: [
    { required: true, message: '请输入课程成绩', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '成绩应在0-100之间', trigger: 'blur' }
  ]
}

// 统计信息
const scheduleStats = computed(() => {
  const total = schedules.value.length
  const upcoming = schedules.value.filter(s =>
    s.status === 'progressing' && new Date(s.scheduledDate) >= new Date()
  ).length
  const completed = schedules.value.filter(s => s.status === 'completed').length
  const trial = schedules.value.filter(s => s.isTrial).length

  return { total, upcoming, completed, trial }
})

// 页面加载时获取数据
onMounted(() => {
  loadSchedules()
  loadTeacherAdjustmentTimes()
})

// 加载课程安排
const loadSchedules = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }

  loading.value = true
  try {
    const result = await bookingAPI.getTeacherSchedules({
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })

    if (result.success && result.data) {
      let data = result.data

      // 根据状态筛选
      if (statusFilter.value) {
        data = data.filter((schedule: any) => schedule.status === statusFilter.value)
      }

      schedules.value = data
    } else {
      ElMessage.error(result.message || '获取课程安排失败')
    }
  } catch (error) {
    console.error('获取课程安排失败:', error)
    ElMessage.error('获取课程安排失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 加载教师本月剩余调课次数
const loadTeacherAdjustmentTimes = async () => {
  try {
    const res = await teacherAPI.getProfile()
    if (res?.success && res.data) {
      teacherAdjustmentTimes.value = res.data.adjustmentTimes ?? null
    }
  } catch (e) {
    teacherAdjustmentTimes.value = null
  }
}

// 查看课程详情
const viewScheduleDetail = (schedule: any) => {
  selectedSchedule.value = schedule
  showDetailModal.value = true
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedSchedule.value = null
}

// 添加课后记录
const addTeacherNotes = (schedule: any) => {
  selectedSchedule.value = schedule
  notesForm.teacherNotes = schedule.teacherNotes || ''
  showNotesModal.value = true
}

// 关闭记录弹窗
const closeNotesModal = () => {
  showNotesModal.value = false
  notesForm.teacherNotes = ''
}

// 保存课后记录
const saveTeacherNotes = async () => {
  if (!notesForm.teacherNotes.trim()) {
    ElMessage.warning('请输入课后记录')
    return
  }

  saving.value = true
  try {
    // 这里需要调用更新课程记录的API
    // const result = await scheduleAPI.updateNotes(selectedSchedule.value.id, {
    //   teacherNotes: notesForm.teacherNotes
    // })

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 更新本地数据
    if (selectedSchedule.value) {
      selectedSchedule.value.teacherNotes = notesForm.teacherNotes
      const index = schedules.value.findIndex(s => s.id === selectedSchedule.value.id)
      if (index !== -1) {
        schedules.value[index].teacherNotes = notesForm.teacherNotes
      }
    }

    ElMessage.success('课后记录保存成功')
    closeNotesModal()
  } catch (error) {
    console.error('保存课后记录失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// 录入成绩相关方法
const openGradeModal = async (schedule: any) => {
  selectedSchedule.value = schedule

  // 重置表单
  gradeForm.scheduleId = schedule.id
  gradeForm.studentId = schedule.studentId
  gradeForm.score = null
  gradeForm.teacherComment = ''
  gradeForm.isEdit = false
  gradeForm.gradeId = null

  // 检查是否已有成绩记录
  try {
    const response = await apiRequest(`/api/lesson-grades/schedule/${schedule.id}/student/${schedule.studentId}`)
    if (response.success && response.data) {
      // 已有成绩记录，设置为编辑模式
      const existingGrade = response.data
      gradeForm.score = existingGrade.score
      gradeForm.teacherComment = existingGrade.teacherComment || ''
      gradeForm.isEdit = true
      gradeForm.gradeId = existingGrade.id
    }
  } catch (error) {
    // 没有成绩记录，保持新建模式
    console.log('暂无成绩记录，进入新建模式')
  }

  showGradeModal.value = true
}

const closeGradeModal = () => {
  showGradeModal.value = false
  selectedSchedule.value = null

  // 重置表单
  gradeForm.scheduleId = null
  gradeForm.studentId = null
  gradeForm.score = null
  gradeForm.teacherComment = ''
  gradeForm.isEdit = false
  gradeForm.gradeId = null

  // 清除表单验证
  if (gradeFormRef.value) {
    gradeFormRef.value.resetFields()
  }
}

const saveGrade = async () => {
  if (!gradeFormRef.value) return

  // 表单验证
  const valid = await gradeFormRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const requestData = {
      scheduleId: gradeForm.scheduleId,
      studentId: gradeForm.studentId,
      score: gradeForm.score,
      teacherComment: gradeForm.teacherComment
    }

    let response
    if (gradeForm.isEdit && gradeForm.gradeId) {
      // 更新成绩
      response = await apiRequest('/api/lesson-grades', {
        method: 'PUT',
        data: {
          id: gradeForm.gradeId,
          score: gradeForm.score,
          teacherComment: gradeForm.teacherComment
        }
      })
    } else {
      // 新建成绩
      response = await apiRequest('/api/lesson-grades', {
        method: 'POST',
        data: requestData
      })
    }

    if (response.success) {
      ElMessage.success(gradeForm.isEdit ? '成绩更新成功' : '成绩录入成功')
      closeGradeModal()
      // 可以选择刷新课程列表
      // loadSchedules()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error: any) {
    console.error('保存成绩失败:', error)
    ElMessage.error(error.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// 工具函数
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  const today = new Date()
  const tomorrow = new Date(today)
  tomorrow.setDate(today.getDate() + 1)

  if (date.toDateString() === today.toDateString()) {
    return '今天'
  } else if (date.toDateString() === tomorrow.toDateString()) {
    return '明天'
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }
}

const getDateTagType = (dateStr: string) => {
  const date = new Date(dateStr)
  const today = new Date()

  if (date.toDateString() === today.toDateString()) {
    return 'danger'
  } else if (date > today) {
    return 'success'
  } else {
    return 'info'
  }
}

const getStatusTagType = (status: string) => {
  switch (status) {
    case 'progressing': return 'success'
    case 'completed': return 'info'
    case 'cancelled': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'progressing': return '进行中'
    case 'completed': return '已完成'
    case 'cancelled': return '已取消'
    default: return '未知'
  }
}

const canAddNotes = (schedule: any) => {
  // 只有当天或之前的课程才能添加课后记录
  const scheduleDate = new Date(schedule.scheduledDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return scheduleDate <= today
}

// 调课相关方法
const openRescheduleModal = (schedule: any) => {
  // 构建课程对象，适配调课组件的接口
  // 确保schedule对象有必要的字段
  const enhancedSchedule = {
    id: schedule.id,
    teacherId: schedule.teacherId,
    teacherName: schedule.teacherName || '当前教师',
    studentId: schedule.studentId,
    studentName: schedule.studentName,
    courseId: schedule.courseId,
    courseTitle: schedule.courseTitle || '自定义课程',
    subjectName: schedule.subjectName || '未知科目',
    scheduledDate: schedule.scheduledDate,
    startTime: schedule.startTime,
    endTime: schedule.endTime,
    durationMinutes: schedule.durationMinutes || 120,
    totalTimes: schedule.totalTimes || 1,
    status: schedule.status || 'progressing',
    teacherNotes: schedule.teacherNotes,
    studentFeedback: schedule.studentFeedback,
    createdAt: schedule.createdAt,
    bookingRequestId: schedule.bookingRequestId,
    bookingSource: schedule.bookingSource,
    isTrial: schedule.isTrial,
    sessionNumber: schedule.sessionNumber || 1,
    courseType: schedule.courseType || 'regular'
  }

  rescheduleCourse.value = {
    id: schedule.courseId || schedule.id,
    title: schedule.courseTitle || '自定义课程',
    teacher: schedule.teacherName || '当前教师',
    teacherId: schedule.teacherId,
    subject: schedule.subjectName || '未知科目',
    remainingLessons: 1, // 单次课程
    weeklySchedule: [],
    schedules: [enhancedSchedule]
  }

  console.log('构建的调课课程数据:', rescheduleCourse.value)
  showRescheduleModal.value = true
}

const handleRescheduleSuccess = () => {
  // 刷新课程列表
  loadSchedules()
}
</script>

<style scoped>
.teacher-schedule {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.stat-icon {
  font-size: 32px;
  color: #409eff;
  opacity: 0.8;
}

.schedule-list-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #303133;
}

.date-cell {
  display: flex;
  align-items: center;
}

.time-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.student-cell {
  display: flex;
  align-items: center;
}

.course-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.course-title {
  font-weight: 500;
  color: #303133;
}

.course-subject {
  font-size: 12px;
  color: #909399;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.schedule-detail {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h3 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.detail-item label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
  margin-right: 8px;
}

.detail-item span {
  color: #303133;
}

.notes-content,
.feedback-content {
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  line-height: 1.6;
  color: #303133;
}

.feedback-content {
  border-left-color: #67c23a;
}

/* 录入成绩弹窗样式 */
.grade-form-header {
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #e6a23c;
}

.grade-form-header .course-info h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.grade-form-header .course-info p {
  margin: 4px 0;
  color: #606266;
  font-size: 14px;
}

.score-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .teacher-schedule {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .header-actions {
    justify-content: center;
  }

  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>
