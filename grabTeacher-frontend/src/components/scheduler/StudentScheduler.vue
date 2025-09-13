<template>
  <el-dialog v-model="visible" :title="title" width="min(1360px, 98vw)" destroy-on-close @closed="onClosed">
    <div class="toolbar">
      <el-radio-group v-model="duration" size="small">
        <el-radio-button :label="90">1.5 小时</el-radio-button>
        <el-radio-button :label="120">2 小时</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <span class="hint">提示：1.5 小时可在每个 2 小时段内自由选择“前90”或“后90”；若段起点有试听，仅可选“后90”。</span>
    </div>

    <div class="content">
      <div class="left">
        <CalendarMultiMonth
          ref="calRef"
          :teacher-id="teacherId"
          :months="months"
          mode="student"
          :duration-minutes="duration"
          :allowed-periods="allowedPeriods"
          :date-range-start="dateRangeStart"
          :date-range-end="dateRangeEnd"
          @change-student-sessions="onChangeSessions"
        />
      </div>
      <div class="right">
        <div class="summary-title">已选择 ({{ sessions.length }})</div>
        <div class="session-list">
          <div v-for="(s, idx) in sessions" :key="idx" class="item">
            <div class="t">{{ formatSession(s) }}</div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" :disabled="sessions.length===0" @click="confirm">确认 ({{ sessions.length }} 次)</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')
import CalendarMultiMonth from '@/components/CalendarMultiMonth.vue'

const props = defineProps<{ teacherId: number; title?: string; months?: number }>()

const visible = ref(false)
const title = ref(props.title || '选择上课时间（按日历）')
const duration = ref<90 | 120>(90)
const months = props.months || 6
const sessions = ref<Array<{ date: string; startTime: string; endTime: string }>>([])
const calRef = ref<InstanceType<typeof CalendarMultiMonth> | null>(null)

function formatSession(s: { date: string; startTime: string; endTime: string }) {
  return `${dayjs(s.date).format('MM-DD（ddd）')} ${s.startTime}-${s.endTime}`
}
const allowedPeriods = ref<Array<'morning'|'afternoon'|'evening'> | undefined>(undefined)
const dateRangeStart = ref<string | undefined>(undefined)
const dateRangeEnd = ref<string | undefined>(undefined)

const emit = defineEmits<{ (e:'confirm', sessions: Array<{ date: string; startTime: string; endTime: string }>, duration: 90|120): void }>()

function onChangeSessions(list: Array<{ date: string; startTime: string; endTime: string }>) {
  sessions.value = list
}

const open = (opts?: { defaultDuration?: 90|120; allowedPeriods?: Array<'morning'|'afternoon'|'evening'>; dateStart?: string; dateEnd?: string }) => {
  // 每次打开前清空上次选择，避免残留
  sessions.value = []
  calRef.value?.clear?.()
  if (opts?.defaultDuration) duration.value = opts.defaultDuration
  allowedPeriods.value = opts?.allowedPeriods
  dateRangeStart.value = opts?.dateStart
  dateRangeEnd.value = opts?.dateEnd
  visible.value = true
}
const confirm = () => {
  emit('confirm', sessions.value, duration.value)
  visible.value = false
}

// 关闭对话框后，统一清空选择与过滤条件，确保切换老师或重新进入时无残留
function onClosed() {
  sessions.value = []
  calRef.value?.clear?.()
  allowedPeriods.value = undefined
  dateRangeStart.value = undefined
  dateRangeEnd.value = undefined
}

// 教师切换时，也清理已有选择（如果对话框未关闭，再次进入也不残留）
watch(() => props.teacherId, () => {
  sessions.value = []
  calRef.value?.clear?.()
})

// 切换 1.5h/2h 时，清空所有已选，避免跨时长残留
watch(duration, () => {
  sessions.value = []
  calRef.value?.clearAllStudentSessions?.()
})


// @ts-ignore
defineExpose({ open })
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.toolbar .spacer { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.content { display: grid; grid-template-columns: 1fr 200px; gap: 12px; height: calc(100vh - 200px); overflow: hidden; }
.left { min-height: 0; overflow: auto; }

.right { border-left: 1px dashed #ebeef5; padding-left: 12px; display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.summary-title { font-weight: 600; margin-bottom: 8px; }
.session-list { flex: 1; min-height: 0; overflow: auto; display: grid; grid-auto-rows: 32px; row-gap: 6px; padding-right: 0; }
.item { font-size: 12px; color: #606266; padding: 0 10px; background: #f7f9fb; border-radius: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 32px; height: 32px; box-sizing: border-box; max-width: 100%; }
</style>

