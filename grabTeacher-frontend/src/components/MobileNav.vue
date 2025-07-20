<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useLangStore } from '@/stores/lang'

// 定义组件名称
defineOptions({
  name: 'MobileNav'
})

const router = useRouter()
const userStore = useUserStore()
const langStore = useLangStore()

const isMenuOpen = ref(false)

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const closeMenu = () => {
  isMenuOpen.value = false
}

const navigateTo = (path: string) => {
  router.push(path)
  closeMenu()
}

const handleLogout = () => {
  userStore.logout()
  closeMenu()
  router.push('/')
}

// 导航菜单项
const navItems = computed(() => [
  { path: '/', label: langStore.t('nav.home') },
  { path: '/latest-courses', label: langStore.t('nav.latestCourses') },
  { path: '/famous-teachers', label: langStore.t('nav.famousTeachers') },
  { path: '/study-abroad', label: langStore.t('nav.studyAbroad') },
  { path: '/about', label: langStore.t('nav.about') },
  { path: '/campus', label: langStore.t('nav.campus') },
  { path: '/platform', label: langStore.t('nav.platform') },
  { path: '/contact', label: langStore.t('nav.contact') }
])
</script>

<template>
  <div class="mobile-nav">
    <!-- 移动端头部 -->
    <div class="mobile-header">
      <div class="logo">GrabTeacher</div>

      <div class="header-actions">
        <!-- 语言切换 -->
        <el-button
          size="small"
          text
          @click="langStore.toggleLang()"
          class="lang-btn"
        >
          {{ langStore.currentLang === 'zh' ? 'EN' : '中' }}
        </el-button>

        <!-- 汉堡菜单按钮 -->
        <el-button
          size="small"
          text
          @click="toggleMenu"
          class="menu-btn"
        >
          <el-icon :size="20">
            <Menu v-if="!isMenuOpen" />
            <Close v-else />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- 移动端菜单遮罩 -->
    <div
      v-if="isMenuOpen"
      class="menu-overlay"
      @click="closeMenu"
    ></div>

    <!-- 移动端侧边菜单 -->
    <div class="mobile-menu" :class="{ 'menu-open': isMenuOpen }">
      <div class="menu-content">
        <!-- 用户信息 -->
        <div v-if="userStore.isLoggedIn" class="user-section">
          <div class="user-info">
            <div class="user-avatar">
              <el-avatar :size="40">
                <el-icon><User /></el-icon>
              </el-avatar>
            </div>
            <div class="user-details">
              <div class="username">{{ userStore.user?.username }}</div>
              <div class="user-type">
                {{ userStore.user?.userType === 'student' ? '学生' :
                   userStore.user?.userType === 'teacher' ? '教师' : '管理员' }}
              </div>
            </div>
          </div>

          <!-- 用户中心快捷入口 -->
          <div class="user-actions">
            <el-button
              v-if="userStore.user?.userType === 'student'"
              size="small"
              type="primary"
              @click="navigateTo('/student-center')"
            >
              学生中心
            </el-button>
            <el-button
              v-else-if="userStore.user?.userType === 'teacher'"
              size="small"
              type="primary"
              @click="navigateTo('/teacher-center')"
            >
              教师中心
            </el-button>
            <el-button
              v-else-if="userStore.user?.userType === 'admin'"
              size="small"
              type="primary"
              @click="navigateTo('/admin-center')"
            >
              管理中心
            </el-button>
          </div>
        </div>

        <!-- 导航菜单 -->
        <div class="nav-section">
          <div class="nav-title">导航菜单</div>
          <div class="nav-items">
            <div
              v-for="item in navItems"
              :key="item.path"
              class="nav-item"
              @click="navigateTo(item.path)"
            >
              {{ item.label }}
            </div>
          </div>
        </div>

        <!-- 登录/注册或登出 -->
        <div class="auth-section">
          <template v-if="!userStore.isLoggedIn">
            <el-button
              type="primary"
              size="large"
              @click="navigateTo('/login')"
              class="auth-btn"
            >
              学生登录
            </el-button>
            <el-button
              size="large"
              @click="navigateTo('/teacher-login')"
              class="auth-btn"
            >
              教师登录
            </el-button>
            <el-button
              size="large"
              @click="navigateTo('/register')"
              class="auth-btn"
            >
              注册账号
            </el-button>
          </template>
          <template v-else>
            <el-button
              size="large"
              @click="handleLogout"
              class="auth-btn logout-btn"
            >
              退出登录
            </el-button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mobile-nav {
  display: none;
}

@media (max-width: 768px) {
  .mobile-nav {
    display: block;
  }

  .mobile-header {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 1000;
    background-color: #fff;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px;
    height: 56px;
  }

  .logo {
    font-size: 18px;
    font-weight: bold;
    color: #409eff;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .lang-btn {
    padding: 8px 12px;
    font-size: 14px;
    font-weight: 500;
  }

  .menu-btn {
    padding: 8px;
  }

  .menu-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    z-index: 1001;
  }

  .mobile-menu {
    position: fixed;
    top: 0;
    right: -300px;
    width: 300px;
    height: 100vh;
    background-color: #fff;
    z-index: 1002;
    transition: right 0.3s ease;
    overflow-y: auto;
  }

  .mobile-menu.menu-open {
    right: 0;
  }

  .menu-content {
    padding: 20px;
    padding-top: 76px; /* 为头部留出空间 */
  }

  .user-section {
    margin-bottom: 30px;
    padding-bottom: 20px;
    border-bottom: 1px solid #eee;
  }

  .user-info {
    display: flex;
    align-items: center;
    margin-bottom: 15px;
  }

  .user-avatar {
    margin-right: 12px;
  }

  .username {
    font-size: 16px;
    font-weight: 500;
    color: #333;
  }

  .user-type {
    font-size: 12px;
    color: #666;
    margin-top: 2px;
  }

  .user-actions {
    display: flex;
    justify-content: center;
  }

  .nav-section {
    margin-bottom: 30px;
  }

  .nav-title {
    font-size: 14px;
    font-weight: 500;
    color: #666;
    margin-bottom: 15px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .nav-items {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .nav-item {
    padding: 12px 16px;
    font-size: 16px;
    color: #333;
    cursor: pointer;
    border-radius: 6px;
    transition: background-color 0.2s;
  }

  .nav-item:hover {
    background-color: #f5f7fa;
  }

  .auth-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .auth-btn {
    width: 100%;
    height: 44px;
    font-size: 16px;
  }

  .logout-btn {
    background-color: #f56c6c;
    border-color: #f56c6c;
    color: white;
  }

  .logout-btn:hover {
    background-color: #f78989;
    border-color: #f78989;
  }
}

@media (max-width: 480px) {
  .mobile-menu {
    width: 280px;
    right: -280px;
  }

  .menu-content {
    padding: 16px;
    padding-top: 72px;
  }
}
</style>
