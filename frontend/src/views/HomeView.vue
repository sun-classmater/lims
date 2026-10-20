<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <el-icon class="brand-icon">
          <Platform />
        </el-icon>
        <span>LIMS</span>
      </div>

      <el-menu
        class="sidebar-menu"
        :default-active="activeMenu"
        :default-openeds="['service', 'task', 'sample', 'dispatch', 'inspection', 'review', 'base', 'standards']"
      >
        <el-sub-menu
          v-for="group in menuGroups"
          :key="group.index"
          :index="group.index"
        >
          <template #title>
            <el-icon>
              <component :is="group.icon" />
            </el-icon>
            <span>{{ group.title }}</span>
          </template>
          <el-menu-item
            v-for="child in group.children"
            :key="child.index"
            :index="child.index"
            @click="selectMenu(child.index)"
          >
            {{ child.title }}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div class="breadcrumb">
          <span>当前位置</span>
          <el-divider direction="vertical" />
          <span class="breadcrumb-current">{{ currentTitle }}</span>
        </div>

        <el-dropdown @command="handleUserCommand">
          <button class="user-trigger">
            <el-avatar
              :size="28"
              class="user-avatar"
            >
              {{ initial }}
            </el-avatar>
            <span>{{ displayName }}</span>
            <el-icon>
              <ArrowDown />
            </el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <el-icon>
                  <SwitchButton />
                </el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="content">
        <slot />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Collection,
  DataLine,
  DocumentChecked,
  Grid,
  HomeFilled,
  Platform,
  Stamp,
  SwitchButton,
  Tickets,
  Box,
} from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

interface MenuChild {
  index: string
  title: string
}

interface MenuGroup {
  index: string
  title: string
  icon: Component
  children: MenuChild[]
}

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => String(route.meta.title || '服务信息'))

const menuGroups: MenuGroup[] = [
  {
    index: 'service',
    title: '服务信息',
    icon: HomeFilled,
    children: [
      { index: '/service/files', title: '中心文件资料' },
      { index: '/service/project-library', title: '项目库查询' },
      { index: '/service/current', title: '在检项目查询' },
      { index: '/service/history', title: '历史项目查询' },
    ],
  },
  {
    index: 'task',
    title: '任务管理',
    icon: Tickets,
    children: [
      { index: '/task/supervision-manage', title: '监抽任务管理' },
      { index: '/task/supervision-execute', title: '监抽执行查询' },
      { index: '/task/supervision-sample', title: '监抽抽样查询' },
    ],
  },
  {
    index: 'sample',
    title: '样品管理',
    icon: Box,
    children: [
      { index: '/sample/import', title: '导入采样单' },
      { index: '/sample/register', title: '样品登记确认' },
      { index: '/sample/report-download', title: '报告下载' },
      { index: '/sample/register-query', title: '登记查询' },
      { index: '/sample/export', title: '导出数据' },
    ],
  },
  {
    index: 'dispatch',
    title: '分解排样',
    icon: Grid,
    children: [
      { index: '/dispatch/single', title: '单个样品分解' },
      { index: '/dispatch/batch', title: '批量样品分解' },
      { index: '/dispatch/arrange', title: '任务安排' },
      { index: '/dispatch/batch-arrange', title: '批量安排' },
    ],
  },
  {
    index: 'inspection',
    title: '项目检验',
    icon: DocumentChecked,
    children: [
      { index: '/inspection/query', title: '任务查询' },
      { index: '/inspection/input', title: '数据录入' },
    ],
  },
  {
    index: 'review',
    title: '审核签发',
    icon: Stamp,
    children: [
      { index: '/review/issue', title: '报告签发' },
      { index: '/review/query', title: '检验汇总查询' },
    ],
  },
  {
    index: 'base',
    title: '基础数据',
    icon: Collection,
    children: [
      { index: '/base/user', title: '用户信息' },
      { index: '/base/method', title: '方法检验员' },
      { index: '/base/position', title: '检验岗位' },
      { index: '/base/project', title: '项目库维护' },
      { index: '/base/upload', title: '中心资料上传' },
      { index: '/base/profile', title: '个人资料修改' },
    ],
  },
  {
    index: 'standards',
    title: '系统标准库名',
    icon: DataLine,
    children: [
      { index: '/standards/name', title: '标准库名维护' },
    ],
  },
]

const displayName = computed(() => authStore.user?.name || authStore.user?.gh || '')
const initial = computed(() => displayName.value.slice(0, 1))

function selectMenu(index: string) {
  if (index.startsWith('/')) {
    router.push(index)
  }
}

function handleUserCommand(command: string) {
  if (command === 'logout') {
    authStore.logout()
    router.replace('/login')
  }
}

</script>

<style scoped>
.shell {
  height: 100vh;
  display: grid;
  grid-template-columns: 240px 1fr;
  background: #f4f6f8;
  color: #1f2937;
  overflow: hidden;
}

.sidebar {
  height: 100vh;
  padding: 14px 10px;
  box-sizing: border-box;
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  overflow-y: auto;
}

.brand {
  height: 54px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
}

.brand-icon {
  color: #2563eb;
  font-size: 22px;
}

.sidebar-menu {
  border-right: 0;
  margin-top: 8px;
  background: transparent;
  color: #1f2937;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 42px;
  margin: 2px 0;
  border-radius: 7px;
  line-height: 42px;
  color: #1f2937;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: #f3f4f6;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #eef4ff;
  color: #1d4ed8;
}

.workspace {
  height: 100vh;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
}

.breadcrumb-current {
  color: #111827;
  font-weight: 600;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 7px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #374151;
  cursor: pointer;
}

.user-trigger:hover {
  background: #f3f4f6;
}

.user-avatar {
  background: #2563eb;
  color: #ffffff;
  font-size: 13px;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
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

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-input {
  width: 280px;
}

.filter-select {
  width: 150px;
}

.table-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

@media (max-width: 760px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }

  .content {
    padding: 16px;
  }

  .filters {
    flex-wrap: wrap;
  }

  .filter-input {
    width: 100%;
  }
}
</style>
