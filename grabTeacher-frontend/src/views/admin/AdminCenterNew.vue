<script setup lang="ts">
import { ref, defineAsyncComponent } from 'vue'
import { useUserStore } from '../../stores/user'
import { User, Lock, Setting, Document, DataBoard, UserFilled, Avatar, Reading, Coin, Calendar } from '@element-plus/icons-vue'

// 动态导入组件，避免默认导出类型报错
const AdminDashboard = defineAsyncComponent(() => import('./components/AdminDashboard.vue'))
const AdminStudentManagement = defineAsyncComponent(() => import('./components/AdminStudentManagement.vue'))
const AdminTeacherManagement = defineAsyncComponent(() => import('./components/AdminTeacherManagement.vue'))
const AdminSubjectManagement = defineAsyncComponent(() => import('./components/AdminSubjectManagement.vue'))
const AdminTeachingLocationManagement = defineAsyncComponent(() => import('./components/AdminTeachingLocationManagement.vue'))
const AdminGradeManagement = defineAsyncComponent(() => import('./components/AdminGradeManagement.vue'))


const AdminCourseManagement = defineAsyncComponent(() => import('./AdminCourseManagement.vue'))
const AdminJobPostManagement = defineAsyncComponent(() => import('./components/AdminJobPostManagement.vue'))
const AdminPasswordChange = defineAsyncComponent(() => import('./components/AdminPasswordChange.vue'))
const AdminProfileSettings = defineAsyncComponent(() => import('./components/AdminProfileSettings.vue'))
const AdminBookingManagement = defineAsyncComponent(() => import('./components/AdminBookingManagement.vue'))
const AdminStudyAbroadCountryManagement = defineAsyncComponent(() => import('./components/AdminStudyAbroadCountryManagement.vue'))
const AdminStudyAbroadStageManagement = defineAsyncComponent(() => import('./components/AdminStudyAbroadStageManagement.vue'))
const AdminStudyAbroadProgramManagement = defineAsyncComponent(() => import('./components/AdminStudyAbroadProgramManagement.vue'))
const AdminBalanceTransactionManagement = defineAsyncComponent(() => import('./components/AdminBalanceTransactionManagement.vue'))
const AdminMessageManagement = defineAsyncComponent(() => import('./components/AdminMessageManagement.vue'))
const AdminTeacherHourDetails = defineAsyncComponent(() => import('./components/AdminTeacherHourDetails.vue'))
const AdminCourseEvaluationManagement = defineAsyncComponent(() => import('./components/AdminCourseEvaluationManagement.vue'))
const AdminRescheduleManagement = defineAsyncComponent(() => import('./components/AdminRescheduleManagement.vue'))
const AdminSuspensionManagement = defineAsyncComponent(() => import('./components/AdminSuspensionManagement.vue'))
const AdminLevelPriceManagement = defineAsyncComponent(() => import('./components/AdminLevelPriceManagement.vue'))

// 获取用户信息
const userStore = useUserStore()
const adminName = ref('管理员')

// 当前活跃菜单
const activeMenu = ref('dashboard')

// 菜单切换
const handleMenuSelect = (key: string) => {
  activeMenu.value = key
}
</script>

