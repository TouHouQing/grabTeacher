<template>
  <div class="admin-booking-management">
    <div class="page-header">
      <h2>预约管理</h2>
      <div class="header-actions">
        <el-select v-model="statusFilter" placeholder="筛选状态" style="width: 120px; margin-right: 16px;" @change="loadBookings">
          <el-option label="全部" value="" />
          <el-option label="待审批" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索学生或教师姓名"
          style="width: 200px; margin-right: 16px;"
          @keyup.enter="loadBookings"
          clearable
        />
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
            <div class="booking-info">
              <div class="student-info">
                <el-avatar :size="40" style="margin-right: 12px;">
                  {{ booking.studentName?.charAt(0) || 'S' }}
                </el-avatar>
                <div class="info-details">
                  <div class="student-name">{{ booking.studentName }}</div>
                  <div class="teacher-name">教师：{{ booking.teacherName }}</div>
                </div>
              </div>
              <div class="booking-time">{{ formatDateTime(booking.createdAt) }}</div>
            </div>
            <div class="booking-status">
              <el-tag :type="getStatusTagType(booking.status)" size="large">
                {{ getStatusText(booking.status) }}
              </el-tag>
              <el-tag v-if="booking.isTrial" type="warning" size="large" style="margin-left: 8px;">
                试听课
              </el-tag>
            </div>
          </div>

          <div class="booking-content">
            <div class="booking-main">
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
                    {{ formatRecurringTime(booking.recurringWeekdays, booking.recurringTimeSlots) || '未设置' }}
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

                <div v-if="booking.adminNotes" class="detail-row">
                  <span class="label">管理员备注：</span>
                  <span class="value admin-notes">{{ booking.adminNotes }}</span>
                </div>
              </div>

            </div>

            <div class="booking-actions-right">
              <el-button
                class="action-btn detail-btn"
                size="small"
                @click="viewBookingDetail(booking)"
              >
                <el-icon><View /></el-icon>
                详情
              </el-button>
              <el-button
                v-if="booking.status === 'pending'"
                class="action-btn approve-btn"
                type="success"
                size="small"
                @click="approveBooking(booking, 'approved')"
              >
                <el-icon><Check /></el-icon>
                通过
              </el-button>
              <el-button
                v-if="booking.status === 'pending'"
                class="action-btn reject-btn"
                type="danger"
                size="small"
                @click="approveBooking(booking, 'rejected')"
              >
                <el-icon><Close /></el-icon>
                拒绝
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
          </div>
        </div>

        <div class="detail-section">
          <h3>教师信息</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <label>教师姓名：</label>
              <span>{{ selectedBooking.teacherName }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h3>预约信息</h3>
          <div class="detail-grid">
            <div class="detail-item">
              <label>预约类型：</label>
              <span>{{ selectedBooking.bookingType === 'single' ? '单次预约' : '周期性预约' }}</span>
            </div>
            <div v-if="selectedBooking.isTrial" class="detail-item">
              <label>试听课：</label>
              <el-tag type="warning" size="small">是</el-tag>
            </div>
          </div>
        </div>

        <div v-if="selectedBooking.studentRequirements" class="detail-section">
          <h3>学习需求</h3>
          <p>{{ selectedBooking.studentRequirements }}</p>
        </div>

        <div v-if="selectedBooking.adminNotes" class="detail-section">
          <h3>管理员备注</h3>
          <p>{{ selectedBooking.adminNotes }}</p>
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
        <el-form-item label="管理员备注">
          <el-input
            v-model="approvalForm.adminNotes"
            type="textarea"
            :rows="4"
            :placeholder="approvalForm.status === 'approved' ? '请输入通过理由和相关说明...' : '请输入拒绝理由...'"
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
import { ElMessage } from 'element-plus'
import { Calendar, Clock, Check, Star, Refresh, View, Close } from '@element-plus/icons-vue'
import { bookingAPI } from '@/utils/api'

// 响应式数据
const loading = ref(false)
const approving = ref(false)
const bookings = ref<any[]>([])
const statusFilter = ref('')
const searchKeyword = ref('')

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
  adminNotes: ''
})

// 统计信息
const bookingStats = reactive({
  total: 0,
  pending: 0,
  approved: 0,
  trial: 0
})

// 页面加载时获取数据
onMounted(() => {
  loadBookings()
})

