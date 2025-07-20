<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { studentAPI } from '../../../utils/api'

// 学生列表
const studentList = ref([])
const loading = ref(false)

// 搜索表单
const studentSearchForm = reactive({
  keyword: '',
  gradeLevel: ''
})

// 分页信息
const studentPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 对话框
const studentDialogVisible = ref(false)
const studentDialogTitle = ref('')
const studentForm = reactive({
  id: 0,
  realName: '',
  gradeLevel: '',
  subjectsInterested: '',
  learningGoals: '',
  preferredTeachingStyle: '',
  budgetRange: ''
})

// 表单验证规则
const studentRules = {
  realName: [
    { required: true, message: '请输入学生姓名', trigger: 'blur' }
  ]
}

// 获取学生列表
const loadStudentList = async () => {
  try {
    loading.value = true
    const params = {
      page: studentPagination.currentPage,
      size: studentPagination.pageSize,
      keyword: studentSearchForm.keyword || undefined,
      gradeLevel: studentSearchForm.gradeLevel || undefined
    }

    const result = await studentAPI.getList(params)

    if (result.success && result.data) {
      if (Array.isArray(result.data)) {
        studentList.value = result.data
        studentPagination.total = result.data.length
      } else if (result.data.records) {
        studentList.value = result.data.records
        studentPagination.total = result.data.total || result.data.records.length
      }
    }
  } catch (error: any) {
    console.error('获取学生列表失败:', error)
    ElMessage.error(error.message || '获取学生列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索学生
const searchStudents = () => {
  studentPagination.currentPage = 1
  loadStudentList()
}

// 重置搜索
const resetStudentSearch = () => {
  studentSearchForm.keyword = ''
  studentSearchForm.gradeLevel = ''
  studentPagination.currentPage = 1
  loadStudentList()
}

// 添加学生
const handleAddStudent = () => {
  studentDialogTitle.value = '添加学生'
  Object.assign(studentForm, {
    id: 0,
    realName: '',
    gradeLevel: '',
    subjectsInterested: '',
    learningGoals: '',
    preferredTeachingStyle: '',
    budgetRange: ''
  })
  studentDialogVisible.value = true
}

// 编辑学生
const handleEditStudent = (student: any) => {
  studentDialogTitle.value = '编辑学生'
  Object.assign(studentForm, student)
  studentDialogVisible.value = true
}

// 保存学生
const saveStudent = async () => {
  try {
    loading.value = true
    const studentData = {
      realName: studentForm.realName,
      gradeLevel: studentForm.gradeLevel,
      subjectsInterested: studentForm.subjectsInterested,
      learningGoals: studentForm.learningGoals,
      preferredTeachingStyle: studentForm.preferredTeachingStyle,
      budgetRange: studentForm.budgetRange
    }

    let result
    if (studentForm.id === 0) {
      result = await studentAPI.create(studentData)
    } else {
      result = await studentAPI.update(studentForm.id, studentData)
    }

    if (result.success) {
      ElMessage.success(studentForm.id === 0 ? '添加成功' : '更新成功')
      studentDialogVisible.value = false
      await loadStudentList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('保存学生失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 删除学生
const handleDeleteStudent = async (student: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除学生"${student.realName}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const result = await studentAPI.delete(student.id)
    if (result.success) {
      ElMessage.success('删除成功')
      await loadStudentList()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除学生失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 分页处理
const handleStudentPageChange = (page: number) => {
  studentPagination.currentPage = page
  loadStudentList()
}

const handleStudentSizeChange = (size: number) => {
  studentPagination.pageSize = size
  studentPagination.currentPage = 1
  loadStudentList()
}

onMounted(() => {
  loadStudentList()
})
</script>

<template>
  <div class="student-management">
    <div class="header">
      <h3>学生管理</h3>
      <el-button type="primary" :icon="Plus" @click="handleAddStudent">
        添加学生
      </el-button>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="studentSearchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="studentSearchForm.keyword"
            placeholder="搜索学生姓名"
            :prefix-icon="Search"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="studentSearchForm.gradeLevel" placeholder="选择年级" clearable style="width: 150px">
            <el-option label="小学" value="小学" />
            <el-option label="初中" value="初中" />
            <el-option label="高中" value="高中" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="searchStudents">搜索</el-button>
          <el-button :icon="Refresh" @click="resetStudentSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 学生表格 -->
    <el-table :data="studentList" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="gradeLevel" label="年级" width="100" />
      <el-table-column prop="subjectsInterested" label="感兴趣科目" min-width="150" show-overflow-tooltip />
      <el-table-column prop="learningGoals" label="学习目标" min-width="200" show-overflow-tooltip />
      <el-table-column prop="preferredTeachingStyle" label="偏好教学方式" min-width="150" show-overflow-tooltip />
      <el-table-column prop="budgetRange" label="预算范围" width="120" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click="handleEditStudent(row)">编辑</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="handleDeleteStudent(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        :current-page="studentPagination.currentPage"
        :page-size="studentPagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="studentPagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleStudentSizeChange"
        @current-change="handleStudentPageChange"
      />
    </div>

    <!-- 添加/编辑学生对话框 -->
    <el-dialog
      v-model="studentDialogVisible"
      :title="studentDialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="studentForm" :rules="studentRules" label-width="120px">
        <el-form-item label="学生姓名" prop="realName">
          <el-input v-model="studentForm.realName" placeholder="请输入学生姓名" />
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="studentForm.gradeLevel" placeholder="请选择年级" style="width: 100%">
            <el-option label="小学" value="小学" />
            <el-option label="初中" value="初中" />
            <el-option label="高中" value="高中" />
          </el-select>
        </el-form-item>
        <el-form-item label="感兴趣科目">
          <el-input v-model="studentForm.subjectsInterested" placeholder="请输入感兴趣的科目" />
        </el-form-item>
        <el-form-item label="学习目标">
          <el-input
            v-model="studentForm.learningGoals"
            type="textarea"
            placeholder="请输入学习目标"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="偏好教学方式">
          <el-input v-model="studentForm.preferredTeachingStyle" placeholder="请输入偏好的教学方式" />
        </el-form-item>
        <el-form-item label="预算范围">
          <el-input v-model="studentForm.budgetRange" placeholder="请输入预算范围" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="studentDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveStudent" :loading="loading">
            {{ studentForm.id === 0 ? '添加' : '更新' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h3 {
  margin: 0;
  color: #303133;
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
</style>
