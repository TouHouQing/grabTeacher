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
        <div class="flex-grow" />
        <template v-if="!userStore.isLoggedIn">
          <el-button type="primary" @click="$router.push('/login')" class="login-btn">学生登录</el-button>
          <el-button type="info" @click="$router.push('/teacher-login')" class="login-btn">教师登录</el-button>
        </template>
        <template v-else>
          <el-button v-if="userStore.isTeacher" @click="$router.push('/teacher-center')" type="success">教师中心</el-button>
          <el-button v-if="userStore.isStudent" @click="$router.push('/student-center')" type="success">学生中心</el-button>
          <el-button @click="userStore.logout" type="info">退出登录</el-button>
        </template>
      </el-menu>
    </header>
    <main class="main">
      <router-view></router-view>
    </main>
    <footer class="footer">
      <div class="footer-content">
        <p>© 2023 GrabTeacher 个性化教学平台 版权所有</p>
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

.flex-grow {
  flex: 1;
}

.login-btn {
  margin-left: 10px;
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
</style>
