import { dataOf, http } from './http'
import type { UploadInit, UploadPartUrl, UploadProgress, UploadPurpose } from '../types'

export const uploadApi = {
  init: (purpose: UploadPurpose, fileName: string, fileSize: number) =>
    dataOf<UploadInit>(http.post('/uploads', { purpose, fileName, fileSize })),
  partUrls: (id: string, partNumbers: number[]) =>
    dataOf<UploadPartUrl[]>(http.post(`/uploads/${id}/part-urls`, { partNumbers })),
  progress: (id: string) => dataOf<UploadProgress>(http.get(`/uploads/${id}`)),
  complete: (id: string) => dataOf<{ taskId: string; fileName: string; fileSize: number }>(http.post(`/uploads/${id}/complete`)),
  abort: (id: string) => dataOf<void>(http.delete(`/uploads/${id}`)),
}
