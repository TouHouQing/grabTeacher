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
            <el-select v-model="selected.start" placeholder="选择开始时间" size="small" style="width: 200px;">
              <el-option
                v-for="opt in halfHourOptions(d, selected.slot || '')"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
                :disabled="opt.disabled"
              />
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
// 可选：用户提示
// import { ElMessage } from 'element-plus'

import { teacherAPI, bookingAPI } from '@/utils/api'

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

// 每天每个基础段对应的“30分钟起始点（含禁用与原因）”缓存
type TrialOpt = { value: string; label: string; disabled: boolean }
const trialOptionMap = ref<Record<string, TrialOpt[]>>({})

// 统一使用已封装的 api.ts（携带 Bearer Token），避免 401
async function getMonthlyCalendar(teacherId: number, y: number, m: number) {
  return teacherAPI.getMonthlyCalendar(teacherId, y, m)
}

async function getDayAvailability(teacherId: number, date: string, segment?: string) {
  return bookingAPI.getDayAvailability(teacherId, date, segment)
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
    // 切月份时清理缓存
    trialOptionMap.value = {}
  } finally {
    loading.value = false
  }
}

function segmentFromSlot(slot: string): 'morning'|'afternoon'|'evening' {
  const start = slot.split('-')[0]
  if (start < '12:00') return 'morning'
  if (start < '17:00') return 'afternoon'
  return 'evening'
}

function isContained(child: string, parent: string): boolean {
  const [cs, ce] = child.split('-')
  const [ps, pe] = parent.split('-')
  return (cs >= ps) && (ce <= pe)
}

const reasonText = (reasons?: string[] | null) => {
  const r = Array.isArray(reasons) ? reasons : []
  const map: Record<string, string> = {
    busyScheduled: '与老师已有课程冲突',
    duplicateTrialSlot: '与试听占位冲突',
    teacherUnavailable: '教师未开放该时间段',
    pendingTrial: '该时段存在待审批试听',
    busy: '该时段已被占用'
  }
  const texts = r.map(k => map[k] || '不可预约')
  return texts.length ? `不可预约：${texts.join('、')}` : '不可预约'
}

async function loadTrialStarts(date: string, baseSlot: string) {
  const key = `${date}|${baseSlot}`
  if (trialOptionMap.value[key]) return
  const seg = segmentFromSlot(baseSlot)
  const res = await getDayAvailability(props.teacherId, date, seg)
  if (!res || !res.data) { trialOptionMap.value[key] = []; return }
  const slots = (res.data.trialSlots || []) as Array<{ slot: string; trialAvailable: boolean; reasons?: string[] }>
  const options: TrialOpt[] = []
  for (const s of slots) {
    if (!isContained(s.slot, baseSlot)) continue
    const start = s.slot.split('-')[0]
    options.push({ value: start, label: s.trialAvailable ? start : `${start}（${reasonText(s.reasons)}）`, disabled: !s.trialAvailable })
  }
  // 去重并按时间排序
  const uniq = new Map<string, TrialOpt>()
  for (const o of options) if (!uniq.has(o.value)) uniq.set(o.value, o)
  trialOptionMap.value[key] = Array.from(uniq.values()).sort((a,b)=> a.value.localeCompare(b.value))
}

const onPickSlot = async (day: any, slot: string, item: any) => {
  if (item.status==='busy_formal' || item.status==='unavailable') return
  selected.value = { date: day.date, slot, start: null }
  await loadTrialStarts(day.date, slot)
}

const halfHourOptions = (day: any, slot: string): TrialOpt[] => {
  const key = `${day.date}|${slot}`
  const list = trialOptionMap.value[key]
  return Array.isArray(list) ? list : []
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

