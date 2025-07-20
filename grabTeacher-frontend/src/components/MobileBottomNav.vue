<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 定义组件名称
defineOptions({
  name: 'MobileBottomNav'
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 底部导航项
const navItems = computed(() => {
  const baseItems = [
    {
      path: '/',
      icon: 'House',
      label: '首页',
      active: route.path === '/'
    },
    {
      path: '/latest-courses',
      icon: 'Reading',
      label: '课程',
      active: route.path === '/latest-courses'
    },
    {
      path: '/famous-teachers',
      icon: 'User',
      label: '教师',
      active: route.path === '/famous-teachers'
    }
  ]

  // 根据登录状态添加不同的导航项
  if (userStore.isLoggedIn) {
    const userType = userStore.user?.userType
    if (userType === 'student') {
      baseItems.push({
        path: '/student-center',
        icon: 'UserFilled',
        label: '我的',
        active: route.path.startsWith('/student-center')
      })
    } else if (userType === 'teacher') {
      baseItems.push({
        path: '/teacher-center',
        icon: 'UserFilled',
        label: '我的',
        active: route.path.startsWith('/teacher-center')
      })
    } else if (userType === 'admin') {
      baseItems.push({
        path: '/admin-center',
        icon: 'Setting',
        label: '管理',
        active: route.path.startsWith('/admin-center')
      })
    }
  } else {
    baseItems.push({
      path: '/login',
      icon: 'UserFilled',
      label: '登录',
      active: route.path === '/login' || route.path === '/register'
    })
  }

  return baseItems
})

const navigateTo = (path: string) => {
  router.push(path)
}
</script>

<template>
  <div class="mobile-bottom-nav">
    <div class="nav-container">
      <div
        v-for="item in navItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: item.active }"
        @click="navigateTo(item.path)"
      >
        <div class="nav-icon">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
        </div>
        <div class="nav-label">{{ item.label }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mobile-bottom-nav {
  display: none;
}

@media (max-width: 768px) {
  .mobile-bottom-nav {
    display: block;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 1000;
    background-color: #fff;
    border-top: 1px solid #e6e6e6;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
    padding-bottom: env(safe-area-inset-bottom); /* 适配iPhone底部安全区域 */
  }

  .nav-container {
    display: flex;
    justify-content: space-around;
    align-items: center;
    padding: 8px 0;
    max-width: 100%;
  }

  .nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4px 8px;
    cursor: pointer;
    transition: all 0.2s ease;
    min-width: 60px;
    border-radius: 8px;
  }

  .nav-item:active {
    transform: scale(0.95);
  }

  .nav-icon {
    font-size: 20px;
    color: #999;
    margin-bottom: 2px;
    transition: color 0.2s ease;
  }

  .nav-label {
    font-size: 10px;
    color: #999;
    transition: color 0.2s ease;
    font-weight: 500;
  }

  .nav-item.active .nav-icon {
    color: #409eff;
  }

  .nav-item.active .nav-label {
    color: #409eff;
  }

  .nav-item:hover:not(.active) .nav-icon,
  .nav-item:hover:not(.active) .nav-label {
    color: #666;
  }

  /* 为底部导航留出空间 */
  body {
    padding-bottom: 60px;
  }
}

@media (max-width: 480px) {
  .nav-container {
    padding: 6px 0;
  }

  .nav-item {
    padding: 2px 4px;
    min-width: 50px;
  }

  .nav-icon {
    font-size: 18px;
  }

  .nav-label {
    font-size: 9px;
  }
}
</style>
