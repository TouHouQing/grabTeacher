<template>
  <div class="base-slot-bar">
    <div class="slot-cell" v-for="s in slots" :key="s.slot">
      <el-tooltip
        :content="s.tips || statusText(s.status)"
        placement="top"
        :disabled="!s.tips && !statusText(s.status)"
      >
        <el-button
          size="small"
          :class="['slot-btn', `status-${s.status}`, { selected: selectedSet.has(s.slot) } ]"
          :disabled="disabledInMode(s)"
          @click="$emit('click-slot', s.slot, s)"
        >
          {{ shortSlot(s.slot) }}
        </el-button>
      </el-tooltip>
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
}>()

const emit = defineEmits<{
  (e: 'click-slot', slot: string, item: SlotItem): void
}>()

const selectedSet = computed(() => new Set(props.selected || []))

const disabledInMode = (s: SlotItem) => {
  // 禁止选择：已排正式课
  if (s.status === 'busy_formal') return true
  // 学生端：不可用/unavailable 禁止；teacher 端允许点亮以发布可用
  if (props.mode === 'student' && (s.status === 'unavailable')) return true
  return false
}

const shortSlot = (slot: string) => slot

const statusText = (status?: string) => {
  switch (status) {
    case 'available': return '可预约'
    case 'busy_formal': return '已有正式课'
    case 'busy_trial_base': return '段起点有试听（2h禁用, 1.5h可）'
    case 'partial_trial': return '段内有试听（按后端最终校验）'
    case 'pending_trial': return '待审批试听'
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
.status-busy_trial_base.slot-btn { border-color: #e6a23c; color: #a87122; background-color: rgba(230, 162, 60, 0.12); }
.status-partial_trial.slot-btn { border-color: #eebe77; color: #9c7b2c; background-color: rgba(238, 190, 119, 0.12); }
.status-pending_trial.slot-btn { border-style: dashed; border-color: #e6a23c; color: #a87122; background-color: rgba(230, 162, 60, 0.08); }
.status-unavailable.slot-btn { border-color: #c0c4cc; color: #909399; background-color: #f5f7fa; }
.slot-btn.selected { background-color: #e8f3ff; border-color: #409eff; color: #1f6fd8; position: relative; }
.slot-btn.selected::after { content: '✓'; position: absolute; right: 6px; top: 2px; font-size: 11px; color: #409eff; }
</style>

