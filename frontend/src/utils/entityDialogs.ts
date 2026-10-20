import { reactive } from 'vue'

import { fetchData } from '@/api/data'

/**
 * 列表页编号列的公共弹窗状态。
 *
 * 设计说明：
 * - 弹窗实例挂在 App.vue 上，全站只渲染一份，列表单元格只负责触发打开；
 * - 传入所在行时，若该行本身就是完整的 project / sjtask 记录则直接复用，
 *   否则（列表接口只返回了部分字段）按编号回查一次，保证弹窗字段完整；
 * - 客户、人员等基础数据按需加载并缓存，避免每次打开窗口都重复请求。
 */
export type EntityDialogRow = Record<string, unknown>

export type EntityLinkType = 'project' | 'task'

export type ProjectDetailType = 'div' | 'arrange' | 'inspect'

interface ProjectAuthState {
  visible: boolean
  loading: boolean
  bh: string
  error: string
  row: EntityDialogRow | null
}

interface TaskInfoState {
  visible: boolean
  loading: boolean
  taskno: string
  error: string
  row: EntityDialogRow | null
}

export const projectAuthState = reactive<ProjectAuthState>({
  visible: false,
  loading: false,
  bh: '',
  error: '',
  row: null,
})

export const taskInfoState = reactive<TaskInfoState>({
  visible: false,
  loading: false,
  taskno: '',
  error: '',
  row: null,
})

