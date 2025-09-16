<template>
  <div class="calendar-multi">
    <div class="toolbar">
      <div class="months">
        <el-button-group>
          <el-button v-for="(m, idx) in monthList" :key="m.key" size="small"
                     :type="idx===activeIdx ? 'primary' : 'default'"
                     @click="activeIdx = idx">{{ m.label }}</el-button>
        </el-button-group>
      </div>
      <div class="spacer" />
      <slot name="extra" />
      <el-radio-group v-model="monthsCount" size="small" style="margin-left: 8px;">
        <el-radio-button :label="3">3个月</el-radio-button>
        <el-radio-button :label="6">6个月</el-radio-button>
        <el-radio-button :label="12">12个月</el-radio-button>
      </el-radio-group>
    </div>

    <CalendarMonth
      :key="monthList[activeIdx].key"
      ref="monthRef"
      :teacher-id="teacherId"
      :year="monthList[activeIdx].year"
      :month="monthList[activeIdx].month"
      :mode="mode"
      :duration-minutes="durationMinutes"
      :allowed-periods="allowedPeriods"
      :date-range-start="dateRangeStart"
      :date-range-end="dateRangeEnd"
      :hide-header="hideHeader"
      :student-single-select="props.studentSingleSelect"
      @change-teacher-selection="onMonthTeacherSelection(monthList[activeIdx].key, $event)"
      @change-student-sessions="onMonthStudentSessions(monthList[activeIdx].key, $event)"
      @clicked-date="(d)=>emit('last-clicked-date', d)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import CalendarMonth from './CalendarMonth.vue'

const props = defineProps<{
  teacherId: number
  startYear?: number
  startMonth?: number // 1-12
  months?: number
  mode: 'teacher' | 'student'
  durationMinutes?: 90 | 120
  allowedPeriods?: Array<'morning'|'afternoon'|'evening'>
  dateRangeStart?: string
  dateRangeEnd?: string
  hideHeader?: boolean
  studentSingleSelect?: boolean
}>()

const emit = defineEmits<{
  (e: 'change-teacher-selection-many', payload: Record<string, string[]>): void
  (e: 'change-student-sessions', payload: Array<{ date: string; startTime: string; endTime: string }>): void
  (e: 'last-clicked-date', payload: string): void
}>()

const base = dayjs(`${props.startYear || dayjs().year()}-${String(props.startMonth || (dayjs().month()+1)).padStart(2,'0')}-01`)
const monthsCount = ref(props.months || 3)
const activeIdx = ref(0)
const monthRef = ref<InstanceType<typeof CalendarMonth> | null>(null)


const monthList = computed(() => {
  const arr: Array<{ year: number; month: number; key: string; label: string }> = []
  for (let i=0;i<monthsCount.value;i++) {
    const d = base.add(i, 'month')
    arr.push({ year: d.year(), month: d.month()+1, key: d.format('YYYY-MM'), label: d.format('YYYY年MM月') })
  }
  // 保持 activeIdx 在范围内
  if (activeIdx.value >= arr.length) activeIdx.value = arr.length - 1
  return arr
})

// 汇总教师选择：date -> slots（跨月聚合）
const selectionByMonth = ref<Record<string, Record<string, string[]>>>({})
const onMonthTeacherSelection = (monthKey: string, selection: Record<string, string[]>) => {
  selectionByMonth.value = { ...selectionByMonth.value, [monthKey]: selection }
  emit('change-teacher-selection-many', flattenTeacherSelection())
}
const flattenTeacherSelection = () => {
  const res: Record<string, string[]> = {}
  for (const m of Object.values(selectionByMonth.value)) {
    for (const [date, slots] of Object.entries(m)) {
      if (!res[date]) res[date] = []
      const set = new Set(res[date])
      for (const s of slots) set.add(s)
      res[date] = Array.from(set)
    }
  }
  return res
}

// 学生已选 sessions 聚合
const sessionsByMonth = ref<Record<string, Array<{ date: string; startTime: string; endTime: string }>>>({})
const onMonthStudentSessions = (monthKey: string, list: Array<{ date: string; startTime: string; endTime: string }>) => {
  sessionsByMonth.value = { ...sessionsByMonth.value, [monthKey]: list }
  emit('change-student-sessions', flattenSessions())
}
const flattenSessions = () => {
  const res: Array<{ date: string; startTime: string; endTime: string }> = []
  for (const v of Object.values(sessionsByMonth.value)) res.push(...v)
  // 去重
  const seen = new Set<string>()
  return res.filter(s => {
    const k = `${s.date}_${s.startTime}_${s.endTime}`
    if (seen.has(k)) return false
    seen.add(k)
    return true
  })
}

// 暴露方法给父组件：获取聚合选择
function getTeacherSelectionAll() { return flattenTeacherSelection() }
function getStudentSessionsAll() { return flattenSessions() }
function refreshActiveMonth() { (monthRef.value as any)?.reload?.() }
function clear() { (monthRef.value as any)?.clearStudentSessions?.() }

// 清空所有月份的学生选择（用于切换1.5h/2h时刷新）
function clearAllStudentSessions() {
  sessionsByMonth.value = {}
  ;(monthRef.value as any)?.clearStudentSessions?.()
  emit('change-student-sessions', [])
}

function applySelectionPatch(patch: Record<string, string[]>, overwrite: boolean) {
  const active = monthList.value[activeIdx.value]
  if (!active) return
  const filtered: Record<string, string[]> = {}
  for (const [date, slots] of Object.entries(patch || {})) {
    if (date.startsWith(active.key)) filtered[date] = slots
  }
  ;(monthRef.value as any)?.applySelectionPatch?.(filtered, overwrite)
}
// @ts-ignore
defineExpose({ getTeacherSelectionAll, getStudentSessionsAll, refreshActiveMonth, clear, clearAllStudentSessions, applySelectionPatch })

watch(monthsCount, () => {
  // 切换月份数量时，重置活跃索引
  activeIdx.value = 0
})
</script>

<style scoped>
.calendar-multi { display: flex; flex-direction: column; gap: 8px; }
.toolbar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar .spacer { flex: 1; }
.months { max-width: 100%; overflow-x: auto; -webkit-overflow-scrolling: touch; white-space: nowrap; }
.months :deep(.el-button-group) { flex-wrap: nowrap; }
@media (max-width: 480px) { .toolbar { gap: 6px; } }
</style>

