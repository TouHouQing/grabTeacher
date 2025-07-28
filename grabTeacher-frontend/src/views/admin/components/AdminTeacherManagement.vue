<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, Check, Close } from '@element-plus/icons-vue'
import { teacherAPI } from '../../../utils/api'
import { getApiBaseUrl } from '../../../utils/env'
import WideTimeSlotSelector from '../../../components/WideTimeSlotSelector.vue'

// 时间段接口
interface TimeSlot {
  weekday: number
  timeSlots: string[]
}

// 教师列表
const teacherList = ref([])
const loading = ref(false)

// 科目列表
const subjects = ref<{id: number, name: string}[]>([])

// 搜索表单
const teacherSearchForm = reactive({
  keyword: '',
  subject: '',
  gender: '',
  isVerified: null as boolean | null
})

// 分页信息
const teacherPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 对话框
const teacherDialogVisible = ref(false)
const teacherDialogTitle = ref('')
const teacherForm = reactive({
  id: 0,
  realName: '',
  educationBackground: '',
  teachingExperience: 0,
  specialties: '',
  subjectIds: [] as number[],
  hourlyRate: 0,
  introduction: '',
  videoIntroUrl: '',
  gender: '不愿透露',
  isVerified: false
})

// 可上课时间
const availableTimeSlots = ref<TimeSlot[]>([])

// 表单验证规则
const teacherRules = {
  realName: [
    { required: true, message: '请输入教师姓名', trigger: 'blur' }
  ]
}

// 性别选项
const genderOptions = [
  { label: '不愿透露', value: '不愿透露' },
  { label: '男', value: '男' },
  { label: '女', value: '女' }
]

// 获取科目列表
const fetchSubjects = async () => {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/public/subjects/active`)
    const result = await response.json()
    if (result.success) {
      subjects.value = result.data
    }
  } catch (error) {
    console.error('获取科目列表失败:', error)
  }
}

// 获取教师科目名称
const getTeacherSubjects = async (teacherId: number) => {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/admin/teachers/${teacherId}/subjects`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    if (result.success && result.data) {
      const subjectNames = result.data.map((subjectId: number) => {
        const subject = subjects.value.find(s => s.id === subjectId)
        return subject ? subject.name : ''
      }).filter(name => name)
      return subjectNames.join(', ')
    }
  } catch (error) {
    console.error('获取教师科目失败:', error)
  }
  return ''
}

