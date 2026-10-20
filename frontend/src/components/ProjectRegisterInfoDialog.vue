<template>
  <el-dialog
    v-model="state.visible"
    :title="title"
    width="980px"
    destroy-on-close
    class="register-dialog"
  >
    <div
      v-loading="state.loading"
      class="register-form"
    >
      <section
        v-for="section in state.sections"
        :key="section.title"
        class="register-section"
      >
        <h4 class="register-section-title">
          {{ section.title }}
        </h4>
        <el-row :gutter="16">
          <el-col
            v-for="field in section.fields"
            :key="field.label"
            :span="field.span ?? 12"
          >
            <div class="register-item">
              <span class="register-item-label">{{ field.label }}：</span>
              <div class="register-item-body">
                <template v-if="field.kind === 'check'">
                  <el-checkbox-group
                    :model-value="field.checked ?? []"
                    class="register-check-group"
                  >
                    <el-checkbox
                      v-for="option in field.options ?? []"
                      :key="option"
                      :value="option"
                      disabled
                    >
                      {{ option }}
                    </el-checkbox>
                  </el-checkbox-group>
                  <div
                    v-if="field.showOther"
                    class="register-other"
                  >
                    <span class="register-item-label">其他：</span>
                    <el-input
                      :model-value="field.other ?? ''"
                      disabled
                    />
                  </div>
                </template>
                <el-input
                  v-else
                  :model-value="field.value"
                  disabled
                />
              </div>
            </div>
          </el-col>
        </el-row>
      </section>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { projectRegisterState } from '@/utils/entityDialogs'

const state = projectRegisterState

const title = computed(
  () => `项目登记信息${state.bh ? ` 【${state.bh}】` : ''}`,
)
</script>

<style scoped>
.register-form {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 6px;
}

.register-section {
  margin-bottom: 6px;
}

.register-section-title {
  margin: 0 0 10px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.register-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.register-item-label {
  flex-shrink: 0;
  width: 96px;
  color: #606266;
  font-size: 13px;
  text-align: right;
  white-space: nowrap;
}

.register-item-body {
  flex: 1;
  min-width: 0;
}

.register-check-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0 12px;
}

.register-check-group :deep(.el-checkbox) {
  margin-right: 0;
}

.register-other {
  display: flex;
  align-items: center;
  margin-top: 4px;
}
</style>
