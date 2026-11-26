import { dataOf, http } from './http'
import type { ContentDetail, ContentListItem, ContentSave, ContentType, PageResult } from '../types'

export interface ContentQuery {
  categoryId?: string
  type?: ContentType
  status?: string
  keyword?: string
  page: number
  size: number
}

export const contentApi = {
  page: (params: ContentQuery) => dataOf<PageResult<ContentListItem>>(http.get('/contents', { params })),
  detail: (id: string) => dataOf<ContentDetail>(http.get(`/contents/${id}`)),
  create: (payload: ContentSave) => dataOf<string>(http.post('/contents', payload)),
  update: (id: string, payload: ContentSave) => dataOf<void>(http.put(`/contents/${id}`, payload)),
  remove: (id: string) => dataOf<void>(http.delete(`/contents/${id}`)),
  publish: (id: string) => dataOf<void>(http.post(`/contents/${id}/publish`)),
  unpublish: (id: string) => dataOf<void>(http.post(`/contents/${id}/unpublish`)),
}
