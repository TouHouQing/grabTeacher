<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { studyAbroadAPI } from '../../../utils/api'

const loading = ref(false)
const list = ref<any[]>([])

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ keyword: '', status: '' as '' | '启用' | '停用' })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive({ id: 0, stageName: '', sortOrder: 0, active: true })

const rules = {
  stageName: [{ required: true, message: '请输入阶段名称', trigger: 'blur' }]
}

const loadList = async () => {
  try {
    loading.value = true
    const params: any = {
      page: pagination.currentPage,
      size: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      isActive: searchForm.status === '启用' ? true : searchForm.status === '停用' ? false : undefined
    }
    const res = await studyAbroadAPI.adminListStages(params)
    if (res?.success) {
      const data = res.data || {}
      list.value = data.records || []
      pagination.total = data.total || list.value.length
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '获取阶段列表失败')
  } finally {
    loading.value = false
  }
}

const onSearch = () => { pagination.currentPage = 1; loadList() }
const onReset = () => { searchForm.keyword = ''; searchForm.status = ''; pagination.currentPage = 1; loadList() }

const onAdd = () => {
  dialogTitle.value = '添加阶段'
  Object.assign(form, { id: 0, stageName: '', sortOrder: 0, active: true })
  dialogVisible.value = true
}

const onEdit = (row: any) => {
  dialogTitle.value = '编辑阶段'
  Object.assign(form, row)
  dialogVisible.value = true
}

const onSave = async () => {
  try {
    loading.value = true
    const payload = { stageName: form.stageName, sortOrder: form.sortOrder, active: form.active }
    const res = form.id === 0
      ? await studyAbroadAPI.adminCreateStage(payload)
      : await studyAbroadAPI.adminUpdateStage(form.id, payload)
    if (res?.success) {
      ElMessage.success(form.id === 0 ? '添加成功' : '更新成功')
      dialogVisible.value = false
      await loadList()
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

const onDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除阶段“${row.stageName}”吗？`, '提示', { type: 'warning' })
    const res = await studyAbroadAPI.adminDeleteStage(row.id)
    if (res?.success) {
      ElMessage.success('删除成功')
      loadList()
    } else {
      ElMessage.error(res?.message || '删除失败')
    }
  } catch (e: any) {
    // 后端如果返回400，apiRequest会抛出异常，这里统一提示后端返回的message
    const msg = e?.response?.message || e?.message || '删除失败'
    ElMessage.error(msg)
  }
}

const onToggleStatus = async (row: any) => {
  try {
    const res = await studyAbroadAPI.adminUpdateStageStatus(row.id, !row.active)
    if (res?.success) { ElMessage.success(row.active ? '已停用' : '已启用'); loadList() } else { ElMessage.error(res?.message || '操作失败') }
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
}

const onPageChange = (p: number) => { pagination.currentPage = p; loadList() }
const onSizeChange = (s: number) => { pagination.pageSize = s; pagination.currentPage = 1; loadList() }

onMounted(loadList)
</script>

<template>
  <div class="stage-mgmt">
    <div class="header"><h3>留学阶段管理</h3></div>

    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索阶段名称" :prefix-icon="Search" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 140px">
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="onAdd">添加阶段</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="stageName" label="阶段名称" />
      <el-table-column prop="sortOrder" label="排序" width="100" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'danger'">{{ row.active ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right" align="center">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click="onEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.active ? 'warning' : 'success'" @click="onToggleStatus(row)">{{ row.active ? '停用' : '启用' }}</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination :current-page="pagination.currentPage" :page-size="pagination.pageSize" :total="pagination.total"
                     :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next, jumper"
                     @current-change="onPageChange" @size-change="onSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-form-item prop="stageName" label="阶段名称">
          <el-input v-model="form.stageName" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model.number="form.sortOrder" type="number" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.active" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stage-mgmt { padding: 20px; padding-top: 5px; }
.header { margin-bottom: 16px; }
.search-card { margin-bottom: 16px; }
.pagination { display: flex; justify-content: center; margin: 16px 0; }
</style>

