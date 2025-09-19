<template>
  <div class="student-booking-management">
    <div class="page-header">
      <div class="header-actions">
        <el-select v-model="statusFilter" placeholder="审批状态" style="width: 120px; margin-right: 12px;" @change="loadBookings">
          <el-option label="全部" value="" />
          <el-option label="待审批" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
        <el-select v-model="requestTypeFilter" placeholder="类型" style="width: 130px; margin-right: 12px;" @change="loadBookings">
          <el-option label="全部" value="" />
          <el-option label="调课" value="reschedule" />
          <el-option label="请假" value="cancel" />
        </el-select>
        <el-select v-model="yearFilter" placeholder="年份" style="width: 110px; margin-right: 12px;" @change="loadBookings">
          <el-option label="全部" :value="null" />
          <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
        </el-select>
        <el-select v-model="monthFilter" placeholder="月份" style="width: 110px; margin-right: 12px;" @change="loadBookings">
          <el-option label="全部" :value="null" />
          <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
        </el-select>
        <el-input v-model="courseNameFilter" placeholder="课程名称" clearable style="width: 160px; margin-right: 12px;" @keyup.enter="loadBookings" />
        <el-button type="primary" @click="loadBookings" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-card class="booking-list-card">
      <template #header>
        <div class="card-header">
          <span>调课/请假记录</span>
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
                {{ booking.teacherName?.charAt(0) || 'T' }}
              </el-avatar>
              <div class="student-details">
                <div class="student-name">{{ booking.teacherName }}</div>
                <div class="booking-time">{{ formatDateTime(booking.createdAt) }}</div>
              </div>
            </div>
            <div class="booking-status">
              <el-tag :type="getStatusTagType(booking.status)" size="large">
                {{ getStatusText(booking.status, booking.requestType) }}
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
                <span class="label">上课地点：</span>
                <span class="value">{{ booking.courseLocation || '-' }}</span>
              </div>

              <div class="detail-row">
                <span class="label">教师姓名：</span>
                <span class="value">{{ booking.teacherName || '-' }}</span>
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
              <div class="detail-item">
                <label>上课地点：</label>
                <span>{{ selectedBooking.courseLocation || '-' }}</span>
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

        <div v-if="selectedBooking.adminNotes && selectedBooking.status !== 'pending'" class="detail-section">
          <h3>管理员回复</h3>
          <div class="admin-reply-content">
            {{ cleanAdminNotes(selectedBooking.adminNotes) }}
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

type ApplicantType = 'student' | 'teacher'
type RequestType = 'single' | 'recurring' | 'cancel'
interface StudentBookingItem {
  id: string | number
  recordType: 'reschedule' | 'suspension'
  status: string
  createdAt: string
  courseTitle?: string
  studentName?: string
  teacherName?: string
  applicantName?: string
  applicantType?: ApplicantType
  requestType?: RequestType
  originalDate?: string
  originalStartTime?: string
  originalEndTime?: string
  newWeeklySchedule?: string
  newDate?: string
  newStartTime?: string
  newEndTime?: string
  startDate?: string
  endDate?: string
  reason?: string
  adminNotes?: string
}

const loading = ref(false)
const bookings = ref<StudentBookingItem[]>([])
const statusFilter = ref('')
const requestTypeFilter = ref<'reschedule' | 'cancel' | ''>('')
const yearFilter = ref<number | null>(null)
const monthFilter = ref<number | null>(null)
const courseNameFilter = ref('')
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 6 }).map((_, i) => currentYear - i)

const pagination = reactive({ current: 1, size: 10, total: 0 })

const showDetailModal = ref(false)
const selectedBooking = ref<StudentBookingItem | null>(null)

const bookingStats = computed(() => {
  const total = bookings.value.length
  const pending = bookings.value.filter(b => b.status === 'pending').length
  const approved = bookings.value.filter(b => b.status === 'approved').length
  return { total, pending, approved }
})

onMounted(() => { loadBookings() })

