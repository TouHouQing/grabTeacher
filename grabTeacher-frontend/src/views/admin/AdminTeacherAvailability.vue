<template>
  <div class="admin-teacher-availability">
    <h2>教师可上课时间（日历）</h2>
    <div class="toolbar">
      <el-input-number v-model="teacherId" :min="1" :step="1" placeholder="教师ID" />
    </div>

    <div v-if="!teacherId" class="hint">请输入教师ID</div>

    <TeacherAvailabilityEditor
      v-else
      :teacher-id="Number(teacherId)"
      :months="6"
      :admin-mode="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TeacherAvailabilityEditor from '@/components/scheduler/TeacherAvailabilityEditor.vue'

const teacherId = ref<number | null>(null)
</script>

<style scoped>
.admin-teacher-availability { padding: 12px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin: 8px 0 12px; }
.hint { color: #909399; margin: 8px 0; }

@media (max-width: 768px) {
  .toolbar { flex-direction: column; align-items: stretch; gap: 8px; }
  .toolbar :deep(.el-input-number),
  .toolbar :deep(.el-input),
  .toolbar :deep(.el-select) { width: 100% !important; }
  :deep(.el-dialog) { width: 100vw !important; max-width: 100vw !important; margin: 0 !important; }
}

</style>

