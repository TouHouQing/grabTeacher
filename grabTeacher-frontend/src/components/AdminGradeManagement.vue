<template>
  <div class="grade-management">
    <div class="header">
      <h3>年级管理</h3>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never" style="margin-bottom: 20px; display: block !important; visibility: visible !important;">
      <el-form :model="searchForm" :inline="true" class="search-form" style="display: flex !important; align-items: center !important;">
        <el-form-item label="年级名称" style="margin-right: 15px;">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入年级名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item style="margin-bottom: 0;">
          <el-button type="primary" :icon="Search" @click="searchGrades">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch" style="margin-left: 10px;">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="showCreateDialog" style="margin-left: 10px;">
            添加年级
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 年级列表 -->
    <el-table :data="grades" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="gradeName" label="年级名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="courseCount" label="关联课程数" width="120" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="scope">
          <div class="operation-buttons">
            <el-button size="small" :icon="Edit" @click="editGrade(scope.row)">编辑</el-button>
            <el-button
              size="small"
              type="danger"
              :icon="Delete"
              @click="deleteGrade(scope.row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建/编辑年级对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      @close="resetForm"
    >
      <el-form
        :model="gradeForm"
        :rules="formRules"
        ref="gradeFormRef"
        label-width="100px"
      >
        <el-form-item label="年级名称" prop="gradeName">
          <el-input v-model="gradeForm.gradeName" placeholder="请输入年级名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="gradeForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入年级描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { gradeApi } from '../utils/api'

// 响应式数据
const grades = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)

// 搜索表单
const searchForm = reactive({
  keyword: ''
})

// 表单数据
const gradeForm = reactive({
  gradeName: '',
  description: ''
})

// 表单验证规则
const formRules = {
  gradeName: [
    { required: true, message: '请输入年级名称', trigger: 'blur' },
    { max: 50, message: '年级名称长度不能超过50个字符', trigger: 'blur' }
  ]
}

// 表单引用
const gradeFormRef = ref()

// 计算属性
const dialogTitle = computed(() => isEdit.value ? '编辑年级' : '添加年级')

// 方法
const loadGrades = async () => {
  loading.value = true
  try {
    const response = await gradeApi.getAll()
    if (response.success) {
      let filteredGrades = response.data
      // 如果有搜索关键词，进行过滤
      if (searchForm.keyword) {
        filteredGrades = response.data.filter((grade: any) =>
          grade.gradeName.toLowerCase().includes(searchForm.keyword.toLowerCase())
        )
      }
      grades.value = filteredGrades
    } else {
      ElMessage.error(response.message || '获取年级列表失败')
    }
  } catch (error) {
    console.error('获取年级列表失败:', error)
    ElMessage.error('获取年级列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索年级
const searchGrades = () => {
  loadGrades()
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  loadGrades()
}

const showCreateDialog = () => {
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const editGrade = (grade: any) => {
  isEdit.value = true
  editingId.value = grade.id
  gradeForm.gradeName = grade.gradeName
  gradeForm.description = grade.description || ''
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!gradeFormRef.value) return

  try {
    await gradeFormRef.value.validate()
    submitting.value = true

    let response: any
    if (isEdit.value) {
      response = await gradeApi.update(editingId.value, gradeForm)
    } else {
      response = await gradeApi.create(gradeForm)
    }

    if (response.success) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      await loadGrades()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error: any) {
    console.error('提交表单失败:', error)
    // 如果错误对象包含具体的错误信息，显示具体信息
    const errorMessage = error.response?.message || error.message || '操作失败'
    ElMessage.error(errorMessage)
  } finally {
    submitting.value = false
  }
}

const deleteGrade = async (grade: any) => {
  try {
    let confirmMessage = `确定要删除年级"${grade.gradeName}"吗？`

    if (grade.courseCount > 0) {
      confirmMessage = `确定要删除年级"${grade.gradeName}"吗？\n\n注意：删除年级将同时删除该年级下的 ${grade.courseCount} 个课程及相关数据，此操作不可恢复！`
    }

    await ElMessageBox.confirm(
      confirmMessage,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: false,
      }
    )

    const response = await gradeApi.delete(grade.id)
    if (response.success) {
      ElMessage.success('删除成功')
      await loadGrades()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除年级失败:', error)
      // 如果错误对象包含具体的错误信息，显示具体信息
      const errorMessage = error.response?.message || error.message || '删除失败'
      ElMessage.error(errorMessage)
    }
  }
}

const resetForm = () => {
  gradeForm.gradeName = ''
  gradeForm.description = ''
  if (gradeFormRef.value) {
    gradeFormRef.value.resetFields()
  }
}

const formatDate = (dateString: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  loadGrades()
})
</script>

<script lang="ts">
export default {
  name: 'AdminGradeManagement'
}
</script>

<style scoped>
.grade-management {
  padding: 20px;
  padding-top: 5px; /* 增加顶部间距，避免被导航栏遮挡 */
}

.header {
  margin-bottom: 20px;
}

.header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.search-card {
  margin-bottom: 20px !important;
  border: 1px solid #e4e7ed !important;
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
}

.search-form {
  margin-bottom: 0 !important;
  display: flex !important;
  align-items: center !important;
  flex-wrap: wrap !important;
}

.search-form .el-form-item {
  margin-bottom: 0 !important;
  margin-right: 15px !important;
}

.search-form .el-form-item:last-child {
  margin-right: 0 !important;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