const loadBookings = async () => {
  loading.value = true
  try {
    const [rescheduleResult, suspensionResult] = await Promise.all([
      rescheduleAPI.getStudentRequests({ page: pagination.current, size: pagination.size, status: statusFilter.value || undefined, year: yearFilter.value || undefined, month: monthFilter.value || undefined }),
      suspensionAPI.getStudentRequests({ page: pagination.current, size: pagination.size, status: statusFilter.value || undefined, year: yearFilter.value || undefined, month: monthFilter.value || undefined })
    ])

    let allBookings: StudentBookingItem[] = []

    if (rescheduleResult.success && rescheduleResult.data) {
      const rescheduleRecords = (rescheduleResult.data.records || []) as unknown as StudentBookingItem[]
      const processedRescheduleRecords: StudentBookingItem[] = rescheduleRecords.map((record) => ({
        ...record,
        recordType: 'reschedule',
        adminNotes: (record as unknown as { reviewNotes?: string }).reviewNotes || record.adminNotes || ''
      }))
      allBookings = allBookings.concat(processedRescheduleRecords)
    }

    if (suspensionResult.success && suspensionResult.data) {
      const suspensionRecords = (suspensionResult.data.records || []) as unknown as StudentBookingItem[]
      const processedSuspensionRecords: StudentBookingItem[] = suspensionRecords.map((record) => ({
        ...record,
        recordType: 'suspension',
        requestType: 'cancel',
        id: `suspension_${(record as unknown as { id: string | number }).id}`,
        studentName: record.studentName || '未知学生',
        applicantName: record.applicantName || ((record.adminNotes || '').includes('[applicant=TEACHER]') ? (record.teacherName || '未知教师') : (record.studentName || '未知学生')),
        applicantType: record.applicantType || (((record.adminNotes || '').includes('[applicant=TEACHER]')) ? 'teacher' : 'student'),
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

    allBookings.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())

    // 应用筛选（类型与课程名本地，其它交由后端）
    let filtered = allBookings
    if (requestTypeFilter.value) {
      filtered = filtered.filter(r => requestTypeFilter.value === 'cancel' ? r.recordType === 'suspension' : r.recordType === 'reschedule')
    }
    if (courseNameFilter.value && courseNameFilter.value.trim()) {
      const kw = courseNameFilter.value.trim().toLowerCase()
      filtered = filtered.filter(r => (r.courseTitle || '').toLowerCase().includes(kw))
    }
    bookings.value = filtered
    pagination.total = filtered.length

    if (!rescheduleResult.success && !suspensionResult.success) {
      ElMessage.error('获取记录失败')
    }
  } catch (e) {
    console.error('获取记录失败:', e)
    ElMessage.error('获取记录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const viewBookingDetail = (booking: StudentBookingItem) => {
  selectedBooking.value = booking
  showDetailModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedBooking.value = null
}

const formatDateTime = (dateTimeStr: string) => {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  return date.toLocaleString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const formatTime = (time: string) => {
  if (!time) return ''
  return time.substring(0, 5)
}

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

const cleanAdminNotes = (notes?: string) => {
  if (!notes) return ''
  return notes.replace(/\[[^\]]*\]/g, '').trim()
}
</script>

<style scoped>
.student-booking-management { padding: 24px; background-color: #f5f7fa; min-height: 100vh; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-actions { display: flex; align-items: center; }
.booking-list-card { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1); border: none; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; color: #303133; }
.booking-list { display: flex; flex-direction: column; gap: 16px; }
.booking-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 20px; background-color: #fff; transition: all 0.3s ease; }
.booking-item.pending { border-left: 4px solid #e6a23c; background: linear-gradient(90deg, #fdf6ec 0%, #ffffff 20%); }
.booking-item.approved { border-left: 4px solid #67c23a; background: linear-gradient(90deg, #f0f9ff 0%, #ffffff 20%); }
.booking-item.rejected { border-left: 4px solid #f56c6c; background: linear-gradient(90deg, #fef0f0 0%, #ffffff 20%); }
.booking-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.student-info { display: flex; align-items: center; }
.student-details { display: flex; flex-direction: column; gap: 4px; }
.student-name { font-size: 16px; font-weight: 600; color: #303133; }
.booking-time { font-size: 12px; color: #909399; }
.booking-status { display: flex; align-items: center; gap: 8px; }
.booking-content { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; }
.booking-details { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.detail-row { display: flex; align-items: flex-start; gap: 8px; }
.detail-row .label { font-weight: 500; color: #606266; min-width: 80px; flex-shrink: 0; }
.detail-row .value { color: #303133; line-height: 1.5; }
.text-muted { color: #c0c4cc; font-style: italic; }
.pagination-wrapper { margin-top: 24px; display: flex; justify-content: center; }
.booking-detail { max-height: 60vh; overflow-y: auto; }
.detail-section { margin-bottom: 24px; }
.detail-section h3 { margin: 0 0 16px 0; color: #303133; font-size: 16px; font-weight: 600; border-bottom: 1px solid #ebeef5; padding-bottom: 8px; }
.detail-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 16px; }
.detail-item { display: flex; align-items: center; }
.detail-item label { font-weight: 500; color: #606266; min-width: 80px; margin-right: 8px; }
.detail-item span { color: #303133; }
.time-info { display: flex; flex-direction: column; gap: 12px; margin-top: 16px; }
.time-item { display: flex; align-items: center; }
.time-item label { font-weight: 500; color: #606266; min-width: 80px; margin-right: 8px; }
.time-item span { color: #303133; }
.requirements-content, .admin-reply-content { background-color: #f8f9fa; padding: 16px; border-radius: 8px; border-left: 4px solid #409eff; line-height: 1.6; color: #303133; }
.admin-reply-content { border-left-color: #e6a23c; }
</style>
