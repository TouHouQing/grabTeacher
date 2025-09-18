<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, Check, Close, Calendar } from '@element-plus/icons-vue'
import { teacherAPI, fileAPI, teacherLevelAPI, teachingLocationAPI } from '../../../utils/api'
import { getApiBaseUrl } from '../../../utils/env'
import AvatarUploader from '../../../components/AvatarUploader.vue'
import TeacherAvailabilityEditor from '../../../components/scheduler/TeacherAvailabilityEditor.vue'

// 学历枚举与归一化
const EDUCATION_ALLOWED = ['专科及以下', '本科', '硕士', '博士'] as const
const normalizeEducation = (value: string): string => {
  if (!value) return ''
  if (EDUCATION_ALLOWED.includes(value as any)) return value
  if (value.includes('硕士')) return '硕士'
  if (value.includes('博士')) return '博士'
  if (value.includes('本科')) return '本科'
  if (value.includes('专科')) return '专科及以下'
  return ''
}

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
const availabilityDialogVisible = ref(false)
const LEVEL_OPTIONS = ref<{ label: string; value: string }[]>([])

const fetchTeacherLevels = async () => {
  try {
    const res = await teacherLevelAPI.list()
    if (res.success && Array.isArray(res.data)) {
      // 仅展示激活状态的级别，避免选择后被后端校验拒绝
      LEVEL_OPTIONS.value = res.data
        .filter((it: any) => it.isActive)
        .map((it: any) => ({ label: it.name, value: it.name }))
    }
  } catch (e) {
    // ignore
  }
}
const TEACHING_LOCATION_OPTIONS = ref<{ label: string; value: number }[]>([])

const fetchTeachingLocations = async () => {
  try {
    const res = await teachingLocationAPI.getList({ page: 1, size: 1000, isActive: true })
    // 管理端返回结构：{ data: { locations: [], total, ... } }
    const list = res?.data?.locations || []
    TEACHING_LOCATION_OPTIONS.value = list.map((it: any) => ({ label: it.name, value: it.id }))
  } catch (e) {
    // ignore
  }
}


const teacherForm = reactive({
  id: 0,
  realName: '',
  username: '',
  email: '',
  phone: '',
  password: '', // 添加密码字段
  educationBackground: '',
  teachingExperience: 0,
  specialties: '',
  subjectIds: [] as number[],
  rating: 5.0, // 添加评分字段，默认5.0分
  introduction: '',
  gender: '男',
  level: '王牌',
  isVerified: false, // 表单内部仍使用 isVerified，编辑时从 row.verified 映射
  avatarUrl: '',
  adjustmentTimes: 3,
  // 授课地点：supportsOnline 为线上开关；ID 数组为线下地点
  teachingLocationIds: [] as number[],
  supportsOnline: true,
  hourlyRateText: ''
})
const _teacherAvatarFile = ref<File | null>(null)

const handleTeacherAvatarFileSelected = (file: File | null) => {
  _teacherAvatarFile.value = file
}

// 上传教师头像到OSS（仅在保存时调用）
const uploadTeacherAvatarIfNeeded = async (): Promise<string | null> => {
  if (!_teacherAvatarFile.value) return null
  // 传递目标教师ID，如果是新建教师则为undefined
  const targetId = teacherForm.id !== 0 ? teacherForm.id : undefined
  return await fileAPI.presignAndPut(_teacherAvatarFile.value, 'admin/teacher/avatar', targetId)
}



// 可上课时间
const availableTimeSlots = ref<TimeSlot[]>([])

