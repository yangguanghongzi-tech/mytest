export type ContentType = 'VIDEO' | 'DOC' | 'TEXT'
export type UploadPurpose = 'VIDEO' | 'DOC' | 'COVER'

export interface CategoryNode {
  id: string
  parentId: string | null
  name: string
  sort: number
  status: number
  children: CategoryNode[]
}

export interface CategorySave {
  parentId: string | null
  name: string
  sort: number
  status: number
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

export interface ContentListItem {
  id: string
  categoryId: string
  type: ContentType
  title: string
  description?: string
  status: string
  sort: number
  coverUrl?: string
  duration?: number
  publishedAt?: string
  updatedAt: string
}

export interface ContentFile {
  fileName: string
  mime: string
  fileSize: number
  duration?: number
  url: string
  downloadUrl?: string
}

export interface ContentDetail extends ContentListItem {
  file?: ContentFile
  body?: string
}

export interface ContentSave {
  categoryId: string | null
  type: ContentType
  title: string
  description: string
  sort: number
  coverTaskId: string | null
  removeCover: boolean
  fileTaskId: string | null
  duration: number | null
  body: string | null
}

export interface UploadInit {
  taskId: string
  partSize: number
  partCount: number
}

export interface UploadPartUrl {
  partNumber: number
  url: string
}

export interface UploadProgress {
  taskId: string
  status: string
  partSize: number
  partCount: number
  uploadedParts: number[]
}
