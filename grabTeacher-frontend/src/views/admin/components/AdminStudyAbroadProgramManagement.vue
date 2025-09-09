<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { studyAbroadAPI, fileAPI } from '../../../utils/api'
import AvatarUploader from '../../../components/AvatarUploader.vue'

const loading = ref(false)
const list = ref<any[]>([])

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ keyword: '', countryId: null as number | null, stageId: null as number | null, status: '' as '' | '启用' | '停用', isHot: '' as '' | '是' | '否' })

const countries = ref<any[]>([])
const stages = ref<any[]>([])
const countryMap = computed<Record<number, string>>(() => Object.fromEntries(countries.value.map((c:any) => [c.id, c.countryName])))
const stageMap = computed<Record<number, string>>(() => Object.fromEntries(stages.value.map((s:any) => [s.id, s.stageName])))

const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive({ id: 0, title: '', countryId: null as number | null, stageId: null as number | null, description: '', imageUrl: '', tags: '[]', hot: false, sortOrder: 0, active: true })

// 项目图片上传（与教师/学生头像一致的两步上传逻辑）
const _programImageFile = ref<File | null>(null)
const handleProgramImageFileSelected = (file: File | null) => { _programImageFile.value = file }
const uploadProgramImageIfNeeded = async (currentId: number): Promise<string | null> => {
  if (!_programImageFile.value) return null
  return await fileAPI.presignAndPut(_programImageFile.value, 'admin/study-abroad/program-image', currentId)
}

const rules = {
  title: [{ required: true, message: '请输入项目标题', trigger: 'blur' }],
  countryId: [{ required: true, message: '请选择国家', trigger: 'change' }],
  stageId: [{ required: true, message: '请选择阶段', trigger: 'change' }]
}

const loadMeta = async () => {
  const [c, s] = await Promise.all([studyAbroadAPI.getCountries(), studyAbroadAPI.getStages()])
  if (c?.success) countries.value = c.data || []
  if (s?.success) stages.value = s.data || []
}

const loadList = async () => {
  try {
    loading.value = true
    const params: any = {
      page: pagination.currentPage,
      size: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      isActive: searchForm.status === '启用' ? true : searchForm.status === '停用' ? false : undefined,
      countryId: searchForm.countryId || undefined,
      stageId: searchForm.stageId || undefined,
      isHot: searchForm.isHot === '是' ? true : searchForm.isHot === '否' ? false : undefined,
    }
    const res = await studyAbroadAPI.adminListPrograms(params)
    if (res?.success) {
      const data = res.data || {}
      list.value = data.records || []
      pagination.total = data.total || list.value.length
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '获取项目列表失败')
  } finally {
    loading.value = false
  }
}

const onSearch = () => { pagination.currentPage = 1; loadList() }
const onReset = () => { Object.assign(searchForm, { keyword: '', countryId: null, stageId: null, status: '', isHot: '' }); pagination.currentPage = 1; loadList() }

const onAdd = async () => {
  dialogTitle.value = '添加项目'
  await loadMeta()
  Object.assign(form, { id: 0, title: '', countryId: null, stageId: null, description: '', imageUrl: '', tags: '[]', hot: false, sortOrder: 0, active: true })
  _programImageFile.value = null
  dialogVisible.value = true
}

const onEdit = async (row: any) => {
  dialogTitle.value = '编辑项目'
  await loadMeta()
  Object.assign(form, { ...row, tags: row.tags || '[]' })
  _programImageFile.value = null
  dialogVisible.value = true
}

