<template>
  <div class="input-inspection">
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
            待录入检验数据项目
          </h3>

          <div class="filter-line">
            <el-radio-group
              v-model="scope"
              @change="refreshAll"
            >
              <el-radio value="all">
                全部
              </el-radio>
              <el-radio value="mine">
                我负责的
              </el-radio>
            </el-radio-group>
            <el-button
              class="action-btn"
              :loading="entryLoading"
              @click="handleBatchEntry"
            >
              检验数据批量录入
            </el-button>
            <el-button
              class="action-btn"
              :loading="previewLoading"
              @click="handlePreview"
            >
              生成结果预览
            </el-button>
            <span class="selected-tip">已勾选 {{ selectedRows.length }} 个</span>
          </div>

          <el-table
            v-loading="pendingLoading"
            :data="pendingList"
            border
            stripe
            size="small"
            height="440"
            empty-text="没有找到匹配的记录"
            @selection-change="handleSelectionChange"
          >
            <el-table-column
              type="selection"
              width="42"
              align="center"
            />
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
              width="118"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_yywc) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              prop="date_sy"
              label="送样日期"
              width="108"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_sy) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              prop="date_cy"
              label="抽样日期"
              width="108"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_cy) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="录入"
              width="80"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  :loading="entryLoading"
                  @click="openEntry([bhOf(row)])"
                >
                  录入
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
            已录入检验数据项目
          </h3>
          <el-table
            v-loading="doneLoading"
            :data="doneList"
            border
            stripe
            size="small"
            height="440"
            empty-text="没有找到匹配的记录"
          >
            <el-table-column
              prop="bh"
              label="编号"
              min-width="150"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <ProjectNoLink :row="row" />
              </template>
            </el-table-column>
            <el-table-column
              prop="productName"
              label="名称"
              min-width="90"
              show-overflow-tooltip
            />
            <el-table-column
              prop="date_jyy"
              label="完成日期"
              width="110"
              align="center"
            >
              <template #default="{ row }">
                {{ safeText(row.date_jyy) || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="转待检验"
              width="98"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  size="small"
                  :loading="actingKey === actionKey(row, 'inspect')"
                  @click="handleRevert(row)"
                >
                  转待检验
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 检验数据录入：单条与批量共用 -->
    <el-dialog
      v-model="entryVisible"
      :title="entryTitle"
      width="1040px"
      destroy-on-close
    >
      <el-table
        :data="entryRows"
        border
        stripe
        size="small"
        max-height="480"
        empty-text="没有找到匹配的记录"
      >
        <el-table-column
          prop="project_bh"
          label="样品编号"
          width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="testItem"
          label="检测项目"
          min-width="170"
          show-overflow-tooltip
        />
        <el-table-column
          prop="stdValue"
          label="指标要求"
          width="100"
          align="center"
        />
        <el-table-column
          label="检验结果"
          width="150"
        >
          <template #default="{ row }">
            <el-input
              v-model="row.jyResult"
              size="small"
              placeholder="请输入"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="单项评价"
          width="120"
        >
          <template #default="{ row }">
            <el-select
              v-model="row.item_evaluate"
              size="small"
              clearable
              placeholder="请选择"
            >
              <el-option
                label="合格"
                value="合格"
              />
              <el-option
                label="不合格"
                value="不合格"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column
          label="结果备注"
          min-width="150"
        >
          <template #default="{ row }">
            <el-input
              v-model="row.jyResult_note"
              size="small"
            />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="entryVisible = false">
          取消
        </el-button>
        <el-button
          :loading="saving"
          @click="handleSave(false)"
        >
          保存
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSave(true)"
        >
          保存并标记检验完成
        </el-button>
      </template>
    </el-dialog>

    <!-- 结果预览 -->
    <el-dialog
      v-model="previewVisible"
      title="检验结果预览"
      width="1040px"
      destroy-on-close
    >
      <el-table
        :data="previewRows"
        border
        stripe
        size="small"
        max-height="480"
        empty-text="没有找到匹配的记录"
      >
        <el-table-column
          prop="project_bh"
          label="样品编号"
          width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="productName"
          label="样品名称"
          width="100"
          show-overflow-tooltip
        />
        <el-table-column
          prop="item_xh"
          label="序号"
          width="60"
          align="center"
        />
        <el-table-column
          prop="testItem"
          label="检测项目"
          min-width="170"
          show-overflow-tooltip
        />
        <el-table-column
          prop="stdValue"
          label="指标要求"
          width="100"
          align="center"
        />
        <el-table-column
          prop="jyResult"
          label="检验结果"
          min-width="110"
          align="center"
        >
          <template #default="{ row }">
            {{ safeText(row.jyResult) || '—' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="item_evaluate"
          label="单项评价"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <span :class="{ 'result-bad': row.item_evaluate === '不合格' }">
              {{ safeText(row.item_evaluate) || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="jyResult_note"
          label="结果备注"
          min-width="120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ safeText(row.jyResult_note) || '—' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  fetchDoneInspect,
  fetchInspectItems,
  fetchPendingInspect,
  revertInspect,
  saveInspectResults,
  type InspectItemRow,
  type InspectTaskRow,
} from '@/api/inspect'
import ProjectNoLink from '@/components/ProjectNoLink.vue'

interface EntryRow {
  project_bh: string
  productName: string
  item_xh: number
  testItem: string
  stdValue: string
  jyResult: string
  item_evaluate: string
  jyResult_note: string
}

const scope = ref<'all' | 'mine'>('all')
const pendingLoading = ref(false)
const doneLoading = ref(false)
const entryLoading = ref(false)
const previewLoading = ref(false)
const saving = ref(false)
const pendingList = ref<InspectTaskRow[]>([])
const doneList = ref<InspectTaskRow[]>([])
const selectedRows = ref<InspectTaskRow[]>([])
const actingKey = ref('')

const entryVisible = ref(false)
const entryTitle = ref('检验数据录入')
const entryRows = ref<EntryRow[]>([])
const previewVisible = ref(false)
const previewRows = ref<InspectItemRow[]>([])

function safeText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim()
}

function bhOf(row: InspectTaskRow): string {
  return safeText(row.bh)
}

function actionKey(row: InspectTaskRow, action: string): string {
  return `${bhOf(row)}#${action}`
}

function toEntryRow(item: InspectItemRow): EntryRow {
  return {
    project_bh: safeText(item.project_bh),
    productName: safeText(item.productName),
    item_xh: Number(item.item_xh ?? 0),
    testItem: safeText(item.testItem),
    stdValue: safeText(item.stdValue),
    jyResult: safeText(item.jyResult),
    item_evaluate: safeText(item.item_evaluate),
    jyResult_note: safeText(item.jyResult_note),
  }
}

function selectedBhs(): string[] {
  return selectedRows.value.map((row) => bhOf(row)).filter((bh) => bh !== '')
}

async function loadPending() {
  pendingLoading.value = true
  try {
    const result = await fetchPendingInspect(scope.value === 'mine')
    pendingList.value = result.records
    selectedRows.value = []
  } finally {
    pendingLoading.value = false
  }
}

async function loadDone() {
  doneLoading.value = true
  try {
    const result = await fetchDoneInspect(scope.value === 'mine')
    doneList.value = result.records
  } finally {
    doneLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadPending(), loadDone()])
}

function handleSelectionChange(selection: InspectTaskRow[]) {
  selectedRows.value = selection
}

/** 打开录入窗口：bhs 为要录入的样品编号 */
async function openEntry(bhs: string[]) {
  const targets = bhs.filter((bh) => bh !== '')
  if (targets.length === 0) {
    ElMessage.warning('请先勾选需要录入的样品')
    return
  }
  entryLoading.value = true
  entryVisible.value = true
  entryRows.value = []
  entryTitle.value =
    targets.length === 1 ? `检验数据录入 【${targets[0]}】` : `检验数据批量录入（${targets.length} 个样品）`
  try {
    const items = await fetchInspectItems(targets)
    entryRows.value = items.map(toEntryRow)
  } finally {
    entryLoading.value = false
  }
}

function handleBatchEntry() {
  openEntry(selectedBhs())
}

async function handlePreview() {
  const bhs = selectedBhs()
  if (bhs.length === 0) {
    ElMessage.warning('请先勾选需要预览的样品')
    return
  }
  previewVisible.value = true
  previewLoading.value = true
  previewRows.value = []
  try {
    previewRows.value = await fetchInspectItems(bhs)
  } finally {
    previewLoading.value = false
  }
}

async function handleSave(complete: boolean) {
  if (entryRows.value.length === 0) {
    ElMessage.warning('没有可保存的检验数据')
    return
  }
  saving.value = true
  try {
    const result = await saveInspectResults(
      entryRows.value.map((row) => ({
        project_bh: row.project_bh,
        item_xh: row.item_xh,
        jyResult: row.jyResult,
        item_evaluate: row.item_evaluate,
        jyResult_note: row.jyResult_note,
      })),
      complete,
    )
    if (result.messages.length > 0) {
      const brief = result.messages.slice(0, 3).join('；')
      const suffix = result.messages.length > 3 ? ' 等' : ''
      ElMessage.warning(`已保存 ${result.saved} 条；${brief}${suffix}`)
    } else if (complete) {
      ElMessage.success(`已保存 ${result.saved} 条检验数据，${result.completed} 个任务标记为检验完成`)
    } else {
      ElMessage.success(`已保存 ${result.saved} 条检验数据`)
    }
    entryVisible.value = false
    await refreshAll()
  } finally {
    saving.value = false
  }
}

async function handleRevert(row: InspectTaskRow) {
  const bh = bhOf(row)
  if (!bh) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将样品「${bh}」转回待检验吗？`,
      '转待检验',
      { type: 'warning', confirmButtonText: '转待检验', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  actingKey.value = actionKey(row, 'inspect')
  try {
    await revertInspect([bh])
    ElMessage.success(`样品 ${bh} 已转回待检验`)
    await refreshAll()
  } finally {
    actingKey.value = ''
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.input-inspection {
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

.filter-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.action-btn {
  color: #ffffff;
  background: #5f7c8b;
  border-color: #5f7c8b;
}

.action-btn:hover,
.action-btn:focus {
  color: #ffffff;
  background: #71909f;
  border-color: #71909f;
}

.selected-tip {
  color: #909399;
  font-size: 13px;
}

.result-bad {
  color: #dc2626;
  font-weight: 600;
}
</style>
