import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('../views/Home.vue')
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/Register.vue')
    },
    {
      path: '/teacher-login',
      name: 'TeacherLogin',
      component: () => import('../views/TeacherLogin.vue')
    },
    {
      path: '/teacher-register',
      name: 'TeacherRegister',
      component: () => import('../views/TeacherRegister.vue')
    },
    // 学生中心
    {
      path: '/student-center',
      name: 'StudentCenter',
      component: () => import('../views/student/StudentCenter.vue'),
      meta: { requiresAuth: true, role: 'student' }
    },
    // 教师中心
    {
      path: '/teacher-center',
      name: 'TeacherCenter',
      component: () => import('../views/teacher/TeacherCenter.vue'),
      meta: { requiresAuth: true, role: 'teacher' }
    },
    // 管理员中心
    {
      path: '/admin-center',
      name: 'AdminCenter',
      component: () => import('../views/admin/AdminCenter.vue'),
      meta: { requiresAuth: true, role: 'admin' }
    },
    // 其他现有路由...
    {
      path: '/about',
      name: 'About',
      component: () => import('../views/About.vue')
    },
    {
      path: '/campus',
      name: 'Campus',
      component: () => import('../views/Campus.vue')
    },
    {
      path: '/contact',
      name: 'Contact',
      component: () => import('../views/Contact.vue')
    },
    {
      path: '/famous-teachers',
      name: 'FamousTeachers',
      component: () => import('../views/FamousTeachers.vue')
    },
    {
      path: '/latest-courses',
      name: 'LatestCourses',
      component: () => import('../views/LatestCourses.vue')
    },
    {
      path: '/platform',
      name: 'Platform',
      component: () => import('../views/Platform.vue')
    },
    {
      path: '/study-abroad',
      name: 'StudyAbroad',
      component: () => import('../views/StudyAbroad.vue')
    },
    {
      path: '/videos',
      name: 'Videos',
      component: () => import('../views/Videos.vue')
    },
    // 学生相关页面
    {
      path: '/student',
      redirect: '/student-center'
    },
    {
      path: '/student/profile',
      name: 'StudentProfile',
      component: () => import('../views/student/StudentProfile.vue'),
      meta: { requiresAuth: true, role: 'student' }
    },
    {
      path: '/student/orders',
      name: 'StudentOrders',
      component: () => import('../views/student/StudentOrders.vue'),
      meta: { requiresAuth: true, role: 'student' }
    },
    {
      path: '/teacher-detail/:id',
      name: 'TeacherDetail',
      component: () => import('../views/student/TeacherDetail.vue')
    },
    {
      path: '/teacher-match',
      name: 'TeacherMatch',
      component: () => import('../views/student/TeacherMatch.vue')
    },
    // 教师相关页面
    {
      path: '/teacher',
      redirect: '/teacher-center'
    },
    {
      path: '/teacher/profile',
      name: 'TeacherProfile',
      component: () => import('../views/teacher/TeacherProfile.vue'),
      meta: { requiresAuth: true, role: 'teacher' }
    },
    {
      path: '/teacher/orders',
      name: 'TeacherOrders',
      component: () => import('../views/teacher/TeacherOrders.vue'),
      meta: { requiresAuth: true, role: 'teacher' }
    },
    {
      path: '/teacher/schedule',
      name: 'TeacherSchedule',
      component: () => import('../views/teacher/TeacherSchedule.vue'),
      meta: { requiresAuth: true, role: 'teacher' }
    },
    // 管理员相关页面
    {
      path: '/admin',
      redirect: '/admin-center'
    },
    {
      path: '/admin/user-management',
      name: 'UserManagement',
      component: () => import('../views/admin/UserManagement.vue'),
      meta: { requiresAuth: true, role: 'admin' }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      // 未登录，跳转到登录页
      next('/login')
      return
    }

    // 检查角色权限
    if (to.meta.role && userStore.user?.userType !== to.meta.role) {
      // 权限不足，跳转到对应的中心页面
      if (userStore.user?.userType === 'student') {
        next('/student-center')
      } else if (userStore.user?.userType === 'teacher') {
        next('/teacher-center')
      } else if (userStore.user?.userType === 'admin') {
        next('/admin-center')
      } else {
        next('/')
      }
      return
    }
  }

  next()
})

export default router