// 检查用户名是否可用（仅在新增时检查）
const checkUsernameAvailable = async (username: string): Promise<boolean> => {
  if (!username || teacherForm.id !== 0) return true // 编辑时不检查
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/auth/check-username?username=${encodeURIComponent(username)}`)
    const result = await response.json()
    return result.success && result.data
  } catch (error) {
    console.error('检查用户名失败:', error)
    return false
  }
}

// 自定义验证器
const validateUsername = async (rule: any, value: string, callback: any) => {
  // 非必填：留空则后端自动生成
  if (!value || !value.trim()) {
    callback()
    return
  }
  if (value.length < 3 || value.length > 50) {
    callback(new Error('用户名长度必须在3-50个字符之间'))
    return
  }
  if (teacherForm.id === 0) { // 仅在新增时检查
    const available = await checkUsernameAvailable(value)
    if (!available) {
      callback(new Error('用户名已被使用，请选择其他用户名'))
      return
    }
  }
  callback()
}

// 校验：至少选择一个科目
const validateSubjectIds = (rule: any, value: any, callback: any) => {
  if (!Array.isArray(value) || value.length === 0) {
    callback(new Error('请选择至少一个教学科目'))
    return
  }
  callback()
}

// 表单验证规则
const teacherRules = {
  realName: [
    { required: true, message: '请输入教师姓名', trigger: 'blur' }
  ],
  username: [
    { validator: validateUsername, trigger: 'blur' }
  ],
  educationBackground: [
    { required: true, message: '请选择学历', trigger: 'change' }
  ],
  level: [
    { required: true, message: '请选择教师级别', trigger: 'change' }
  ],
  subjectIds: [
    { validator: validateSubjectIds, trigger: 'change' }
  ]
}

// 性别选项
const genderOptions = [
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

      // 批量查询科目信息，避免N+1请求
      const ids = teachers.map((t: any) => t.id)
      try {
        const mapResp = await teacherAPI.getSubjectsByTeacherIds(ids)
        const idMap: Record<number, number[]> = (mapResp && mapResp.data) ? mapResp.data : {}
        // 预构建 subjectId->name 的字典
        const nameMap: Record<number, string> = {}
        for (const s of subjects.value) {
          nameMap[s.id] = s.name
        }
        for (const t of teachers) {
          const arr = idMap[t.id] || []
          t.subjects = arr.map((sid: number) => nameMap[sid]).filter(Boolean).join(', ')
        }
      } catch (e) {
        console.warn('批量获取教师科目失败，降级为空', e)
        for (const t of teachers) t.subjects = ''
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
    username: '',
    email: '',
    phone: '',
    educationBackground: '',
    teachingExperience: 0,
    specialties: '',
    subjectIds: [],
    rating: 5.0, // 默认评分5.0分
    introduction: '',
    gender: '男',
    level: '',
    isVerified: false,
    hourlyRateText: '',
    teachingLocationIds: []
  })
  teacherForm.supportsOnline = true
  // 重置头像与本地文件
  teacherForm.avatarUrl = ''
  _teacherAvatarFile.value = null

  availableTimeSlots.value = []
  teacherDialogVisible.value = true
}

// 编辑教师（从后端拉取详情，保证头像等字段完整回显）
const handleEditTeacher = async (teacher: any) => {
  teacherDialogTitle.value = '编辑教师'
  try {
    const detail = await teacherAPI.getById(teacher.id)
    if (detail.success && detail.data) {
      Object.assign(teacherForm, detail.data)
      // 回显教师的统一时薪（varchar），兼容历史 numeric 字段
      ;(teacherForm as any).hourlyRateText = (detail.data as any).hourlyRateText
        || (typeof (detail.data as any).hourlyRate === 'number' ? `${(detail.data as any).hourlyRate}` : '')
      // 统一旧值到新枚举：如“硕士研究生”=>“硕士”，“博士研究生”=>“博士”，“专科”=>“专科及以下”
      teacherForm.educationBackground = normalizeEducation(teacherForm.educationBackground || '')
      teacherForm.level = detail.data.level || '王牌'
      teacherForm.isVerified = !!detail.data.verified
    } else {
      Object.assign(teacherForm, teacher)
      teacherForm.level = teacher.level || '王牌'
      teacherForm.isVerified = !!teacher.verified
    }
  } catch (e) {
    Object.assign(teacherForm, teacher)
    teacherForm.isVerified = !!teacher.verified
  }

  // 回显授课地点（CSV -> ID数组）
  try {
    const csv: string = (typeof (teacherForm as any).teachingLocations === 'string')
      ? (teacherForm as any).teachingLocations
      : (teacher as any)?.teachingLocations || ''
    if (csv && csv.trim()) {
      teacherForm.teachingLocationIds = csv.split(',').map((s: string) => Number(s.trim())).filter((n: number) => !Number.isNaN(n))
    } else {
      teacherForm.teachingLocationIds = []
    }
  } catch (e) {
    // 回显授课地点异常时，降级为空（仅线下为空，线上开关另行回显）
    teacherForm.teachingLocationIds = []
  }
  // 线上授课开关回显（兼容旧数据）
  if (typeof (teacherForm as any).supportsOnline === 'boolean') {
    teacherForm.supportsOnline = !!(teacherForm as any).supportsOnline
  } else {
    teacherForm.supportsOnline = teacherForm.teachingLocationIds.length === 0
  }


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

  // 按月日历管理教师可上课时间：此处不再拉取按星期模板，改由“教师可用时间日历”页面集中管理
  availableTimeSlots.value = []


  // 编辑时清空密码字段，避免显示敏感信息
  teacherForm.password = ''
  teacherDialogVisible.value = true
}


// 保存教师
const saveTeacher = async () => {
  try {
    loading.value = true

    // 基本必填校验（前端快速阻断）
    const normalizedEdu = normalizeEducation(teacherForm.educationBackground || '')
    if (!normalizedEdu) {
      ElMessage.error('请选择学历')
      loading.value = false
      return
    }
    if (!Array.isArray(teacherForm.subjectIds) || teacherForm.subjectIds.length === 0) {
      ElMessage.error('请选择至少一个教学科目')
      loading.value = false
      return
    }
    if (!teacherForm.level || !teacherForm.level.trim()) {
      ElMessage.error('请选择教师级别')
      loading.value = false
      return
    }
    const activeLevelSet = new Set(LEVEL_OPTIONS.value.map(o => o.value))
    if (!activeLevelSet.has(teacherForm.level)) {
      ElMessage.error('请选择有效的教师级别')
      loading.value = false
      return
    }
    // 授课地点：至少选择一个（线上或线下）
    if (!teacherForm.supportsOnline && (!Array.isArray(teacherForm.teachingLocationIds) || teacherForm.teachingLocationIds.length === 0)) {
      ElMessage.error('请至少选择一个授课地点（线上或线下）')
      loading.value = false
      return
    }


    // 组装基础数据（不带头像）
    const baseData: any = {
      realName: teacherForm.realName,
      username: teacherForm.username,
      educationBackground: normalizedEdu,
      teachingExperience: teacherForm.teachingExperience,
      specialties: teacherForm.specialties,
      subjectIds: teacherForm.subjectIds,
      rating: teacherForm.rating, // 包含评分字段
      introduction: teacherForm.introduction,
      gender: teacherForm.gender,
      level: teacherForm.level,
      isVerified: teacherForm.isVerified,
      adjustmentTimes: teacherForm.adjustmentTimes,
      supportsOnline: teacherForm.supportsOnline,
      teachingLocationIds: teacherForm.teachingLocationIds,
      hourlyRateText: teacherForm.hourlyRateText
    }
    // 用户名可选：留空则不提交，由后端按 teacher+userId 自动生成
    if (!teacherForm.username || !teacherForm.username.trim()) {
      delete baseData.username
    }

    // 处理密码：新建时使用默认密码，编辑时如果填写了密码则更新
    const wasNew = teacherForm.id === 0
    if (wasNew) {
      baseData.password = '123456' // 新建教师默认密码
    } else if (teacherForm.password && teacherForm.password.trim()) {
      baseData.password = teacherForm.password // 编辑时如果填写了密码则更新
    }
      // 审核状态：后端 DTO 无此字段，这里不直接提交 isVerified，由单独接口控制

    let result: any
    let currentId = teacherForm.id

    if (teacherForm.id === 0) {
      // 新建：先创建拿到ID
      result = await teacherAPI.create(baseData)
      if (!result.success || !result.data?.id) {
        ElMessage.error(result.message || '创建教师失败')
        return
      }
      currentId = result.data.id
      teacherForm.id = currentId
    } else {
      // 编辑：先更新基础资料
      result = await teacherAPI.update(currentId, baseData)
      if (!result.success) {
        ElMessage.error(result.message || '更新失败')
        return
      }
    }

    // 若选择了新头像：按最终ID上传并二次更新头像URL
    if (_teacherAvatarFile.value) {
      const uploadedAvatarUrl = await fileAPI.presignAndPut(_teacherAvatarFile.value, 'admin/teacher/avatar', currentId)
      const updateAvatarResp = await teacherAPI.update(currentId, { realName: teacherForm.realName, avatarUrl: uploadedAvatarUrl } as any)
      if (!updateAvatarResp.success) {
        ElMessage.error(updateAvatarResp.message || '头像更新失败')
        return
      }
      teacherForm.avatarUrl = uploadedAvatarUrl
    }


    ElMessage.success(teacherForm.id === 0 ? '添加成功，默认密码为123456' : '更新成功')

    // 如果是新增教师，保存成功后询问是否设置可上课时间
    if (teacherForm.id === 0) {
      try {
        await ElMessageBox.confirm(
          '教师添加成功！是否现在设置该教师的可上课时间？',
          '设置可上课时间',
          {
            confirmButtonText: '立即设置',
            cancelButtonText: '稍后设置',
            type: 'info'
          }
        )
        // 用户选择立即设置，打开可上课时间设置对话框
        teacherDialogVisible.value = false
        await loadTeacherList()
        // 等待一下确保列表更新完成
        setTimeout(() => {
          openAvailabilityDialog()
        }, 100)
        return
      } catch {
        // 用户选择稍后设置，继续正常流程
      }
    }

    // 清理状态
    _teacherAvatarFile.value = null
    teacherDialogVisible.value = false
    await loadTeacherList()
  } catch (error: any) {
    console.error('保存教师失败:', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 打开可上课时间设置对话框
const openAvailabilityDialog = () => {
  if (teacherForm.id === 0) {
    ElMessage.warning('请先保存教师信息后再设置可上课时间')
    return
  }
  availabilityDialogVisible.value = true
}

// 关闭可上课时间设置对话框
const closeAvailabilityDialog = () => {
  availabilityDialogVisible.value = false
}

// 可上课时间保存成功回调
const onAvailabilitySaveSuccess = () => {
  ElMessage.success('可上课时间设置已保存')
  closeAvailabilityDialog()
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
    const result = await teacherAPI.updateVerification(teacher.id, !teacher.verified)
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

// 切换首页轮播状态
const toggleFeatured = async (teacher: any) => {
  try {
    const result = await teacherAPI.setFeatured(teacher.id, !teacher.featured)
    if (result.success) {
      ElMessage.success(teacher.isFeatured ? '已移出首页轮播' : '已加入首页轮播')
      await loadTeacherList()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    console.error('更新精选状态失败:', error)
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
  await Promise.all([
    fetchSubjects(),
    fetchTeacherLevels(),
    fetchTeachingLocations()
  ])
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
          <el-select
            v-model="teacherSearchForm.subject"
            placeholder="选择科目"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option
              v-for="s in subjects"
              :key="s.id"
              :label="s.name"
              :value="s.name"
            />
          </el-select>
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
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="educationBackground" label="教育背景" width="150" show-overflow-tooltip />
      <el-table-column prop="teachingExperience" label="教学经验" width="100">
        <template #default="{ row }">
          {{ row.teachingExperience }}年
        </template>
      </el-table-column>
      <el-table-column prop="subjects" label="教学科目" min-width="150" show-overflow-tooltip />
      <el-table-column prop="rating" label="评分" width="100">
        <template #default="{ row }">
          <span class="rating-display">{{ (row.rating || 0).toFixed(3) }}分</span>
        </template>
      </el-table-column>
      <el-table-column label="课时" min-width="160">
        <template #default="{ row }">
          <span>本月{{ (row.currentHours || 0) }}h / 上月{{ (row.lastHours || 0) }}h</span>
        </template>
      </el-table-column>
      <el-table-column prop="verified" label="认证状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.verified ? 'success' : 'warning'">
            {{ row.verified ? '已认证' : '未认证' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="featured" label="轮播状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.featured ? 'success' : 'info'">
            {{ row.featured ? '首页轮播' : '未轮播' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="350" fixed="right" align="center">
        <template #default="{ row }">
          <div class="operation-buttons">
            <el-button size="small" :icon="Edit" @click="handleEditTeacher(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.verified ? 'warning' : 'success'"
              :icon="row.verified ? Close : Check"
              @click="toggleVerification(row)"
            >
              {{ row.verified ? '取消认证' : '认证' }}
            </el-button>
            <el-button
              size="small"
              :type="row.featured ? 'warning' : 'primary'"
              @click="toggleFeatured(row)"
            >
              {{ row.featured ? '移出轮播' : '加入轮播' }}
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
        <!-- 账号信息 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item prop="username">
              <template #label>
                <span><span style="color:#f56c6c"></span> 用户名</span>
              </template>
              <el-input v-model="teacherForm.username" placeholder="teacher+用户ID" />
              <div style="margin-top:4px;">
                <el-text type="info" size="small">默认：teacher+用户ID；保存后自动生成，管理员可在此处修改</el-text>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码">
              <el-input
                v-model="teacherForm.password"
                type="password"
                :placeholder="teacherForm.id === 0 ? '默认密码为123456' : '留空则不修改密码'"
                show-password
              />
              <small style="color: #999; font-size: 12px;">
                {{ teacherForm.id === 0 ? '新建教师默认密码为123456' : '留空则不修改密码' }}
              </small>
            </el-form-item>
          </el-col>
        </el-row>


        <!-- 基本信息 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item prop="realName">
              <template #label>
                <span><span style="color:#f56c6c"></span> 教师姓名</span>
              </template>
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
            <el-form-item label="教师级别" prop="level">
              <el-select v-model="teacherForm.level" placeholder="请选择教师级别" style="width: 100%">
                <el-option v-for="opt in LEVEL_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="授课地点">
              <div style="display: flex; flex-direction: column; gap: 8px; width: 100%;">
                <el-checkbox v-model="teacherForm.supportsOnline">线上</el-checkbox>
                <el-select
                  v-model="teacherForm.teachingLocationIds"
                  multiple
                  placeholder="请选择线下地点"
                  style="width: 100%"
                  collapse-tags
                  collapse-tags-tooltip
                  :max-collapse-tags="3"
                >
                  <el-option
                    v-for="opt in TEACHING_LOCATION_OPTIONS"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
                <small style="color:#999;font-size:12px;">至少选择一个（线上或线下），可同时选择“线上 + 多个线下地点”</small>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="教学经验">
              <el-input-number v-model="teacherForm.teachingExperience" :min="0" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="时薪">
          <el-input v-model="teacherForm.hourlyRateText" placeholder="请输入时薪展示文本，例如：$45/h" />
          <div style="margin-top:4px;"><el-text type="info" size="small">此字段为展示用文本，将显示在教师端个人资料“时薪”处</el-text></div>
        </el-form-item>
        <el-form-item label="教师评分">
          <el-input-number
            v-model="teacherForm.rating"
            :min="0"
            :max="5"
            :step="0.001"
            :precision="3"
            placeholder="请输入评分"
            style="width: 200px"
          />
          <div class="rating-tip">
            <el-text type="info" size="small">评分范围：0-5分，支持0.001分精度</el-text>
          </div>
        </el-form-item>
        <el-form-item label="学历" prop="educationBackground">
          <el-select v-model="teacherForm.educationBackground" placeholder="请选择学历" style="width: 100%">
            <el-option label="专科及以下" value="专科及以下" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>
        <el-form-item label="头像">
          <AvatarUploader
            :key="teacherForm.id"
            v-model="teacherForm.avatarUrl"
            :show-upload-button="false"
            :immediate-upload="false"
            upload-module="admin/teacher/avatar"
            @file-selected="handleTeacherAvatarFileSelected"
          />
          <div v-if="_teacherAvatarFile" class="avatar-tip">
            <el-text type="info" size="small">选择新头像后，点击"保存"生效</el-text>
          </div>
        </el-form-item>

        <el-form-item prop="subjectIds">
          <template #label>
            <span><span style="color:#f56c6c">*</span> 教学科目</span>
          </template>
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
          <el-input v-model="teacherForm.specialties" placeholder="示例：冲刺高考、光速提分、幽默风趣（用中文/英文逗号分隔）" />
          <div style="margin-top:4px;"><el-text type="info" size="small">建议用逗号分隔的短词，例如：冲刺高考、光速提分、幽默风趣</el-text></div>
        </el-form-item>
        <el-form-item label="个人介绍">
          <el-input
            v-model="teacherForm.introduction"
            type="textarea"
            placeholder="请输入个人介绍"
            :rows="4"
          />
        </el-form-item>
        <el-form-item label="认证状态">
          <el-switch v-model="teacherForm.isVerified" active-text="已认证" inactive-text="未认证" />
        </el-form-item>
        <el-form-item label="本月调课次数">
          <el-input-number
            v-model="teacherForm.adjustmentTimes"
            :min="0"
            :precision="0"
            :step="1"
            style="width: 200px"
            placeholder="请输入本月调课次数"
          />
        </el-form-item>

        <!-- 可上课时间设置 -->
        <el-form-item label="可上课时间">
          <div class="availability-setting-container">
            <el-button
              type="primary"
              @click="openAvailabilityDialog"
              :disabled="teacherForm.id === 0"
            >
              <el-icon><Calendar /></el-icon>
              设置可上课时间
            </el-button>
            <el-text type="info" size="small" style="margin-left: 8px;">
              {{ teacherForm.id === 0 ? '请先保存教师信息后再设置可上课时间' : '点击设置教师的可上课时间段' }}
            </el-text>
          </div>
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

    <!-- 可上课时间设置对话框 -->
    <el-dialog
      v-model="availabilityDialogVisible"
      :title="`设置 ${teacherForm.realName} 的可上课时间`"
      width="90%"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="availability-dialog-content">
        <TeacherAvailabilityEditor
          :teacher-id="teacherForm.id"
          :months="12"
          :admin-mode="true"
          @save-success="onAvailabilitySaveSuccess"
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeAvailabilityDialog">关闭</el-button>
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

.avatar-tip {
  margin-top: 8px;
  text-align: center;
}

.rating-tip {
  margin-top: 8px;
  text-align: left;
}

.rating-display {
  font-weight: 600;
  color: #ff9900;
  font-size: 14px;
}

.availability-setting-container {
  display: flex;
  align-items: center;
  margin-top: 8px;
}

.availability-dialog-content {
  max-height: 70vh;
  overflow-y: auto;
}

.availability-dialog-content .editor {
  background: white;
  border-radius: 4px;
  padding: 16px;
}
</style>
