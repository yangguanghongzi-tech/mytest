import axios, { AxiosError } from 'axios'
import { showFailToast } from 'vant'
import { clearToken, getToken } from '../utils/auth'

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/h5/api',
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
      showFailToast(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    if (error.response?.status === 401) {
      clearToken()
      window.location.replace('/login')
      return Promise.reject(error)
    }
    showFailToast(error.response?.data?.message || error.message || '网络请求失败')
    return Promise.reject(error)
  },
)

export async function dataOf<T>(request: Promise<{ data: ApiResult<T> }>) {
  const response = await request
  return response.data.data
}
