<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { gradeAPI } from '../../../utils/api'

// 列表与加载状态
const list = ref<any[]>([])
const loading = ref(false)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: '' as '' | '启用' | '停用'
})

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 对话框与表单
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive({
  id: 0,
  name: '',
  isActive: true,
  sortOrder: 0
})

const rules = {
  name: [{ required: true, message: '请输入年级名称', trigger: 'blur' }]
}

// 加载列表
const loadList = async () => {
  try {
    loading.value = true
    const params: any = {
      page: pagination.currentPage,
      size: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      isActive: searchForm.status === '启用' ? true : searchForm.status === '停用' ? false : undefined
    }
    const res = await gradeAPI.getList(params)
    if (res?.success && res.data) {
      const data = res.data
      list.value = Array.isArray(data) ? data : (data.grades || [])
      pagination.total = data.total ?? list.value.length
    }
  } catch (e: any) {
    console.error('获取年级失败:', e)
    ElMessage.error(e?.message || '获取列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const onSearch = () => { pagination.currentPage = 1; loadList() }
const onReset = () => { searchForm.keyword = ''; searchForm.status = ''; pagination.currentPage = 1; loadList() }

// 新增
const onAdd = () => {
  dialogTitle.value = '添加年级'
  Object.assign(form, { id: 0, name: '', isActive: true, sortOrder: 0 })
  dialogVisible.value = true
}

// 编辑
const onEdit = (row: any) => {
  dialogTitle.value = '编辑年级'
  Object.assign(form, row)
  dialogVisible.value = true
}

// 保存
const onSave = async () => {
  try {
    loading.value = true
    const payload = { name: form.name, isActive: form.isActive, sortOrder: form.sortOrder }
    const res = form.id === 0
      ? await gradeAPI.create(payload)
      : await gradeAPI.update(form.id, payload)
    if (res?.success) {
      ElMessage.success(form.id === 0 ? '添加成功' : '更新成功')
      dialogVisible.value = false
      await loadList()
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e: any) {
    console.error('保存年级失败:', e)
    ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 删除
const onDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除年级"${row.name}"吗？`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await gradeAPI.delete(row.id)
    if (res?.success) {
      ElMessage.success('删除成功')
      await loadList()
    } else {
      ElMessage.error(res?.message || '删除失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      console.error('删除年级失败:', e)
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

// 切换状态
const onToggleStatus = async (row: any) => {
  try {
    const res = await gradeAPI.updateStatus(row.id, !row.isActive)
    if (res?.success) {
      ElMessage.success(row.isActive ? '已停用' : '已启用')
      await loadList()
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e: any) {
    console.error('更新状态失败:', e)
    ElMessage.error(e?.message || '操作失败')
  }
}

// 分页
const onPageChange = (page: number) => { pagination.currentPage = page; loadList() }
const onSizeChange = (size: number) => { pagination.pageSize = size; pagination.currentPage = 1; loadList() }

onMounted(() => { loadList() })
</script>

<template>
  <div class="grade-management">
    <div class="header">
      <h3>年级管理</h3>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索名称" :prefix-icon="Search" clearable style="width: 240px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="onAdd">新增年级</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="sortOrder" label="排序" width="100" align="center" />
      <el-table-column prop="isActive" label="状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'danger'">{{ row.isActive ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right" align="center">
        <template #default="{ row }">
          <div class="operation-buttons">
            <el-button size="small" :icon="Edit" @click="onEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.isActive ? 'warning' : 'success'" @click="onToggleStatus(row)">
              {{ row.isActive ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="onDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        :current-page="pagination.currentPage"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入年级名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model.number="form.sortOrder" type="number" placeholder="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.isActive" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="onSave" :loading="loading">{{ form.id === 0 ? '添加' : '更新' }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.grade-management { padding: 20px; padding-top: 5px; }
.header { display: flex; justify-content: flex-start; align-items: center; margin-bottom: 20px; }
.header h3 { margin: 0; color: #303133; font-size: 20px; font-weight: 600; }
.search-card { margin-bottom: 20px; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }
.operation-buttons { display: flex; gap: 8px; flex-wrap: nowrap; align-items: center; justify-content: center; white-space: nowrap; width: 100%; padding: 0 8px; }
.operation-buttons .el-button { margin: 0; min-width: 40px; text-align: center; }
:deep(.el-table__header-wrapper .el-table__header th:last-child .cell) { text-align: center; }
@media (max-width: 768px) { .operation-buttons { flex-wrap: wrap; gap: 4px; justify-content: center; } .operation-buttons .el-button { font-size: 12px; padding: 4px 8px; min-width: 50px; } }
</style>