// 加载预约申请
const loadBookings = async () => {
  loading.value = true
  try {
    const result = await bookingAPI.getAdminRequests({
      page: pagination.current,
      size: pagination.size,
      status: statusFilter.value,
      keyword: searchKeyword.value
    })

    if (result.success && result.data) {
      bookings.value = result.data.records || []
      pagination.total = result.data.total || 0
      console.log('管理员预约数据:', result.data)
      console.log('预约列表:', bookings.value)
      console.log('预约列表长度:', bookings.value.length)
      if (bookings.value.length > 0) {
        console.log('第一个预约:', bookings.value[0])
      }

      // 更新统计信息 - 基于当前页面的数据
      const currentPageStats = {
        total: pagination.total,
        pending: bookings.value.filter(b => b.status === 'pending').length,
        approved: bookings.value.filter(b => b.status === 'approved').length,
        trial: bookings.value.filter(b => b.isTrial).length
      }

      Object.assign(bookingStats, currentPageStats)
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

// 加载统计信息
const loadStats = async () => {
  try {
    // 获取所有状态的统计信息
    const [totalResult, pendingResult, approvedResult, trialResult] = await Promise.all([
      bookingAPI.getAdminRequests({ page: 1, size: 1 }),
      bookingAPI.getAdminRequests({ page: 1, size: 1, status: 'pending' }),
      bookingAPI.getAdminRequests({ page: 1, size: 1, status: 'approved' }),
      bookingAPI.getAdminRequests({ page: 1, size: 1000 }) // 获取更多数据来统计试听课
    ])

    if (totalResult.success) {
      bookingStats.total = totalResult.data?.total || 0
    }
    if (pendingResult.success) {
      bookingStats.pending = pendingResult.data?.total || 0
    }
    if (approvedResult.success) {
      bookingStats.approved = approvedResult.data?.total || 0
    }
    if (trialResult.success && trialResult.data?.records) {
      bookingStats.trial = trialResult.data.records.filter(b => b.isTrial).length
    }
  } catch (error) {
    console.error('获取统计信息失败:', error)
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
  approvalForm.adminNotes = ''
  showApprovalModal.value = true
}

// 关闭审批弹窗
const closeApprovalModal = () => {
  showApprovalModal.value = false
  approvalForm.adminNotes = ''
}

// 提交审批
const submitApproval = async () => {
  if (!approvalForm.adminNotes.trim()) {
    ElMessage.warning('请输入管理员备注')
    return
  }

  approving.value = true
  try {
    const result = await bookingAPI.approve(selectedBooking.value.id, {
      status: approvalForm.status,
      adminNotes: approvalForm.adminNotes
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

  return `${weekdayList.join('、')} ${timeSlotList.join('、')}`
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
.admin-booking-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
}

.stat-card.pending {
  border-left: 4px solid #e6a23c;
}

.stat-card.approved {
  border-left: 4px solid #67c23a;
}

.stat-card.trial {
  border-left: 4px solid #409eff;
}

.stat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-icon {
  position: absolute;
  top: 20px;
  right: 20px;
  font-size: 24px;
  color: #c0c4cc;
}

.booking-list-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.booking-list {
  max-height: 600px;
  overflow-y: auto;
}

.booking-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 16px;
  padding: 16px;
  transition: all 0.3s;
}

.booking-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.booking-item.pending {
  border-left: 4px solid #e6a23c;
  background-color: #fdf6ec;
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.booking-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
}

.student-info {
  display: flex;
  align-items: center;
}

.info-details {
  display: flex;
  flex-direction: column;
}

.student-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.teacher-name {
  font-size: 12px;
  color: #909399;
}

.booking-time {
  font-size: 12px;
  color: #909399;
}

.booking-status {
  display: flex;
  align-items: center;
}

.booking-content {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
}

.booking-main {
  flex: 1;
}

.booking-details {
  margin-bottom: 16px;
}

.detail-row {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-row .label {
  color: #606266;
  min-width: 80px;
  font-weight: 500;
}

.detail-row .value {
  color: #303133;
  flex: 1;
}

.requirements {
  background-color: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.admin-notes {
  background-color: #f0f9ff;
  padding: 8px;
  border-radius: 4px;
  border-left: 3px solid #67c23a;
}

.booking-actions-right {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 80px;
  align-items: center;
  justify-content: flex-start;
}

.action-btn {
  width: 80px !important;
  height: 28px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-size: 12px !important;
  font-weight: 400 !important;
  border-radius: 4px !important;
  transition: all 0.2s ease !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) !important;
  padding: 0 !important;
  margin: 0 !important;
  flex-shrink: 0 !important;
}

.action-btn .el-icon {
  margin-right: 4px !important;
  font-size: 12px !important;
  display: inline-flex !important;
  align-items: center !important;
}

.detail-btn {
  background: linear-gradient(135deg, #409eff, #66b3ff) !important;
  border: none !important;
  color: white !important;
}

.detail-btn:hover {
  background: linear-gradient(135deg, #337ecc, #5aa3e6) !important;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.3) !important;
}

.approve-btn {
  background: linear-gradient(135deg, #67c23a, #85ce61) !important;
  border: none !important;
  color: white !important;
}

.approve-btn:hover {
  background: linear-gradient(135deg, #529b2e, #6cb344) !important;
  box-shadow: 0 2px 4px rgba(103, 194, 58, 0.3) !important;
}

.reject-btn {
  background: linear-gradient(135deg, #f56c6c, #f78989) !important;
  border: none !important;
  color: white !important;
}

.reject-btn:hover {
  background: linear-gradient(135deg, #dd6161, #e67c7c) !important;
  box-shadow: 0 2px 4px rgba(245, 108, 108, 0.3) !important;
}

.empty-state, .loading-state {
  padding: 40px;
  text-align: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.booking-detail {
  max-height: 500px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h3 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.detail-item label {
  color: #606266;
  min-width: 80px;
  font-weight: 500;
}

.detail-item span {
  color: #303133;
}

/* 移动端响应式设计 */
@media (max-width: 768px) {
  .booking-content {
    flex-direction: column;
    gap: 12px;
  }

  .booking-actions-right {
    flex-direction: row;
    justify-content: flex-end;
    min-width: auto;
    flex-wrap: wrap;
    gap: 8px;
  }

  .action-btn {
    width: 60px !important;
    height: 26px !important;
    font-size: 11px !important;
    padding: 0 6px !important;
  }

  .action-btn .el-icon {
    margin-right: 2px !important;
    font-size: 10px !important;
  }
}
</style>
