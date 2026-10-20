import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'

export interface InspectTaskRow extends Record<string, unknown> {
  id: number
  userId_jyy: string | null
  state_jyy: string | null
  date_jyy: string | null
  bh: string
  taskno: string | null
  productName: string | null
  date_yywc: string | null
  date_sy: string | null
  date_cy: string | null
  issue_date: string | null
}

export interface InspectItemRow extends Record<string, unknown> {
  project_bh: string
  productName: string | null
  taskno: string | null
  item_xh: number
  testItem: string | null
  basis: string | null
  mathod: string | null
  stdValue: string | null
  jyResult: string | null
  jyResult_note: string | null
  item_evaluate: string | null
  userId_jyy: string | null
  inspector_name: string | null
}

export interface InspectListResult {
  records: InspectTaskRow[]
  total: number
}

export interface SaveInspectResult {
  saved: number
  completed: number
  messages: string[]
}

export interface InspectResultItem {
  project_bh: string
  item_xh: number
  jyResult: string
  item_evaluate: string
  jyResult_note: string
}

/** 待检验检验任务 */
export async function fetchPendingInspect(mine: boolean): Promise<InspectListResult> {
  const response = await http.get<ApiResponse<InspectListResult>>('/inspect/pending', {
    params: { mine },
  })
  return unwrap(response)
}

/** 已检验检验任务 */
export async function fetchDoneInspect(mine: boolean): Promise<InspectListResult> {
  const response = await http.get<ApiResponse<InspectListResult>>('/inspect/done', {
    params: { mine },
  })
  return unwrap(response)
}

/** 样品的检验项目明细 */
export async function fetchInspectItems(bhs: string[]): Promise<InspectItemRow[]> {
  if (bhs.length === 0) {
    return []
  }
  const response = await http.get<ApiResponse<InspectItemRow[]>>('/inspect/items', {
    params: { bhs: bhs.join(',') },
  })
  return unwrap(response)
}

/** 保存检验数据；complete=true 时同时标记检验完成 */
export async function saveInspectResults(
  items: InspectResultItem[],
  complete: boolean,
): Promise<SaveInspectResult> {
  const response = await http.post<ApiResponse<SaveInspectResult>>('/inspect/save', {
    items,
    complete,
  })
  return unwrap(response)
}

/** 转待检验 */
export async function revertInspect(bhs: string[]): Promise<number> {
  const response = await http.post<ApiResponse<number>>('/inspect/revert', { bhs })
  return unwrap(response)
}

/** 退安排 */
export async function returnArrange(bhs: string[]): Promise<number> {
  const response = await http.post<ApiResponse<number>>('/inspect/return-arrange', { bhs })
  return unwrap(response)
}
