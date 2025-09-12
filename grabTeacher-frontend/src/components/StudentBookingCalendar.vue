<template>
  <StudentScheduler
    ref="dlg"
    :teacher-id="teacherId"
    :months="6"
    @confirm="confirmProxy"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import StudentScheduler from './scheduler/StudentScheduler.vue'

defineProps<{ teacherId: number; title?: string }>()
const emit = defineEmits<{ (e:'confirm', sessions: Array<{ date: string; startTime: string; endTime: string }>, duration: 90|120): void }>()
const dlg = ref<InstanceType<typeof StudentScheduler> | null>(null)

function confirmProxy(sessions: Array<{ date: string; startTime: string; endTime: string }>, duration: 90|120) {
  emit('confirm', sessions, duration)
}

const open = (opts?: { defaultDuration?: 90|120 }) => dlg.value?.open(opts)
// @ts-ignore
defineExpose({ open })
</script>

<style scoped></style>
