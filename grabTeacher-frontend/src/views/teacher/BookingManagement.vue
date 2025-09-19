<template>
  <div class="teacher-booking-management">
    <div class="page-header">
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
          <el-option label="调课" value="single" />
          <el-option label="请假" value="cancel" />
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
        <div v-for="booking in bookings" :key="booking.id" class="booking-item" :class="{
          'pending': booking.status === 'pending',
          'approved': booking.status === 'approved',
          'rejected': booking.status === 'rejected'
        }">
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
                <span class="value">{{ booking.recordType === 'suspension' ? '请假' : (booking.requestType === 'cancel' ? '取消' : '调课') }}</span>
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
                <span class="value">
                  <template v-if="booking.recordType === 'suspension'">
                    {{ booking.startDate }} - {{ booking.endDate }}
                  </template>
                  <template v-else>
                    {{ booking.originalDate }} {{ booking.originalStartTime }}-{{ booking.originalEndTime }}
                  </template>
                </span>
              </div>

              <div class="detail-row" v-if="booking.requestType !== 'cancel' && booking.recordType !== 'suspension'">
                <span class="label">时间信息：</span>
                <span class="value">
                  <!-- 教师发起：pending/rejected 仅展示候选；approved 展示候选 + 最终新时间 -->
                  <template v-if="booking.applicantType === 'teacher'">
                    <template v-if="booking.newWeeklySchedule && booking.newWeeklySchedule.startsWith('CANDIDATES|')">
                      <template v-if="parseCandidateList(booking.newWeeklySchedule).length > 0">
                        候选：
                        <span v-for="(c, idx) in parseCandidateList(booking.newWeeklySchedule).slice(0, 3)" :key="idx" style="margin-right: 8px;">
                          {{ c.date }} {{ formatTime(c.startTime) }}-{{ formatTime(c.endTime) }}
                        </span>
                        <span v-if="parseCandidateList(booking.newWeeklySchedule).length > 3" class="text-muted" style="margin-left: 6px;">+{{ parseCandidateList(booking.newWeeklySchedule).length - 3 }}</span>
                        <div v-if="booking.status === 'approved' && booking.newDate" style="margin-top: 4px;">
                          最终上课时间：{{ booking.newDate }} {{ formatTime(booking.newStartTime) }}-{{ formatTime(booking.newEndTime) }}
                        </div>
                      </template>
                    </template>
                    <template v-else>
                      <template v-if="booking.status === 'approved' && booking.newDate">
                        <div>最终上课时间：{{ booking.newDate }} {{ formatTime(booking.newStartTime) }}-{{ formatTime(booking.newEndTime) }}</div>
                      </template>
                      <template v-else>-</template>
                    </template>
                  </template>
                  <!-- 学生发起：沿用原逻辑，直接展示新时间 -->
                  <template v-else>
                    <template v-if="booking.newDate">
                      {{ booking.newDate }} {{ formatTime(booking.newStartTime) }}-{{ formatTime(booking.newEndTime) }}
                    </template>
                    <template v-else>-</template>
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
              <span>{{ selectedBooking.recordType === 'suspension' ? '请假' : (selectedBooking.requestType === 'cancel' ? '取消' : '调课') }}</span>
            </div>
            <div class="detail-item">
              <label>课程名称：</label>
              <span>{{ selectedBooking.courseTitle || '-' }}</span>
            </div>
          </div>

          <div class="time-info">
            <template v-if="selectedBooking.recordType === 'suspension'">
              <div class="time-item">
                <label>请假开始日期：</label>
                <span>{{ selectedBooking.startDate }}</span>
              </div>
              <div class="time-item">
                <label>请假结束日期：</label>
                <span>{{ selectedBooking.endDate }}</span>
              </div>
            </template>
            <template v-else>
              <div class="time-item">
                <label>原上课日期：</label>
                <span>{{ selectedBooking.originalDate }}</span>
              </div>
              <div class="time-item">
                <label>原上课时间：</label>
                <span>{{ selectedBooking.originalStartTime }} - {{ selectedBooking.originalEndTime }}</span>
              </div>
            </template>
          </div>

          <div class="time-info" v-if="selectedBooking.requestType !== 'cancel' && selectedBooking.recordType !== 'suspension'">
            <!-- 教师发起：pending/rejected 显示候选；approved 显示候选 + 最终 -->
            <template v-if="selectedBooking.applicantType === 'teacher'">
              <div class="time-item" v-if="selectedBooking.newWeeklySchedule && selectedBooking.newWeeklySchedule.startsWith('CANDIDATES|')">
                <label>教师候选：</label>
                <span>
                  <el-tag
                    v-for="(c, idx) in parseCandidateList(selectedBooking.newWeeklySchedule)"
                    :key="idx"
                    type="success"
                    effect="plain"
                    style="margin-right: 6px; margin-bottom: 4px;"
                  >
                    {{ c.date }} {{ formatTime(c.startTime) }}-{{ formatTime(c.endTime) }}
                  </el-tag>
                </span>
              </div>
              <div class="time-item" v-if="selectedBooking.status === 'approved' && selectedBooking.newDate">
                <label>最终上课时间：</label>
                <span>{{ selectedBooking.newDate }} {{ formatTime(selectedBooking.newStartTime) }} - {{ formatTime(selectedBooking.newEndTime) }}</span>
              </div>
              <div class="time-item" v-if="!selectedBooking.newWeeklySchedule && selectedBooking.status === 'approved' && selectedBooking.newDate">
                <label>最终上课时间：</label>
                <span>{{ selectedBooking.newDate }} {{ formatTime(selectedBooking.newStartTime) }} - {{ formatTime(selectedBooking.newEndTime) }}</span>
              </div>
            </template>
            <!-- 学生发起：直接显示新上课时间（若已生成） -->
            <template v-else>
              <div class="time-item" v-if="selectedBooking.newDate">
                <label>新上课日期：</label>
                <span>{{ selectedBooking.newDate }}</span>
              </div>
              <div class="time-item" v-if="selectedBooking.newDate">
                <label>最终上课时间：</label>
                <span>{{ formatTime(selectedBooking.newStartTime) }} - {{ formatTime(selectedBooking.newEndTime) }}</span>
              </div>
              <div class="time-item" v-else>
                <label>新上课时间：</label>
                <span>-</span>
              </div>
            </template>
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
import { rescheduleAPI, suspensionAPI } from '../../utils/api'

