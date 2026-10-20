<template>
  <div class="export-data">
    <div class="section-title">
      导出样品全部检测项结果数据
    </div>

    <el-card
      shadow="never"
      class="filter-card"
    >
      <div class="filter-line">
        <span class="filter-label">搜索样品：编号区间</span>
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
        <span class="filter-label filter-label-gap">登记时间区间</span>
        <el-date-picker
          v-model="query.regStart"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
        />
        <span class="range-sep">至</span>
        <el-date-picker
          v-model="query.regEnd"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
        />
      </div>

      <div class="filter-line">
        <span class="filter-label">产品名称:</span>
        <el-input
          v-model="query.productName"
          class="text-input"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="filter-label filter-label-gap">受检单位:</span>
        <el-input
          v-model="query.sjUnit"
          class="text-input"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="filter-label filter-label-gap">任务编号:</span>
        <el-input
          v-model="query.taskno"
          class="text-input"
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

      <div class="export-line">
        <el-button
          v-for="button in exportButtons"
          :key="button.type"
          class="export-btn"
          :loading="exporting === button.type"
          @click="handleExport(button.type)"
        >
          {{ button.label }}
        </el-button>
        <span class="selected-tip">已勾选 {{ selectedRows.length }} 个样品</span>
      </div>
    </el-card>

    <el-card
      shadow="never"
      class="table-card"
    >
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="rows"
        border
        stripe
        height="520"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          width="46"
          align="center"
        />
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
          prop="productName"
          label="产品名称"
          min-width="110"
          sortable
        />
        <el-table-column
          prop="dwname_sj_nj"
          label="受检单位"
          min-width="180"
          show-overflow-tooltip
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
          prop="date_yywc"
          label="预约完成日期"
          width="130"
          align="center"
        >
          <template #default="{ row }">
            {{ safeText(row.date_yywc) || '—' }}
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
import { ElMessage } from 'element-plus'

import { fetchData } from '@/api/data'
import {
  exportButtons,
  exportSamples,
  type SampleExportRow,
  type SampleExportType,
} from '@/api/sampleExport'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'

const loading = ref(false)
const rows = ref<SampleExportRow[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const selectedRows = ref<SampleExportRow[]>([])
const exporting = ref<SampleExportType | ''>('')

const query = reactive({
  bhStart: '',
  bhEnd: '',
  regStart: '',
  regEnd: '',
  productName: '',
  sjUnit: '',
  taskno: '',
})

function safeText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim()
}

function trimmed(value: string): string | undefined {
  const text = value.trim()
  return text ? text : undefined
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: page.value,
      size: pageSize.value,
      // 导出面向检验已完成、有结果数据的样品
      state_comlete: 'T',
    }
    const mapping: Array<[string, string | undefined]> = [
      ['bhStart', trimmed(query.bhStart)],
      ['bhEnd', trimmed(query.bhEnd)],
      ['date_regStart', trimmed(query.regStart)],
      ['date_regEnd', trimmed(query.regEnd)],
      ['productName', trimmed(query.productName)],
      ['dwname_sj_nj', trimmed(query.sjUnit)],
      ['taskno', trimmed(query.taskno)],
    ]
    for (const [key, value] of mapping) {
      if (value) {
        params[key] = value
      }
    }

    const result = await fetchData('project', params)
    rows.value = result.records as SampleExportRow[]
    total.value = result.total
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handleReset() {
  query.bhStart = ''
  query.bhEnd = ''
  query.regStart = ''
  query.regEnd = ''
  query.productName = ''
  query.sjUnit = ''
  query.taskno = ''
  handleSearch()
}

function handleSizeChange() {
  page.value = 1
  loadData()
}

function handleSelectionChange(selection: SampleExportRow[]) {
  selectedRows.value = selection
}

async function handleExport(type: SampleExportType) {
  const bhs = selectedRows.value
    .map((row) => safeText(row.bh))
    .filter((bh) => bh !== '')
  if (bhs.length === 0) {
    ElMessage.warning('请先勾选需要导出的样品')
    return
  }

  exporting.value = type
  try {
    await exportSamples(type, bhs)
    ElMessage.success(`已导出 ${bhs.length} 个样品的数据`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  } finally {
    exporting.value = ''
  }
}

onMounted(loadData)
</script>

<style scoped>
.export-data {
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

.filter-label {
  color: #374151;
  font-size: 13px;
  white-space: nowrap;
}

.filter-label-gap {
  margin-left: 18px;
}

.text-input {
  width: 180px;
}

.bh-input {
  width: 175px;
}

.date-picker {
  width: 150px;
}

.range-sep {
  color: #374151;
  font-size: 13px;
}

.search-btn {
  margin-left: 8px;
}

.export-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 4px;
  border-top: 1px dashed #ebeef5;
}

.export-btn {
  color: #ffffff;
  background: #5f7c8b;
  border-color: #5f7c8b;
}

.export-btn:hover,
.export-btn:focus {
  color: #ffffff;
  background: #71909f;
  border-color: #71909f;
}

.selected-tip {
  color: #909399;
  font-size: 13px;
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
</style>
