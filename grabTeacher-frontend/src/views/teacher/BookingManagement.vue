<template>
  <div class="teacher-booking-management">
    <div class="page-header">
      <h1>调课记录</h1>
      <div class="header-actions">
        <el-select v-model="statusFilter" placeholder="审批状态" style="width: 120px; margin-right: 12px;" @change="loadBookings">
          <el-option label="全部" value="" />
          <el-option label="待审批" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
        <el-select v-model="yearFilter" placeholder="年份" style="width: 110px; margin-right: 12px;" @change="loadBookings">
          <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
        </el-select>
        <el-select v-model="monthFilter" placeholder="月份" style="width: 110px; margin-right: 12px;" @change="loadBookings">
          <el-option label="全部" :value="null" />
          <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
        </el-select>
        <el-select v-model="requestTypeFilter" placeholder="类型" style="width: 130px; margin-right: 12px;" @change="loadBookings">
          <el-option label="全部" value="" />
          <el-option label="调课(单次)" value="single" />
          <el-option label="调课(周期)" value="recurring" />
          <el-option label="请假/取消" value="cancel" />
        </el-select>
        <el-select v-model="applicantTypeFilter" placeholder="申请人" style="width: 130px; margin-right: 12px;" @change="loadBookings">
          <el-option label="全部" value="" />
          <el-option label="学生" value="student" />
          <el-option label="教师" value="teacher" />
        </el-select>
        <el-input v-model="applicantNameFilter" placeholder="申请人姓名" clearable style="width: 160px; margin-right: 12px;" @keyup.enter="loadBookings" />
        <el-input v-model="courseNameFilter" placeholder="课程名称" clearable style="width: 160px; margin-right: 12px;" @keyup.enter="loadBookings" />
        <el-button type="primary" @click="loadBookings" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>



    <!-- 预约列表 -->
    <el-card class="booking-list-card">
      <template #header>
        <div class="card-header">
          <span>调课/请假记录</span>
          <el-badge :value="bookingStats.pending" :hidden="bookingStats.pending === 0" type="warning">
            <el-button type="text" size="small">待处理</el-button>
          </el-badge>
        </div>
      </template>

      <div class="booking-list">
        <div v-for="booking in bookings" :key="booking.id" class="booking-item" :class="{ 'pending': booking.status === 'pending' }">
          <div class="booking-header">
            <div class="student-info">
              <el-avatar :size="48" style="margin-right: 12px;">
                {{ booking.studentName?.charAt(0) || 'S' }}
              </el-avatar>
              <div class="student-details">
                <div class="student-name">{{ booking.studentName }}</div>
                <div class="booking-time">{{ formatDateTime(booking.createdAt) }}</div>
              </div>
            </div>
            <div class="booking-status">
              <el-tag :type="getStatusTagType(booking.status)" size="large">
                {{ getStatusText(booking.status, booking.requestType) }}
              </el-tag>
              <el-tag v-if="booking.requestType === 'cancel'" type="info" size="small" style="margin-left: 8px;">
                已请假
              </el-tag>
            </div>
          </div>

          <div class="booking-content">
            <div class="booking-details">
              <div class="detail-row">
                <span class="label">申请类型：</span>
                <span class="value">{{ booking.requestType === 'single' ? '单次调课' : booking.requestType === 'recurring' ? '周期调课' : '请假/取消' }}</span>
              </div>

              <div class="detail-row">
                <span class="label">课程名称：</span>
                <span class="value">{{ booking.courseTitle || '-' }}</span>
              </div>

              <div class="detail-row">
                <span class="label">学生姓名：</span>
                <span class="value">{{ booking.studentName || '-' }}</span>
              </div>

              <div class="detail-row">
                <span class="label">申请人：</span>
                <span class="value">{{ booking.applicantName }}（{{ booking.applicantType === 'teacher' ? '教师' : '学生' }}）</span>
              </div>

              <div class="detail-row">
                <span class="label">原上课时间：</span>
                <span class="value">{{ booking.originalDate }} {{ booking.originalStartTime }}-{{ booking.originalEndTime }}</span>
              </div>

              <div class="detail-row" v-if="booking.requestType !== 'cancel'">
                <span class="label">新上课时间：</span>
                <span class="value">
                  <template v-if="booking.newDate">
                    {{ booking.newDate }} {{ booking.newStartTime }}-{{ booking.newEndTime }}
                  </template>
                  <template v-else>
                    {{ booking.newWeeklySchedule || '-' }}
                  </template>
                </span>
              </div>
            </div>

            <div class="booking-actions">
              <div class="action-buttons-row">
                <el-button size="small" @click="viewBookingDetail(booking)">
                  详情
                </el-button>

              </div>
              <el-alert
                v-if="booking.status === 'pending'"
                title="预约申请需要管理员审批"
                type="info"
                :closable="false"
                show-icon
                style="margin-top: 8px;"
              />
            </div>
          </div>
        </div>

        <div v-if="bookings.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无调课/请假记录" />
        </div>

        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="pagination-wrapper">
        <el-pagination
          :current-page="pagination.current"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="p=>{ pagination.current=p; loadBookings() }"
          @size-change="s=>{ pagination.size=s; pagination.current=1; loadBookings() }"
        />
      </div>
    </el-card>

    <!-- 预约详情弹窗 -->
    <el-dialog
      v-model="showDetailModal"
      title="调课/请假详情"
      width="600px"
      :before-close="closeDetailModal"
    >
      <div v-if="selectedBooking" class="booking-detail">
        <div class="detail-section">
          <h3>申请信息</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <label>学生姓名：</label>
              <span>{{ selectedBooking.studentName }}</span>
            </div>
            <div class="detail-item">
              <label>申请时间：</label>
              <span>{{ formatDateTime(selectedBooking.createdAt) }}</span>
            </div>
            <div class="detail-item">
              <label>审批状态：</label>
              <el-tag :type="getStatusTagType(selectedBooking.status)">
                {{ getStatusText(selectedBooking.status) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <label>是否试听：</label>
              <span>
                {{ selectedBooking.isTrial ? '是' : '否' }}
                <el-tag v-if="selectedBooking.isTrial" type="warning" size="small" style="margin-left: 8px;">
                  {{ selectedBooking.trialDurationMinutes }}分钟
                </el-tag>
              </span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h3>原课程信息</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <label>申请类型：</label>
              <span>{{ selectedBooking.requestType === 'single' ? '单次调课' : selectedBooking.requestType === 'recurring' ? '周期调课' : '请假/取消' }}</span>
            </div>
            <div class="detail-item">
              <label>课程名称：</label>
              <span>{{ selectedBooking.courseTitle || '-' }}</span>
            </div>
          </div>

          <div class="time-info">
            <div class="time-item">
              <label>原上课日期：</label>
              <span>{{ selectedBooking.originalDate }}</span>
            </div>
            <div class="time-item">
              <label>原上课时间：</label>
              <span>{{ selectedBooking.originalStartTime }} - {{ selectedBooking.originalEndTime }}</span>
            </div>
          </div>

          <div class="time-info" v-if="selectedBooking.requestType !== 'cancel'">
            <div class="time-item">
              <label>新上课日期：</label>
              <span>{{ selectedBooking.newDate || '-' }}</span>
            </div>
            <div class="time-item">
              <label>新上课时间：</label>
              <span>{{ selectedBooking.newStartTime }} - {{ selectedBooking.newEndTime }}</span>
            </div>
            <div class="time-item" v-if="selectedBooking.newWeeklySchedule">
              <label>新周期安排：</label>
              <span>{{ selectedBooking.newWeeklySchedule }}</span>
            </div>
          </div>
        </div>

        <div v-if="selectedBooking.reason" class="detail-section">
          <h3>申请原因</h3>
          <div class="requirements-content">
            {{ selectedBooking.reason }}
          </div>
        </div>

        <div v-if="selectedBooking.adminNotes" class="detail-section">
          <h3>管理员回复</h3>
          <div class="admin-reply-content">
            {{ selectedBooking.adminNotes }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeDetailModal">关闭</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { rescheduleAPI } from '../../utils/api'

// 响应式数据
const loading = ref(false)
const bookings = ref<any[]>([])
const statusFilter = ref('')
const yearFilter = ref<number | ''>('')
const monthFilter = ref<number | null>(null)
const requestTypeFilter = ref<'single' | 'recurring' | 'cancel' | ''>('')
const applicantTypeFilter = ref<'student' | 'teacher' | ''>('')
const applicantNameFilter = ref('')
const courseNameFilter = ref('')

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 6 }).map((_, i) => currentYear - i)

// 分页数据
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 弹窗相关
const showDetailModal = ref(false)
const selectedBooking = ref<any>(null)

// 统计信息
const bookingStats = computed(() => {
  const total = bookings.value.length
  const pending = bookings.value.filter(b => b.status === 'pending').length
  const approved = bookings.value.filter(b => b.status === 'approved').length
  const trial = bookings.value.filter(b => b.isTrial).length

  return { total, pending, approved, trial }
})

// 页面加载时获取数据
onMounted(() => {
  loadBookings()
})

// 加载预约申请
const loadBookings = async () => {
  loading.value = true
  try {
    const result = await rescheduleAPI.getTeacherRequests({
      page: pagination.current,
      size: pagination.size,
      status: statusFilter.value,
      year: yearFilter.value || undefined,
      month: monthFilter.value || undefined,
      requestType: requestTypeFilter.value ? (requestTypeFilter.value === 'cancel' ? 'cancel' : 'reschedule') : undefined,
      applicantType: applicantTypeFilter.value || undefined,
      applicantName: applicantNameFilter.value || undefined,
      courseName: courseNameFilter.value || undefined
    })

    if (result.success && result.data) {
      bookings.value = result.data.records || []
      pagination.total = result.data.total || 0
    } else {
      ElMessage.error(result.message || '获取调课记录失败')
    }
  } catch (error) {
    console.error('获取调课记录失败:', error)
    ElMessage.error('获取调课记录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 查看预约详情
const viewBookingDetail = (booking: any) => {
  selectedBooking.value = booking
  showDetailModal.value = true
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  selectedBooking.value = null
}





// 工具函数
const formatDateTime = (dateTimeStr: string) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatRecurringTime = (weekdays: any, timeSlots: any) => {
  if (!weekdays || !timeSlots) return ''

  const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

  // 处理weekdays，可能是数组或字符串
  let weekdayList = []
  if (Array.isArray(weekdays)) {
    weekdayList = weekdays.map(day => weekdayNames[parseInt(day)])
  } else if (typeof weekdays === 'string') {
    weekdayList = weekdays.split(',').map(day => weekdayNames[parseInt(day)])
  }

  // 处理timeSlots，可能是数组或字符串
  let timeSlotList = []
  if (Array.isArray(timeSlots)) {
    timeSlotList = timeSlots
  } else if (typeof timeSlots === 'string') {
    timeSlotList = timeSlots.split(',')
  }

  return `每周${weekdayList.join('、')} ${timeSlotList.join('、')}`
}

const getStatusTagType = (status: string) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'approved': return 'success'
    case 'rejected': return 'danger'
    case 'cancelled': return 'info'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'pending': return '待审批'
    case 'approved': return '已通过'
    case 'rejected': return '已拒绝'
    case 'cancelled': return '已取消'
    default: return '未知'
  }
}
</script>

<style scoped>
.teacher-booking-management {
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
  transition: transform 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card.pending {
  border-left: 4px solid #e6a23c;
}

.stat-card.approved {
  border-left: 4px solid #67c23a;
}

.stat-card.trial {
  border-left: 4px solid #f56c6c;
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

.stat-card.pending .stat-number {
  color: #e6a23c;
}

.stat-card.approved .stat-number {
  color: #67c23a;
}

.stat-card.trial .stat-number {
  color: #f56c6c;
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

.stat-card.pending .stat-icon {
  color: #e6a23c;
}

.stat-card.approved .stat-icon {
  color: #67c23a;
}

.stat-card.trial .stat-icon {
  color: #f56c6c;
}

.booking-list-card {
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

.booking-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.booking-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  background-color: #fff;
  transition: all 0.3s ease;
}

.booking-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.booking-item.pending {
  border-left: 4px solid #e6a23c;
  background: linear-gradient(90deg, #fdf6ec 0%, #ffffff 20%);
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.student-info {
  display: flex;
  align-items: center;
}

.student-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.student-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.booking-time {
  font-size: 12px;
  color: #909399;
}

.booking-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.booking-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.booking-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.detail-row .label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
  flex-shrink: 0;
}

.detail-row .value {
  color: #303133;
  line-height: 1.5;
}

.detail-row .value.requirements {
  background-color: #f0f9ff;
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.detail-row .value.teacher-reply {
  background-color: #f0f9ff;
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid #67c23a;
}

.admin-notes {
  background-color: #fdf6ec;
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid #e6a23c;
}

.booking-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.action-buttons-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.empty-state,
.loading-state {
  padding: 40px 0;
  text-align: center;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.booking-detail {
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

.time-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.time-item {
  display: flex;
  align-items: center;
}

.time-item label {
  font-weight: 500;
  color: #606266;
  min-width: 80px;
  margin-right: 8px;
}

.time-item span {
  color: #303133;
}

.requirements-content,
.reply-content,
.admin-reply-content {
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #409eff;
  line-height: 1.6;
  color: #303133;
}

.reply-content {
  border-left-color: #67c23a;
}

.admin-reply-content {
  border-left-color: #e6a23c;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .teacher-booking-management {
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

  .booking-content {
    flex-direction: column;
    gap: 16px;
  }

  .booking-actions {
    flex-direction: row;
    justify-content: flex-end;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }

  .booking-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .booking-actions {
    flex-direction: column;
    width: 100%;
  }
}
</style>
