<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

// 如果不是教师，重定向到首页
if (!userStore.isTeacher) {
    router.push('/')
}
</script>

<template>
    <el-container class="admin-container">
        <el-aside width="200px">
            <el-menu router default-active="/admin/users" class="admin-menu">
                <el-menu-item index="/admin/users">
                    <el-icon>
                        <User />
                    </el-icon>
                    <span>用户管理</span>
                </el-menu-item>
            </el-menu>
        </el-aside>

        <el-main>
            <router-view></router-view>
        </el-main>
    </el-container>
</template>

<style scoped>
.admin-container {
    height: calc(100vh - 60px);
}

.admin-menu {
    height: 100%;
    border-right: none;
}

.el-aside {
    background-color: #f0f2f5;
    border-right: 1px solid #dcdfe6;
}
</style>