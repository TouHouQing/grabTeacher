<script setup lang="ts">
import { useUserStore } from './stores/user'
import { useLangStore } from './stores/lang'
import { onMounted } from 'vue'

const userStore = useUserStore()
const langStore = useLangStore()

// 初始化语言设置
onMounted(() => {
  langStore.initLang()
})
</script>

<template>
  <div class="app">
    <header class="header">
      <div class="logo">GrabTeacher</div>
      <el-menu mode="horizontal" router class="nav-menu">
        <el-menu-item index="/">{{ $t('nav.home') }}</el-menu-item>
        <el-menu-item index="/latest-courses">{{ $t('nav.latestCourses') }}</el-menu-item>
        <el-menu-item index="/famous-teachers">{{ $t('nav.famousTeachers') }}</el-menu-item>
        <el-menu-item index="/premium-courses">{{ $t('nav.premiumCourses') }}</el-menu-item>
        <el-menu-item index="/about">{{ $t('nav.about') }}</el-menu-item>
        <el-menu-item index="/campus">{{ $t('nav.campus') }}</el-menu-item>
        <el-menu-item index="/platform">{{ $t('nav.platform') }}</el-menu-item>
        <el-menu-item index="/contact">{{ $t('nav.contact') }}</el-menu-item>
      </el-menu>
      <div class="auth-buttons" v-if="!userStore.isLoggedIn">
        <el-button type="primary" size="small" @click="$router.push('/login')">{{ $t('auth.studentLogin') }}</el-button>
        <el-button type="info" size="small" @click="$router.push('/teacher-login')">{{ $t('auth.teacherLogin') }}</el-button>
        <el-button type="success" size="small" @click="$router.push('/register')">{{ $t('auth.studentRegister') }}</el-button>
        <el-button type="success" size="small" @click="$router.push('/teacher-register')">{{ $t('auth.teacherRegister') }}</el-button>
        <el-button type="warning" size="small" @click="langStore.toggleLang()">
          {{ langStore.currentLang === 'zh' ? 'English' : '中文' }}
        </el-button>
      </div>
      <div class="auth-buttons" v-else>
        <el-button v-if="userStore.isTeacher" @click="$router.push('/teacher-center')" type="success" size="small">{{ $t('auth.teacherCenter') }}</el-button>
        <el-button v-if="userStore.isStudent" @click="$router.push('/student-center')" type="success" size="small">{{ $t('auth.studentCenter') }}</el-button>
        <el-button @click="userStore.logout" type="info" size="small">{{ $t('auth.logout') }}</el-button>
        <el-button type="warning" size="small" @click="langStore.toggleLang()">
          {{ langStore.currentLang === 'zh' ? 'English' : '中文' }}
        </el-button>
      </div>
    </header>
    <main class="main">
      <router-view></router-view>
    </main>
    <footer class="footer">
      <div class="footer-content">
        <p>{{ $t('footer.copyright') }}</p>
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
  padding: 0 1px;
  height: 60px;
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
}

.auth-buttons {
  display: flex;
  gap: 4px;
  margin-left: 10px;
  flex-shrink: 0;
  align-items: center;
  white-space: nowrap;
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

@media (max-width: 992px) {
  .header {
    flex-wrap: nowrap;
    overflow-x: auto;
  }

  .nav-menu {
    overflow-x: auto;
  }

  .nav-menu .el-menu-item {
    padding: 0 8px;
    font-size: 13px;
  }
}

@media (max-width: 768px) {
  .header {
    padding: 0 5px;
  }

  .logo {
    font-size: 18px;
    margin-right: 5px;
  }

  .auth-buttons {
    margin-left: 5px;
    gap: 2px;
  }

  .el-button.el-button--small {
    padding: 4px 6px;
    font-size: 11px;
  }

  .nav-menu .el-menu-item {
    padding: 0 6px;
    font-size: 12px;
  }
}
</style>
