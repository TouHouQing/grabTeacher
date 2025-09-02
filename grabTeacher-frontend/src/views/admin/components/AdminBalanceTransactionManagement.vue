<template>
  <div class="balance-transaction-management">
    <div class="header">
      <h2>学生明细</h2>
      <p class="description">查看所有学生的余额变动记录</p>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="学生姓名">
          <el-input
            v-model="filterForm.name"
            placeholder="请输入学生姓名"
            clearable
            style="width: 200px"
            @keyup.enter="loadTransactionList"
          />
        </el-form-item>
        <el-form-item label="交易类型">
          <el-select
            v-model="filterForm.transactionType"
            placeholder="请选择交易类型"
            clearable
            style="width: 150px"
          >
            <el-option label="充值" value="RECHARGE" />
            <el-option label="扣费" value="DEDUCT" />
            <el-option label="退费" value="REFUND" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 交易记录表格 -->
    <el-table
      :data="transactionList"
      v-loading="loading"
      stripe
      border
      style="width: 100%"
    >
      <el-table-column prop="id" label="交易ID" width="70" />
      <el-table-column prop="name" label="学生姓名" width="120" />
      <el-table-column label="变动金额" width="120" align="center">
        <template #default="{ row }">
          <span :class="getAmountClass(row.amount)">
            {{ formatAmount(row.amount) }}M豆
          </span>
        </template>
      </el-table-column>
      <el-table-column label="变动前余额" width="120" align="center">
        <template #default="{ row }">
          <span>{{ row.balanceBefore }}M豆</span>
        </template>
      </el-table-column>
      <el-table-column label="变动后余额" width="120" align="center">
        <template #default="{ row }">
          <span>{{ row.balanceAfter }}M豆</span>
        </template>
      </el-table-column>
      <el-table-column label="交易类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="getTransactionTypeTag(row.transactionType)">
            {{ getTransactionTypeName(row.transactionType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="变动原因" min-width="200" show-overflow-tooltip />
      <el-table-column label="关联预约" width="85" align="center">
        <template #default="{ row }">
          <span v-if="row.bookingId" class="booking-link">
            <el-link type="primary" :underline="false">
              #{{ row.bookingId }}
            </el-link>
          </span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作员" width="80" align="center">
        <template #default="{ row }">
          <span v-if="row.operatorId" class="operator-info">
            <el-tag size="small" type="info">
              管理员{{ row.operatorId }}
            </el-tag>
          </span>
          <span v-else class="text-muted">系统</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="200" align="center">
        <template #default="{ row }">
          <span>{{ formatDateTime(row.createdAt) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { balanceTransactionAPI } from '../../../utils/api'

// 筛选表单
const filterForm = reactive({
  name: '',
  transactionType: ''
})

// 交易记录列表
const transactionList = ref([])
const loading = ref(false)

// 分页信息
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 加载交易记录列表
const loadTransactionList = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.page,
      size: pagination.size,
      ...filterForm
    }

    // 清理空值
    Object.keys(params).forEach(key => {
      if (params[key] === '') {
        delete params[key]
      }
    })

    const response = await balanceTransactionAPI.getList(params)
    if (response.success) {
      transactionList.value = response.data.records || []
      pagination.total = response.data.total || 0
    } else {
      ElMessage.error(response.message || '获取交易记录失败')
    }
  } catch (error: any) {
    console.error('获取交易记录失败:', error)
    ElMessage.error(error.message || '获取交易记录失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadTransactionList()
}

// 重置筛选条件
const handleReset = () => {
  filterForm.name = ''
  filterForm.transactionType = ''
  pagination.page = 1
  loadTransactionList()
}

// 分页大小改变
const handleSizeChange = (newSize: number) => {
  pagination.size = newSize
  pagination.page = 1
  loadTransactionList()
}

// 当前页改变
const handleCurrentChange = (newPage: number) => {
  pagination.page = newPage
  loadTransactionList()
}

// 格式化金额显示
const formatAmount = (amount: number) => {
  if (amount > 0) {
    return `+${amount}`
  }
  return amount.toString()
}

// 获取金额样式类
const getAmountClass = (amount: number) => {
  if (amount > 0) {
    return 'amount-positive'
  } else if (amount < 0) {
    return 'amount-negative'
  }
  return ''
}

// 获取交易类型标签颜色
const getTransactionTypeTag = (type: string) => {
  switch (type) {
    case 'RECHARGE':
      return 'success'
    case 'DEDUCT':
      return 'danger'
    case 'REFUND':
      return 'warning'
    default:
      return ''
  }
}

// 获取交易类型名称
const getTransactionTypeName = (type: string) => {
  switch (type) {
    case 'RECHARGE':
      return '充值'
    case 'DEDUCT':
      return '扣费'
    case 'REFUND':
      return '退费'
    default:
      return type
  }
}

// 格式化日期时间
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  loadTransactionList()
})
</script>

<style scoped>
.balance-transaction-management {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.header h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.description {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.filter-section {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-form {
  margin: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.text-muted {
  color: #909399;
}

.amount-positive {
  color: #67c23a;
  font-weight: 600;
}

.amount-negative {
  color: #f56c6c;
  font-weight: 600;
}

.booking-link {
  cursor: pointer;
}

.operator-info .el-tag {
  font-size: 12px;
}
</style>
