<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { learningApi } from '../api/learning'
import type { ContentDetail } from '../types'
import { getToken } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const detail = ref<ContentDetail>()
const loading = ref(true)
const videoRef = ref<HTMLVideoElement>()
let lastSavedSeconds = 0
let saving = false
let queuedSeconds: number | null = null
let playbackInitialized = false
let playbackCompleted = false
let lastKnownSeconds = 0
const typeName = computed(() => detail.value ? ({ VIDEO: '视频课程', DOC: '学习文档', TEXT: '图文内容' }[detail.value.type]) : '')

function progressCacheKey(id: string) {
  return `learning:video-progress:v2:${id}`
}

function readCachedProgress(id: string) {
  try {
    const raw = sessionStorage.getItem(progressCacheKey(id))
    if (!raw) return 0
    const cached = JSON.parse(raw) as { seconds?: number; savedAt?: number }
    // 只保护刷新/热更新的短暂竞态，不能让过期本地值长期压过后端数据。
    if (!cached.savedAt || Date.now() - cached.savedAt > 5 * 60_000) return 0
    return Number.isFinite(cached.seconds) && (cached.seconds || 0) > 0
      ? Math.floor(cached.seconds as number)
      : 0
  } catch {
    return 0
  }
}

function rememberProgress(seconds: number) {
  if (!detail.value || seconds <= 0) return
  const duration = detail.value.file?.duration || videoRef.value?.duration || 0
  if (duration > 0 && seconds >= duration) {
    sessionStorage.removeItem(progressCacheKey(detail.value.id))
  } else {
    sessionStorage.setItem(progressCacheKey(detail.value.id), JSON.stringify({
      seconds,
      savedAt: Date.now(),
    }))
  }
}

function formatSize(size?: number) {
  if (!size) return ''
  if (size >= 1024 ** 3) return `${(size / 1024 ** 3).toFixed(1)} GB`
  if (size >= 1024 ** 2) return `${(size / 1024 ** 2).toFixed(1)} MB`
  return `${Math.ceil(size / 1024)} KB`
}

function showFirstFrame(event: Event) {
  const video = event.currentTarget as HTMLVideoElement
  const resumeAt = lastKnownSeconds || detail.value?.progressSeconds || 0
  if (resumeAt > 0 && Number.isFinite(video.duration)) {
    playbackInitialized = false
    video.currentTime = Math.min(resumeAt, Math.max(0, video.duration - 0.1))
  } else if (!detail.value?.coverUrl && Number.isFinite(video.duration) && video.duration > 0) {
    video.currentTime = Math.min(0.05, video.duration)
    playbackInitialized = true
  } else {
    playbackInitialized = true
  }
}

function finishResume() {
  playbackInitialized = true
  const seconds = Math.max(0, Math.floor(videoRef.value?.currentTime || 0))
  if (seconds > 0) lastKnownSeconds = seconds
}

function beginMediaReload() {
  // 媒体资源重新加载时保留内存中的最新卡点，禁止新的 0 秒 timeupdate 写入。
  playbackInitialized = false
}

async function persistProgress(seconds: number) {
  queuedSeconds = seconds
  if (saving || !detail.value) return
  saving = true
  try {
    while (queuedSeconds !== null) {
      const next = queuedSeconds
      queuedSeconds = null
      await learningApi.saveProgress(detail.value.id, next)
      lastSavedSeconds = next
      rememberProgress(next)
    }
  } finally {
    saving = false
  }
}

function saveCurrentProgress(force = false) {
  if (!playbackInitialized || playbackCompleted) return
  const currentSeconds = Math.max(0, Math.floor(videoRef.value?.currentTime || 0))
  const declaredDuration = detail.value?.file?.duration || 0
  // 后台读取的整数时长可能比浏览器实际时长少几秒。普通进度最多保存到 duration - 1，
  // 只有 ended 才保存完整时长，避免视频尚未真正结束就从“继续观看”消失。
  const seconds = declaredDuration > 0
    ? Math.min(currentSeconds, Math.max(0, declaredDuration - 1))
    : currentSeconds
  // 初始化、刷新或销毁期间的瞬时 0 不是有效观看进度，不能覆盖后端卡点。
  if (seconds === 0 && lastSavedSeconds > 0) return
  if (seconds > 0) {
    lastKnownSeconds = seconds
    rememberProgress(seconds)
  }
  if (!force && Math.abs(seconds - lastSavedSeconds) < 5) return
  void persistProgress(seconds).catch(() => undefined)
}

function completePlayback() {
  if (!playbackInitialized || !videoRef.value) return
  playbackCompleted = true
  const duration = Math.ceil(videoRef.value.duration || detail.value?.file?.duration || 0)
  sessionStorage.removeItem(progressCacheKey(detail.value.id))
  void persistProgress(duration).catch(() => undefined)
}

function flushProgressKeepalive() {
  if (!detail.value || playbackCompleted || lastKnownSeconds <= 0) return
  rememberProgress(lastKnownSeconds)
  const base = (import.meta.env.VITE_API_BASE_URL || '/h5/api').replace(/\/$/, '')
  const token = getToken()
  void fetch(`${base}/contents/${detail.value.id}/progress`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ progressSeconds: lastKnownSeconds }),
    keepalive: true,
  }).catch(() => undefined)
}

onMounted(async () => {
  try {
    detail.value = await learningApi.detail(String(route.params.id))
    lastSavedSeconds = detail.value.progressSeconds || 0
    const cached = readCachedProgress(detail.value.id)
    lastKnownSeconds = Math.max(lastSavedSeconds, cached)
    detail.value.progressSeconds = lastKnownSeconds
    if (cached > lastSavedSeconds) void persistProgress(cached).catch(() => undefined)
  }
  finally { loading.value = false }
  window.addEventListener('pagehide', flushProgressKeepalive)
})

onBeforeUnmount(() => {
  flushProgressKeepalive()
  window.removeEventListener('pagehide', flushProgressKeepalive)
})
</script>

<template>
  <main class="detail-page">
    <van-nav-bar title="内容详情" left-arrow fixed placeholder @click-left="router.back()" />
    <van-loading v-if="loading" class="page-loading" vertical>加载中...</van-loading>
    <template v-else-if="detail">
      <section v-if="detail.type === 'VIDEO' && detail.file" class="video-wrap"><video ref="videoRef" :src="detail.file.url" :poster="detail.coverUrl" controls playsinline preload="metadata" @loadstart="beginMediaReload" @emptied="beginMediaReload" @loadedmetadata="showFirstFrame" @seeked="finishResume" @timeupdate="saveCurrentProgress()" @pause="saveCurrentProgress(true)" @ended="completePlayback" /></section>
      <section v-else-if="detail.coverUrl" class="detail-cover"><van-image :src="detail.coverUrl" width="100%" fit="cover" /></section>
      <section class="detail-heading">
        <span class="detail-type">{{ typeName }}</span>
        <h1>{{ detail.title }}</h1>
        <p v-if="detail.description">{{ detail.description }}</p>
      </section>
      <section v-if="detail.type === 'TEXT'" class="article-body" v-html="detail.body" />
      <section v-else-if="detail.type === 'DOC' && detail.file" class="document-panel">
        <van-icon name="description-o" size="42" />
        <div><strong>{{ detail.file.fileName }}</strong><span>{{ formatSize(detail.file.fileSize) }}</span></div>
        <van-button type="primary" round tag="a" :href="detail.file.downloadUrl || detail.file.url">打开文档</van-button>
      </section>
    </template>
  </main>
</template>
