<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const form = ref({
    username: '',
    password: '',
    confirmPassword: '',
    role: 'student'
})

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        {
            validator: (rule: any, value: string, callback: Function) => {
                if (value !== form.value.password) {
                    callback(new Error('两次输入的密码不一致'))
                } else {
                    callback()
                }
            },
            trigger: 'blur'
        }
    ],
    role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const handleSubmit = async (formEl: FormInstance | undefined) => {
    if (!formEl) return

    await formEl.validate((valid) => {
        if (valid) {
            // 演示用，实际调用后端API
            ElMessage.success('注册成功')
            router.push('/login')
        }
    })
}
</script>

<template>
    <div class="register-container">
        <el-card class="register-card">
            <template #header>
                <h2>注册</h2>
            </template>

            <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="form.username" />
                </el-form-item>

                <el-form-item label="密码" prop="password">
                    <el-input v-model="form.password" type="password" />
                </el-form-item>

                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="form.confirmPassword" type="password" />
                </el-form-item>

                <el-form-item label="角色" prop="role">
                    <el-radio-group v-model="form.role">
                        <el-radio label="student">学生</el-radio>
                        <el-radio label="teacher">教师</el-radio>
                    </el-radio-group>
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="handleSubmit(formRef)">
                        注册
                    </el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<style scoped>
.register-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 80vh;
}

.register-card {
    width: 100%;
    max-width: 400px;
}

.el-button {
    width: 100%;
}
</style>