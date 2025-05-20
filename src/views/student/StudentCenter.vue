<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '../../stores/user'
import StudentCourses from './components/StudentCourses.vue'
import StudentSchedule from './components/StudentSchedule.vue'
import StudentMessages from './components/StudentMessages.vue'

const userStore = useUserStore()
const activeMenu = ref('dashboard')
</script>

<template>
  <div class="student-center">
    <el-container>
      <el-aside width="250px">
        <div class="sidebar">
          <div class="user-info">
            <div class="avatar">
              <img :src="$getImageUrl('@/assets/pictures/studentBoy2.jpeg')" alt="用户头像">
            </div>
            <div class="info">
              <h3>{{ userStore.username }}</h3>
              <p>学生</p>
            </div>
          </div>
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical"
            @select="activeMenu = $event"
          >
            <el-menu-item index="dashboard">
              <el-icon><HomeFilled /></el-icon>
              <span>控制台</span>
            </el-menu-item>
            <el-menu-item index="match" @click="$router.push('/student-center/match')">
              <el-icon><Connection /></el-icon>
              <span>智能匹配教师</span>
            </el-menu-item>
            <el-menu-item index="orders" @click="$router.push('/student-center/orders')">
              <el-icon><Tickets /></el-icon>
              <span>我的订单</span>
            </el-menu-item>
            <el-menu-item index="courses">
              <el-icon><Reading /></el-icon>
              <span>我的课程</span>
            </el-menu-item>
            <el-menu-item index="schedule">
              <el-icon><Calendar /></el-icon>
              <span>上课安排</span>
            </el-menu-item>
            <el-menu-item index="messages">
              <el-icon><ChatLineRound /></el-icon>
              <span>消息中心</span>
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
          <h2>欢迎回来，{{ userStore.username }}!</h2>
          <div class="dashboard-stats">
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Reading /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">2</div>
                <div class="stat-label">进行中的课程</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">3</div>
                <div class="stat-label">今日课程</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Tickets /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">5</div>
                <div class="stat-label">完成课程</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">18</div>
                <div class="stat-label">学习小时</div>
              </div>
            </div>
          </div>

          <div class="quick-actions">
            <h3>快捷操作</h3>
            <div class="action-buttons">
              <el-button type="primary" @click="$router.push('/student-center/match')">
                <el-icon><Connection /></el-icon>
                智能匹配教师
              </el-button>
              <el-button type="success" @click="activeMenu = 'schedule'">
                <el-icon><Calendar /></el-icon>
                查看课表
              </el-button>
              <el-button type="warning" @click="activeMenu = 'messages'">
                <el-icon><ChatLineRound /></el-icon>
                联系客服
              </el-button>
            </div>
          </div>

          <div class="upcoming-courses">
            <h3>即将开始的课程</h3>
            <el-table :data="upcomingCourses" style="width: 100%">
              <el-table-column prop="date" label="日期" width="180" />
              <el-table-column prop="time" label="时间" width="180" />
              <el-table-column prop="course" label="课程" />
              <el-table-column prop="teacher" label="教师" />
              <el-table-column label="操作">
                <template #default>
                  <el-button type="primary" size="small">进入课堂</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
        <div v-else-if="activeMenu === 'match'">
          <router-view></router-view>
        </div>
        <div v-else-if="activeMenu === 'orders'">
          <router-view></router-view>
        </div>
        <div v-else-if="activeMenu === 'courses'">
          <StudentCourses />
        </div>
        <div v-else-if="activeMenu === 'schedule'">
          <StudentSchedule />
        </div>
        <div v-else-if="activeMenu === 'messages'">
          <StudentMessages />
        </div>
        <div v-else-if="activeMenu === 'profile'">
          <router-view></router-view>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script lang="ts">
export default {
  name: 'StudentCenterView',
  data() {
    return {
      upcomingCourses: [
        {
          date: '2023-07-10',
          time: '14:00-16:00',
          course: '初中数学 - 函数与导数',
          teacher: '张老师'
        },
        {
          date: '2023-07-12',
          time: '16:00-18:00',
          course: '初中物理 - 力学与电学',
          teacher: '王老师'
        },
        {
          date: '2023-07-15',
          time: '10:00-12:00',
          course: '初中数学 - 函数与导数',
          teacher: '张老师'
        }
      ]
    }
  }
}
</script>

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

@media (max-width: 768px) {
  .el-aside {
    width: 60px !important;
  }

  .user-info {
    flex-direction: column;
    padding: 10px;
  }

  .avatar {
    margin-right: 0;
    margin-bottom: 10px;
  }

  .info h3, .info p {
    display: none;
  }

  .dashboard-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>
