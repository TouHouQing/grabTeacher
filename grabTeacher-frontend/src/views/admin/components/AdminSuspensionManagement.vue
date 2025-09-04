<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, View, Refresh } from '@element-plus/icons-vue'
import { suspensionAPI } from '../../../utils/api'

interface SuspensionRequest {
  id: number
  enrollmentId: number
  studentId: number
  studentName?: string
  teacherId: number
  teacherName?: string
  courseTitle?: string
  subjectName?: string
  reason?: string
  status: 'pending' | 'approved' | 'rejected' | 'cancelled'
  statusDisplay?: string
  adminId?: number
  adminNotes?: string
  createdAt?: string
  updatedAt?: string
  reviewedAt?: string
}

const loading = ref(false)
const requests = ref<SuspensionRequest[]>([])
const statusFilter = ref('all')
const currentRequest = ref<SuspensionRequest | null>(null)
const showDetailModal = ref(false)

const pagination = reactive({ current: 1, size: 10, total: 0 })

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待审批', value: 'pending' },
  { label: '已同意', value: 'approved' },
  { label: '已拒绝', value: 'rejected' },
  { label: '已取消', value: 'cancelled' }
]

const getStatusTagType = (status: string) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'approved': return 'success'
    case 'rejected': return 'danger'
    case 'cancelled': return 'info'
    default: return 'info'
  }
}

