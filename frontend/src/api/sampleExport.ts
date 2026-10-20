import http from './http'

import { saveBlobResponse } from '@/utils/fileDownload'

export type SampleExportType = 'water' | 'agri' | 'livestock' | 'detail'

export interface SampleExportRow extends Record<string, unknown> {
  id_nj: number
  taskno: string | null
  bh: string | null
  productName: string | null
  dwname_sj_nj: string | null
  result: string | null
  date_reg: string | null
  date_yywc: string | null
  date_jycomplete: string | null
}

/** 页面上的四个导出按钮 */
export const exportButtons: Array<{
  type: SampleExportType
  label: string
  fallbackName: string
}> = [
  { type: 'water', label: '水检上报Excel', fallbackName: '水检上报数据.xls' },
  { type: 'agri', label: '农检上报Excel', fallbackName: '农检上报数据.xls' },
  { type: 'livestock', label: '畜检上报Excel', fallbackName: '畜检上报数据.xls' },
  { type: 'detail', label: '检验明细Excel', fallbackName: '检验明细数据.xls' },
]

/**
 * 导出勾选样品的检测项结果数据。
 * 勾选数量较多时用 POST 提交，避免 URL 过长。
 */
export async function exportSamples(
  type: SampleExportType,
  bhs: string[],
): Promise<void> {
  const response = await http.post<Blob>(
    `/sample-export/${type}`,
    { bhs },
    { responseType: 'blob' },
  )
  const fallbackName =
    exportButtons.find((button) => button.type === type)?.fallbackName ??
    '导出数据.xls'
  await saveBlobResponse(response, fallbackName)
}
