export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
}

export interface UserInfo {
  gh: string
  name: string
  userRoleId: string
  deptId: string | null
}
