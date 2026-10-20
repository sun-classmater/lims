import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'

export interface ArrangeRow extends Record<string, unknown> {
  id_nj: number
  bh: string
  taskno: string | null
  productName: string | null
  date_yywc: string | null
  state_div: string | null
  state_arrange: string | null
  detail_count: number
  task_count: number
}

export interface ArrangeListResult {
  records: ArrangeRow[]
  total: number
}

export interface ArrangeResult {
  total: number
  assigned: number
  skipped: number
  messages: string[]
}

/** 待安排检验项目 */
export async function fetchPendingArrange(
  taskno?: string,
): Promise<ArrangeListResult> {
  const response = await http.get<ApiResponse<ArrangeListResult>>('/arrange/pending', {
    params: { taskno },
  })
  return unwrap(response)
}

/** 已安排检验项目 */
export async function fetchDoneArrange(): Promise<ArrangeListResult> {
  const response = await http.get<ApiResponse<ArrangeListResult>>('/arrange/done')
  return unwrap(response)
}

/** 安排检验任务：按算法分配检验员并生成任务 */
export async function assignTasks(
  bhs: string[],
  taskno?: string,
): Promise<ArrangeResult> {
  const response = await http.post<ApiResponse<ArrangeResult>>('/arrange/assign', {
    bhs,
    taskno,
  })
  return unwrap(response)
}

/** 转待安排 */
export async function revertArrange(bhs: string[]): Promise<number> {
  const response = await http.post<ApiResponse<number>>('/arrange/revert', { bhs })
  return unwrap(response)
}

/** 退分解 */
export async function returnDecompose(bhs: string[]): Promise<number> {
  const response = await http.post<ApiResponse<number>>('/arrange/return-decompose', {
    bhs,
  })
  return unwrap(response)
}

/** 退登记 */
export async function arrangeReturnRegister(bhs: string[]): Promise<number> {
  const response = await http.post<ApiResponse<number>>('/arrange/return-register', {
    bhs,
  })
  return unwrap(response)
}
