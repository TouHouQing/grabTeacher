<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = ref({
    username: '',
    password: '',
    role: 'student'
})

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const handleSubmit = async (formEl: FormInstance | undefined) => {
    if (!formEl) return

    await formEl.validate((valid) => {
        if (valid) {
            // 演示用，实际调用后端API
            userStore.login(form.value.username, form.value.role)

            if (form.value.role === 'teacher') {
                router.push('/admin')
            } else {
                router.push('/')
            }
        }
    })
}
</script>

<template>
    <div class="login-container">
        <el-card class="login-card">
            <template #header>
                <h2>登录</h2>
            </template>

            <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="form.username" />
                </el-form-item>

                <el-form-item label="密码" prop="password">
                    <el-input v-model="form.password" type="password" />
                </el-form-item>

                <el-form-item label="角色" prop="role">
                    <el-radio-group v-model="form.role">
                        <el-radio label="student">学生</el-radio>
                        <el-radio label="teacher">教师</el-radio>
                    </el-radio-group>
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="handleSubmit(formRef)">
                        登录
                    </el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<style scoped>
.login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 80vh;
}

.login-card {
    width: 100%;
    max-width: 400px;
}

.el-button {
    width: 100%;
}
</style>