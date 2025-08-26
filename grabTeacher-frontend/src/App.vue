<script setup lang="ts">
import { useUserStore } from './stores/user'
import { useLangStore } from './stores/lang'
import { onMounted, watch } from 'vue'
import MobileNav from './components/MobileNav.vue'
import MobileBottomNav from './components/MobileBottomNav.vue'
import MobileSearch from './components/MobileSearch.vue'

const userStore = useUserStore()
const langStore = useLangStore()

// 初始化语言设置
onMounted(async () => {
  try {
    console.log('App组件开始挂载...')
    langStore.initLang()
    updateLangClass()
    await userStore.initializeAuth()
    console.log('App组件挂载完成')
  } catch (error) {
    console.error('App组件挂载时发生错误:', error)
  }
})

// 监听语言变化，添加或移除特定语言的类
watch(() => langStore.currentLang, () => {
  updateLangClass()
})

// 根据当前语言更新文档根元素的类名
function updateLangClass() {
  if (langStore.currentLang === 'zh') {
    document.documentElement.classList.add('zh-lang')
    document.documentElement.classList.remove('en-lang')
  } else {
    document.documentElement.classList.add('en-lang')
    document.documentElement.classList.remove('zh-lang')
  }
}
</script>

<template>
  <div class="app">
    <!-- 移动端导航 -->
    <MobileNav />

    <!-- 移动端搜索 -->
    <MobileSearch />

    <!-- 桌面端导航 -->
    <header class="header desktop-only">
      <div class="logo">Mindrift</div>
      <el-menu mode="horizontal" router class="nav-menu">
        <el-menu-item index="/">{{ $t('nav.home') }}</el-menu-item>
        <el-menu-item index="/latest-courses">{{ $t('nav.latestCourses') }}</el-menu-item>
        <el-menu-item index="/famous-teachers">{{ $t('nav.famousTeachers') }}</el-menu-item>
        <el-menu-item index="/student-reviews">{{ $t('nav.studentReviews') }}</el-menu-item>
        <el-menu-item index="/study-abroad">{{ $t('nav.studyAbroad') }}</el-menu-item>
        <el-menu-item index="/platform">{{ $t('nav.platform') }}</el-menu-item>
        <el-menu-item index="/about">{{ $t('nav.about') }}</el-menu-item>
      </el-menu>
      <div class="auth-buttons" v-if="!userStore.isLoggedIn">
        <el-button type="primary" size="small" @click="$router.push('/login')">{{ $t('auth.login') || '登录' }}</el-button>
        <el-button type="success" size="small" @click="$router.push('/register')">{{ $t('auth.register') || '注册' }}</el-button>
        <el-button type="warning" size="small" @click="langStore.toggleLang()">
          {{ langStore.currentLang === 'zh' ? 'English' : '中文' }}
        </el-button>
      </div>
      <div class="auth-buttons" v-else>
        <el-button v-if="userStore.isTeacher" @click="$router.push('/teacher-center')" type="success" size="small">{{ $t('auth.teacherCenter') }}</el-button>
        <el-button v-if="userStore.isStudent" @click="$router.push('/student-center')" type="success" size="small">{{ $t('auth.studentCenter') }}</el-button>
        <el-button v-if="userStore.isAdmin" @click="$router.push('/admin-center')" type="danger" size="small">{{ langStore.currentLang === 'zh' ? '管理中心' : 'Admin Center' }}</el-button>
        <el-button @click="userStore.logout" type="info" size="small">{{ $t('auth.logout') }}</el-button>
        <el-button type="warning" size="small" @click="langStore.toggleLang()">
          {{ langStore.currentLang === 'zh' ? 'English' : '中文' }}
        </el-button>
      </div>
    </header>
    <main class="main">
      <router-view></router-view>
    </main>

    <!-- 移动端底部导航 -->
    <MobileBottomNav />

    <footer class="footer">
      <div class="footer-content">
        <p>
          © 2025 Mindrift 个性化教学平台
          <a href="https://beian.miit.gov.cn" target="_blank" class="icp-link">
            {{ langStore.currentLang === 'zh' ? '津ICP备2025035841' : 'ICP License: 津ICP备2025035841' }}
          </a>
          <a href="https://beian.mps.gov.cn/#/query/webSearch?code=12011202001012" rel="noreferrer" target="_blank" class="beian-link">
            <img src="/beian.png" alt="公安备案" class="beian-icon">
            津公网安备12011202001012号
          </a>
        </p>
      </div>
    </footer>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  width: 100%;
  height: 100%;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  /* 移动端优化 */
  -webkit-text-size-adjust: 100%;
  -webkit-tap-highlight-color: transparent;
  touch-action: manipulation;
}

#app {
  width: 100%;
  min-height: 100vh;
}

