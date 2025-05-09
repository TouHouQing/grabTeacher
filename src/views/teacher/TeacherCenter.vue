<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '../../stores/user'
import TeacherStudents from './components/TeacherStudents.vue'
import TeacherMaterials from './components/TeacherMaterials.vue'
import TeacherMessages from './components/TeacherMessages.vue'
import TeacherSchedule from './components/TeacherSchedule.vue'
import TeacherIncome from './components/TeacherIncome.vue'

const userStore = useUserStore()
const activeMenu = ref('dashboard')
</script>

<template>
  <div class="teacher-center">
    <el-container>
      <el-aside width="250px">
        <div class="sidebar">
          <div class="user-info">
            <div class="avatar">
              <el-avatar :size="64">
                <img src="@/assets/pictures/teacherBoy2.jpeg" alt="用户头像">
              </el-avatar>
            </div>
            <div class="info">
              <h3>{{ userStore.username }}</h3>
              <p>教师</p>
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
            <el-menu-item index="orders" @click="$router.push('/teacher-center/orders')">
              <el-icon><Tickets /></el-icon>
              <span>订单管理</span>
            </el-menu-item>
            <el-menu-item index="schedule">
              <el-icon><Calendar /></el-icon>
              <span>课表管理</span>
            </el-menu-item>
            <el-menu-item index="students">
              <el-icon><User /></el-icon>
              <span>学生管理</span>
            </el-menu-item>
            <el-menu-item index="materials">
              <el-icon><Document /></el-icon>
              <span>教学资料</span>
            </el-menu-item>
            <el-menu-item index="messages">
              <el-icon><ChatLineRound /></el-icon>
              <span>消息中心</span>
            </el-menu-item>
            <el-menu-item index="income">
              <el-icon><Money /></el-icon>
              <span>收入管理</span>
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
          <h2>欢迎回来，{{ userStore.username }}!</h2>
          <div class="dashboard-stats">
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">2</div>
                <div class="stat-label">我的学生</div>
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
                <div class="stat-value">2</div>
                <div class="stat-label">新订单</div>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Money /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">¥3600</div>
                <div class="stat-label">本月收入</div>
              </div>
            </div>
          </div>

          <div class="quick-actions">
            <h3>快捷操作</h3>
            <div class="action-buttons">
              <el-button type="primary" @click="activeMenu = 'orders'">
                <el-icon><View /></el-icon>
                查看新订单
              </el-button>
              <el-button type="success">
                <el-icon><VideoCamera /></el-icon>
                开始上课
              </el-button>
              <el-button type="warning">
                <el-icon><ChatLineRound /></el-icon>
                联系客服
              </el-button>
            </div>
          </div>

          <div class="upcoming-courses">
            <h3>今日课程</h3>
            <el-table :data="todayCourses" style="width: 100%">
              <el-table-column prop="time" label="时间" width="180" />
              <el-table-column prop="course" label="课程" />
              <el-table-column prop="student" label="学生" />
              <el-table-column label="操作">
                <template #default>
                  <el-button type="primary" size="small">进入教室</el-button>
                  <el-button type="info" size="small">查看详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="new-orders">
            <h3>最新订单</h3>
            <el-table :data="newOrders" style="width: 100%">
              <el-table-column prop="date" label="日期" width="180" />
              <el-table-column prop="student" label="学生" />
              <el-table-column prop="course" label="课程" />
              <el-table-column prop="status" label="状态">
                <template #default="scope">
                  <el-tag
                    :type="scope.row.status === '待接单' ? 'warning' :
                          scope.row.status === '已接单' ? 'success' : 'info'"
                  >
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作">
                <template #default="scope">
                  <el-button
                    v-if="scope.row.status === '待接单'"
                    type="primary"
                    size="small"
                  >
                    接单
                  </el-button>
                  <el-button type="info" size="small">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
        <div v-else-if="activeMenu === 'orders'">
          <router-view></router-view>
        </div>
        <div v-else-if="activeMenu === 'schedule'">
          <TeacherSchedule />
        </div>
        <div v-else-if="activeMenu === 'students'">
          <TeacherStudents />
        </div>
        <div v-else-if="activeMenu === 'materials'">
          <TeacherMaterials />
        </div>
        <div v-else-if="activeMenu === 'messages'">
          <TeacherMessages />
        </div>
        <div v-else-if="activeMenu === 'income'">
          <TeacherIncome />
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
  name: 'TeacherCenterView',
  data() {
    return {
      todayCourses: [
        {
          time: '14:00-16:00',
          course: '高中英语 - 阅读理解技巧',
          student: '赵同学'
        },
        {
          time: '16:30-18:30',
          course: '高中化学 - 有机化学',
          student: '钱同学'
        },
        {
          time: '10:00-12:00',
          course: '初中数学 - 几何证明',
          student: '孙同学'
        }
      ],
      newOrders: [
        {
          date: '2023-07-10',
          student: '王芳',
          course: '高中数学 - 概率统计',
          status: '待接单'
        },
        {
          date: '2023-07-09',
          student: '张明',
          course: '高中数学 - 立体几何',
          status: '已接单'
        }
      ]
    }
  }
}
</script>

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

.quick-actions, .upcoming-courses, .new-orders {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.quick-actions h3, .upcoming-courses h3, .new-orders h3 {
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 18px;
  color: #333;
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
  .el-container {
    flex-direction: column;
  }

  .el-aside {
    width: 100% !important;
    height: auto;
  }

  .el-main {
    padding: 10px;
  }

  .dashboard-stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .action-buttons .el-button {
    margin-bottom: 10px;
  }
}

@media (max-width: 480px) {
  .dashboard-stats {
    grid-template-columns: 1fr;
  }

  .stat-card {
    width: 100%;
  }
}
</style>
