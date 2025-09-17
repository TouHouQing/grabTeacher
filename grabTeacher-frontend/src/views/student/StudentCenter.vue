<script setup lang="ts">
import { ref, watch, onMounted, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { HomeFilled, Connection, Document, Reading, User, Coin, ChatDotRound, Star, List } from '@element-plus/icons-vue'
import StudentCourses from './components/StudentCourses.vue'
import StudentMessages from './components/StudentMessages.vue'
const StudentBalanceDetails = defineAsyncComponent(() => import('./components/StudentBalanceDetails.vue'))
import { bookingAPI, studentAPI } from '../../utils/api'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const activeMenu = ref('dashboard')
const upcomingCourses = ref<UpcomingCourse[]>([])
const loading = ref(false)

// 统计数据
const statistics = ref({
  totalBookings: 0,
  pendingBookings: 0,
  monthlyAdjustments: 0,
  completedCourses: 0
})
const statsLoading = ref(false)

// 学生余额/调课次数
const studentBalance = ref(0)
const balanceLoading = ref(false)
const studentAdjustmentTimes = ref<number | null>(null)

// 根据当前路由设置激活菜单
watch(() => route.path, (path: string) => {
  if (path.includes('/profile')) {
    activeMenu.value = 'profile'
  } else if (path.includes('/trial')) {
    activeMenu.value = 'trial'
  } else if (path.includes('/match')) {
    activeMenu.value = 'match'
  } else {
    activeMenu.value = 'dashboard'
  }
}, { immediate: true })

// 定义课程安排类型
interface ScheduleResponse {
  id: number;
  teacherId: number;
  teacherName: string;
  studentId: number;
  studentName: string;
  courseId: number;
  courseTitle: string;
  scheduledDate: string;
  startTime: string;
  endTime: string;
  status: string;
  [key: string]: unknown;
}

// 定义即将开始课程类型
interface UpcomingCourse {
  date: string;
  time: string;
  course: string;
  teacher: string;
  scheduleId: number;
}

// 获取即将开始的课程
const loadUpcomingCourses = async () => {
  try {
    loading.value = true

    // 获取未来30天的课程安排
    const startDate = new Date().toISOString().split('T')[0]
    const endDate = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]

    const result = await bookingAPI.getStudentSchedules({
      startDate,
      endDate
    })

    if (result.success && result.data) {
      // 筛选出状态为progressing且日期在今天或之后的课程
      const today = new Date()
      today.setHours(0, 0, 0, 0)

      const upcomingSchedules = result.data
        .filter((schedule: ScheduleResponse) =>
          schedule.status === 'progressing' &&
          new Date(schedule.scheduledDate) >= today
        )
        .sort((a: ScheduleResponse, b: ScheduleResponse) =>
          new Date(a.scheduledDate).getTime() - new Date(b.scheduledDate).getTime()
        )
        .slice(0, 5) // 只显示最近的5个课程

      // 转换为表格所需的格式
      upcomingCourses.value = upcomingSchedules.map((schedule: ScheduleResponse): UpcomingCourse => ({
        date: schedule.scheduledDate,
        time: `${schedule.startTime}-${schedule.endTime}`,
        course: schedule.courseTitle || '未指定课程',
        teacher: schedule.teacherName || '未指定教师',
        scheduleId: schedule.id
      }))
    } else {
      ElMessage.error(result.message || '获取课程安排失败')
    }
  } catch (error) {
    console.error('获取即将开始的课程失败:', error)
    ElMessage.error('获取课程安排失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 进入课堂
const enterClassroom = (scheduleId: number) => {
  // 跳转到课堂页面
  router.push(`/classroom/${scheduleId}`)
}

// 获取统计数据
const loadStatistics = async () => {
  try {
    statsLoading.value = true
    const result = await studentAPI.getStatistics()

    if (result.success && result.data) {
              statistics.value = {
          totalBookings: result.data.totalBookings || 0,
          pendingBookings: result.data.pendingBookings || 0,
          monthlyAdjustments: result.data.monthlyAdjustments || 0,
          completedCourses: result.data.completedCourses || 0
        }
    } else {
      ElMessage.error(result.message || '获取统计数据失败')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败，请稍后重试')
  } finally {
    statsLoading.value = false
  }
}

// 获取学生余额和调课次数
const loadStudentBalance = async () => {
  try {
    balanceLoading.value = true
    const result = await studentAPI.getProfile()

    if (result.success && result.data) {
      studentBalance.value = result.data.balance || 0
      studentAdjustmentTimes.value = result.data.adjustmentTimes ?? null
    } else {
      ElMessage.error(result.message || '获取余额失败')
    }
  } catch (error) {
    console.error('获取余额失败:', error)
    ElMessage.error('获取余额失败，请稍后重试')
  } finally {
    balanceLoading.value = false
  }
}

// 页面加载时获取数据
onMounted(async () => {
  // 确保用户头像已加载
  if (userStore.user) {
    await userStore.loadUserAvatar()
  }
  loadUpcomingCourses()
  loadStatistics()
  loadStudentBalance()
})
</script>

<template>
  <div class="student-center">
    <el-container>
      <el-aside width="250px">
        <div class="sidebar">
          <div class="user-info">
            <div class="avatar">
              <img :src="(userStore.user?.avatarUrl) || $getImageUrl('@/assets/pictures/studentBoy2.jpeg')" alt="用户头像">
            </div>
            <div class="info">
              <h3>{{ userStore.user?.username }}</h3>
              <p>学生</p>
            </div>
          </div>
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical"
            @select="activeMenu = $event"
          >
            <el-menu-item index="dashboard" @click="$router.push('/student-center')">
              <el-icon><HomeFilled /></el-icon>
              <span>控制台</span>
            </el-menu-item>
            <el-menu-item index="trial" @click="$router.push('/student-center/trial')">
              <el-icon><Connection /></el-icon>
              <span>预约免费试听课</span>
            </el-menu-item>
            <el-menu-item index="match" @click="$router.push('/student-center/match')">
              <el-icon><Connection /></el-icon>
              <span>1V1教师智能匹配</span>
            </el-menu-item>

            <el-menu-item index="courses">
              <el-icon><Reading /></el-icon>
              <span>我的课程</span>
            </el-menu-item>

            <el-menu-item index="messages">
              <el-icon><ChatDotRound /></el-icon>
              <span>消息中心</span>
            </el-menu-item>

            <el-menu-item index="my-balance-details">
              <el-icon><List /></el-icon>
              <span>余额明细</span>
            </el-menu-item>

            <el-menu-item index="profile" @click="$router.push('/student-center/profile')">
              <el-icon><User /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-aside>
      <el-main>
        <div v-if="activeMenu === 'dashboard'" class="dashboard">
          <h2>欢迎回来，{{ userStore.user?.realName }}!</h2>
          <div class="dashboard-stats">
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Coin /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ balanceLoading ? '-' : studentBalance }}</div>
                <div class="stat-label">M豆余额</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Star /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.totalBookings }}</div>
                <div class="stat-label">总预约课次</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Reading /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.pendingBookings }}</div>
                <div class="stat-label">待审批课次</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.monthlyAdjustments }}</div>
                <div class="stat-label">本月已调课/取消课程次数</div>
              </div>
            </div>
          </div>

          <div class="quick-actions">
            <h3>快捷操作</h3>
            <div class="action-buttons">
              <el-button type="primary" @click="$router.push('/student-center/trial')">
                <el-icon><Connection /></el-icon>
                预约免费试听课
              </el-button>

              <el-button @click="$router.push('/student-center/match')">
                <el-icon><Connection /></el-icon>
                智能匹配教师
              </el-button>

              <el-button type="warning" @click="$router.push({ path: '/about', hash: '#contact-us' })">
                <el-icon><Connection /></el-icon>
                联系客服
              </el-button>
            </div>
          </div>

          <div class="upcoming-courses">
            <h3>即将开始的课程</h3>
            <div v-if="loading" class="loading-container">
              <el-skeleton :rows="3" animated />
            </div>
            <el-empty v-else-if="upcomingCourses.length === 0" description="暂无即将开始的课程">
              <el-button type="primary" @click="loadUpcomingCourses">刷新</el-button>
            </el-empty>
            <div v-else class="table-wrap">
              <el-table :data="upcomingCourses" style="width: 100%">
                <el-table-column prop="date" label="日期" width="180" />
                <el-table-column prop="time" label="时间" width="180" />
                <el-table-column prop="course" label="课程" />
                <el-table-column prop="teacher" label="教师" />
                <el-table-column label="操作">
                  <template #default="scope">
                    <el-button type="primary" size="small" @click="enterClassroom(scope.row.scheduleId)">
                      进入课堂
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
        <div v-else-if="activeMenu === 'courses'">
          <StudentCourses />
        </div>
        <div v-else-if="activeMenu === 'messages'">
          <StudentMessages />
        </div>

        <div v-else-if="activeMenu === 'my-balance-details'">
          <StudentBalanceDetails />
        </div>


        <div v-else>
          <router-view></router-view>
        </div>
      </el-main>
    </el-container>
  </div>
