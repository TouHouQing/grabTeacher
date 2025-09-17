<template>
  <div class="student-balance-details">
    <div class="filter-section">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="交易类型">
          <el-select v-model="filterForm.transactionType" placeholder="全部" clearable style="width: 150px">
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
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="交易ID" width="90" />
        <el-table-column label="变动金额" width="120" align="center">
          <template #default="{ row }"><span :class="getAmountClass(row.amount)">{{ formatAmount(row.amount) }}M豆</span></template>
        </el-table-column>
        <el-table-column label="变动前余额" width="130" align="center">
          <template #default="{ row }">{{ row.balanceBefore }}M豆</template>
        </el-table-column>
        <el-table-column label="变动后余额" width="130" align="center">
          <template #default="{ row }">{{ row.balanceAfter }}M豆</template>
        </el-table-column>
        <el-table-column label="交易类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTransactionTypeTag(row.transactionType)">{{ getTransactionTypeName(row.transactionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" width="200" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
    <div class="pagination-container">
      <el-pagination :current-page="pagination.page" :page-size="pagination.size" :total="pagination.total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { myBalanceTransactionAPI } from '../../../utils/api'

const filterForm = reactive({ transactionType: '' as string | '' })
const list = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 20, total: 0 })

const loadList = async () => {
  try {
    loading.value = true
    const res = await myBalanceTransactionAPI.getList({ page: pagination.page, size: pagination.size, transactionType: filterForm.transactionType || undefined })
    if (res.success) {
      list.value = res.data.records || []
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取明细失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取明细失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; loadList() }
const handleReset = () => { filterForm.transactionType = ''; pagination.page = 1; loadList() }
const handleSizeChange = (s: number) => { pagination.size = s; pagination.page = 1; loadList() }
const handleCurrentChange = (p: number) => { pagination.page = p; loadList() }

const formatAmount = (v: number) => (v > 0 ? `+${v}` : `${v}`)
const getAmountClass = (v: number) => (v > 0 ? 'amount-positive' : v < 0 ? 'amount-negative' : '')
const getTransactionTypeTag = (t: string) => (t === 'RECHARGE' ? 'success' : t === 'DEDUCT' ? 'danger' : t === 'REFUND' ? 'warning' : '')
const getTransactionTypeName = (t: string) => (t === 'RECHARGE' ? '充值' : t === 'DEDUCT' ? '扣费' : t === 'REFUND' ? '退费' : t)
const formatDateTime = (dt: string) => {
  if (!dt) return ''
  const d = new Date(dt)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

onMounted(loadList)
</script>

<style scoped>
.student-balance-details { padding: 20px; }
.filter-section { background: #f8f9fa; padding: 16px; border-radius: 8px; margin-bottom: 20px; }
.pagination-container { margin-top: 20px; display: flex; justify-content: center; }

/* 表格横向兜底 */
.table-wrap{ width:100%; overflow-x:auto; }
.table-wrap :deep(table){ min-width:720px; }
/* 筛选表单响应式 */
.filter-form{ display:flex; flex-wrap:wrap; gap:16px; align-items:center; }
.filter-form .el-form-item{ margin-bottom:0; }
@media (max-width:768px){
  .student-balance-details{ padding:16px; }
  .filter-form{ flex-direction:column; align-items:stretch; }
  .filter-form .el-form-item{ width:100%; }
  .filter-form .el-select, .filter-form .el-input{ width:100%; }
}

.amount-positive { color: #67c23a; font-weight: 600; }
.amount-negative { color: #f56c6c; font-weight: 600; }
</style>


