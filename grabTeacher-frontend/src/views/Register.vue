<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

// 组件名称
defineOptions({
    name: 'RegisterView'
})

// 路由
const router = useRouter()

// 表单引用
const formRef = ref<FormInstance | null>(null)

// 表单数据
const form = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    phone: '',
    agreement: false
})

// 表单校验规则
const validatePass = (rule: any, value: string, callback: any) => {
    if (value === '') {
        callback(new Error('请再次输入密码'))
    } else if (value !== form.password) {
        callback(new Error('两次输入密码不一致!'))
    } else {
        callback()
    }
}

const rules = reactive<FormRules>({
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        { validator: validatePass, trigger: 'blur' }
    ],
    email: [
        { required: true, message: '请输入邮箱地址', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ],
    phone: [
        { required: true, message: '请输入手机号码', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ],
    agreement: [
        { type: 'boolean', required: true, message: '请阅读并同意用户协议', trigger: 'change' }
    ]
})

// 表单提交
const submitForm = async (formEl: FormInstance | null) => {
    if (!formEl) return
    await formEl.validate((valid, fields) => {
        if (valid) {
            console.log('注册成功', form)
            ElMessage.success('注册成功，即将跳转到登录页')
            setTimeout(() => {
                router.push('/login')
            }, 1500)
        } else {
            console.log('表单验证失败', fields)
        }
    })
}

// 重置表单
const resetForm = (formEl: FormInstance | null) => {
    if (!formEl) return
    formEl.resetFields()
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

                <el-form-item label="邮箱" prop="email">
                    <el-input v-model="form.email" />
                </el-form-item>

                <el-form-item label="手机号码" prop="phone">
                    <el-input v-model="form.phone" />
                </el-form-item>

                <el-form-item label="用户协议" prop="agreement">
                    <el-checkbox v-model="form.agreement" />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" @click="submitForm(formRef)">
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
