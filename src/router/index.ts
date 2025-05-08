import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import Home from '../views/Home.vue'
import FamousTeachers from '../views/FamousTeachers.vue'
import PremiumCourses from '../views/PremiumCourses.vue'
import LatestCourses from '../views/LatestCourses.vue'
import About from '../views/About.vue'
import Campus from '../views/Campus.vue'
import Login from '../views/Login.vue'
import TeacherLogin from '../views/TeacherLogin.vue'
import StudentCenter from '../views/student/StudentCenter.vue'
import TeacherMatch from '../views/student/TeacherMatch.vue'
import TeacherCenter from '../views/teacher/TeacherCenter.vue'
import StudentOrders from '../views/student/StudentOrders.vue'
import StudentProfile from '../views/student/StudentProfile.vue'
import TeacherOrders from '../views/teacher/TeacherOrders.vue'
import TeacherSchedule from '../views/teacher/TeacherSchedule.vue'
import TeacherProfile from '../views/teacher/TeacherProfile.vue'
import Platform from '../views/Platform.vue'
import Contact from '../views/Contact.vue'
import AdminCenter from '../views/admin/AdminCenter.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home
    },
    {
      path: '/famous-teachers',
      name: 'famous-teachers',
      component: FamousTeachers
    },
    {
      path: '/premium-courses',
      name: 'premium-courses',
      component: PremiumCourses
    },
    {
      path: '/latest-courses',
      name: 'latest-courses',
      component: LatestCourses
    },
    {
      path: '/about',
      name: 'about',
      component: About
    },
    {
      path: '/campus',
      name: 'campus',
      component: Campus
    },
    {
      path: '/platform',
      name: 'platform',
      component: Platform
    },
    {
      path: '/contact',
      name: 'contact',
      component: Contact
    },
    {
      path: '/login',
      name: 'login',
      component: Login
    },
    {
      path: '/teacher-login',
      name: 'teacher-login',
      component: TeacherLogin
    },
    {
      path: '/student-center',
      name: 'student-center',
      component: StudentCenter,
      meta: { requiresAuth: true, role: 'student' },
      children: [
        {
          path: 'match',
          name: 'student-match',
          component: TeacherMatch,
          meta: { requiresAuth: true, role: 'student' }
        },
        {
          path: 'orders',
          name: 'student-orders',
          component: StudentOrders,
          meta: { requiresAuth: true, role: 'student' }
        },
        {
          path: 'profile',
          name: 'student-profile',
          component: StudentProfile,
          meta: { requiresAuth: true, role: 'student' }
        }
      ]
    },
    {
      path: '/teacher-center',
      name: 'teacher-center',
      component: TeacherCenter,
      meta: { requiresAuth: true, role: 'teacher' },
      children: [
        {
          path: 'orders',
          name: 'teacher-orders',
          component: TeacherOrders,
          meta: { requiresAuth: true, role: 'teacher' }
        },
        {
          path: 'schedule',
          name: 'teacher-schedule',
          component: TeacherSchedule,
          meta: { requiresAuth: true, role: 'teacher' }
        },
        {
          path: 'profile',
          name: 'teacher-profile',
          component: TeacherProfile,
          meta: { requiresAuth: true, role: 'teacher' }
        }
      ]
    },
    {
      path: '/admin',
      name: 'admin-center',
      component: AdminCenter,
      meta: { requiresAuth: true, role: 'admin' }
    }
  ]
})

// 全局前置守卫
router.beforeEach((to) => {
  const userStore = useUserStore()

  // 需要认证但未登录
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  // 需要特定角色
  if (to.meta.role && to.meta.role !== userStore.role) {
    return { path: '/' } // 重定向到首页
  }
})

export default router
