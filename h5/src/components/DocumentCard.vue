<script setup lang="ts">
import { computed, onMounted } from 'vue'
import type { ContentItem } from '../types'

const props = defineProps<{ item: ContentItem }>()

const fileExtension = computed(() => {
  const fileName = props.item.fileName || ''
  const dotIndex = fileName.lastIndexOf('.')
  return dotIndex >= 0 ? fileName.slice(dotIndex + 1).trim().toLowerCase() : ''
})

const documentKind = computed(() => {
  if (fileExtension.value === 'pdf') return 'pdf'
  if (fileExtension.value === 'doc' || fileExtension.value === 'docx') return 'doc'
  if (fileExtension.value === 'xls' || fileExtension.value === 'xlsx') return 'xls'
  if (fileExtension.value === 'ppt' || fileExtension.value === 'pptx') return 'ppt'
  return 'file'
})

const extension = computed(() => documentKind.value === 'file' ? 'FILE' : documentKind.value.toUpperCase())

const actionText = computed(() => {
  if (extension.value === 'PDF') return '预览'
  if (extension.value === 'PPT') return '预览'
  return '预览'
})

const metaText = computed(() => {
  const values: string[] = []
  if (props.item.fileSize) values.push(formatSize(props.item.fileSize))
  values.push(props.item.description || '学习资料')
  return values.join('　·　')
})

function formatSize(size: number) {
  if (size >= 1024 ** 3) return `${(size / 1024 ** 3).toFixed(1)} GB`
  if (size >= 1024 ** 2) return `${(size / 1024 ** 2).toFixed(size >= 10 * 1024 ** 2 ? 1 : 2)} MB`
  return `${Math.max(1, Math.ceil(size / 1024))} KB`
}

onMounted(() => {
  if (!import.meta.env.DEV) return
  console.info('[DocumentCard] 文档格式解析', {
    title: props.item.title,
    fileName: props.item.fileName,
    fileExtension: fileExtension.value,
    documentKind: documentKind.value,
  })
})
</script>

<template>
  <article class="document-card">
    <div class="document-icon" :class="`document-${documentKind}`">
      <svg v-if="documentKind === 'pdf'" class="document-type-svg" viewBox="0 0 32 32" aria-hidden="true">
        <path d="M7 3.5h13l5 5V28.5H7z" />
        <path d="M20 3.5v5h5M10.5 13h11v8h-11z" />
        <text x="16" y="19" text-anchor="middle">PDF</text>
      </svg>
      <svg v-else-if="documentKind === 'ppt'" class="document-type-svg" viewBox="0 0 32 32" aria-hidden="true">
        <path d="M6 5h20v22H6z" />
        <path d="M11 10h10v8H11zM13.5 12.2l5 1.8-5 1.8zM10 22h12" />
      </svg>
      <svg v-else-if="documentKind === 'xls'" class="document-type-svg" viewBox="0 0 32 32" aria-hidden="true">
        <path d="M7 4h18v24H7zM7 11h18M7 18h18M13 11v17M19 11v17" />
        <path d="m10 7 4 3m0-3-4 3" />
      </svg>
      <svg v-else class="document-type-svg" viewBox="0 0 32 32" aria-hidden="true">
        <path d="M7 3.5h13l5 5V28.5H7z" />
        <path d="M20 3.5v5h5M11 14h10M11 18h10M11 22h7" />
      </svg>
      <strong>{{ extension }}</strong>
    </div>
    <div class="document-info">
      <h3>{{ item.title }}</h3>
      <p>{{ metaText }}</p>
    </div>
    <button type="button" class="document-action">{{ actionText }}</button>
  </article>
</template>
