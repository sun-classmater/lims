<template>
  <HomeView v-if="route.meta.layout === 'main'">
    <router-view :key="$route.fullPath" />
  </HomeView>
  <router-view
    v-else
    :key="$route.fullPath"
  />
  <!-- 全站共用详情窗口：编号列见编号弹窗，环节信息列见登记/分解/安排/检验弹窗 -->
  <ProjectAuthInfoDialog />
  <TaskInfoDialog />
  <ProjectRegisterInfoDialog />
  <ProjectDetailDialog />
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

import ProjectAuthInfoDialog from '@/components/ProjectAuthInfoDialog.vue'
import ProjectDetailDialog from '@/components/ProjectDetailDialog.vue'
import ProjectRegisterInfoDialog from '@/components/ProjectRegisterInfoDialog.vue'
import TaskInfoDialog from '@/components/TaskInfoDialog.vue'
import HomeView from '@/views/HomeView.vue'

const route = useRoute()
</script>

<style>
/* 所有 el-table 的表格主体在表格内部滚动：
   表头固定、水平滚动条固定在表格底部可视区，
   内容向下滚动时滚动条位置不动、始终可见 */
.el-table .el-table__body-wrapper {
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  overflow-x: hidden;
  /* 垂直滚动条（上下滚动轮）默认透明隐藏，鼠标 hover 表格时才显示 */
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
}

.el-table .el-table__body-wrapper:hover {
  scrollbar-color: #c0c4cc #f5f7fa;
}

.el-table .el-table__body-wrapper::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.el-table .el-table__body-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

.el-table .el-table__body-wrapper::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 4px;
}

.el-table .el-table__body-wrapper:hover::-webkit-scrollbar-thumb {
  background: #c0c4cc;
}

.el-table .el-table__body-wrapper:hover::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

/* 水平滚动条始终可见、绝不允许隐藏：
   - 覆盖 Element Plus 默认仅 hover 显示的行为
   - 覆盖 JS 内联隐藏（display:none）与淡出动画 */
.el-table .el-scrollbar__bar.is-horizontal {
  z-index: 20;
  display: block !important;
  opacity: 1 !important;
  visibility: visible !important;
  transition: none !important;
  height: 10px;
  background: #f5f7fa;
}

.el-table:hover .el-scrollbar__bar.is-horizontal,
.el-table .el-scrollbar__bar.is-horizontal:hover {
  display: block !important;
  opacity: 1 !important;
  visibility: visible !important;
}

.el-table .el-scrollbar__bar.is-horizontal .el-scrollbar__thumb {
  display: block !important;
  opacity: 1 !important;
  visibility: visible !important;
  min-width: 24px;
  background: #c0c4cc;
  border-radius: 5px;
}

.el-table .el-scrollbar__bar.is-horizontal .el-scrollbar__thumb:hover {
  background: #909399;
}
</style>
