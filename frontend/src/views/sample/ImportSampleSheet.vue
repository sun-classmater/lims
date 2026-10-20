<template>
  <div class="import-sample-sheet">
    <h2 class="page-title">
      导入抽样单数据（注意xls文件格式要求）
    </h2>

    <p class="steps">
      步骤:1、上传抽样单到服务器&nbsp;&nbsp;2、用户确认&nbsp;&nbsp;3、确认无误导入数据库。
    </p>

    <div class="sheet-box">
      <div class="choose-row">
        <span class="choose-label">请选择抽样单（.xls）文件:</span>
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".xls"
          :on-change="handleFileChange"
        >
          <el-button>
            选择文件
          </el-button>
        </el-upload>
        <span
          class="file-name"
          :class="{ 'file-name-empty': !selectedFile }"
        >
          {{ selectedFile ? selectedFile.name : '未选择文件' }}
        </span>
      </div>

      <div class="upload-action">
        <el-button
          :loading="uploading"
          @click="handleUpload"
        >
          上传抽样单数据到服务器
        </el-button>
      </div>

      <div class="notes">
        <span class="notes-title">采样单Excel文件注意点：</span>
        <span class="notes-body">
          【1】文件名后缀为.xls，数据在“sheet1”中<br>
          【2】A1列为文件标识，防止重复导入<br>
          【3】数据从第3行开始，按列要求对应
        </span>
      </div>

      <p class="template-row">
        <el-link
          type="primary"
          :underline="false"
          @click="handleDownloadTemplate"
        >
          查看抽样单.xls模板(png)
        </el-link>
      </p>
    </div>

    <el-dialog
      v-model="confirmVisible"
      title="确认抽样单数据"
      width="min(900px, 92vw)"
      :close-on-click-modal="false"
    >
      <template v-if="preview">
        <el-alert
          v-if="preview.duplicated"
          class="dup-alert"
          type="error"
          :closable="false"
          show-icon
        >
          <template #title>
            文件标识「{{ preview.marker }}」此前已导入过，禁止重复导入。
          </template>
        </el-alert>
        <el-descriptions
          :column="2"
          border
          class="preview-meta"
        >
          <el-descriptions-item label="文件标识(A1)">
            {{ preview.marker }}
          </el-descriptions-item>
          <el-descriptions-item label="原始文件">
            {{ preview.originalName }}
          </el-descriptions-item>
          <el-descriptions-item
            label="解析数据行数"
            :span="2"
          >
            {{ preview.total }} 行（第 3 行起）
          </el-descriptions-item>
        </el-descriptions>

        <el-table
          :data="preview.rows"
          border
          stripe
          max-height="360"
          class="preview-table"
        >
          <el-table-column
            type="index"
            label="#"
            width="50"
            align="center"
          />
          <el-table-column
            v-for="(column, index) in preview.columns"
            :key="preview.columnKeys[index]"
            :prop="preview.columnKeys[index]"
            :label="column"
            min-width="130"
            show-overflow-tooltip
          />
        </el-table>
      </template>

      <template #footer>
        <el-button @click="confirmVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :disabled="!preview || preview.duplicated"
          :loading="importing"
          @click="handleConfirmImport"
        >
          确认无误，导入数据库
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'

import {
  confirmSampleSheetImport,
  downloadSampleSheetTemplate,
  previewSampleSheet,
  type SampleSheetPreview,
} from '@/api/sampleSheet'

const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const importing = ref(false)
const confirmVisible = ref(false)
const preview = ref<SampleSheetPreview | null>(null)

function handleFileChange(uploadFile: UploadFile) {
  if (!uploadFile.raw) {
    return
  }
  const file = uploadFile.raw
  if (!file.name.toLowerCase().endsWith('.xls')) {
    ElMessage.error('仅支持 .xls 格式的抽样单文件')
    selectedFile.value = null
    return
  }
  selectedFile.value = file
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择抽样单 .xls 文件')
    return
  }

  uploading.value = true
  try {
    preview.value = await previewSampleSheet(selectedFile.value)
    confirmVisible.value = true
    if (preview.value.duplicated) {
      ElMessage.warning('该抽样单文件标识已导入过，请勿重复导入')
    }
  } finally {
    uploading.value = false
  }
}

async function handleConfirmImport() {
  if (!preview.value) {
    return
  }

  importing.value = true
  try {
    const result = await confirmSampleSheetImport(preview.value.token)
    ElMessage.success(`抽样单「${result.marker}」导入成功，共导入 ${result.imported} 条任务`)
    confirmVisible.value = false
    preview.value = null
    selectedFile.value = null
  } finally {
    importing.value = false
  }
}

async function handleDownloadTemplate() {
  try {
    await downloadSampleSheetTemplate()
  } catch {
    ElMessage.error('模板下载失败，请稍后重试')
  }
}
</script>

<style scoped>
.import-sample-sheet {
  max-width: 1100px;
  margin: 0 auto;
  padding: 10px 20px 40px;
}

.page-title {
  margin: 10px 0 18px;
  color: #303133;
  font-size: 26px;
  font-weight: 700;
  text-align: center;
}

.steps {
  margin: 0 0 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
}

.sheet-box {
  margin-top: 6px;
  padding: 34px 40px 28px;
  border: 1px solid #9aa4ad;
}

.choose-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.choose-label {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.file-name {
  color: #303133;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-name-empty {
  color: #909399;
}

.upload-action {
  display: flex;
  justify-content: flex-end;
  margin: 34px 12px 30px 0;
}

.notes {
  display: flex;
  align-items: flex-start;
  color: #c45656;
  font-size: 15px;
  line-height: 1.9;
}

.notes-title {
  flex: none;
  font-weight: 600;
  white-space: nowrap;
}

.notes-body {
  flex: 1;
  min-width: 0;
}

.template-row {
  margin: 14px 0 0;
  text-align: center;
  font-size: 18px;
}

.dup-alert {
  margin-bottom: 14px;
}

.preview-meta {
  margin-bottom: 14px;
}

.preview-table {
  width: 100%;
}
</style>
