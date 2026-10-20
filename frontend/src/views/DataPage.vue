<template>
  <div class="data-page">
    <div class="page-header">
      <h2>{{ config.title }}</h2>
      <div class="header-actions">
        <el-button
          v-if="config.canUpload"
          :icon="Upload"
          @click="fileInput?.click()"
        >
          上传文件
        </el-button>
        <input
          ref="fileInput"
          class="hidden-input"
          type="file"
          @change="handleFileChange"
        >
        <el-button
          v-if="config.canCreate"
          :type="config.createButtonType || 'primary'"
          :icon="Plus"
          @click="handleCreate"
        >
          {{ config.createButtonText || '新增' }}
        </el-button>
      </div>
    </div>

    <div
      v-if="config.canUpload"
      class="file-list"
    >
      <div class="file-list-title">
        已上传文件
      </div>
      <el-empty
        v-if="files.length === 0"
        :image-size="64"
        description="暂无上传文件"
      />
      <div
        v-for="file in files"
        :key="file.name"
        class="file-item"
      >
        <el-icon>
          <Document />
        </el-icon>
        <a
          :href="file.url"
          target="_blank"
        >
          {{ file.name }}
        </a>
      </div>
    </div>

    <div
      v-if="showFilters"
      class="filters"
    >
      <el-input
        v-if="config.showKeyword !== false"
        v-model="keyword"
        class="keyword-input"
        :prefix-icon="Search"
        clearable
        placeholder="关键词搜索"
        @keyup.enter="handleSearch"
      />
      <template
        v-for="field in config.searchFields || []"
        :key="field.prop"
      >
        <el-select
          v-if="field.type === 'select'"
          v-model="filters[field.prop]"
          clearable
          :placeholder="field.placeholder || field.label"
        >
          <el-option
            v-for="option in field.options || []"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-input
          v-else
          v-model="filters[field.prop]"
          clearable
          :placeholder="field.placeholder || field.label"
        />
      </template>
      <el-button
        type="primary"
        :icon="Search"
        @click="handleSearch"
      >
        {{ config.searchButtonText || '查询' }}
      </el-button>
      <el-button
        :icon="Refresh"
        @click="handleReset"
      >
        重置
      </el-button>
      <el-button
        v-if="config.canExport !== false"
        :icon="Download"
        @click="handleExport"
      >
        导出
      </el-button>
    </div>

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
          v-if="config.showIndex"
          type="index"
          label="序号"
          width="70"
          align="center"
          :index="resolveRowIndex"
        />
        <el-table-column
          v-for="column in config.columns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth"
          :align="column.align"
          show-overflow-tooltip
        >
          <template
            v-if="column.detail"
            #default="{ row }"
          >
            <el-link
              type="primary"
              @click="openDetail(row)"
            >
              {{ formatCell(row[column.prop]) }}
            </el-link>
          </template>
          <template
            v-else-if="entityLinkOfColumn(column.prop) === 'project'"
            #default="{ row }"
          >
            <ProjectNoLink
              :row="row"
              :prop="column.prop"
            />
          </template>
          <template
            v-else-if="entityLinkOfColumn(column.prop) === 'task'"
            #default="{ row }"
          >
            <TaskNoLink
              :row="row"
              :prop="column.prop"
            />
          </template>
        </el-table-column>
        <el-table-column
          v-if="config.canEdit || config.canDelete"
          :label="config.actionColumnLabel || '操作'"
          width="150"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="config.canEdit"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="config.canDelete"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              {{ config.deleteButtonText || '删除' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchRows"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="680px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item
          v-for="field in config.formFields || []"
          :key="field.prop"
          :label="field.label"
          :prop="field.prop"
        >
          <el-select
            v-if="field.type === 'select'"
            v-model="form[field.prop]"
            :disabled="editing && field.disabled"
            clearable
            :placeholder="field.placeholder || `请输入${field.label}`"
          >
            <el-option
              v-for="option in field.options || []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input
            v-else
            v-model="form[field.prop]"
            :type="field.type === 'textarea' ? 'textarea' : field.type === 'number' ? 'number' : 'text'"
            :disabled="editing && field.disabled"
            :rows="field.type === 'textarea' ? 4 : 1"
            :placeholder="field.placeholder || `请输入${field.label}`"
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

    <el-dialog
      v-model="detailVisible"
      :title="detailTitle"
      width="900px"
      destroy-on-close
    >
      <div
        v-if="detailRow"
        class="detail-summary"
      >
        <span>编号：<b>{{ detailRow.id }}</b></span>
        <span>产品类别：<b>{{ detailRow.prd_category }}</b></span>
        <span>检验项目表：<b>{{ detailRow.libName }}</b></span>
      </div>
      <el-table
        v-loading="detailLoading"
        :data="detailRows"
        border
        stripe
        max-height="480"
      >
        <el-table-column
          type="index"
          label="序号"
          width="70"
          align="center"
        />
        <el-table-column
          prop="testItem"
          label="检测项目"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="basis"
          label="检验依据"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column
          prop="mathod"
          label="检验方法"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="stdValue"
          label="标准值"
          width="100"
          align="center"
        />
        <el-table-column
          prop="num_lowerst"
          label="检出限"
          width="100"
          align="center"
        />
        <el-table-column
          prop="method_note"
          label="方法备注"
          min-width="120"
          show-overflow-tooltip
        />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Download,
  Document,
  Plus,
  Refresh,
  Search,
  Upload,
} from '@element-plus/icons-vue'

import {
  createData,
  deleteData,
  fetchData,
  fetchExport,
  fetchFiles,
  updateData,
  uploadFile,
} from '@/api/data'
import ProjectNoLink from '@/components/ProjectNoLink.vue'
import TaskNoLink from '@/components/TaskNoLink.vue'
import type { FileInfo } from '@/api/data'
import type { PageConfig } from '@/types/page'
import { entityLinkOfColumn } from '@/utils/entityDialogs'

const route = useRoute()
const router = useRouter()
const config = route.meta.config as PageConfig

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref<Array<Record<string, unknown>>>([])
const filters = reactive<Record<string, string>>({})
const form = reactive<Record<string, string>>({})
const formRef = ref<FormInstance>()
const fileInput = ref<HTMLInputElement>()
const editRow = ref<Record<string, unknown> | null>(null)
const files = ref<FileInfo[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRows = ref<Array<Record<string, unknown>>>([])
const detailRow = ref<Record<string, unknown> | null>(null)

const showFilters = computed(() => config.showFilters !== false)

const dialogTitle = computed(() => {
  if (editing.value) {
    return '编辑数据'
  }
  return config.createDialogTitle || '新增数据'
})

const detailTitle = computed(() => {
  const id = detailRow.value?.id ?? ''
  return `产品检验项目库信息${id ? ` - ${id}` : ''}`
})

const formRules = computed<FormRules>(() => {
  const rules: FormRules = {}
  for (const field of config.formFields || []) {
    if (field.required) {
      rules[field.prop] = [
        {
          required: true,
          message: `请输入${field.label}`,
          trigger: 'blur',
        },
      ]
    }
  }
  return rules
})

function buildParams() {
  const params: Record<string, string | number> = {
    page: page.value,
    size: size.value,
  }
  if (keyword.value) {
    params.keyword = keyword.value
  }
  Object.entries({ ...(config.defaultFilters || {}), ...filters }).forEach(([key, value]) => {
    if (value) {
      params[key] = value
    }
  })
  return params
}

async function fetchRows() {
  loading.value = true
  try {
    const result = await fetchData(config.entity, buildParams())
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchRows()
}

function handleReset() {
  keyword.value = ''
  Object.keys(filters).forEach((key) => {
    filters[key] = ''
  })
  page.value = 1
  fetchRows()
}

function handleSizeChange() {
  page.value = 1
  fetchRows()
}

function resolveRowIndex(index: number) {
  return (page.value - 1) * size.value + index + 1
}

function resetForm() {
  for (const field of config.formFields || []) {
    form[field.prop] = String(field.defaultValue ?? '')
  }
}

function openCreate() {
  editing.value = false
  editRow.value = null
  resetForm()
  dialogVisible.value = true
}

/** 配置了 createLink 时，新增/导入按钮改为跳转到对应页面 */
function handleCreate() {
  if (config.createLink) {
    router.push(config.createLink)
    return
  }
  openCreate()
}

function openEdit(row: Record<string, unknown>) {
  editing.value = true
  editRow.value = row
  resetForm()
  for (const field of config.formFields || []) {
    form[field.prop] = String(row[field.prop] ?? '')
  }
  dialogVisible.value = true
}

function formatCell(value: unknown): string {
  return value === null || value === undefined ? '' : String(value)
}

async function openDetail(row: Record<string, unknown>) {
  const productId = String(row.id ?? '')
  if (!productId) {
    return
  }
  detailRow.value = row
  detailRows.value = []
  detailVisible.value = true
  detailLoading.value = true
  try {
    const result = await fetchData('lib', {
      page: 1,
      size: 200,
      productId,
    })
    detailRows.value = result.records
  } finally {
    detailLoading.value = false
  }
}

function serializeForm() {
  const payload: Record<string, unknown> = { ...form }
  if (editing.value && editRow.value) {
    for (const key of config.primaryKeys) {
      payload[key] = editRow.value[key]
    }
  }
  for (const field of config.formFields || []) {
    if (field.type === 'number' && payload[field.prop] !== '') {
      payload[field.prop] = Number(payload[field.prop])
    }
  }
  return payload
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const payload = serializeForm()
    if (editing.value) {
      await updateData(config.entity, payload)
    } else {
      await createData(config.entity, payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await fetchRows()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Record<string, unknown>) {
  await ElMessageBox.confirm(
    config.deleteConfirmText || '确定删除这条数据吗？',
    '删除确认',
    {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    },
  )

  const keys: Record<string, string> = {}
  for (const key of config.primaryKeys) {
    keys[key] = String(row[key] ?? '')
  }
  await deleteData(config.entity, keys)
  ElMessage.success('删除成功')
  await fetchRows()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }

  try {
    const result = await uploadFile(file)
    await loadFiles()
    ElMessage.success(`上传成功：${result.originalName}`)
  } finally {
    input.value = ''
  }
}

async function loadFiles() {
  if (config.canUpload) {
    files.value = await fetchFiles()
  }
}

async function handleExport() {
  const records = await fetchExport(config.entity, buildParams())
  const headers = config.columns.map((column) => column.label).join(',')
  const lines = records.map((row) =>
    config.columns
      .map((column) => String(row[column.prop] ?? ''))
      .join(','),
  )
  const csv = [headers, ...lines].join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${config.entity}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  for (const field of config.searchFields || []) {
    filters[field.prop] = ''
  }
  resetForm()
  fetchRows()
  loadFiles()
})
</script>

<style scoped>
.data-page {
  min-width: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-header h2 {
  margin: 0;
  color: #111827;
  font-size: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.file-list {
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.file-list-title {
  margin-bottom: 10px;
  color: #111827;
  font-size: 14px;
  font-weight: 600;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  color: #374151;
}

.file-item a {
  color: #2563eb;
  text-decoration: none;
}

.filters :deep(.el-input),
.filters :deep(.el-select) {
  width: 200px;
}

.keyword-input {
  width: 240px;
}

.table-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.hidden-input {
  display: none;
}

.detail-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 14px;
  padding: 12px 16px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 8px;
  color: #7a1f1f;
  font-size: 14px;
}

.detail-summary b {
  color: #c0392b;
}

@media (max-width: 760px) {
  .filters :deep(.el-input),
  .filters :deep(.el-select),
  .keyword-input {
    width: 100%;
  }
}
</style>
