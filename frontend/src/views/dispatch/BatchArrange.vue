<template>
  <div class="batch-arrange">
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
            待安排检验项目
          </h3>

          <div class="filter-line">
            <span class="filter-label">任务编号:</span>
            <el-input
              v-model="taskno"
              class="text-input"
              placeholder="all"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-button
              type="primary"
              :loading="pendingLoading"
              @click="handleSearch"
            >
              筛选
            </el-button>
            <el-button
              class="action-btn"
              :loading="assigning"
              @click="handleBatchAssign"
            >
              批量安排检验任务
            </el-button>
            <span class="selected-tip">已勾选 {{ selectedRows.length }} 个</span>
          </div>

          <el-table
            v-loading="pendingLoading"
            :data="pendingList"
            border
            stripe
            size="small"
            height="480"
            @selection-change="handleSelectionChange"
          >
            <el-table-column
              type="selection"
              width="42"
              align="center"
            />
            <el-table-column
              prop="taskno"
              label="任务编号"
              min-width="130"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <TaskNoLink :row="row" />
              </template>
            </el-table-column>
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
              label="退登记"
              width="88"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  class="row-btn"
                  :loading="actingKey === actionKey(row, 'return')"
                  @click="handleReturn(row)"
                >
                  退登记
                </el-button>
              </template>
            </el-table-column>
            <el-table-column
              label="退分解"
              width="88"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  class="row-btn"
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
        :md="10"
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
            height="560"
            empty-text="没有找到匹配的记录"
          >
            <el-table-column
              prop="taskno"
              label="任务编号"
              min-width="130"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <TaskNoLink :row="row" />
              </template>
            </el-table-column>
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
              label="名称"
              min-width="90"
              show-overflow-tooltip
            />
            <el-table-column
              prop="date_yywc"
              label="预约完成日期"
              width="118"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_yywc) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="转待安排"
              width="98"
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
import TaskNoLink from '@/components/TaskNoLink.vue'

const pendingLoading = ref(false)
const doneLoading = ref(false)
const assigning = ref(false)
const pendingList = ref<ArrangeRow[]>([])
const doneList = ref<ArrangeRow[]>([])
const selectedRows = ref<ArrangeRow[]>([])
const actingKey = ref('')
const taskno = ref('')

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
    const result = await fetchPendingArrange(taskno.value.trim() || undefined)
    pendingList.value = result.records
    selectedRows.value = []
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

function handleSearch() {
  loadPending()
}

function handleSelectionChange(selection: ArrangeRow[]) {
  selectedRows.value = selection
}

/** 批量安排：对勾选的样品运行检验员分配算法 */
async function handleBatchAssign() {
  const bhs = selectedRows.value.map((row) => bhOf(row)).filter((bh) => bh !== '')
  if (bhs.length === 0) {
    ElMessage.warning('请先勾选需要安排检验任务的样品')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认对选中的 ${bhs.length} 个样品批量安排检验任务吗？`,
      '批量安排检验任务',
      { type: 'warning', confirmButtonText: '批量安排', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  assigning.value = true
  try {
    const result = await assignTasks(bhs)
    reportAssign(result, `已安排 ${result.assigned} 个样品`)
    await refreshAll()
  } finally {
    assigning.value = false
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
.batch-arrange {
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
  gap: 10px;
  margin-bottom: 12px;
}

.filter-label {
  color: #374151;
  font-size: 13px;
  white-space: nowrap;
}

.text-input {
  width: 170px;
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

.selected-tip {
  color: #909399;
  font-size: 13px;
}
</style>
