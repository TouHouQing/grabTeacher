<template>
  <div class="teacher-hour-details">
    <div class="header">
      <h2>教师明细</h2>
      <p class="description">查看所有教师的课时变动记录</p>
    </div>

    <div class="filter-section">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="教师姓名">
          <el-input
            v-model="filterForm.name"
            placeholder="请输入教师姓名"
            clearable
            style="width: 200px"
            @keyup.enter="loadList"
          />
        </el-form-item>
        <el-form-item label="变动类型">
          <el-select
            v-model="filterForm.transactionType"
            placeholder="请选择变动类型"
            clearable
            style="width: 150px"
          >
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
      <el-table-column prop="id" label="记录ID" width="80" />
      <el-table-column prop="name" label="教师姓名" width="140" />
      <el-table-column label="变动课时" width="120" align="center">
        <template #default="{ row }">
          <span :class="getAmountClass(row.hours)">
            {{ formatHours(row.hours) }}h
          </span>
        </template>
      </el-table-column>
      <el-table-column label="变动前课时" width="130" align="center">
        <template #default="{ row }">
          <span>{{ formatHoursPlain(row.hoursBefore) }}h</span>
        </template>
      </el-table-column>
      <el-table-column label="变动后课时" width="130" align="center">
        <template #default="{ row }">
          <span>{{ formatHoursPlain(row.hoursAfter) }}h</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.transactionType === 1 ? 'success' : 'danger'">
            {{ row.transactionType === 1 ? '增加' : '减少' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="变动原因" min-width="200" show-overflow-tooltip />
      <el-table-column label="关联预约" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.bookingId">
            <el-link type="primary" :underline="false">#{{ row.bookingId }}</el-link>
          </span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作员" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.operatorId" class="operator-info">
            <el-tag size="small" type="info">管理员{{ row.operatorId }}</el-tag>
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
import { teacherHourDetailsAPI } from '../../../utils/api'

const filterForm = reactive({
  name: '',
  transactionType: undefined as undefined | 0 | 1
})

const list = ref([])
const loading = ref(false)

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const loadList = async () => {
  try {
    loading.value = true
    const params: Record<string, any> = {
      page: pagination.page,
      size: pagination.size,
      name: filterForm.name,
      transactionType: filterForm.transactionType
    }
    const res = await teacherHourDetailsAPI.getList(params)
    if (res.success) {
      list.value = res.data.records || []
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取教师课时明细失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取教师课时明细失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadList()
}

const handleReset = () => {
  filterForm.name = ''
  filterForm.transactionType = undefined
  pagination.page = 1
  loadList()
}

const handleSizeChange = (newSize: number) => {
  pagination.size = newSize
  pagination.page = 1
  loadList()
}

const handleCurrentChange = (newPage: number) => {
  pagination.page = newPage
  loadList()
}

const formatHours = (v: number) => (v > 0 ? `+${v}` : `${v}`)
const formatHoursPlain = (v: number) => `${v}`
const getAmountClass = (v: number) => (v > 0 ? 'amount-positive' : v < 0 ? 'amount-negative' : '')

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
  loadList()
})
</script>

<style scoped>
.teacher-hour-details {
  padding: 20px;
}
.header { margin-bottom: 20px; }
.header h2 { margin: 0 0 8px 0; color: #303133; font-size: 20px; font-weight: 600; }
.description { margin: 0; color: #606266; font-size: 14px; }
.filter-section { background: #f8f9fa; padding: 16px; border-radius: 8px; margin-bottom: 20px; }
.filter-form { margin: 0; }
.pagination-container { margin-top: 20px; display: flex; justify-content: center; }
.text-muted { color: #909399; }
.amount-positive { color: #67c23a; font-weight: 600; }
.amount-negative { color: #f56c6c; font-weight: 600; }
.operator-info .el-tag { font-size: 12px; }
</style>


