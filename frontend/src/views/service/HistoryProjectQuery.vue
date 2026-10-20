<template>
  <div class="history-project-query">
    <div class="section-title">
      已授权签发历史项目查询
    </div>

    <el-card
      class="filter-card"
      shadow="never"
    >
      <div class="filter-grid">
        <div class="filter-group">
          <span class="filter-label">项目编号区间:</span>
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
          <span class="filter-label">委托单位:</span>
          <el-input
            v-model="query.clientUnit"
            class="text-input"
            clearable
            placeholder="委托单位"
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="filter-group">
          <span class="filter-label">生产单位:</span>
          <el-input
            v-model="query.produceUnit"
            class="text-input"
            clearable
            placeholder="生产单位"
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="filter-group filter-group-wide">
          <span class="filter-label">结题日期区间:</span>
          <el-date-picker
            v-model="query.finishDateStart"
            type="date"
            class="date-picker"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
          />
          <span class="range-sep">至</span>
          <el-date-picker
            v-model="query.finishDateEnd"
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
          min-width="120"
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
          min-width="160"
        />
        <el-table-column
          prop="productName"
          label="样品名称"
          min-width="100"
        />
        <el-table-column
          prop="result"
          label="结论"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <span v-if="row.result">{{ row.result }}</span>
            <span
              v-else
              class="empty-cell"
            >—</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="date_stamp"
          label="签发日期"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            <span v-if="row.date_stamp">{{ row.date_stamp }}</span>
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
            <el-link
              type="primary"
              @click="openProjectDetailDialog(row, 'arrange')"
            >
              查看
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
              查看
            </el-link>
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
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'
import { fetchData } from '@/api/data'
import {
  openProjectDetailDialog,
  openProjectRegisterDialog,
} from '@/utils/entityDialogs'

interface QueryForm {
  bhStart: string
  bhEnd: string
  productName: string
  clientUnit: string
  produceUnit: string
  finishDateStart: string
  finishDateEnd: string
  taskno: string
  batchNo: string
}

interface ProjectRow extends Record<string, unknown> {
  id_nj: number
  bh: string
  taskno: string
}

function createDefaultQuery(): QueryForm {
  return {
    bhStart: '',
    bhEnd: '',
    productName: '',
    clientUnit: '',
    produceUnit: '',
    finishDateStart: '',
    finishDateEnd: '',
    taskno: '',
    batchNo: '',
  }
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
      state_comlete: 'T',
    }
    if (query.bhStart) {
      params.bhStart = query.bhStart
    }
    if (query.bhEnd) {
      params.bhEnd = query.bhEnd
    }
    if (query.productName) {
      params.productName = query.productName
    }
    if (query.clientUnit) {
      params.dwname_wt_nj = query.clientUnit
    }
    if (query.produceUnit) {
      params.dwname_sc_nj = query.produceUnit
    }
    if (query.finishDateStart) {
      params.date_jycompleteStart = query.finishDateStart
    }
    if (query.finishDateEnd) {
      params.date_jycompleteEnd = query.finishDateEnd
    }
    if (query.taskno) {
      params.taskno = query.taskno
    }
    if (query.batchNo) {
      params.taskbatchno = query.batchNo
    }

    const result = await fetchData('project', params)
    rows.value = result.records as ProjectRow[]
  } finally {
    loading.value = false
  }
}

async function loadTaskOptions() {
  const result = await fetchData('project', {
    page: 1,
    size: 200,
    state_comlete: 'T',
  })
  taskOptions.value = Array.from(
    new Set(
      result.records
        .map((row) => String(row.taskno ?? '').trim())
        .filter((value) => value !== ''),
    ),
  ).sort()
}

function handleSearch() {
  fetchRows()
}

async function handleReturn(row: ProjectRow) {
  try {
    await ElMessageBox.confirm(
      `确定要将项目「${row.bh}」退回到登记状态吗？`,
      '退登记确认',
      {
        type: 'warning',
        confirmButtonText: '确定退登记',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }
  ElMessage.info('当前为查询演示页，退登记业务操作暂未开放')
}

onMounted(() => {
  fetchRows()
  loadTaskOptions()
})
</script>

<style scoped>
.history-project-query {
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
  margin-left: auto;
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

@media (max-width: 1400px) {
  .search-btn {
    margin-left: 0;
  }
}
</style>
