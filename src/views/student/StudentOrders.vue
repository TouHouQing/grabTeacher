<script setup lang="ts">
import { ref } from 'vue'
import { computed } from 'vue'
import { ElMessage } from 'element-plus'

interface Order {
  id: string;
  date: string;
  course: string;
  teacher: string;
  amount: number;
  lessons: number;
  status: string;
}

// 模拟订单数据
const orders = ref<Order[]>([
  {
    id: '20230001',
    date: '2023-07-01',
    course: '初中数学 - 函数与导数',
    teacher: '张老师',
    amount: 500,
    lessons: 10,
    status: '已完成'
  },
  {
    id: '20230002',
    date: '2023-07-05',
    course: '初中物理 - 力学与电学',
    teacher: '王老师',
    amount: 600,
    lessons: 12,
    status: '进行中'
  },
  {
    id: '20230003',
    date: '2023-07-10',
    course: '初中英语 - 阅读理解',
    teacher: '李老师',
    amount: 450,
    lessons: 8,
    status: '待付款'
  }
])

// 订单过滤
const orderStatus = ref('all')
const filteredOrders = computed(() => {
  if (orderStatus.value === 'all') {
    return orders.value
  }
  return orders.value.filter(order => order.status === orderStatus.value)
})

// 支付订单
const payOrder = (orderId: string) => {
  const order = orders.value.find(o => o.id === orderId)
  if (order) {
    order.status = '已付款'
    ElMessage.success('支付成功')
  }
}

// 查看详情
const orderDetail = ref<Order | null>(null)
const dialogVisible = ref(false)

const showDetail = (order: Order) => {
  orderDetail.value = order
  dialogVisible.value = true
}

// 订单状态类型映射
const getStatusType = (status: string): string => {
  switch (status) {
    case '待付款':
      return 'warning'
    case '已付款':
      return 'success'
    case '进行中':
      return 'primary'
    case '已完成':
      return 'info'
    default:
      return 'info'
  }
}
</script>

<template>
  <div class="student-orders">
    <h2>我的订单</h2>

    <div class="filter-bar">
      <el-radio-group v-model="orderStatus">
        <el-radio-button label="all">全部订单</el-radio-button>
        <el-radio-button label="待付款">待付款</el-radio-button>
        <el-radio-button label="已付款">已付款</el-radio-button>
        <el-radio-button label="进行中">进行中</el-radio-button>
        <el-radio-button label="已完成">已完成</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="filteredOrders" style="width: 100%">
      <el-table-column prop="id" label="订单号" width="120" />
      <el-table-column prop="date" label="下单日期" width="120" />
      <el-table-column prop="course" label="课程名称" min-width="200" />
      <el-table-column prop="teacher" label="教师" width="100" />
      <el-table-column prop="amount" label="金额" width="100">
        <template #default="scope">
          ¥{{ scope.row.amount }}
        </template>
      </el-table-column>
      <el-table-column prop="lessons" label="课时" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === '待付款'"
            type="primary"
            size="small"
            @click="payOrder(scope.row.id)"
          >
            付款
          </el-button>
          <el-button
            type="info"
            size="small"
            @click="showDetail(scope.row)"
          >
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="订单详情"
      width="50%"
    >
      <div v-if="orderDetail" class="order-detail">
        <div class="detail-item">
          <span class="label">订单号：</span>
          <span>{{ orderDetail.id }}</span>
        </div>
        <div class="detail-item">
          <span class="label">下单日期：</span>
          <span>{{ orderDetail.date }}</span>
        </div>
        <div class="detail-item">
          <span class="label">课程名称：</span>
          <span>{{ orderDetail.course }}</span>
        </div>
        <div class="detail-item">
          <span class="label">教师：</span>
          <span>{{ orderDetail.teacher }}</span>
        </div>
        <div class="detail-item">
          <span class="label">课时：</span>
          <span>{{ orderDetail.lessons }}课时</span>
        </div>
        <div class="detail-item">
          <span class="label">金额：</span>
          <span>¥{{ orderDetail.amount }}</span>
        </div>
        <div class="detail-item">
          <span class="label">状态：</span>
          <span>{{ orderDetail.status }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-orders {
  padding: 20px;
}

.filter-bar {
  margin-bottom: 20px;
}

.order-detail {
  padding: 10px;
}

.detail-item {
  margin-bottom: 10px;
  display: flex;
}

.label {
  width: 100px;
  font-weight: bold;
}
</style>
