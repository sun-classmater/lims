import http, { unwrap } from './http'

import type { ApiResponse } from '@/types/api'

export interface PageResult<T> {
  records: T[]
  total: number
}

export interface FileUploadResult {
  originalName: string
  storedName: string
  size: number
  url: string
}

export interface FileInfo {
  name: string
  url: string
}

export async function fetchData(
  entity: string,
  params: Record<string, string | number>,
): Promise<PageResult<Record<string, unknown>>> {
  const response = await http.get<ApiResponse<PageResult<Record<string, unknown>>>>(
    `/data/${entity}`,
    { params },
  )
  return unwrap(response)
}

export async function fetchExport(
  entity: string,
  params: Record<string, string | number>,
): Promise<Array<Record<string, unknown>>> {
  const response = await http.get<ApiResponse<Array<Record<string, unknown>>>>(
    `/data/${entity}/export`,
    { params },
  )
  return unwrap(response)
}

export async function createData(
  entity: string,
  data: Record<string, unknown>,
): Promise<number> {
  const response = await http.post<ApiResponse<number>>(`/data/${entity}`, data)
  return unwrap(response)
}

export async function updateData(
  entity: string,
  data: Record<string, unknown>,
): Promise<number> {
  const response = await http.put<ApiResponse<number>>(`/data/${entity}`, data)
  return unwrap(response)
}

export async function deleteData(
  entity: string,
  keys: Record<string, string>,
): Promise<number> {
  const response = await http.delete<ApiResponse<number>>(`/data/${entity}`, {
    params: keys,
  })
  return unwrap(response)
}

export async function uploadFile(file: File): Promise<FileUploadResult> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await http.post<ApiResponse<FileUploadResult>>(
    '/files/upload',
    formData,
  )
  return unwrap(response)
}

export async function fetchFiles(): Promise<FileInfo[]> {
  const response = await http.get<ApiResponse<FileInfo[]>>('/files')
  return unwrap(response)
}
