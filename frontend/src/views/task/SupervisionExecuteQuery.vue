<template>
  <div class="supervision-execute-query">
    <div class="section-title">
      抽检任务抽样信息查询:
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

        <div class="filter-group">
          <span class="filter-label">抽检任务编号:</span>
          <el-select
            v-model="query.taskno"
            class="task-select"
            clearable
            filterable
            placeholder="抽检任务编号"
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
          <span class="filter-label">任务批号:</span>
          <el-input
            v-model="query.batchNo"
            class="batch-input"
            clearable
            placeholder="任务批号"
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
        v-loading="loading"
        :data="rows"
        border
        stripe
      >
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
          prop="productName"
          label="名称"
          min-width="90"
        />
        <el-table-column
          prop="dwname_sj_nj"
          label="受检单位"
          min-width="120"
        />
        <el-table-column
          prop="date_yywc"
          label="要求日期"
          width="110"
          align="center"
          sortable
        >
          <template #default="{ row }">
            <span v-if="safeText(row.date_yywc)">{{ safeText(row.date_yywc) }}</span>
            <span
              v-else
              class="empty-cell"
            >—</span>
          </template>
        </el-table-column>
        <el-table-column
          label="登记信息"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              @click="openProjectRegisterDialog(row)"
            >
              查看
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          label="分解信息"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              @click="openProjectDetailDialog(row, 'div')"
            >
              查看
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          label="安排信息"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <ProjectStageCell
              :row="row"
              stage="arrange"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="检验报告"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <ProjectStageCell
              :row="row"
              stage="report"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="检验数据"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <ProjectStageCell
              :row="row"
              stage="examine"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="报告递送信息"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            <ProjectStageCell
              :row="row"
              stage="delivery"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

import ProjectStageCell from '@/components/ProjectStageCell.vue'
import { fetchData } from '@/api/data'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'
import {
  openProjectDetailDialog,
  openProjectRegisterDialog,
} from '@/utils/entityDialogs'
import type { ProjectStageRow } from '@/utils/projectStage'

interface QueryForm {
  bhStart: string
  bhEnd: string
  dateRegStart: string
  dateRegEnd: string
  productName: string
  enterprise: string
  taskno: string
  batchNo: string
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
    taskno: '',
    batchNo: '',
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

async function fetchRows() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: 1,
      size: 200,
      // 本页仅展示执行中项目：未授权签发（state_comlete <> T）
      state_comleteNe: 'T',
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
    if (query.taskno) {
      params.taskno = query.taskno
    }
    if (query.batchNo) {
      params.taskbatchno = query.batchNo
    }

    const result = await fetchData('project', params)
    const list = result.records as ProjectRow[]
    // 旧 DEMO 默认按样品编号倒序展示（最新登记在前）
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
    state_comleteNe: 'T',
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

onMounted(() => {
  fetchRows()
  loadTaskOptions()
})
</script>

<style scoped>
.supervision-execute-query {
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
