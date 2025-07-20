<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminAPI } from '../../../utils/api'

// 统计数据
const statistics = ref({
  totalUsers: 0,
  totalTeachers: 0,
  totalStudents: 0,
  verifiedTeachers: 0
})

const loading = ref(false)

// 获取统计数据
const loadStatistics = async () => {
  try {
    loading.value = true
    const response = await adminAPI.getStatistics()
    if (response.success) {
      statistics.value = response.data
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="admin-dashboard">
    <h3>数据概览</h3>
    <el-row :gutter="16" class="statistics-cards" v-loading="loading">
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <el-card class="statistic-card" shadow="hover">
          <div class="statistic">
            <h4>用户总数</h4>
            <div class="statistic-value">{{ statistics.totalUsers }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <el-card class="statistic-card" shadow="hover">
          <div class="statistic">
            <h4>教师总数</h4>
            <div class="statistic-value">{{ statistics.totalTeachers }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <el-card class="statistic-card" shadow="hover">
          <div class="statistic">
            <h4>学生总数</h4>
            <div class="statistic-value">{{ statistics.totalStudents }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <el-card class="statistic-card" shadow="hover">
          <div class="statistic">
            <h4>认证教师</h4>
            <div class="statistic-value">{{ statistics.verifiedTeachers }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <h4>快捷操作</h4>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="8" :md="8" :lg="8">
          <el-card class="action-card" shadow="hover">
            <div class="action-content">
              <h5>用户管理</h5>
              <p>管理平台用户信息</p>
              <el-button type="primary" size="small">进入管理</el-button>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8" :md="8" :lg="8">
          <el-card class="action-card" shadow="hover">
            <div class="action-content">
              <h5>课程管理</h5>
              <p>管理平台课程信息</p>
              <el-button type="success" size="small">进入管理</el-button>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8" :md="8" :lg="8">
          <el-card class="action-card" shadow="hover">
            <div class="action-content">
              <h5>系统设置</h5>
              <p>配置系统参数</p>
              <el-button type="warning" size="small">进入设置</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.statistics-cards {
  margin-bottom: 30px;
}

.statistic-card {
  margin-bottom: 20px;
}

.statistic {
  text-align: center;
  padding: 20px;
}

.statistic h4 {
  margin: 0 0 10px 0;
  color: #666;
  font-weight: normal;
}

.statistic-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.quick-actions {
  margin-top: 30px;
}

.quick-actions h4 {
  margin-bottom: 20px;
  color: #303133;
}

.action-card {
  height: 120px;
  cursor: pointer;
  transition: transform 0.2s;
}

.action-card:hover {
  transform: translateY(-2px);
}

.action-content {
  text-align: center;
  padding: 10px;
}

.action-content h5 {
  margin: 0 0 8px 0;
  color: #303133;
}

.action-content p {
  margin: 0 0 15px 0;
  color: #909399;
  font-size: 14px;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .admin-dashboard {
    padding: 10px;
  }

  .statistics-cards {
    margin-bottom: 20px;
  }

  .statistic-card {
    margin-bottom: 12px;
  }

  .statistic {
    padding: 15px;
  }

  .statistic h4 {
    font-size: 13px;
    margin-bottom: 8px;
  }

  .statistic-value {
    font-size: 24px;
  }

  .quick-actions h4 {
    font-size: 16px;
    margin-bottom: 15px;
  }

  .action-card {
    margin-bottom: 12px;
    height: auto;
  }

  .action-content {
    padding: 15px;
  }

  .action-content h5 {
    font-size: 15px;
    margin-bottom: 6px;
  }

  .action-content p {
    font-size: 13px;
    margin-bottom: 12px;
  }
}

@media (max-width: 480px) {
  .statistic-value {
    font-size: 20px;
  }

  .statistic h4 {
    font-size: 12px;
  }

  .action-content h5 {
    font-size: 14px;
  }

  .action-content p {
    font-size: 12px;
  }
}
</style>
