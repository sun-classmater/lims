<template>
  <div class="single-decompose">
    <el-row :gutter="16">
      <el-col
        :xs="24"
        :md="14"
      >
        <el-card
          shadow="never"
          class="panel"
        >
          <h3 class="panel-title">
            待分解样品检验项目
          </h3>
          <el-table
            v-loading="pendingLoading"
            :data="pendingList"
            border
            stripe
            size="small"
            height="520"
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
              label="分解状态"
              width="90"
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
              width="120"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_yywc) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="分解完毕"
              width="104"
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
              width="92"
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
        :md="10"
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
            height="520"
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
              width="120"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_yywc) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="转待分解"
              width="100"
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
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  completeDecompose,
  fetchDoneSamples,
  fetchPendingSamples,
  returnToRegister,
  revertDecompose,
  type DecomposeResult,
  type DecomposeRow,
} from '@/api/decompose'
import ProjectNoLink from '@/components/ProjectNoLink.vue'

const pendingLoading = ref(false)
const doneLoading = ref(false)
const pendingList = ref<DecomposeRow[]>([])
const doneList = ref<DecomposeRow[]>([])
const actingKey = ref('')

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

async function loadPending() {
  pendingLoading.value = true
  try {
    const result = await fetchPendingSamples({})
    pendingList.value = result.records
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

async function handleComplete(row: DecomposeRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  const tip = hasDetails(row)
    ? `确认样品「${bh}」分解完毕吗？`
    : `样品「${bh}」尚未分解，确认按该样品的标准库生成检验项目并标记分解完毕吗？`
  try {
    await ElMessageBox.confirm(tip, '分解完毕', {
      type: 'warning',
      confirmButtonText: '分解完毕',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'done')
  try {
    const result = await completeDecompose([bh])
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
.single-decompose {
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

.state-undone {
  color: #c45656;
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
