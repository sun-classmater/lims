<template>
  <div class="report-download">
    <el-card
      shadow="never"
      class="filter-card"
    >
      <div class="filter-line">
        <span class="filter-label">抽检任务编号:</span>
        <el-input
          v-model="query.taskno"
          class="text-input"
          placeholder="请输入任务编号"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="filter-label filter-label-gap">样品编号区间:</span>
        <el-input
          v-model="query.bhStart"
          class="bh-input"
          placeholder="请输入样品编号"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="range-sep">至</span>
        <el-input
          v-model="query.bhEnd"
          class="bh-input"
          placeholder="请输入样品编号"
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
        height="560"
      >
        <el-table-column
          prop="taskno"
          label="任务编号"
          min-width="140"
          sortable
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
          sortable
        >
          <template #default="{ row }">
            <ProjectNoLink :row="row" />
          </template>
        </el-table-column>
        <el-table-column
          prop="dwname_sj_nj"
          label="受检单位"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="productName"
          label="名称"
          min-width="100"
          sortable
        />
        <el-table-column
          prop="result"
          label="结论"
          width="90"
          align="center"
          sortable
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
          prop="date_reg"
          label="登记日期"
          width="120"
          align="center"
          sortable
        >
          <template #default="{ row }">
            {{ safeText(row.date_reg) || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="date_jycomplete"
          label="检验完成日期"
          width="130"
          align="center"
          sortable
        >
          <template #default="{ row }">
            {{ safeText(row.date_jycomplete) || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          v-for="report in reportColumns"
          :key="report.type"
          :label="report.label"
          :width="report.width"
          align="center"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              :disabled="downloadingKey === rowKey(row, report.type)"
              @click="handleDownload(row, report.type)"
            >
              {{ report.linkText }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          label="标记完毕"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :loading="actingId === row.id_nj"
              @click="handleToIssue(row)"
            >
              转签发
            </el-button>
          </template>
        </el-table-column>
        <el-table-column
          label="退登记"
          width="100"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              class="return-btn"
              :loading="actingId === row.id_nj"
              @click="handleReturn(row)"
            >
              退登记
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100, 200]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { fetchData, updateData } from '@/api/data'
import {
  downloadReport,
  reportColumns,
  type ReportDownloadRow,
  type ReportType,
} from '@/api/reportDownload'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'

const loading = ref(false)
const rows = ref<ReportDownloadRow[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const downloadingKey = ref('')
const actingId = ref<number | null>(null)

const query = reactive({
  taskno: '',
  bhStart: '',
  bhEnd: '',
})

function safeText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim()
}

function trimmed(value: string): string | undefined {
  const text = value.trim()
  return text ? text : undefined
}

function rowKey(row: ReportDownloadRow, type: ReportType): string {
  return `${safeText(row.bh)}#${type}`
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: page.value,
      size: pageSize.value,
      // 报告下载面向检验已完成的项目
      state_comlete: 'T',
    }
    if (trimmed(query.taskno)) {
      params.taskno = trimmed(query.taskno) as string
    }
    if (trimmed(query.bhStart)) {
      params.bhStart = trimmed(query.bhStart) as string
    }
    if (trimmed(query.bhEnd)) {
      params.bhEnd = trimmed(query.bhEnd) as string
    }

    const result = await fetchData('project', params)
    rows.value = result.records as ReportDownloadRow[]
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handleReset() {
  query.taskno = ''
  query.bhStart = ''
  query.bhEnd = ''
  handleSearch()
}

function handleSizeChange() {
  page.value = 1
  loadData()
}

async function handleDownload(row: ReportDownloadRow, type: ReportType) {
  const bh = safeText(row.bh)
  if (!bh) {
    ElMessage.warning('该行没有样品编号，无法下载报告')
    return
  }

  downloadingKey.value = rowKey(row, type)
  try {
    await downloadReport(type, bh)
    ElMessage.success('报告已下载')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '报告下载失败')
  } finally {
    downloadingKey.value = ''
  }
}

async function handleToIssue(row: ReportDownloadRow) {
  try {
    await ElMessageBox.confirm(
      `确认将样品「${safeText(row.bh)}」的报告标记完毕并转签发吗？`,
      '标记完毕',
      { type: 'warning', confirmButtonText: '转签发', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingId.value = row.id_nj
  try {
    await updateData('project', { id_nj: row.id_nj, state_reportprint: 'T' })
    ElMessage.success('已标记完毕并转签发')
    await loadData()
  } finally {
    actingId.value = null
  }
}

async function handleReturn(row: ReportDownloadRow) {
  try {
    await ElMessageBox.confirm(
      `确认将样品「${safeText(row.bh)}」退回到登记状态吗？`,
      '退登记确认',
      { type: 'warning', confirmButtonText: '确定退登记', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingId.value = row.id_nj
  try {
    await updateData('project', { id_nj: row.id_nj, state_register: 'F' })
    ElMessage.success('已退回登记状态')
    await loadData()
  } finally {
    actingId.value = null
  }
}

onMounted(loadData)
</script>

<style scoped>
.report-download {
  min-width: 0;
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
}

.filter-label {
  color: #374151;
  font-size: 13px;
  white-space: nowrap;
}

.filter-label-gap {
  margin-left: 18px;
}

.text-input {
  width: 190px;
}

.bh-input {
  width: 170px;
}

.range-sep {
  color: #374151;
  font-size: 13px;
}

.search-btn {
  margin-left: 8px;
}

.table-card {
  border-radius: 8px;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.empty-cell {
  color: #9ca3af;
}

.result-bad {
  color: #dc2626;
  font-weight: 600;
}

.return-btn {
  padding: 4px 10px;
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
