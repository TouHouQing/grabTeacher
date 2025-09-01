<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { bookingAPI, teacherAPI } from '../../utils/api'
import { ElMessage } from 'element-plus'


import {
  HomeFilled,
  DocumentChecked,
  Calendar,
  Reading,
  User,
  Money,
  Setting,
  VideoCamera,
  ChatDotRound
} from '@element-plus/icons-vue'

// 导入消息组件
import TeacherMessages from './components/TeacherMessages.vue'

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
}

const userStore = useUserStore()
const route = useRoute()
const activeMenu = ref('dashboard')
const todayCourses = ref<ScheduleItem[]>([])
const loading = ref(false)

// 统计数据
const statistics = ref({
  rescheduleRequests: 0,
  totalCourses: 0,
  upcomingClasses: 0,
  bookingRequests: 0
})
const statsLoading = ref(false)

// 根据当前路由设置激活菜单
watch(() => route.path, (path: string) => {
  if (path.includes('/profile')) {
    activeMenu.value = 'profile'
  } else if (path.includes('/schedule')) {
    activeMenu.value = 'schedule'
  } else if (path.includes('/courses')) {
    activeMenu.value = 'courses'
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
    const nextWeek = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]

    const response = await bookingAPI.getTeacherSchedules({
      startDate: today,
      endDate: nextWeek
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

// 获取统计数据
const loadStatistics = async () => {
  try {
    statsLoading.value = true
    const result = await teacherAPI.getStatistics()

    if (result.success && result.data) {
      statistics.value = {
        rescheduleRequests: result.data.rescheduleRequests || 0,
        totalCourses: result.data.totalCourses || 0,
        upcomingClasses: result.data.upcomingClasses || 0,
        bookingRequests: result.data.bookingRequests || 0
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

// 组件挂载时获取数据
onMounted(async () => {
  // 确保用户头像已加载
  if (userStore.user && !userStore.user.avatarUrl) {
    await userStore.loadUserAvatar()
  }
  fetchUpcomingCourses()
  loadStatistics()
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
              <span>控制台</span>
            </el-menu-item>
            <el-menu-item index="bookings" @click="$router.push('/teacher-center/bookings')">
              <el-icon><DocumentChecked /></el-icon>
              <span>预约管理</span>
            </el-menu-item>
            <el-menu-item index="schedule" @click="$router.push('/teacher-center/schedule')">
              <el-icon><Calendar /></el-icon>
              <span>课表管理</span>
            </el-menu-item>
            <el-menu-item index="courses" @click="$router.push('/teacher-center/courses')">
              <el-icon><Reading /></el-icon>
              <span>课程管理</span>
            </el-menu-item>


            <el-menu-item index="messages">
              <el-icon><ChatDotRound /></el-icon>
              <span>消息中心</span>
            </el-menu-item>

            <el-menu-item index="profile" @click="$router.push('/teacher-center/profile')">
              <el-icon><Setting /></el-icon>
              <span>个人设置</span>
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
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.rescheduleRequests }}</div>
                <div class="stat-label">调课申请数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Reading /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.totalCourses }}</div>
                <div class="stat-label">总课程数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><VideoCamera /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.upcomingClasses }}</div>
                <div class="stat-label">即将上课数</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><DocumentChecked /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statsLoading ? '-' : statistics.bookingRequests }}</div>
                <div class="stat-label">预约申请数</div>
              </div>
            </div>
          </div>

          <div class="quick-actions">
            <h3>快捷操作</h3>
            <div class="action-buttons">
              <el-button type="success">
                <el-icon><VideoCamera /></el-icon>
                开始上课
              </el-button>
              <el-button type="primary" @click="$router.push('/teacher-center/schedule')">
                <el-icon><Calendar /></el-icon>
                查看课表
              </el-button>
              <el-button type="warning">
                <el-icon><ChatLineRound /></el-icon>
                联系客服
              </el-button>
            </div>
          </div>

          <div class="upcoming-courses">
            <h3>即将开始课程</h3>
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
                <el-table-column label="操作" width="180">
                  <template #default>
                    <el-button type="primary" size="small">进入教室</el-button>
                    <el-button type="info" size="small">查看详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!loading && todayCourses.length === 0" class="empty-state">
                <el-empty description="暂无即将开始的课程" />
              </div>
            </div>
          </div>


        </div>
        <div v-else-if="activeMenu === 'courses'">
          <router-view v-if="$route.path.includes('/courses')" />
          <div v-else>
            <h3>课程管理</h3>
            <p>请点击菜单中的"课程管理"进入课程管理页面。</p>
            <el-button type="primary" @click="$router.push('/teacher-center/courses')">
              进入课程管理
            </el-button>
          </div>
        </div>

        <div v-else-if="activeMenu === 'messages'">
          <TeacherMessages />
        </div>

        <div v-else>
          <router-view></router-view>
        </div>
      </el-main>
    </el-container>
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
  grid-template-columns: repeat(4, 1fr);
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
    grid-template-columns: repeat(2, 1fr);
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
