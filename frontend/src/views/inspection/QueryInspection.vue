<template>
  <div class="query-inspection">
    <el-row :gutter="16">
      <el-col
        :xs="24"
        :md="14"
      >
        <el-card
          shadow="never"
          class="panel"
        >
          <h3 class="panel-title">
            待检验项目
          </h3>

          <div class="filter-line">
            <el-radio-group
              v-model="scope"
              @change="refreshAll"
            >
              <el-radio value="all">
                全部
              </el-radio>
              <el-radio value="mine">
                我负责的
              </el-radio>
            </el-radio-group>
            <el-button
              class="action-btn"
              :loading="itemsLoading"
              @click="openAllItems"
            >
              全部样品检验任务明细列表
            </el-button>
          </div>

          <el-table
            v-loading="pendingLoading"
            :data="pendingList"
            border
            stripe
            size="small"
            height="440"
            empty-text="没有找到匹配的记录"
          >
            <el-table-column
              prop="bh"
              label="样品编号"
              min-width="150"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <ProjectNoLink :row="row" />
              </template>
            </el-table-column>
            <el-table-column
              prop="productName"
              label="样品名称"
              min-width="90"
              show-overflow-tooltip
            />
            <el-table-column
              prop="date_yywc"
              label="预定日期"
              width="108"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_yywc) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              prop="issue_date"
              label="下达日期"
              width="108"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.issue_date) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              prop="date_sy"
              label="送样日期"
              width="108"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_sy) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              prop="date_cy"
              label="抽样日期"
              width="108"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_cy) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="退安排"
              width="88"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  class="row-btn"
                  :loading="actingKey === actionKey(row, 'arrange')"
                  @click="handleReturnArrange(row)"
                >
                  退安排
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col
        :xs="24"
        :md="10"
      >
        <el-card
          shadow="never"
          class="panel"
        >
          <h3 class="panel-title">
            已检验项目
          </h3>
          <el-table
            v-loading="doneLoading"
            :data="doneList"
            border
            stripe
            size="small"
            height="440"
            empty-text="没有找到匹配的记录"
          >
            <el-table-column
              prop="bh"
              label="编号"
              min-width="150"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <ProjectNoLink :row="row" />
              </template>
            </el-table-column>
            <el-table-column
              prop="productName"
              label="名称"
              min-width="90"
              show-overflow-tooltip
            />
            <el-table-column
              prop="date_jyy"
              label="完成日期"
              width="110"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_jyy) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="转待检验"
              width="98"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  :loading="actingKey === actionKey(row, 'inspect')"
                  @click="handleRevert(row)"
                >
                  转待检验
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="itemsVisible"
      title="全部样品检验任务明细列表"
      width="1040px"
      destroy-on-close
    >
      <el-table
        v-loading="itemsLoading"
        :data="itemsRows"
        border
        stripe
        size="small"
        max-height="480"
        empty-text="没有找到匹配的记录"
      >
        <el-table-column
          prop="project_bh"
          label="样品编号"
          width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="productName"
          label="样品名称"
          width="100"
          show-overflow-tooltip
        />
        <el-table-column
          prop="item_xh"
          label="序号"
          width="60"
          align="center"
        />
        <el-table-column
          prop="testItem"
          label="检测项目"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="stdValue"
          label="指标要求"
          width="100"
          align="center"
        />
        <el-table-column
          prop="inspector_name"
          label="检验员"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            {{ safeText(row.inspector_name) || safeText(row.userId_jyy) || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="jyResult"
          label="检验结果"
          min-width="110"
          align="center"
        >
          <template #default="{ row }">
            {{ safeText(row.jyResult) || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="item_evaluate"
          label="单项评价"
          width="90"
          align="center"
        />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  fetchDoneInspect,
  fetchInspectItems,
  fetchPendingInspect,
  returnArrange,
  revertInspect,
  type InspectItemRow,
  type InspectTaskRow,
} from '@/api/inspect'
import ProjectNoLink from '@/components/ProjectNoLink.vue'

const scope = ref<'all' | 'mine'>('all')
const pendingLoading = ref(false)
const doneLoading = ref(false)
const itemsLoading = ref(false)
const pendingList = ref<InspectTaskRow[]>([])
const doneList = ref<InspectTaskRow[]>([])
const itemsRows = ref<InspectItemRow[]>([])
const itemsVisible = ref(false)
const actingKey = ref('')

function safeText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim()
}

function bhOf(row: InspectTaskRow): string {
  return safeText(row.bh)
}

function actionKey(row: InspectTaskRow, action: string): string {
  return `${bhOf(row)}#${action}`
}

async function loadPending() {
  pendingLoading.value = true
  try {
    const result = await fetchPendingInspect(scope.value === 'mine')
    pendingList.value = result.records
  } finally {
    pendingLoading.value = false
  }
}

async function loadDone() {
  doneLoading.value = true
  try {
    const result = await fetchDoneInspect(scope.value === 'mine')
    doneList.value = result.records
  } finally {
    doneLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadPending(), loadDone()])
}

/** 全部待检验样品的检验项目明细 */
async function openAllItems() {
  const bhs = pendingList.value.map((row) => bhOf(row)).filter((bh) => bh !== '')
  if (bhs.length === 0) {
    ElMessage.warning('当前没有待检验的样品')
    return
  }
  itemsVisible.value = true
  itemsLoading.value = true
  itemsRows.value = []
  try {
    itemsRows.value = await fetchInspectItems(bhs)
  } finally {
    itemsLoading.value = false
  }
}

async function handleReturnArrange(row: InspectTaskRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将样品「${bh}」退回安排环节吗？`,
      '退安排确认',
      { type: 'warning', confirmButtonText: '确定退安排', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'arrange')
  try {
    await returnArrange([bh])
    ElMessage.success(`样品 ${bh} 已退回安排环节`)
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

async function handleRevert(row: InspectTaskRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将样品「${bh}」转回待检验吗？`,
      '转待检验',
      { type: 'warning', confirmButtonText: '转待检验', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'inspect')
  try {
    await revertInspect([bh])
    ElMessage.success(`样品 ${bh} 已转回待检验`)
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.query-inspection {
  min-width: 0;
}

.panel {
  margin-bottom: 12px;
}

.panel-title {
  margin: 0 0 12px;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.filter-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 12px;
}

.action-btn {
  color: #ffffff;
  background: #5f7c8b;
  border-color: #5f7c8b;
}

.action-btn:hover,
.action-btn:focus {
  color: #ffffff;
  background: #71909f;
  border-color: #71909f;
}

.row-btn {
  padding: 4px 8px;
  color: #ffffff;
  background: #5f7c8b;
  border-color: #5f7c8b;
}

.row-btn:hover,
.row-btn:focus {
  color: #ffffff;
  background: #71909f;
  border-color: #71909f;
}
</style>
