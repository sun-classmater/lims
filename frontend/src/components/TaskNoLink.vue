<template>
  <el-link
    v-if="taskno"
    type="primary"
    @click="handleClick"
  >
    {{ taskno }}
  </el-link>
  <span
    v-else
    class="entity-empty"
  >—</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import {
  asText,
  openTaskInfoDialog,
  type EntityDialogRow,
} from '@/utils/entityDialogs'

const props = withDefaults(
  defineProps<{
    row: EntityDialogRow
    prop?: string
  }>(),
  { prop: 'taskno' },
)

const taskno = computed(() => asText(props.row[props.prop]))

function handleClick() {
  if (taskno.value) {
    openTaskInfoDialog(taskno.value, props.row)
  }
}
</script>

<style scoped>
.entity-empty {
  color: #9ca3af;
}
</style>
