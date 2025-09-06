<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { subjectAPI } from '../../../utils/api'

// 科目列表
const subjectList = ref([])
const loading = ref(false)

// 搜索表单
const subjectSearchForm = reactive({
  keyword: '',
  status: ''
})

// 分页信息
const subjectPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 对话框
const subjectDialogVisible = ref(false)
const subjectDialogTitle = ref('')
const subjectForm = reactive({
  id: 0,
  name: '',
  iconUrl: '',
  active: true
})

// 表单验证规则
const subjectRules = {
  name: [
    { required: true, message: '请输入科目名称', trigger: 'blur' }
  ],
}

// 获取科目列表
const loadSubjectList = async () => {
  try {
    loading.value = true
    const params = {
      page: subjectPagination.currentPage,
      size: subjectPagination.pageSize,
      keyword: subjectSearchForm.keyword || undefined,
      isActive: subjectSearchForm.status === '启用' ? true : subjectSearchForm.status === '停用' ? false : undefined
    }

    const result = await subjectAPI.getList(params)

    if (result.success && result.data) {
      if (Array.isArray(result.data)) {
        subjectList.value = result.data
        subjectPagination.total = result.data.length
      } else if (result.data.subjects) {
        subjectList.value = result.data.subjects
        subjectPagination.total = result.data.total || result.data.subjects.length
      }
    }
  } catch (error: any) {
    console.error('获取科目列表失败:', error)
    ElMessage.error(error.message || '获取科目列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索科目
const searchSubjects = () => {
  subjectPagination.currentPage = 1
  loadSubjectList()
}

// 重置搜索
const resetSubjectSearch = () => {
  subjectSearchForm.keyword = ''
  subjectSearchForm.status = ''
  subjectPagination.currentPage = 1
  loadSubjectList()
}

// 添加科目
const handleAddSubject = () => {
  subjectDialogTitle.value = '添加科目'
  Object.assign(subjectForm, {
    id: 0,
    name: '',
    iconUrl: '',
    active: true
  })
  subjectDialogVisible.value = true
}

// 编辑科目
const handleEditSubject = (subject: any) => {
  subjectDialogTitle.value = '编辑科目'
  Object.assign(subjectForm, subject)
  subjectDialogVisible.value = true
}

// 保存科目
const saveSubject = async () => {
  try {
    loading.value = true
    const subjectData = {
      name: subjectForm.name,
      iconUrl: subjectForm.iconUrl,
      active: subjectForm.active
    }

    let result
    if (subjectForm.id === 0) {
      result = await subjectAPI.create(subjectData)
    } else {
      result = await subjectAPI.update(subjectForm.id, subjectData)
    }

    if (result.success) {
      ElMessage.success(subjectForm.id === 0 ? '添加成功' : '更新成功')
      subjectDialogVisible.value = false
      await loadSubjectList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('保存科目失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 删除科目
const handleDeleteSubject = async (subject: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除科目"${subject.name}"吗？\n\n注意：删除科目将同时删除该科目下的所有课程及相关数据，此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: false,
      }
    )

    const result = await subjectAPI.delete(subject.id)
    if (result.success) {
      ElMessage.success('删除成功')
      await loadSubjectList()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除科目失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 切换科目状态
const toggleSubjectStatus = async (subject: any) => {
  try {
    const result = await subjectAPI.updateStatus(subject.id, !subject.active)
    if (result.success) {
      ElMessage.success(subject.active ? '已停用' : '已启用')
      await loadSubjectList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('更新科目状态失败:', error)
    ElMessage.error(error.message || '操作失败')
  }
}

// 分页处理
const handleSubjectPageChange = (page: number) => {
  subjectPagination.currentPage = page
  loadSubjectList()
}

const handleSubjectSizeChange = (size: number) => {
  subjectPagination.pageSize = size
  subjectPagination.currentPage = 1
  loadSubjectList()
}

onMounted(() => {
  loadSubjectList()
})
</script>

<template>
  <div class="subject-management">
    <div class="header">
      <h3>科目管理</h3>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="subjectSearchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="subjectSearchForm.keyword"
            placeholder="搜索科目名称"
            :prefix-icon="Search"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="subjectSearchForm.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="searchSubjects">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSubjectSearch">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAddSubject">
            添加科目
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 科目表格 -->
    <el-table :data="subjectList" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="科目名称" width="400" />
      <el-table-column prop="iconUrl" label="图标" width="400">
        <template #default="{ row }">
          <el-image
            v-if="row.iconUrl"
            :src="row.iconUrl"
            style="width: 40px; height: 40px"
            fit="cover"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="active" label="状态" width="200">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'danger'">
            {{ row.active ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right" align="center">
        <template #default="{ row }">
          <div class="operation-buttons">
            <el-button size="small" :icon="Edit" @click="handleEditSubject(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.active ? 'warning' : 'success'"
              @click="toggleSubjectStatus(row)"
            >
              {{ row.active ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDeleteSubject(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        :current-page="subjectPagination.currentPage"
        :page-size="subjectPagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="subjectPagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSubjectSizeChange"
        @current-change="handleSubjectPageChange"
      />
    </div>

    <!-- 添加/编辑科目对话框 -->
    <el-dialog
      v-model="subjectDialogVisible"
      :title="subjectDialogTitle"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="subjectForm" :rules="subjectRules" label-width="100px">
        <el-form-item label="科目名称" prop="name">
          <el-input v-model="subjectForm.name" placeholder="请输入科目名称" />
        </el-form-item>
        <el-form-item label="图标URL">
          <el-input v-model="subjectForm.iconUrl" placeholder="请输入图标链接地址" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="subjectForm.active" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="subjectDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveSubject" :loading="loading">
            {{ subjectForm.id === 0 ? '添加' : '更新' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.subject-management {
  padding: 20px;
  padding-top: 5px; /* 增加顶部间距，避免被导航栏遮挡 */
}

.header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 20px;
}

.header h3 {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.search-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  width: 100%;
  padding: 0 8px;
}

.operation-buttons .el-button {
  margin: 0;
  min-width: 40px;
  text-align: center;
}

/* 确保操作列标题居中 */
:deep(.el-table__header-wrapper .el-table__header th:last-child .cell) {
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .operation-buttons {
    flex-wrap: wrap;
    gap: 4px;
    justify-content: center;
  }

  .operation-buttons .el-button {
    font-size: 12px;
    padding: 4px 8px;
    min-width: 50px;
  }
}
</style>
