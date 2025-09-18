<template>
  <div class="calendar-month">
    <div class="header" v-if="!hideHeader">
      <el-select v-model="localYear" size="small" style="width: 100px" @change="reload">
        <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
      </el-select>
      <el-select v-model="localMonth" size="small" style="width: 100px; margin-left: 8px" @change="reload">
        <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
      </el-select>
      <div class="spacer" />
      <slot name="extra" />
    </div>

    <el-alert v-if="mode==='student'" type="info" :closable="false" show-icon
              title="说明：1.5小时固定为每个2小时时间段的中间90分钟（如 08:15-09:45）；若该基础段内存在任意30分钟试听（已排或待审批），该时间段不可预约正式课。" />

    <div v-if="loading" class="loading"><el-skeleton :rows="5" animated /></div>

    <div v-else class="calendar-grid-wrapper">
      <div class="week-header">
        <div v-for="(w, idx) in 7" :key="idx" class="week-head-cell">{{ weekLabels[idx] }}</div>
      </div>
      <div class="month-grid">
        <div
          v-for="cell in calendarCells"
          :key="cell.key"
          class="day-card"
          :class="{ 'is-other': cell.isOtherMonth, 'is-past': dayjs(cell.date).isBefore(dayjs().startOf('day')) }"
        >
          <div class="date">
            <span class="date-number">{{ dayjs(cell.date).date() }}</span>
          </div>
          <BaseSlotBar
            v-if="!cell.isOtherMonth && !dayjs(cell.date).isBefore(dayjs().startOf('day'))"
            :slots="cell.slots"
            :mode="mode"
            :selected="mode==='teacher' ? (teacherSelected[cell.date] || []) : ((props.durationMinutes===90) ? (selectedHalfSlots[cell.date] || []) : (selectedBaseSlots[cell.date] || []))"
            :duration-minutes="mode==='student' ? (props.durationMinutes || 90) : undefined"
            @click-slot="onClickSlot(cell, $event)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch, computed } from 'vue'
import dayjs from 'dayjs'
import BaseSlotBar from './BaseSlotBar.vue'
import { teacherAPI } from '@/utils/api'

const BASE_SLOTS = ['08:00-10:00','10:00-12:00','13:00-15:00','15:00-17:00','17:00-19:00','19:00-21:00']

type Mode = 'teacher' | 'student'

const props = defineProps<{
  teacherId: number
  year: number
  month: number // 1-12
  mode: Mode
  durationMinutes?: 90 | 120 // student only
  allowedPeriods?: Array<'morning'|'afternoon'|'evening'>
  dateRangeStart?: string // 'YYYY-MM-DD'
  dateRangeEnd?: string   // 'YYYY-MM-DD'
  hideHeader?: boolean // 隐藏顶部年月选择器
  studentSingleSelect?: boolean // 学生模式下是否单选（默认单选）
}>()

const emit = defineEmits<{
  (e: 'change-teacher-selection', payload: Record<string, string[]>): void
  (e: 'change-student-sessions', payload: Array<{ date: string; startTime: string; endTime: string }>): void
  (e: 'clicked-date', payload: string): void
}>()

const localYear = ref(props.year)
const localMonth = ref(props.month)
const loading = ref(false)

// month days collection
const days = ref<Array<{ key:string; date: string; label: string; slots: Array<{slot: string; status: string; tips?: string}> }>>([])
// 保留未过滤的原始slots，便于根据偏好动态二次过滤而无需重新请求
const rawSlotsByDate = ref<Record<string, Array<{slot: string; status: string; tips?: string}>>>({})

// teacher mode selection map
const teacherSelected = reactive<Record<string, string[]>>({})

// student mode selected sessions

// 周几显示与格式化（中文）
const weekdayName = (dateStr: string) => {
  const w = dayjs(dateStr).day() // 0=周日
  const map = ['周日','周一','周二','周三','周四','周五','周六']
  return map[w]
}
const weekdayClass = (dateStr: string) => {
  const w = dayjs(dateStr).day()
  return (w === 0 || w === 6) ? 'is-weekend' : 'is-weekday'
}
const formatDate = (dateStr: string) => dayjs(dateStr).format('MM-DD')

