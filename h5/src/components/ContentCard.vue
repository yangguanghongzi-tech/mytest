<script setup lang="ts">
import { computed } from 'vue'
import type { ContentItem } from '../types'

const props = defineProps<{ item: ContentItem }>()
const typeName = computed(() => ({ VIDEO: '视频', DOC: '文档', TEXT: '图文' }[props.item.type]))

function durationText(seconds?: number) {
  if (!seconds) return ''
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor(seconds % 3600 / 60)
  const rest = seconds % 60
  return hours ? `${hours}:${String(minutes).padStart(2, '0')}:${String(rest).padStart(2, '0')}` : `${minutes}:${String(rest).padStart(2, '0')}`
}

function showFirstFrame(event: Event) {
  const video = event.currentTarget as HTMLVideoElement
  if (Number.isFinite(video.duration) && video.duration > 0) {
    video.currentTime = Math.min(0.05, video.duration)
  }
}
</script>

<template>
  <article class="content-card">
    <div class="card-cover">
      <van-image v-if="item.coverUrl" :src="item.coverUrl" width="100%" height="100%" fit="cover" />
      <video v-else-if="item.type === 'VIDEO' && item.videoUrl" class="video-first-frame" :src="item.videoUrl" muted playsinline preload="metadata" @loadedmetadata="showFirstFrame" />
      <div v-else class="cover-fallback" :class="`type-${item.type.toLowerCase()}`"><van-icon :name="item.type === 'VIDEO' ? 'play-circle-o' : item.type === 'DOC' ? 'description-o' : 'notes-o'" /></div>
      <span class="type-badge">{{ typeName }}</span>
      <span v-if="item.type === 'VIDEO' && item.duration" class="duration">{{ durationText(item.duration) }}</span>
    </div>
    <div class="card-body">
      <h3>{{ item.title }}</h3>
      <p>{{ item.description || '点击查看学习内容' }}</p>
    </div>
  </article>
</template>
