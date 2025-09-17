<template>
  <div class="class-records">
    <div class="filter-section">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="年份">
          <el-select v-model="filterForm.year" placeholder="全部年份" clearable style="width: 120px">
            <el-option
              v-for="year in yearOptions"
              :key="year"
              :label="year"
              :value="year"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="月份">
          <el-select v-model="filterForm.month" placeholder="全部月份" clearable style="width: 120px">
            <el-option
              v-for="month in monthOptions"
              :key="month.value"
              :label="month.label"
              :value="month.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学生姓名">
          <el-input
            v-model="filterForm.studentName"
            placeholder="请输入学生姓名"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input
            v-model="filterForm.courseName"
            placeholder="请输入课程名称"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-wrap">
      <el-table :data="list" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="scheduledDate" label="上课日期" width="120" align="center" />
        <el-table-column label="上课时间" width="140" align="center">
          <template #default="{ row }">
            {{ formatTimeRange(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column label="课程类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getCourseTypeTag(row.courseType)" size="small">
              {{ getCourseTypeText(row.courseType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="教师姓名" width="120" />
        <el-table-column label="第几次课" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.sessionNumber">第{{ row.sessionNumber }}次</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="试听" width="60" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isTrial" type="warning" size="small">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="80" align="center">
          <template #default="{ row }">
            {{ row.durationMinutes }}分钟
          </template>
        </el-table-column>
        <el-table-column label="时薪" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.isTrial || row.courseType !== 'one_on_one' || !row.teacherHourlyRate">-</span>
            <span v-else>{{ row.teacherHourlyRate }} M豆/小时</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.courseType === 'one_on_one' && !row.isTrial"
              type="primary"
              size="small"
              @click="openGradeDialog(row)"
            >录入成绩</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>

      </el-table>
    </div>
    <el-dialog v-model="gradeDialogVisible" title="录入成绩" width="500px">
      <div>
        <el-form label-width="100px">
          <el-form-item label="学生">
            <span>{{ editingRow?.studentName || '-' }}</span>
          </el-form-item>
          <el-form-item label="课程">
            <span>{{ editingRow?.courseName || '-' }}</span>
          </el-form-item>
          <el-form-item label="成绩">
            <el-input-number v-model="gradeForm.score" :min="0" :max="100" :step="1" controls-position="right" style="width: 180px" />
          </el-form-item>
          <el-form-item label="课后评语">
            <el-input v-model="gradeForm.teacherComment" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="可填写对本节课的评价与建议" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="gradeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingGrade" @click="submitGrade">{{ gradeMode === 'create' ? '提交' : '更新' }}</el-button>
      </template>
    </el-dialog>

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
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teacherAPI, lessonGradeAPI } from '../../../utils/api'

// 接口定义
interface ClassRecord {
  scheduleId: number
  scheduledDate: string
  startTime: string
  endTime: string
  courseName: string
  studentName: string
  studentId?: number
  courseType: string
  teacherName: string
  sessionNumber?: number
  isTrial: boolean
  teacherNotes?: string
  studentFeedback?: string
  durationMinutes: number
  teacherHourlyRate?: number
}

const filterForm = reactive({
  year: undefined as number | undefined,
  month: undefined as number | undefined,
  studentName: '',
  courseName: ''
})
const list = ref<ClassRecord[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 20, total: 0 })

// 年份选项（当前年份前后5年）
const currentYear = new Date().getFullYear()
const yearOptions = computed(() => {
  const years = []
  for (let i = currentYear - 5; i <= currentYear + 1; i++) {
    years.push(i)
  }
  return years
})

// 月份选项
const monthOptions = [
  { label: '1月', value: 1 },
  { label: '2月', value: 2 },
  { label: '3月', value: 3 },
  { label: '4月', value: 4 },
  { label: '5月', value: 5 },
  { label: '6月', value: 6 },
  { label: '7月', value: 7 },
  { label: '8月', value: 8 },
  { label: '9月', value: 9 },
  { label: '10月', value: 10 },
  { label: '11月', value: 11 },
  { label: '12月', value: 12 }
]

const loadList = async () => {
  try {
    loading.value = true
    const res = await teacherAPI.getClassRecords({
      page: pagination.page,
      size: pagination.size,
      year: filterForm.year,
      month: filterForm.month,
      studentName: filterForm.studentName || undefined,
      courseName: filterForm.courseName || undefined
    })
    if (res.success) {
      list.value = res.data.records || []
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取上课记录失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取上课记录失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; loadList() }
const handleReset = () => {
  filterForm.year = undefined
  filterForm.month = undefined
  filterForm.studentName = ''
  filterForm.courseName = ''
  pagination.page = 1
  loadList()
}


// 
const gradeDialogVisible = ref(false)
const submittingGrade = ref(false)
const gradeForm = reactive<{ score: number | null; teacherComment: string }>({
  score: null,
  teacherComment: ''
})
const gradeMode = ref<'create' | 'update'>('create')
const editingRow = ref<ClassRecord | null>(null)
const editingGradeId = ref<number | null>(null)

const openGradeDialog = async (row: ClassRecord) => {
  try {
    if (!row.studentId) {
      ElMessage.warning('缺少学生ID，无法录入成绩')
      return
    }
    editingRow.value = row
    gradeForm.score = null
    gradeForm.teacherComment = ''
    gradeMode.value = 'create'
    editingGradeId.value = null

    const res = await lessonGradeAPI.getByScheduleAndStudent(row.scheduleId, row.studentId)
    if (res?.success && res.data) {
      gradeMode.value = 'update'
      editingGradeId.value = res.data.id
      gradeForm.score = res.data.score ?? null
      gradeForm.teacherComment = res.data.teacherComment || ''
    }

    gradeDialogVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '加载成绩信息失败')
  }
}

const submitGrade = async () => {
  if (!editingRow.value || !editingRow.value.studentId) return
  if (gradeForm.score == null) {
    ElMessage.warning('请填写成绩（0-100）')
    return
  }
  try {
    submittingGrade.value = true
    if (gradeMode.value === 'create') {
      await ElMessageBox.confirm(`确认为学生【${editingRow.value.studentName}】录入成绩 ${gradeForm.score} 分？`, '确认', { type: 'warning' })
      const res = await lessonGradeAPI.create({
        scheduleId: editingRow.value.scheduleId,
        studentId: editingRow.value.studentId,
        score: Number(gradeForm.score),
        teacherComment: gradeForm.teacherComment || ''
      })
      if (res.success) {
        ElMessage.success('成绩录入成功')
        gradeDialogVisible.value = false
      } else {
        ElMessage.error(res.message || '成绩录入失败')
      }
    } else {
      await ElMessageBox.confirm('确定更新该成绩吗？', '确认', { type: 'warning' })
      const res = await lessonGradeAPI.update({
        id: editingGradeId.value!,
        score: Number(gradeForm.score),
        teacherComment: gradeForm.teacherComment || ''
      })
      if (res.success) {
        ElMessage.success('成绩更新成功')
        gradeDialogVisible.value = false
      } else {
        ElMessage.error(res.message || '成绩更新失败')
      }
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '提交失败')
    }
  } finally {
    submittingGrade.value = false
  }
}

const handleSizeChange = (s: number) => { pagination.size = s; pagination.page = 1; loadList() }
const handleCurrentChange = (p: number) => { pagination.page = p; loadList() }

// 格式化时间范围
const formatTimeRange = (startTime: string, endTime: string) => {
  if (!startTime || !endTime) return '-'
  return `${startTime}-${endTime}`
}

// 获取课程类型标签样式
const getCourseTypeTag = (courseType: string) => {
  switch (courseType) {
    case 'one_on_one':
      return 'primary'
    case 'large_class':
      return 'success'
    default:
      return 'info'
  }
}

// 获取课程类型文本
const getCourseTypeText = (courseType: string) => {
  switch (courseType) {
    case 'one_on_one':
      return '一对一'
    case 'large_class':
      return '小班课'
    default:
      return courseType || '-'
  }
}

onMounted(loadList)
</script>

<style scoped>
.class-records {
  padding: 20px;
}

.filter-section {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.filter-form .el-form-item {
  margin-bottom: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .class-records {
    padding: 16px;
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-form .el-form-item {
    width: 100%;
  }

  .filter-form .el-form-item .el-select,
  .filter-form .el-form-item .el-input {
    width: 100%;
  }
}

.table-wrap{ width:100%; overflow-x:auto; }
.table-wrap :deep(table){ min-width:900px; }
@media (max-width:768px){
  :deep(.el-dialog){ width:100vw!important; max-width:100vw!important; margin:0!important; }
  :deep(.el-dialog__body){ padding:12px; }
  :deep(.el-dialog .el-form .el-form-item__label){ float:none; display:block; padding-bottom:4px; }
  :deep(.el-dialog .el-form .el-form-item__content){ margin-left:0!important; }
}
</style>


