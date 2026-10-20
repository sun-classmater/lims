import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'

export interface DecomposeRow extends Record<string, unknown> {
  id_nj: number
  bh: string
  taskno: string | null
  productName: string | null
  date_yywc: string | null
  state_div: string | null
  testLib: string | null
  detail_count: number
}

export interface DecomposeListResult {
  records: DecomposeRow[]
  total: number
}

export interface DecomposeResult {
  total: number
  generated: number
  skipped: number
  completed?: number
  messages: string[]
}

export interface DecomposeRevertResult {
  total: number
  reverted: number
  messages: string[]
}

/** 待分解样品检验项目 */
export async function fetchPendingSamples(params: {
  taskno?: string
  productName?: string
}): Promise<DecomposeListResult> {
  const response = await http.get<ApiResponse<DecomposeListResult>>(
    '/decompose/pending',
    { params },
  )
  return unwrap(response)
}

/** 已分解样品检验项目 */
export async function fetchDoneSamples(): Promise<DecomposeListResult> {
  const response = await http.get<ApiResponse<DecomposeListResult>>('/decompose/done')
  return unwrap(response)
}

/** 批量分解：按勾选样品，或按任务号 / 样品名称生成检验明细 */
export async function generateDetails(payload: {
  bhs?: string[]
  library?: string
  taskno?: string
  keyword?: string
}): Promise<DecomposeResult> {
  const response = await http.post<ApiResponse<DecomposeResult>>(
    '/decompose/generate',
    payload,
  )
  return unwrap(response)
}

/** 分解完毕：单条或批量 */
export async function completeDecompose(
  bhs: string[],
  library?: string,
): Promise<DecomposeResult> {
  const response = await http.post<ApiResponse<DecomposeResult>>('/decompose/complete', {
    bhs,
    library,
  })
  return unwrap(response)
}

/** 转待分解 */
export async function revertDecompose(bhs: string[]): Promise<DecomposeRevertResult> {
  const response = await http.post<ApiResponse<DecomposeRevertResult>>(
    '/decompose/revert',
    { bhs },
  )
  return unwrap(response)
}

/** 退登记 */
export async function returnToRegister(bhs: string[]): Promise<number> {
  const response = await http.post<ApiResponse<number>>('/decompose/return-register', {
    bhs,
  })
  return unwrap(response)
}