const onSave = async () => {
  try {
    // 校验标签JSON
    if (form.tags && form.tags.trim().length > 0) {
      try {
        const parsed = JSON.parse(form.tags)
        if (!Array.isArray(parsed) || !parsed.every((t:any) => typeof t === 'string')) {
          ElMessage.error('标签必须是字符串数组，例如：["文书指导","面试培训"]')
          return
        }
      } catch (e) {
        ElMessage.error('标签(JSON)格式不正确，例如：["文书指导","面试培训"]')
        return
      }
    }

    loading.value = true
    const payload = { title: form.title, countryId: form.countryId!, stageId: form.stageId!, description: form.description, imageUrl: form.imageUrl, tags: form.tags, hot: form.hot, sortOrder: form.sortOrder, active: form.active }

    // 第一步：创建或更新主体信息
    const res = form.id === 0 ? await studyAbroadAPI.adminCreateProgram(payload) : await studyAbroadAPI.adminUpdateProgram(form.id, payload)
    if (!res?.success) { ElMessage.error(res?.message || '操作失败'); return }

    // 计算当前ID（新建时取后端返回ID）
    const currentId: number = form.id === 0 ? (res.data?.id || 0) : form.id
    if (form.id === 0 && currentId === 0) {
      ElMessage.error('未获取到新建ID')
      return
    }

    // 第二步：如选择了新图片，则按最终ID上传并二次更新imageUrl
    if (_programImageFile.value) {
      const uploadedUrl = await uploadProgramImageIfNeeded(currentId)
      if (uploadedUrl) {
        const payload2 = { ...payload, imageUrl: uploadedUrl }
        const updateImgRes = await studyAbroadAPI.adminUpdateProgram(currentId, payload2 as any)
        if (!updateImgRes?.success) { ElMessage.error(updateImgRes?.message || '图片更新失败'); return }
        form.imageUrl = uploadedUrl
      }
    }

    ElMessage.success(form.id === 0 ? '添加成功' : '更新成功')
    _programImageFile.value = null
    dialogVisible.value = false
    await loadList()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') } finally { loading.value = false }
}

const onDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除项目“${row.title}”吗？`, '提示', { type: 'warning' })
    const res = await studyAbroadAPI.adminDeleteProgram(row.id)
    if (res?.success) { ElMessage.success('删除成功'); loadList() } else { ElMessage.error(res?.message || '删除失败') }
  } catch {}
}

const onToggleStatus = async (row: any) => {
  try {
    const res = await studyAbroadAPI.adminUpdateProgramStatus(row.id, !row.active)
    if (res?.success) { ElMessage.success(row.active ? '已停用' : '已启用'); loadList() } else { ElMessage.error(res?.message || '操作失败') }
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
}

const onToggleHot = async (row: any, val: any) => {
  const next = !!val
  const prev = !!row.hot
  row.hot = next
  try {
    const res = await studyAbroadAPI.adminUpdateProgramFlags(row.id, next, undefined)
    if (res?.success) {
      ElMessage.success('已更新热门状态')
    } else {
      row.hot = prev
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e: any) {
    row.hot = prev
    ElMessage.error(e?.message || '操作失败')
  }
}

const onPageChange = (p: number) => { pagination.currentPage = p; loadList() }
const onSizeChange = (s: number) => { pagination.pageSize = s; pagination.currentPage = 1; loadList() }

onMounted(async () => { await loadMeta(); await loadList() })
</script>

<template>
  <div class="program-mgmt">
    <div class="header"><h3>留学项目管理</h3></div>

    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="项目标题" :prefix-icon="Search" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="国家">
          <el-select v-model="searchForm.countryId" clearable filterable placeholder="选择国家" style="width: 160px">
            <el-option v-for="c in countries" :key="c.id" :label="c.countryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="searchForm.stageId" clearable filterable placeholder="选择阶段" style="width: 160px">
            <el-option v-for="s in stages" :key="s.id" :label="s.stageName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="热门">
          <el-select v-model="searchForm.isHot" placeholder="是否热门" clearable style="width: 120px">
            <el-option label="是" value="是" />
            <el-option label="否" value="否" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="onAdd">添加项目</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column label="国家" width="140">
        <template #default="{ row }">{{ countryMap[row.countryId] || row.countryId }}</template>
      </el-table-column>
      <el-table-column label="阶段" width="140">
        <template #default="{ row }">{{ stageMap[row.stageId] || row.stageId }}</template>
      </el-table-column>
      <el-table-column label="热门" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.hot" @change="val => onToggleHot(row, val)" />
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'danger'">{{ row.active ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right" align="center">
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-form-item prop="title" label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item prop="countryId" label="国家">
          <el-select v-model="form.countryId" placeholder="选择国家" filterable style="width: 240px">
            <el-option v-for="c in countries" :key="c.id" :label="c.countryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="stageId" label="阶段">
          <el-select v-model="form.stageId" placeholder="选择阶段" filterable style="width: 240px">
            <el-option v-for="s in stages" :key="s.id" :label="s.stageName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目图片">
          <AvatarUploader
            v-model="form.imageUrl"
            :show-upload-button="false"
            :immediate-upload="false"
            :size="96"
            alt-text="项目图片"
            @file-selected="handleProgramImageFileSelected"
          />
          <div v-if="_programImageFile" class="avatar-tip">
            <el-text type="info" size="small">选择新图片后，点击"保存"生效</el-text>
          </div>
        </el-form-item>
        <el-form-item label="标签(JSON)">
          <el-input v-model="form.tags" type="textarea" autosize placeholder='请输入JSON数组，例如：["文书指导","面试培训"]'>
            <template #prepend>JSON</template>
          </el-input>
          <div style="color:#909399;font-size:12px;line-height:1.2;margin-top:4px;">
            示例：["文书指导", "院校申请", "面试培训"]
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="热门">
          <el-switch v-model="form.hot" active-text="热门" inactive-text="非热门" />
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
.program-mgmt { padding: 20px; padding-top: 5px; }
.header { margin-bottom: 16px; }
.search-card { margin-bottom: 16px; }
.pagination { display: flex; justify-content: center; margin: 16px 0; }
</style>

