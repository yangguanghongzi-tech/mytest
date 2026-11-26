<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { contentApi } from '../api/content'
import { useCategories } from '../composables/useCategories'
import ChunkUploader from '../components/ChunkUploader.vue'
import type { ContentSave, ContentType } from '../types'

const route = useRoute()
const router = useRouter()
const editingId = computed(() => typeof route.params.id === 'string' && route.params.id ? route.params.id : null)
const isEdit = computed(() => editingId.value !== null)
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const editorRef = shallowRef<IDomEditor>()
const { options: categories, loadCategories } = useCategories()
const existingFileName = ref('')
const existingCoverUrl = ref('')
const originalCoverUrl = ref('')
const coverUploaderVisible = ref(false)
const form = reactive<ContentSave>({
  categoryId: null, type: 'VIDEO', title: '', description: '', sort: 0,
  coverTaskId: null, removeCover: false, fileTaskId: null, duration: null, body: '',
})
const rules: FormRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }, { max: 100, message: '最多 100 个字', trigger: 'blur' }],
  description: [{ max: 500, message: '最多 500 个字', trigger: 'blur' }],
}
const toolbarConfig: Partial<IToolbarConfig> = { excludeKeys: ['group-video', 'insertVideo', 'uploadVideo', 'fullScreen'] }
const editorConfig: Partial<IEditorConfig> = { placeholder: '请输入正文内容…', MENU_CONF: {} }

function handleCreated(editor: IDomEditor) { editorRef.value = editor }
function fileComplete(taskId: string) { form.fileTaskId = taskId }
function coverComplete(taskId: string) {
  form.coverTaskId = taskId
  form.removeCover = false
  existingCoverUrl.value = ''
}

function replaceCover() {
  form.removeCover = false
  form.coverTaskId = null
  coverUploaderVisible.value = true
}

async function removeCover() {
  await ElMessageBox.confirm('保存内容后将删除当前封面，确定继续吗？', '删除封面', { type: 'warning' })
  form.coverTaskId = null
  form.removeCover = true
  existingCoverUrl.value = ''
  coverUploaderVisible.value = false
}

function clearCoverUpload() {
  form.coverTaskId = null
  form.removeCover = false
  existingCoverUrl.value = originalCoverUrl.value
  coverUploaderVisible.value = false
}

async function loadDetail() {
  if (!editingId.value) return
  loading.value = true
  try {
    const detail = await contentApi.detail(editingId.value)
    Object.assign(form, {
      categoryId: detail.categoryId,
      type: detail.type,
      title: detail.title,
      description: detail.description || '',
      sort: detail.sort,
      coverTaskId: null,
      removeCover: false,
      fileTaskId: null,
      duration: detail.duration ?? detail.file?.duration ?? null,
      body: detail.body || '',
    })
    existingFileName.value = detail.file?.fileName || ''
    originalCoverUrl.value = detail.coverUrl || ''
    existingCoverUrl.value = originalCoverUrl.value
    coverUploaderVisible.value = false
  } finally { loading.value = false }
}

async function save() {
  await formRef.value?.validate()
  if (!isEdit.value && form.type !== 'TEXT' && !form.fileTaskId) return ElMessage.warning(`请先上传${form.type === 'VIDEO' ? '视频' : '文档'}`)
  const plainText = editorRef.value?.getText().trim() || ''
  if (form.type === 'TEXT' && !plainText) return ElMessage.warning('请输入正文内容')
  saving.value = true
  try {
    const payload: ContentSave = {
      ...form,
      body: form.type === 'TEXT' ? form.body : null,
      duration: form.type === 'VIDEO' ? form.duration : null,
      fileTaskId: form.type === 'TEXT' ? null : form.fileTaskId,
    }
    if (editingId.value) await contentApi.update(editingId.value, payload)
    else await contentApi.create(payload)
    ElMessage.success(isEdit.value ? '内容已保存' : '内容已创建')
    router.push('/contents')
  } finally { saving.value = false }
}

function changeType(next: string | number | boolean | undefined) {
  if (isEdit.value || !next) return
  form.type = next as ContentType
  form.fileTaskId = null
  form.duration = null
  existingFileName.value = ''
}

