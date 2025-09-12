<template>
  <el-dialog v-model="visible" :title="title" width="860px">
    <div class="toolbar">
      <el-alert type="info" :closable="false" show-icon title="请选择某天的基础段，然后选择具体30分钟开始时间" />
    </div>

    <div v-if="loading" class="loading"><el-skeleton :rows="5" animated /></div>

    <div v-else class="month-block">
      <div class="header">
        <el-select v-model="year" size="small" style="width: 100px" @change="reload">
          <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
        </el-select>
        <el-select v-model="month" size="small" style="width: 100px; margin-left: 8px" @change="reload">
          <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
        </el-select>
      </div>

      <div class="days">
        <div v-for="d in days" :key="d.date" class="day-card">
          <div class="date">{{ d.label }}</div>
          <BaseSlotBar :slots="d.slots" mode="student" @click-slot="(slot,item)=>onPickSlot(d, slot, item)" />
          <div v-if="selected.date===d.date && selected.slot" class="half-hour">
            <el-select v-model="selected.start" placeholder="选择开始时间" size="small" style="width: 160px;">
              <el-option v-for="opt in halfHourOptions(d, selected.slot || '')" :key="opt" :label="opt" :value="opt" />
            </el-select>
            <span class="end">结束：{{ endTimeFromStart(selected.start) }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" :disabled="!selected.date || !selected.start" @click="confirm">确认预约</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import dayjs from 'dayjs'
import BaseSlotBar from './BaseSlotBar.vue'

const props = defineProps<{ teacherId: number; title?: string }>()

const visible = ref(false)
const title = ref(props.title || '选择试听时间（按日历）')
const loading = ref(false)
const year = ref(dayjs().year())
const month = ref(dayjs().month() + 1)

const days = ref<Array<{ date: string; label: string; slots: Array<{slot:string; status:string; tips?:string}> }>>([])

const selected = ref<{ date: string | null; slot: string | null; start: string | null }>({ date: null, slot: null, start: null })

const yearOptions = Array.from({length: 4}).map((_,i)=> dayjs().year() -1 + i)

const BASE_SLOTS = ['08:00-10:00','10:00-12:00','13:00-15:00','15:00-17:00','17:00-19:00','19:00-21:00']

// API 走 teacherAPI.getMonthlyCalendar，避免多次请求
// 这里直接用 fetch 来避免对项目别名的依赖；实际项目中应使用已封装的 apiRequest
async function getMonthlyCalendar(teacherId: number, y: number, m: number) {
  const url = `/api/calendar/teacher/${teacherId}/month?year=${y}&month=${m}`
  const res = await fetch(url, { credentials: 'include' })
  return res.json()
}

const buildMonthDays = (y: number, m: number) => {
  const start = dayjs(`${y}-${String(m).padStart(2,'0')}-01`)
  const end = start.endOf('month')
  const res: any[] = []
  for (let d = 0; d < end.date(); d++) {
    const date = start.add(d,'day')
    res.push({
      date: date.format('YYYY-MM-DD'),
      label: date.format('MM-DD ddd'),
      slots: BASE_SLOTS.map(s => ({ slot: s, status: 'unavailable' }))
    })
  }
  days.value = res
}

const reload = async () => {
  loading.value = true
  buildMonthDays(year.value, month.value)
  try {
    const cal = await getMonthlyCalendar(props.teacherId, year.value, month.value)
    if (cal?.data?.dates) {
      const map: Record<string, any> = {}
      for (const d of cal.data.dates) map[d.date] = d.slots
      for (const day of days.value) {
        const slots = map[day.date] || []
        const statusBy: Record<string, any> = Object.fromEntries(slots.map((s: any)=> [s.slot, s]))
        day.slots = BASE_SLOTS.map(s => statusBy[s] || { slot: s, status: 'unavailable' })
      }
    }
  } finally {
    loading.value = false
  }
}

const onPickSlot = (day: any, slot: string, item: any) => {
  if (item.status==='busy_formal' || item.status==='unavailable') return
  selected.value = { date: day.date, slot, start: null }
}

const halfHourOptions = (day: any, slot: string) => {
  const [startStr, endStr] = slot.split('-')
  const start = dayjs(`${day.date} ${startStr}`)
  const end = dayjs(`${day.date} ${endStr}`)
  const res: string[] = []
  let cur = start
  while (cur.add(30,'minute').valueOf() <= end.valueOf()) {
    res.push(cur.format('HH:mm'))
    cur = cur.add(30,'minute')
  }
  return res
}

const endTimeFromStart = (start: string | null) => {
  if (!selected.value.date || !selected.value.slot || !start) return '--:--'
  return dayjs(`${selected.value.date} ${start}`).add(30,'minute').format('HH:mm')
}

const emit = defineEmits<{ (e:'confirm', payload: { date: string; startTime: string; endTime: string }): void }>()

const confirm = () => {
  if (!selected.value.date || !selected.value.start) return
  const date = selected.value.date
  const startTime = selected.value.start
  const endTime = dayjs(`${date} ${startTime}`).add(30,'minute').format('HH:mm')
  emit('confirm', { date, startTime, endTime })
  visible.value = false
}

const open = () => { visible.value = true }

// expose
// @ts-ignore
defineExpose({ open })

reload()
</script>

<style scoped>
.toolbar { margin-bottom: 8px; }
.header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.days { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
@media (min-width: 992px) { .days { grid-template-columns: repeat(3, 1fr); } }
.day-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px; }
.date { font-weight: 600; margin-bottom: 6px; }
.half-hour { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.half-hour .end { color: #909399; font-size: 12px; }
.loading { padding: 12px; }
</style>

