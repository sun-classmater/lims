import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'

export interface RegisterQueryRow {
  id_nj: number
  taskno: string | null
  bh: string | null
  dwname_sj_nj: string | null
  dwname_sc_nj: string | null
  productName: string | null
  stateDiv: string | null
  byNum: string | null
  date_reg: string | null
  date_yywc: string | null
  result: string | null
  detailCount: number
}

export interface DivideDetailRow {
  itemXh: number
  testItem: string | null
  basis: string | null
  mathod: string | null
  jyResult: string | null
  itemEvaluate: string | null
}

export interface RegisterQueryParams {
  page: number
  size: number
  bhStart?: string
  bhEnd?: string
  regStart?: string
  regEnd?: string
  productName?: string
  sjUnit?: string
  taskno?: string
}

export interface RegisterQueryResult {
  records: RegisterQueryRow[]
  total: number
}

export async function fetchRegisterQuery(
  params: RegisterQueryParams,
): Promise<RegisterQueryResult> {
  const response = await http.get<ApiResponse<RegisterQueryResult>>(
    '/sample-register-query/list',
    { params },
  )
  return unwrap(response)
}

export async function fetchDivideDetail(bh: string): Promise<DivideDetailRow[]> {
  const response = await http.get<ApiResponse<DivideDetailRow[]>>(
    '/sample-register-query/divide-detail',
    { params: { bh } },
  )
  return unwrap(response)
}
