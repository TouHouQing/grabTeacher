<script setup lang="ts">
import { ref } from 'vue'

const features = ref([
    {
        title: '在线课程',
        description: '丰富的教学视频资源，随时随地学习',
        icon: 'VideoCamera'
    },
    {
        title: '名师授课',
        description: '优秀教师在线授课，提供专业指导',
        icon: 'User'
    },
    {
        title: '个性化学习',
        description: '根据学生需求定制学习计划',
        icon: 'Reading'
    }
])

const courses = ref([
    {
        title: '最新课程',
        list: [
            { name: '高等数学基础', teacher: '张老师', students: 128 },
            { name: '大学物理实验', teacher: '李老师', students: 89 },
            { name: 'Python编程入门', teacher: '王老师', students: 256 }
        ]
    },
    {
        title: '热门课程',
        list: [
            { name: '数据结构与算法', teacher: '刘老师', students: 432 },
            { name: '计算机网络基础', teacher: '陈老师', students: 367 },
            { name: 'Web前端开发', teacher: '赵老师', students: 521 }
        ]
    }
])
</script>

<template>
    <div class="home">
        <section class="banner">
            <div class="banner-content">
                <h1>开启你的在线学习之旅</h1>
                <p>优质的在线教育平台，为每一位学习者提供专业的课程资源</p>
                <el-button type="primary" size="large" @click="$router.push('/videos')">
                    立即开始学习
                </el-button>
            </div>
        </section>

        <section class="features">
            <div class="container">
                <h2>平台特色</h2>
                <el-row :gutter="30">
                    <el-col v-for="(feature, index) in features" :key="index" :span="8">
                        <div class="feature-card">
                            <el-icon size="40" color="#409EFF">
                                <component :is="feature.icon" />
                            </el-icon>
                            <h3>{{ feature.title }}</h3>
                            <p>{{ feature.description }}</p>
                        </div>
                    </el-col>
                </el-row>
            </div>
        </section>

        <section class="courses">
            <div class="container">
                <el-row :gutter="30">
                    <el-col v-for="(section, index) in courses" :key="index" :span="12">
                        <div class="course-section">
                            <h2>{{ section.title }}</h2>
                            <div class="course-list">
                                <el-card v-for="course in section.list" :key="course.name" class="course-card">
                                    <div class="course-info">
                                        <h4>{{ course.name }}</h4>
                                        <p>
                                            <el-icon>
                                                <User />
                                            </el-icon>
                                            {{ course.teacher }}
                                            <span class="student-count">
                                                <el-icon>
                                                    <User />
                                                </el-icon>
                                                {{ course.students }}
                                            </span>
                                        </p>
                                    </div>
                                </el-card>
                            </div>
                        </div>
                    </el-col>
                </el-row>
            </div>
        </section>
    </div>
</template>

<style>
/* 重置一些基础样式 */
body {
    margin: 0;
    padding: 0;
    width: 100%;
}

#app {
    width: 100%;
}

.el-main {
    padding: 0 !important;
    width: 100%;
}
</style>

<style scoped>
.home {
    width: 100%;
}

.banner {
    width: 100%;
    height: 500px;
    background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
}

.banner-content {
    padding: 0 20px;
}

.banner h1 {
    font-size: 3.5em;
    margin-bottom: 20px;
}

.banner p {
    font-size: 1.4em;
    margin-bottom: 30px;
    opacity: 0.9;
}

.container {
    width: 100%;
    padding: 0 20px;
}

section {
    padding: 60px 0;
    width: 100%;
}

section h2 {
    font-size: 2.2em;
    margin-bottom: 40px;
    text-align: center;
    color: #2c3e50;
}

.features {
    background-color: #fff;
}

.feature-card {
    text-align: center;
    padding: 30px;
    height: 100%;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    transition: transform 0.3s;
}

.feature-card:hover {
    transform: translateY(-5px);
}

.feature-card h3 {
    margin: 20px 0;
    color: #2c3e50;
    font-size: 1.4em;
}

.feature-card p {
    color: #666;
    line-height: 1.6;
}

.courses {
    background-color: #f8f9fa;
}

.course-section {
    margin-bottom: 40px;
}

.course-card {
    margin-bottom: 15px;
    cursor: pointer;
    transition: transform 0.3s;
}

.course-card:hover {
    transform: translateX(5px);
}

.course-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.course-info h4 {
    margin: 0;
    color: #2c3e50;
    font-size: 1.2em;
}

.course-info p {
    margin: 0;
    color: #666;
    display: flex;
    align-items: center;
    gap: 8px;
}

.student-count {
    margin-left: 20px;
}

:deep(.el-row) {
    margin: 0 !important;
}

:deep(.el-col) {
    padding: 15px;
}

@media (max-width: 768px) {
    .banner h1 {
        font-size: 2.5em;
    }

    .banner p {
        font-size: 1.2em;
    }

    :deep(.el-col) {
        width: 100% !important;
        flex: 0 0 100% !important;
        max-width: 100% !important;
    }
}
</style>