const loadRequests = async () => {
  loading.value = true
  try {
    const result = await suspensionAPI.getAdminRequests({
      page: pagination.current,
      size: pagination.size,
      status: statusFilter.value === 'all' ? undefined : statusFilter.value
    })
    if (result.success) {
      requests.value = result.data.records || []
      pagination.total = result.data.total || 0
    } else {
      ElMessage.error(result.message || '获取停课申请列表失败')
    }
  } catch (e) {
    console.error('获取停课申请列表失败:', e)
    ElMessage.error('获取停课申请列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const viewDetail = (request: SuspensionRequest) => {
  currentRequest.value = request
  showDetailModal.value = true
}

const quickApproval = async (request: SuspensionRequest, status: 'approved' | 'rejected') => {
  try {
    const action = status === 'approved' ? '同意' : '拒绝'
    const { value: reviewNotes } = await ElMessageBox.prompt(
      `请输入${action}停课申请的原因：`,
      `${action}停课申请`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: `请输入${action}的原因...`,
        inputValidator: (value) => {
          if (!value || value.trim().length === 0) return '审批原因不能为空'
          if (value.trim().length < 5) return '审批原因至少需要5个字符'
          return true
        },
        inputErrorMessage: '审批原因不能为空且至少需要5个字符'
      }
    )

    await ElMessageBox.confirm(
      `确定要${action}这个停课申请吗？\n审批原因：${reviewNotes}`,
      '确认操作',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )

    const result = await suspensionAPI.adminApprove(request.id, {
      status,
      reviewNotes: reviewNotes.trim()
    })

    if (result.success) {
      ElMessage.success(`停课申请已${action}`)
      await loadRequests()
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

const handlePageChange = (page: number) => {
  pagination.current = page
  loadRequests()
}

const handleStatusChange = () => {
  pagination.current = 1
  loadRequests()
}

const refreshList = () => {
  pagination.current = 1
  loadRequests()
}

const formatDateTime = (dateTime?: string) => {
  return dateTime ? new Date(dateTime).toLocaleString('zh-CN') : '-'
}

onMounted(() => {
  loadRequests()
})
</script>

<template>
  <div class="admin-suspension-management">
    <div class="header">
      <h2>停课申请管理</h2>
    </div>

    <el-card class="search-card" shadow="never">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="选择状态" style="width: 150px" @change="handleStatusChange">
            <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refreshList" :icon="Refresh">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never" v-loading="loading">
      <div v-if="requests.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无停课申请" />
      </div>

      <el-table v-else :data="requests" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="courseTitle" label="课程标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="subjectName" label="科目" width="100" />
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column prop="reason" label="停课原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">{{ row.statusDisplay || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="审核时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.reviewedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button type="primary" size="small" @click="viewDetail(row)" :icon="View">详情</el-button>
              <template v-if="row.status === 'pending'">
                <el-button type="success" size="small" @click="quickApproval(row, 'approved')" :icon="Check">同意</el-button>
                <el-button type="danger" size="small" @click="quickApproval(row, 'rejected')" :icon="Close">拒绝</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="pagination.total > 0">
        <el-pagination :current-page="pagination.current" :page-size="pagination.size" :page-sizes="[10, 20, 50]" :total="pagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="loadRequests" @current-change="handlePageChange" />
      </div>
    </el-card>

    <el-dialog v-model="showDetailModal" title="停课申请详情" width="600px" :close-on-click-modal="false">
      <div v-if="currentRequest" class="detail-content">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-row"><span class="label">课程：</span><span>{{ currentRequest.courseTitle || '-' }}</span></div>
          <div class="detail-row"><span class="label">科目：</span><span>{{ currentRequest.subjectName || '-' }}</span></div>
          <div class="detail-row"><span class="label">教师：</span><span>{{ currentRequest.teacherName || '-' }}</span></div>
          <div class="detail-row"><span class="label">学生：</span><span>{{ currentRequest.studentName || '-' }}</span></div>
        </div>
        <div class="detail-section">
          <h4>申请信息</h4>
          <div class="detail-row"><span class="label">停课原因：</span><span>{{ currentRequest.reason || '-' }}</span></div>
          <div class="detail-row"><span class="label">状态：</span><span>{{ currentRequest.statusDisplay || currentRequest.status }}</span></div>
          <div class="detail-row" v-if="currentRequest.adminNotes"><span class="label">审批意见：</span><span>{{ currentRequest.adminNotes }}</span></div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDetailModal = false">关闭</el-button>
          <template v-if="currentRequest?.status === 'pending'">
            <el-button type="success" @click="quickApproval(currentRequest, 'approved')">同意申请</el-button>
            <el-button type="danger" @click="quickApproval(currentRequest, 'rejected')">拒绝申请</el-button>
          </template>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-suspension-management { padding: 20px; padding-top: 5px; }
.header { display: flex; justify-content: flex-start; align-items: center; margin-bottom: 20px; }
.header h2 { margin: 0; color: #303133; font-size: 20px; font-weight: 600; }
.search-card { margin-bottom: 20px; }
.table-card { min-height: 400px; }
.empty-state { text-align: center; padding: 60px 0; }
.operation-buttons { display: flex; gap: 8px; flex-wrap: nowrap; align-items: center; justify-content: center; white-space: nowrap; width: 100%; padding: 0 8px; }
.operation-buttons .el-button { margin: 0; min-width: 60px; text-align: center; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
.detail-content { max-height: 500px; overflow-y: auto; }
.detail-section { margin-bottom: 24px; }
.detail-section h4 { margin: 0 0 12px 0; color: #303133; font-size: 16px; border-bottom: 1px solid #ebeef5; padding-bottom: 8px; }
.detail-row { display: flex; margin-bottom: 12px; align-items: flex-start; }
.detail-row .label { font-weight: 500; color: #606266; min-width: 100px; flex-shrink: 0; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }
@media (max-width: 768px) { .admin-suspension-management { padding: 15px; padding-top: 5px; } .header h2 { font-size: 18px; } .search-card { margin-bottom: 15px; } .operation-buttons { flex-wrap: wrap; gap: 6px; } .operation-buttons .el-button { font-size: 12px; padding: 4px 8px; } }
@media (max-width: 480px) { .admin-suspension-management { padding: 10px; padding-top: 5px; } .header h2 { font-size: 16px; } .operation-buttons .el-button { padding: 3px 6px; font-size: 11px; } .table-card { overflow-x: auto; } .el-table { min-width: 1200px; } }
</style>
