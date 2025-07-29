<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '../../stores/user'
import { User, Lock, Setting, Document, DataBoard, UserFilled, Avatar, Reading } from '@element-plus/icons-vue'

// 导入拆分后的组件
import AdminDashboard from './components/AdminDashboard.vue'
import AdminStudentManagement from './components/AdminStudentManagement.vue'
import AdminTeacherManagement from './components/AdminTeacherManagement.vue'
import AdminSubjectManagement from './components/AdminSubjectManagement.vue'
import AdminCourseManagement from './AdminCourseManagement.vue'
import AdminGradeManagement from '../../components/AdminGradeManagement.vue'
import AdminPasswordChange from './components/AdminPasswordChange.vue'
import AdminBookingManagement from './components/AdminBookingManagement.vue'

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
        <el-menu
          :default-active="activeMenu"
          class="admin-menu"
          @select="handleMenuSelect"
        >
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
          <el-menu-item index="subjects">
            <el-icon><Document /></el-icon>
            <span>科目管理</span>
          </el-menu-item>
          <el-menu-item index="courses">
            <el-icon><Reading /></el-icon>
            <span>课程管理</span>
          </el-menu-item>
          <el-menu-item index="bookings">
            <el-icon><Document /></el-icon>
            <span>预约管理</span>
          </el-menu-item>
          <el-menu-item index="grades">
            <el-icon><Setting /></el-icon>
            <span>年级管理</span>
          </el-menu-item>
          <el-menu-item index="password">
            <el-icon><Lock /></el-icon>
            <span>账户设置</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 主要内容区域 -->
      <div class="admin-content">
        <!-- 数据概览 -->
        <div v-show="activeMenu === 'dashboard'" class="content-panel">
          <AdminDashboard />
        </div>

        <!-- 学生管理 -->
        <div v-show="activeMenu === 'students'" class="content-panel">
          <AdminStudentManagement />
        </div>

        <!-- 教师管理 -->
        <div v-show="activeMenu === 'teachers'" class="content-panel">
          <AdminTeacherManagement />
        </div>

        <!-- 科目管理 -->
        <div v-show="activeMenu === 'subjects'" class="content-panel">
          <AdminSubjectManagement />
        </div>

        <!-- 课程管理 -->
        <div v-show="activeMenu === 'courses'" class="content-panel">
          <AdminCourseManagement />
        </div>

        <!-- 预约管理 -->
        <div v-show="activeMenu === 'bookings'" class="content-panel">
          <AdminBookingManagement />
        </div>

        <!-- 年级管理 -->
        <div v-show="activeMenu === 'grades'" class="content-panel">
          <AdminGradeManagement />
        </div>

        <!-- 账户设置 -->
        <div v-show="activeMenu === 'password'" class="content-panel">
          <AdminPasswordChange />
        </div>
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
}

.admin-menu {
  border-right: none;
  height: 100%;
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
</style>
