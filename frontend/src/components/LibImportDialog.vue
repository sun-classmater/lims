<template>
  <el-dialog
    :model-value="modelValue"
    title="导入检验项目库 (lib.xls)"
    width="900px"
    destroy-on-close
    @update:model-value="handleVisibleChange"
  >
    <p class="subtitle">
      项目库文件上传到服务器后，将要求您确认无误再导入数据库。
    </p>
    <p class="warning">
      如果项目编号已存在，请先删除该编号项目，再导入项目。
    </p>

    <div class="file-line">
      <span class="file-label">请选择项目库（lib.xls）文件:</span>
      <button
        type="button"
        class="file-button"
        @click="fileInput?.click()"
      >
        选择文件
      </button>
      <span class="file-name">{{ selectedFile ? selectedFile.name : '未选择文件' }}</span>
      <input
        ref="fileInput"
        class="hidden-input"
        type="file"
        accept=".xls"
        @change="handleFileChange"
      >
    </div>

    <div class="action-line">
      <el-button
        type="primary"
        :loading="uploading"
        :disabled="!selectedFile"
        @click="handleUpload"
      >
        上传项目库文件到服务器
      </el-button>
      <el-button
        :loading="downloading"
        @click="handleDownloadTemplate"
      >
        下载项目库文件模板(lib.xls)
      </el-button>
      <el-link
        type="primary"
        :underline="false"
        @click="templateVisible = true"
      >
        查看lib.xls模板(png)
      </el-link>
    </div>

    <el-alert
      v-if="preview && preview.duplicated"
      type="error"
      :closable="false"
      show-icon
      :title="`项目编号 ${preview.productId} 已存在，请先删除该编号项目再导入`"
      class="block"
    />

    <div
      v-if="preview"
      class="preview-block"
    >
      <div class="preview-summary">
        <span>项目库编号：<b>{{ preview.productId }}</b></span>
        <span>产品分类：<b>{{ preview.prdCategory }}</b></span>
        <span>检验项目表：<b>{{ preview.libName }}</b></span>
        <span>检测项目数：<b>{{ preview.total }}</b></span>
      </div>
      <el-table
        :data="preview.rows"
        border
        stripe
        size="small"
        max-height="320"
        empty-text="没有解析到检测项目"
      >
        <el-table-column
          prop="xh"
          label="序号"
          width="60"
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
          min-width="130"
          show-overflow-tooltip
        />
        <el-table-column
          prop="mathod"
          label="检验方法"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="methodNote"
          label="第几法"
          width="90"
          align="center"
        />
        <el-table-column
          prop="standardValue"
          label="标准值"
          width="90"
          align="center"
        />
        <el-table-column
          prop="lowerLimit"
          label="检出限"
          width="90"
          align="center"
        />
      </el-table>
      <p
        v-if="preview.total > preview.rows.length"
        class="more-tip"
      >
        预览仅显示前 {{ preview.rows.length }} 条，确认导入将写入全部 {{ preview.total }} 条。
      </p>
    </div>

    <template #footer>
      <el-button @click="handleVisibleChange(false)">
        取消
      </el-button>
      <el-button
        type="primary"
        :loading="confirming"
        :disabled="!preview || preview.duplicated"
        @click="handleConfirm"
      >
        确认导入
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="templateVisible"
    title="上传的项目库Excel文件格式说明"
    width="880px"
  >
    <ol class="format-list">
      <li>Excel 文件名任意，扩展名必须为 .xls</li>
      <li>单元格 A1：项目库产品编号，建议 NA、XA、SA 开头</li>
      <li>单元格 B1：产品分类名称</li>
      <li>单元格 C1：检验项目表名称</li>
      <li>检验项目数据必须从第 3 行开始</li>
      <li>检测项目包含单位，以“，”分隔</li>
      <li>数据必须位于 “sheet1” 页</li>
      <li>数据的右边、下边区域必须清空</li>
    </ol>
    <el-table
      :data="templateRows"
      border
      size="small"
    >
      <el-table-column
        prop="xh"
        label="序号"
        width="60"
        align="center"
      />
      <el-table-column
        prop="testItem"
        label="检测项目"
        min-width="260"
        show-overflow-tooltip
      />
      <el-table-column
        prop="basis"
        label="检验依据"
        min-width="130"
      />
      <el-table-column
        prop="mathod"
        label="检验方法"
        min-width="150"
      />
      <el-table-column
        prop="methodNote"
        label="第几法"
        width="80"
        align="center"
      />
      <el-table-column
        prop="standardValue"
        label="标准值"
        width="80"
        align="center"
      />
      <el-table-column
        prop="lowerLimit"
        label="检出限"
        width="80"
        align="center"
      />
    </el-table>
    <p class="template-meta">
      A1：SA001；B1：一、调味品；C1：SA表sa001-1调味品产品检验项目
    </p>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import {
  confirmLibImport,
  downloadLibTemplate,
  previewLibImport,
  type LibImportPreview,
  type LibImportRow,
} from '@/api/libImport'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'imported'): void
}>()

