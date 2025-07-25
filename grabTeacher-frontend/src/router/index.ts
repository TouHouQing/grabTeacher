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
    // 向后兼容的重定向路由
    {
      path: '/teacher-login',
      redirect: '/login'
    },
    {
      path: '/admin',
      redirect: '/login'
    },
    {
      path: '/admin-login',
      redirect: '/login'
    },
    {
      path: '/teacher-register',
      name: 'TeacherRegister',
      component: () => import('../views/TeacherRegister.vue')
    },
    // 学生中心及其子路由
    {
      path: '/student-center',
      name: 'StudentCenter',
      component: () => import('../views/student/StudentCenter.vue'),
      meta: { requiresAuth: true, role: 'student' },
      children: [
        {
          path: 'profile',
          name: 'StudentProfile',
          component: () => import('../views/student/StudentProfile.vue'),
          meta: { requiresAuth: true, role: 'student' }
        },

        {
          path: 'match',
          name: 'TeacherMatch',
          component: () => import('../views/student/TeacherMatch.vue'),
          meta: { requiresAuth: true, role: 'student' }
        },
        {
          path: 'bookings',
          name: 'StudentBookings',
          component: () => import('../views/student/BookingManagement.vue'),
          meta: { requiresAuth: true, role: 'student' }
        }
      ]
    },
    // 教师中心及其子路由
    {
      path: '/teacher-center',
      name: 'TeacherCenter',
      component: () => import('../views/teacher/TeacherCenter.vue'),
      meta: { requiresAuth: true, role: 'teacher' },
      children: [
        {
          path: 'profile',
          name: 'TeacherProfile',
          component: () => import('../views/teacher/TeacherProfile.vue'),
          meta: { requiresAuth: true, role: 'teacher' }
        },

        {
          path: 'schedule',
          name: 'TeacherSchedule',
          component: () => import('../views/teacher/Schedule.vue'),
          meta: { requiresAuth: true, role: 'teacher' }
        },
        {
          path: 'bookings',
          name: 'TeacherBookings',
          component: () => import('../views/teacher/BookingManagement.vue'),
          meta: { requiresAuth: true, role: 'teacher' }
        },
        {
          path: 'courses',
          name: 'TeacherCourses',
          component: () => import('../views/teacher/components/TeacherCourses.vue'),
          meta: { requiresAuth: true, role: 'teacher' }
        },
        {
          path: 'reschedule',
          name: 'TeacherReschedule',
          component: () => import('../views/teacher/RescheduleManagement.vue'),
          meta: { requiresAuth: true, role: 'teacher' }
        }
      ]
    },
    // 管理员中心
    {
      path: '/admin-center',
      name: 'AdminCenter',
      component: () => import('../views/admin/AdminCenterNew.vue'),
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
    {
      path: '/teacher-detail/:id',
      name: 'TeacherDetail',
      component: () => import('../views/student/TeacherDetail.vue')
    },
    // 兼容旧路由的重定向
    {
      path: '/student',
      redirect: '/student-center'
    },
    {
      path: '/student/profile',
      redirect: '/student-center/profile'
    },

    {
      path: '/teacher',
      redirect: '/teacher-center'
    },
    {
      path: '/teacher/profile',
      redirect: '/teacher-center/profile'
    },

    {
      path: '/teacher/schedule',
      redirect: '/teacher-center/schedule'
    },
    {
      path: '/admin/user-management',
      redirect: '/admin-center/user-management'
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
