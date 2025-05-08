import { defineStore } from 'pinia'

interface UserState {
    isLoggedIn: boolean
    role: 'teacher' | 'student' | null
    username: string | null
}

export const useUserStore = defineStore('user', {
    state: (): UserState => ({
        isLoggedIn: false,
        role: null,
        username: null
    }),

    actions: {
        login(username: string, role: 'teacher' | 'student') {
            this.isLoggedIn = true
            this.username = username
            this.role = role
            localStorage.setItem('userRole', role)
        },

        logout() {
            this.isLoggedIn = false
            this.username = null
            this.role = null
            localStorage.removeItem('userRole')
        }
    },

    getters: {
        isTeacher: (state) => state.role === 'teacher',
        isStudent: (state) => state.role === 'student'
    }
}) 