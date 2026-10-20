import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'

export interface SampleSheetPreview {
  token: string
  marker: string
  originalName: string
  columns: string[]
  columnKeys: string[]
  rows: Array<Record<string, string>>
  total: number
  duplicated: boolean
}

export interface SampleSheetImportResult {
  marker: string
  imported: number
}

export async function previewSampleSheet(file: File): Promise<SampleSheetPreview> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await http.post<ApiResponse<SampleSheetPreview>>(
    '/sample-sheet/preview',
    formData,
    { timeout: 30000 },
  )
  return unwrap(response)
}

export async function confirmSampleSheetImport(token: string): Promise<SampleSheetImportResult> {
  const formData = new FormData()
  formData.append('token', token)

  const response = await http.post<ApiResponse<SampleSheetImportResult>>(
    '/sample-sheet/import',
    formData,
  )
  return unwrap(response)
}

export async function downloadSampleSheetTemplate(): Promise<void> {
  const response = await http.get('/sample-sheet/template', {
    responseType: 'blob',
  })

  const blob = new Blob([response.data as BlobPart], {
    type: 'application/vnd.ms-excel',
  })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '抽样单模板.xls'
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}
