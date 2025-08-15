<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { jobPostAPI, gradeApi, subjectAPI } from '../../../utils/api'

// 列表与加载状态
const list = ref<any[]>([])
const loading = ref(false)

// 过滤与分页
const searchForm = reactive({
  keyword: '',
  status: '',
  gradeId: undefined as number | undefined,
  subjectId: undefined as number | undefined,
})
const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })

// 选项数据
const gradeOptions = ref<any[]>([])
const subjectOptions = ref<any[]>([])

// 对话框与表单
const dialogVisible = ref(false)
const dialogTitle = ref('')
const form = reactive({
  id: 0 as number,
  title: '',
  introduction: '',
  gradeIds: [] as number[],
  subjectIds: [] as number[],
  tags: [] as string[],
  status: 'active',
  priority: 0,
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  gradeIds: [{ required: true, message: '请选择年级', trigger: 'change' }],
  subjectIds: [{ required: true, message: '请选择科目', trigger: 'change' }],
}

// 加载选项
const loadOptions = async () => {
  const [gradesRes, subjectsRes] = await Promise.all([
    gradeApi.getAllPublic(),
    subjectAPI.getActiveSubjects(),
  ])
  gradeOptions.value = (gradesRes?.data || []).map((g: any) => ({ label: g.gradeName, value: g.id }))
  subjectOptions.value = (subjectsRes?.data || []).map((s: any) => ({ label: s.name, value: s.id }))
}

// 加载列表
const loadList = async () => {
  loading.value = true
  try {
    const res = await jobPostAPI.adminList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      gradeId: searchForm.gradeId,
      subjectId: searchForm.subjectId,
      status: searchForm.status || undefined,
      keyword: searchForm.keyword || undefined,
      includeDeleted: false
    })
    const data = res.data || {}
    list.value = data.records || []
    pagination.total = data.total || 0
    pagination.currentPage = data.current || 1
    pagination.pageSize = data.size || pagination.pageSize
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索与重置
const onSearch = () => { pagination.currentPage = 1; loadList() }
const onReset = () => { searchForm.keyword=''; searchForm.status=''; searchForm.gradeId=undefined; searchForm.subjectId=undefined; pagination.currentPage=1; loadList() }

// 新建与编辑
const onAdd = () => {
  dialogTitle.value = '新增招聘'
  Object.assign(form, { id: 0, title: '', introduction: '', gradeIds: [], subjectIds: [], tags: [], status: 'active', priority: 0 })
  dialogVisible.value = true
}
const onEdit = async (row: any) => {
  dialogTitle.value = '编辑招聘'
  const res = await jobPostAPI.adminGetById(row.id)
  const jp = res.data || row
  Object.assign(form, {
    id: jp.id,
    title: jp.title,
    introduction: jp.introduction,
    // 冗余字段 grade_ids/subject_ids 为逗号字符串，优先解析；否则从名称推断为空
    gradeIds: (jp.gradeIds ? String(jp.gradeIds).split(',').filter(Boolean).map((x: string)=>+x) : []),
    subjectIds: (jp.subjectIds ? String(jp.subjectIds).split(',').filter(Boolean).map((x: string)=>+x) : []),
    tags: (()=>{ try{ return (jp.positionTags? JSON.parse(jp.positionTags): [])?.slice(0, TAG_MAX_COUNT) }catch{ return [] } })(),
    status: jp.status || 'active',
    priority: jp.priority ?? 0,
  })
  dialogVisible.value = true
}

// 保存
const onSave = async () => {
  if (!form.title?.trim()) { ElMessage.warning('请输入标题'); return }
  if (!form.gradeIds?.length) { ElMessage.warning('请选择年级'); return }
  if (!form.subjectIds?.length) { ElMessage.warning('请选择科目'); return }
  try {
    const payload = { title: form.title, introduction: form.introduction, gradeIds: form.gradeIds, subjectIds: form.subjectIds, tags: form.tags.slice(0, TAG_MAX_COUNT), status: form.status, priority: form.priority }
    const res = form.id === 0
      ? await jobPostAPI.create(payload)
      : await jobPostAPI.update(form.id, payload)
    if (res.success) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      pagination.currentPage = 1
      await loadList()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

// 删除
const onDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该招聘？此操作不可恢复', '提示', { type: 'warning' })
    const res = await jobPostAPI.delete(row.id)
    if (res.success) { ElMessage.success('删除成功'); loadList() } else { ElMessage.error(res.message || '删除失败') }
  } catch {}
}

// 分页
const onPageChange = (p: number) => { pagination.currentPage = p; loadList() }
const onSizeChange = (s: number) => { pagination.pageSize = s; pagination.currentPage = 1; loadList() }


// 标签编辑器：轻量输入 + 按回车/逗号添加
const TAG_MAX_COUNT = 4
const TAG_MAX_LEN = 12
const tagInput = ref('')
const tagFocused = ref(false)
const tagInputRef = ref<HTMLInputElement | null>(null)
const tagLimitReached = computed(() => form.tags.length >= TAG_MAX_COUNT)

const splitToTags = (text: string): string[] => {
  return text
    .split(/[,，、\s;；]+/)
    .map(t => t.trim())
    .filter(Boolean)
}

const addTags = (candidates: string[]) => {
  for (const raw of candidates) {
    if (form.tags.length >= TAG_MAX_COUNT) break
    const t = raw.slice(0, TAG_MAX_LEN)
    if (!t) continue
    if (form.tags.includes(t)) continue
    form.tags.push(t)
    if (form.tags.length >= TAG_MAX_COUNT) break
  }
}

const addTagsFromInput = () => {
  if (!tagInput.value) return
  addTags(splitToTags(tagInput.value))
  tagInput.value = ''
}

const onTagRemove = (tag: string) => {
  form.tags = form.tags.filter(t => t !== tag)
}

const onTagInputKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' || e.key === ',' || e.key === '、' || e.key === '，' || e.key === ';' || e.key === '；') {
    e.preventDefault()
    addTagsFromInput()
  }
}

