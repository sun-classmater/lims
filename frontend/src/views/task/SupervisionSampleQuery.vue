<template>
  <div class="supervision-sample-query">
    <div class="section-title">
      抽样查询:
    </div>

    <el-card
      class="filter-card"
      shadow="never"
    >
      <div class="filter-grid">
        <div class="filter-group filter-group-wide">
          <span class="filter-label">样品编号区间:</span>
          <el-input
            v-model="query.bhStart"
            class="range-input"
            clearable
            placeholder="起始编号"
            @keyup.enter="handleSearch"
          />
          <span class="range-sep">至</span>
          <el-input
            v-model="query.bhEnd"
            class="range-input"
            clearable
            placeholder="结束编号"
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="filter-group filter-group-wide">
          <span class="filter-label">登记时间区间:</span>
          <el-date-picker
            v-model="query.dateRegStart"
            type="date"
            class="date-picker"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
          />
          <span class="range-sep">至</span>
          <el-date-picker
            v-model="query.dateRegEnd"
            type="date"
            class="date-picker"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
          />
        </div>

        <div class="filter-group">
          <span class="filter-label">样品名称:</span>
          <el-input
            v-model="query.productName"
            class="text-input"
            clearable
            placeholder="样品名称"
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="filter-group">
          <span class="filter-label">企业名称:</span>
          <el-input
            v-model="query.enterprise"
            class="text-input"
            clearable
            placeholder="企业名称"
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="filter-group filter-group-wide">
          <span class="filter-label">抽样日期区间:</span>
          <el-date-picker
            v-model="query.dateCyStart"
            type="date"
            class="date-picker"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
          />
          <span class="range-sep">至</span>
          <el-date-picker
            v-model="query.dateCyEnd"
            type="date"
            class="date-picker"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
          />
        </div>

        <div class="filter-group">
          <span class="filter-label">抽样任务编号:</span>
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
        </div>

        <div class="filter-group">
          <span class="filter-label">抽样批号:</span>
          <el-input
            v-model="query.batchNo"
            class="batch-input"
            clearable
            placeholder="抽样批号"
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="filter-group">
          <span class="filter-label">抽样人:</span>
          <el-input
            v-model="query.sampler"
            class="sampler-input"
            clearable
            placeholder="抽样人"
            @keyup.enter="handleSearch"
          />
        </div>

        <el-button
          class="search-btn"
          :icon="Search"
          @click="handleSearch"
        >
          查找
        </el-button>
      </div>
    </el-card>

    <el-card
      class="table-card"
      shadow="never"
    >
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="rows"
        border
        stripe
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
          min-width="130"
          sortable
        >
          <template #default="{ row }">
            <TaskNoLink :row="row" />
          </template>
        </el-table-column>
        <el-table-column
          prop="bh"
          label="样品编号"
          width="150"
          sortable
        >
          <template #default="{ row }">
            <ProjectNoLink :row="row" />
          </template>
        </el-table-column>
        <el-table-column
          prop="dwname_sj_nj"
          label="受检单位"
          min-width="130"
        />
        <el-table-column
          prop="cybh"
          label="抽样单号"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <span v-if="safeText(row.cybh)">{{ safeText(row.cybh) }}</span>
            <span
              v-else
              class="empty-cell"
            >—</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="productName"
          label="样品名称"
          min-width="90"
        />
        <el-table-column
          prop="date_cy"
          label="抽样日期"
          width="110"
          align="center"
          sortable
        >
          <template #default="{ row }">
            <span v-if="safeText(row.date_cy)">{{ safeText(row.date_cy) }}</span>
            <span
              v-else
              class="empty-cell"
            >—</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="customerId_sjdw"
          label="被抽样单位"
          min-width="130"
        >
          <template #default="{ row }">
            <span v-if="safeText(row.customerId_sjdw)">{{ safeText(row.customerId_sjdw) }}</span>
            <span
              v-else
              class="empty-cell"
            >—</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="userId_cy"
          label="抽样人"
          width="100"
          align="center"
        />
        <el-table-column
          prop="scBatch"
          label="批号"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <span v-if="safeText(row.scBatch)">{{ safeText(row.scBatch) }}</span>
            <span
              v-else
              class="empty-cell"
            >—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import type { TableInstance } from 'element-plus'

