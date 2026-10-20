<template>
  <el-dialog
    v-model="state.visible"
    :title="title"
    width="920px"
    destroy-on-close
  >
    <div
      v-if="state.row"
      class="sample-form"
    >
      <el-form
        label-width="110px"
        label-position="left"
        inline
      >
        <el-form-item label="样品编号">
          <el-input
            :model-value="asText(state.row.bh)"
            disabled
          />
        </el-form-item>
        <el-form-item label="样品名称">
          <el-input
            :model-value="asText(state.row.productName)"
            disabled
          />
        </el-form-item>
        <el-form-item
          v-if="state.type === 'div'"
          label="项目登记备注"
        >
          <el-input
            :model-value="asText(state.row.remarks_register)"
            disabled
          />
        </el-form-item>
        <el-form-item
          v-if="state.type === 'div'"
          label="项目分解备注"
        >
          <el-input
            :model-value="asText(state.row.remarks_div)"
            disabled
          />
        </el-form-item>
      </el-form>
    </div>

    <div
      v-if="state.libInfo"
      class="lib-info"
    >
      【项目检验库编号：{{ state.libInfo.id }}，产品分类：{{ state.libInfo.prd_category }}，检验项目库名称：{{ state.libInfo.libName }}】
    </div>

    <el-table
      v-loading="state.loading"
      :data="state.rows"
      border
      stripe
      max-height="480"
    >
      <el-table-column
        type="index"
        label="序号"
        width="60"
        align="center"
      />
      <el-table-column
        prop="testItem"
        label="检测单项"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column
        prop="stdValue"
        label="指标要求"
        min-width="110"
        align="center"
      />
      <el-table-column
        v-if="state.type !== 'div'"
        prop="jyResult"
        label="检验结果"
        min-width="110"
        align="center"
      />
      <el-table-column
        v-if="state.type !== 'div'"
        prop="item_evaluate"
        label="单项评价"
        width="90"
        align="center"
      >
        <template #default="{ row }">
          <span
            :class="{ 'result-bad': row.item_evaluate === '不合格' }"
          >{{ row.item_evaluate || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column
        v-if="state.type !== 'div'"
        prop="jyResult_note"
        label="单项评价说明"
        min-width="110"
        show-overflow-tooltip
      />
      <el-table-column
        v-if="state.type !== 'div'"
        prop="userId_jyy"
        label="检验员"
        width="90"
        align="center"
      >
        <template #default="{ row }">
          {{ state.userNames[asText(row.userId_jyy)] || row.userId_jyy || '—' }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="state.type !== 'div'"
        prop="date_jyy"
        label="检验完成日期"
        width="120"
        align="center"
      >
        <template #default="{ row }">
          {{ row.date_jyy || '—' }}
        </template>
      </el-table-column>
      <el-table-column
        prop="basis"
        label="检验依据"
        min-width="160"
        show-overflow-tooltip
      />
      <el-table-column
        prop="mathod"
        label="检验方法"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column
        prop="method_note"
        label="方法备注"
        min-width="120"
        show-overflow-tooltip
      />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { asText, projectDetailState } from '@/utils/entityDialogs'

const state = projectDetailState

const titles: Record<string, string> = {
  div: '项目分解信息',
  arrange: '项目安排信息',
  inspect: '项目检验信息',
}

const title = computed(
  () => `${titles[state.type] ?? ''}${state.bh ? ` 【${state.bh}】` : ''}`,
)
</script>

<style scoped>
.sample-form {
  margin-bottom: 12px;
}

.sample-form :deep(.el-form-item) {
  margin-bottom: 4px;
}

.lib-info {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  color: #7a1f1f;
  font-size: 13px;
}

.result-bad {
  color: #dc2626;
  font-weight: 600;
}
</style>
