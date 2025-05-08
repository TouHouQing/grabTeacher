<script setup lang="ts">
import { ref } from 'vue'

interface User {
    id: number
    username: string
    role: string
    createTime: string
    status: boolean
}

const users = ref<User[]>([
    {
        id: 1,
        username: 'student1',
        role: 'student',
        createTime: '2024-03-20',
        status: true
    },
    {
        id: 2,
        username: 'teacher1',
        role: 'teacher',
        createTime: '2024-03-19',
        status: true
    }
])

const dialogVisible = ref(false)
const form = ref({
    username: '',
    role: 'student',
    password: ''
})

const handleAdd = () => {
    dialogVisible.value = true
}

const handleSubmit = () => {
    // 演示用，实际调用后端API
    users.value.push({
        id: users.value.length + 1,
        username: form.value.username,
        role: form.value.role,
        createTime: new Date().toISOString().split('T')[0],
        status: true
    })

    dialogVisible.value = false
    form.value = {
        username: '',
        role: 'student',
        password: ''
    }
}
</script>

<template>
    <div class="user-management">
        <div class="header">
            <h2>用户管理</h2>
            <el-button type="primary" @click="handleAdd">添加用户</el-button>
        </div>

        <el-table :data="users" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="role" label="角色">
                <template #default="{ row }">
                    <el-tag :type="row.role === 'teacher' ? 'success' : ''">
                        {{ row.role === 'teacher' ? '教师' : '学生' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" />
            <el-table-column prop="status" label="状态">
                <template #default="{ row }">
                    <el-switch v-model="row.status" />
                </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
                <template #default>
                    <el-button link type="primary">编辑</el-button>
                    <el-button link type="danger">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-dialog v-model="dialogVisible" title="添加用户" width="500px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="用户名">
                    <el-input v-model="form.username" />
                </el-form-item>
                <el-form-item label="密码">
                    <el-input v-model="form.password" type="password" />
                </el-form-item>
                <el-form-item label="角色">
                    <el-select v-model="form.role" style="width: 100%">
                        <el-option label="学生" value="student" />
                        <el-option label="教师" value="teacher" />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleSubmit">确定</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.user-management {
    padding: 20px;
}

.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}
</style>