import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'
import { saveBlobResponse } from '@/utils/fileDownload'

export interface LibImportRow {
  xh: number
  testItem: string
  basis: string
  mathod: string
  standardValue: string
  lowerLimit: string
  methodNote: string
}

export interface LibImportPreview {
  token: string
  productId: string
  prdCategory: string
  libName: string
  total: number
  duplicated: boolean
  rows: LibImportRow[]
}

export interface LibImportResult {
  productId: string
  libName: string
  inserted: number
}

/** 上传项目库文件并解析预览 */
export async function previewLibImport(file: File): Promise<LibImportPreview> {
  const form = new FormData()
  form.append('file', file)
  const response = await http.post<ApiResponse<LibImportPreview>>(
    '/lib-import/preview',
    form,
  )
  return unwrap(response)
}

/** 确认导入数据库 */
export async function confirmLibImport(token: string): Promise<LibImportResult> {
  const response = await http.post<ApiResponse<LibImportResult>>(
    '/lib-import/confirm',
    { token },
  )
  return unwrap(response)
}

/** 下载项目库模板 */
export async function downloadLibTemplate(): Promise<void> {
  const response = await http.get<Blob>('/lib-import/template', {
    responseType: 'blob',
  })
  await saveBlobResponse(response, '项目库模板lib.xls')
}
