<template>
  <div class="register-query">
    <el-card
      shadow="never"
      class="filter-card"
    >
      <div class="filter-line">
        <span class="filter-label">样品编号区间:</span>
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
        <span class="filter-label filter-label-gap">样品登记时间区间:</span>
        <el-date-picker
          v-model="query.regStart"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
          :clearable="true"
        />
        <span class="range-sep">至</span>
        <el-date-picker
          v-model="query.regEnd"
          type="date"
          class="date-picker"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
          :clearable="true"
        />
      </div>
      <div class="filter-line">
        <span class="filter-label">产品名称:</span>
        <el-input
          v-model="query.productName"
          class="text-input"
          placeholder="请输入产品名称"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="filter-label filter-label-gap">受检单位:</span>
        <el-input
          v-model="query.sjUnit"
          class="text-input"
          placeholder="请输入受检单位"
          clearable
          @keyup.enter="handleSearch"
        />
        <span class="filter-label filter-label-gap">监抽任务编号:</span>
        <el-input
          v-model="query.taskno"
          class="text-input"
          placeholder="请输入任务编号"
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

        height="520"
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
          prop="dwname_sj_nj"
          label="受检单位"
          min-width="130"
          show-overflow-tooltip
        />
        <el-table-column
          prop="dwname_sc_nj"
          label="生产单位"
          min-width="130"
          show-overflow-tooltip
        />
        <el-table-column
          prop="productName"
          label="产品名称"
          min-width="100"
          show-overflow-tooltip
        />
        <el-table-column
          label="分解信息"
          width="150"
          align="center"
        >
          <template #default="{ row }">
            <div class="divide-cell">
              <el-tag
                :type="divideTagType(row.stateDiv)"
                size="small"
                effect="plain"
              >
                {{ divideStateText(row.stateDiv) }}
              </el-tag>
              <el-button
                link
                type="primary"
                :disabled="row.detailCount === 0"
                @click="openDivideDialog(row)"
              >
                明细({{ row.detailCount }})
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="byNum"
          label="备样量"
          width="90"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.byNum || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="date_reg"
          label="登记日期"
          width="110"
          align="center"
        />
        <el-table-column
          prop="date_yywc"
          label="预约完成日期"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            {{ row.date_yywc || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="result"
          label="结论"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.result === '合格'"
              type="success"
              size="small"
            >
              合格
            </el-tag>
            <el-tag
              v-else-if="row.result === '不合格'"
              type="danger"
              size="small"
            >
              不合格
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <template #empty>
          没有找到匹配的记录
        </template>
      </el-table>

      <div class="pager-row">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="divideVisible"
      :title="`分解信息 - ${divideBh}`"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-table
        v-loading="detailLoading"
        :data="details"
        border
        stripe
        max-height="420"
      >
        <el-table-column
          prop="itemXh"
          label="序号"
          width="60"
          align="center"
        />
        <el-table-column
          prop="testItem"
          label="检测单项"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          prop="basis"
          label="检验依据"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="mathod"
          label="检验方法"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="jyResult"
          label="检验结果"
          min-width="100"
          show-overflow-tooltip
        />
        <el-table-column
          prop="itemEvaluate"
          label="单项评价"
          width="90"
          align="center"
        />
        <template #empty>
          该样品暂无分解明细
        </template>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import {
  fetchDivideDetail,
  fetchRegisterQuery,
  type DivideDetailRow,
  type RegisterQueryRow,
} from '@/api/sampleRegisterQuery'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'

const pageSize = ref(20)
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const rows = ref<RegisterQueryRow[]>([])

const query = reactive({
  bhStart: '',
  bhEnd: '',
  regStart: '',
  regEnd: '',
  productName: '',
  sjUnit: '',
  taskno: '',
})

const divideVisible = ref(false)
const detailLoading = ref(false)
const details = ref<DivideDetailRow[]>([])
const divideBh = ref('')

function divideStateText(state: string | null): string {
  if (state === 'T') {
    return '已分解'
  }
  if (state === 'R') {
    return '退回'
  }
  return '未分解'
}

function divideTagType(state: string | null): 'success' | 'danger' | 'info' {
  if (state === 'T') {
    return 'success'
  }
  if (state === 'R') {
    return 'danger'
  }
  return 'info'
}

function trimmed(value: string): string | undefined {
  const text = value.trim()
  return text ? text : undefined
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchRegisterQuery({
      page: page.value,
      size: pageSize.value,
      bhStart: trimmed(query.bhStart),
      bhEnd: trimmed(query.bhEnd),
      regStart: trimmed(query.regStart),
      regEnd: trimmed(query.regEnd),
      productName: trimmed(query.productName),
      sjUnit: trimmed(query.sjUnit),
      taskno: trimmed(query.taskno),
    })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  if (query.bhStart.trim() && query.bhEnd.trim() && query.bhStart.trim() > query.bhEnd.trim()) {
    ElMessage.warning('样品编号区间起始值不能大于结束值')
    return
  }
  if (query.regStart && query.regEnd && query.regStart > query.regEnd) {
    ElMessage.warning('登记时间开始日期不能晚于结束日期')
    return
  }
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
  page.value = 1
  loadData()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  page.value = 1
  loadData()
}

async function openDivideDialog(row: RegisterQueryRow) {
  if (!row.bh) {
    return
  }
  divideBh.value = row.bh
  divideVisible.value = true
  detailLoading.value = true
  try {
    details.value = await fetchDivideDetail(row.bh)
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.register-query {
  padding: 12px 16px;
}

.filter-card {
  margin-bottom: 12px;
}

.table-card {
  margin-bottom: 12px;
}

.filter-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 10px;
}

.filter-line + .filter-line {
  margin-top: 14px;
}

.filter-label {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
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
  width: 160px !important;
}

.range-sep {
  color: #606266;
}

.search-btn {
  margin-left: 4px;
}

.divide-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
