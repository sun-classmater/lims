<template>
  <div class="register-confirm">
    <el-row :gutter="16">
      <!-- 左：样品登记信息维护与确认 -->
      <el-col
        :xs="24"
        :md="12"
      >
        <el-card
          shadow="never"
          class="panel"
        >
          <h3 class="panel-title panel-title-danger">
            样品登记信息维护与确认
          </h3>

          <div class="filter-row">
            <span class="filter-label">任务编号:</span>
            <el-input
              v-model="pendingQuery.taskno"
              class="filter-input"
              placeholder="请输入完整的任务编号"
              clearable
              @keyup.enter="searchPending"
            />
            <span class="filter-label">样品编号</span>
            <el-input
              v-model="pendingQuery.bh"
              class="filter-input"
              placeholder="请输入项目编号"
              clearable
              @keyup.enter="searchPending"
            />
            <el-button
              type="primary"
              @click="searchPending"
            >
              查找
            </el-button>
          </div>

          <div class="batch-row">
            <el-button
              type="danger"
              :disabled="selectedIds.length === 0"
              :loading="confirming"
              @click="handleBatchConfirm"
            >
              批量确认【登记完毕】
            </el-button>
          </div>

          <div class="op-label">
            操作项目
          </div>

          <el-table
            v-loading="pendingLoading"
            :data="pendingList"
            border
            stripe
            size="small"
            height="430"
            class="data-table"
            @selection-change="handleSelectionChange"
          >
            <el-table-column
              type="selection"
              width="40"
              align="center"
            />
            <el-table-column
              prop="taskno"
              label="任务编号"
              min-width="110"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <TaskNoLink :row="row" />
              </template>
            </el-table-column>
            <el-table-column
              label="样品信息维护"
              width="98"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openMaintainDialog(row)"
                >
                  维护
                </el-button>
              </template>
            </el-table-column>
            <el-table-column
              prop="dwname_sj_nj"
              label="受检单位"
              min-width="100"
              show-overflow-tooltip
            />
            <el-table-column
              prop="productName"
              label="样品名称"
              min-width="90"
              show-overflow-tooltip
            />
            <el-table-column
              prop="date_reg"
              label="登记日期"
              width="100"
              align="center"
            />
            <el-table-column
              prop="registerName"
              label="登记人"
              width="80"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column
              label="确认登记"
              width="88"
              align="center"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  :loading="confirmingId === row.id_nj"
                  @click="handleSingleConfirm(row)"
                >
                  确认登记
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager-row">
            <el-pagination
              v-model:current-page="pendingPage"
              :page-size="pageSize"
              :total="pendingTotal"
              layout="total, prev, pager, next"
              small
              background
              @current-change="loadPending"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右：已确认登记样品 -->
      <el-col
        :xs="24"
        :md="12"
      >
        <el-card
          shadow="never"
          class="panel"
        >
          <h3 class="panel-title panel-title-danger">
            已确认登记样品
          </h3>

          <div class="filter-row">
            <span class="filter-label">搜索项目</span>
            <el-input
              v-model="registeredQuery.bh"
              class="filter-input-wide"
              placeholder="请输入项目编号"
              clearable
              @keyup.enter="searchRegistered"
            />
            <el-button
              type="primary"
              @click="searchRegistered"
            >
              查找
            </el-button>
          </div>

          <div class="op-label">
            操作项目
          </div>

          <el-table
            v-loading="registeredLoading"
            :data="registeredList"
            border
            stripe
            size="small"
            height="478"
            class="data-table"
          >
            <el-table-column
              prop="taskno"
              label="任务编号"
              min-width="110"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <TaskNoLink :row="row" />
              </template>
            </el-table-column>
            <el-table-column
              prop="bh"
              label="样品编号"
              min-width="130"
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
              prop="date_yywc"
              label="预约完成日期"
              width="105"
              align="center"
            />
            <el-table-column
              prop="registerName"
              label="登记人"
              width="80"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column
              label="转待登记"
              width="90"
              align="center"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  type="warning"
                  size="small"
                  plain
                  :loading="revertingId === row.id_nj"
                  @click="handleRevert(row)"
                >
                  转待登记
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              没有找到匹配的记录
            </template>
          </el-table>

          <div class="pager-row">
            <el-pagination
              v-model:current-page="registeredPage"
              :page-size="pageSize"
              :total="registeredTotal"
              layout="total, prev, pager, next"
              small
              background
              @current-change="loadRegistered"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 样品信息维护 -->
    <el-dialog
      v-model="maintainVisible"
      title="样品信息维护"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form
        v-if="maintainForm"
        v-loading="maintainLoading"
        :model="maintainForm"
        label-width="104px"
        class="maintain-form"
      >
        <el-row :gutter="12">
          <el-col
            v-for="field in maintainFields"
            :key="field.prop"
            :span="12"
          >
            <el-form-item :label="field.label">
              <el-input
                v-model="maintainForm[field.prop]"
                :type="field.type === 'textarea' ? 'textarea' : 'text'"
                :rows="field.type === 'textarea' ? 2 : undefined"
                placeholder="请输入"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="maintainVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="maintainLoading"
          @click="handleSaveMaintain"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { updateData } from '@/api/data'
