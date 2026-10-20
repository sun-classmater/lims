<template>
  <div class="single-arrange">
    <el-row :gutter="16">
      <el-col
        :xs="24"
        :md="13"
      >
        <el-card
          shadow="never"
          class="panel"
        >
          <h3 class="panel-title">
            待安排检验项目
          </h3>
          <el-table
            v-loading="pendingLoading"
            :data="pendingList"
            border
            stripe
            size="small"
            height="520"
          >
            <el-table-column
              prop="bh"
              label="样品编号"
              min-width="160"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <ProjectNoLink :row="row" />
              </template>
            </el-table-column>
            <el-table-column
              prop="productName"
              label="样品名称"
              min-width="100"
              show-overflow-tooltip
            />
            <el-table-column
              label="安排"
              width="86"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  :loading="actingKey === actionKey(row, 'assign')"
                  @click="handleAssign(row)"
                >
                  安排
                </el-button>
              </template>
            </el-table-column>
            <el-table-column
              label="退登记"
              width="90"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  class="action-btn"
                  :loading="actingKey === actionKey(row, 'return')"
                  @click="handleReturn(row)"
                >
                  退登记
                </el-button>
              </template>
            </el-table-column>
            <el-table-column
              label="退分解"
              width="90"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  class="action-btn"
                  :loading="actingKey === actionKey(row, 'decompose')"
                  @click="handleReturnDecompose(row)"
                >
                  退分解
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col
        :xs="24"
        :md="11"
      >
        <el-card
          shadow="never"
          class="panel"
        >
          <h3 class="panel-title">
            已安排检验项目
          </h3>
          <el-table
            v-loading="doneLoading"
            :data="doneList"
            border
            stripe
            size="small"
            height="520"
            empty-text="没有找到匹配的记录"
          >
            <el-table-column
              prop="bh"
              label="编号"
              min-width="160"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <ProjectNoLink :row="row" />
              </template>
            </el-table-column>
            <el-table-column
              prop="productName"
              label="名称"
              min-width="100"
              show-overflow-tooltip
            />
            <el-table-column
              prop="date_yywc"
              label="预约完成日期"
              width="120"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_yywc) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="转待安排"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  :loading="actingKey === actionKey(row, 'revert')"
                  @click="handleRevert(row)"
                >
                  转待安排
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  arrangeReturnRegister,
  assignTasks,
  fetchDoneArrange,
  fetchPendingArrange,
  returnDecompose,
  revertArrange,
  type ArrangeResult,
  type ArrangeRow,
} from '@/api/arrange'
import ProjectNoLink from '@/components/ProjectNoLink.vue'

const pendingLoading = ref(false)
const doneLoading = ref(false)
const pendingList = ref<ArrangeRow[]>([])
const doneList = ref<ArrangeRow[]>([])
const actingKey = ref('')

function safeText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim()
}

function bhOf(row: ArrangeRow): string {
  return safeText(row.bh)
}

function actionKey(row: ArrangeRow, action: string): string {
  return `${bhOf(row)}#${action}`
}

function reportAssign(result: ArrangeResult, successText: string) {
  if (result.messages.length > 0) {
    const brief = result.messages.slice(0, 3).join('；')
    const suffix = result.messages.length > 3 ? ' 等' : ''
    ElMessage.warning(`${successText}；${brief}${suffix}`)
    return
  }
  ElMessage.success(successText)
}

async function loadPending() {
  pendingLoading.value = true
  try {
    const result = await fetchPendingArrange()
    pendingList.value = result.records
  } finally {
    pendingLoading.value = false
  }
}

async function loadDone() {
  doneLoading.value = true
  try {
    const result = await fetchDoneArrange()
    doneList.value = result.records
  } finally {
    doneLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadPending(), loadDone()])
}

async function handleAssign(row: ArrangeRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认对样品「${bh}」按分配算法安排检验任务吗？`,
      '安排检验任务',
      { type: 'warning', confirmButtonText: '安排', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'assign')
  try {
    const result = await assignTasks([bh])
    reportAssign(result, `样品 ${bh} 已完成安排`)
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

async function handleRevert(row: ArrangeRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将样品「${bh}」转回待安排吗？`,
      '转待安排',
      { type: 'warning', confirmButtonText: '转待安排', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'revert')
  try {
    await revertArrange([bh])
    ElMessage.success(`样品 ${bh} 已转回待安排`)
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

async function handleReturnDecompose(row: ArrangeRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将样品「${bh}」退回分解环节吗？`,
      '退分解确认',
      { type: 'warning', confirmButtonText: '确定退分解', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'decompose')
  try {
    await returnDecompose([bh])
    ElMessage.success(`样品 ${bh} 已退回分解环节`)
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

async function handleReturn(row: ArrangeRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将样品「${bh}」退回到登记状态吗？`,
      '退登记确认',
      { type: 'warning', confirmButtonText: '确定退登记', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'return')
  try {
    await arrangeReturnRegister([bh])
    ElMessage.success(`样品 ${bh} 已退回登记状态`)
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.single-arrange {
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

.action-btn {
  padding: 4px 8px;
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
</style>
