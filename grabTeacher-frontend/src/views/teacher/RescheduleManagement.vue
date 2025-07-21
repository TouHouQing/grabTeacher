<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Clock,
  Check,
  Close,
  View,
  Calendar,
  Timer,
  User,
  Document,
  Refresh
} from '@element-plus/icons-vue'
import { rescheduleAPI } from '@/utils/api'

// 调课申请接口
interface RescheduleRequest {
  id: number
  scheduleId: number
  applicantId: number
  applicantName: string
  applicantType: string
  requestType: 'single' | 'recurring' | 'cancel'
  originalDate: string
  originalStartTime: string
  originalEndTime: string
  newDate?: string
  newStartTime?: string
  newEndTime?: string
  newWeeklySchedule?: string
  reason: string
  urgencyLevel: 'low' | 'medium' | 'high'
  advanceNoticeHours?: number
  status: 'pending' | 'approved' | 'rejected' | 'cancelled'
  statusDisplay: string
  reviewerId?: number
  reviewerName?: string
  reviewNotes?: string
  createdAt: string
  updatedAt: string
  courseTitle: string
  subjectName: string
  teacherName: string
  studentName: string
}

// 响应式数据
const loading = ref(false)
const rescheduleRequests = ref<RescheduleRequest[]>([])
const statusFilter = ref('all')
const currentRequest = ref<RescheduleRequest | null>(null)
const showDetailModal = ref(false)
const showApprovalModal = ref(false)

// 分页配置
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 审批表单
const approvalForm = reactive({
  status: 'approved' as 'approved' | 'rejected',
  reviewNotes: '',
  compensationAmount: 0,
  effectiveDate: ''
})

// 状态选项
const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待审批', value: 'pending' },
  { label: '已同意', value: 'approved' },
  { label: '已拒绝', value: 'rejected' },
  { label: '已取消', value: 'cancelled' }
]

// 紧急程度标签类型
const getUrgencyTagType = (urgency: string) => {
  switch (urgency) {
    case 'high': return 'danger'
    case 'medium': return 'warning'
    case 'low': return 'info'
    default: return 'info'
  }
}

// 状态标签类型
const getStatusTagType = (status: string) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'approved': return 'success'
    case 'rejected': return 'danger'
    case 'cancelled': return 'info'
    default: return 'info'
  }
}