<template>
  <div class="admin-center">
    <div class="admin-header">
      <h2>管理员控制台</h2>
      <div class="admin-info">
        <span>欢迎回来，{{ adminName }}</span>
      </div>
    </div>

    <div class="admin-container">
      <!-- 侧边菜单 -->
      <div class="admin-sidebar">
        <el-menu :default-active="activeMenu" class="admin-menu" @select="handleMenuSelect">
          <el-menu-item index="dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>数据概览</span>
          </el-menu-item>
          <el-menu-item index="students">
            <el-icon><UserFilled /></el-icon>
            <span>学生管理</span>
          </el-menu-item>
          <el-menu-item index="teachers">
            <el-icon><Avatar /></el-icon>
            <span>教师管理</span>
          </el-menu-item>
          <el-menu-item index="bookings">
            <el-icon><Document /></el-icon>
            <span>预约管理</span>
          </el-menu-item>
          <el-menu-item index="reschedule">
            <el-icon><Calendar /></el-icon>
            <span>调课管理</span>
          </el-menu-item>
          <el-menu-item index="suspension">
            <el-icon><Calendar /></el-icon>
            <span>停课管理</span>
          </el-menu-item>

          <el-sub-menu index="course-mgmt">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>课程管理</span>
            </template>
            <el-menu-item index="subjects">
              <el-icon><Document /></el-icon>
              <span>科目管理</span>
            </el-menu-item>
            <el-menu-item index="grades">
              <el-icon><Document /></el-icon>
              <span>年级管理</span>
            </el-menu-item>

            <el-menu-item index="locations">
              <el-icon><Document /></el-icon>
              <span>授课地点</span>
            </el-menu-item>
            <el-menu-item index="courses">
              <el-icon><Reading /></el-icon>
              <span>最新课程</span>
            </el-menu-item>
            <el-menu-item index="course-evaluations">
              <el-icon><Reading /></el-icon>
              <span>评价管理</span>
            </el-menu-item>
            <el-menu-item index="jobposts">
              <el-icon><Reading /></el-icon>
              <span>招聘管理</span>
            </el-menu-item>
            <el-menu-item index="level-prices">
              <el-icon><Coin /></el-icon>
              <span>教师级别</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="abroad-mgmt">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>留学管理</span>
            </template>
            <el-menu-item index="abroad-countries">
              <el-icon><Setting /></el-icon>
              <span>留学国家</span>
            </el-menu-item>
            <el-menu-item index="abroad-stages">
              <el-icon><Setting /></el-icon>
              <span>留学阶段</span>
            </el-menu-item>
            <el-menu-item index="abroad-programs">
              <el-icon><Setting /></el-icon>
              <span>留学项目</span>
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="transactions">
            <template #title>
              <el-icon><Coin /></el-icon>
              <span>交易明细</span>
            </template>
            <el-menu-item index="transactions-students">
              <el-icon><UserFilled /></el-icon>
              <span>学生明细</span>
            </el-menu-item>
            <el-menu-item index="transactions-teachers">
              <el-icon><Avatar /></el-icon>
              <span>教师明细</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="messages">
            <el-icon><Document /></el-icon>
            <span>消息管理</span>
          </el-menu-item>
          <el-menu-item index="profile">
            <el-icon><User /></el-icon>
            <span>管理员资料</span>
          </el-menu-item>
          <el-menu-item index="password">
            <el-icon><Lock /></el-icon>
            <span>账户设置</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 主要内容区域 -->

      <div class="admin-content">
        <div v-if="activeMenu === 'dashboard'" class="content-panel"><AdminDashboard /></div>
        <div v-if="activeMenu === 'students'" class="content-panel"><AdminStudentManagement /></div>
        <div v-if="activeMenu === 'teachers'" class="content-panel"><AdminTeacherManagement /></div>
        <div v-if="activeMenu === 'subjects'" class="content-panel"><AdminSubjectManagement /></div>
        <div v-if="activeMenu === 'grades'" class="content-panel"><AdminGradeManagement /></div>
        <div v-if="activeMenu === 'locations'" class="content-panel"><AdminTeachingLocationManagement /></div>


        <div v-if="activeMenu === 'courses'" class="content-panel"><AdminCourseManagement /></div>
        <div v-if="activeMenu === 'course-evaluations'" class="content-panel"><AdminCourseEvaluationManagement /></div>
        <div v-if="activeMenu === 'jobposts'" class="content-panel"><AdminJobPostManagement /></div>
        <div v-if="activeMenu === 'bookings'" class="content-panel"><AdminBookingManagement /></div>
        <div v-if="activeMenu === 'reschedule'" class="content-panel"><AdminRescheduleManagement /></div>
        <div v-if="activeMenu === 'suspension'" class="content-panel"><AdminSuspensionManagement /></div>
        <div v-if="activeMenu === 'abroad-countries'" class="content-panel"><AdminStudyAbroadCountryManagement /></div>
        <div v-if="activeMenu === 'abroad-stages'" class="content-panel"><AdminStudyAbroadStageManagement /></div>
        <div v-if="activeMenu === 'abroad-programs'" class="content-panel"><AdminStudyAbroadProgramManagement /></div>
        <div v-if="activeMenu === 'transactions-students'" class="content-panel"><AdminBalanceTransactionManagement /></div>
        <div v-if="activeMenu === 'transactions-teachers'" class="content-panel"><AdminTeacherHourDetails /></div>
        <div v-if="activeMenu === 'messages'" class="content-panel"><AdminMessageManagement /></div>
        <div v-if="activeMenu === 'level-prices'" class="content-panel"><AdminLevelPriceManagement /></div>
        <div v-if="activeMenu === 'profile'" class="content-panel"><AdminProfileSettings /></div>
        <div v-if="activeMenu === 'password'" class="content-panel"><AdminPasswordChange /></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-center {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.admin-header {
  background: white;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.admin-header h2 {
  margin: 0;
  color: #303133;
}

.admin-info {
  color: #606266;
}

.admin-container {
  display: flex;
  height: calc(100vh - 60px);
}

.admin-sidebar {
  width: 200px;
  background: white;
  box-shadow: 2px 0 4px rgba(0,0,0,0.1);
  overflow-y: auto;
  height: 100%;
}

.admin-menu {
  border-right: none;
  height: 100%;
  overflow-y: auto;
}

.admin-content {
  flex: 1;
  overflow-y: auto;
  background: #f5f5f5;
}

.content-panel {
  background: white;
  margin: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  min-height: calc(100vh - 120px);
  /* 确保内容不被遮挡 */
  position: relative;
  z-index: 1;
}

/* 响应式适配：小桌面与移动端 */
@media (max-width: 992px) {
  .admin-sidebar { width: 180px; }
}
@media (max-width: 768px) {
  .admin-container { flex-direction: column; height: auto; min-height: calc(100vh - 56px); }
  .admin-sidebar { width: 100%; height: auto; box-shadow: 0 2px 4px rgba(0,0,0,0.06); }
  .admin-menu { display: flex; overflow-x: auto; border-right: none; border-bottom: 1px solid #eee; }
  .admin-menu :deep(.el-menu-item),
  .admin-menu :deep(.el-sub-menu) { flex-shrink: 0; }
  .admin-menu :deep(.el-menu-item) { height: 48px; line-height: 48px; padding: 0 12px; }
  .admin-content { flex: 1; }
  .content-panel { margin: 12px; min-height: auto; }
}
</style>
