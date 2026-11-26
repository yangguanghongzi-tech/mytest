import { dataOf, http } from './http'
import type { CategoryNode, ContentDetail, ContentItem, ContinueWatching, PageResult } from '../types'

export const learningApi = {
  categories: () => dataOf<CategoryNode[]>(http.get('/categories')),
  contents: (categoryId: string | undefined, page: number, size = 10) =>
    dataOf<PageResult<ContentItem>>(http.get('/contents', { params: { categoryId, page, size } })),
  detail: (id: string) => dataOf<ContentDetail>(http.get(`/contents/${id}`)),
  continueWatching: () => dataOf<ContinueWatching | null>(http.get('/contents/continue-watching')),
  saveProgress: (id: string, progressSeconds: number) =>
    dataOf<void>(http.put(`/contents/${id}/progress`, { progressSeconds })),
}
