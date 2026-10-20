<template>
  <div class="batch-decompose">
    <el-row :gutter="16">
      <el-col
        :xs="24"
        :md="15"
      >
        <el-card
          shadow="never"
          class="panel"
        >
          <h3 class="panel-title">
            待分解样品检验项目
          </h3>

          <div class="filter-line">
            <span class="filter-label">任务号:</span>
            <el-input
              v-model="query.taskno"
              class="text-input"
              placeholder="all"
              clearable
              @keyup.enter="handleSearch"
            />
            <span class="filter-label filter-label-gap">样品名称:</span>
            <el-input
              v-model="query.productName"
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
          </div>

          <div class="action-line">
            <span class="filter-label">标准库:</span>
            <el-input
              v-model="query.library"
              class="library-input"
              placeholder="all"
              clearable
            />
            <el-button
              class="action-btn"
              :loading="acting === 'task'"
              @click="handleGenerateByTask"
            >
              按任务号批量分解
            </el-button>
            <el-button
              class="action-btn"
              :loading="acting === 'keyword'"
              @click="handleGenerateByKeyword"
            >
              按任务号或名称批量分解
            </el-button>
            <el-button
              type="danger"
              :loading="acting === 'complete'"
              @click="handleBatchComplete"
            >
              批量确认【分解完毕】
            </el-button>
            <span class="selected-tip">已勾选 {{ selectedRows.length }} 个</span>
          </div>

          <el-table
            v-loading="pendingLoading"
            :data="pendingList"
            border
            stripe
            size="small"
            height="470"
            @selection-change="handleSelectionChange"
          >
            <el-table-column
              type="selection"
              width="42"
              align="center"
            />
            <el-table-column
              prop="taskno"
              label="任务号"
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
              label="分解状态"
              width="88"
              align="center"
            >
              <template #default="{ row }">
                <span :class="{ 'state-undone': !hasDetails(row) }">
                  {{ hasDetails(row) ? '已分解' : '未分解' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              prop="date_yywc"
              label="预约完成日期"
              width="115"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_yywc) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="分解完毕"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  :loading="actingKey === actionKey(row, 'done')"
                  @click="handleComplete(row)"
                >
                  分解完毕
                </el-button>
              </template>
            </el-table-column>
            <el-table-column
              label="退登记"
              width="88"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  class="return-btn"
                  :loading="actingKey === actionKey(row, 'return')"
                  @click="handleReturn(row)"
                >
                  退登记
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col
        :xs="24"
        :md="9"
      >
        <el-card
          shadow="never"
          class="panel"
        >
          <h3 class="panel-title">
            已分解样品检验项目
          </h3>
          <el-table
            v-loading="doneLoading"
            :data="doneList"
            border
            stripe
            size="small"
            height="580"
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
              label="预约完成日期"
              width="115"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_yywc) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="转待分解"
              width="98"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  :loading="actingKey === actionKey(row, 'revert')"
                  @click="handleRevert(row)"
                >
                  转待分解
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  completeDecompose,
  fetchDoneSamples,
  fetchPendingSamples,
  generateDetails,
  returnToRegister,
  revertDecompose,
  type DecomposeResult,
  type DecomposeRow,
} from '@/api/decompose'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'

const pendingLoading = ref(false)
const doneLoading = ref(false)
const pendingList = ref<DecomposeRow[]>([])
const doneList = ref<DecomposeRow[]>([])
const selectedRows = ref<DecomposeRow[]>([])
const acting = ref<'' | 'task' | 'keyword' | 'complete'>('')
const actingKey = ref('')

const query = reactive({
  taskno: '',
  productName: '',
  library: 'all',
})

function safeText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim()
}

function bhOf(row: DecomposeRow): string {
  return safeText(row.bh)
}

function actionKey(row: DecomposeRow, action: string): string {
  return `${bhOf(row)}#${action}`
}

function hasDetails(row: DecomposeRow): boolean {
  return Number(row.detail_count ?? 0) > 0
}

function reportResult(result: DecomposeResult, successText: string) {
  if (result.messages.length > 0) {
    const brief = result.messages.slice(0, 3).join('；')
    const suffix = result.messages.length > 3 ? ' 等' : ''
    ElMessage.warning(`${successText}；${brief}${suffix}`)
    return
  }
  ElMessage.success(successText)
}

