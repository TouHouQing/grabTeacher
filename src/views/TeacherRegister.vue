<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

// 组件名称
defineOptions({
    name: 'TeacherRegisterView'
})

// 路由
const router = useRouter()

// 表单引用
const formRef = ref<FormInstance | null>(null)

// 可教授科目选项
const subjectOptions = [
    { label: '数学 Mathematics', value: 'Mathematics' },
    { label: '科学 Science', value: 'Science' },
    { label: '英语 English', value: 'English' },
    { label: '物理 Physics', value: 'Physics' },
    { label: '化学 Chemistry', value: 'Chemistry' },
    { label: '生物 Biology', value: 'Biology' },
    { label: '语文 Chinese', value: 'Chinese' },
    { label: '历史 History', value: 'History' },
    { label: '地理 Geography', value: 'Geography' }
]

// 可教授年级选项
const gradeOptions = [
    { label: '小学 Primary', options: [
        { label: 'Primary 1', value: 'Primary 1' },
        { label: 'Primary 2', value: 'Primary 2' },
        { label: 'Primary 3', value: 'Primary 3' },
        { label: 'Primary 4', value: 'Primary 4' },
        { label: 'Primary 5', value: 'Primary 5' },
        { label: 'Primary 6', value: 'Primary 6' }
    ]},
    { label: '中学 Secondary', options: [
        { label: 'Secondary 1', value: 'Secondary 1' },
        { label: 'Secondary 2', value: 'Secondary 2' },
        { label: 'Secondary 3', value: 'Secondary 3' },
        { label: 'Secondary 4', value: 'Secondary 4' }
    ]},
    { label: '考试 Exam', options: [
        { label: 'O-level', value: 'O-level' },
        { label: 'A-level', value: 'A-level' }
    ]}
]

// 教学风格选项
const teachingStyleOptions = [
    { label: '幽默风趣型', value: 'Humorous and Witty Style' },
    { label: '严谨教学型', value: 'Rigorous Teaching Style' },
    { label: '严慈相济型', value: 'Both Strict and Benevolent Style' }
]

// 表单数据
const form = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    realName: '',
    gender: '',
    email: '',
    phone: '',
    subjects: [],
    grades: [],
    teachingStyle: '',
    experience: 0,
    education: '',
    certificate: '',
    introduction: '',
    availableTimes: [],
    agreement: false
})

