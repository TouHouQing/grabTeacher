<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { levelPriceAPI } from '@/utils/api'

interface LevelPrice {
  id: number
  name: string
  price: number
  updateTime?: string
}

const loading = ref(false)
const list = ref<LevelPrice[]>([])

const fetchList = async () => {
  try {
    loading.value = true
    const res = await levelPriceAPI.list()
    if (res.success) {
      list.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取级别价格失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '获取级别价格失败')
  } finally {
    loading.value = false
  }
}

const onPriceChange = async (row: LevelPrice) => {
  try {
    if (row.price == null || row.price < 0) {
      ElMessage.warning('价格必须为非负数')
      return
    }
    loading.value = true
    const res = await levelPriceAPI.updatePrice(row.id, Number(row.price.toFixed ? row.price.toFixed(2) : row.price))
    if (res.success) {
      ElMessage.success('价格已更新')
      await fetchList()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="level-price-mgmt">
    <div class="header">
      <h3>级别价格管理</h3>
    </div>
    <el-card>
      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="级别名称" min-width="160" />
        <el-table-column label="每小时价格 (M豆)" min-width="200">
          <template #default="{ row }">
            <el-input-number
              v-model="row.price"
              :min="0"
              :precision="2"
              :step="10"
              @change="() => onPriceChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="180">
          <template #default="{ row }">
            {{ row.updateTime ? new Date(row.updateTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.level-price-mgmt { padding: 10px; }
.header { margin-bottom: 10px; }
</style>