import { fetchData } from '@/api/data'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'
import type { ProjectStageRow } from '@/utils/projectStage'

interface QueryForm {
  bhStart: string
  bhEnd: string
  dateRegStart: string
  dateRegEnd: string
  productName: string
  enterprise: string
  dateCyStart: string
  dateCyEnd: string
  taskno: string
  batchNo: string
  sampler: string
}

interface ProjectRow extends ProjectStageRow {
  id_nj: number
  bh: string
}

function createDefaultQuery(): QueryForm {
  return {
    bhStart: '',
    bhEnd: '',
    dateRegStart: '',
    dateRegEnd: '',
    productName: '',
    enterprise: '',
    dateCyStart: '',
    dateCyEnd: '',
    taskno: '',
    batchNo: '',
    sampler: '',
  }
}

function safeText(value: unknown): string {
  return value === null || value === undefined
    ? ''
    : String(value).trim()
}

const query = reactive<QueryForm>(createDefaultQuery())
const loading = ref(false)
const rows = ref<ProjectRow[]>([])
const taskOptions = ref<string[]>([])
const tableRef = ref<TableInstance>()
const selectedRows = ref<ProjectRow[]>([])

async function fetchRows() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: 1,
      size: 200,
    }
    if (query.bhStart) {
      params.bhStart = query.bhStart
    }
    if (query.bhEnd) {
      params.bhEnd = query.bhEnd
    }
    if (query.dateRegStart) {
      params.date_regStart = query.dateRegStart
    }
    if (query.dateRegEnd) {
      params.date_regEnd = query.dateRegEnd
    }
    if (query.productName) {
      params.productName = query.productName
    }
    if (query.enterprise) {
      params.dwname_sj_nj = query.enterprise
    }
    if (query.dateCyStart) {
      params.date_cyStart = query.dateCyStart
    }
    if (query.dateCyEnd) {
      params.date_cyEnd = query.dateCyEnd
    }
    if (query.taskno) {
      params.taskno = query.taskno
    }
    if (query.batchNo) {
      params.taskbatchno = query.batchNo
    }
    if (query.sampler) {
      params.userId_cy = query.sampler
    }

    const result = await fetchData('project', params)
    const list = result.records as ProjectRow[]
    // 与旧 DEMO 一致：按样品编号倒序展示
    list.sort((a, b) => safeText(b.bh).localeCompare(safeText(a.bh)))
    rows.value = list
  } finally {
    loading.value = false
  }
}

async function loadTaskOptions() {
  const result = await fetchData('project', {
    page: 1,
    size: 200,
  })
  taskOptions.value = Array.from(
    new Set(
      result.records
        .map((row) => safeText((row as ProjectStageRow).taskno))
        .filter((value) => value !== ''),
    ),
  ).sort()
}

function handleSearch() {
  fetchRows()
}

function handleSelectionChange(selection: ProjectRow[]) {
  selectedRows.value = selection
}

onMounted(() => {
  fetchRows()
  loadTaskOptions()
})
</script>

<style scoped>
.supervision-sample-query {
  min-width: 0;
}

.section-title {
  margin-bottom: 14px;
  color: #e36d6d;
  font-size: 15px;
  font-weight: 600;
}

.filter-card {
  margin-bottom: 14px;
  border-radius: 8px;
}

.filter-grid {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px 18px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group-wide {
  flex-basis: 470px;
}

.filter-label {
  flex-shrink: 0;
  color: #374151;
  font-size: 13px;
  white-space: nowrap;
}

.range-input {
  width: 160px;
}

.text-input {
  width: 170px;
}

.task-select {
  width: 210px;
}

.batch-input {
  width: 90px;
}

.sampler-input {
  width: 170px;
}

.date-picker {
  width: 150px;
}

.range-sep {
  color: #374151;
  font-size: 13px;
}

.search-btn {
  color: #ffffff;
  background: #5f7c8b;
  border-color: #5f7c8b;
}

.search-btn:hover,
.search-btn:focus {
  color: #ffffff;
  background: #71909f;
  border-color: #71909f;
}

.table-card {
  border-radius: 8px;
}

.empty-cell {
  color: #9ca3af;
}
</style>
