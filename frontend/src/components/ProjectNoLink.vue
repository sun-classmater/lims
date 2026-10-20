<template>
  <el-link
    v-if="bh"
    type="primary"
    @click="handleClick"
  >
    {{ bh }}
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
  openProjectAuthDialog,
  type EntityDialogRow,
} from '@/utils/entityDialogs'

const props = withDefaults(
  defineProps<{
    row: EntityDialogRow
    prop?: string
  }>(),
  { prop: 'bh' },
)

const bh = computed(() => asText(props.row[props.prop]))

function handleClick() {
  if (bh.value) {
    openProjectAuthDialog(bh.value, props.row)
  }
}
</script>

<style scoped>
.entity-empty {
  color: #9ca3af;
}
</style>
