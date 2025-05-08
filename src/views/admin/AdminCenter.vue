<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

// 获取用户信息
const userStore = useUserStore()
const adminName = ref(userStore.username || '管理员')

// 页面加载状态
const loading = ref(true)

// 统计数据
const statistics = ref({
  totalUsers: 2587,
  totalTeachers: 368,
  totalStudents: 2219,
  totalOrders: 1562,
  totalCourses: 298,
  pendingVerification: 24
})

// 获取统计数据
const fetchStatistics = () => {
  // 模拟API请求
  setTimeout(() => {
    loading.value = false
  }, 800)
}

// 最近注册用户列表
interface User {
  id: number;
  username: string;
  name: string;
  role: string;
  registerTime: string;
  status: string;
}

const recentUsers = ref<User[]>([
  {
    id: 1001,
    username: 'teacher001',
    name: '张老师',
    role: '教师',
    registerTime: '2023-07-10 10:23',
    status: '已认证'
  },
  {
    id: 1002,
    username: 'student002',
    name: '王明',
    role: '学生',
    registerTime: '2023-07-10 14:35',
    status: '正常'
  },
  {
    id: 1003,
    username: 'teacher003',
    name: '李老师',
    role: '教师',
    registerTime: '2023-07-09 09:12',
    status: '待认证'
  },
  {
    id: 1004,
    username: 'student004',
    name: '赵丽',
    role: '学生',
    registerTime: '2023-07-09 16:48',
    status: '正常'
  },
  {
    id: 1005,
    username: 'teacher005',
    name: '王老师',
    role: '教师',
    registerTime: '2023-07-08 11:30',
    status: '已认证'
  }
])

// 待处理认证列表
interface Verification {
  id: number;
  teacherName: string;
  submitTime: string;
  certificateType: string;
  status: string;
}

const pendingVerifications = ref<Verification[]>([
  {
    id: 101,
    teacherName: '张老师',
    submitTime: '2023-07-10 10:23',
    certificateType: '教师资格证',
    status: '待审核'
  },
  {
    id: 102,
    teacherName: '李老师',
    submitTime: '2023-07-09 09:12',
    certificateType: '教师资格证',
    status: '待审核'
  },
  {
    id: 103,
    teacherName: '刘老师',
    submitTime: '2023-07-08 15:42',
    certificateType: '学历证书',
    status: '待审核'
  },
  {
    id: 104,
    teacherName: '陈老师',
    submitTime: '2023-07-08 14:18',
    certificateType: '教师资格证',
    status: '待审核'
  }
])

// 最近订单列表
interface Order {
  id: string;
  student: string;
  teacher: string;
  course: string;
  amount: number;
  status: string;
  time: string;
}

const recentOrders = ref<Order[]>([
  {
    id: 'ORD20230001',
    student: '王明',
    teacher: '张老师',
    course: '高中数学 - 函数与导数',
    amount: 500,
    status: '已支付',
    time: '2023-07-10 15:23'
  },
  {
    id: 'ORD20230002',
    student: '赵丽',
    teacher: '李老师',
    course: '高中英语 - 阅读理解',
    amount: 450,
    status: '已完成',
    time: '2023-07-09 16:48'
  },
  {
    id: 'ORD20230003',
    student: '刘强',
    teacher: '王老师',
    course: '高中物理 - 力学',
    amount: 480,
    status: '待支付',
    time: '2023-07-09 14:32'
  },
  {
    id: 'ORD20230004',
    student: '张华',
    teacher: '陈老师',
    course: '高中化学 - 有机化学',
    amount: 520,
    status: '已取消',
    time: '2023-07-08 10:15'
  }
])

