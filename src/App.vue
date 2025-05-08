<script setup lang="ts">
import { useUserStore } from './stores/user'

const userStore = useUserStore()
</script>

<template>
  <div class="app">
    <header class="header">
      <el-menu mode="horizontal" router>
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/videos">视频课程</el-menu-item>
        <div class="flex-grow" />
        <template v-if="!userStore.isLoggedIn">
          <el-menu-item index="/login">登录</el-menu-item>
          <el-menu-item index="/register">注册</el-menu-item>
        </template>
        <template v-else>
          <el-menu-item v-if="userStore.isTeacher" index="/admin">后台管理</el-menu-item>
          <el-menu-item @click="userStore.logout">退出登录</el-menu-item>
        </template>
      </el-menu>
    </header>
    <main class="main">
      <router-view></router-view>
    </main>
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
}

#app {
  width: 100%;
  min-height: 100vh;
}

.app {
  width: 100%;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.header .el-menu {
  border: none;
  padding: 0 20px;
}

.main {
  padding-top: 60px;
  width: 100%;
}

.flex-grow {
  flex: 1;
}

/* 重置Element Plus的一些默认样式 */
.el-menu--horizontal {
  justify-content: center;
  border-bottom: none !important;
}

.el-menu--horizontal>.el-menu-item {
  border-bottom: none !important;
}

.el-main {
  padding: 0 !important;
}
</style>
