<template>
  <StudentScheduler
    ref="dlg"
    :teacher-id="teacherId"
    :months="12"
    :multi-select="multiSelect"
    @confirm="confirmProxy"
  />
</template>

<script setup lang="ts">
import { ref, defineAsyncComponent } from 'vue'
const StudentScheduler = defineAsyncComponent(() => import('./scheduler/StudentScheduler.vue'))

const props = withDefaults(defineProps<{ teacherId: number; title?: string; multiSelect?: boolean }>(), { multiSelect: true })
const emit = defineEmits<{ (e:'confirm', sessions: Array<{ date: string; startTime: string; endTime: string }>, duration: 90|120): void }>()
type SchedulerExpose = { open: (opts?: { defaultDuration?: 90|120; allowedPeriods?: Array<'morning'|'afternoon'|'evening'>; dateStart?: string; dateEnd?: string; preselectSessions?: Array<{ date: string; startTime: string; endTime: string }> }) => void }
const dlg = ref<SchedulerExpose | null>(null)
const multiSelect = props.multiSelect

function confirmProxy(sessions: Array<{ date: string; startTime: string; endTime: string }>, duration: 90|120) {
  emit('confirm', sessions, duration)
}

const open = (opts?: { defaultDuration?: 90|120; allowedPeriods?: Array<'morning'|'afternoon'|'evening'>; dateStart?: string; dateEnd?: string; preselectSessions?: Array<{ date: string; startTime: string; endTime: string }> }) => dlg.value?.open(opts)
defineExpose({ open })
</script>

<style scoped></style>