async function cancel() {
  if (form.title || form.fileTaskId || form.coverTaskId || form.removeCover || form.body) {
    await ElMessageBox.confirm('尚未保存的内容将丢失，确定返回吗？', '确认返回', { type: 'warning' })
  }
  router.push('/contents')
}

onMounted(async () => { await Promise.all([loadCategories(), loadDetail()]) })
onBeforeUnmount(() => editorRef.value?.destroy())
</script>

<template>
  <div v-loading="loading" class="page-stack edit-page">
    <header class="page-header compact">
      <div class="title-with-back"><el-button circle :icon="ArrowLeft" @click="cancel" /><div><h1>{{ isEdit ? '编辑内容' : '新建内容' }}</h1><p>{{ isEdit ? '修改内容信息与素材' : '创建一条新的学习内容' }}</p></div></div>
      <div><el-button @click="cancel">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存内容</el-button></div>
    </header>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-card shadow="never" class="form-card">
        <template #header><div class="card-title"><strong>基本信息</strong><span>带 * 为必填项</span></div></template>
        <el-row :gutter="24">
          <el-col :span="12"><el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入内容标题" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="所属分类" prop="categoryId"><el-select v-model="form.categoryId" filterable placeholder="请选择分类" style="width:100%"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" :disabled="item.disabled" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="简介" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="简要介绍内容，便于学习者快速了解" /></el-form-item>
      </el-card>

      <el-card shadow="never" class="form-card">
        <template #header><div class="card-title"><strong>内容类型与素材</strong><span>创建后不可修改内容类型</span></div></template>
        <el-form-item label="内容类型" required>
          <el-segmented :model-value="form.type" :disabled="isEdit" :options="[{ label: '视频', value: 'VIDEO' }, { label: '文档', value: 'DOC' }, { label: '图文', value: 'TEXT' }]" @change="changeType" />
        </el-form-item>
        <template v-if="form.type === 'VIDEO'">
          <el-form-item label="视频文件" required><ChunkUploader purpose="VIDEO" accept=".mp4,video/mp4" label="选择 MP4 视频" :existing-name="existingFileName" @complete="fileComplete" @duration="form.duration = $event" @clear="form.fileTaskId = null; existingFileName = ''" /></el-form-item>
          <el-form-item label="视频时长（秒）"><el-input-number v-model="form.duration" :min="0" :max="864000" /><span class="field-help">选择视频时由浏览器自动读取，可手动修正</span></el-form-item>
        </template>
        <el-form-item v-else-if="form.type === 'DOC'" label="文档文件" required><ChunkUploader purpose="DOC" accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx" label="选择文档" :existing-name="existingFileName" @complete="fileComplete" @clear="form.fileTaskId = null; existingFileName = ''" /></el-form-item>
        <el-form-item v-else label="正文" required>
          <div class="editor-wrap"><Toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" class="editor-toolbar" /><Editor v-model="form.body" :default-config="editorConfig" mode="default" class="editor-content" @on-created="handleCreated" /></div>
        </el-form-item>
      </el-card>

      <el-card shadow="never" class="form-card">
        <template #header><div class="card-title"><strong>展示封面</strong><span>支持 JPG、PNG、WebP，最大 5MB</span></div></template>
        <div v-if="existingCoverUrl && !coverUploaderVisible && !form.coverTaskId" class="existing-cover">
          <el-image :src="existingCoverUrl" fit="cover" />
          <div class="cover-actions"><el-button link type="primary" @click="replaceCover">更换封面</el-button><el-button link type="danger" @click="removeCover">删除封面</el-button></div>
        </div>
        <template v-else>
          <el-alert v-if="form.removeCover && !form.coverTaskId" title="当前封面将在保存后删除，也可以重新上传新封面" type="warning" show-icon :closable="false" />
          <ChunkUploader purpose="COVER" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" label="选择封面图片" @complete="coverComplete" @clear="clearCoverUpload" />
        </template>
      </el-card>
    </el-form>
  </div>
</template>
