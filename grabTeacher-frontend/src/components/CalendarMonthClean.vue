<template>
  <div class="calendar-month">
    <div class="header">
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
              title="说明：橙色表示段起点有试听（2小时禁用，1.5小时可选）；灰色为老师未开放；红色为已排课不可选。" />

    <div v-if="loading" class="loading"><el-skeleton :rows="5" animated /></div>

    <div v-else class="days">
      <div v-for="day in days" :key="day.key" class="day-card">
        <div class="date">{{ day.label }}</div>
        <BaseSlotBar
          :slots="day.slots"
          :mode="mode"
          :selected="(teacherSelected[day.date] || [])"
          @click-slot="onClickSlot(day, $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
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

// teacher mode selection map
const teacherSelected = reactive<Record<string, string[]>>({})

// student mode selected sessions
const studentSessions = ref<Array<{ date: string; startTime: string; endTime: string }>>([])

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

const reload = async () => {
  loading.value = true
  buildMonthDays(localYear.value, localMonth.value)
  try {
    const cal = await teacherAPI.getMonthlyCalendar(props.teacherId, localYear.value, localMonth.value)
    // group API results by date
    const grouped: Record<string, Array<{ slot: string; status: string; tips?: string }>> = {}
    if (cal?.data?.slots && Array.isArray((cal as any).data.slots)) {
      for (const s of (cal as any).data.slots as Array<any>) {
        const dateStr = typeof s.date === 'string' ? s.date : (s.date?.toString?.() || '')
        if (!dateStr) continue
        ;(grouped[dateStr] = grouped[dateStr] || []).push({ slot: s.slot, status: s.status, tips: s.tips })
      }
    } else if (cal?.data?.dates && Array.isArray((cal as any).data.dates)) {
      for (const d of (cal as any).data.dates as Array<any>) grouped[d.date] = d.slots
    }
    for (const day of days.value) {
      const slots = grouped[day.date] || []
      const statusBy = Object.fromEntries(slots.map((s: any)=> [s.slot, s]))
      day.slots = BASE_SLOTS.map(s => statusBy[s] || { slot: s, status: 'available' })
    }
    if (props.mode === 'teacher') {
      // seed teacher selection from daily availability
      const start = `${localYear.value}-${String(localMonth.value).padStart(2,'0')}-01`
      const end = dayjs(start).endOf('month').format('YYYY-MM-DD')
      const avail = await teacherAPI.getDailyAvailability(props.teacherId, start, end)
      Object.keys(teacherSelected).forEach(k=> delete teacherSelected[k])
      if (avail?.data) {
        for (const [date, slots] of Object.entries(avail.data as Record<string, string[]>)) {
          teacherSelected[date] = [...(slots as string[])]
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

const onClickSlot = (day: any, slot: string) => {
  const item = day.slots.find((s: any)=> s.slot === slot)
  if (!item) return
  if (props.mode === 'teacher') {
    const arr = teacherSelected[day.date] || []
    const idx = arr.indexOf(slot)
    if (idx >= 0) {
      arr.splice(idx, 1)
    } else {
      arr.push(slot)
    }
    teacherSelected[day.date] = [...arr]
    emit('change-teacher-selection', teacherSelected)
  } else {
    // student: build concrete session by duration rule
    const duration = props.durationMinutes || 90
    const [startStr, endStr] = slot.split('-')
    let startTime = startStr
    let endTime = endStr
    if (duration === 120) {
      // 2h:  must be whole base slot; block when busy_trial_base
      if (item.status === 'busy_trial_base' || item.status === 'busy_formal' || item.status === 'unavailable') return
    } else {
      // 1.5h: if busy_trial_base -> choose later 90m; else choose earlier 90m
      if (item.status === 'busy_formal' || item.status === 'unavailable') return
      const start = dayjs(`${day.date} ${startStr}`)
      const end = dayjs(`${day.date} ${endStr}`)
      if (item.status === 'busy_trial_base') {
        // later: end aligned 90m
        endTime = end.format('HH:mm')
        startTime = end.subtract(90, 'minute').format('HH:mm')
      } else {
        // earlier: start aligned 90m
        startTime = start.format('HH:mm')
        endTime = start.add(90, 'minute').format('HH:mm')
      }
    }
    const key = `${day.date}_${startTime}_${endTime}`
    const exists = studentSessions.value.find(i=> `${i.date}_${i.startTime}_${i.endTime}` === key)
    if (exists) {
      studentSessions.value = studentSessions.value.filter(i=> `${i.date}_${i.startTime}_${i.endTime}` !== key)
    } else {
      studentSessions.value = [...studentSessions.value, { date: day.date, startTime, endTime }]
    }
    emit('change-student-sessions', studentSessions.value)
  }
  emit('clicked-date', day.date)
}

</script>

<style scoped>
.calendar-month { display: flex; flex-direction: column; gap: 12px; }
.header { display: flex; align-items: center; gap: 8px; }
.header .spacer { flex: 1; }
.days { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
@media (min-width: 992px) { .days { grid-template-columns: repeat(3, 1fr); } }
.day-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px; }
.date { font-weight: 600; margin-bottom: 6px; }
.loading { padding: 12px; }
</style>

