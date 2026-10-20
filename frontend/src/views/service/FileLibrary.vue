<template>
  <div class="file-library">
    <h2 class="page-title">
      文件资料
    </h2>

    <div
      v-loading="loading"
      class="file-table"
    >
      <div
        v-for="item in files"
        :key="item.type"
        class="file-row"
      >
        <span class="file-name">{{ item.name }}</span>
        <el-button
          size="small"
          :loading="downloading === item.type"
          @click="handleDownload(item)"
        >
          下载
        </el-button>
      </div>

      <div
        v-if="!loading && files.length === 0"
        class="file-row file-empty"
      >
        暂无可下载文件
      </div>

      <div class="file-row file-tip">
        下载时请选择浏览器“极速模式”
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import {
  downloadFileLibrary,
  fetchFileLibrary,
  type FileLibraryItem,
} from '@/api/fileLibrary'

const loading = ref(false)
const files = ref<FileLibraryItem[]>([])
const downloading = ref('')

async function loadFiles() {
  loading.value = true
  try {
    files.value = await fetchFileLibrary()
  } finally {
    loading.value = false
  }
}

async function handleDownload(item: FileLibraryItem) {
  downloading.value = item.type
  try {
    await downloadFileLibrary(item.type, item.name)
    ElMessage.success(`已下载：${item.name}`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败，请稍后重试')
  } finally {
    downloading.value = ''
  }
}

onMounted(loadFiles)
</script>

<style scoped>
.file-library {
  min-width: 0;
}

.page-title {
  margin: 4px 0 18px;
  color: #303133;
  font-size: 18px;
  font-weight: 700;
  text-align: center;
}

.file-table {
  max-width: 920px;
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
  color: #303133;
  font-size: 14px;
  word-break: break-all;
}

.file-empty {
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.file-tip {
  justify-content: center;
  color: #c45656;
  font-size: 14px;
}
</style>
