<script setup lang="ts">
import { ref, watch, onMounted, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { bookingAPI, teacherAPI, rescheduleAPI } from '../../utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'


import {
  HomeFilled,
  DocumentChecked,
  Calendar,
  Reading,
  Money,
  Setting,
  ChatDotRound,
  List,
  Refresh,
  Lock
} from '@element-plus/icons-vue'

// 导入消息组件
import TeacherMessages from './components/TeacherMessages.vue'
const RescheduleModal = defineAsyncComponent(() => import('../../components/RescheduleModal.vue'))
const TeacherHourDetails = defineAsyncComponent(() => import('./components/TeacherHourDetails.vue'))

// 课程安排接口定义
interface ScheduleItem {
  id: number
  scheduledDate: string
  startTime: string
  endTime: string
  courseName: string
  studentName: string
  status: string
  isTrial: boolean
  teacherId?: number
  teacherName?: string
  studentId?: number
  courseId?: number
  durationMinutes?: number
  courseTitle?: string
  subjectName?: string
}

const userStore = useUserStore()
const route = useRoute()
const activeMenu = ref('dashboard')
const todayCourses = ref<ScheduleItem[]>([])
const loading = ref(false)
// 即将开始筛选范围（周）
const upcomingWeeks = ref(2)

// 当月日历数据
const calendarLoading = ref(false)
const monthDate = ref(new Date())
const monthSchedules = ref<ScheduleItem[]>([])

// 统计数据
const statistics = ref({
  rescheduleRequests: 0,
  totalCourses: 0,
  upcomingClasses: 0,
  bookingRequests: 0,
  currentHours: 0,
  lastHours: 0,
  monthlyRescheduleCount: 0
})
const statsLoading = ref(false)
// 工资统计（基于时薪 * 课时）
const salary = ref({ currentSalary: 0, lastSalary: 0 })

// 课时详情模态框
const showHourDetailsModal = ref(false)
const hourDetails = ref({
  normalHours: 0,        // 正常上课的课时数
  teacherDeduction: 0,   // 教师超出调课/请假次数后所扣课时
  studentCompensation: 0, // 学生超出调课/请假次数的补偿课时
  adminAdjustment: 0,    // 总Admin手动调整记录
  totalHours: 0          // 总课时
})
const hourDetailsLoading = ref(false)

// 调课弹窗
const showRescheduleModal = ref(false)
const rescheduleCourse = ref<any>(null)

// 根据当前路由设置激活菜单
watch(() => route.path, (path: string) => {
  if (path.includes('/profile')) {
    activeMenu.value = 'profile'
  } else if (path.includes('/password')) {
    activeMenu.value = 'password'
  } else if (path.includes('/schedule')) {
    activeMenu.value = 'schedule'
  } else if (path.includes('/bookings')) {
    activeMenu.value = 'bookings'
  } else if (path.includes('/reschedule')) {
    activeMenu.value = 'reschedule'

  } else {
    activeMenu.value = 'dashboard'
  }
}, { immediate: true })

// 获取即将开始的课程
const fetchUpcomingCourses = async () => {
  try {
    loading.value = true
    const today = new Date().toISOString().split('T')[0]
    const rangeDays = upcomingWeeks.value * 7
    const nextDate = new Date(Date.now() + rangeDays * 24 * 60 * 60 * 1000).toISOString().split('T')[0]

    const response = await bookingAPI.getTeacherSchedules({
      startDate: today,
      endDate: nextDate
    })

    if (response.success && response.data) {
      // 过滤即将开始的课程并按时间排序
      const upcoming_schedules = response.data.filter((schedule: ScheduleItem) =>
        schedule.status === 'progressing'
      ).sort((a: ScheduleItem, b: ScheduleItem) => {
        // 先按日期排序，再按时间排序
        const dateCompare = a.scheduledDate.localeCompare(b.scheduledDate)
        if (dateCompare !== 0) return dateCompare
        return a.startTime.localeCompare(b.startTime)
      }).slice(0, 10) // 只显示最近的10个课程

      todayCourses.value = upcoming_schedules
    } else {
      ElMessage.error(response.message || '获取即将开始课程失败')
    }
  } catch (error) {
    console.error('获取即将开始课程失败:', error)
    ElMessage.error('获取即将开始课程失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 格式化时间显示
const formatTimeRange = (startTime: string, endTime: string) => {
  return `${startTime}-${endTime}`
}

// 加载当月课表（用于日历渲染）
const loadMonthSchedules = async () => {
  try {
    calendarLoading.value = true
    const d = new Date(monthDate.value)
    const start = new Date(d.getFullYear(), d.getMonth(), 1)
    const end = new Date(d.getFullYear(), d.getMonth() + 1, 0)
    const startStr = start.toISOString().split('T')[0]
    const endStr = end.toISOString().split('T')[0]
    const res = await bookingAPI.getTeacherSchedules({ startDate: startStr, endDate: endStr })
    if (res.success && res.data) {
      monthSchedules.value = res.data
    } else {
      ElMessage.error(res.message || '获取当月课表失败')
    }
  } catch (e) {
    console.error('加载当月课表失败:', e)
  } finally {
    calendarLoading.value = false
  }
}

const dateHasSchedule = (date: Date) => {
  const ds = date.toISOString().split('T')[0]
  return monthSchedules.value.some(s => s.scheduledDate === ds)
}

const dateScheduleStatus = (date: Date): 'none' | 'has' => {
  return dateHasSchedule(date) ? 'has' : 'none'
}

// 打开调课弹窗
const openRescheduleModal = (schedule: ScheduleItem) => {
  const enhancedSchedule = {
    id: schedule.id,
    teacherId: schedule.teacherId,
    teacherName: schedule.teacherName || '当前教师',
    studentId: schedule.studentId,
    studentName: schedule.studentName,
    courseId: schedule.courseId,
    courseTitle: schedule.courseTitle || schedule.courseName || '自定义课程',
    subjectName: schedule.subjectName || '未知科目',
    scheduledDate: schedule.scheduledDate,
    startTime: schedule.startTime,
    endTime: schedule.endTime,
    durationMinutes: schedule.durationMinutes || 120,
    totalTimes: 1,
    status: schedule.status || 'progressing',
    teacherNotes: undefined,
    studentFeedback: undefined,
    createdAt: undefined,
    bookingRequestId: undefined,
    bookingSource: undefined,
    isTrial: schedule.isTrial,
    sessionNumber: 1,
    courseType: 'regular'
  }

  rescheduleCourse.value = {
    id: schedule.courseId || schedule.id,
    title: schedule.courseTitle || schedule.courseName || '自定义课程',
    teacher: schedule.teacherName || '当前教师',
    teacherId: schedule.teacherId,
    subject: schedule.subjectName || '未知科目',
    remainingLessons: 1,
    weeklySchedule: [],
    schedules: [enhancedSchedule]
  }
  showRescheduleModal.value = true
}

// 请假（取消本次课程）
const applyLeave = async (schedule: ScheduleItem) => {
  try {
    const { value, action } = await ElMessageBox.prompt('请输入请假/取消原因', '请假申请', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPlaceholder: '请填写原因（必填）',
      inputValidator: (v: string) => !!v && v.trim().length > 0 || '请填写原因'
    })
    if (action !== 'confirm') return

    const res = await rescheduleAPI.createTeacherRequest({
      scheduleId: schedule.id,
      requestType: 'cancel',
      reason: value
    })
    if (res.success) {
      ElMessage.success('请假申请已提交，等待管理员审批')
    } else {
      ElMessage.error(res.message || '请假申请提交失败')
    }
  } catch (e: any) {
    if (e === 'cancel') return
    console.error('请假申请失败:', e)
    ElMessage.error(e?.message || '请假申请失败')
  }
}

const handleRescheduleSuccess = () => {
  fetchUpcomingCourses()
  loadMonthSchedules()
}

// 获取统计数据
const loadStatistics = async () => {
  try {
    statsLoading.value = true
    const [statsRes, profileRes] = await Promise.all([
      teacherAPI.getStatistics(),
      teacherAPI.getProfile()
    ])

    if (statsRes.success && statsRes.data) {
      statistics.value = {
        rescheduleRequests: statsRes.data.rescheduleRequests || 0,
        totalCourses: statsRes.data.totalCourses || 0,
        upcomingClasses: statsRes.data.upcomingClasses || 0,
        bookingRequests: statsRes.data.bookingRequests || 0,
        currentHours: statsRes.data.currentHours || 0,
        lastHours: statsRes.data.lastHours || 0,
        monthlyRescheduleCount: statsRes.data.monthlyRescheduleCount || 0
      }
    } else {
      ElMessage.error(statsRes.message || '获取统计数据失败')
    }

    // 基于资料中的时薪计算工资
    if (profileRes.success && profileRes.data) {
      const rate = Number(profileRes.data.hourlyRate || 0)
      const ch = Number(statistics.value.currentHours || 0)
      const lh = Number(statistics.value.lastHours || 0)
      salary.value.currentSalary = Math.round(rate * ch * 100) / 100
      salary.value.lastSalary = Math.round(rate * lh * 100) / 100
    } else {
      ElMessage.error(profileRes.message || '获取教师资料失败')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败，请稍后重试')
  } finally {
    statsLoading.value = false
  }
}

// 获取课时详情
const loadHourDetails = async () => {
  try {
    hourDetailsLoading.value = true
    const res = await teacherAPI.getHourDetails()
    if (res.success && res.data) {
      hourDetails.value = res.data
    } else {
      ElMessage.error(res.message || '获取课时详情失败')
    }
  } catch (error) {
    console.error('获取课时详情失败:', error)
    ElMessage.error('获取课时详情失败，请稍后重试')
  } finally {
    hourDetailsLoading.value = false
  }
}

// 组件挂载时获取数据
onMounted(async () => {
  // 确保用户头像已加载
  if (userStore.user && !userStore.user.avatarUrl) {
    await userStore.loadUserAvatar()
  }
  fetchUpcomingCourses()
  loadStatistics()
  loadMonthSchedules()
})
</script>

<template>
  <div class="teacher-center">
    <el-container>
      <el-aside width="250px">
        <div class="sidebar">
          <div class="user-info">
            <div class="avatar">
              <el-avatar :size="64" :src="userStore.user?.avatarUrl || $getImageUrl('@/assets/pictures/teacherBoy2.jpeg')" />
            </div>
            <div class="info">
              <h3>{{ userStore.user?.username }}</h3>
              <p>教师</p>
            </div>
          </div>
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical"
            @select="activeMenu = $event"
          >
            <el-menu-item index="dashboard" @click="$router.push('/teacher-center')">
              <el-icon><HomeFilled /></el-icon>
              <span>我的课表</span>
            </el-menu-item>
            <el-menu-item index="bookings" @click="$router.push('/teacher-center/bookings')">
              <el-icon><DocumentChecked /></el-icon>
              <span>调课记录</span>
            </el-menu-item>

            <el-menu-item index="my-hour-details">
              <el-icon><List /></el-icon>
              <span>上课记录</span>
            </el-menu-item>


            <el-menu-item index="messages">
              <el-icon><ChatDotRound /></el-icon>
              <span>消息中心</span>
            </el-menu-item>

            <el-menu-item index="profile" @click="$router.push('/teacher-center/profile')">
              <el-icon><Setting /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="password" @click="$router.push('/teacher-center/password')">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-aside>
      <el-main>
        <div v-if="activeMenu === 'dashboard'" class="dashboard">
          <h2>我的课表</h2>
          <div class="dashboard-stats">
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Refresh /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.monthlyRescheduleCount }}</div>
                <div class="stat-label">本月已申请调课/请假次数</div>
              </div>
            </div>
            <div class="stat-card clickable" @click="showHourDetailsModal = true">
              <div class="stat-icon">
                <el-icon><Money /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.currentHours }}</div>
                <div class="stat-label">本月累计课时(小时)</div>
                <div class="stat-hint">点击查看详情</div>
              </div>
            </div>
          </div>

          <div class="calendar-section">
            <div class="calendar-header">
              <h3>当月课表</h3>
              <div class="calendar-actions">
                <el-date-picker
                  v-model="monthDate"
                  type="month"
                  placeholder="选择月份"
                  format="YYYY-MM"
                  value-format="YYYY-MM-DD"
                  @change="loadMonthSchedules"
                  style="width: 160px;"
                />
                <el-button type="primary" @click="loadMonthSchedules" :loading="calendarLoading" style="margin-left: 8px;">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </div>
            <el-calendar v-model="monthDate">
              <template #date-cell="{ data }">
                <div class="calendar-cell" :class="dateScheduleStatus(data.date) === 'has' ? 'has-schedule' : 'no-schedule'">
                  <span class="date-number">{{ data.day.split('-').slice(-1)[0] }}</span>
                  <span v-if="dateScheduleStatus(data.date) === 'has'" class="dot"></span>
                </div>
              </template>
            </el-calendar>
            <div class="calendar-legend">
              <span><i class="dot"></i> 有课程</span>
              <span class="muted">无课程</span>
            </div>
          </div>

          <div class="quick-actions">
            <h3>快捷操作</h3>
            <div class="action-buttons">

              <el-button type="warning" @click="$router.push({ path: '/about', hash: '#contact-us' })">
                <el-icon><ChatLineRound /></el-icon>
                联系客服
              </el-button>
            </div>
          </div>

          <div class="upcoming-courses">
            <div class="upcoming-header">
              <h3>即将开始课程</h3>
              <div class="upcoming-actions">
                <el-select v-model="upcomingWeeks" style="width: 140px;" @change="fetchUpcomingCourses">
                  <el-option :value="1" label="1周内" />
                  <el-option :value="2" label="2周内" />
                  <el-option :value="4" label="4周内" />
                </el-select>
              </div>
            </div>
            <div v-loading="loading">
              <el-table :data="todayCourses" style="width: 100%">
                <el-table-column prop="scheduledDate" label="日期" width="120" />
                <el-table-column label="时间" width="140">
                  <template #default="scope">
                    {{ formatTimeRange(scope.row.startTime, scope.row.endTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="courseName" label="课程" />
                <el-table-column prop="studentName" label="学生" width="120" />
                <el-table-column label="类型" width="80">
                  <template #default="scope">
                    <el-tag v-if="scope.row.isTrial" type="warning" size="small">试听</el-tag>
                    <el-tag v-else type="success" size="small">正式</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="260">
                  <template #default="scope">
                    <el-button type="primary" size="small">详情</el-button>
                    <el-button type="warning" size="small" @click="openRescheduleModal(scope.row)">
                      <el-icon><Refresh /></el-icon>
                      申请调课
                    </el-button>
                    <el-button type="info" size="small" @click="applyLeave(scope.row)">请假</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!loading && todayCourses.length === 0" class="empty-state">
                <el-empty description="暂无即将开始的课程" />
              </div>
            </div>
          </div>


        </div>
        <div v-else-if="activeMenu === 'messages'">
          <TeacherMessages />
        </div>

        <div v-else-if="activeMenu === 'my-hour-details'">
          <TeacherHourDetails />
        </div>

        <div v-else>
          <router-view></router-view>
        </div>
      </el-main>
    </el-container>

    <RescheduleModal
      v-model="showRescheduleModal"
      :course="rescheduleCourse"
      :is-teacher="true"
      @success="handleRescheduleSuccess"
    />

    <!-- 课时详情模态框 -->
    <el-dialog
      v-model="showHourDetailsModal"
      title="本月课时详情"
      width="600px"
      @open="loadHourDetails"
    >
      <div v-loading="hourDetailsLoading" class="hour-details-content">
        <div class="detail-item">
          <div class="detail-label">正常上课的课时数：</div>
          <div class="detail-value positive">{{ hourDetails.normalHours }}小时</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">教师超出调课/请假次数后所扣课时：</div>
          <div class="detail-value negative">-{{ hourDetails.teacherDeduction }}小时</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">学生超出调课/请假次数的补偿课时：</div>
          <div class="detail-value positive">+{{ hourDetails.studentCompensation }}小时</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">Admin手动调整记录：</div>
          <div class="detail-value" :class="hourDetails.adminAdjustment >= 0 ? 'positive' : 'negative'">
            {{ hourDetails.adminAdjustment >= 0 ? '+' : '' }}{{ hourDetails.adminAdjustment }}小时
          </div>
        </div>
        <el-divider />
        <div class="detail-item total">
          <div class="detail-label">本月累计课时：</div>
          <div class="detail-value total-value">{{ hourDetails.totalHours }}小时</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showHourDetailsModal = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>



<style scoped>
.teacher-center {
  height: calc(100vh - 60px);
}

.el-container {
  height: 100%;
}

.el-aside {
  background-color: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.user-info {
  padding: 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #eee;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 15px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info h3 {
  margin: 0 0 5px 0;
  font-size: 16px;
}

.info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.el-menu-vertical {
  border-right: none;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}

.dashboard h2 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  background-color: #ecf5ff;
  color: #409EFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.quick-actions, .upcoming-courses {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.calendar-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.calendar-cell {
  position: relative;
  padding: 4px 6px;
}

.calendar-cell .date-number {
  font-size: 12px;
}

.calendar-cell .dot {
  position: absolute;
  right: 6px;
  bottom: 6px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #67c23a;
}

.calendar-legend {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.calendar-legend .dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  margin-right: 6px;
}

.calendar-legend .muted {
  color: #999;
}

.upcoming-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.quick-actions h3, .upcoming-courses h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.action-buttons {
  display: flex;
  gap: 15px;
}

/* 课时详情模态框样式 */
.clickable {
  cursor: pointer;
  transition: all 0.3s ease;
}

.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.1);
}

.stat-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.hour-details-content {
  padding: 20px 0;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item.total {
  font-weight: bold;
  font-size: 16px;
  padding: 16px 0;
}

.detail-label {
  color: #666;
  flex: 1;
}

.detail-value {
  font-weight: 600;
  font-size: 16px;
}

.detail-value.positive {
  color: #67c23a;
}

.detail-value.negative {
  color: #f56c6c;
}

.detail-value.total-value {
  color: #409eff;
  font-size: 18px;
}

/* 响应式布局 */
@media (max-width: 992px) {
  .el-aside {
    width: 200px !important;
  }

  .stat-card {
    width: calc(50% - 20px);
  }
}

@media (max-width: 768px) {
  .teacher-center {
    padding-top: 56px; /* 适配移动端导航高度 */
  }

  .el-container {
    flex-direction: column;
    height: calc(100vh - 56px);
  }

  .el-aside {
    width: 100% !important;
    height: auto;
    order: 2; /* 将侧边栏移到底部 */
    box-shadow: 0 -2px 12px 0 rgba(0, 0, 0, 0.05);
  }

  .el-main {
    padding: 16px;
    order: 1;
    overflow-y: auto;
    flex: 1;
  }

  .sidebar {
    height: auto;
  }

  .user-info {
    flex-direction: row;
    padding: 12px 16px;
    align-items: center;
  }

  .avatar {
    margin-right: 12px;
    margin-bottom: 0;
  }

  .info h3 {
    font-size: 14px;
    margin-bottom: 2px;
  }

  .info p {
    font-size: 12px;
    color: #999;
  }

  .el-menu-vertical {
    display: flex;
    overflow-x: auto;
    border-right: none;
    border-bottom: 1px solid #e6e6e6;
  }

  .el-menu-vertical .el-menu-item {
    flex-shrink: 0;
    white-space: nowrap;
    padding: 0 16px;
    height: 48px;
    line-height: 48px;
    border-bottom: none;
    border-right: 1px solid #e6e6e6;
  }

  .el-menu-vertical .el-menu-item:last-child {
    border-right: none;
  }

  .el-menu-vertical .el-menu-item span {
    margin-left: 8px;
    font-size: 13px;
  }

  .dashboard {
    padding: 0;
  }

  .dashboard h2 {
    font-size: 20px;
    margin-bottom: 16px;
  }

  .dashboard-stats {
    grid-template-columns: 1fr;
    gap: 12px;
    margin-bottom: 20px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-card h3 {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 24px;
  }

  .quick-actions h3,
  .upcoming-courses h3 {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 8px;
  }

  .action-buttons .el-button {
    width: 100%;
    margin-bottom: 0;
  }
}

@media (max-width: 480px) {
  .teacher-center {
    padding-top: 52px;
  }

  .el-container {
    height: calc(100vh - 52px);
  }

  .el-main {
    padding: 12px;
  }

  .user-info {
    padding: 10px 12px;
  }

  .dashboard-stats {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .stat-card {
    width: 100%;
    padding: 12px;
  }

  .el-menu-vertical .el-menu-item {
    padding: 0 12px;
    height: 44px;
    line-height: 44px;
  }

  .el-menu-vertical .el-menu-item span {
    font-size: 12px;
  }
}
</style>

