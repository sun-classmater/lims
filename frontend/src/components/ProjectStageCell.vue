<template>
  <span
    v-if="visible"
    class="stage-info-link"
  >{{ label }}</span>
  <span
    v-else
    class="stage-empty"
  >—</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import {
  isStageInfoVisible,
  projectStageLabels,
  type ProjectStage,
  type ProjectStageRow,
} from '@/utils/projectStage'

const props = defineProps<{
  row: ProjectStageRow
  stage: ProjectStage
}>()

const visible = computed(() => isStageInfoVisible(props.row, props.stage))
const label = computed(() => projectStageLabels[props.stage])
</script>

<style scoped>
.stage-empty {
  color: #9ca3af;
}

.stage-info-link {
  color: #303133;
  cursor: pointer;
}

.stage-info-link:hover {
  color: #409eff;
}
</style>
