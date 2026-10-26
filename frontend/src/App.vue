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
  <AiAssistant v-if="!route.meta.public" />
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

import AiAssistant from '@/components/AiAssistant.vue'
import ProjectAuthInfoDialog from '@/components/ProjectAuthInfoDialog.vue'
import ProjectDetailDialog from '@/components/ProjectDetailDialog.vue'
import ProjectRegisterInfoDialog from '@/components/ProjectRegisterInfoDialog.vue'
import TaskInfoDialog from '@/components/TaskInfoDialog.vue'
import HomeView from '@/views/HomeView.vue'

const route = useRoute()
</script>

<style>
/* ===== 全局主题 ===== */
:root {
  --lims-primary: #2f6fb3;
  --lims-primary-dark: #265a93;
  --lims-bg: #e4eaf2;
  --lims-content-bg: #ebf0f6;
  --lims-surface: #ffffff;
  --lims-line: #d3dde9;
  --lims-line-strong: #b9c7d8;
  --lims-table-line: #c8d4e2;
  --lims-text: #1f2d3d;
  --lims-muted: #6b7c93;
  --lims-radius: 12px;
  --el-color-primary: var(--lims-primary);
  --el-color-primary-light-3: #5b8fc7;
  --el-color-primary-light-5: #87abd6;
  --el-color-primary-light-7: #b3c8e4;
  --el-color-primary-light-8: #c9d7ec;
  --el-color-primary-light-9: #eaf1f9;
  --el-color-primary-dark-2: var(--lims-primary-dark);
  --el-border-color: var(--lims-line-strong);
  --el-border-color-light: var(--lims-line);
  --el-border-color-lighter: #dfe6ef;
  --el-border-color-extra-light: #e7edf4;
  --el-border-radius-base: 8px;
}

html,
body {
  margin: 0;
}

body {
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  color: var(--lims-text);
  background: var(--lims-bg);
  -webkit-font-smoothing: antialiased;
}

/* 卡片 */
.el-card {
  border: 1px solid var(--lims-line-strong);
  border-radius: var(--lims-radius);
  background: var(--lims-surface);
  box-shadow:
    0 1px 2px rgba(23, 50, 77, 0.08),
    0 8px 22px rgba(23, 50, 77, 0.06);
}

.content .el-card.el-card {
  border-color: var(--lims-line-strong);
}

.el-card__header {
  border-bottom: 1px solid var(--lims-line);
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

/* 表格 */
.el-table {
  --el-table-border-color: var(--lims-table-line);
  --el-table-bg-color: var(--lims-surface);
  --el-table-tr-bg-color: var(--lims-surface);
  --el-table-header-bg-color: #eaf0f7;
  --el-table-header-text-color: #263e57;
  --el-table-text-color: #26394d;
  --el-table-row-hover-bg-color: #eaf4ff;
  --el-table-current-row-bg-color: #e2effc;
  border: 1px solid var(--lims-table-line);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(23, 50, 77, 0.08);
}

.el-table th.el-table__cell {
  background: #eaf0f7;
  color: var(--el-table-header-text-color);
  font-weight: 700;
  border-bottom: 1px solid var(--lims-line-strong);
}

.el-table .el-table__cell {
  padding: 9px 0;
  border-right: 1px solid var(--lims-table-line);
}

.el-table .el-table__cell:last-child {
  border-right: 0;
}

.el-table .el-table__row:nth-child(even) > .el-table__cell {
  background: #fafcff;
}

.el-table--border {
  border-color: var(--lims-line-strong);
}

.el-table--border::after,
.el-table--group::after,
.el-table::before {
  background-color: var(--lims-line-strong);
}

/* 表单控件 */
.el-button {
  border-radius: 8px;
  font-weight: 500;
}

.el-input__wrapper,
.el-select__wrapper,
.el-textarea__inner {
  border-radius: 8px;
}

.el-tag {
  border-radius: 6px;
}

.el-dialog {
  overflow: hidden;
  border: 1px solid var(--lims-line-strong);
  border-radius: 14px;
  box-shadow: 0 20px 55px rgba(15, 35, 55, 0.2);
}

.el-dialog__header {
  margin-right: 0;
  padding: 18px 22px 10px;
  border-bottom: 1px solid var(--lims-line);
}

.el-dialog__body {
  padding: 18px 22px;
}

.el-dialog__footer {
  padding: 10px 22px 18px;
  border-top: 1px solid var(--lims-line);
}

.el-divider {
  --el-border-color: var(--lims-line-strong);
}

.el-divider--horizontal {
  margin: 18px 0;
}

/* 页面小节标题 */
.content .section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.content .section-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, #4f95cf, #2f6fb3);
}

/* 页面切换动画 */
.content > * {
  animation: lims-fade-in 0.28s ease both;
}

@keyframes lims-fade-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }

  to {
    opacity: 1;
    transform: none;
  }
}

/* 滚动条 */
*::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

*::-webkit-scrollbar-thumb {
  background: #c6d2e0;
  border-radius: 6px;
}

*::-webkit-scrollbar-thumb:hover {
  background: #a9b9cc;
}

*::-webkit-scrollbar-track {
  background: transparent;
}

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
