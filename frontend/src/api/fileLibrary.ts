import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'
import { saveBlobResponse } from '@/utils/fileDownload'

export interface FileLibraryItem {
  type: string
  name: string
}

/** 中心文件资料：可下载的模板列表 */
export async function fetchFileLibrary(): Promise<FileLibraryItem[]> {
  const response = await http.get<ApiResponse<FileLibraryItem[]>>('/file-library')
  return unwrap(response)
}

/** 下载指定模板 */
export async function downloadFileLibrary(type: string, name: string): Promise<void> {
  const response = await http.get<Blob>(`/file-library/${type}`, {
    responseType: 'blob',
  })
  await saveBlobResponse(response, name)
}
