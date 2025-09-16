<template>
  <div class="classroom-page">
    <div class="classroom-container">
      <div class="classroom-header">
        <h1>课堂</h1>
        <p class="classroom-subtitle">待接入classin课堂</p>
      </div>

      <div class="classroom-content">
        <div class="coming-soon-card">
          <div class="icon-container">
            <el-icon size="80" color="#409EFF">
              <VideoCamera />
            </el-icon>
          </div>
          <h2>课堂功能即将上线</h2>
          <p class="description">
            我们正在接入classin课堂系统，为您提供更好的在线学习体验。
            敬请期待！
          </p>
          <div class="features">
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>高清视频通话</span>
            </div>
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>实时互动白板</span>
            </div>
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>屏幕共享</span>
            </div>
            <div class="feature-item">
              <el-icon><Check /></el-icon>
              <span>课程录制回放</span>
            </div>
          </div>
          <div class="action-buttons">
            <el-button type="primary" @click="goBack">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <el-button @click="contactSupport">
              <el-icon><Service /></el-icon>
              联系客服
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { VideoCamera, Check, ArrowLeft, Service } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const goBack = () => {
  router.back()
}

const contactSupport = () => {
  ElMessage.info('请联系客服获取更多信息')
}
</script>

<style scoped>
.classroom-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.classroom-container {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  max-width: 600px;
  width: 100%;
}

.classroom-header {
  background: linear-gradient(135deg, #409EFF 0%, #67C23A 100%);
  color: white;
  padding: 40px 30px;
  text-align: center;
}

.classroom-header h1 {
  margin: 0 0 10px 0;
  font-size: 2.5rem;
  font-weight: 700;
}

.classroom-subtitle {
  margin: 0;
  font-size: 1.1rem;
  opacity: 0.9;
  font-weight: 500;
}

.classroom-content {
  padding: 50px 30px;
}

.coming-soon-card {
  text-align: center;
}

.icon-container {
  margin-bottom: 30px;
}

.coming-soon-card h2 {
  color: #303133;
  margin: 0 0 20px 0;
  font-size: 1.8rem;
  font-weight: 600;
}

.description {
  color: #606266;
  font-size: 1.1rem;
  line-height: 1.6;
  margin: 0 0 40px 0;
}

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.feature-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 10px;
  color: #606266;
  font-weight: 500;
}

.feature-item .el-icon {
  color: #67C23A;
  font-size: 1.2rem;
}

.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  padding: 12px 24px;
  font-size: 1rem;
  border-radius: 25px;
}

@media (max-width: 768px) {
  .classroom-page {
    padding: 10px;
  }

  .classroom-header {
    padding: 30px 20px;
  }

  .classroom-header h1 {
    font-size: 2rem;
  }

  .classroom-content {
    padding: 30px 20px;
  }

  .features {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
  }

  .action-buttons .el-button {
    width: 100%;
    max-width: 200px;
  }
}
</style>
