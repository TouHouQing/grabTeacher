<template>
  <el-dialog v-model="visible" :title="title" width="960px">
    <div class="toolbar">
      <el-radio-group v-model="duration" size="small">
        <el-radio-button :label="90">1.5 小时</el-radio-button>
        <el-radio-button :label="120">2 小时</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <span class="hint">提示：段起点有试听时，2 小时禁用；1.5 小时自动选后半段</span>
    </div>

    <div class="content">
      <div class="left">
        <CalendarMultiMonth
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
            <div class="t">{{ s.date }} {{ s.startTime }}-{{ s.endTime }}</div>
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
import { ref } from 'vue'
import CalendarMultiMonth from '@/components/CalendarMultiMonth.vue'

const props = defineProps<{ teacherId: number; title?: string; months?: number }>()

const visible = ref(false)
const title = ref(props.title || '选择上课时间（按日历）')
const duration = ref<90 | 120>(90)
const months = props.months || 6
const sessions = ref<Array<{ date: string; startTime: string; endTime: string }>>([])
const allowedPeriods = ref<Array<'morning'|'afternoon'|'evening'> | undefined>(undefined)
const dateRangeStart = ref<string | undefined>(undefined)
const dateRangeEnd = ref<string | undefined>(undefined)

const emit = defineEmits<{ (e:'confirm', sessions: Array<{ date: string; startTime: string; endTime: string }>, duration: 90|120): void }>()

function onChangeSessions(list: Array<{ date: string; startTime: string; endTime: string }>) {
  sessions.value = list
}

const open = (opts?: { defaultDuration?: 90|120; allowedPeriods?: Array<'morning'|'afternoon'|'evening'>; dateStart?: string; dateEnd?: string }) => {
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

// @ts-ignore
defineExpose({ open })
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.toolbar .spacer { flex: 1; }
.hint { color: #909399; font-size: 12px; }
.content { display: grid; grid-template-columns: 1fr 260px; gap: 12px; }
.right { border-left: 1px dashed #ebeef5; padding-left: 12px; }
.summary-title { font-weight: 600; margin-bottom: 8px; }
.session-list { max-height: 420px; overflow: auto; display: flex; flex-direction: column; gap: 6px; }
.item { font-size: 13px; color: #606266; padding: 6px 8px; background: #f7f9fb; border-radius: 6px; }
</style>

