<template>
  <div class="editor">
    <div class="toolbar">
      <el-alert type="info" :closable="false" show-icon title="设置按日历的可上课基础段，仅影响学生可预约；已排课程优先级更高" />
      <div class="actions">
        <el-checkbox v-model="overwrite" size="small">覆盖同日已设置</el-checkbox>
        <el-button size="small" @click="onCopyDay" :disabled="!lastClickedDate">复制当日</el-button>
        <el-button size="small" @click="pasteDialog=true" :disabled="!clipboardSlots.length">粘贴到…</el-button>
        <el-dropdown>
          <el-button size="small">预设<i class="el-icon--right el-icon-arrow-down" /></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="openPreset('weekday')">工作日全开</el-dropdown-item>
              <el-dropdown-item @click="openPreset('weekend')">周末全开</el-dropdown-item>
              <el-dropdown-item divided @click="openPreset('clear')">清空范围</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="primary" size="small" :loading="saving" @click="onSave">保存可用时间</el-button>
      </div>
    </div>
    <CalendarMultiMonth
      :teacher-id="teacherId"
      :months="months"
      mode="teacher"
      @change-teacher-selection-many="(m)=>selection.value=m"
      @last-clicked-date="d=>lastClickedDate=d"
    />

    <el-dialog v-model="pasteDialog" title="粘贴到范围" width="460px">
      <div class="paste-form">
        <el-date-picker v-model="pasteRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-radio-group v-model="pasteScope" class="scope">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="weekday">仅工作日</el-radio-button>
          <el-radio-button label="weekend">仅周末</el-radio-button>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="pasteDialog=false">取消</el-button>
        <el-button type="primary" :disabled="!canPaste" @click="onPaste">确定粘贴</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="presetDialog" :title="presetTitle" width="460px">
      <div class="paste-form">
        <el-date-picker v-model="presetRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
      </div>
      <template #footer>
        <el-button @click="presetDialog=false">取消</el-button>
        <el-button type="primary" :disabled="!presetRange || presetRange.length!==2" @click="onApplyPreset">应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import CalendarMultiMonth from '@/components/CalendarMultiMonth.vue'
import { teacherAPI } from '@/utils/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const BASE_SLOTS = ['08:00-10:00','10:00-12:00','13:00-15:00','15:00-17:00','17:00-19:00','19:00-21:00']

const props = defineProps<{ teacherId: number; months?: number; adminMode?: boolean }>()

const months = props.months || 6
const overwrite = ref(false)
const saving = ref(false)
const selection = ref<Record<string, string[]>>({})
const lastClickedDate = ref<string>('')
const clipboardSlots = ref<string[]>([])

// 粘贴对话框
const pasteDialog = ref(false)
const pasteRange = ref<[Date, Date] | null>(null)
const pasteScope = ref<'all'|'weekday'|'weekend'>('all')
const canPaste = computed(()=>clipboardSlots.value.length>0 && !!pasteRange.value && pasteRange.value.length===2)

// 预设对话框
const presetDialog = ref(false)
const presetType = ref<'weekday'|'weekend'|'clear'>('weekday')
const presetTitle = computed(()=> presetType.value==='clear' ? '清空范围' : (presetType.value==='weekday'?'工作日全开':'周末全开'))
const presetRange = ref<[Date, Date] | null>(null)

// 引导：提示先点击日历某一天以便“复制当日”

const onSave = async () => {
  const items = Object.entries(selection.value).filter(([,arr])=>arr && arr.length>0).map(([date, slots])=>({ date, timeSlots: slots }))
  saving.value = true
  try {
    if (props.adminMode) {
      await teacherAPI.setDailyAvailabilityByAdmin({ teacherId: props.teacherId, items, overwrite: overwrite.value })
    } else {
      await teacherAPI.setDailyAvailability({ teacherId: props.teacherId, items, overwrite: overwrite.value })
    }
    ElMessage.success('保存成功')
  } catch (e:any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function onCopyDay() {
  if (!lastClickedDate.value) { ElMessage.warning('请先在月历中点击某一天的任一时段'); return }
  const slots = selection.value[lastClickedDate.value] || []
  clipboardSlots.value = [...slots]
  if (!clipboardSlots.value.length) ElMessage.info('该日尚未选择任何时段，已复制为空集')
  else ElMessage.success(`已复制 ${lastClickedDate.value} 的 ${clipboardSlots.value.length} 个时段`)
}

function inScope(d: dayjs.Dayjs, scope: 'all'|'weekday'|'weekend') {
  if (scope==='all') return true
  const w = d.day() // 0=周日,1=周一,...
  if (scope==='weekday') return w>=1 && w<=5
  return w===0 || w===6
}

function onPaste() {
  if (!pasteRange.value || clipboardSlots.value.length===0) return
  const [s,e] = pasteRange.value
  const start = dayjs(s), end = dayjs(e)
  let cur = start
  while (cur.isBefore(end) || cur.isSame(end,'day')) {
    if (inScope(cur, pasteScope.value)) {
      const key = cur.format('YYYY-MM-DD')
      if (overwrite.value) selection.value[key] = [...clipboardSlots.value]
      else selection.value[key] = Array.from(new Set([...(selection.value[key]||[]), ...clipboardSlots.value]))
    }
    cur = cur.add(1,'day')
  }
  pasteDialog.value = false
  ElMessage.success('已粘贴到指定范围')
}

function openPreset(t: 'weekday'|'weekend'|'clear') {
  presetType.value = t
  presetDialog.value = true
}

function onApplyPreset() {
  if (!presetRange.value) return
  const [s,e] = presetRange.value
  const start = dayjs(s), end = dayjs(e)
  const scope: 'weekday'|'weekend'|'all' = presetType.value==='clear' ? 'all' : presetType.value
  let cur = start
  while (cur.isBefore(end) || cur.isSame(end,'day')) {
    if (inScope(cur, scope)) {
      const key = cur.format('YYYY-MM-DD')
      if (presetType.value==='clear') selection.value[key] = []
      else selection.value[key] = overwrite.value ? [...BASE_SLOTS] : Array.from(new Set([...(selection.value[key]||[]), ...BASE_SLOTS]))
    }
    cur = cur.add(1,'day')
  }
  presetDialog.value = false
  ElMessage.success('已应用预设到范围内日期')
}
</script>

<style scoped>
.editor { display: flex; flex-direction: column; gap: 10px; }
.toolbar { display: flex; flex-direction: column; gap: 8px; }
.actions { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
</style>

