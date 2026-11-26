import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, redirectToLogin } from '../utils/auth'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/admin/api',
  timeout: 20_000,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      const message = body.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    if (error.response?.status === 401) {
      redirectToLogin()
      return Promise.reject(error)
    }
    const message = error.response?.data?.message || error.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export async function dataOf<T>(request: Promise<{ data: ApiResult<T> }>) {
  const response = await request
  return response.data.data
}
