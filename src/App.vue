<script setup lang="ts">
import { useUserStore } from './stores/user'

const userStore = useUserStore()
</script>

<template>
  <div class="app">
    <header class="header">
      <div class="logo">GrabTeacher</div>
      <el-menu mode="horizontal" router class="nav-menu">
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/latest-courses">最新课程</el-menu-item>
        <el-menu-item index="/famous-teachers">天下名师</el-menu-item>
        <el-menu-item index="/premium-courses">金牌课程</el-menu-item>
        <el-menu-item index="/about">关于我们</el-menu-item>
        <el-menu-item index="/campus">校区查询</el-menu-item>
        <el-menu-item index="/platform">教师招聘</el-menu-item>
        <el-menu-item index="/contact">联系我们</el-menu-item>
        <template v-if="!userStore.isLoggedIn">
          <div class="auth-buttons">
            <el-button type="primary" size="small" @click="$router.push('/login')">学生登录</el-button>
            <el-button type="info" size="small" @click="$router.push('/teacher-login')">教师登录</el-button>
            <el-button type="success" size="small" @click="$router.push('/register')">学生注册</el-button>
            <el-button type="success" size="small" @click="$router.push('/teacher-register')">教师注册</el-button>
          </div>
        </template>
        <template v-else>
          <div class="auth-buttons">
            <el-button v-if="userStore.isTeacher" @click="$router.push('/teacher-center')" type="success" size="small">教师中心</el-button>
            <el-button v-if="userStore.isStudent" @click="$router.push('/student-center')" type="success" size="small">学生中心</el-button>
            <el-button @click="userStore.logout" type="info" size="small">退出登录</el-button>
          </div>
        </template>
      </el-menu>
    </header>
    <main class="main">
      <router-view></router-view>
    </main>
    <footer class="footer">
      <div class="footer-content">
        <p>© 2023 GrabTeacher 个性化教学平台 版权所有者：青浩洋 联系方式：19114846550微信同号</p>
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
  padding: 0 20px;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  margin-right: 20px;
}

.nav-menu {
  border: none;
  flex: 1;
  display: flex;
  align-items: center;
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
  gap: 8px;
  margin-left: 20px;
}

/* 重置Element Plus的一些默认样式 */
.el-menu--horizontal {
  border-bottom: none !important;
}

.el-menu--horizontal>.el-menu-item {
  border-bottom: none !important;
}

.el-main {
  padding: 0 !important;
}

@media (max-width: 992px) {
  .auth-buttons {
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .auth-buttons {
    margin-top: 10px;
  }
}
</style>