// 周视图头部与对齐填充
const weekLabels = ['周一','周二','周三','周四','周五','周六','周日']

const calendarCells = computed(() => {
  const y = localYear.value
  const m = localMonth.value
  const start = dayjs(`${y}-${String(m).padStart(2,'0')}-01`)
  const end = start.endOf('month')
  const leading = (start.day() + 6) % 7 // 以周一为一周起点
  const trailing = (7 - ((leading + end.date()) % 7)) % 7

  const arr: Array<{ key: string; date: string; isOtherMonth: boolean; slots?: Array<{slot: string; status: string; tips?: string}> }> = []
  for (let i = leading; i > 0; i--) {
    const d = start.subtract(i, 'day')
    arr.push({ key: `prev-${d.format('YYYY-MM-DD')}`, date: d.format('YYYY-MM-DD'), isOtherMonth: true })
  }
  for (const d of days.value) {
    arr.push({ key: d.key, date: d.date, isOtherMonth: false, slots: d.slots })
  }
  for (let i = 1; i <= trailing; i++) {
    const d = end.add(i, 'day')
    arr.push({ key: `next-${d.format('YYYY-MM-DD')}`, date: d.format('YYYY-MM-DD'), isOtherMonth: true })
  }
  return arr
})


const studentSessions = ref<Array<{ date: string; startTime: string; endTime: string }>>([])


// 学生模式下：将已选 sessions 映射为按天的基础段，用于高亮显示
const toMin = (t: string) => { const [h, m] = t.split(':').map(Number); return h * 60 + m }
const inferBaseSlot = (startTime: string, endTime: string): string | null => {
  const s = toMin(startTime); const e = toMin(endTime)
  for (const base of BASE_SLOTS) {
    const [bs, be] = base.split('-'); const bsMin = toMin(bs); const beMin = toMin(be)
    // 2小时完整覆盖
    if (s === bsMin && e === beMin) return base
    // 1.5小时：必须为基础段的中间90分钟（bs+15 ~ be-15）
    if ((e - s) === 90 && s === bsMin + 15 && e === beMin - 15) return base
  }
  return null
}
const selectedBaseSlots = computed<Record<string, string[]>>(()=>{
  const map: Record<string, string[]> = {}
  for (const sess of studentSessions.value) {
    const base = inferBaseSlot(sess.startTime, sess.endTime)
    if (!base) continue
    if (!map[sess.date]) map[sess.date] = []
    if (!map[sess.date].includes(base)) map[sess.date].push(base)
  }
  return map
})

// 1.5h（中心固定段）
const selectedHalfSlots = computed<Record<string, string[]>>(()=>{
  const map: Record<string, string[]> = {}
  for (const sess of studentSessions.value) {
    const base = inferBaseSlot(sess.startTime, sess.endTime)
    if (!base) continue
    if (!map[sess.date]) map[sess.date] = []
    if (!map[sess.date].includes(base)) map[sess.date].push(base)
  }
  return map
})


const yearOptions = Array.from({length: 4}).map((_,i)=> dayjs().year() -1 + i)

const buildMonthDays = (y: number, m: number) => {
  const start = dayjs(`${y}-${String(m).padStart(2,'0')}-01`)
  const end = start.endOf('month')
  const res: any[] = []
  for (let d = 0; d < end.date(); d++) {
    const date = start.add(d,'day')
    res.push({
      key: date.format('YYYY-MM-DD'),
      date: date.format('YYYY-MM-DD'),
      label: date.format('MM-DD ddd'),
      slots: BASE_SLOTS.map(s => ({ slot: s, status: 'unavailable' }))
    })
  }
  days.value = res
}

