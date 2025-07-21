<template>
  <div class="teacher-booking-management">
    <div class="page-header">
      <h1>预约管理</h1>
      <div class="header-actions">
        <el-select v-model="statusFilter" placeholder="筛选状态" style="width: 120px; margin-right: 16px;" @change="loadBookings">
          <el-option label="全部" value="" />
          <el-option label="待审批" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-button type="primary" @click="loadBookings" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-number">{{ bookingStats.total }}</div>
          <div class="stat-label">总预约数</div>
        </div>
        <el-icon class="stat-icon"><Calendar /></el-icon>
      </el-card>
      <el-card class="stat-card pending">
        <div class="stat-content">
          <div class="stat-number">{{ bookingStats.pending }}</div>
          <div class="stat-label">待审批</div>
        </div>
        <el-icon class="stat-icon"><Clock /></el-icon>
      </el-card>
      <el-card class="stat-card approved">
        <div class="stat-content">
          <div class="stat-number">{{ bookingStats.approved }}</div>
          <div class="stat-label">已通过</div>
        </div>
        <el-icon class="stat-icon"><Check /></el-icon>
      </el-card>
      <el-card class="stat-card trial">
        <div class="stat-content">
          <div class="stat-number">{{ bookingStats.trial }}</div>
          <div class="stat-label">试听课</div>
        </div>
        <el-icon class="stat-icon"><Star /></el-icon>
      </el-card>
    </div>

    <!-- 预约列表 -->
    <el-card class="booking-list-card">
      <template #header>
        <div class="card-header">
          <span>预约申请</span>
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
                {{ getStatusText(booking.status) }}
              </el-tag>
              <el-tag v-if="booking.isTrial" type="warning" size="small" style="margin-left: 8px;">
                试听课
              </el-tag>
            </div>
          </div>

          <div class="booking-content">
            <div class="booking-details">
              <div class="detail-row">
                <span class="label">预约类型：</span>
                <span class="value">{{ booking.bookingType === 'single' ? '单次预约' : '周期性预约' }}</span>
              </div>

              <div v-if="booking.bookingType === 'single'" class="detail-row">
                <span class="label">上课时间：</span>
                <span class="value">
                  {{ booking.requestedDate }} {{ booking.requestedStartTime }}-{{ booking.requestedEndTime }}
                </span>
              </div>

              <div v-else class="detail-row">
                <span class="label">上课时间：</span>
                <span class="value">
                  {{ formatRecurringTime(booking.recurringWeekdays, booking.recurringTimeSlots) }}
                </span>
              </div>

              <div v-if="booking.bookingType === 'recurring'" class="detail-row">
                <span class="label">课程周期：</span>
                <span class="value">
                  {{ booking.startDate }} 至 {{ booking.endDate }}
                  <span v-if="booking.totalTimes">（共{{ booking.totalTimes }}次课）</span>
                </span>
              </div>

              <div v-if="booking.studentRequirements" class="detail-row">
                <span class="label">学习需求：</span>
                <span class="value requirements">{{ booking.studentRequirements }}</span>
              </div>

              <div v-if="booking.teacherReply" class="detail-row">
                <span class="label">我的回复：</span>
                <span class="value teacher-reply">{{ booking.teacherReply }}</span>
              </div>
            </div>

            <div class="booking-actions">
              <el-button size="small" @click="viewBookingDetail(booking)">
                详情
              </el-button>
              <el-button
                v-if="booking.status === 'pending'"
                type="success"
                size="small"
                @click="approveBooking(booking, 'approved')"
              >
                通过
              </el-button>
              <el-button
                v-if="booking.status === 'pending'"
                type="danger"
                size="small"
                @click="approveBooking(booking, 'rejected')"
              >
                拒绝
              </el-button>
              <el-button
                v-if="booking.status === 'approved'"
                type="primary"
                size="small"
                @click="viewSchedules(booking)"
              >
                查看课表
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="bookings.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无预约申请" />
        </div>

        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadBookings"
          @size-change="loadBookings"
        />
      </div>
    </el-card>

    <!-- 预约详情弹窗 -->
    <el-dialog
      v-model="showDetailModal"
      title="预约详情"
      width="600px"
      :before-close="closeDetailModal"
    >
      <div v-if="selectedBooking" class="booking-detail">
        <div class="detail-section">
          <h3>学生信息</h3>
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
              <label>申请状态：</label>
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
          <h3>课程安排</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <label>预约类型：</label>
              <span>{{ selectedBooking.bookingType === 'single' ? '单次预约' : '周期性预约' }}</span>
            </div>
            <div class="detail-item">
              <label>课程类型：</label>
              <span>{{ selectedBooking.courseTitle || '自定义课程' }}</span>
            </div>
          </div>

          <div v-if="selectedBooking.bookingType === 'single'" class="time-info">
            <div class="time-item">
              <label>上课日期：</label>
              <span>{{ selectedBooking.requestedDate }}</span>
            </div>
            <div class="time-item">
              <label>上课时间：</label>
              <span>{{ selectedBooking.requestedStartTime }} - {{ selectedBooking.requestedEndTime }}</span>
            </div>
          </div>
          <div v-else class="time-info">
            <div class="time-item">
              <label>上课时间：</label>
              <span>{{ formatRecurringTime(selectedBooking.recurringWeekdays, selectedBooking.recurringTimeSlots) }}</span>
            </div>
            <div class="time-item">
              <label>课程周期：</label>
              <span>{{ selectedBooking.startDate }} 至 {{ selectedBooking.endDate }}</span>
            </div>
            <div v-if="selectedBooking.totalTimes" class="time-item">
              <label>总课程数：</label>
              <span>{{ selectedBooking.totalTimes }}次</span>
            </div>
          </div>
        </div>

        <div v-if="selectedBooking.studentRequirements" class="detail-section">
          <h3>学习需求</h3>
          <div class="requirements-content">
            {{ selectedBooking.studentRequirements }}
          </div>
        </div>

        <div v-if="selectedBooking.teacherReply" class="detail-section">
          <h3>我的回复</h3>
          <div class="reply-content">
            {{ selectedBooking.teacherReply }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeDetailModal">关闭</el-button>
        <el-button
          v-if="selectedBooking && selectedBooking.status === 'pending'"
          type="success"
          @click="approveBooking(selectedBooking, 'approved')"
        >
          通过申请
        </el-button>
        <el-button
          v-if="selectedBooking && selectedBooking.status === 'pending'"
          type="danger"
          @click="approveBooking(selectedBooking, 'rejected')"
        >
          拒绝申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog
      v-model="showApprovalModal"
      :title="approvalForm.status === 'approved' ? '通过申请' : '拒绝申请'"
      width="500px"
      :before-close="closeApprovalModal"
    >
      <el-form :model="approvalForm" label-width="100px">
        <el-form-item label="审批结果">
          <el-tag :type="approvalForm.status === 'approved' ? 'success' : 'danger'" size="large">
            {{ approvalForm.status === 'approved' ? '通过申请' : '拒绝申请' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="approvalForm.teacherReply"
            type="textarea"
            :rows="4"
            :placeholder="approvalForm.status === 'approved' ? '请输入通过理由和课程安排说明...' : '请输入拒绝理由...'"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeApprovalModal">取消</el-button>
        <el-button
          :type="approvalForm.status === 'approved' ? 'success' : 'danger'"
          @click="submitApproval"
          :loading="approving"
        >
          确认{{ approvalForm.status === 'approved' ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Clock, Check, Star, Refresh } from '@element-plus/icons-vue'
import { bookingAPI } from '@/utils/api'

// 响应式数据
const loading = ref(false)
const approving = ref(false)
const bookings = ref<any[]>([])
const statusFilter = ref('')

// 分页数据
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 弹窗相关
const showDetailModal = ref(false)
const showApprovalModal = ref(false)
const selectedBooking = ref<any>(null)

// 审批表单
const approvalForm = reactive({
  status: 'approved' as 'approved' | 'rejected',
  teacherReply: ''
})

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
    const result = await bookingAPI.getTeacherRequests({
      page: pagination.current,
      size: pagination.size,
      status: statusFilter.value
    })

    if (result.success && result.data) {
      bookings.value = result.data.records || []
      pagination.total = result.data.total || 0
    } else {
      ElMessage.error(result.message || '获取预约申请失败')
    }
  } catch (error) {
    console.error('获取预约申请失败:', error)
    ElMessage.error('获取预约申请失败，请稍后重试')
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

// 审批预约
const approveBooking = (booking: any, status: 'approved' | 'rejected') => {
  selectedBooking.value = booking
  approvalForm.status = status
  approvalForm.teacherReply = ''
  showApprovalModal.value = true
}

// 关闭审批弹窗
const closeApprovalModal = () => {
  showApprovalModal.value = false
  approvalForm.teacherReply = ''
}

// 提交审批
const submitApproval = async () => {
  if (!approvalForm.teacherReply.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  approving.value = true
  try {
    const result = await bookingAPI.approve(selectedBooking.value.id, {
      status: approvalForm.status,
      teacherReply: approvalForm.teacherReply
    })

    if (result.success) {
      ElMessage.success(`预约申请已${approvalForm.status === 'approved' ? '通过' : '拒绝'}`)
      loadBookings()
      closeApprovalModal()
      if (showDetailModal.value) {
        closeDetailModal()
      }
    } else {
      ElMessage.error(result.message || '审批失败')
    }
  } catch (error) {
    console.error('审批失败:', error)
    ElMessage.error('审批失败，请稍后重试')
  } finally {
    approving.value = false
  }
}

// 查看课表
const viewSchedules = (booking: any) => {
  // 跳转到课表页面
  ElMessage.info('即将跳转到课表页面...')
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

const formatRecurringTime = (weekdays: number[], timeSlots: string[]) => {
  if (!weekdays || !timeSlots) return ''

  const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const dayNames = weekdays.map(day => weekdayNames[day]).join('、')
  const times = timeSlots.join('、')

  return `每周${dayNames} ${times}`
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

.booking-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
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
.reply-content {
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
