import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
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
        path: '/videos',
        name: 'Videos',
        component: () => import('../views/Videos.vue')
    },
    {
        path: '/admin',
        name: 'Admin',
        component: () => import('../views/admin/Layout.vue'),
        meta: { requiresAuth: true, role: 'teacher' },
        children: [
            {
                path: 'users',
                name: 'UserManagement',
                component: () => import('../views/admin/UserManagement.vue')
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    const isTeacher = localStorage.getItem('userRole') === 'teacher'

    if (to.meta.requiresAuth && !isTeacher) {
        next('/login')
    } else {
        next()
    }
})

export default router 