<template>
  <div class="project-issue">
    <div class="section-title">
      待授权签发项目
    </div>

    <el-card
      shadow="never"
      class="filter-card"
    >
      <div class="filter-line">
        <span class="filter-label">搜索样品：样品编号区间</span>
        <el-input
          v-model="query.bhStart"
          class="bh-input"
          placeholder="请输入项目编号"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="range-sep">至</span>
        <el-input
          v-model="query.bhEnd"
          class="bh-input"
          placeholder="请输入项目编号"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="filter-label filter-label-gap">样品名称:</span>
        <el-input
          v-model="query.productName"
          class="text-input"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="filter-label filter-label-gap">委托单位:</span>
        <el-input
          v-model="query.clientUnit"
          class="text-input"
          clearable
          @keyup.enter="handleSearch"
        />
      </div>

      <div class="filter-line">
        <span class="filter-label">预约完成日期:</span>
        <el-date-picker
          v-model="query.finishStart"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
        />
        <span class="range-sep">至</span>
        <el-date-picker
          v-model="query.finishEnd"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
        />
        <span class="filter-label filter-label-gap">抽样任务编号:</span>
        <el-select
          v-model="query.taskno"
          class="task-select"
          clearable
          filterable
          placeholder="抽样任务编号"
        >
          <el-option
            v-for="item in taskOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
        <span class="filter-label filter-label-gap">抽样批号</span>
        <el-input
          v-model="query.batchNo"
          class="batch-input"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button
          type="primary"
          class="search-btn"
          :loading="loading"
          @click="handleSearch"
        >
          查找
        </el-button>
        <el-button @click="handleReset">
          重置
        </el-button>
        <span class="total-tip">共 {{ total }} 条</span>
      </div>
    </el-card>

    <el-card
      shadow="never"
      class="table-card"
    >
      <el-table
        v-loading="loading"
        :data="rows"
        border
        stripe
        height="520"
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
          width="160"
        >
          <template #default="{ row }">
            <ProjectNoLink :row="row" />
          </template>
        </el-table-column>
        <el-table-column
          prop="productName"
          label="样品名称"
          min-width="100"
        />
        <el-table-column
          prop="client_name"
          label="委托单位"
          min-width="170"
          show-overflow-tooltip
        />
        <el-table-column
          prop="result"
          label="结论"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <span
              v-if="safeText(row.result)"
              :class="{ 'result-bad': row.result === '不合格' }"
            >{{ row.result }}</span>
            <span
              v-else
              class="empty-cell"
            >—</span>
          </template>
        </el-table-column>
        <el-table-column
          label="安排信息"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              @click="openProjectDetailDialog(row, 'arrange')"
            >
              安排信息
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          label="检验数据"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              @click="openProjectDetailDialog(row, 'inspect')"
            >
              检验数据
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          label="签发操作"
          width="150"
          align="center"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :loading="actingKey === actionKey(row, 'issue')"
              @click="handleIssue(row)"
            >
              签发
            </el-button>
            <el-button
              size="small"
              :loading="actingKey === actionKey(row, 'return')"
              @click="handleReturn(row)"
            >
              退登记
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { fetchData } from '@/api/data'
import {
  fetchPendingIssue,
  issueProjects,
  reviewReturnRegister,
  type IssueRow,
} from '@/api/review'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'
import { openProjectDetailDialog } from '@/utils/entityDialogs'

const loading = ref(false)
const rows = ref<IssueRow[]>([])
const total = ref(0)
const taskOptions = ref<string[]>([])
const actingKey = ref('')

const query = reactive({
  bhStart: '',
  bhEnd: '',
  productName: '',
  clientUnit: '',
  finishStart: '',
  finishEnd: '',
  taskno: '',
  batchNo: '',
})

function safeText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim()
}

function bhOf(row: IssueRow): string {
  return safeText(row.bh)
}

function actionKey(row: IssueRow, action: string): string {
  return `${bhOf(row)}#${action}`
}

function trimmed(value: string): string | undefined {
  const text = value.trim()
  return text ? text : undefined
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchPendingIssue({
      bhStart: trimmed(query.bhStart),
      bhEnd: trimmed(query.bhEnd),
      productName: trimmed(query.productName),
      clientUnit: trimmed(query.clientUnit),
      finishStart: trimmed(query.finishStart),
      finishEnd: trimmed(query.finishEnd),
      taskno: trimmed(query.taskno),
      batchNo: trimmed(query.batchNo),
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadTaskOptions() {
  try {
    const result = await fetchData('project', {
      page: 1,
      size: 200,
      state_comlete: 'T',
    })
    taskOptions.value = Array.from(
      new Set(
        result.records
          .map((row) => safeText(row.taskno))
          .filter((value) => value !== ''),
      ),
    ).sort()
  } catch {
    // 抽样任务编号下拉加载失败时不影响查询
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  query.bhStart = ''
  query.bhEnd = ''
  query.productName = ''
  query.clientUnit = ''
  query.finishStart = ''
  query.finishEnd = ''
  query.taskno = ''
  query.batchNo = ''
  handleSearch()
}

async function handleIssue(row: IssueRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认签发样品「${bh}」的报告吗？`,
      '报告签发',
      { type: 'warning', confirmButtonText: '签发', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'issue')
  try {
    const affected = await issueProjects([bh])
    if (affected > 0) {
      ElMessage.success(`样品 ${bh} 已签发`)
    } else {
      ElMessage.warning('该项目当前状态不允许签发')
    }
    await loadData()
  } finally {
    actingKey.value = ''
  }
}

async function handleReturn(row: IssueRow) {
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
    await reviewReturnRegister([bh])
    ElMessage.success(`样品 ${bh} 已退回登记状态`)
    await loadData()
  } finally {
    actingKey.value = ''
  }
}

onMounted(() => {
  loadData()
  loadTaskOptions()
})
</script>

<style scoped>
.project-issue {
  min-width: 0;
}

.section-title {
  margin-bottom: 14px;
  color: #374151;
  font-size: 15px;
  font-weight: 500;
}

.filter-card {
  margin-bottom: 14px;
  border-radius: 8px;
}

.filter-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.filter-line:last-child {
  margin-bottom: 0;
}

.filter-label {
  color: #374151;
  font-size: 13px;
  white-space: nowrap;
}

.filter-label-gap {
  margin-left: 12px;
}

.bh-input {
  width: 170px;
}

.text-input {
  width: 170px;
}

.date-picker {
  width: 150px;
}

.task-select {
  width: 190px;
}

.batch-input {
  width: 100px;
}

.range-sep {
  color: #374151;
  font-size: 13px;
}

.search-btn {
  margin-left: 8px;
}

.total-tip {
  color: #909399;
  font-size: 13px;
}

.table-card {
  border-radius: 8px;
}

.empty-cell {
  color: #9ca3af;
}

.result-bad {
  color: #dc2626;
  font-weight: 600;
}
</style>
