<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { contentApi, type ContentQuery } from '../api/content'
import { useCategories } from '../composables/useCategories'
import type { ContentListItem, ContentType } from '../types'

const router = useRouter()
const { options: categories, nameMap, loadCategories } = useCategories()
const loading = ref(false)
const records = ref<ContentListItem[]>([])
const total = ref(0)
const query = reactive<ContentQuery>({ page: 1, size: 20, keyword: undefined, categoryId: undefined, type: undefined, status: undefined })
const typeLabels: Record<ContentType, string> = { VIDEO: '视频', DOC: '文档', TEXT: '图文' }

async function load() {
  loading.value = true
  try {
    const result = await contentApi.page(query)
    records.value = result.records
    total.value = result.total
  } finally { loading.value = false }
}

function search() { query.page = 1; load() }
function resetFilters() { Object.assign(query, { page: 1, keyword: undefined, categoryId: undefined, type: undefined, status: undefined }); load() }

async function togglePublish(row: ContentListItem) {
  const published = row.status === 'PUBLISHED'
  await ElMessageBox.confirm(`确定${published ? '下架' : '发布'}“${row.title}”吗？`, published ? '下架内容' : '发布内容', { type: published ? 'warning' : 'info' })
  if (published) await contentApi.unpublish(row.id)
  else await contentApi.publish(row.id)
  ElMessage.success(published ? '内容已下架' : '内容已发布')
  await load()
}

async function remove(row: ContentListItem) {
  await ElMessageBox.confirm(`删除后不可恢复，确定删除“${row.title}”吗？`, '删除内容', { type: 'warning' })
  await contentApi.remove(row.id)
  ElMessage.success('内容已删除')
  await load()
}

onMounted(async () => { await Promise.all([loadCategories(), load()]) })
</script>

<template>
  <div class="page-stack">
    <header class="page-header"><div><h1>内容管理</h1><p>管理视频、文档和图文内容的完整生命周期</p></div><el-button type="primary" :icon="Plus" @click="router.push('/contents/new')">新建内容</el-button></header>
    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent="search">
        <el-form-item><el-input v-model="query.keyword" clearable placeholder="搜索标题或简介" :prefix-icon="Search" @keyup.enter="search" /></el-form-item>
        <el-form-item><el-select v-model="query.categoryId" clearable filterable placeholder="全部分类" style="width: 160px"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item><el-select v-model="query.type" clearable placeholder="全部类型" style="width: 130px"><el-option label="视频" value="VIDEO" /><el-option label="文档" value="DOC" /><el-option label="图文" value="TEXT" /></el-select></el-form-item>
        <el-form-item><el-select v-model="query.status" clearable placeholder="全部状态" style="width: 130px"><el-option label="已发布" value="PUBLISHED" /><el-option label="草稿/下架" value="DRAFT" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="resetFilters">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never" class="content-card">
      <el-table v-loading="loading" :data="records">
        <el-table-column label="内容" min-width="300">
          <template #default="{ row }"><div class="content-cell"><el-image v-if="row.coverUrl" :src="row.coverUrl" fit="cover" class="cover-thumb" /><div v-else class="cover-placeholder">{{ typeLabels[row.type as ContentType] }}</div><div><strong>{{ row.title }}</strong><p>{{ row.description || '暂无简介' }}</p></div></div></template>
        </el-table-column>
        <el-table-column label="分类" min-width="130"><template #default="{ row }">{{ nameMap.get(row.categoryId) || '-' }}</template></el-table-column>
        <el-table-column label="类型" width="90"><template #default="{ row }"><el-tag effect="plain">{{ typeLabels[row.type as ContentType] }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><span class="status-dot" :class="row.status === 'PUBLISHED' ? 'online' : 'draft'" />{{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}</template></el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right" align="right">
          <template #default="{ row }"><el-button link type="primary" @click="router.push(`/contents/${row.id}/edit`)">编辑</el-button><el-button link :type="row.status === 'PUBLISHED' ? 'warning' : 'success'" @click="togglePublish(row)">{{ row.status === 'PUBLISHED' ? '下架' : '发布' }}</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !records.length" description="暂无符合条件的内容" />
      <div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </el-card>
  </div>
</template>
