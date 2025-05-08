<script setup lang="ts">
import { ref } from 'vue'

interface Video {
    id: number
    title: string
    teacher: string
    description: string
    cover: string
    duration: string
    students: number
}

const videos = ref<Video[]>([
    {
        id: 1,
        title: '数学基础课程',
        teacher: '张老师',
        description: '本课程涵盖高等数学基础知识，适合大一新生学习。包括函数、极限、导数、积分等重要概念。',
        cover: 'https://via.placeholder.com/400x225',
        duration: '12课时',
        students: 328
    },
    {
        id: 2,
        title: '物理实验课程',
        teacher: '李老师',
        description: '通过实验讲解物理现象，加深对物理知识的理解。包括力学、电学、光学等经典实验。',
        cover: 'https://via.placeholder.com/400x225',
        duration: '15课时',
        students: 256
    },
    {
        id: 3,
        title: '编程入门课程',
        teacher: '王老师',
        description: '从零开始学习编程，掌握基本的编程思维和技能。通过实践项目培养编程能力。',
        cover: 'https://via.placeholder.com/400x225',
        duration: '20课时',
        students: 512
    },
    {
        id: 4,
        title: '数据结构与算法',
        teacher: '刘老师',
        description: '系统学习常用数据结构和算法，提高编程能力和问题解决能力。',
        cover: 'https://via.placeholder.com/400x225',
        duration: '18课时',
        students: 423
    }
])
</script>

<template>
    <div class="videos-container">
        <div class="videos-content">
            <div class="page-header">
                <h2>视频课程</h2>
                <div class="filters">
                    <el-input placeholder="搜索课程" prefix-icon="Search" style="width: 300px" />
                    <el-select placeholder="课程分类" style="width: 200px">
                        <el-option label="全部课程" value="" />
                        <el-option label="数学" value="math" />
                        <el-option label="物理" value="physics" />
                        <el-option label="计算机" value="computer" />
                    </el-select>
                </div>
            </div>

            <el-row :gutter="40">
                <el-col v-for="video in videos" :key="video.id" :xs="24" :sm="12" :lg="8" :xl="6">
                    <el-card class="video-card" :body-style="{ padding: 0 }">
                        <div class="video-cover">
                            <img :src="video.cover" :alt="video.title" />
                            <div class="video-duration">{{ video.duration }}</div>
                        </div>
                        <div class="video-content">
                            <h3>{{ video.title }}</h3>
                            <p class="description">{{ video.description }}</p>
                            <div class="video-footer">
                                <div class="teacher-info">
                                    <el-icon>
                                        <User />
                                    </el-icon>
                                    {{ video.teacher }}
                                </div>
                                <div class="student-count">
                                    <el-icon>
                                        <User />
                                    </el-icon>
                                    {{ video.students }}人学习
                                </div>
                            </div>
                            <el-button type="primary" class="watch-btn">观看课程</el-button>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>
    </div>
</template>

<style scoped>
.videos-container {
    width: 100%;
}

.videos-content {
    max-width: 1800px;
    margin: 0 auto;
    padding: 20px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 40px;
}

.page-header h2 {
    font-size: 2.2em;
    margin: 0;
    color: #2c3e50;
}

.filters {
    display: flex;
    gap: 20px;
}

.video-card {
    margin-bottom: 40px;
    transition: transform 0.3s;
    border: none;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.video-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.video-cover {
    position: relative;
    height: 260px;
}

.video-cover img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.video-duration {
    position: absolute;
    bottom: 12px;
    right: 12px;
    background: rgba(0, 0, 0, 0.7);
    color: white;
    padding: 6px 12px;
    border-radius: 4px;
    font-size: 14px;
}

.video-content {
    padding: 24px;
}

.video-content h3 {
    margin: 0 0 12px;
    font-size: 1.4em;
    color: #2c3e50;
}

.description {
    color: #666;
    font-size: 15px;
    line-height: 1.6;
    margin-bottom: 20px;
    height: 72px;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
}

.video-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    color: #666;
    font-size: 15px;
}

.teacher-info,
.student-count {
    display: flex;
    align-items: center;
    gap: 8px;
}

.watch-btn {
    width: 100%;
    height: 40px;
    font-size: 16px;
}

.el-icon {
    vertical-align: middle;
}

@media (max-width: 1200px) {
    .videos-content {
        padding: 15px;
    }

    .video-cover {
        height: 220px;
    }
}

@media (max-width: 768px) {
    .page-header {
        flex-direction: column;
        align-items: stretch;
        gap: 20px;
    }

    .filters {
        flex-direction: column;
    }

    .video-cover {
        height: 200px;
    }
}
</style>