</template>



<style scoped>
.student-center {
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
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  margin-bottom: 30px;
}

.stat-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 15px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background-color: #ecf5ff;
  color: #409EFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 12px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin-bottom: 3px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

.quick-actions {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.quick-actions h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #333;
}

.action-buttons {
  display: flex;
  gap: 15px;
}

.upcoming-courses {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.upcoming-courses h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #333;

}

/* 表格横向滚动兜底，避免小屏列拥挤溢出 */
.table-wrap { width: 100%; overflow-x: auto; }
.table-wrap :deep(table) { min-width: 720px; }


/* 响应式布局 */
@media (max-width: 768px) {
  .student-center {
    height: calc(100vh - 56px); /* 适配移动端导航高度 */
    padding-top: 56px;
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
    width: 40px;
    height: 40px;
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
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
    margin-bottom: 20px;
  }

  .stat-card {
    padding: 12px;
  }

  .stat-card h3 {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .stat-value {
    font-size: 24px;
  }

  .quick-actions h3,
  .upcoming-courses h3,
  .recent-activities h3 {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .loading-container {
    padding: 20px;
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
  .student-center {
    height: calc(100vh - 52px);
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

  .avatar {
    width: 36px;
    height: 36px;
    margin-right: 10px;
  }

  .dashboard-stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .stat-card {
    padding: 10px;
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
