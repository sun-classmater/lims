<template>
  <div class="active-project-query">
    <div class="section-title">
      在检项目查询
    </div>

    <el-card
      class="filter-card"
      shadow="never"
    >
      <div class="filter-grid">
        <div class="filter-group">
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
          <span class="filter-label">预约完成时间区间:</span>
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
          <span class="filter-label">任务编号:</span>
          <el-input
            v-model="query.taskNo"
            class="text-input"
            clearable
            placeholder="任务编号"
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="filter-group">
          <span class="filter-label">检验情况:</span>
          <el-radio-group v-model="query.examineState">
            <el-radio value="all">
              全部
            </el-radio>
            <el-radio value="done">
              已完成
            </el-radio>
            <el-radio value="undone">
              未完成
            </el-radio>
          </el-radio-group>
        </div>

        <el-button
          type="primary"
          :icon="Search"
          class="search-btn"
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
          prop="bh"
          label="样品编号"
          min-width="170"
          sortable
        >
          <template #default="{ row }">
            <ProjectNoLink :row="row" />
          </template>
        </el-table-column>
        <el-table-column
          prop="dwname_wt_nj"
          label="受检单位"
          min-width="180"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column
          prop="productName"
          label="样品名称"
          min-width="130"
        />
        <el-table-column
          prop="result"
          label="结论"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <span
              class="result-text"
              :class="{ 'result-bad': row.result === '不合格' }"
            >
              {{ row.result || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="date_jycomplete"
          label="预约完成日期"
          width="140"
          align="center"
        />
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
          prop="scdateflag"
          label="任务编号"
          min-width="170"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <TaskNoLink
              :row="row"
              prop="scdateflag"
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

import { fetchData } from '@/api/data'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'
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
  taskNo: string
  examineState: 'all' | 'done' | 'undone'
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
    taskNo: '',
    examineState: 'all',
  }
}

const query = reactive<QueryForm>(createDefaultQuery())
const loading = ref(false)
const rows = ref<Array<Record<string, unknown>>>([])

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
    if (query.taskNo) {
      params.scdateflag = query.taskNo
    }
    if (query.examineState === 'done') {
      params.state_comlete = 'T'
    } else if (query.examineState === 'undone') {
      params.state_comleteNe = 'T'
    }

    const result = await fetchData('project', params)
    rows.value = result.records
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchRows()
}

onMounted(() => {
  fetchRows()
})
</script>

<style scoped>
.active-project-query {
  min-width: 0;
}

.section-title {
  margin-bottom: 14px;
  color: #e36d6d;
  font-size: 16px;
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

.date-picker {
  width: 150px;
}

.range-sep {
  color: #374151;
  font-size: 13px;
}

.search-btn {
  margin-left: auto;
}

.table-card {
  border-radius: 8px;
}

.result-text {
  color: #1f2937;
}

.result-bad {
  color: #dc2626;
  font-weight: 600;
}

@media (max-width: 1400px) {
  .search-btn {
    margin-left: 0;
  }
}
</style>
