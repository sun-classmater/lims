import { createRouter, createWebHistory } from 'vue-router'

import DataPage from '@/views/DataPage.vue'
import ForbiddenView from '@/views/ForbiddenView.vue'
import LoginView from '@/views/LoginView.vue'
import NotFoundView from '@/views/NotFoundView.vue'
import ProfileView from '@/views/ProfileView.vue'
import { useAuthStore } from '@/stores/auth'
import type { PageConfig } from '@/types/page'

function dataRoute(path: string, name: string, config: PageConfig) {
  return {
    path,
    name,
    component: DataPage,
    meta: {
      title: config.title,
      config,
      layout: 'main',
    },
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/service/files',
    },
    {
      path: '/login',
      name: 'Login',
      component: LoginView,
      meta: {
        public: true,
      },
    },
    {
      path: '/base/profile',
      name: 'BaseProfile',
      component: ProfileView,
      meta: {
        title: '个人资料修改',
        layout: 'main',
      },
    },
    {
      path: '/service/files',
      name: 'ServiceFiles',
      component: () => import('@/views/service/FileLibrary.vue'),
      meta: {
        title: '中心文件资料',
        layout: 'main',
      },
    },
    dataRoute('/service/project-library', 'ServiceProjectLibrary', {
      entity: 'product',
      title: '产品检验项目库查询',
      primaryKeys: ['id'],
      showIndex: true,
      showFilters: false,
      canExport: false,
      columns: [
        { prop: 'id', label: '检验项目库编号', minWidth: 200, align: 'center', detail: true },
        { prop: 'prd_category', label: '产品分类', width: 180, align: 'center' },
        { prop: 'libName', label: '使用检验项目库', minWidth: 240, align: 'center' },
      ],
    }),
    {
      path: '/service/current',
      name: 'ServiceCurrentProjects',
      component: () => import('@/views/service/ActiveProjectQuery.vue'),
      meta: {
        title: '在检项目查询',
        layout: 'main',
      },
    },
    {
      path: '/service/history',
      name: 'ServiceHistoryProjects',
      component: () => import('@/views/service/HistoryProjectQuery.vue'),
      meta: {
        title: '历史项目查询',
        layout: 'main',
      },
    },
    dataRoute('/task/import', 'TaskImport', {
      entity: 'sjtask',
      title: '导入采样单',
      primaryKeys: ['id'],
      columns: [
        { prop: 'id', label: 'ID', width: 80 },
        { prop: 'taskno', label: '任务号', minWidth: 160 },
        { prop: 'leader', label: '负责人', width: 120 },
        { prop: 'source', label: '来源', minWidth: 160 },
        { prop: 'region', label: '区域', width: 130 },
        { prop: 'taskname', label: '任务名称', minWidth: 180 },
        { prop: 'taskxz', label: '任务性质', width: 120 },
        { prop: 'state', label: '状态', width: 100 },
      ],
      searchFields: [
        { prop: 'taskno', label: '任务号' },
        { prop: 'leader', label: '负责人' },
        { prop: 'taskxz', label: '任务性质' },
      ],
      formFields: [
        { prop: 'taskno', label: '任务号', required: true },
        { prop: 'leader', label: '负责人' },
        { prop: 'source', label: '来源' },
        { prop: 'region', label: '区域' },
        { prop: 'taskname', label: '任务名称' },
        { prop: 'taskxz', label: '任务性质' },
        { prop: 'date', label: '日期' },
        { prop: 'state', label: '状态' },
      ],
      canCreate: true,
      canEdit: true,
      canDelete: true,
    }),
    dataRoute('/task/register', 'TaskRegister', {
      entity: 'project',
      title: '样品登记确认',
      primaryKeys: ['id_nj'],
      columns: [
        { prop: 'bh', label: '项目编号', minWidth: 150 },
        { prop: 'customerId', label: '客户ID', width: 100 },
        { prop: 'productName', label: '产品名称', minWidth: 150 },
        { prop: 'date_reg', label: '登记日期', width: 130 },
        { prop: 'state_register', label: '登记状态', width: 100 },
        { prop: 'taskno', label: '任务号', minWidth: 140 },
      ],
      searchFields: [
        { prop: 'bh', label: '项目编号' },
        { prop: 'productName', label: '产品名称' },
      ],
      defaultFilters: {
        state_register: 'F',
      },
    }),
    dataRoute('/task/export', 'TaskExport', {
      entity: 'sjtask',
      title: '导出数据',
      primaryKeys: ['id'],
      columns: [
        { prop: 'taskno', label: '任务号', minWidth: 160 },
        { prop: 'leader', label: '负责人', width: 120 },
        { prop: 'source', label: '来源', minWidth: 160 },
        { prop: 'region', label: '区域', width: 130 },
        { prop: 'taskname', label: '任务名称', minWidth: 180 },
        { prop: 'taskxz', label: '任务性质', width: 120 },
        { prop: 'state', label: '状态', width: 100 },
      ],
    }),
    {
      path: '/sample/single',
      name: 'SampleSingle',
      component: () => import('@/views/dispatch/SingleDecompose.vue'),
      meta: {
        title: '单个样品分解',
        layout: 'main',
      },
    },
    {
      path: '/sample/batch',
      name: 'SampleBatch',
      component: () => import('@/views/dispatch/BatchDecompose.vue'),
      meta: {
        title: '批量样品分解',
        layout: 'main',
      },
    },
    dataRoute('/dispatch/query', 'DispatchQuery', {
      entity: 'task',
      title: '任务查询',
      primaryKeys: ['id'],
      columns: [
        { prop: 'id', label: 'ID', width: 80 },
        { prop: 'userId_jyy', label: '检验员', width: 120 },
        { prop: 'project_bh', label: '项目编号', minWidth: 160 },
        { prop: 'date_jyy', label: '检验日期', width: 130 },
        { prop: 'state_jyy', label: '状态', width: 100 },
      ],
      searchFields: [
        { prop: 'project_bh', label: '项目编号' },
        { prop: 'userId_jyy', label: '检验员' },
      ],
    }),
    dataRoute('/dispatch/input', 'DispatchInput', {
      entity: 'task',
      title: '数据录入',
      primaryKeys: ['id'],
      columns: [
        { prop: 'id', label: 'ID', width: 80 },
        { prop: 'userId_jyy', label: '检验员', width: 120 },
        { prop: 'project_bh', label: '项目编号', minWidth: 160 },
        { prop: 'date_jyy', label: '检验日期', width: 130 },
        { prop: 'state_jyy', label: '状态', width: 100 },
      ],
      formFields: [
        { prop: 'userId_jyy', label: '检验员', required: true },
        { prop: 'project_bh', label: '项目编号', required: true },
        { prop: 'date_jyy', label: '检验日期' },
        { prop: 'state_jyy', label: '状态' },
      ],
      canCreate: true,
      canEdit: true,
      canDelete: true,
    }),
    dataRoute('/base/user', 'BaseUser', {
      entity: 'user',
      title: '用户信息',
      primaryKeys: ['gh'],
      columns: [
        { prop: 'gh', label: '工号', width: 110 },
        { prop: 'name', label: '姓名', width: 120 },
        { prop: 'userRoleId', label: '角色编号', width: 110 },
        { prop: 'deptId', label: '部门编号', width: 110 },
        { prop: 'mobile', label: '手机', width: 130 },
        { prop: 'email', label: '邮箱', minWidth: 160 },
        { prop: 'position', label: '岗位', width: 130 },
        { prop: 'nature', label: '人员性质', width: 120 },
      ],
      searchFields: [
        { prop: 'gh', label: '工号' },
        { prop: 'name', label: '姓名' },
        { prop: 'position', label: '岗位' },
      ],
      formFields: [
        { prop: 'gh', label: '工号', required: true, disabled: true },
        { prop: 'name', label: '姓名', required: true },
        { prop: 'password', label: '密码', required: true },
        { prop: 'userRoleId', label: '角色编号' },
        { prop: 'deptId', label: '部门编号' },
        { prop: 'mobile', label: '手机' },
        { prop: 'email', label: '邮箱' },
        { prop: 'position', label: '岗位' },
        { prop: 'nature', label: '人员性质' },
      ],
      canCreate: true,
      canEdit: true,
      canDelete: true,
    }),
    dataRoute('/base/method', 'BaseMethod', {
      entity: 'user_method',
      title: '方法检验员',
      primaryKeys: ['id'],
      columns: [
        { prop: 'id', label: 'ID', width: 80 },
        { prop: 'usergh', label: '工号', width: 120 },
        { prop: 'method', label: '检验方法', minWidth: 220 },
        { prop: 'method_note', label: '方法备注', minWidth: 160 },
        { prop: 'memo', label: '备注', minWidth: 160 },
      ],
      searchFields: [
        { prop: 'usergh', label: '工号' },
        { prop: 'method', label: '检验方法' },
      ],
      formFields: [
        { prop: 'usergh', label: '工号', required: true },
        { prop: 'method', label: '检验方法', required: true },
        { prop: 'method_note', label: '方法备注' },
        { prop: 'memo', label: '备注' },
      ],
      canCreate: true,
      canEdit: true,
      canDelete: true,
    }),
    dataRoute('/base/position', 'BasePosition', {
      entity: 'user',
      title: '检验岗位',
      primaryKeys: ['gh'],
      columns: [
        { prop: 'gh', label: '工号', width: 110 },
        { prop: 'name', label: '姓名', width: 120 },
        { prop: 'deptId', label: '部门编号', width: 110 },
        { prop: 'position', label: '岗位', width: 140 },
        { prop: 'nature', label: '人员性质', width: 120 },
        { prop: 'mobile', label: '手机', width: 130 },
      ],
      searchFields: [
        { prop: 'position', label: '岗位' },
        { prop: 'deptId', label: '部门编号' },
      ],
      formFields: [
        { prop: 'gh', label: '工号', required: true, disabled: true },
        { prop: 'position', label: '岗位' },
        { prop: 'nature', label: '人员性质' },
        { prop: 'deptId', label: '部门编号' },
      ],
      canEdit: true,
    }),
    dataRoute('/base/project', 'BaseProject', {
      entity: 'product',
      title: '项目库维护',
      primaryKeys: ['id'],
      showIndex: true,
      showFilters: false,
      canExport: false,
      columns: [
        { prop: 'id', label: '检验项目库编号', minWidth: 200, align: 'center', detail: true },
        { prop: 'prd_category', label: '产品分类', width: 180, align: 'center' },
        { prop: 'libName', label: '使用检验项目库', minWidth: 240, align: 'center' },
      ],
      formFields: [
        { prop: 'id', label: '检验项目库编号', required: true },
        {
          prop: 'prd_category',
          label: '产品分类',
          type: 'select',
          required: true,
          options: [
            { label: '谷物', value: '谷物' },
            { label: '蔬菜', value: '蔬菜' },
            { label: '水果', value: '水果' },
          ],
        },
        { prop: 'libName', label: '使用检验项目库', required: true },
      ],
      canCreate: true,
      createLink: '/sample/import',
      createButtonText: '导入新的项目库',
      createButtonType: 'warning',
      createDialogTitle: '导入新的项目库',
      canDelete: true,
      actionColumnLabel: '删除操作',
      deleteButtonText: '删除-慎重!',
      deleteConfirmText: '删除后不可恢复，确定要慎重删除该检验项目库吗？',
    }),
    dataRoute('/base/upload', 'BaseUpload', {
      entity: 'contract',
      title: '中心资料上传',
      primaryKeys: ['id'],
      columns: [
        { prop: 'id', label: 'ID', width: 80 },
        { prop: 'htbh', label: '合同编号', minWidth: 150 },
        { prop: 'customerId', label: '客户ID', width: 100 },
        { prop: 'content', label: '合同内容', minWidth: 220 },
        { prop: 'date', label: '日期', width: 120 },
      ],
      canUpload: true,
    }),
    dataRoute('/standards/name', 'StandardName', {
      entity: 'basisname',
      title: '标准库名维护',
      primaryKeys: ['id'],
      columns: [
        { prop: 'id', label: 'ID', width: 80 },
        { prop: 'basis', label: '依据编号', minWidth: 160 },
        { prop: 'basisName', label: '依据名称', minWidth: 260 },
        { prop: 'note', label: '备注', minWidth: 160 },
      ],
      searchFields: [
        { prop: 'basis', label: '依据编号' },
        { prop: 'basisName', label: '依据名称' },
      ],
      formFields: [
        { prop: 'basis', label: '依据编号', required: true },
        { prop: 'basisName', label: '依据名称', required: true },
        { prop: 'note', label: '备注' },
      ],
      canCreate: true,
      canEdit: true,
      canDelete: true,
    }),
    {
      path: '/task/supervision-manage',
      name: 'TaskSupervisionManage',
      component: () => import('@/views/task/MonitorTaskManage.vue'),
      meta: {
        title: '监抽任务管理',
        layout: 'main',
      },
    },
    {
      path: '/task/supervision-execute',
      name: 'TaskSupervisionExecute',
      component: () => import('@/views/task/SupervisionExecuteQuery.vue'),
      meta: {
        title: '监抽执行查询',
        layout: 'main',
      },
    },
    {
      path: '/task/supervision-sample',
      name: 'TaskSupervisionSample',
      component: () => import('@/views/task/SupervisionSampleQuery.vue'),
      meta: {
        title: '监抽抽样查询',
        layout: 'main',
      },
    },
    {
      path: '/sample/import',
      name: 'SampleImport',
      component: () => import('@/views/sample/ImportSampleSheet.vue'),
      meta: {
        title: '导入采样单',
        layout: 'main',
      },
    },
    {
      path: '/sample/register',
      name: 'SampleRegister',
      component: () => import('@/views/sample/SampleRegisterConfirm.vue'),
      meta: {
        title: '样品登记确认',
        layout: 'main',
      },
    },
    {
      path: '/sample/report-download',
      name: 'SampleReportDownload',
      component: () => import('@/views/sample/ReportDownload.vue'),
      meta: {
        title: '报告下载',
        layout: 'main',
      },
    },
    {
      path: '/sample/register-query',
      name: 'SampleRegisterQuery',
      component: () => import('@/views/sample/SampleRegisterQuery.vue'),
      meta: {
        title: '登记查询',
        layout: 'main',
      },
    },
    {
      path: '/sample/export',
      name: 'SampleExport',
      component: () => import('@/views/sample/ExportData.vue'),
      meta: {
        title: '导出数据',
        layout: 'main',
      },
    },
    {
      path: '/dispatch/single',
      name: 'DispatchSingle',
      component: () => import('@/views/dispatch/SingleDecompose.vue'),
      meta: {
        title: '单个样品分解',
        layout: 'main',
      },
    },
    {
      path: '/dispatch/batch',
      name: 'DispatchBatch',
      component: () => import('@/views/dispatch/BatchDecompose.vue'),
      meta: {
        title: '批量样品分解',
        layout: 'main',
      },
    },
    {
      path: '/dispatch/arrange',
      name: 'DispatchArrange',
      component: () => import('@/views/dispatch/SingleArrange.vue'),
      meta: {
        title: '任务安排',
        layout: 'main',
      },
    },
    {
      path: '/dispatch/batch-arrange',
      name: 'DispatchBatchArrange',
      component: () => import('@/views/dispatch/BatchArrange.vue'),
      meta: {
        title: '批量安排',
        layout: 'main',
      },
    },
    {
      path: '/inspection/query',
      name: 'InspectionQuery',
      component: () => import('@/views/inspection/QueryInspection.vue'),
      meta: {
        title: '任务查询',
        layout: 'main',
      },
    },
    {
      path: '/inspection/input',
      name: 'InspectionInputNew',
      component: () => import('@/views/inspection/InputInspection.vue'),
      meta: {
        title: '数据录入',
        layout: 'main',
      },
    },
    {
      path: '/review/issue',
      name: 'ReviewIssue',
      component: () => import('@/views/review/ProjectIssue.vue'),
      meta: {
        title: '报告签发',
        layout: 'main',
      },
    },
    {
      path: '/review/query',
      name: 'ReviewQueryNew',
      component: () => import('@/views/review/InspectSummary.vue'),
      meta: {
        title: '检验汇总查询',
        layout: 'main',
      },
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: ForbiddenView,
      meta: {
        public: true,
      },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: NotFoundView,
      meta: {
        public: true,
      },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    if (to.name === 'Login' && authStore.isLoggedIn) {
      return { path: '/service/files' }
    }
    return true
  }

  if (!authStore.isLoggedIn) {
    return {
      name: 'Login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (!authStore.user) {
    try {
      await authStore.fetchMe()
    } catch {
      authStore.logout()
      return {
        name: 'Login',
        query: {
          redirect: to.fullPath,
        },
      }
    }
  }

  return true
})

export default router