const applyStudentFilters = () => {
  if (props.mode !== 'student') return
  for (const day of days.value) {
    const original = rawSlotsByDate.value[day.date] || []

    // 过去日期过滤：不显示过去的日期
    if (dayjs(day.date).isBefore(dayjs().startOf('day'))) {
      day.slots = []
      continue
    }

    // 日期范围过滤
    if ((props.dateRangeStart && day.date < props.dateRangeStart) || (props.dateRangeEnd && day.date > props.dateRangeEnd)) {
      day.slots = []
      continue
    }

    // 今天的时间段过滤：只显示未来的时间段
    if (dayjs(day.date).isSame(dayjs(), 'day')) {
      const currentHour = dayjs().hour()
      day.slots = original.filter((it: any) => {
        const start = String(it.slot || '').split('-')[0]
        const h = parseInt((start || '00').split(':')[0], 10)
        return h > currentHour
      })
    } else {
      // 未来日期：应用时间段偏好过滤
      if (props.allowedPeriods && props.allowedPeriods.length > 0) {
        day.slots = original.filter((it: any) => {
          const start = String(it.slot || '').split('-')[0]
          const h = parseInt((start || '00').split(':')[0], 10)
          let p: 'morning' | 'afternoon' | 'evening' | '' = ''
          if (h >= 8 && h < 12) p = 'morning'
          else if (h >= 13 && h < 17) p = 'afternoon'
          else if (h >= 17 && h < 21) p = 'evening'
          return p ? (props.allowedPeriods as any).includes(p) : false
        })
      } else {
        day.slots = [...original]
      }
    }
  }
}

const reload = async () => {
  loading.value = true
  buildMonthDays(localYear.value, localMonth.value)
  try {
    const cal = await teacherAPI.getMonthlyCalendar(props.teacherId, localYear.value, localMonth.value)
    // group API results by date（兼容后端直接返回VO或包一层data）
    let grouped: Record<string, Array<{ slot: string; status: string; tips?: string }>> = {}
    const slotsA: any[] | null = Array.isArray((cal as any)?.slots) ? (cal as any).slots
      : (Array.isArray((cal as any)?.data?.slots) ? (cal as any).data.slots : null)
    if (slotsA) {
      for (const s of slotsA) {
        const dateStr = typeof s.date === 'string' ? s.date : (s.date?.toString?.() || '')
        if (!dateStr) continue
        ;(grouped[dateStr] = grouped[dateStr] || []).push({ slot: s.slot, status: s.status, tips: s.tips })
      }
    } else if (Array.isArray((cal as any)?.data?.dates)) {
      for (const d of (cal as any).data.dates as Array<any>) grouped[d.date] = d.slots
    }

    // Fallback: 月历接口无数据时，使用公开课表构建本月基础段状态
    if (Object.keys(grouped).length === 0) {
      const start = dayjs(`${localYear.value}-${String(localMonth.value).padStart(2,'0')}-01`)
      const end = start.endOf('month')
      try {
        const ps = await teacherAPI.getPublicSchedule(props.teacherId, { startDate: start.format('YYYY-MM-DD'), endDate: end.format('YYYY-MM-DD') })
        if (ps?.success && ps.data && Array.isArray(ps.data.daySchedules)) {
          grouped = {}
          for (const day of ps.data.daySchedules as Array<any>) {
            const dateStr = day.date
            const arr: Array<{ slot: string; status: string; tips?: string }> = []
            for (const t of (day.timeSlots || [])) {
              const slot = t.timeSlot
              if (!slot || !BASE_SLOTS.includes(slot)) continue
              let status = 'unavailable'
              if (t.available === true) status = 'available'
              else if (t.booked === true) status = 'busy_formal'
              arr.push({ slot, status })
            }
            if (arr.length > 0) grouped[dateStr] = arr
          }
          console.log('[CalendarMonth][FallbackPublicSchedule] grouped keys:', Object.keys(grouped).length)
        }
      } catch (e) {
        console.warn('[CalendarMonth] Fallback to public schedule failed:', e)
      }
    }
    // 每次重载重置原始状态
    rawSlotsByDate.value = {}
    for (const day of days.value) {
      const slots = grouped[day.date] || []
      const statusBy = Object.fromEntries(slots.map((s: any)=> [s.slot, s]))
      // 默认无数据时按“不可用”呈现，避免后端未返回时误判为全可预约
      const computed = BASE_SLOTS.map(s => statusBy[s] || { slot: s, status: 'unavailable' })
      rawSlotsByDate.value[day.date] = computed
      day.slots = [...computed]
    }
    // 学生模式：二次应用偏好过滤
    applyStudentFilters()

    if (props.mode === 'teacher') {
    // 当 getDailyAvailability 返回空时，使用月历可用状态作为回显兜底
    const fallbackSelected: Record<string, string[]> = {}
    for (const day of days.value) {
      const list = (grouped[day.date] || []) as Array<{slot:string; status:string}>
      const avail = list.filter(s=> s.status==='available').map(s=> s.slot)
      if (avail.length) fallbackSelected[day.date] = avail
    }

      // seed teacher selection from daily availability
      const start = `${localYear.value}-${String(localMonth.value).padStart(2,'0')}-01`
      const end = dayjs(start).endOf('month').format('YYYY-MM-DD')
      const avail = await teacherAPI.getDailyAvailability(props.teacherId, start, end)
      Object.keys(teacherSelected).forEach(k=> delete teacherSelected[k])
      let used = false
      if (avail?.data && Object.keys(avail.data as any).length > 0) {
        for (const [date, slots] of Object.entries(avail.data as Record<string, string[]>)) {
          teacherSelected[date] = [...(slots as string[])]
        }
        used = true
      }
      if (!used) {
        // 兜底：以月历接口返回的“available”状态作为回显
        for (const [date, slots] of Object.entries(fallbackSelected)) {
          teacherSelected[date] = [...slots]
        }
      }
      emit('change-teacher-selection', teacherSelected)
    }
  } finally {
    loading.value = false
  }
}

