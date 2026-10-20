<template>
  <div class="inspect-summary">
    <div class="section-title">
      检验任务完成汇总查询
    </div>

    <el-card
      shadow="never"
      class="filter-card"
    >
      <div class="filter-line">
        <span class="filter-label">检验员:</span>
        <el-input
          v-model="query.inspector"
          class="text-input"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="filter-label filter-label-gap">检测项:</span>
        <el-input
          v-model="query.testItem"
          class="text-input"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="filter-label filter-label-gap">检验员完成检验时间段:</span>
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
      </div>

      <div class="filter-line">
        <span class="filter-label">签发日期范围:</span>
        <el-date-picker
          v-model="query.issueStart"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
        />
        <span class="range-sep">至</span>
        <el-date-picker
          v-model="query.issueEnd"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
        />
        <span class="filter-label filter-label-gap">盖章日期范围:</span>
        <el-date-picker
          v-model="query.stampStart"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
        />
        <span class="range-sep">至</span>
        <el-date-picker
          v-model="query.stampEnd"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
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
          prop="inspector_name"
          label="检验员姓名"
          min-width="110"
        >
          <template #default="{ row }">
            {{ safeText(row.inspector_name) || safeText(row.userId_jyy) || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="project_bh"
          label="编号"
          width="160"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <ProjectNoLink
              :row="row"
              prop="project_bh"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="testItem"
          label="检测单项"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="jyResult"
          label="检测数据"
          min-width="140"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span :class="{ 'result-bad': row.item_evaluate === '不合格' }">
              {{ safeText(row.jyResult) || safeText(row.item_evaluate) || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="finish_date"
          label="完成时间"
          width="115"
          align="center"
        >
          <template #default="{ row }">
            {{ safeText(row.finish_date) || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="issue_date"
          label="签发时间"
          width="115"
          align="center"
        >
          <template #default="{ row }">
            {{ safeText(row.issue_date) || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="stamp_date"
          label="盖章时间"
          width="115"
          align="center"
        >
          <template #default="{ row }">
            {{ safeText(row.stamp_date) || '—' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { fetchInspectSummary, type SummaryRow } from '@/api/review'
import ProjectNoLink from '@/components/ProjectNoLink.vue'

const loading = ref(false)
const rows = ref<SummaryRow[]>([])
const total = ref(0)

const query = reactive({
  inspector: '',
  testItem: '',
  finishStart: '',
  finishEnd: '',
  issueStart: '',
  issueEnd: '',
  stampStart: '',
  stampEnd: '',
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
    const result = await fetchInspectSummary({
      inspector: trimmed(query.inspector),
      testItem: trimmed(query.testItem),
      finishStart: trimmed(query.finishStart),
      finishEnd: trimmed(query.finishEnd),
      issueStart: trimmed(query.issueStart),
      issueEnd: trimmed(query.issueEnd),
      stampStart: trimmed(query.stampStart),
      stampEnd: trimmed(query.stampEnd),
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  query.inspector = ''
  query.testItem = ''
  query.finishStart = ''
  query.finishEnd = ''
  query.issueStart = ''
  query.issueEnd = ''
  query.stampStart = ''
  query.stampEnd = ''
  handleSearch()
}

onMounted(loadData)
</script>

<style scoped>
.inspect-summary {
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

.text-input {
  width: 160px;
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

.total-tip {
  color: #909399;
  font-size: 13px;
}

.table-card {
  border-radius: 8px;
}

.result-bad {
  color: #dc2626;
  font-weight: 600;
}
</style>