// 获取教师列表
const loadTeacherList = async () => {
  try {
    loading.value = true
    const params = {
      page: teacherPagination.currentPage,
      size: teacherPagination.pageSize,
      keyword: teacherSearchForm.keyword || undefined,
      subject: teacherSearchForm.subject || undefined,
      gender: teacherSearchForm.gender || undefined,
      isVerified: teacherSearchForm.isVerified
    }

    const result = await teacherAPI.getList(params)

    if (result.success && result.data) {
      let teachers = []
      if (Array.isArray(result.data)) {
        teachers = result.data
        teacherPagination.total = result.data.length
      } else if (result.data.records) {
        teachers = result.data.records
        teacherPagination.total = result.data.total || result.data.records.length
      }

      // 为每个教师获取科目信息
      for (const teacher of teachers) {
        teacher.subjects = await getTeacherSubjects(teacher.id)
      }

      teacherList.value = teachers
    }
  } catch (error: any) {
    console.error('获取教师列表失败:', error)
    ElMessage.error(error.message || '获取教师列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索教师
const searchTeachers = () => {
  teacherPagination.currentPage = 1
  loadTeacherList()
}

// 重置搜索
const resetTeacherSearch = () => {
  teacherSearchForm.keyword = ''
  teacherSearchForm.subject = ''
  teacherSearchForm.gender = ''
  teacherSearchForm.isVerified = null
  teacherPagination.currentPage = 1
  loadTeacherList()
}

// 添加教师
const handleAddTeacher = () => {
  teacherDialogTitle.value = '添加教师'
  Object.assign(teacherForm, {
    id: 0,
    realName: '',
    educationBackground: '',
    teachingExperience: 0,
    specialties: '',
    subjectIds: [],
    hourlyRate: 0,
    introduction: '',
    videoIntroUrl: '',
    gender: '不愿透露',
    isVerified: false
  })
  availableTimeSlots.value = []
  teacherDialogVisible.value = true
}

// 编辑教师
const handleEditTeacher = async (teacher: any) => {
  teacherDialogTitle.value = '编辑教师'
  Object.assign(teacherForm, teacher)

  // 获取教师的科目ID列表
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/admin/teachers/${teacher.id}/subjects`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    if (result.success) {
      teacherForm.subjectIds = result.data || []
    }
  } catch (error) {
    console.error('获取教师科目失败:', error)
    teacherForm.subjectIds = []
  }

  // 获取教师的可上课时间
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/available-time/teacher/${teacher.id}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    const result = await response.json()
    if (result.success && result.data && result.data.availableTimeSlots) {
      availableTimeSlots.value = [...result.data.availableTimeSlots]
      console.log('加载教师可上课时间成功:', result.data.availableTimeSlots)
    } else {
      // 如果没有设置可上课时间，清空数组（表示所有时间都可以）
      availableTimeSlots.value = []
      console.log('教师未设置可上课时间，默认所有时间可用')
    }
  } catch (error) {
    console.error('获取教师可上课时间失败:', error)
    availableTimeSlots.value = []
  }

  teacherDialogVisible.value = true
}

// 保存教师
const saveTeacher = async () => {
  try {
    loading.value = true
    const teacherData = {
      realName: teacherForm.realName,
      educationBackground: teacherForm.educationBackground,
      teachingExperience: teacherForm.teachingExperience,
      specialties: teacherForm.specialties,
      subjectIds: teacherForm.subjectIds,
      hourlyRate: teacherForm.hourlyRate,
      introduction: teacherForm.introduction,
      videoIntroUrl: teacherForm.videoIntroUrl,
      gender: teacherForm.gender,
      isVerified: teacherForm.isVerified,
      availableTimeSlots: availableTimeSlots.value
    }

    let result: any
    if (teacherForm.id === 0) {
      result = await teacherAPI.create(teacherData)
    } else {
      result = await teacherAPI.update(teacherForm.id, teacherData)
    }

    if (result.success) {
      ElMessage.success(teacherForm.id === 0 ? '添加成功' : '更新成功')
      teacherDialogVisible.value = false
      await loadTeacherList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('保存教师失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 删除教师
const handleDeleteTeacher = async (teacher: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除教师"${teacher.realName}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const result = await teacherAPI.delete(teacher.id)
    if (result.success) {
      ElMessage.success('删除成功')
      await loadTeacherList()
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除教师失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 切换认证状态
const toggleVerification = async (teacher: any) => {
  try {
    const result = await teacherAPI.updateVerification(teacher.id, !teacher.isVerified)
    if (result.success) {
      ElMessage.success(teacher.isVerified ? '已取消认证' : '认证成功')
      await loadTeacherList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('更新认证状态失败:', error)
    ElMessage.error(error.message || '操作失败')
  }
}

// 分页处理
const handleTeacherPageChange = (page: number) => {
  teacherPagination.currentPage = page
  loadTeacherList()
}

const handleTeacherSizeChange = (size: number) => {
  teacherPagination.pageSize = size
  teacherPagination.currentPage = 1
  loadTeacherList()
}

onMounted(async () => {
  await fetchSubjects()
  loadTeacherList()
})
</script>

<template>
  <div class="teacher-management">
    <div class="header">
      <h3>教师管理</h3>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="teacherSearchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="teacherSearchForm.keyword"
            placeholder="搜索教师姓名"
            :prefix-icon="Search"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="科目">
          <el-input
            v-model="teacherSearchForm.subject"
            placeholder="搜索科目"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="teacherSearchForm.gender" placeholder="选择性别" clearable style="width: 120px">
            <el-option
              v-for="option in genderOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="认证状态">
          <el-select v-model="teacherSearchForm.isVerified" placeholder="选择认证状态" clearable style="width: 120px">
            <el-option label="已认证" :value="true" />
            <el-option label="未认证" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="searchTeachers">搜索</el-button>
          <el-button :icon="Refresh" @click="resetTeacherSearch">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAddTeacher">
            添加教师
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 教师表格 -->
    <el-table :data="teacherList" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="educationBackground" label="教育背景" width="150" show-overflow-tooltip />
      <el-table-column prop="teachingExperience" label="教学经验" width="100">
        <template #default="{ row }">
          {{ row.teachingExperience }}年
        </template>
      </el-table-column>
      <el-table-column prop="subjects" label="教学科目" min-width="150" show-overflow-tooltip />
      <el-table-column prop="hourlyRate" label="时薪" width="100">
        <template #default="{ row }">
          ¥{{ row.hourlyRate }}
        </template>
      </el-table-column>
      <el-table-column prop="isVerified" label="认证状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isVerified ? 'success' : 'warning'">
            {{ row.isVerified ? '已认证' : '未认证' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right" align="center">
        <template #default="{ row }">
          <div class="operation-buttons">
            <el-button size="small" :icon="Edit" @click="handleEditTeacher(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.isVerified ? 'warning' : 'success'"
              :icon="row.isVerified ? Close : Check"
              @click="toggleVerification(row)"
            >
              {{ row.isVerified ? '取消认证' : '认证' }}
            </el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDeleteTeacher(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        :current-page="teacherPagination.currentPage"
        :page-size="teacherPagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="teacherPagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleTeacherSizeChange"
        @current-change="handleTeacherPageChange"
      />
    </div>

    <!-- 添加/编辑教师对话框 -->
    <el-dialog
      v-model="teacherDialogVisible"
      :title="teacherDialogTitle"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="teacherForm" :rules="teacherRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="教师姓名" prop="realName">
              <el-input v-model="teacherForm.realName" placeholder="请输入教师姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="teacherForm.gender">
                <el-radio
                  v-for="option in genderOptions"
                  :key="option.value"
                  :label="option.value"
                >
                  {{ option.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="教学经验">
              <el-input-number v-model="teacherForm.teachingExperience" :min="0" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时薪">
              <el-input-number v-model="teacherForm.hourlyRate" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="教育背景">
          <el-input v-model="teacherForm.educationBackground" placeholder="请输入教育背景" />
        </el-form-item>
        <el-form-item label="教学科目">
          <el-select
            v-model="teacherForm.subjectIds"
            multiple
            placeholder="请选择教学科目"
            style="width: 100%"
          >
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="专业特长">
          <el-input v-model="teacherForm.specialties" placeholder="请输入专业特长" />
        </el-form-item>
        <el-form-item label="个人介绍">
          <el-input
            v-model="teacherForm.introduction"
            type="textarea"
            placeholder="请输入个人介绍"
            :rows="4"
          />
        </el-form-item>
        <el-form-item label="视频介绍URL">
          <el-input v-model="teacherForm.videoIntroUrl" placeholder="请输入视频介绍链接" />
        </el-form-item>
        <el-form-item label="认证状态">
          <el-switch v-model="teacherForm.isVerified" active-text="已认证" inactive-text="未认证" />
        </el-form-item>
        <el-form-item label="可上课时间">
          <WideTimeSlotSelector
            v-model="availableTimeSlots"
            title="设置可上课时间"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="teacherDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveTeacher" :loading="loading">
            {{ teacherForm.id === 0 ? '添加' : '更新' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.teacher-management {
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
  min-width: 60px;
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