onMounted(async () => { await loadOptions(); await loadList() })
</script>

<template>
  <div class="jobpost-mgmt">
    <div class="header"><h3>教师招聘管理</h3></div>

    <el-card class="search-card" shadow="never">
      <div class="search-header">
        <el-form :model="searchForm" inline class="search-form">
          <el-form-item label="关键词">
            <el-input v-model="searchForm.keyword" placeholder="标题关键词" :prefix-icon="Search" clearable style="width: 220px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 110px">
              <el-option label="招聘中" value="active" />
              <el-option label="已暂停" value="expired" />
            </el-select>
          </el-form-item>
          <el-form-item label="年级">
            <el-select v-model="searchForm.gradeId" placeholder="全部年级" clearable filterable style="width: 160px">
              <el-option v-for="g in gradeOptions" :key="g.value" :label="g.label" :value="g.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="科目">
            <el-select v-model="searchForm.subjectId" placeholder="全部科目" clearable filterable style="width: 160px">
              <el-option v-for="s in subjectOptions" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item class="form-actions-row">
            <el-button type="primary" :icon="Plus" @click="onAdd">新增招聘</el-button>
            <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="onReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>


    <el-table :data="list" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="title" label="标题" min-width="280" />
      <el-table-column prop="gradeNames" label="年级" min-width="200" />
      <el-table-column prop="subjectNames" label="科目" min-width="200" />
      <el-table-column label="标签" min-width="220">
        <template #default="{ row }">
          <div class="tags-cell">
            <el-tag
              v-for="(t, idx) in (Array.isArray(row.positionTags) ? row.positionTags : (row.positionTags ? JSON.parse(row.positionTags) : []))"
              :key="idx"
              size="small"
              type="info"
              class="cell-tag"
            >{{ t }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status==='active' ? 'success' : 'info'">{{ row.status==='active' ? '招聘中' : '已暂停' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="排序" width="100" />
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <div class="op">
            <el-button size="small" :icon="Edit" @click="onEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="onDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        :current-page="pagination.currentPage"
        :page-size="pagination.pageSize"
        :page-sizes="[10,20,50,100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="介绍">
          <el-input type="textarea" v-model="form.introduction" :rows="4" placeholder="岗位介绍" />
        </el-form-item>
        <el-form-item label="年级" prop="gradeIds">
          <el-select v-model="form.gradeIds" multiple filterable placeholder="请选择年级" style="width: 520px">
            <el-option v-for="g in gradeOptions" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="科目" prop="subjectIds">
          <el-select v-model="form.subjectIds" multiple filterable placeholder="请选择科目" style="width: 520px">
            <el-option v-for="s in subjectOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <div style="width: 520px">
            <div class="tag-editor" @click="tagInputRef?.focus()">
              <el-tag v-for="t in form.tags" :key="t" closable @close="onTagRemove(t)" type="info" class="tag-chip">{{ t }}</el-tag>
              <input ref="tagInputRef" v-model="tagInput" class="tag-input" :disabled="tagLimitReached" :placeholder="`输入后按回车或逗号添加（最多${TAG_MAX_COUNT}个）`" @keydown="onTagInputKeydown" @blur="addTagsFromInput" />
            </div>
            <div class="tag-hint">提示：标签可自定义，建议使用短语词（如“急招”“线上”“线下”“寒假”），将展示在招聘列表。</div>
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 200px">
            <el-option label="招聘中" value="active" />
            <el-option label="已暂停" value="expired" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <div style="width: 200px">
            <el-input type="number" v-model.number="form.priority" style="width: 100%" />
            <div style="color:#909399; font-size:12px; margin-top:6px;">数字越小越靠前</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="onSave">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.jobpost-mgmt { padding: 20px; padding-top: 5px; }
.header { margin-bottom: 12px; }
.op { display: flex; gap: 8px; justify-content: center; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 16px; }
.search-header { display:flex; align-items:flex-start; justify-content:space-between; gap:16px; }
.search-form { flex: 1; display:flex; flex-wrap: wrap; column-gap: 16px; row-gap: 8px; }
.search-actions { white-space: nowrap; display:flex; gap:8px; align-items:center; }
.form-actions-row { width: 100%; margin-top: 4px; }
.form-actions-row .el-form-item__content { display:flex; gap:8px; }
.tag-editor { display:flex; flex-wrap:wrap; gap:6px; min-height:34px; align-items:center; border:1px solid var(--el-border-color); border-radius:4px; padding:4px 8px; }
.tag-chip { margin: 2px 0; }
.tag-input { flex:1; min-width:160px; border: none; outline: none; height:26px; line-height:26px; }
.tag-hint { color:#909399; font-size:12px; margin-top:6px; }
.tags-cell { display:flex; flex-wrap:wrap; gap:6px; max-height: 44px; overflow:hidden; align-items:center; }
.cell-tag { margin: 2px 0; }
</style>