// 表单校验规则
const validatePass = (rule: unknown, value: string, callback: (error?: Error) => void) => {
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
    realName: [
        { required: true, message: '请输入真实姓名', trigger: 'blur' }
    ],
    gender: [
        { required: true, message: '请选择性别', trigger: 'change' }
    ],
    email: [
        { required: true, message: '请输入邮箱地址', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ],
    phone: [
        { required: true, message: '请输入手机号码', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ],
    subjects: [
        { required: true, message: '请选择至少一个教授科目', trigger: 'change', type: 'array', min: 1 }
    ],
    grades: [
        { required: true, message: '请选择至少一个教授年级', trigger: 'change', type: 'array', min: 1 }
    ],
    teachingStyle: [
        { required: true, message: '请选择您的教学风格', trigger: 'change' }
    ],
    experience: [
        { required: true, message: '请输入教学经验年限', trigger: 'blur' }
    ],
    education: [
        { required: true, message: '请输入您的学历信息', trigger: 'blur' }
    ],
    introduction: [
        { required: true, message: '请输入个人介绍', trigger: 'blur' },
        { min: 50, max: 500, message: '介绍长度应为50-500个字符', trigger: 'blur' }
    ],
    availableTimes: [
        { required: true, message: '请选择至少一个可授课时间', trigger: 'change', type: 'array', min: 1 }
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
            console.log('教师注册成功', form)
            ElMessage.success('注册成功，即将跳转到教师登录页')
            setTimeout(() => {
                router.push('/teacher-login')
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

// 周内时间段选项
const weekdayTimeOptions = [
    { label: '周一 8:00-10:00', value: 'Mon 8:00-10:00' },
    { label: '周一 10:00-12:00', value: 'Mon 10:00-12:00' },
    { label: '周一 14:00-16:00', value: 'Mon 14:00-16:00' },
    { label: '周一 16:00-18:00', value: 'Mon 16:00-18:00' },
    { label: '周一 18:00-20:00', value: 'Mon 18:00-20:00' },
    { label: '周二 8:00-10:00', value: 'Tue 8:00-10:00' },
    { label: '周二 10:00-12:00', value: 'Tue 10:00-12:00' },
    { label: '周二 14:00-16:00', value: 'Tue 14:00-16:00' },
    { label: '周二 16:00-18:00', value: 'Tue 16:00-18:00' },
    { label: '周二 18:00-20:00', value: 'Tue 18:00-20:00' },
    { label: '周三 8:00-10:00', value: 'Wed 8:00-10:00' },
    { label: '周三 10:00-12:00', value: 'Wed 10:00-12:00' },
    { label: '周三 14:00-16:00', value: 'Wed 14:00-16:00' },
    { label: '周三 16:00-18:00', value: 'Wed 16:00-18:00' },
    { label: '周三 18:00-20:00', value: 'Wed 18:00-20:00' },
    { label: '周四 8:00-10:00', value: 'Thu 8:00-10:00' },
    { label: '周四 10:00-12:00', value: 'Thu 10:00-12:00' },
    { label: '周四 14:00-16:00', value: 'Thu 14:00-16:00' },
    { label: '周四 16:00-18:00', value: 'Thu 16:00-18:00' },
    { label: '周四 18:00-20:00', value: 'Thu 18:00-20:00' },
    { label: '周五 8:00-10:00', value: 'Fri 8:00-10:00' },
    { label: '周五 10:00-12:00', value: 'Fri 10:00-12:00' },
    { label: '周五 14:00-16:00', value: 'Fri 14:00-16:00' },
    { label: '周五 16:00-18:00', value: 'Fri 16:00-18:00' },
    { label: '周五 18:00-20:00', value: 'Fri 18:00-20:00' }
]

// 周末时间段选项
const weekendTimeOptions = [
    { label: '周六 8:00-10:00', value: 'Sat 8:00-10:00' },
    { label: '周六 10:00-12:00', value: 'Sat 10:00-12:00' },
    { label: '周六 14:00-16:00', value: 'Sat 14:00-16:00' },
    { label: '周六 16:00-18:00', value: 'Sat 16:00-18:00' },
    { label: '周六 18:00-20:00', value: 'Sat 18:00-20:00' },
    { label: '周日 8:00-10:00', value: 'Sun 8:00-10:00' },
    { label: '周日 10:00-12:00', value: 'Sun 10:00-12:00' },
    { label: '周日 14:00-16:00', value: 'Sun 14:00-16:00' },
    { label: '周日 16:00-18:00', value: 'Sun 16:00-18:00' },
    { label: '周日 18:00-20:00', value: 'Sun 18:00-20:00' }
]
</script>

<template>
    <div class="teacher-register-container">
        <el-card class="register-card">
            <template #header>
                <div class="register-header">
                    <h2>教师注册</h2>
                    <p class="subtitle">加入GrabTeacher平台，成为一名优秀教师</p>
                </div>
            </template>

            <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
                <!-- 基础信息部分 -->
                <h3 class="section-title">基础信息</h3>
                <div class="form-grid">
                    <el-form-item label="用户名" prop="username">
                        <el-input v-model="form.username" placeholder="请输入登录用户名" />
                    </el-form-item>

                    <el-form-item label="真实姓名" prop="realName">
                        <el-input v-model="form.realName" placeholder="请输入真实姓名" />
                    </el-form-item>

                    <el-form-item label="性别" prop="gender">
                        <el-radio-group v-model="form.gender">
                            <el-radio label="Male">男</el-radio>
                            <el-radio label="Female">女</el-radio>
                        </el-radio-group>
                    </el-form-item>
                </div>

                <div class="form-grid">
                    <el-form-item label="密码" prop="password">
                        <el-input v-model="form.password" type="password" placeholder="请输入密码" />
                    </el-form-item>

                    <el-form-item label="确认密码" prop="confirmPassword">
                        <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" />
                    </el-form-item>
                </div>

                <div class="form-grid">
                    <el-form-item label="邮箱" prop="email">
                        <el-input v-model="form.email" placeholder="请输入邮箱地址" />
                    </el-form-item>

                    <el-form-item label="手机号码" prop="phone">
                        <el-input v-model="form.phone" placeholder="请输入手机号码" />
                    </el-form-item>
                </div>

                <!-- 教学信息部分 -->
                <h3 class="section-title">教学信息</h3>
                <div class="form-grid">
                    <el-form-item label="教学经验(年)" prop="experience">
                        <el-input-number v-model="form.experience" :min="0" :max="50" />
                    </el-form-item>

                    <el-form-item label="教学风格" prop="teachingStyle">
                        <el-select v-model="form.teachingStyle" placeholder="请选择您的教学风格" style="width: 100%">
                            <el-option
                                v-for="item in teachingStyleOptions"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value"
                            />
                        </el-select>
                    </el-form-item>
                </div>

                <el-form-item label="可教授科目" prop="subjects">
                    <el-select
                        v-model="form.subjects"
                        multiple
                        collapse-tags
                        collapse-tags-tooltip
                        placeholder="请选择可教授科目"
                        style="width: 100%"
                    >
                        <el-option
                            v-for="item in subjectOptions"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </el-form-item>

                <el-form-item label="可教授年级" prop="grades">
                    <el-select
                        v-model="form.grades"
                        multiple
                        collapse-tags
                        collapse-tags-tooltip
                        placeholder="请选择可教授年级"
                        style="width: 100%"
                    >
                        <el-option-group
                            v-for="group in gradeOptions"
                            :key="group.label"
                            :label="group.label"
                        >
                            <el-option
                                v-for="item in group.options"
                                :key="item.value"
                                :label="item.label"
                                :value="item.value"
                            />
                        </el-option-group>
                    </el-select>
                </el-form-item>

                <el-form-item label="最高学历" prop="education">
                    <el-input v-model="form.education" placeholder="例如：北京大学，计算机科学，硕士" />
                </el-form-item>

                <el-form-item label="专业证书" prop="certificate">
                    <el-input v-model="form.certificate" placeholder="例如：教师资格证，TESOL证书等" />
                </el-form-item>

                <el-form-item label="教师简介" prop="introduction">
                    <el-input
                        v-model="form.introduction"
                        type="textarea"
                        :rows="5"
                        placeholder="请简要介绍您的教学经历、专业特长和教学理念等（50-500字）"
                    />
                </el-form-item>

                <!-- 授课时间部分 -->
                <h3 class="section-title">可授课时间</h3>
                <p class="section-desc">请选择您可以进行授课的时间段</p>

                <el-form-item label="工作日时间" prop="availableTimes">
                    <el-checkbox-group v-model="form.availableTimes">
                        <div class="time-checkboxes">
                            <el-checkbox
                                v-for="option in weekdayTimeOptions"
                                :key="option.value"
                                :label="option.value"
                            >
                                {{ option.label }}
                            </el-checkbox>
                        </div>
                    </el-checkbox-group>
                </el-form-item>

                <el-form-item label="周末时间">
                    <el-checkbox-group v-model="form.availableTimes">
                        <div class="time-checkboxes">
                            <el-checkbox
                                v-for="option in weekendTimeOptions"
                                :key="option.value"
                                :label="option.value"
                            >
                                {{ option.label }}
                            </el-checkbox>
                        </div>
                    </el-checkbox-group>
                </el-form-item>

                <el-form-item prop="agreement">
                    <el-checkbox v-model="form.agreement">
                        我已阅读并同意 <el-link type="primary">《用户协议》</el-link> 和 <el-link type="primary">《隐私政策》</el-link>
                    </el-checkbox>
                </el-form-item>

                <div class="form-actions">
                    <el-button type="primary" @click="submitForm(formRef)" size="large">
                        提交注册
                    </el-button>
                    <el-button @click="resetForm(formRef)" size="large">重置</el-button>
                </div>

                <div class="login-link">
                    已有账号？<el-link type="primary" @click="router.push('/teacher-login')">立即登录</el-link>
                </div>
            </el-form>
        </el-card>
    </div>
</template>

<style scoped>
.teacher-register-container {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 40px 0;
    background-color: #f5f7fa;
}

.register-card {
    width: 100%;
    max-width: 800px;
}

.register-header {
    text-align: center;
    margin-bottom: 10px;
}

h2 {
    margin: 0 0 10px 0;
    font-size: 24px;
    color: #409EFF;
}

.subtitle {
    color: #666;
    margin: 0;
}

.section-title {
    font-size: 18px;
    margin: 30px 0 15px;
    padding-bottom: 8px;
    border-bottom: 1px solid #eee;
    color: #333;
}

.section-desc {
    font-size: 14px;
    color: #666;
    margin-top: -10px;
    margin-bottom: 15px;
}

.form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
}

.time-checkboxes {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 10px;
}

.form-actions {
    display: flex;
    gap: 15px;
    margin-top: 30px;
}

.form-actions .el-button {
    flex: 1;
}

.login-link {
    margin-top: 20px;
    text-align: center;
}

@media (max-width: 768px) {
    .form-grid {
        grid-template-columns: 1fr;
        gap: 10px;
    }

    .time-checkboxes {
        grid-template-columns: 1fr;
    }

    .register-card {
        padding: 15px;
    }
}
</style>