// 审核教师认证
const reviewVerification = (id: number, approved: boolean) => {
  const message = approved ? '确认批准该教师认证？' : '确认拒绝该教师认证？'
  const type = approved ? 'success' : 'warning'

  ElMessageBox.confirm(message, '确认操作', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type
  }).then(() => {
    // 移除该认证项
    const index = pendingVerifications.value.findIndex(v => v.id === id)
    if (index !== -1) {
      pendingVerifications.value.splice(index, 1)
      statistics.value.pendingVerification--

      if (approved) {
        ElMessage.success('已批准教师认证')
      } else {
        ElMessage.warning('已拒绝教师认证')
      }
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 激活/冻结用户
const toggleUserStatus = (id: number, currentStatus: string) => {
  const toStatus = currentStatus === '正常' ? '已冻结' : '正常'
  const message = toStatus === '正常' ? '确认激活该用户？' : '确认冻结该用户？'
  const type = toStatus === '正常' ? 'success' : 'warning'

  ElMessageBox.confirm(message, '确认操作', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type
  }).then(() => {
    // 更新用户状态
    const user = recentUsers.value.find(u => u.id === id)
    if (user) {
      user.status = toStatus
      ElMessage.success(`用户状态已更新为${toStatus}`)
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 获取最近一周的日期标签
const getLastWeekDates = (): string[] => {
  const dates = []
  const today = new Date()

  for (let i = 6; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(today.getDate() - i)
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    dates.push(`${month}-${day}`)
  }

  return dates
}

// 最近一周用户注册数据
const userRegistrationData = ref({
  dates: getLastWeekDates(),
  students: [15, 23, 18, 25, 30, 22, 28],
  teachers: [3, 5, 4, 6, 5, 7, 4]
})

// 最近一周订单数据
const orderData = ref({
  dates: getLastWeekDates(),
  orders: [12, 18, 15, 20, 25, 22, 30],
  amount: [6000, 9000, 7500, 10000, 12500, 11000, 15000]
})

// 页面初始化
onMounted(() => {
  fetchStatistics()
})
</script>

<template>
  <div class="admin-center">
    <h2>管理员控制台 <small>欢迎回来，{{ adminName }}</small></h2>

    <el-row :gutter="20" class="statistics-cards">
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-card class="statistic-card">
          <div class="statistic">
            <h3>用户总数</h3>
            <div class="statistic-value">{{ statistics.totalUsers }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-card class="statistic-card">
          <div class="statistic">
            <h3>教师数量</h3>
            <div class="statistic-value">{{ statistics.totalTeachers }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-card class="statistic-card">
          <div class="statistic">
            <h3>学生数量</h3>
            <div class="statistic-value">{{ statistics.totalStudents }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-card class="statistic-card">
          <div class="statistic">
            <h3>订单总数</h3>
            <div class="statistic-value">{{ statistics.totalOrders }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-card class="statistic-card">
          <div class="statistic">
            <h3>课程数量</h3>
            <div class="statistic-value">{{ statistics.totalCourses }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-card class="statistic-card alert">
          <div class="statistic">
            <h3>待审核认证</h3>
            <div class="statistic-value">{{ statistics.pendingVerification }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-section">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template v-slot:header>
<div >
            <h3>用户注册趋势</h3>
          </div>
</template>
          <div class="chart-container">
            <!-- 图表占位，实际项目中可以使用ECharts等库 -->
            <div class="chart-placeholder">
              <div class="chart-info">
                <div>
                  <span class="chart-label student">学生</span>
                  <span class="chart-label teacher">教师</span>
                </div>
                <div class="chart-dates">
                  <span v-for="(date, index) in userRegistrationData.dates" :key="index">{{ date }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template v-slot:header>
<div >
            <h3>订单统计</h3>
          </div>
</template>
          <div class="chart-container">
            <!-- 图表占位，实际项目中可以使用ECharts等库 -->
            <div class="chart-placeholder">
              <div class="chart-info">
                <div>
                  <span class="chart-label order">订单数</span>
                  <span class="chart-label amount">金额</span>
                </div>
                <div class="chart-dates">
                  <span v-for="(date, index) in orderData.dates" :key="index">{{ date }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="tables-section">
      <el-col :xs="24" :lg="12">
        <el-card class="table-card">
          <template #header>
            <div class="card-header">
              <h3>最近注册用户</h3>
              <el-button type="primary" size="small">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentUsers" style="width: 100%">
            <el-table-column prop="username" label="用户名" width="100" />
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="role" label="角色" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.role === '教师' ? 'primary' : 'success'">
                  {{ scope.row.role }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="registerTime" label="注册时间" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status === '已认证' ? 'success' : scope.row.status === '待认证' ? 'warning' : 'info'">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button
                  type="text"
                  size="small"
                  @click="toggleUserStatus(scope.row.id, scope.row.status)"
                >
                  {{ scope.row.status === '已冻结' ? '激活' : '冻结' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="table-card">
          <template #header>
            <div class="card-header">
              <h3>待审核认证</h3>
              <el-button type="primary" size="small">查看全部</el-button>
            </div>
          </template>
          <el-table :data="pendingVerifications" style="width: 100%">
            <el-table-column prop="teacherName" label="教师" width="100" />
            <el-table-column prop="certificateType" label="证书类型" />
            <el-table-column prop="submitTime" label="提交时间" />
            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button
                  type="success"
                  size="small"
                  @click="reviewVerification(scope.row.id, true)"
                >
                  批准
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="reviewVerification(scope.row.id, false)"
                >
                  拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row>
      <el-col :span="24">
        <el-card class="table-card">
          <template #header>
            <div class="card-header">
              <h3>最近订单</h3>
              <el-button type="primary" size="small">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentOrders" style="width: 100%">
            <el-table-column prop="id" label="订单号" width="130" />
            <el-table-column prop="student" label="学生" width="100" />
            <el-table-column prop="teacher" label="教师" width="100" />
            <el-table-column prop="course" label="课程" />
            <el-table-column prop="amount" label="金额" width="100">
              <template #default="scope">
                ¥{{ scope.row.amount }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === '已完成' ? 'success' : scope.row.status === '已支付' ? 'primary' : scope.row.status === '待支付' ? 'warning' : 'info'">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="time" label="时间" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.admin-center {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

h2 small {
  font-size: 16px;
  color: #909399;
  font-weight: normal;
  margin-left: 10px;
}

h3 {
  margin: 0;
  font-size: 16px;
  font-weight: normal;
  color: #606266;
}

.statistics-cards {
  margin-bottom: 20px;
}

.statistic-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.statistic-card:hover {
  transform: translateY(-5px);
}

.statistic {
  text-align: center;
}

.statistic-value {
  font-size: 28px;
  font-weight: bold;
  margin-top: 10px;
  color: #409EFF;
}

.alert .statistic-value {
  color: #E6A23C;
}

.charts-section {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
}

.chart-placeholder {
  height: 100%;
  background-color: #f8f8f8;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-info {
  text-align: center;
}

.chart-label {
  display: inline-block;
  margin: 0 10px;
  padding: 5px 10px;
  border-radius: 3px;
  color: white;
}

.chart-label.student {
  background-color: #409EFF;
}

.chart-label.teacher {
  background-color: #67C23A;
}

.chart-label.order {
  background-color: #E6A23C;
}

.chart-label.amount {
  background-color: #F56C6C;
}

.chart-dates {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
}

.chart-dates span {
  color: #909399;
  font-size: 12px;
}

.tables-section {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 768px) {
  .statistics-cards .el-col {
    width: 50%;
  }
}
</style>