function selectedBhs(): string[] {
  return selectedRows.value.map((row) => bhOf(row)).filter((bh) => bh !== '')
}

async function loadPending() {
  pendingLoading.value = true
  try {
    const result = await fetchPendingSamples({
      taskno: query.taskno.trim() || undefined,
      productName: query.productName.trim() || undefined,
    })
    pendingList.value = result.records
    selectedRows.value = []
  } finally {
    pendingLoading.value = false
  }
}

async function loadDone() {
  doneLoading.value = true
  try {
    const result = await fetchDoneSamples()
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

function handleSelectionChange(selection: DecomposeRow[]) {
  selectedRows.value = selection
}

/** 按任务号批量分解：把该任务下未分解的样品按标准库生成检验项目 */
async function handleGenerateByTask() {
  const taskno = query.taskno.trim()
  if (!taskno) {
    ElMessage.warning('请先填写任务号')
    return
  }
  acting.value = 'task'
  try {
    const result = await generateDetails({ taskno, library: query.library.trim() })
    reportResult(
      result,
      `已按任务号 ${taskno} 分解 ${result.generated} 个样品（跳过已分解 ${result.skipped} 个）`,
    )
    await refreshAll()
  } finally {
    acting.value = ''
  }
}

/** 按任务号或名称批量分解：名称优先，未填名称时用任务号匹配 */
async function handleGenerateByKeyword() {
  const keyword = query.productName.trim() || query.taskno.trim()
  if (!keyword) {
    ElMessage.warning('请先填写任务号或样品名称')
    return
  }
  acting.value = 'keyword'
  try {
    const result = await generateDetails({
      keyword,
      library: query.library.trim(),
    })
    reportResult(
      result,
      `已按「${keyword}」分解 ${result.generated} 个样品（跳过已分解 ${result.skipped} 个）`,
    )
    await refreshAll()
  } finally {
    acting.value = ''
  }
}

/** 批量确认【分解完毕】：对勾选的样品生成明细并标记分解完毕 */
async function handleBatchComplete() {
  const bhs = selectedBhs()
  if (bhs.length === 0) {
    ElMessage.warning('请先勾选需要确认分解完毕的样品')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将选中的 ${bhs.length} 个样品批量标记为分解完毕吗？`,
      '批量确认分解完毕',
      { type: 'warning', confirmButtonText: '确认分解完毕', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  acting.value = 'complete'
  try {
    const result = await completeDecompose(bhs, query.library.trim())
    reportResult(result, `已确认分解完毕，共处理 ${result.total} 个样品`)
    await refreshAll()
  } finally {
    acting.value = ''
  }
}

async function handleComplete(row: DecomposeRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认样品「${bh}」分解完毕吗？`,
      '分解完毕',
      { type: 'warning', confirmButtonText: '分解完毕', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'done')
  try {
    const result = await completeDecompose([bh], query.library.trim())
    reportResult(result, `样品 ${bh} 已标记分解完毕`)
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

async function handleRevert(row: DecomposeRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将样品「${bh}」转回待分解吗？`,
      '转待分解',
      { type: 'warning', confirmButtonText: '转待分解', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'revert')
  try {
    const result = await revertDecompose([bh])
    if (result.messages.length > 0) {
      ElMessage.warning(result.messages.join('；'))
    } else {
      ElMessage.success(`样品 ${bh} 已转回待分解`)
    }
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

async function handleReturn(row: DecomposeRow) {
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
    await returnToRegister([bh])
    ElMessage.success(`样品 ${bh} 已退回登记状态`)
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.batch-decompose {
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

.filter-line,
.action-line {
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

.filter-label-gap {
  margin-left: 10px;
}

.text-input {
  width: 170px;
}

.library-input {
  width: 150px;
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

.selected-tip {
  color: #909399;
  font-size: 13px;
}

.state-undone {
  color: #c45656;
}

.return-btn {
  padding: 4px 8px;
  color: #ffffff;
  background: #5f7c8b;
  border-color: #5f7c8b;
}

.return-btn:hover,
.return-btn:focus {
  color: #ffffff;
  background: #71909f;
  border-color: #71909f;
}
</style>
