import http from './http'

import { saveBlobResponse } from '@/utils/fileDownload'

export type ReportType = 'cma' | 'catl'

export interface ReportDownloadRow extends Record<string, unknown> {
  id_nj: number
  taskno: string | null
  bh: string | null
  dwname_sj_nj: string | null
  productName: string | null
  result: string | null
  date_reg: string | null
  date_jycomplete: string | null
}

/** 报告类型与页面上的列标题、链接文字一一对应 */
export const reportColumns: Array<{
  type: ReportType
  label: string
  linkText: string
  width: number
}> = [
  { type: 'catl', label: 'CMA_CATL报告', linkText: '下载CMA_CATL报告', width: 165 },
  { type: 'cma', label: 'CMA报告', linkText: '下载CMA报告', width: 140 },
]

/**
 * 下载报告：后端按样品编号实时生成 Excel 报告。
 * 请求带 Token（走统一 axios 实例），文件名优先取响应头 filename*。
 */
export async function downloadReport(type: ReportType, bh: string): Promise<void> {
  const response = await http.get<Blob>(`/report-download/${type}`, {
    params: { bh },
    responseType: 'blob',
  })
  await saveBlobResponse(
    response,
    `${bh}-${type === 'catl' ? 'CMA_CATL' : 'CMA'}报告.xls`,
  )
}
