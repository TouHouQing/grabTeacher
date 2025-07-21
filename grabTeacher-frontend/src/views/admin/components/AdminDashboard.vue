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


  </div>
</template>

<style scoped>
.admin-dashboard {
  padding: 20px;
  padding-top: 5px; /* 增加顶部间距，避免被导航栏遮挡 */
}

.admin-dashboard h3 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
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


}

@media (max-width: 480px) {
  .statistic-value {
    font-size: 20px;
  }

  .statistic h4 {
    font-size: 12px;
  }


}
</style>
