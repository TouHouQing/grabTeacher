<template>
  <div class="editor">
    <div class="toolbar">
      <el-alert type="info" :closable="false" show-icon title="设置按日历的可上课基础段，仅影响学生可预约；已排课程优先级更高" />
      <div class="actions">
        <el-button size="small" @click="onFullOpenThisMonth">本月全开</el-button>
        <el-button size="small" @click="onClearThisMonth">清空本月</el-button>
        <el-button type="primary" size="small" :loading="saving" @click="onSave">保存本月</el-button>
      </div>
    </div>
    <CalendarMultiMonth
      ref="calRef"
      :teacher-id="teacherId"
      :months="months"
      :hide-header="true"
      mode="teacher"
      @change-teacher-selection-many="onTeacherSelectionMany"
      @last-clicked-date="onLastClickedDate"
    />




  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import CalendarMultiMonth from '@/components/CalendarMultiMonth.vue'
import { teacherAPI } from '@/utils/api'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const calRef = ref<InstanceType<typeof CalendarMultiMonth> | null>(null)

const BASE_SLOTS = ['08:00-10:00','10:00-12:00','13:00-15:00','15:00-17:00','17:00-19:00','19:00-21:00']

// 常用预设配置（高性能：常量 + 简单集合运算）
const PRESETS: Record<string, { label: string; scope: 'all'|'weekday'|'weekend'; slots: string[] }> = {
  weekday_full: { label: '工作日全开', scope: 'weekday', slots: [...BASE_SLOTS] },
  weekend_full: { label: '周末全开', scope: 'weekend', slots: [...BASE_SLOTS] },
  all_full:     { label: '整月全开', scope: 'all',     slots: [...BASE_SLOTS] },
  evening:      { label: '每日晚上', scope: 'all', slots: ['17:00-19:00','19:00-21:00'] },
  daytime:      { label: '每日下午', scope: 'all', slots: ['13:00-15:00','15:00-17:00'] },
  morning_evening: { label: '每日上午', scope: 'all', slots: ['08:00-10:00','10:00-12:00'] },
  clear:        { label: '清空范围', scope: 'all', slots: [] },
}


const props = defineProps<{ teacherId: number; months?: number; adminMode?: boolean }>()

const emit = defineEmits<{
  (e: 'save-success'): void
}>()

const months = props.months || 12
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

// 预设对话框 + 回显
const presetDialog = ref(false)
const presetKey = ref<keyof typeof PRESETS>('weekday_full')
const presetTitle = computed(()=> PRESETS[presetKey.value]?.label || '预设')
const presetRange = ref<[Date, Date] | null>(null)
const lastAppliedPreset = ref<keyof typeof PRESETS | ''>('')
const lastPresetLabel = computed(()=> lastAppliedPreset.value ? PRESETS[lastAppliedPreset.value].label : '')
const lastPresetSlots = computed<string[]>(()=> lastAppliedPreset.value ? PRESETS[lastAppliedPreset.value].slots : [])
const lastPresetSlotsLabel = computed(()=> lastPresetSlots.value.join(','))
const lastPresetSlotsLabelShort = computed(()=> lastPresetSlotsLabel.value.length > 28 ? (lastPresetSlotsLabel.value.slice(0, 28) + '…') : lastPresetSlotsLabel.value)

// 事件处理：聚合选择与最后点击日期（避免在模板中直接写 .value 导致未更新）
function onTeacherSelectionMany(m: Record<string, string[]>) {
  selection.value = m
}
function onLastClickedDate(d: string) {
  lastClickedDate.value = d
}

// 引导：提示先点击日历某一天以便“复制当日”

const onSave = async () => {
  // 聚合所有月份后，仅保留当前活跃月份（保存本月）
  const agg = (calRef.value && (calRef.value as any).getTeacherSelectionAll) ? (calRef.value as any).getTeacherSelectionAll() : selection.value
  const active = (calRef.value as any)?.getActiveMonthInfo?.()
  const activeKey: string | undefined = active?.key

  const allItems = Object.entries(agg)
    .map(([date, slots]) => ({ date, timeSlots: Array.isArray(slots) ? (slots as string[]) : [] }))
  const items = activeKey ? allItems.filter(it => it.date.startsWith(activeKey)) : allItems

  if (items.length === 0) {
    ElMessage.warning('本月未选择任何时段，已取消保存')
    return
  }
  saving.value = true
  try {
    if (props.adminMode) {
      await teacherAPI.setDailyAvailabilityByAdmin({ teacherId: props.teacherId, items, overwrite: true })
    } else {
      await teacherAPI.setDailyAvailability({ teacherId: props.teacherId, items, overwrite: true })
    }
    ElMessage.success('已保存本月')
    ;(calRef.value as any)?.refreshActiveMonth?.()

  } catch (e:any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function onFullOpenThisMonth() {
  ;(calRef.value as any)?.applyTeacherFullMonth?.(BASE_SLOTS, true)
}
function onClearThisMonth() {
  ;(calRef.value as any)?.clearTeacherFullMonth?.()
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
  const patch: Record<string,string[]> = {}
  let cur = start
  while (cur.isBefore(end) || cur.isSame(end,'day')) {
    if (inScope(cur, pasteScope.value)) {
      const key = cur.format('YYYY-MM-DD')
      if (overwrite.value) selection.value[key] = [...clipboardSlots.value]
      else selection.value[key] = Array.from(new Set([...(selection.value[key]||[]), ...clipboardSlots.value]))
      patch[key] = [...clipboardSlots.value]
    }
    cur = cur.add(1,'day')
  }
  // 同步到日历UI（仅作用于当前活跃月份内的日期）
  ;(calRef.value as any)?.applySelectionPatch?.(patch, overwrite.value)
  pasteDialog.value = false
  ElMessage.success('已粘贴到指定范围')
}

function openPreset(t: keyof typeof PRESETS) {
  presetKey.value = t
  presetDialog.value = true
}

function onApplyPreset() {
  if (!presetRange.value) return
  const [s,e] = presetRange.value
  const start = dayjs(s), end = dayjs(e)
  const { scope, slots } = PRESETS[presetKey.value]
  const patch: Record<string,string[]> = {}
  let cur = start
  while (cur.isBefore(end) || cur.isSame(end,'day')) {
    if (inScope(cur, scope)) {
      const key = cur.format('YYYY-MM-DD')
      if (presetKey.value==='clear') {
        selection.value[key] = []
        patch[key] = []
      } else {
        if (overwrite.value) selection.value[key] = [...slots]
        else selection.value[key] = Array.from(new Set([...(selection.value[key]||[]), ...slots]))
        patch[key] = [...slots]
      }
    }
    cur = cur.add(1,'day')
  }
  // 清空：强制覆盖；其他预设：尊重“覆盖同日已设置”
  const overwriteFlag = presetKey.value==='clear' ? true : overwrite.value
  ;(calRef.value as any)?.applySelectionPatch?.(patch, overwriteFlag)

  lastAppliedPreset.value = presetKey.value
  presetDialog.value = false
  ElMessage.success(`已应用预设：${PRESETS[presetKey.value].label}`)
}
</script>

<style scoped>
.editor { display: flex; flex-direction: column; gap: 10px; }
.toolbar { display: flex; flex-direction: column; gap: 8px; }
.actions { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
</style>

