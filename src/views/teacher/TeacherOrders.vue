<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Order {
  id: string;
  date: string;
  student: string;
  course: string;
  status: string;
  amount: number;
  lessons: number;
  time: string;
}

// 模拟订单数据
const orders = ref<Order[]>([
  {
    id: '20230001',
    date: '2023-07-01',
    student: '张明',
    course: '初中数学 - 函数与导数',
    status: '待接单',
    amount: 500,
    lessons: 10,
    time: '周一、周三 18:00-20:00'
  },
  {
    id: '20230002',
    date: '2023-07-05',
    student: '李华',
    course: '初中数学 - 三角函数',
    status: '已接单',
    amount: 600,
    lessons: 12,
    time: '周二、周四 16:00-18:00'
  },
  {
    id: '20230003',
    date: '2023-07-10',
    student: '王芳',
    course: '初中数学 - 立体几何',
    status: '已完成',
    amount: 450,
    lessons: 8,
    time: '周六 9:00-11:00'
  },
  {
    id: '20230004',
    date: '2023-07-12',
    student: '赵强',
    course: '初中数学 - 概率统计',
    status: '已取消',
    amount: 480,
    lessons: 8,
    time: '周日 14:00-16:00'
  }
])

// 订单过滤
const statusFilter = ref('all')
const filteredOrders = computed(() => {
  if (statusFilter.value === 'all') {
    return orders.value
  }
  return orders.value.filter(order => order.status === statusFilter.value)
})

// 接单
const acceptOrder = (orderId: string) => {
  ElMessageBox.confirm('确认接受这个订单吗？', '确认信息', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    const order = orders.value.find(o => o.id === orderId)
    if (order) {
      order.status = '已接单'
      ElMessage.success('订单已接受')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 拒绝订单
const rejectOrder = (orderId: string) => {
  ElMessageBox.confirm('确认拒绝这个订单吗？拒绝后无法恢复。', '确认信息', {
    confirmButtonText: '确认拒绝',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const order = orders.value.find(o => o.id === orderId)
    if (order) {
      order.status = '已取消'
      ElMessage.warning('订单已拒绝')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 完成订单
const completeOrder = (orderId: string) => {
  ElMessageBox.confirm('确认完成这个订单吗？', '确认信息', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'success'
  }).then(() => {
    const order = orders.value.find(o => o.id === orderId)
    if (order) {
      order.status = '已完成'
      ElMessage.success('订单已完成')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

// 查看详情
const orderDetail = ref<Order | null>(null)
const detailDialogVisible = ref(false)

const viewOrderDetail = (order: Order) => {
  orderDetail.value = order
  detailDialogVisible.value = true
}

// 获取订单状态对应的标签类型
const getStatusType = (status: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  switch (status) {
    case '待接单':
      return 'warning'
    case '已接单':
      return 'primary'
    case '已完成':
      return 'success'
    case '已取消':
      return 'info'
    default:
      return 'info'
  }
}
</script>

<template>
  <div class="teacher-orders">
    <h2>订单管理</h2>

    <div class="filter-bar">
      <el-radio-group v-model="statusFilter">
        <el-radio-button label="all">全部订单</el-radio-button>
        <el-radio-button label="待接单">待接单</el-radio-button>
        <el-radio-button label="已接单">已接单</el-radio-button>
        <el-radio-button label="已完成">已完成</el-radio-button>
        <el-radio-button label="已取消">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="filteredOrders" style="width: 100%">
      <el-table-column prop="id" label="订单号" width="100" />
      <el-table-column prop="date" label="下单日期" width="100" />
      <el-table-column prop="student" label="学生" width="80" />
      <el-table-column prop="course" label="课程" min-width="200" />
      <el-table-column prop="lessons" label="课时" width="60" />
      <el-table-column prop="amount" label="金额" width="80">
        <template #default="scope">
          ¥{{ scope.row.amount }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === '待接单'"
            type="primary"
            size="small"
            @click="acceptOrder(scope.row.id)"
          >
            接单
          </el-button>
          <el-button
            v-if="scope.row.status === '待接单'"
            type="danger"
            size="small"
            @click="rejectOrder(scope.row.id)"
          >
            拒绝
          </el-button>
          <el-button
            v-if="scope.row.status === '已接单'"
            type="success"
            size="small"
            @click="completeOrder(scope.row.id)"
          >
            完成
          </el-button>
          <el-button
            type="info"
            size="small"
            @click="viewOrderDetail(scope.row)"
          >
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="订单详情"
      width="50%"
    >
      <div v-if="orderDetail" class="order-detail">
        <div class="detail-row">
          <div class="detail-item">
            <span class="label">订单号：</span>
            <span>{{ orderDetail.id }}</span>
          </div>
          <div class="detail-item">
            <span class="label">下单日期：</span>
            <span>{{ orderDetail.date }}</span>
          </div>
        </div>
        <div class="detail-row">
          <div class="detail-item">
            <span class="label">学生：</span>
            <span>{{ orderDetail.student }}</span>
          </div>
          <div class="detail-item">
            <span class="label">状态：</span>
            <span>{{ orderDetail.status }}</span>
          </div>
        </div>
        <div class="detail-row">
          <div class="detail-item">
            <span class="label">课程：</span>
            <span>{{ orderDetail.course }}</span>
          </div>
          <div class="detail-item">
            <span class="label">课时：</span>
            <span>{{ orderDetail.lessons }}课时</span>
          </div>
        </div>
        <div class="detail-row">
          <div class="detail-item">
            <span class="label">金额：</span>
            <span>¥{{ orderDetail.amount }}</span>
          </div>
          <div class="detail-item">
            <span class="label">上课时间：</span>
            <span>{{ orderDetail.time }}</span>
          </div>
        </div>
        <div class="action-buttons" v-if="orderDetail.status === '待接单'">
          <el-button type="primary" @click="acceptOrder(orderDetail.id)">接受订单</el-button>
          <el-button type="danger" @click="rejectOrder(orderDetail.id)">拒绝订单</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.teacher-orders {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.filter-bar {
  margin-bottom: 20px;
}

.order-detail {
  padding: 20px;
}

.detail-row {
  display: flex;
  margin-bottom: 20px;
}

.detail-item {
  flex: 1;
  display: flex;
}

.label {
  font-weight: bold;
  color: #606266;
  margin-right: 10px;
  width: 80px;
  text-align: right;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 20px;
}
</style>