export function asText(value: unknown): string {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

function hasColumn(row: EntityDialogRow, column: string): boolean {
  return Object.prototype.hasOwnProperty.call(row, column)
}

/* ---------------- 基础数据缓存 ---------------- */

let customerCache: Record<string, EntityDialogRow> | null = null
let userNameCache: Record<string, string> | null = null

async function loadCustomers(): Promise<Record<string, EntityDialogRow>> {
  if (customerCache) {
    return customerCache
  }
  const map: Record<string, EntityDialogRow> = {}
  try {
    const result = await fetchData('customer', { page: 1, size: 200 })
    for (const item of result.records) {
      map[asText(item.id)] = item
    }
  } catch {
    // 客户表加载失败时联系人/地址/电话留空，其余字段照常展示
  }
  customerCache = map
  return map
}

async function loadUserNames(): Promise<Record<string, string>> {
  if (userNameCache) {
    return userNameCache
  }
  const map: Record<string, string> = {}
  try {
    const result = await fetchData('user', { page: 1, size: 200 })
    for (const item of result.records) {
      map[asText(item.gh)] = asText(item.name) || asText(item.gh)
    }
  } catch {
    // 用户表加载失败时回退显示工号
  }
  userNameCache = map
  return map
}

/**
 * 按样品编号（bh）回查 project 表，返回精确匹配的那一行。
 * 列表接口通常只返回部分字段，需要完整内容时以 project 表为准。
 */
export async function fetchProjectRow(
  bh: string,
): Promise<EntityDialogRow | null> {
  try {
    const result = await fetchData('project', { page: 1, size: 50, bh })
    return result.records.find((item) => asText(item.bh) === bh) ?? null
  } catch {
    return null
  }
}

/**
 * 通用表格（DataPage）中「编号列」到弹窗类型的映射。
 * 样品编号/项目编号 → 项目授权信息；任务编号 → 检验任务信息维护。
 */
export function entityLinkOfColumn(prop: string): EntityLinkType | null {
  if (prop === 'bh' || prop === 'project_bh') {
    return 'project'
  }
  if (prop === 'taskno' || prop === 'scdateflag') {
    return 'task'
  }
  return null
}

/** 打开「项目授权信息」窗口 */
export async function openProjectAuthDialog(
  bh: unknown,
  row?: EntityDialogRow | null,
) {
  const value = asText(bh)
  if (!value) {
    return
  }

  projectAuthState.bh = value
  projectAuthState.error = ''
  projectAuthState.row = row && hasColumn(row, 'remarks_register') ? row : null
  projectAuthState.visible = true

  if (projectAuthState.row) {
    return
  }

  projectAuthState.loading = true
  try {
    const result = await fetchData('project', { page: 1, size: 50, bh: value })
    const matched = result.records.find((item) => asText(item.bh) === value)
    projectAuthState.row = matched ?? null
    if (!matched) {
      projectAuthState.error = `未查询到编号为 ${value} 的项目记录`
    }
  } catch {
    projectAuthState.error = '项目信息加载失败，请稍后重试'
  } finally {
    projectAuthState.loading = false
  }
}

/** 打开「检验任务信息维护」窗口 */
export async function openTaskInfoDialog(
  taskno: unknown,
  row?: EntityDialogRow | null,
) {
  const value = asText(taskno)
  if (!value) {
    return
  }

  taskInfoState.taskno = value
  taskInfoState.error = ''
  taskInfoState.row = row && hasColumn(row, 'taskname') ? row : null
  taskInfoState.visible = true

  if (taskInfoState.row) {
    return
  }

  taskInfoState.loading = true
  try {
    const result = await fetchData('sjtask', { page: 1, size: 50, taskno: value })
    const matched = result.records.find((item) => asText(item.taskno) === value)
    taskInfoState.row = matched ?? null
    if (!matched) {
      taskInfoState.error = `任务表中未查询到任务编号 ${value} 的记录`
    }
  } catch {
    taskInfoState.error = '任务信息加载失败，请稍后重试'
  } finally {
    taskInfoState.loading = false
  }
}

/* ---------------- 项目登记信息 ---------------- */

export interface ProjectRegisterField {
  kind: 'text' | 'check'
  label: string
  value: string
  options?: string[]
  checked?: string[]
  other?: string
  showOther?: boolean
  span?: number
}

export interface ProjectRegisterSection {
  title: string
  fields: ProjectRegisterField[]
}

interface ProjectRegisterState {
  visible: boolean
  loading: boolean
  bh: string
  sections: ProjectRegisterSection[]
}

export const projectRegisterState = reactive<ProjectRegisterState>({
  visible: false,
  loading: false,
  bh: '',
  sections: [],
})

/** 旧库日期多存为 2024.9.1 形式，展示时统一成 2024-09-01 */
function formatLegacyDate(value: unknown): string {
  const raw = asText(value)
  const matched = /^(\d{4})[.\-/](\d{1,2})[.\-/](\d{1,2})$/.exec(raw)
  if (!matched) {
    return raw
  }
  const [, year, month, day] = matched
  return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`
}

function splitTokens(value: string): string[] {
  return value
    .split(/[;；,，、]/)
    .map((token) => token.trim())
    .filter((token) => token !== '')
}

function textField(label: string, value: unknown): ProjectRegisterField {
  return { kind: 'text', label, value: asText(value) }
}

function dateField(label: string, value: unknown): ProjectRegisterField {
  return { kind: 'text', label, value: formatLegacyDate(value) }
}

/**
 * 勾选型字段：按选项做匹配，匹配不上的文字落到「其他」输入框，
 * 保证库里存成自由文本的历史数据也不会被藏起来。
 */
function checkField(
  label: string,
  options: string[],
  value: unknown,
): ProjectRegisterField {
  const raw = asText(value)
  const tokens = splitTokens(raw)
  const checked = options.filter((option) => tokens.includes(option))
  const other = tokens.filter((token) => !options.includes(token)).join('；')
  return {
    kind: 'check',
    label,
    value: raw,
    options,
    checked,
    other,
    showOther: options.includes('其他') || other !== '',
    span: 24,
  }
}

/** 委托单位 / 生产单位 / 受检单位 四要素 */
function unitSection(
  title: string,
  customer: EntityDialogRow,
  fallbackName: unknown,
): ProjectRegisterSection {
  return {
    title,
    fields: [
      textField('名称', asText(customer.company) || asText(fallbackName)),
      textField('联系人', customer.lxr),
      textField('地址', customer.addr),
      textField('电话', asText(customer.lxrMobileNo) || asText(customer.leaderMobileNO)),
    ],
  }
}

function buildProjectRegisterSections(
  row: EntityDialogRow,
  customers: Record<string, EntityDialogRow>,
  userNames: Record<string, string>,
): ProjectRegisterSection[] {
  const client = customers[asText(row.customerId)] ?? {}
  const producer = customers[asText(row.customerId_scdw)] ?? {}
  const inspected = customers[asText(row.customerId_sjdw)] ?? {}
  const sampler = userNames[asText(row.userId_cy)] || asText(row.userId_cy)

  return [
    unitSection('委托单位', client, row.dwname_wt_nj),
    {
      title: '检验信息',
      fields: [
        checkField('检验目的', ['质量检验', '社会证明', '执法检查', '其他'], row.jymd),
        checkField('检验类别', ['委托送样', '委托抽检', '监督抽检', '仲裁检验', '其他'], row.jylb),
        checkField('检验类型', ['发证检验', '出厂检验', '型式检验', '其他'], row.jylx),
        dateField('送样日期', row.date_sy),
        dateField('预约完成日期', row.date_yywc),
        dateField('检验起始日期', row.date_jystart),
        textField('抽样基数', row.cyjs),
        textField('抽样人', sampler),
        dateField('抽样日期', row.date_cy),
        textField('抽样地点', row.cyaddr),
        textField('上传抽样单', asText(row.isUpfile) ? '已上传' : '未上传'),
      ],
    },
    unitSection('生产单位', producer, row.dwname_sc_nj),
    unitSection('受检单位', inspected, row.dwname_sj_nj),
    {
      title: '样品信息',
      fields: [
        textField('产品名称', row.productName),
        textField('规格型号', row.ggxh),
        textField('样品商标', row.ypsb),
        textField('样品等级', row.ypdj),
        textField('样品备注信息', row.ypbz),
        textField('生产日期', row.date_sc),
        textField('生产批号', row.scBatch),
        textField('样品数量', row.ypNum),
        textField('备样量', row.byNum),
        checkField('随样品所附资料', ['未附资料', '说明书', '标准', '执法文书', '其他'], row.otherFile),
        checkField('样品存储要求', ['常温', '冷冻', '冷藏', '特殊要求'], row.ypSaveReq),
        checkField('余样处理', ['自取', '承检方按规定处理', '邮寄'], row.yyProcess),
      ],
    },
    {
      title: '样品状态描述',
      fields: [
        checkField('是否符合检验要求', ['符合', '不符合'], row.isFitTestReq),
        checkField('样品性状', ['固体', '液体', '其他'], row.ypCharacter),
        checkField('样品状况', ['预包装', '散装', '真空包装'], row.ypSituation),
      ],
    },
    {
      title: '报告领取',
      fields: [
        checkField('报告领取方式', ['自取', '邮寄'], row.getReport),
      ],
    },
    {
      title: '项目备注',
      fields: [textField('项目登记备注', row.remarks_register)],
    },
  ]
}

/** 打开「项目登记信息」窗口 */
export async function openProjectRegisterDialog(
  row: EntityDialogRow | null | undefined,
) {
  if (!row) {
    return
  }

  const bh = asText(row.bh)
  projectRegisterState.bh = bh
  projectRegisterState.sections = []
  projectRegisterState.visible = true
  projectRegisterState.loading = true
  try {
    const [customers, users] = await Promise.all([loadCustomers(), loadUserNames()])
    const source = hasColumn(row, 'remarks_register')
      ? row
      : (await fetchProjectRow(bh)) ?? row
    projectRegisterState.sections = buildProjectRegisterSections(source, customers, users)
  } finally {
    projectRegisterState.loading = false
  }
}

/* ---------------- 项目分解 / 安排 / 检验 明细 ---------------- */

interface ProjectDetailState {
  visible: boolean
  loading: boolean
  type: ProjectDetailType
  bh: string
  row: EntityDialogRow | null
  rows: EntityDialogRow[]
  libInfo: EntityDialogRow | null
  userNames: Record<string, string>
}

export const projectDetailState = reactive<ProjectDetailState>({
  visible: false,
  loading: false,
  type: 'div',
  bh: '',
  row: null,
  rows: [],
  libInfo: null,
  userNames: {},
})

/**
 * 打开「项目分解信息 / 项目安排信息 / 项目检验信息」窗口。
 * 三者共用同一份明细（prj_detail 按 project_bh 关联），区别在于
 * 是否展示检验结果相关的列以及顶部的项目备注。
 */
export async function openProjectDetailDialog(
  row: EntityDialogRow | null | undefined,
  type: ProjectDetailType,
) {
  if (!row) {
    return
  }
  const bh = asText(row.bh)
  if (!bh) {
    return
  }

  projectDetailState.bh = bh
  projectDetailState.type = type
  projectDetailState.row = row
  projectDetailState.rows = []
  projectDetailState.libInfo = null
  projectDetailState.visible = true
  projectDetailState.loading = true

  try {
    projectDetailState.userNames = await loadUserNames()

    const detailResult = await fetchData('prj_detail', {
      page: 1,
      size: 200,
      project_bh: bh,
    })
    const taskResult = await fetchData('task', {
      page: 1,
      size: 200,
      project_bh: bh,
    })
    const finishDate =
      taskResult.records.length > 0 ? asText(taskResult.records[0].date_jyy) : ''
    projectDetailState.rows = detailResult.records.map((item) => ({
      ...item,
      date_jyy: finishDate,
    }))

    const testLib = asText(row.testLib)
    if (testLib) {
      const libResult = await fetchData('product', {
        page: 1,
        size: 20,
        id: testLib,
      })
      projectDetailState.libInfo =
        libResult.records.length > 0 ? libResult.records[0] : null
    }
  } catch {
    // 明细加载失败时保留空表，表格自身的空状态会给出提示
  } finally {
    projectDetailState.loading = false
  }
}
