<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check,
  Close,
  View,
  Refresh
} from '@element-plus/icons-vue'
import { rescheduleAPI } from '../../../utils/api'

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

// 分页配置
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
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
    const result = await rescheduleAPI.getAdminRequests({
      page: pagination.current,
      size: pagination.size,
      status: statusFilter.value === 'all' ? undefined : statusFilter.value
    })

    if (result.success) {
      rescheduleRequests.value = result.data.records || []
      pagination.total = result.data.total || 0
    } else {
      ElMessage.error(result.message || '获取调课申请列表失败')
    }
  } catch (error) {
    console.error('获取调课申请列表失败:', error)
    ElMessage.error('获取调课申请列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 查看详情
const viewDetail = (request: RescheduleRequest) => {
  currentRequest.value = request
  showDetailModal.value = true
}

// 快速审批
const quickApproval = async (request: RescheduleRequest, status: 'approved' | 'rejected') => {
  try {
    const action = status === 'approved' ? '同意' : '拒绝'

    // 使用ElMessageBox.prompt获取审批原因
    const { value: reviewNotes } = await ElMessageBox.prompt(
      `请输入${action}调课申请的原因：`,
      `${action}调课申请`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: `请输入${action}的原因...`,
        inputValidator: (value) => {
          if (!value || value.trim().length === 0) {
            return '审批原因不能为空'
          }
          if (value.trim().length < 5) {
            return '审批原因至少需要5个字符'
          }
          return true
        },
        inputErrorMessage: '审批原因不能为空且至少需要5个字符'
      }
    )

    // 再次确认操作
    await ElMessageBox.confirm(
      `确定要${action}这个调课申请吗？\n审批原因：${reviewNotes}`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const result = await rescheduleAPI.adminApprove(request.id, {
      status,
      reviewNotes: reviewNotes.trim()
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

// 格式化周期性调课时间
const formatRecurringSchedule = (schedule: string) => {
  if (!schedule) return '-'

  // 解析格式：周一、周三 14:00-16:00;周五 18:00-20:00
  try {
    // 如果包含时间段信息，提取星期部分
    if (schedule.includes(' ')) {
      const parts = schedule.split(' ')
      if (parts.length >= 2) {
        const weekdays = parts[0]
        const timeSlots = parts.slice(1).join(' ')
        return `${weekdays} ${timeSlots}`
      }
    }
    return schedule
  } catch {
    return schedule
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadRescheduleRequests()
})
</script>

<template>
  <div class="admin-reschedule-management">
    <div class="header">
      <h2>调课申请管理</h2>
    </div>

    <!-- 筛选器 -->
    <el-card class="search-card" shadow="never">
      <el-form inline>
        <el-form-item label="状态">
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
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refreshList" :icon="Refresh">
            刷新
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 申请列表 -->
    <el-card class="table-card" shadow="never" v-loading="loading">
      <div v-if="rescheduleRequests.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无调课申请" />
      </div>

      <el-table
        v-else
        :data="rescheduleRequests"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="courseTitle" label="课程标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="subjectName" label="科目" width="100" />
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="applicantName" label="申请人" width="120">
          <template #default="{ row }">
            {{ row.applicantName }} ({{ row.applicantType === 'student' ? '学生' : '教师' }})
          </template>
        </el-table-column>
        <el-table-column label="原定时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.originalDate) }} {{ formatTime(row.originalStartTime) }}-{{ formatTime(row.originalEndTime) }}
          </template>
        </el-table-column>
        <el-table-column label="新时间" width="180" v-if="rescheduleRequests.some(r => r.newDate || r.newWeeklySchedule)">
          <template #default="{ row }">
            <span v-if="row.requestType === 'recurring' && row.newWeeklySchedule">
              {{ formatRecurringSchedule(row.newWeeklySchedule) }}
            </span>
            <span v-else-if="row.newDate">
              {{ formatDate(row.newDate) }} {{ formatTime(row.newStartTime) }}-{{ formatTime(row.newEndTime) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="申请原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="reason-text">{{ row.reason }}</span>
          </template>
        </el-table-column>
        <el-table-column label="紧急程度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getUrgencyTagType(row.urgencyLevel)" size="small">
              {{ row.urgencyLevel === 'high' ? '紧急' : row.urgencyLevel === 'medium' ? '一般' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ row.statusDisplay }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button
                type="primary"
                size="small"
                @click="viewDetail(row)"
                :icon="View"
              >
                详情
              </el-button>

              <template v-if="row.status === 'pending'">
                <el-button
                  type="success"
                  size="small"
                  @click="quickApproval(row, 'approved')"
                  :icon="Check"
                >
                  同意
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="quickApproval(row, 'rejected')"
                  :icon="Close"
                >
                  拒绝
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="pagination.total > 0">
        <el-pagination
          :current-page="pagination.current"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadRescheduleRequests"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="showDetailModal"
      title="调课申请详情"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="currentRequest" class="detail-content">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-row">
            <span class="label">课程：</span>
            <span>{{ currentRequest.courseTitle }}</span>
          </div>
          <div class="detail-row">
            <span class="label">科目：</span>
            <span>{{ currentRequest.subjectName }}</span>
          </div>
          <div class="detail-row">
            <span class="label">申请人：</span>
            <span>{{ currentRequest.applicantName }} ({{ currentRequest.applicantType === 'student' ? '学生' : '教师' }})</span>
          </div>
          <div class="detail-row">
            <span class="label">教师：</span>
            <span>{{ currentRequest.teacherName }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>时间安排</h4>
          <div class="detail-row">
            <span class="label">原定时间：</span>
            <span>{{ formatDate(currentRequest.originalDate) }} {{ formatTime(currentRequest.originalStartTime) }}-{{ formatTime(currentRequest.originalEndTime) }}</span>
          </div>
          <div class="detail-row" v-if="currentRequest.newDate || currentRequest.newWeeklySchedule">
            <span class="label">新时间：</span>
            <span v-if="currentRequest.requestType === 'recurring' && currentRequest.newWeeklySchedule">
              {{ formatRecurringSchedule(currentRequest.newWeeklySchedule) }}
            </span>
            <span v-else-if="currentRequest.newDate">
              {{ formatDate(currentRequest.newDate) }} {{ formatTime(currentRequest.newStartTime) }}-{{ formatTime(currentRequest.newEndTime) }}
            </span>
          </div>
        </div>

        <div class="detail-section">
          <h4>申请信息</h4>
          <div class="detail-row">
            <span class="label">申请类型：</span>
            <span>{{ currentRequest.requestType === 'single' ? '单次调课' : currentRequest.requestType === 'recurring' ? '周期性调课' : '取消课程' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">紧急程度：</span>
            <el-tag :type="getUrgencyTagType(currentRequest.urgencyLevel)">
              {{ currentRequest.urgencyLevel === 'high' ? '紧急' : currentRequest.urgencyLevel === 'medium' ? '一般' : '普通' }}
            </el-tag>
          </div>
          <div class="detail-row">
            <span class="label">申请原因：</span>
            <span class="reason-text">{{ currentRequest.reason }}</span>
          </div>
        </div>

        <div class="detail-section" v-if="currentRequest.reviewNotes">
          <h4>审批信息</h4>
          <div class="detail-row">
            <span class="label">审批意见：</span>
            <span>{{ currentRequest.reviewNotes }}</span>
          </div>
          <div class="detail-row" v-if="currentRequest.reviewerName">
            <span class="label">审批人：</span>
            <span>{{ currentRequest.reviewerName }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDetailModal = false">关闭</el-button>
          <template v-if="currentRequest?.status === 'pending'">
            <el-button
              type="success"
              @click="quickApproval(currentRequest, 'approved')"
            >
              同意申请
            </el-button>
            <el-button
              type="danger"
              @click="quickApproval(currentRequest, 'rejected')"
            >
              拒绝申请
            </el-button>
          </template>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-reschedule-management {
  padding: 20px;
  padding-top: 5px; /* 增加顶部间距，避免被导航栏遮挡 */
}

.header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  min-height: 400px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  width: 100%;
  padding: 0 8px;
}

.operation-buttons .el-button {
  margin: 0;
  min-width: 60px;
  text-align: center;
}

.text-muted {
  color: #c0c4cc;
  font-style: italic;
}

.reason-text {
  color: #606266;
  word-break: break-word;
  line-height: 1.5;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detail-content {
  max-height: 500px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
  align-items: flex-start;
}

.detail-row .label {
  font-weight: 500;
  color: #606266;
  min-width: 100px;
  flex-shrink: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-reschedule-management {
    padding: 15px;
    padding-top: 5px;
  }

  .header h2 {
    font-size: 18px;
  }

  .search-card {
    margin-bottom: 15px;
  }

  .operation-buttons {
    flex-wrap: wrap;
    gap: 6px;
  }

  .operation-buttons .el-button {
    font-size: 12px;
    padding: 4px 8px;
  }
}

@media (max-width: 480px) {
  .admin-reschedule-management {
    padding: 10px;
    padding-top: 5px;
  }

  .header h2 {
    font-size: 16px;
  }

  .operation-buttons .el-button {
    padding: 3px 6px;
    font-size: 11px;
  }

  .table-card {
    overflow-x: auto;
  }

  .el-table {
    min-width: 1200px;
  }
}
</style>
