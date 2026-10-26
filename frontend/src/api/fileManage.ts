import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'
import { saveBlobResponse } from '@/utils/fileDownload'

export interface StoredFile {
  name: string
  originalName: string
  size: number
  uploadedAt: string
  url: string
}

/** 中心资料文件列表 */
export async function fetchStoredFiles(): Promise<StoredFile[]> {
  const response = await http.get<ApiResponse<StoredFile[]>>('/files')
  return unwrap(response)
}

/** 上传文件到服务器 */
export async function uploadStoredFile(file: File): Promise<StoredFile> {
  const form = new FormData()
  form.append('file', file)
  const response = await http.post<ApiResponse<StoredFile>>('/files/upload', form)
  return unwrap(response)
}

/** 删除文件 */
export async function deleteStoredFile(name: string): Promise<number> {
  const response = await http.delete<ApiResponse<number>>(`/files/${name}`)
  return unwrap(response)
}

/** 下载文件（按原始文件名保存） */
export async function downloadStoredFile(name: string, fileName: string): Promise<void> {
  const response = await http.get<Blob>(`/files/${name}`, { responseType: 'blob' })
  await saveBlobResponse(response, fileName)
}
