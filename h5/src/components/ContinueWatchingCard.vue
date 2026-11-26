<script setup lang="ts">
import type { ContinueWatching } from '../types'

defineProps<{ item: ContinueWatching }>()

function showFrame(event: Event) {
  const video = event.currentTarget as HTMLVideoElement
  if (Number.isFinite(video.duration) && video.duration > 0) {
    video.currentTime = Math.min(0.05, video.duration)
  }
}
</script>

<template>
  <article class="continue-card">
    <div class="continue-cover">
      <van-image v-if="item.coverUrl" :src="item.coverUrl" width="100%" height="100%" fit="cover" />
      <video v-else :src="item.videoUrl" muted playsinline preload="metadata" @loadedmetadata="showFrame" />
      <span class="continue-play"><van-icon name="play" /></span>
    </div>
    <div class="continue-info">
      <div class="continue-label">正在看</div>
      <h3>{{ item.title }}</h3>
      <p v-if="item.description">{{ item.description }}</p>
      <div class="continue-progress">
        <span><i :style="{ width: `${item.progressPercent}%` }" /></span>
        <strong>{{ item.progressPercent }}%</strong>
      </div>
    </div>
    <button class="continue-button" aria-label="继续播放"><van-icon name="play" /></button>
  </article>
</template>