// 加载调课申请列表
const loadRescheduleRequests = async () => {
  loading.value = true
  try {
    const result = await rescheduleAPI.getTeacherRequests({
      page: pagination.current,
      size: pagination.size,
      status: statusFilter.value === 'all' ? undefined : statusFilter.value
    })

    if (result.success && result.data) {
      rescheduleRequests.value = result.data.records || []
      pagination.total = result.data.total || 0
    } else {
      ElMessage.error(result.message || '获取调课申请失败')
    }
  } catch (error) {
    console.error('获取调课申请失败:', error)
    ElMessage.error('获取调课申请失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 查看申请详情
const viewRequestDetail = (request: RescheduleRequest) => {
  currentRequest.value = request
  showDetailModal.value = true
}

// 关闭详情弹窗
const closeDetailModal = () => {
  showDetailModal.value = false
  currentRequest.value = null
}

// 显示审批弹窗
const showApproval = (request: RescheduleRequest, status: 'approved' | 'rejected') => {
  currentRequest.value = request
  approvalForm.status = status
  approvalForm.reviewNotes = ''
  approvalForm.compensationAmount = 0
  approvalForm.effectiveDate = ''
  showApprovalModal.value = true
}

// 关闭审批弹窗
const closeApprovalModal = () => {
  showApprovalModal.value = false
  currentRequest.value = null
}

// 提交审批
const submitApproval = async () => {
  if (!currentRequest.value) return

  if (!approvalForm.reviewNotes.trim()) {
    ElMessage.warning('请输入审批意见')
    return
  }

  try {
    const result = await rescheduleAPI.approve(currentRequest.value.id, {
      status: approvalForm.status,
      reviewNotes: approvalForm.reviewNotes,
      compensationAmount: approvalForm.compensationAmount || undefined,
      effectiveDate: approvalForm.effectiveDate || undefined
    })

    if (result.success) {
      ElMessage.success(`调课申请已${approvalForm.status === 'approved' ? '同意' : '拒绝'}`)
      closeApprovalModal()
      await loadRescheduleRequests()
    } else {
      ElMessage.error(result.message || '审批失败')
    }
  } catch (error) {
    console.error('审批失败:', error)
    ElMessage.error('审批失败，请稍后重试')
  }
}

// 快速审批
const quickApproval = async (request: RescheduleRequest, status: 'approved' | 'rejected') => {
  try {
    const action = status === 'approved' ? '同意' : '拒绝'
    await ElMessageBox.confirm(
      `确定要${action}这个调课申请吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const result = await rescheduleAPI.approve(request.id, {
      status,
      reviewNotes: status === 'approved' ? '同意调课申请' : '拒绝调课申请'
    })

    if (result.success) {
      ElMessage.success(`调课申请已${action}`)
      await loadRescheduleRequests()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败，请稍后重试')
    }
  }
}

// 分页变化
const handlePageChange = (page: number) => {
  pagination.current = page
  loadRescheduleRequests()
}

// 状态筛选变化
const handleStatusChange = () => {
  pagination.current = 1
  loadRescheduleRequests()
}

// 刷新列表
const refreshList = () => {
  pagination.current = 1
  loadRescheduleRequests()
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN')
}

// 格式化时间
const formatTime = (time: string) => {
  return time.substring(0, 5) // 只显示 HH:mm
}

// 页面加载时获取数据
onMounted(() => {
  loadRescheduleRequests()
})
</script>

<template>
  <div class="reschedule-management">
    <div class="page-header">
      <h2>调课申请管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="refreshList" :icon="Refresh">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 筛选器 -->
    <div class="filters">
      <el-select
        v-model="statusFilter"
        placeholder="选择状态"
        style="width: 150px"
        @change="handleStatusChange"
      >
        <el-option
          v-for="option in statusOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
    </div>

    <!-- 申请列表 -->
    <div class="request-list" v-loading="loading">
      <div v-if="rescheduleRequests.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无调课申请" />
      </div>

      <div v-else class="request-cards">
        <el-card
          v-for="request in rescheduleRequests"
          :key="request.id"
          class="request-card"
          shadow="hover"
        >
          <div class="card-header">
            <div class="request-info">
              <h3>{{ request.courseTitle }}</h3>
              <div class="meta-info">
                <el-tag :type="getStatusTagType(request.status)" size="small">
                  {{ request.statusDisplay }}
                </el-tag>
                <el-tag :type="getUrgencyTagType(request.urgencyLevel)" size="small">
                  {{ request.urgencyLevel === 'high' ? '紧急' :
                     request.urgencyLevel === 'medium' ? '一般' : '不急' }}
                </el-tag>
                <span class="apply-time">{{ formatDateTime(request.createdAt) }}</span>
              </div>
            </div>
          </div>

          <div class="card-content">
            <div class="student-info">
              <el-icon><User /></el-icon>
              <span>学生：{{ request.studentName }}</span>
            </div>

            <div class="schedule-info">
              <div class="original-schedule">
                <h4>原定时间</h4>
                <div class="time-info">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ formatDate(request.originalDate) }}</span>
                  <el-icon><Timer /></el-icon>
                  <span>{{ formatTime(request.originalStartTime) }} - {{ formatTime(request.originalEndTime) }}</span>
                </div>
              </div>

              <div v-if="request.requestType === 'single' && request.newDate" class="new-schedule">
                <h4>调整后时间</h4>
                <div class="time-info">
                  <el-icon><Calendar /></el-icon>
                  <span>{{ formatDate(request.newDate) }}</span>
                  <el-icon><Timer /></el-icon>
                  <span>{{ formatTime(request.newStartTime) }} - {{ formatTime(request.newEndTime) }}</span>
                </div>
              </div>

              <div v-else-if="request.requestType === 'recurring'" class="new-schedule">
                <h4>新的周期安排</h4>
                <div class="weekly-schedule">
                  {{ request.newWeeklySchedule }}
                </div>
              </div>

              <div v-else-if="request.requestType === 'cancel'" class="cancel-info">
                <h4>申请取消课程</h4>
              </div>
            </div>

            <div class="reason-info">
              <h4>调课原因</h4>
              <p>{{ request.reason }}</p>
            </div>

            <div v-if="request.reviewNotes" class="review-notes">
              <h4>审批意见</h4>
              <p>{{ request.reviewNotes }}</p>
            </div>
          </div>

          <div class="card-actions">
            <el-button size="small" @click="viewRequestDetail(request)" :icon="View">
              详情
            </el-button>
            <el-button
              v-if="request.status === 'pending'"
              type="success"
              size="small"
              @click="quickApproval(request, 'approved')"
              :icon="Check"
            >
              同意
            </el-button>
            <el-button
              v-if="request.status === 'pending'"
              type="danger"
              size="small"
              @click="quickApproval(request, 'rejected')"
              :icon="Close"
            >
              拒绝
            </el-button>
            <el-button
              v-if="request.status === 'pending'"
              type="warning"
              size="small"
              @click="showApproval(request, 'approved')"
              :icon="Document"
            >
              详细审批
            </el-button>
          </div>
        </el-card>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="pagination">
        <el-pagination
          :current-page="pagination.current"
          :page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="showDetailModal"
      title="调课申请详情"
      width="600px"
      @close="closeDetailModal"
    >
      <div v-if="currentRequest" class="request-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="课程">{{ currentRequest.courseTitle }}</el-descriptions-item>
            <el-descriptions-item label="科目">{{ currentRequest.subjectName }}</el-descriptions-item>
            <el-descriptions-item label="学生">{{ currentRequest.studentName }}</el-descriptions-item>
            <el-descriptions-item label="申请类型">
              {{ currentRequest.requestType === 'single' ? '单次调课' :
                 currentRequest.requestType === 'recurring' ? '周期调课' : '取消课程' }}
            </el-descriptions-item>
            <el-descriptions-item label="紧急程度">
              <el-tag :type="getUrgencyTagType(currentRequest.urgencyLevel)" size="small">
                {{ currentRequest.urgencyLevel === 'high' ? '紧急' :
                   currentRequest.urgencyLevel === 'medium' ? '一般' : '不急' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="申请状态">
              <el-tag :type="getStatusTagType(currentRequest.status)" size="small">
                {{ currentRequest.statusDisplay }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ formatDateTime(currentRequest.createdAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="currentRequest.advanceNoticeHours" label="提前通知">
              {{ currentRequest.advanceNoticeHours }}小时
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <h4>时间安排</h4>
          <div class="time-comparison">
            <div class="original-time">
              <h5>原定时间</h5>
              <p>{{ formatDate(currentRequest.originalDate) }} {{ formatTime(currentRequest.originalStartTime) }} - {{ formatTime(currentRequest.originalEndTime) }}</p>
            </div>
            <div v-if="currentRequest.requestType === 'single' && currentRequest.newDate" class="new-time">
              <h5>调整后时间</h5>
              <p>{{ formatDate(currentRequest.newDate) }} {{ formatTime(currentRequest.newStartTime) }} - {{ formatTime(currentRequest.newEndTime) }}</p>
            </div>
            <div v-else-if="currentRequest.requestType === 'recurring'" class="new-time">
              <h5>新的周期安排</h5>
              <p>{{ currentRequest.newWeeklySchedule }}</p>
            </div>
            <div v-else-if="currentRequest.requestType === 'cancel'" class="cancel-notice">
              <h5>申请取消课程</h5>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4>调课原因</h4>
          <p class="reason-text">{{ currentRequest.reason }}</p>
        </div>

        <div v-if="currentRequest.reviewNotes" class="detail-section">
          <h4>审批意见</h4>
          <p class="review-text">{{ currentRequest.reviewNotes }}</p>
          <p v-if="currentRequest.reviewerName" class="reviewer">审批人：{{ currentRequest.reviewerName }}</p>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDetailModal">关闭</el-button>
          <el-button
            v-if="currentRequest?.status === 'pending'"
            type="success"
            @click="showApproval(currentRequest, 'approved')"
          >
            同意
          </el-button>
          <el-button
            v-if="currentRequest?.status === 'pending'"
            type="danger"
            @click="showApproval(currentRequest, 'rejected')"
          >
            拒绝
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog
      v-model="showApprovalModal"
      :title="`${approvalForm.status === 'approved' ? '同意' : '拒绝'}调课申请`"
      width="500px"
      @close="closeApprovalModal"
    >
      <el-form :model="approvalForm" label-width="100px">
        <el-form-item label="审批意见" required>
          <el-input
            v-model="approvalForm.reviewNotes"
            type="textarea"
            :rows="4"
            :placeholder="`请输入${approvalForm.status === 'approved' ? '同意' : '拒绝'}的理由`"
          />
        </el-form-item>

        <el-form-item v-if="approvalForm.status === 'approved'" label="补偿金额">
          <el-input-number
            v-model="approvalForm.compensationAmount"
            :min="0"
            :precision="2"
            placeholder="如需补偿请填写金额"
          />
          <span style="margin-left: 8px; color: #999;">元</span>
        </el-form-item>

        <el-form-item v-if="approvalForm.status === 'approved'" label="生效日期">
          <el-date-picker
            v-model="approvalForm.effectiveDate"
            type="date"
            placeholder="选择生效日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeApprovalModal">取消</el-button>
          <el-button
            :type="approvalForm.status === 'approved' ? 'success' : 'danger'"
            @click="submitApproval"
          >
            确认{{ approvalForm.status === 'approved' ? '同意' : '拒绝' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.reschedule-management {
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
  font-size: 24px;
  color: #333;
}

.filters {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
}

.request-list {
  min-height: 400px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.request-cards {
  display: grid;
  gap: 16px;
  margin-bottom: 20px;
}

.request-card {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.request-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  margin-bottom: 16px;
}

.request-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #333;
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.apply-time {
  color: #666;
  font-size: 12px;
}

.card-content {
  margin-bottom: 16px;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #666;
}

.schedule-info {
  margin-bottom: 16px;
}

.schedule-info h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #333;
  font-weight: 600;
}

.original-schedule,
.new-schedule,
.cancel-info {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.new-schedule {
  background: #e8f5e8;
}

.cancel-info {
  background: #ffeaea;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.weekly-schedule {
  color: #666;
}

.reason-info h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #333;
  font-weight: 600;
}

.reason-info p {
  margin: 0;
  color: #666;
  line-height: 1.5;
}

.review-notes {
  background: #f0f9ff;
  padding: 12px;
  border-radius: 6px;
  margin-top: 12px;
}

.review-notes h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #333;
  font-weight: 600;
}

.review-notes p {
  margin: 0;
  color: #666;
  line-height: 1.5;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 弹窗样式 */
.request-detail {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.time-comparison {
  display: grid;
  gap: 12px;
}

.original-time,
.new-time,
.cancel-notice {
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}

.original-time {
  background: #f8f9fa;
}

.new-time {
  background: #e8f5e8;
}

.cancel-notice {
  background: #ffeaea;
}

.original-time h5,
.new-time h5,
.cancel-notice h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
}

.original-time p,
.new-time p {
  margin: 0;
  color: #666;
}

.reason-text,
.review-text {
  margin: 0;
  color: #666;
  line-height: 1.6;
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
}

.reviewer {
  margin: 8px 0 0 0;
  color: #999;
  font-size: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .reschedule-management {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .card-actions {
    justify-content: flex-start;
  }

  .meta-info {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