.app {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

/* 确保桌面端导航在大屏幕上显示 */
.header.desktop-only {
  display: flex;
}

/* 移动端隐藏桌面导航 */
@media (max-width: 768px) {
  .header.desktop-only {
    display: none !important;
  }
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
  margin-right: 10px;
  flex-shrink: 0;
}

.nav-menu {
  border: none;
  flex: 1;
  display: flex;
  align-items: center;
}

.nav-menu .el-menu-item {
  padding: 0 10px;
  height: 60px;
  line-height: 60px;
  font-size: 14px;
}

.main {
  padding-top: 60px;
  flex: 1;
  width: 100%;
  min-height: calc(100vh - 60px);
}

/* 移动端为底部导航留出空间 */
@media (max-width: 768px) {
  .main {
    padding-top: 56px;
    padding-bottom: 60px; /* 为底部导航留出空间 */
    min-height: calc(100vh - 116px);
  }
}

@media (max-width: 480px) {
  .main {
    padding-top: 52px;
    padding-bottom: 55px;
    min-height: calc(100vh - 107px);
  }
}

.footer {
  background-color: #2c3e50;
  color: #fff;
  padding: 20px 0;
  text-align: center;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer-content p {
  margin: 0;
  line-height: 1.6;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .footer-content p {
    font-size: 12px;
    line-height: 1.8;
  }

  .icp-link,
  .beian-link {
    display: block;
    margin: 8px 0 0 0;
  }

  .beian-icon {
    width: 14px;
    height: 14px;
  }
}

.icp-link {
  color: #bdc3c7;
  text-decoration: none;
  margin-left: 10px;
  transition: color 0.3s ease;
}

.icp-link:hover {
  color: #ecf0f1;
  text-decoration: underline;
}

.beian-link {
  color: #bdc3c7;
  text-decoration: none;
  margin-left: 10px;
  transition: color 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.beian-link:hover {
  color: #ecf0f1;
  text-decoration: underline;
}

.beian-icon {
  width: 16px;
  height: 16px;
  vertical-align: middle;
}

.auth-buttons {
  display: flex;
  gap: 4px;
  margin-left: 10px;
  flex-shrink: 0;
  align-items: center;
  white-space: nowrap;
}

/* 中文模式下的特殊样式 */
.zh-lang .header {
  padding: 0;
}

.zh-lang .logo {
  font-size: 20px;
  margin-right: 5px;
}

.zh-lang .nav-menu .el-menu-item {
  padding: 10px 15px;
  font-size: 15px;
}

.zh-lang .auth-buttons {
  gap: 5px;
  margin-left: 10px;
}

.zh-lang .el-button.el-button--small {
  padding: 10px 10px;
  font-size: 10px;
}

/* 重置Element Plus的一些默认样式 */
.el-menu--horizontal {
  border-bottom: none !important;
}

.el-menu--horizontal>.el-menu-item {
  border-bottom: none !important;
}

.el-button.el-button--small {
  padding: 5px 7px;
  font-size: 10px;
}

.el-main {
  padding: 0 !important;
}

/* 全局移动端优化类 */
.mobile-container {
  padding: 0 16px;
}

.mobile-card {
  margin: 8px 0;
  border-radius: 8px;
  overflow: hidden;
}

.mobile-grid {
  display: grid;
  gap: 16px;
}

.mobile-grid-1 {
  grid-template-columns: 1fr;
}

.mobile-grid-2 {
  grid-template-columns: repeat(2, 1fr);
}

/* 隐藏/显示类 */
.mobile-only {
  display: none;
}

.desktop-only {
  display: block;
}

@media (max-width: 768px) {
  .mobile-only {
    display: block;
  }

  .desktop-only {
    display: none !important;
  }

  .mobile-container {
    padding: 0 12px;
  }

  .mobile-grid {
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .mobile-container {
    padding: 0 8px;
  }

  .mobile-grid {
    gap: 8px;
  }
}

/* 移动端优化 - 添加更多断点 */
@media (max-width: 1200px) {
  .header {
    padding: 0 15px;
  }

  .nav-menu .el-menu-item {
    padding: 0 12px;
    font-size: 14px;
  }
}

@media (max-width: 992px) {
  .header {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding: 0 10px;
  }

  .nav-menu {
    overflow-x: auto;
    scrollbar-width: none; /* Firefox */
    -ms-overflow-style: none; /* IE and Edge */
  }

  .nav-menu::-webkit-scrollbar {
    display: none; /* Chrome, Safari and Opera */
  }

  .nav-menu .el-menu-item {
    padding: 0 8px;
    font-size: 13px;
    white-space: nowrap;
  }
}

@media (max-width: 768px) {
  .header {
    padding: 0 8px;
    height: 56px;
  }

  .logo {
    font-size: 16px;
    margin-right: 8px;
    font-weight: 600;
  }

  .auth-buttons {
    margin-left: 8px;
    gap: 4px;
  }

  .el-button.el-button--small {
    padding: 6px 8px;
    font-size: 12px;
    border-radius: 4px;
  }

  .nav-menu .el-menu-item {
    padding: 0 6px;
    font-size: 12px;
    min-width: auto;
  }

  /* 主内容区域适配 */
  .el-main {
    padding-top: 56px !important;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 0 6px;
    height: 52px;
  }

  .logo {
    font-size: 14px;
    margin-right: 6px;
  }

  .auth-buttons {
    margin-left: 6px;
    gap: 2px;
  }

  .el-button.el-button--small {
    padding: 4px 6px;
    font-size: 11px;
    min-width: auto;
  }

  .nav-menu .el-menu-item {
    padding: 0 4px;
    font-size: 11px;
  }

  .el-main {
    padding-top: 52px !important;
  }
}

/* 移动端触摸优化 */
@media (hover: none) and (pointer: coarse) {
  .nav-menu .el-menu-item {
    min-height: 44px; /* 增加触摸目标大小 */
  }

  .el-button {
    min-height: 44px;
    min-width: 44px;
  }
}
</style>