import {
  confirmRegister,
  fetchPendingProjects,
  fetchRegisteredProjects,
  revertRegister,
  type PendingProject,
  type RegisteredProject,
} from '@/api/sampleRegister'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'
import { fetchProjectRow } from '@/utils/entityDialogs'

const pageSize = 10

const pendingLoading = ref(false)
const registeredLoading = ref(false)
const confirming = ref(false)
const confirmingId = ref<number | null>(null)
const revertingId = ref<number | null>(null)
const saving = ref(false)

const pendingList = ref<PendingProject[]>([])
const registeredList = ref<RegisteredProject[]>([])
const pendingTotal = ref(0)
const registeredTotal = ref(0)
const pendingPage = ref(1)
const registeredPage = ref(1)
const selectedIds = ref<number[]>([])

const pendingQuery = reactive({ taskno: '', bh: '' })
const registeredQuery = reactive({ bh: '' })

const maintainVisible = ref(false)
const maintainForm = ref<Record<string, string> | null>(null)
const maintainId = ref<number | null>(null)
const maintainLoading = ref(false)

interface MaintainField {
  prop: string
  label: string
  type?: 'textarea'
}

const maintainFields: MaintainField[] = [
  { prop: 'productName', label: '样品名称' },
  { prop: 'ggxh', label: '规格型号' },
  { prop: 'ypsb', label: '样品商标' },
  { prop: 'ypdj', label: '样品等级' },
  { prop: 'ypNum', label: '样品数量' },
  { prop: 'byNum', label: '备样量' },
  { prop: 'cyjs', label: '抽样基数' },
  { prop: 'cyaddr', label: '抽样地点' },
  { prop: 'ypCharacter', label: '样品性状' },
  { prop: 'ypSituation', label: '样品状况' },
  { prop: 'ypSaveReq', label: '存储要求' },
  { prop: 'yyProcess', label: '余样处理' },
  { prop: 'ypbz', label: '样品备注', type: 'textarea' },
]

async function loadPending() {
  pendingLoading.value = true
  try {
    const result = await fetchPendingProjects({
      page: pendingPage.value,
      size: pageSize,
      taskno: pendingQuery.taskno.trim() || undefined,
      bh: pendingQuery.bh.trim() || undefined,
    })
    pendingList.value = result.records
    pendingTotal.value = result.total
  } finally {
    pendingLoading.value = false
  }
}

async function loadRegistered() {
  registeredLoading.value = true
  try {
    const result = await fetchRegisteredProjects({
      page: registeredPage.value,
      size: pageSize,
      bh: registeredQuery.bh.trim() || undefined,
    })
    registeredList.value = result.records
    registeredTotal.value = result.total
  } finally {
    registeredLoading.value = false
  }
}

function searchPending() {
  pendingPage.value = 1
  loadPending()
}

