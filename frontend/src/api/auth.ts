import http, { unwrap } from './http'

import type {
  ApiResponse,
  LoginParams,
  LoginResult,
  UserInfo,
} from '@/types/api'

export async function loginApi(params: LoginParams): Promise<LoginResult> {
  const response = await http.post<ApiResponse<LoginResult>>('/auth/login', params)
  return unwrap(response)
}

export async function getCurrentUserApi(): Promise<UserInfo> {
  const response = await http.get<ApiResponse<UserInfo>>('/auth/me')
  return unwrap(response)
}