// 响应式数据
const loading = ref(false)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
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
// eslint-disable-next-line @typescript-eslint/no-explicit-any
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
    // 并行获取调课记录和请假记录
    const [rescheduleResult, suspensionResult] = await Promise.all([
      rescheduleAPI.getTeacherRequests({
        page: pagination.current,
        size: pagination.size,
        status: statusFilter.value,
        year: yearFilter.value || undefined,
        month: monthFilter.value || undefined,
        requestType: requestTypeFilter.value ? (requestTypeFilter.value === 'cancel' ? 'cancel' : 'reschedule') : undefined,
        applicantType: applicantTypeFilter.value || undefined,
        applicantName: applicantNameFilter.value || undefined,
        courseName: courseNameFilter.value || undefined
      }),
      suspensionAPI.getTeacherRequests({
        page: pagination.current,
        size: pagination.size,
        status: statusFilter.value
      })
    ])

    let allBookings: any[] = []

    // 处理调课记录
    if (rescheduleResult.success && rescheduleResult.data) {
      const rescheduleRecords = rescheduleResult.data.records || []
      // 为调课记录添加类型标识
      const processedRescheduleRecords = rescheduleRecords.map(record => ({
        ...record,
        recordType: 'reschedule'
      }))
      allBookings = allBookings.concat(processedRescheduleRecords)
    }

    // 处理请假记录
    if (suspensionResult.success && suspensionResult.data) {
      const suspensionRecords = suspensionResult.data.records || []
      // 为请假记录添加类型标识并转换为统一格式
      const processedSuspensionRecords = suspensionRecords.map(record => ({
        ...record,
        recordType: 'suspension',
        requestType: 'cancel', // 请假记录统一标记为cancel类型
        id: `suspension_${record.id}`, // 避免ID冲突
        studentName: record.studentName || '未知学生',
        applicantName: record.applicantName || '未知申请人',
        applicantType: record.applicantType || 'teacher',
        courseTitle: record.courseTitle || '未知课程',
        originalDate: record.startDate,
        originalStartTime: '00:00:00',
        originalEndTime: '23:59:59',
        reason: record.reason,
        status: record.status,
        createdAt: record.createdAt,
        adminNotes: record.adminNotes
      }))
      allBookings = allBookings.concat(processedSuspensionRecords)
    }

    // 按创建时间倒序排序
    allBookings.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())

    // 应用筛选条件
    let filteredBookings = allBookings
    if (requestTypeFilter.value) {
      if (requestTypeFilter.value === 'cancel') {
        filteredBookings = filteredBookings.filter(record => record.requestType === 'cancel')
      } else if (requestTypeFilter.value === 'single') {
        filteredBookings = filteredBookings.filter(record => record.requestType === 'single')
      }
    }

    bookings.value = filteredBookings
    pagination.total = filteredBookings.length

    if (!rescheduleResult.success && !suspensionResult.success) {
      ElMessage.error('获取记录失败')
    }
  } catch (error) {
    console.error('获取记录失败:', error)
    ElMessage.error('获取记录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 查看预约详情
// eslint-disable-next-line @typescript-eslint/no-explicit-any
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

// 周期性展示逻辑已移除

// 仅显示 HH:mm
const formatTime = (time: string) => {
  if (!time) return ''
  return time.substring(0, 5)
}

// 解析教师多选候选时间：形如 CANDIDATES|2025-09-20 08:00-10:00,2025-09-21 10:00-12:00
const parseCandidateList = (s: string | undefined): Array<{ date: string; startTime: string; endTime: string }> => {
  const list: Array<{ date: string; startTime: string; endTime: string }> = []
  if (!s || !s.startsWith('CANDIDATES|')) return list
  const body = s.substring('CANDIDATES|'.length)
  for (const item of body.split(',')) {
    const [d, times] = item.trim().split(' ')
    if (!d || !times || !times.includes('-')) continue
    const [st, et] = times.split('-')
    list.push({ date: d, startTime: st, endTime: et })
  }
  return list
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

.booking-item.approved {
  border-left: 4px solid #67c23a;
  background: linear-gradient(90deg, #f0f9ff 0%, #ffffff 20%);
}

.booking-item.rejected {
  border-left: 4px solid #f56c6c;
  background: linear-gradient(90deg, #fef0f0 0%, #ffffff 20%);
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

/* 响应式适配：小屏对话框全宽与表单布局（覆盖修正） */
@media (max-width: 768px) {
  /* el-dialog 全屏与表单标签置顶 */
  :deep(.el-dialog) { width: 100vw !important; max-width: 100vw !important; margin: 0 !important; }
  :deep(.el-dialog__body) { padding: 12px; }
  :deep(.el-dialog .el-form .el-form-item__label) { float: none; display: block; padding-bottom: 4px; }
  :deep(.el-dialog .el-form .el-form-item__content) { margin-left: 0 !important; }
}


</style>
