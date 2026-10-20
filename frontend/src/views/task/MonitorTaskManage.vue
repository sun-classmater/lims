<template>
  <div class="monitor-task-manage">
    <div class="section-title">
      监抽任务管理
    </div>

    <el-card
      class="filter-card"
      shadow="never"
    >
      <div class="filter-grid">
        <div class="filter-group">
          <span class="filter-label">任务编号:</span>
          <el-input
            v-model="query.taskno"
            class="taskno-input"
            clearable
            placeholder="请输入任务编号"
            @keyup.enter="handleSearch"
          />
        </div>

        <div class="filter-group filter-group-wide">
          <span class="filter-label">任务时间区间:</span>
          <el-date-picker
            v-model="query.dateStart"
            type="date"
            class="date-picker"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
          />
          <span class="range-sep">至</span>
          <el-date-picker
            v-model="query.dateEnd"
            type="date"
            class="date-picker"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
          />
        </div>

        <div class="filter-group">
          <span class="filter-label">任务来源:</span>
          <el-input
            v-model="query.source"
            class="source-input"
            clearable
            placeholder="任务来源"
            @keyup.enter="handleSearch"
          />
        </div>

        <el-button
          type="primary"
          :icon="Search"
          @click="handleSearch"
        >
          查找
        </el-button>

        <el-button
          type="danger"
          :icon="Plus"
          @click="openCreate"
        >
          新增任务
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
          prop="id"
          label="id"
          width="70"
          align="center"
          sortable
        />
        <el-table-column
          prop="taskno"
          label="任务编号"
          min-width="150"
          sortable
        >
          <template #default="{ row }">
            <TaskNoLink :row="row" />
          </template>
        </el-table-column>
        <el-table-column
          prop="leader"
          label="责任人"
          width="90"
          align="center"
        />
        <el-table-column
          prop="source"
          label="任务来源"
          min-width="170"
        />
        <el-table-column
          prop="region"
          label="省市区县"
          width="100"
          align="center"
        />
        <el-table-column
          prop="note"
          label="任务描述"
          min-width="280"
          show-overflow-tooltip
        />
        <el-table-column
          prop="date"
          label="任务接受日期"
          width="130"
          align="center"
          sortable
        />
        <el-table-column
          label="任务通知单"
          width="110"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              size="small"
              class="notice-btn"
              @click="handleNotice(row)"
            >
              通知单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="新增任务"
      width="520px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
      >
        <el-form-item
          label="任务编号"
          prop="taskno"
        >
          <el-input
            v-model="form.taskno"
            placeholder="请输入任务编号"
          />
        </el-form-item>
        <el-form-item
          label="责任人"
          prop="leader"
        >
          <el-input v-model="form.leader" />
        </el-form-item>
        <el-form-item
          label="任务来源"
          prop="source"
        >
          <el-input v-model="form.source" />
        </el-form-item>
        <el-form-item
          label="省市区县"
          prop="region"
        >
          <el-input v-model="form.region" />
        </el-form-item>
        <el-form-item
          label="任务描述"
          prop="note"
        >
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item
          label="任务接受日期"
          prop="date"
        >
          <el-date-picker
            v-model="form.date"
            type="date"
            class="form-date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="submit"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { createData, fetchData } from '@/api/data'
import TaskNoLink from '@/components/TaskNoLink.vue'

interface QueryForm {
  taskno: string
  dateStart: string
  dateEnd: string
  source: string
}

interface TaskForm {
  taskno: string
  leader: string
  source: string
  region: string
  note: string
  date: string
}

interface SjTaskRow extends Record<string, unknown> {
  id: number
  taskno: string
}

function createDefaultQuery(): QueryForm {
  return {
    taskno: '',
    dateStart: '',
    dateEnd: '',
    source: '',
  }
}

function createDefaultForm(): TaskForm {
  return {
    taskno: '',
    leader: '',
    source: '',
    region: '',
    note: '',
    date: '',
  }
}

const query = reactive<QueryForm>(createDefaultQuery())
const form = reactive<TaskForm>(createDefaultForm())
const loading = ref(false)
const submitting = ref(false)
const rows = ref<SjTaskRow[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const rules: FormRules = {
  taskno: [
    {
      required: true,
      message: '请输入任务编号',
      trigger: 'blur',
    },
  ],
}

async function fetchRows() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: 1,
      size: 200,
    }
    if (query.taskno) {
      params.taskno = query.taskno
    }
    if (query.source) {
      params.source = query.source
    }
    if (query.dateStart) {
      params.dateStart = query.dateStart
    }
    if (query.dateEnd) {
      params.dateEnd = query.dateEnd
    }

    const result = await fetchData('sjtask', params)
    rows.value = result.records as SjTaskRow[]
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchRows()
}

function openCreate() {
  Object.assign(form, createDefaultForm())
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await createData('sjtask', { ...form })
    ElMessage.success('新增任务成功')
    dialogVisible.value = false
    await fetchRows()
  } finally {
    submitting.value = false
  }
}

function handleNotice(row: SjTaskRow) {
  ElMessage.info(`任务「${row.taskno}」的通知单功能暂未开放`)
}

onMounted(() => {
  fetchRows()
})
</script>

<style scoped>
.monitor-task-manage {
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
  flex-basis: 430px;
}

.filter-label {
  flex-shrink: 0;
  color: #374151;
  font-size: 13px;
  white-space: nowrap;
}

.taskno-input {
  width: 200px;
}

.source-input {
  width: 200px;
}

.date-picker {
  width: 150px;
}

.range-sep {
  color: #374151;
  font-size: 13px;
}

.form-date {
  width: 100%;
}

.table-card {
  border-radius: 8px;
}

.notice-btn {
  padding: 4px 10px;
  color: #ffffff;
  background: #4a7a9b;
  border-color: #4a7a9b;
}

.notice-btn:hover,
.notice-btn:focus {
  color: #ffffff;
  background: #5e90b2;
  border-color: #5e90b2;
}
</style>
