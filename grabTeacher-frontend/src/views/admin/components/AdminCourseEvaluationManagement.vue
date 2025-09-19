<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiRequest } from '../../../utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Edit, Delete } from '@element-plus/icons-vue'

type EvalVO = {
  id: number
  teacherId: number
  studentId: number
  courseId: number
  teacherName?: string
  studentName?: string
  courseName?: string
  studentComment?: string
  rating?: number
  createdAt?: string
  updatedAt?: string
  isFeatured?: boolean
}

const loading = ref(false)
const list = ref<EvalVO[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filters = ref<{ teacherId?: number; studentId?: number; courseId?: number; minRating?: number; teacherName?: string; studentName?: string; courseName?: string }>({})

const fetchList = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    params.append('page', String(currentPage.value))
    params.append('size', String(pageSize.value))
    Object.entries(filters.value).forEach(([k, v]) => {
      if (v !== undefined && v !== null) params.append(k, String(v))
    })
    const res = await apiRequest(`/api/admin/course-evaluations?${params.toString()}`)
    if (res?.success) {
      list.value = res.data.records || []
      total.value = Number(res.data.total || 0)
      currentPage.value = Number(res.data.current || 1)
      pageSize.value = Number(res.data.size || 10)
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)

const dialogVisible = ref(false)
const editing = ref(false)
const form = ref<Partial<EvalVO>>({})
const formRef = ref()

// 表单校验规则（必填 + 长度）
const rules = {
  teacherName: [
    { required: true, message: '教师姓名为必填项', trigger: 'blur' }
  ],
  studentName: [
    { required: true, message: '学生姓名为必填项', trigger: 'blur' }
  ],
  courseName: [
    { required: true, message: '课程名称为必填项', trigger: 'blur' }
  ],
  rating: [
    { required: true, message: '评分为必填项', trigger: 'change' }
  ],
  studentComment: [
    { required: true, message: '评价内容为必填项', trigger: 'blur' },
    { min: 1, max: 255, message: '评价内容长度需在1-255字符内', trigger: 'blur' }
  ]
} as const

const openCreate = () => {
  editing.value = false
  form.value = { teacherId: undefined, studentId: undefined, courseId: undefined, teacherName: '', studentName: '', courseName: '', rating: 5, studentComment: '', isFeatured: false }
  dialogVisible.value = true
}

const openEdit = (row: EvalVO) => {
  editing.value = true
  form.value = { ...row }
  // 规范化必填字段，避免为空导致后端校验失败
  form.value.teacherName = (row.teacherName ?? '').toString()
  form.value.studentName = (row.studentName ?? '').toString()
  form.value.courseName = (row.courseName ?? '').toString()
  form.value.studentComment = (row.studentComment ?? '').toString()
  if (row.rating === undefined || row.rating === null || Number.isNaN(Number(row.rating))) {
    form.value.rating = 5
  } else {
    const num = Number(row.rating)
    form.value.rating = Number(num.toFixed(2))
  }
  dialogVisible.value = true
}

const handleDelete = async (row: EvalVO) => {
  try {
    await ElMessageBox.confirm(`确定要删除该评价（ID: ${row.id}）吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await apiRequest(`/api/admin/course-evaluations/${row.id}`, { method: 'DELETE' })
    if (res?.success) {
      ElMessage.success('删除成功')
      fetchList()
    }
  } catch {
    // 用户取消不提示错误
  }
}

const handleSubmit = async () => {
  // 先进行前端校验
  const valid = await formRef.value?.validate?.()
  if (valid === false) return
  const payload = {
    teacherId: form.value.teacherId,
    studentId: form.value.studentId,
    courseId: form.value.courseId,
    teacherName: form.value.teacherName,
    studentName: form.value.studentName,
    courseName: form.value.courseName,
    rating: form.value.rating,
    studentComment: form.value.studentComment,
    isFeatured: !!form.value.isFeatured
  }
  if (editing.value && form.value.id) {
    const res = await apiRequest(`/api/admin/course-evaluations/${form.value.id}`, { method: 'PUT', data: payload })
    if (res?.success) ElMessage.success('更新成功')
  } else {
    const res = await apiRequest('/api/admin/course-evaluations', { method: 'POST', data: payload })
    if (res?.success) ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchList()
}

// 切换精选
const toggleFeatured = async (row: EvalVO) => {
  try {
    const next = !!row.isFeatured
    const res = await apiRequest(`/api/admin/course-evaluations/${row.id}/featured?isFeatured=${next}`, { method: 'PATCH' })
    if (res?.success) {
      ElMessage.success(next ? '已设为精选' : '已取消精选')
    } else {
      throw new Error(res?.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
    // 回滚UI
    row.isFeatured = !row.isFeatured
  }
}

const handlePage = (p: number) => { currentPage.value = p; fetchList() }
const handleSize = (s: number) => { pageSize.value = s; currentPage.value = 1; fetchList() }

// 重置仅姓名类筛选
const resetFilters = () => {
  filters.value.teacherName = ''
  filters.value.studentName = ''
  filters.value.courseName = ''
  currentPage.value = 1
  fetchList()
}

// 搜索按钮点击：重置到第1页并查询
const handleSearch = () => {
  currentPage.value = 1
  fetchList()
}

// 失焦自动查询并回填姓名
const fetchTeacherName = async () => {
  if (!form.value.teacherId) return
  try {
    const res = await apiRequest(`/api/admin/teachers/${form.value.teacherId}`)
    if (res?.success && res.data) {
      // 后端教师详情里一般有 realName/username，这里优先 realName
      form.value.teacherName = res.data.realName || res.data.username || form.value.teacherName
    } else {
      ElMessage.warning('未查询到该教师ID的信息，可自定义填写姓名')
    }
  } catch {
    ElMessage.warning('未查询到该教师ID的信息，可自定义填写姓名')
  }
}

const fetchStudentName = async () => {
  if (!form.value.studentId) return
  try {
    const res = await apiRequest(`/api/admin/students/${form.value.studentId}`)
    if (res?.success && res.data) {
      form.value.studentName = res.data.realName || res.data.username || form.value.studentName
    } else {
      ElMessage.warning('未查询到该学生ID的信息，可自定义填写姓名')
    }
  } catch {
    ElMessage.warning('未查询到该学生ID的信息，可自定义填写姓名')
  }
}

const fetchCourseName = async () => {
  if (!form.value.courseId) return
  try {
    const res = await apiRequest(`/api/courses/${form.value.courseId}`)
    if (res?.success && res.data) {
      form.value.courseName = res.data.title || form.value.courseName
    } else {
      ElMessage.warning('未查询到该课程ID的信息，可自定义填写名称')
    }
  } catch {
    ElMessage.warning('未查询到该课程ID的信息，可自定义填写名称')
  }
}

// 时间格式化：YYYY-MM-DD HH:mm:ss
const two = (n: number) => (n < 10 ? `0${n}` : String(n))
const formatDateTime = (iso?: string) => {
  if (!iso) return '-'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const y = d.getFullYear()
  const m = two(d.getMonth() + 1)
  const day = two(d.getDate())
  const hh = two(d.getHours())
  const mm = two(d.getMinutes())
  const ss = two(d.getSeconds())
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`
}

// 详情弹窗
const detailVisible = ref(false)
const detailRow = ref<Partial<EvalVO>>({})
const openDetail = (row: EvalVO) => {
  detailRow.value = { ...row }
  detailVisible.value = true
}

</script>

<template>
  <div class="evaluation-management">
    <div class="header">
      <h3>评价管理</h3>
    </div>

    <!-- 搜索区域（仅姓名类条件） -->
    <el-card class="search-card" shadow="never">
      <el-form inline>
        <el-form-item label="学生名称">
          <el-input v-model="filters.studentName" placeholder="学生名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="filters.courseName" placeholder="课程名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="教师名称">
          <el-input v-model="filters.teacherName" placeholder="教师名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="success" @click="openCreate">新建评价</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="list" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="50" />
      <el-table-column prop="rating" label="评分" width="80">
        <template #default="{ row }">
          <span>{{ Number(row.rating || 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="teacherName" label="教师姓名" width="130" />
      <el-table-column prop="studentName" label="学生姓名" width="130" />
      <el-table-column prop="courseName" label="课程名称" width="160" />
      <el-table-column label="精选" width="90" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.isFeatured" @change="() => toggleFeatured(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="学生评价内容" min-width="250" show-overflow-tooltip>
        <template #default="{ row }">{{ row.studentComment }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right" align="center">
        <template #default="{ row }">
          <div class="operation-buttons">
            <el-button size="small" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button size="small" :icon="Edit" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" :icon="Delete" type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSize"
        @current-change="handlePage"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑评价' : '新建评价'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="教师ID">
          <el-input v-model.number="form.teacherId" @blur="fetchTeacherName" />
        </el-form-item>
        <el-form-item label="教师姓名" prop="teacherName">
          <el-input v-model="form.teacherName" />
        </el-form-item>
        <el-form-item label="学生ID">
          <el-input v-model.number="form.studentId" @blur="fetchStudentName" />
        </el-form-item>
        <el-form-item label="学生姓名" prop="studentName">
          <el-input v-model="form.studentName" />
        </el-form-item>
        <el-form-item label="课程ID">
          <el-input v-model.number="form.courseId" @blur="fetchCourseName" />
        </el-form-item>
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" />
        </el-form-item>
        <el-form-item label="评分" prop="rating">
          <el-input-number v-model="form.rating" :min="0" :max="5" :step="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评价内容" prop="studentComment">
          <el-input v-model="form.studentComment" type="textarea" :rows="4" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="是否精选">
          <el-switch v-model="form.isFeatured" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="评价详情" width="840px">
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">评价ID</div>
          <div class="detail-value">{{ detailRow.id ?? '-' }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">教师ID</div>
          <div class="detail-value">{{ detailRow.teacherId ?? '-' }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">学生ID</div>
          <div class="detail-value">{{ detailRow.studentId ?? '-' }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">课程ID</div>
          <div class="detail-value">{{ detailRow.courseId ?? '-' }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">教师姓名</div>
          <div class="detail-value">{{ detailRow.teacherName || '-' }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">学生姓名</div>
          <div class="detail-value">{{ detailRow.studentName || '-' }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">课程名称</div>
          <div class="detail-value">{{ detailRow.courseName || '-' }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">评分</div>
          <div class="detail-value">{{ detailRow.rating ? Number(detailRow.rating).toFixed(2) : '-' }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">创建时间</div>
          <div class="detail-value">{{ formatDateTime(detailRow.createdAt) }}</div>
        </div>
        <div class="detail-item">
          <div class="detail-label">更新时间</div>
          <div class="detail-value">{{ formatDateTime(detailRow.updatedAt) }}</div>
        </div>
        <div class="detail-item detail-item--full">
          <div class="detail-label">评价内容</div>
          <div class="detail-value detail-value--multiline">{{ detailRow.studentComment || '-' }}</div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>

</template>

<style scoped>
.evaluation-management {
  padding: 20px;
  padding-top: 5px;
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

/* 单行高度与溢出控制 */
:deep(.el-table .cell) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

/* 详情横向布局样式 */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 24px;
}
.detail-item {
  display: flex;
  align-items: center;
}
.detail-item--full {
  grid-column: 1 / -1;
}
.detail-label {
  width: 96px;
  color: #606266;
  font-weight: 600;
  flex-shrink: 0;
}
.detail-value {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-value--multiline {
  white-space: pre-wrap;
  overflow: auto;
  text-overflow: initial;
  line-height: 1.6;
}
</style>


