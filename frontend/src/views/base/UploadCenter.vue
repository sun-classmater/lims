<template>
  <div class="upload-center">
    <h2 class="page-title">
      中心文件资料上传管理
    </h2>

    <div
      v-loading="loading"
      class="file-table"
    >
      <div
        v-for="item in files"
        :key="item.name"
        class="file-row"
      >
        <span class="file-name">{{ item.originalName }}</span>
        <span class="file-actions">
          <el-button
            size="small"
            :loading="downloading === item.name"
            @click="handleDownload(item)"
          >
            下载
          </el-button>
          <el-button
            size="small"
            plain
            type="danger"
            :loading="deleting === item.name"
            @click="handleDelete(item)"
          >
            删除
          </el-button>
        </span>
      </div>

      <div
        v-if="!loading && files.length === 0"
        class="file-row file-empty"
      >
        暂无文件
      </div>

      <div class="file-row file-upload">
        <button
          type="button"
          class="upload-link"
          @click="fileInput?.click()"
        >
          上传文件到服务器
        </button>
        <input
          ref="fileInput"
          class="hidden-input"
          type="file"
          @change="handleUpload"
        >
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  deleteStoredFile,
  downloadStoredFile,
  fetchStoredFiles,
  uploadStoredFile,
  type StoredFile,
} from '@/api/fileManage'

const loading = ref(false)
const files = ref<StoredFile[]>([])
const downloading = ref('')
const deleting = ref('')
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()

async function loadFiles() {
  loading.value = true
  try {
    files.value = await fetchStoredFiles()
  } finally {
    loading.value = false
  }
}

async function handleDownload(item: StoredFile) {
  downloading.value = item.name
  try {
    await downloadStoredFile(item.name, item.originalName)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败')
  } finally {
    downloading.value = ''
  }
}

async function handleDelete(item: StoredFile) {
  try {
    await ElMessageBox.confirm(
      `确认删除文件「${item.originalName}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  deleting.value = item.name
  try {
    await deleteStoredFile(item.name)
    ElMessage.success('文件已删除')
    await loadFiles()
  } finally {
    deleting.value = ''
  }
}

async function handleUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  if (uploading.value) {
    return
  }

  uploading.value = true
  try {
    await uploadStoredFile(file)
    ElMessage.success(`上传成功：${file.name}`)
    await loadFiles()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

onMounted(loadFiles)
</script>

<style scoped>
.upload-center {
  min-width: 0;
}

.page-title {
  margin: 6px 0 20px;
  color: #303133;
  font-size: 20px;
  font-weight: 700;
  text-align: center;
}

.file-table {
  max-width: 980px;
  margin: 0 auto;
  border: 1px solid #c8c9cc;
}

.file-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 42px;
  padding: 6px 14px;
  border-bottom: 1px solid #dcdfe6;
}

.file-row:last-child {
  border-bottom: none;
}

.file-name {
  flex: 1;
  color: #303133;
  font-size: 14px;
  word-break: break-all;
}

.file-actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.file-empty {
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.file-upload {
  justify-content: center;
}

.upload-link {
  padding: 0;
  color: #606266;
  font-size: 14px;
  background: none;
  border: none;
  cursor: pointer;
}

.upload-link:hover {
  color: #409eff;
  text-decoration: underline;
}

.hidden-input {
  display: none;
}
</style>
