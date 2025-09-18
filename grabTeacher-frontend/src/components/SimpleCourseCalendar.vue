<template>
  <div class="simple-calendar">
    <div class="sc-header">
      <el-button-group>
        <el-button size="small" type="warning" :disabled="disabledPrev" @click="toPrevMonth">上个月</el-button>
        <el-button size="small" type="warning" :disabled="disabledNext" @click="toNextMonth">下个月</el-button>
      </el-button-group>
      <div class="sc-title">{{ currentLabel }}</div>
    </div>

    <div class="sc-week">
      <div v-for="w in weekLabels" :key="w" class="sc-week-item">{{ w }}</div>
    </div>

    <div class="sc-grid">
      <div v-for="cell in grid" :key="cell.key"
           class="sc-cell"
           :class="{ 'is-other': !cell.inMonth, 'is-today': cell.isToday }">
        <div class="sc-date">{{ cell.day }}</div>
        <div class="sc-slots" v-if="cell.slots && cell.slots.length">
          <div v-for="(t, i) in cell.slots" :key="i" class="sc-slot">{{ t }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'

const props = defineProps<{
  sessions: Array<{ date: string; startTime: string; endTime: string }>
  start?: string
  end?: string
}>()

// 当前展示的年月（受 start/end 限制）
const current = ref(dayjs(props.start || dayjs().format('YYYY-MM-01')))
const monthNum = (d: dayjs.Dayjs) => parseInt(d.format('YYYYMM'))
const minMonthNum = computed(() => props.start ? monthNum(dayjs(props.start)) : 0)
const maxMonthNum = computed(() => props.end ? monthNum(dayjs(props.end)) : 999912)
function clampMonth(d: dayjs.Dayjs) {
  const n = monthNum(d)
  if (props.start && n < minMonthNum.value) return dayjs(props.start).startOf('month')
  if (props.end && n > maxMonthNum.value) return dayjs(props.end).startOf('month')
  return d.startOf('month')
}
watch(() => [props.start, props.end], ([s]) => {
  const base = s ? dayjs(s as string) : current.value
  current.value = clampMonth(base)
}, { immediate: true })

const weekLabels = ['一', '二', '三', '四', '五', '六', '日']
const currentLabel = computed(() => current.value.format('YYYY年MM月'))
const curMonthNum = computed(() => monthNum(current.value))
const disabledPrev = computed(() => curMonthNum.value <= minMonthNum.value)
const disabledNext = computed(() => curMonthNum.value >= maxMonthNum.value)

// 将 sessions 预聚合到日期 -> 文本数组
const slotsByDate = computed<Record<string, string[]>>(() => {
  const map: Record<string, string[]> = {}
  for (const s of props.sessions || []) {
    if (!s?.date || !s?.startTime || !s?.endTime) continue
    const key = s.date
    ;(map[key] = map[key] || []).push(`${s.startTime}-${s.endTime}`)
  }
  // 每日时间去重并按时间排序
  for (const k of Object.keys(map)) {
    map[k] = Array.from(new Set(map[k])).sort()
  }
  return map
})

// 生成当月网格（6行 x 7列，含前后月补齐）
const grid = computed(() => {
  const year = current.value.year()
  const month = current.value.month() + 1
  const first = dayjs(`${year}-${String(month).padStart(2, '0')}-01`)
  const startWeekday = (first.day() + 6) % 7 // 将周一视为0
  const startCellDate = first.subtract(startWeekday, 'day')
  const cells: Array<{ key: string; day: number; inMonth: boolean; isToday: boolean; slots: string[] }>= []
  for (let i = 0; i < 42; i++) {
    const d = startCellDate.add(i, 'day')
    const key = d.format('YYYY-MM-DD')
    cells.push({
      key,
      day: d.date(),
      inMonth: d.month() === first.month(),
      isToday: d.isSame(dayjs(), 'day'),
      slots: slotsByDate.value[key] || []
    })
  }
  return cells
})

function toPrevMonth() { if (disabledPrev.value) return; current.value = clampMonth(current.value.subtract(1, 'month')) }
function toNextMonth() { if (disabledNext.value) return; current.value = clampMonth(current.value.add(1, 'month')) }
function toCurrentMonth() { const today = dayjs().startOf('month'); current.value = clampMonth(today) }
</script>

<style scoped>
.simple-calendar { width: 100%; box-sizing: border-box; }
.sc-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.sc-title { font-weight: 600; font-size: 16px; }

.sc-week { display: grid; grid-template-columns: repeat(7, 1fr); background: #f7f8fa; border-radius: 6px; overflow: hidden; }
.sc-week-item { text-align: center; padding: 6px 0; color: #909399; font-size: 12px; }

.sc-grid { display: grid; grid-template-columns: repeat(7, 1fr); grid-auto-rows: 110px; gap: 6px; margin-top: 6px; }
.sc-cell { border: 1px solid #ebeef5; border-radius: 8px; padding: 6px; overflow: hidden; background: #fff; }
.sc-cell.is-other { background: #fafafa; color: #bbb; }
.sc-cell.is-today { border-color: #409eff; box-shadow: inset 0 0 0 1px rgba(64,158,255,0.35); }
.sc-date { font-size: 12px; color: #606266; margin-bottom: 4px; }
.sc-slots { display: grid; grid-auto-rows: 22px; row-gap: 4px; overflow: auto; max-height: 80px; }
.sc-slot { font-size: 12px; color: #303133; background: #ecf5ff; border-radius: 4px; padding: 0 6px; line-height: 22px; white-space: nowrap; text-overflow: ellipsis; overflow: hidden; }

@media (max-width: 768px) {
  .sc-grid { grid-auto-rows: 90px; }
  .sc-slot { font-size: 11px; }
}
</style>

