export type ContentType = 'VIDEO' | 'DOC' | 'TEXT'

export interface CategoryNode {
  id: string
  parentId: string | null
  name: string
  sort: number
  status: number
  children: CategoryNode[]
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

export interface ContentItem {
  id: string
  categoryId: string
  type: ContentType
  title: string
  description?: string
  status: string
  sort: number
  coverUrl?: string
  videoUrl?: string
  fileName?: string
  mime?: string
  fileSize?: number
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

export interface ContentDetail extends ContentItem {
  file?: ContentFile
  body?: string
  progressSeconds?: number
}

export interface ContinueWatching {
  contentId: string
  title: string
  description?: string
  coverUrl?: string
  videoUrl: string
  duration?: number
  progressSeconds: number
  progressPercent: number
  watchedAt: string
}
