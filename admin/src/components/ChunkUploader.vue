<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Delete, UploadFilled, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import { uploadApi } from '../api/upload'
import type { UploadPurpose } from '../types'

const props = withDefaults(defineProps<{
  purpose: UploadPurpose
  accept: string
  label?: string
  existingName?: string
}>(), { label: '选择文件', existingName: '' })

const emit = defineEmits<{
  complete: [taskId: string, file: File]
  duration: [seconds: number]
  clear: []
}>()

type UploadState = 'idle' | 'uploading' | 'paused' | 'failed' | 'completed'
const fileInput = ref<HTMLInputElement>()
const file = ref<File | null>(null)
const taskId = ref<string | null>(null)
const partSize = ref(0)
const partCount = ref(0)
const uploaded = ref(new Set<number>())
const uploadedBytes = ref(0)
const state = ref<UploadState>('idle')
const errorMessage = ref('')
const controllers = new Set<AbortController>()
let runId = 0

const fileName = computed(() => file.value?.name || props.existingName)
const percent = computed(() => {
  if (state.value === 'completed') return 100
  if (!file.value?.size) return 0
  return Math.min(99, Math.round(uploadedBytes.value / file.value.size * 100))
})
const statusText = computed(() => ({
  idle: props.existingName ? '已上传' : '等待上传',
  uploading: `正在上传 ${uploaded.value.size}/${partCount.value} 个分片`,
  paused: '已暂停，可继续上传',
  failed: errorMessage.value || '上传失败',
  completed: '上传完成',
}[state.value]))

function chooseFile() { fileInput.value?.click() }

async function readVideoDuration(selected: File) {
  if (props.purpose !== 'VIDEO') return
  const url = URL.createObjectURL(selected)
  const video = document.createElement('video')
  video.preload = 'metadata'
  video.src = url
  try {
    const seconds = await new Promise<number>((resolve, reject) => {
      video.onloadedmetadata = () => resolve(Math.max(0, Math.round(video.duration)))
      video.onerror = () => reject(new Error('无法读取视频时长'))
    })
    emit('duration', seconds)
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '无法读取视频时长')
  } finally { URL.revokeObjectURL(url) }
}

async function selectFile(event: Event) {
  const selected = (event.target as HTMLInputElement).files?.[0]
  if (!selected) return
  reset(false)
  file.value = selected
  await readVideoDuration(selected)
  await startUpload()
}

function calculateUploadedBytes(parts: Set<number>) {
  if (!file.value) return 0
  let total = 0
  for (const number of parts) {
    const start = (number - 1) * partSize.value
    total += Math.max(0, Math.min(partSize.value, file.value.size - start))
  }
  return total
}

async function ensureTask() {
  if (!file.value) throw new Error('请先选择文件')
  if (!taskId.value) {
    const task = await uploadApi.init(props.purpose, file.value.name, file.value.size)
    taskId.value = task.taskId
    partSize.value = task.partSize
    partCount.value = task.partCount
    uploaded.value = new Set()
  } else {
    const progress = await uploadApi.progress(taskId.value)
    partSize.value = progress.partSize
    partCount.value = progress.partCount
    uploaded.value = new Set(progress.uploadedParts)
  }
  uploadedBytes.value = calculateUploadedBytes(uploaded.value)
}

async function uploadPart(partNumber: number, currentRun: number) {
  if (!file.value || !taskId.value || state.value !== 'uploading' || currentRun !== runId) return
  const [{ url }] = await uploadApi.partUrls(taskId.value, [partNumber])
  const start = (partNumber - 1) * partSize.value
  const chunk = file.value.slice(start, Math.min(start + partSize.value, file.value.size))
  const controller = new AbortController()
  controllers.add(controller)
  try {
    const response = await fetch(url, { method: 'PUT', body: chunk, signal: controller.signal })
    if (!response.ok) throw new Error(`分片 ${partNumber} 上传失败（${response.status}）`)
    uploaded.value = new Set([...uploaded.value, partNumber])
    uploadedBytes.value = calculateUploadedBytes(uploaded.value)
  } finally { controllers.delete(controller) }
}

async function runWorkers(pending: number[], currentRun: number) {
  let cursor = 0
  async function worker() {
    while (cursor < pending.length && state.value === 'uploading' && currentRun === runId) {
      const number = pending[cursor++]
      await uploadPart(number, currentRun)
    }
  }
  await Promise.all(Array.from({ length: Math.min(3, pending.length) }, worker))
}

async function startUpload() {
  if (!file.value || state.value === 'uploading') return
  const currentRun = ++runId
  state.value = 'uploading'
  errorMessage.value = ''
  try {
    await ensureTask()
    const pending = Array.from({ length: partCount.value }, (_, index) => index + 1).filter((n) => !uploaded.value.has(n))
    await runWorkers(pending, currentRun)
    if (currentRun !== runId || state.value !== 'uploading') return
    if (!taskId.value) throw new Error('上传任务不存在')
    await uploadApi.complete(taskId.value)
    state.value = 'completed'
    uploadedBytes.value = file.value.size
    emit('complete', taskId.value, file.value)
    ElMessage.success(`${file.value.name} 上传完成`)
  } catch (error) {
    if (currentRun !== runId || state.value === 'paused') return
    state.value = 'failed'
    errorMessage.value = error instanceof Error ? error.message : '上传失败，请重试'
  }
}

function pause() {
  if (state.value !== 'uploading') return
  state.value = 'paused'
  runId++
  controllers.forEach((controller) => controller.abort())
  controllers.clear()
}

function reset(notify = true) {
  runId++
  controllers.forEach((controller) => controller.abort())
  controllers.clear()
  file.value = null
  taskId.value = null
  partSize.value = 0
  partCount.value = 0
  uploaded.value = new Set()
  uploadedBytes.value = 0
  state.value = 'idle'
  errorMessage.value = ''
  if (fileInput.value) fileInput.value.value = ''
  if (notify) emit('clear')
}

onBeforeUnmount(() => {
  runId++
  controllers.forEach((controller) => controller.abort())
})
</script>

<template>
  <div class="chunk-uploader">
    <input ref="fileInput" class="visually-hidden" type="file" :accept="accept" @change="selectFile" />
    <button v-if="!fileName" type="button" class="upload-drop" @click="chooseFile">
      <el-icon :size="30"><UploadFilled /></el-icon><strong>{{ label }}</strong><span>选择文件后自动开始，支持暂停与断点续传</span>
    </button>
    <div v-else class="upload-file">
      <div class="file-icon"><el-icon><CircleCheck v-if="state === 'completed' || (!file && existingName)" /><UploadFilled v-else /></el-icon></div>
      <div class="file-info">
        <strong>{{ fileName }}</strong><span :class="{ danger: state === 'failed' }">{{ statusText }}</span>
        <el-progress v-if="file" :percentage="percent" :status="state === 'failed' ? 'exception' : state === 'completed' ? 'success' : undefined" />
      </div>
      <div class="file-actions">
        <el-button v-if="state === 'uploading'" circle :icon="VideoPause" title="暂停" @click="pause" />
        <el-button v-if="state === 'paused' || state === 'failed'" circle type="primary" :icon="VideoPlay" title="继续/重试" @click="startUpload" />
        <el-button circle :icon="Delete" title="移除" @click="reset()" />
      </div>
    </div>
    <p v-if="state === 'failed'" class="upload-error">{{ errorMessage }}，点击播放按钮从已完成分片继续。</p>
  </div>
</template>
