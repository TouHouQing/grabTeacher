<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teacherLevelAPI } from '@/utils/api'

interface TeacherLevel {
  id: number
  name: string
  isActive?: boolean
  sortOrder?: number
  createTime?: string
  updateTime?: string
  usageCount?: number
}

const loading = ref(false)
const list = ref<TeacherLevel[]>([])
const creating = ref(false)
const newName = ref('')
const newIsActive = ref(true)
const newSortOrder = ref<number | null>(0)

const fetchList = async () => {
  try {
    loading.value = true
    const res = await teacherLevelAPI.list()
    if (res.success) {
      list.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取教师级别失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '获取教师级别失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)

const createLevel = async () => {
  if (!newName.value.trim()) {
    ElMessage.warning('请输入级别名称')
    return
  }
  try {
    creating.value = true
    const payload = { name: newName.value.trim(), isActive: newIsActive.value, sortOrder: Number(newSortOrder.value ?? 0) }
    const res = await teacherLevelAPI.create(payload)
    if (res.success) {
      ElMessage.success('创建成功')
      newName.value = ''
      newIsActive.value = true
      newSortOrder.value = 0
      await fetchList()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } finally {
    creating.value = false
  }
}

const renameLevel = async (row: TeacherLevel) => {
  const { value } = await ElMessageBox.prompt('输入新的级别名称', '重命名', { inputValue: row.name })
  const name = (value || '').trim()
  if (!name) return
  const res = await teacherLevelAPI.update(row.id, { name })
  if (res.success) {
    ElMessage.success('更新成功')
    await fetchList()
  } else {
    ElMessage.error(res.message || '更新失败')
  }
}

const deleteLevel = async (row: TeacherLevel) => {
  await ElMessageBox.confirm(`确定删除级别“${row.name}”吗？`, '删除确认', { type: 'warning' })
  const res = await teacherLevelAPI.remove(row.id)
  if (res.success) {
    ElMessage.success('删除成功')
    await fetchList()
  } else {
    ElMessage.error(res.message || '删除失败')
  }
}

const toggleActive = async (row: TeacherLevel) => {
  const res = await teacherLevelAPI.update(row.id, { isActive: !row.isActive })
  if (res.success) {
    ElMessage.success('状态已更新')
    await fetchList()
  } else {
    ElMessage.error(res.message || '更新状态失败')
  }
}

const setSortOrder = async (row: TeacherLevel) => {
  const { value } = await ElMessageBox.prompt('输入新的排序（数字，越小越靠前）', '设置排序', {
    inputValue: String(row.sortOrder ?? 0),
    inputPattern: /^\d+$/,
    inputErrorMessage: '请输入非负整数'
  })
  const sortOrder = Number(value)
  const res = await teacherLevelAPI.update(row.id, { sortOrder })
  if (res.success) {
    ElMessage.success('排序已更新')
    await fetchList()
  } else {
    ElMessage.error(res.message || '更新排序失败')
  }
}
</script>

<template>
  <div class="level-mgmt">
    <div class="header">
      <h3>教师级别管理</h3>
    </div>
    <el-card>
      <div style="margin-bottom:12px; display:flex; gap:8px; align-items:center;">
        <el-input v-model="newName" placeholder="输入级别名称" style="width:240px" />
        <el-switch v-model="newIsActive" active-text="启用" inactive-text="停用" />
        <el-input-number v-model="newSortOrder" :min="0" :step="1" placeholder="排序" />
        <el-button type="primary" :loading="creating" @click="createLevel">新增级别</el-button>
      </div>
      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="级别名称" min-width="160" />
        <el-table-column prop="isActive" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'">{{ row.isActive ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" align="center" />
        <el-table-column prop="usageCount" label="使用数量" width="120" align="center">
          <template #default="{ row }">
            {{ row.usageCount ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="180">
          <template #default="{ row }">
            {{ row.updateTime ? new Date(row.updateTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="renameLevel(row)">重命名</el-button>
            <el-button size="small" @click="() => toggleActive(row)">{{ row.isActive ? '停用' : '启用' }}</el-button>
            <el-button size="small" @click="() => setSortOrder(row)">设排序</el-button>
            <el-button size="small" type="danger" @click="deleteLevel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
  </template>

<style scoped>
.level-mgmt { padding: 10px; }
.header { margin-bottom: 10px; }
</style>