onMounted(reload)
watch(() => [props.teacherId, props.mode], reload)
// 偏好变化时仅进行本地过滤应用，避免不必要的网络请求
watch(() => [props.allowedPeriods?.join(','), props.dateRangeStart, props.dateRangeEnd], () => {
  applyStudentFilters()
})

function applySelectionPatch(patch: Record<string, string[]>, overwrite: boolean) {
  // 仅在教师模式下生效
  if (props.mode !== 'teacher') return
  for (const [date, slots] of Object.entries(patch || {})) {
    if (overwrite) {
      teacherSelected[date] = [...(slots || [])]
    } else {
      const set = new Set([...(teacherSelected[date] || []), ...(slots || [])])
      teacherSelected[date] = Array.from(set)
    }
  }
  emit('change-teacher-selection', teacherSelected)
}
function clearStudentSessions() {
  studentSessions.value = []
  emit('change-student-sessions', studentSessions.value)
}
function setStudentSessions(list: Array<{ date: string; startTime: string; endTime: string }>) {
  studentSessions.value = Array.isArray(list) ? [...list] : []
  emit('change-student-sessions', studentSessions.value)
}
// @ts-ignore
function getSelectableSessions(duration?: 90 | 120 | number) {
  const dur = (duration as any) || 90
  const res: Array<{ date: string; startTime: string; endTime: string }> = []
  for (const day of days.value) {
    const slots = (day as any).slots || []
    for (const it of slots) {
      const raw = String(it.slot || '')
      const [startStr, endStr] = raw.split('-')
      if (dur === 120) {
        if (it.status === 'busy_trial_base' || it.status === 'busy_formal' || it.status === 'pending_trial' || it.status === 'pending_formal' || it.status === 'unavailable') continue
        res.push({ date: day.date, startTime: startStr, endTime: endStr })
      } else {
        if (it.status === 'busy_formal' || it.status === 'pending_trial' || it.status === 'pending_formal' || it.status === 'unavailable' || it.status === 'busy_trial_base') continue
        const s = dayjs(`${day.date} ${startStr}`)
        const e = dayjs(`${day.date} ${endStr}`)
        const startTime = s.add(15, 'minute').format('HH:mm')
        const endTime = e.subtract(15, 'minute').format('HH:mm')
        res.push({ date: day.date, startTime, endTime })
      }
    }
  }
  return res
}

