<template>
  <el-dialog
    v-model="state.visible"
    title="检验任务信息维护"
    width="960px"
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
        label-width="118px"
        class="task-form"
        @submit.prevent
      >
        <el-row :gutter="20">
          <el-col
            v-for="field in fields"
            :key="field.prop"
            :span="field.span ?? 12"
          >
            <el-form-item :label="field.label">
              <el-radio-group
                v-if="field.kind === 'radio'"
                :model-value="value(field.prop)"
                disabled
              >
                <el-radio
                  v-for="option in field.options ?? []"
                  :key="option"
                  :value="option"
                >
                  {{ option }}
                </el-radio>
              </el-radio-group>
              <el-input
                v-else
                :model-value="value(field.prop)"
                :type="field.kind === 'textarea' ? 'textarea' : 'text'"
                :rows="field.kind === 'textarea' ? 4 : undefined"
                disabled
              />
              <div
                v-if="field.hint"
                class="field-hint"
              >
                {{ field.hint }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { asText, taskInfoState } from '@/utils/entityDialogs'

interface TaskField {
  prop: string
  label: string
  hint?: string
  kind?: 'text' | 'textarea' | 'radio'
  options?: string[]
  span?: number
}

const state = taskInfoState

/** 字段与 sjtask 表列一一对应，顺序按旧系统「检验任务信息维护」表单排布 */
const fields: TaskField[] = [
  { prop: 'taskno', label: '任务编号', hint: '*必填项(不得超过11位数字)' },
  { prop: 'source', label: '任务来源', hint: '*必填项' },
  { prop: 'leader', label: '负责人', hint: '*必填项' },
  {
    prop: 'region',
    label: '任务级别（省市县级）',
    kind: 'radio',
    options: ['省级', '市级', '区级'],
    hint: '*必填项',
  },
  { prop: 'taskname', label: '任务名称', hint: '*必填项' },
  { prop: 'taskxz', label: '任务性质', hint: '监督抽检、委托送样、委托抽检' },
  { prop: 'picino', label: '任务下达批次' },
  { prop: 'rwdji', label: '任务等级', hint: '一般、重要、紧急、非常紧急' },
  { prop: 'xiaddate', label: '任务下达日期' },
  { prop: 'wancdate', label: '要求完成日期' },
  { prop: 'yxlyaoq', label: '阳性率要求' },
  {
    prop: 'chouyhj',
    label: '抽样环节',
    hint: '生产、流通、餐饮、养殖、种植、屠宰、加工……等等',
  },
  {
    prop: 'jcxm',
    label: '检测项目要求',
    hint: '监抽全项、非全项、根据文件等要求、实验室自行制定 其他要求 等',
  },
  { prop: 'date', label: '任务接受日期' },
  { prop: 'note', label: '任务描述', kind: 'textarea', span: 24 },
]

function value(prop: string): string {
  return asText((state.row ?? {})[prop])
}
</script>

<style scoped>
.dialog-alert {
  margin-bottom: 14px;
}

.task-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.task-form :deep(.el-form-item__label) {
  color: #606266;
}

.task-form :deep(.el-textarea__inner) {
  resize: vertical;
}

.field-hint {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}
</style>
