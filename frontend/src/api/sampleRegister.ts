import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'

export interface PendingProject {
  id_nj: number
  taskno: string | null
  bh: string | null
  dwname_sj_nj: string | null
  productName: string | null
  date_reg: string | null
  registerName: string
  ggxh: string | null
  ypsb: string | null
  ypdj: string | null
  ypNum: string | null
  byNum: string | null
  cyjs: string | null
  cyaddr: string | null
  ypCharacter: string | null
  ypSituation: string | null
  ypSaveReq: string | null
  yyProcess: string | null
  ypbz: string | null
}

export interface RegisteredProject {
  id_nj: number
  taskno: string | null
  bh: string | null
  productName: string | null
  date_yywc: string | null
  registerName: string
}

export interface RegisterPageResult<T> {
  records: T[]
  total: number
}

interface PageQuery {
  page: number
  size: number
}

export async function fetchPendingProjects(
  params: PageQuery & { taskno?: string; bh?: string },
): Promise<RegisterPageResult<PendingProject>> {
  const response = await http.get<ApiResponse<RegisterPageResult<PendingProject>>>(
    '/sample-register/pending',
    { params },
  )
  return unwrap(response)
}

export async function fetchRegisteredProjects(
  params: PageQuery & { bh?: string },
): Promise<RegisterPageResult<RegisteredProject>> {
  const response = await http.get<ApiResponse<RegisterPageResult<RegisteredProject>>>(
    '/sample-register/registered',
    { params },
  )
  return unwrap(response)
}

export async function confirmRegister(ids: number[]): Promise<number> {
  const response = await http.post<ApiResponse<{ affected: number }>>(
    '/sample-register/confirm',
    { ids },
  )
  return unwrap(response).affected
}

export async function revertRegister(id: number): Promise<number> {
  const response = await http.post<ApiResponse<{ affected: number }>>(
    '/sample-register/revert',
    { id },
  )
  return unwrap(response).affected
}
