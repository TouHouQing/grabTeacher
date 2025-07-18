import { defineStore } from 'pinia'

interface UserState {
    isLoggedIn: boolean
    role: 'teacher' | 'student' | 'admin' | null
    username: string | null
}

// 预设账户
const PRESET_ACCOUNTS = {
    teacher: { username: 'teacher', password: '123456' },
    student: { username: 'student', password: '123456' },
    admin: { username: 'admin', password: '123456' }
}

export const useUserStore = defineStore('user', {
    state: (): UserState => ({
        isLoggedIn: false,
        role: null,
        username: null
    }),

    actions: {
        login(username: string, role: 'teacher' | 'student' | 'admin') {
            this.isLoggedIn = true
            this.username = username
            this.role = role
            localStorage.setItem('userRole', role)
            localStorage.setItem('username', username)
        },

        logout() {
            this.isLoggedIn = false
            this.username = null
            this.role = null
            localStorage.removeItem('userRole')
            localStorage.removeItem('username')
        },

        validateLogin(username: string, password: string, role: 'teacher' | 'student' | 'admin'): boolean {
            const account = PRESET_ACCOUNTS[role]
            return account.username === username && account.password === password
        }
    },

    getters: {
        isTeacher: (state) => state.role === 'teacher',
        isStudent: (state) => state.role === 'student',
        isAdmin: (state) => state.role === 'admin'
    }
})
