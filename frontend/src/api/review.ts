import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'

export interface IssueRow extends Record<string, unknown> {
  id_nj: number
  bh: string
  taskno: string | null
  productName: string | null
  result: string | null
  date_jycomplete: string | null
  state_verify: string | null
  client_name: string | null
}

export interface SummaryRow extends Record<string, unknown> {
  inspector_name: string | null
  userId_jyy: string | null
  project_bh: string
  item_xh: number
  testItem: string | null
  jyResult: string | null
  item_evaluate: string | null
  jyResult_note: string | null
  finish_date: string | null
  issue_date: string | null
  stamp_date: string | null
}

export interface ReviewListResult<T> {
  records: T[]
  total: number
}

export type ReviewFilters = Record<string, string | undefined>

/** 待授权签发项目 */
export async function fetchPendingIssue(
  params: ReviewFilters,
): Promise<ReviewListResult<IssueRow>> {
  const response = await http.get<ApiResponse<ReviewListResult<IssueRow>>>(
    '/review/pending',
    { params },
  )
  return unwrap(response)
}

/** 签发 */
export async function issueProjects(bhs: string[]): Promise<number> {
  const response = await http.post<ApiResponse<number>>('/review/issue', { bhs })
  return unwrap(response)
}

/** 退登记 */
export async function reviewReturnRegister(bhs: string[]): Promise<number> {
  const response = await http.post<ApiResponse<number>>('/review/return-register', { bhs })
  return unwrap(response)
}

/** 检验任务完成汇总查询 */
export async function fetchInspectSummary(
  params: ReviewFilters,
): Promise<ReviewListResult<SummaryRow>> {
  const response = await http.get<ApiResponse<ReviewListResult<SummaryRow>>>(
    '/review/summary',
    { params },
  )
  return unwrap(response)
}
