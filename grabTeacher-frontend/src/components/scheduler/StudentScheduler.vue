<template>
  <el-dialog v-model="visible" :title="title" :width="dialogWidth" :fullscreen="isSmall" destroy-on-close @closed="onClosed">
    <div class="toolbar">
      <el-radio-group v-model="duration" size="small">
        <el-radio-button :label="90">1.5 小时</el-radio-button>
        <el-radio-button :label="120">2 小时</el-radio-button>
      </el-radio-group>
      <div class="spacer" />
      <span class="hint">说明：1.5小时固定为每个2小时时间段的中间90分钟（如 08:15-09:45）；若该基础段内存在任意30分钟试听（已排或待审批），该时间段不可预约正式课。</span>
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
          :hide-header="true"
          :student-single-select="!props.multiSelect"
          @change-student-sessions="onChangeSessions"
        />
      </div>
      <div class="right">
        <div class="summary-title">已选择</div>
        <div class="session-list">
          <div v-for="(s, idx) in sessions" :key="idx" class="item">
            <div class="t">{{ formatSession(s) }}</div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dlg-footer">
        <div class="left">
          <el-tag type="info" effect="plain">请选择日历中的日期与时间段</el-tag>
        </div>
        <div class="right">
          <el-button @click="visible=false">取消</el-button>
          <el-button type="primary" :disabled="sessions.length===0" @click="confirm">确认</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
dayjs.locale('zh-cn')
import CalendarMultiMonth from '@/components/CalendarMultiMonth.vue'

const props = defineProps<{ teacherId: number; title?: string; months?: number; multiSelect?: boolean }>()

const visible = ref(false)

// 响应式：小屏适配（手机、小尺寸电脑）
const isSmall = ref(false)
let mql: MediaQueryList | null = null
let mediaHandler: any = null
onMounted(() => {
  try {
    mql = window.matchMedia('(max-width: 768px)')
    mediaHandler = (e: any) => { isSmall.value = (e?.matches ?? mql?.matches ?? false) }
    isSmall.value = !!mql?.matches
    if ((mql as any)?.addEventListener) (mql as any).addEventListener('change', mediaHandler)
    else if ((mql as any)?.addListener) (mql as any).addListener(mediaHandler)
  } catch {}
})
onBeforeUnmount(() => {
  try {
    if (mql && mediaHandler) {
      if ((mql as any)?.removeEventListener) (mql as any).removeEventListener('change', mediaHandler)
      else if ((mql as any)?.removeListener) (mql as any).removeListener(mediaHandler)
    }
  } catch {}
})

const dialogWidth = computed(() => isSmall.value ? '100vw' : 'min(1360px, 98vw)')
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
  // 根据是否允许多选，决定保留全部或仅保留最后一次
  if (props.multiSelect) {
    sessions.value = Array.isArray(list) ? [...list] : []
  } else {
    sessions.value = (list && list.length) ? [list[list.length - 1]] : []
  }
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
.content { display: grid; grid-template-columns: 1fr 200px; gap: 12px; height: calc(100vh - 220px); overflow: hidden; }
.left { min-height: 0; overflow: auto; }

.right { border-left: 1px dashed #ebeef5; padding-left: 12px; display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.summary-title { font-weight: 600; margin-bottom: 8px; }
.session-list { flex: 1; min-height: 0; overflow: auto; display: grid; grid-auto-rows: 32px; row-gap: 6px; padding-right: 0; }
.item { font-size: 12px; color: #606266; padding: 0 10px; background: #f7f9fb; border-radius: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 32px; height: 32px; box-sizing: border-box; max-width: 100%; }
.dlg-footer { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.dlg-footer .left { display: flex; align-items: center; gap: 8px; }
.dlg-footer .right { display: flex; gap: 10px; }

/* 小屏幕自适配：对话框、布局、提示文案、按钮区 */
@media (max-width: 1024px) {
  .content { grid-template-columns: 1fr 220px; height: calc(100vh - 200px); }
}
@media (max-width: 768px) {
  :deep(.el-dialog) { margin: 0 !important; }
  .content { grid-template-columns: 1fr; height: calc(100vh - 180px); }
  .right { border-left: 0; border-top: 1px dashed #ebeef5; padding-left: 0; padding-top: 8px; }
  .hint { display: none; }
  .dlg-footer { flex-direction: column; align-items: stretch; gap: 8px; }
  .dlg-footer .right { justify-content: flex-end; }
}
@media (max-width: 480px) {
  .content { height: calc(100vh - 160px); }
  .session-list { grid-auto-rows: 28px; }
}
</style>

