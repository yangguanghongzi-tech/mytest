<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { learningApi } from '../api/learning'
import ContentCard from '../components/ContentCard.vue'
import DocumentCard from '../components/DocumentCard.vue'
import ContinueWatchingCard from '../components/ContinueWatchingCard.vue'
import type { CategoryNode, ContentItem, ContinueWatching } from '../types'
import { clearToken } from '../utils/auth'

const router = useRouter()
const categories = ref<CategoryNode[]>([])
const selectedRoot = ref('all')
const selectedChild = ref('all')
const records = ref<ContentItem[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const requesting = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const continueWatching = ref<ContinueWatching | null>(null)
const rootOptions = computed(() => [{ id: 'all', name: '全部' }, ...categories.value])
const currentRoot = computed(() => categories.value.find((item) => item.id === selectedRoot.value))
const childOptions = computed(() => currentRoot.value?.children || [])
const categoryId = computed(() => selectedChild.value !== 'all' ? selectedChild.value : selectedRoot.value !== 'all' ? selectedRoot.value : undefined)
const documents = computed(() => records.value.filter((item) => item.type === 'DOC'))
const learningContents = computed(() => records.value.filter((item) => item.type !== 'DOC'))

async function load(reset = false) {
  if (requesting.value) return
  if (reset) {
    page.value = 1
    records.value = []
    finished.value = false
  }
  requesting.value = true
  loading.value = true
  try {
    const result = await learningApi.contents(categoryId.value, page.value)
    records.value.push(...result.records)
    total.value = result.total
    if (records.value.length >= result.total || result.records.length === 0) finished.value = true
    else page.value++
  } finally {
    requesting.value = false
    loading.value = false
    refreshing.value = false
  }
}

function chooseRoot(id: string) {
  if (selectedRoot.value === id) return
  selectedRoot.value = id
  selectedChild.value = 'all'
  load(true)
}

function chooseChild(id: string) {
  if (selectedChild.value === id) return
  selectedChild.value = id
  load(true)
}

async function refresh() {
  const [, recent] = await Promise.all([load(true), learningApi.continueWatching()])
  continueWatching.value = recent
}
async function logout() {
  await showConfirmDialog({ title: '退出登录', message: '确定退出当前账号吗？' })
  clearToken()
  router.replace('/login')
}

onMounted(async () => {
  const [categoryData, recent] = await Promise.all([
    learningApi.categories(),
    learningApi.continueWatching(),
  ])
  categories.value = categoryData
  continueWatching.value = recent
  await load(true)
})
</script>

<template>
  <main class="home-page">
    <section class="welcome">
      <h1>今天想学点什么？</h1>
      <span>发现适合你的课程与学习资料</span>
    </section>
    <section v-if="continueWatching" class="continue-section">
      <ContinueWatchingCard :item="continueWatching" @click="router.push(`/contents/${continueWatching.contentId}`)" />
    </section>
    <nav class="root-tabs">
      <button v-for="item in rootOptions" :key="item.id" :class="{ active: selectedRoot === item.id }" @click="chooseRoot(item.id)">{{ item.name }}</button>
    </nav>
    <nav v-if="childOptions.length" class="child-tabs">
      <button :class="{ active: selectedChild === 'all' }" @click="chooseChild('all')">全部</button>
      <button v-for="item in childOptions" :key="item.id" :class="{ active: selectedChild === item.id }" @click="chooseChild(item.id)">{{ item.name }}</button>
    </nav>
    <section class="content-section">
      <van-pull-refresh v-model="refreshing" @refresh="refresh">
        <van-list v-model:loading="loading" :finished="finished" :immediate-check="false" finished-text="没有更多内容了" @load="load()">
          <section v-if="documents.length" class="document-library">
            <div class="library-heading">
              <div><h2>资料文库</h2><span>支持在线查看与下载</span></div>
            </div>
            <div class="document-list">
              <DocumentCard v-for="item in documents" :key="item.id" :item="item" @click="router.push(`/contents/${item.id}`)" />
            </div>
          </section>
          <section v-if="learningContents.length" class="learning-content-list">
            <div class="content-grid">
              <ContentCard v-for="item in learningContents" :key="item.id" :item="item" @click="router.push(`/contents/${item.id}`)" />
            </div>
          </section>
          <van-empty v-if="!loading && !records.length" image="search" description="该分类暂时没有内容" />
        </van-list>
      </van-pull-refresh>
    </section>
  </main>
</template>