function searchRegistered() {
  registeredPage.value = 1
  loadRegistered()
}

function handleSelectionChange(rows: PendingProject[]) {
  selectedIds.value = rows.map((row) => row.id_nj)
}

async function handleSingleConfirm(row: PendingProject) {
  confirmingId.value = row.id_nj
  try {
    const affected = await confirmRegister([row.id_nj])
    if (affected > 0) {
      ElMessage.success(`样品 ${row.bh ?? row.id_nj} 已确认登记`)
      await refreshAfterChange()
    } else {
      ElMessage.warning('该样品已登记，无需重复确认')
      await refreshAfterChange()
    }
  } finally {
    confirmingId.value = null
  }
}

async function handleBatchConfirm() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先勾选需要确认登记的样品')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将选中的 ${selectedIds.value.length} 个样品批量登记完毕吗？`,
      '批量确认登记',
      { type: 'warning', confirmButtonText: '确认登记', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  confirming.value = true
  try {
    const affected = await confirmRegister(selectedIds.value)
    ElMessage.success(`批量确认完成，共登记 ${affected} 个样品`)
    selectedIds.value = []
    await refreshAfterChange()
  } finally {
    confirming.value = false
  }
}

async function handleRevert(row: RegisteredProject) {
  try {
    await ElMessageBox.confirm(
      `确认将样品 ${row.bh ?? row.id_nj} 转为待登记状态吗？`,
      '转待登记',
      { type: 'warning', confirmButtonText: '转待登记', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  revertingId.value = row.id_nj
  try {
    await revertRegister(row.id_nj)
    ElMessage.success('已转为待登记')
    await refreshAfterChange()
  } finally {
    revertingId.value = null
  }
}

async function refreshAfterChange() {
  const pendingTask = loadPending()
  const registeredTask = loadRegistered()
  await Promise.all([pendingTask, registeredTask])
}

function buildMaintainForm(source: Record<string, unknown>): Record<string, string> {
  const form: Record<string, string> = {}
  for (const field of maintainFields) {
    const value = source[field.prop]
    form[field.prop] = value == null ? '' : String(value)
  }
  return form
}

/**
 * 样品信息维护：内容一律以 project 表为准（按样品编号 bh 取值），
 * 列表里的字段只是子集且可能滞后，因此打开时回查一次最新数据。
 */
async function openMaintainDialog(row: PendingProject) {
  maintainId.value = row.id_nj
  maintainForm.value = {}
  maintainVisible.value = true
  maintainLoading.value = true
  try {
    const project = await fetchProjectRow(String(row.bh ?? '').trim())
    if (project) {
      const projectId = project.id_nj
      maintainId.value =
        projectId === null || projectId === undefined ? row.id_nj : Number(projectId)
      maintainForm.value = buildMaintainForm(project)
    } else {
      maintainForm.value = buildMaintainForm(row as unknown as Record<string, unknown>)
      ElMessage.warning('未从项目表读取到该样品，已显示列表中的内容')
    }
  } finally {
    maintainLoading.value = false
  }
}

async function handleSaveMaintain() {
  if (!maintainForm.value || maintainId.value === null || maintainLoading.value) {
    return
  }
  saving.value = true
  try {
    await updateData('project', {
      id_nj: maintainId.value,
      ...maintainForm.value,
    })
    ElMessage.success('样品信息保存成功')
    maintainVisible.value = false
    await loadPending()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadPending()
  loadRegistered()
})
</script>

<style scoped>
.register-confirm {
  padding: 12px 16px;
}

.panel {
  margin-bottom: 12px;
}

.panel-title {
  margin: 0 0 14px;
  font-size: 17px;
  font-weight: 700;
}

.panel-title-danger {
  color: #c45656;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.filter-label {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.filter-input {
  width: 150px;
}

.filter-input-wide {
  width: 190px;
}

.batch-row {
  margin-bottom: 10px;
}

.op-label {
  margin-bottom: 8px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.data-table {
  width: 100%;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.maintain-form :deep(.el-textarea__inner) {
  resize: vertical;
}
</style>
