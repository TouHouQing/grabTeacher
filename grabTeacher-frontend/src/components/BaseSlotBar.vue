<template>
  <div class="base-slot-bar">
    <div class="slot-cell" v-for="s in slots" :key="s.slot">
      <!-- 学生 + 1.5h：仅允许基础2小时段的中间90分钟（如 08:15-09:45） -->
      <template v-if="mode==='student' && durationMinutes === 90">
        <el-tooltip :content="s.tips || statusText(s.status)" placement="top" :disabled="!shouldShowTooltip(s)">
          <el-button
            size="small"
            :class="['slot-btn', `status-${effectiveStatus(s)}`, { selected: selectedSet.has(s.slot) } ]"
            :disabled="disabledCenter(s)"
            @click="$emit('click-slot', s.slot, s)"
          >
            {{ centerLabel(s.slot) }}
          </el-button>
        </el-tooltip>
      </template>
      <!-- 其他情况（教师端或2小时）：仍使用基础段按钮 -->
      <template v-else>
        <el-tooltip :content="s.tips || statusText(s.status)" placement="top" :disabled="!shouldShowTooltip(s)">
          <el-button
            size="small"
            :class="['slot-btn', `status-${effectiveStatus(s)}`, { selected: selectedSet.has(s.slot) } ]"
            :disabled="disabledInMode(s)"
            @click="$emit('click-slot', s.slot, s)"
          >
            {{ shortSlot(s.slot) }}
          </el-button>
        </el-tooltip>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface SlotItem { slot: string; status: string; tips?: string }

const props = defineProps<{
  slots: SlotItem[]
  mode: 'teacher' | 'student'
  selected?: string[]
  durationMinutes?: 90 | 120
}>()

const emit = defineEmits<{
  (e: 'click-slot', slot: string, item: SlotItem): void
}>()

const selectedSet = computed(() => new Set(props.selected || []))


// 冲突与提示控制
const isConflict = (s: SlotItem) => s && ['busy_formal','busy_trial_base','pending_trial','pending_formal'].includes(s.status)
function shouldShowTooltip(s: SlotItem): boolean {
  // 学生端：冲突状态不展示任何提示
  if (props.mode === 'student' && isConflict(s)) return false
  // 其他情况按内容决定
  return !!(s?.tips || statusText(s?.status))
}

const disabledInMode = (s: SlotItem) => {
  // 学生端：统一将冲突（正式/试听/待审批）与不可用全部禁用
  if (props.mode === 'student') {
    if (s.status === 'unavailable' || s.status === 'busy_formal' || s.status === 'busy_trial_base' || s.status === 'pending_trial' || s.status === 'pending_formal') return true
    return false
  }
  // 教师/管理员端：禁止点击已有课程（正式课或基础段内有试听）以及待审批
  if (s.status === 'busy_formal' || s.status === 'busy_trial_base' || s.status === 'pending_trial' || s.status === 'pending_formal') return true
  return false
}

// 1.5h
const parseToMin = (t: string) => { const [h, m] = t.split(':').map(Number); return h * 60 + m }
const minToStr = (m: number) => `${String(Math.floor(m/60)).padStart(2,'0')}:${String(m%60).padStart(2,'0')}`
function centerLabel(slot: string): string {
  const [s, e] = slot.split('-')
  const sMin = parseToMin(s); const eMin = parseToMin(e)
  return `${minToStr(sMin + 15)}-${minToStr(eMin - 15)}`
}
function disabledCenter(s: SlotItem): boolean {
  // 已有正式课/教师未开放/基础段内有试听（含待审批）均禁用
  if (s.status === 'busy_formal' || s.status === 'unavailable' || s.status === 'busy_trial_base' || s.status === 'pending_trial' || s.status === 'pending_formal') return true
  return false
}

function effectiveStatus(s: SlotItem): string {
  if (props.mode === 'teacher') {
    // 冲突状态优先级最高：无论是否已选，均以冲突态展示
    if (s.status === 'busy_trial_base') return 'busy_formal' // 试听与正式同样标红
    if (s.status === 'busy_formal' || s.status === 'pending_trial' || s.status === 'pending_formal') return s.status
    // 非冲突：已选视为可预约；否则按不可预约展示
    if (selectedSet.value.has(s.slot)) return 'available'
    return 'unavailable'
  }
  // 学生端：所有冲突（正式/试听/待审批）一律按灰色不可选展示
  if (s.status === 'busy_formal' || s.status === 'busy_trial_base' || s.status === 'pending_trial' || s.status === 'pending_formal') return 'unavailable'
  return s.status || 'unavailable'
}



const shortSlot = (slot: string) => slot

const statusText = (status?: string) => {

  switch (status) {
    case 'available': return '可预约'
    case 'busy_formal': return '该时间段已有课程'
    case 'busy_trial_base': return '该时间段已有课程'
    case 'pending_trial': return '待审批试听（不可预约）'
    case 'pending_formal': return '待审批正式课（不可预约）'
    case 'unavailable': return ''
    default: return ''
  }
}
</script>

<style scoped>
.base-slot-bar { display: grid; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr)); gap: 6px; }
@media (max-width: 768px) { .base-slot-bar { grid-template-columns: repeat(auto-fill, minmax(84px, 1fr)); } }
@media (max-width: 400px) { .base-slot-bar { grid-template-columns: repeat(auto-fill, minmax(80px, 1fr)); } }
.slot-cell { width: 100%; }
.slot-btn { width: 100%; min-width: 0; padding: 6px 8px; font-size: 12px; line-height: 18px; box-sizing: border-box; touch-action: manipulation; -webkit-tap-highlight-color: transparent; }
@media (max-width: 480px) { .slot-btn { padding: 5px 6px; font-size: 11px; } }
.status-available.slot-btn { border-color: #67c23a; color: #2f8a1a; background-color: rgba(103, 194, 58, 0.12); }
.status-busy_formal.slot-btn { border-color: #f56c6c; color: #a93a3a; background-color: rgba(245, 108, 108, 0.12); }
.status-busy_trial_base.slot-btn { border-color: #f56c6c; color: #a93a3a; background-color: rgba(245, 108, 108, 0.12); }
.status-partial_trial.slot-btn { border-color: #eebe77; color: #9c7b2c; background-color: rgba(238, 190, 119, 0.12); }
.status-pending_trial.slot-btn { border-style: dashed; border-color: #e6a23c; color: #a87122; background-color: rgba(230, 162, 60, 0.08); }
.status-pending_formal.slot-btn { border-style: dashed; border-color: #e6a23c; color: #a87122; background-color: rgba(230, 162, 60, 0.08); }
.status-unavailable.slot-btn { border-color: #c0c4cc; color: #909399; background-color: #f5f7fa; }
.slot-btn.selected { background-color: #e8f3ff; border-color: #409eff; color: #1f6fd8; position: relative; }
.slot-btn.selected::after { content: '✓'; position: absolute; right: 6px; top: 2px; font-size: 11px; color: #409eff; }

.half-group { display: flex; gap: 6px; }
.half-group .slot-btn { flex: 1; }

</style>

