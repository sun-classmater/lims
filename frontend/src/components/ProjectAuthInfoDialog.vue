<template>
  <el-dialog
    v-model="state.visible"
    :title="title"
    width="820px"
    destroy-on-close
  >
    <div v-loading="state.loading">
      <el-alert
        v-if="state.error"
        :title="state.error"
        type="warning"
        show-icon
        :closable="false"
        class="dialog-alert"
      />
      <el-form
        label-width="120px"
        class="auth-form"
        @submit.prevent
      >
        <el-form-item label="项目编号">
          <el-input
            :model-value="state.bh"
            disabled
          />
        </el-form-item>
        <el-form-item
          v-for="field in fields"
          :key="field.label"
          :label="field.label"
        >
          <el-input
            :model-value="field.value"
            disabled
          />
        </el-form-item>
      </el-form>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { asText, projectAuthState } from '@/utils/entityDialogs'

const state = projectAuthState

const title = computed(
  () => `项目授权信息${state.bh ? ` 【${state.bh}】` : ''}`,
)

/**
 * 备注字段与 project 表列的对应关系（库内实际列名）：
 * remarks_register 登记、remarks_div 分解、remarks_arrange 安排、
 * remarks_agree 报告打印、remarks_verify 报告签发。
 * 说明：旧库这几个备注列当前基本为空，映射按环节名称就近对应，
 * 若后续确认原始系统另有对应关系，只需调整此处。
 */
const fields = computed(() => {
  const row = state.row ?? {}
  return [
    { label: '项目登记备注', value: asText(row.remarks_register) },
    { label: '项目分解备注', value: asText(row.remarks_div) },
    { label: '项目安排备注', value: asText(row.remarks_arrange) },
    { label: '报告打印备注', value: asText(row.remarks_agree) },
    { label: '报告签发备注', value: asText(row.remarks_verify) },
  ]
})
</script>

<style scoped>
.dialog-alert {
  margin-bottom: 14px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.auth-form :deep(.el-form-item__label) {
  color: #606266;
}
</style>