defineExpose({ reload, applySelectionPatch, clearStudentSessions, setStudentSessions, getSelectableSessions })

const onClickSlot = (day: any, slot: string) => {
  const rawSlot = slot
  const item = day.slots.find((s: any)=> s.slot === rawSlot)
  if (!item) return
  if (props.mode === 'teacher') {
    const arr = teacherSelected[day.date] || []
    const idx = arr.indexOf(rawSlot)
    if (idx >= 0) {
      arr.splice(idx, 1)
    } else {
      arr.push(rawSlot)
    }
    teacherSelected[day.date] = [...arr]
    emit('change-teacher-selection', teacherSelected)
  } else {
    // student: build concrete session by duration rule
    const duration = props.durationMinutes || 90
    const [startStr, endStr] = (rawSlot || '').split('-')
    let startTime = startStr
    let endTime = endStr
    if (duration === 120) {
      // 2h: must be whole base slot; block when trial/pending/formal/unavailable
      if (item.status === 'busy_trial_base' || item.status === 'busy_formal' || item.status === 'pending_trial' || item.status === 'pending_formal' || item.status === 'unavailable') return
    } else {
      // 1.5h: center-only; block when any trial exists in base slot or pending
      if (item.status === 'busy_formal' || item.status === 'pending_trial' || item.status === 'pending_formal' || item.status === 'unavailable' || item.status === 'busy_trial_base') return
      const s = dayjs(`${day.date} ${startStr}`)
      const e = dayjs(`${day.date} ${endStr}`)
      startTime = s.add(15, 'minute').format('HH:mm')
      endTime = e.subtract(15, 'minute').format('HH:mm')
    }
    const key = `${day.date}_${startTime}_${endTime}`
    const exists = studentSessions.value.find(i=> `${i.date}_${i.startTime}_${i.endTime}` === key)
    if (props.studentSingleSelect !== false) {
      // 单选：再次点击同一时段 => 取消；点击其他 => 仅保留当前
      if (exists) {
        studentSessions.value = []
      } else {
        studentSessions.value = [{ date: day.date, startTime, endTime }]
      }
    } else {
      // 多选：点击已选 => 移除；点击未选 => 累加
      if (exists) {
        studentSessions.value = studentSessions.value.filter(i=> `${i.date}_${i.startTime}_${i.endTime}` !== key)
      } else {
        studentSessions.value = [...studentSessions.value, { date: day.date, startTime, endTime }]
      }
    }
    emit('change-student-sessions', studentSessions.value)
  }
  emit('clicked-date', day.date)
}

</script>

<style scoped>
.calendar-month { display: flex; flex-direction: column; gap: 12px; }
.header { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.header .spacer { flex: 1; }
.calendar-grid-wrapper { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.week-header, .month-grid { display: grid; grid-template-columns: repeat(7, minmax(112px, 1fr)); gap: 8px; }
@media (max-width: 480px) { .week-header, .month-grid { grid-template-columns: repeat(7, minmax(96px, 1fr)); } }
.week-head-cell { text-align: center; font-weight: 600; color: #606266; padding: 6px 4px; }
.day-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px; box-sizing: border-box; background: #fff; overflow: visible; }
.day-card.is-other { background: #fafafa; color: #c0c4cc; pointer-events: none; }
.day-card.is-past { background: transparent; border-color: transparent; pointer-events: none; }
.day-card.is-past .date { display: none; }
.date { display: inline-flex; align-items: center; gap: 6px; font-weight: 600; margin-bottom: 6px; }
.date-number { font-variant-numeric: tabular-nums; }
.loading { padding: 12px; }
</style>

