import { dataOf, http } from './http'
import type { CategoryNode, CategorySave } from '../types'

export const categoryApi = {
  tree: () => dataOf<CategoryNode[]>(http.get('/categories')),
  create: (payload: CategorySave) => dataOf<string>(http.post('/categories', payload)),
  update: (id: string, payload: CategorySave) => dataOf<void>(http.put(`/categories/${id}`, payload)),
  remove: (id: string) => dataOf<void>(http.delete(`/categories/${id}`)),
}
