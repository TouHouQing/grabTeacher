<template>
  <div class="teacher-hour-details">
    <div class="filter-section">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="类型">
          <el-select v-model="filterForm.transactionType" placeholder="全部" clearable style="width: 150px">
            <el-option label="增加" :value="1" />
            <el-option label="减少" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="list" v-loading="loading" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="变动课时" width="120" align="center">
        <template #default="{ row }">
          <span :class="getAmountClass(row.hours)">{{ formatHours(row.hours) }}h</span>
        </template>
      </el-table-column>
      <el-table-column label="前课时" width="120" align="center">
        <template #default="{ row }">{{ formatHoursPlain(row.hoursBefore) }}h</template>
      </el-table-column>
      <el-table-column label="后课时" width="120" align="center">
        <template #default="{ row }">{{ formatHoursPlain(row.hoursAfter) }}h</template>
      </el-table-column>
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.transactionType === 1 ? 'success' : 'danger'">{{ row.transactionType === 1 ? '增加' : '减少' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
      <el-table-column label="创建时间" width="200" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teacherAPI } from '../../../utils/api'

const filterForm = reactive({ transactionType: undefined as undefined | 0 | 1 })
const list = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 20, total: 0 })

const loadList = async () => {
  try {
    loading.value = true
    const res = await teacherAPI.getMyHourDetails({ page: pagination.page, size: pagination.size, transactionType: filterForm.transactionType as any })
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
const handleReset = () => { filterForm.transactionType = undefined; pagination.page = 1; loadList() }
const handleSizeChange = (s: number) => { pagination.size = s; pagination.page = 1; loadList() }
const handleCurrentChange = (p: number) => { pagination.page = p; loadList() }

const formatHours = (v: number) => (v > 0 ? `+${v}` : `${v}`)
const formatHoursPlain = (v: number) => `${v}`
const getAmountClass = (v: number) => (v > 0 ? 'amount-positive' : v < 0 ? 'amount-negative' : '')
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return ''
  const d = new Date(dateTime)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

onMounted(loadList)
</script>

<style scoped>
.teacher-hour-details { padding: 20px; }
.filter-section { background: #f8f9fa; padding: 16px; border-radius: 8px; margin-bottom: 20px; }
.pagination-container { margin-top: 20px; display: flex; justify-content: center; }
.amount-positive { color: #67c23a; font-weight: 600; }
.amount-negative { color: #f56c6c; font-weight: 600; }
</style>