const fileInput = ref<HTMLInputElement>()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const confirming = ref(false)
const downloading = ref(false)
const preview = ref<LibImportPreview | null>(null)
const templateVisible = ref(false)

const templateRows: LibImportRow[] = [
  {
    xh: 1,
    testItem: '苯甲酸及其钠盐（以苯甲酸计），g/kg',
    basis: 'GB 2760-2014',
    mathod: 'GB 5009.28-2016',
    standardValue: '≤1.0',
    lowerLimit: '0.9',
    methodNote: '第一法',
  },
  {
    xh: 2,
    testItem: '山梨酸及其钾盐（以山梨酸计），g/kg',
    basis: 'GB 2760-2014',
    mathod: 'GB 5009.28-2016',
    standardValue: '≤1.0',
    lowerLimit: '0.9',
    methodNote: '第一法',
  },
]

function reset() {
  selectedFile.value = null
  preview.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      reset()
    }
  },
)

function handleVisibleChange(visible: boolean) {
  emit('update:modelValue', visible)
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  preview.value = null
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择项目库文件')
    return
  }
  uploading.value = true
  try {
    preview.value = await previewLibImport(selectedFile.value)
    if (preview.value.duplicated) {
      ElMessage.warning(`项目编号 ${preview.value.productId} 已存在，请先删除该编号项目`)
    } else {
      ElMessage.success(`解析成功，共 ${preview.value.total} 个检测项目，请确认后导入`)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件解析失败')
  } finally {
    uploading.value = false
  }
}

async function handleConfirm() {
  if (!preview.value) {
    return
  }
  confirming.value = true
  try {
    const result = await confirmLibImport(preview.value.token)
    ElMessage.success(`项目库 ${result.productId} 导入成功，共 ${result.inserted} 个检测项目`)
    emit('imported')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败')
  } finally {
    confirming.value = false
  }
}

async function handleDownloadTemplate() {
  downloading.value = true
  try {
    await downloadLibTemplate()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '模板下载失败')
  } finally {
    downloading.value = false
  }
}
</script>

<style scoped>
.subtitle {
  margin: 0 0 8px;
  color: #303133;
  font-size: 14px;
}

.warning {
  margin: 0 0 18px;
  color: #c45656;
  font-size: 14px;
  text-align: center;
}

.file-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.file-label {
  color: #303133;
  font-size: 14px;
}

.file-button {
  padding: 3px 10px;
  color: #606266;
  font-size: 13px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
}

.file-button:hover {
  color: #409eff;
  border-color: #c6e2ff;
}

.file-name {
  color: #909399;
  font-size: 13px;
}

.hidden-input {
  display: none;
}

.action-line {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.block {
  margin-bottom: 14px;
}

.preview-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 10px;
  padding: 10px 14px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  color: #7a1f1f;
  font-size: 13px;
}

.more-tip {
  margin: 8px 0 0;
  color: #909399;
  font-size: 12px;
}

.format-list {
  margin: 0 0 14px;
  padding-left: 22px;
  color: #c45656;
  font-size: 14px;
  line-height: 1.9;
}

.template-meta {
  margin: 10px 0 0;
  color: #606266;
  font-size: 13px;
}
</style>
