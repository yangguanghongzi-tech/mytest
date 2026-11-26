<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { categoryApi } from '../api/category'
import { useCategories } from '../composables/useCategories'
import type { CategoryNode, CategorySave } from '../types'

const { tree, loading, loadCategories } = useCategories()
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<CategorySave>({ parentId: null, name: '', sort: 0, status: 1 })
const rules: FormRules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }, { max: 50, message: '最多 50 个字', trigger: 'blur' }] }

function openCreate(parent?: CategoryNode) {
  editingId.value = null
  Object.assign(form, { parentId: parent?.id ?? null, name: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

function isRoot(row: CategoryNode) {
  return row.parentId == null || row.parentId === '0'
}

function openEdit(row: CategoryNode) {
  editingId.value = row.id
  Object.assign(form, { parentId: isRoot(row) ? null : row.parentId, name: row.name, sort: row.sort, status: row.status })
  dialogVisible.value = true
}

async function save() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) await categoryApi.update(editingId.value, { ...form })
    else await categoryApi.create({ ...form })
    ElMessage.success(editingId.value ? '分类已更新' : '分类已新增')
    dialogVisible.value = false
    await loadCategories()
  } finally { saving.value = false }
}

async function toggleStatus(row: CategoryNode, value: string | number | boolean) {
  const next = Number(value)
  try {
    await categoryApi.update(row.id, { parentId: isRoot(row) ? null : row.parentId, name: row.name, sort: row.sort, status: next })
    row.status = next
    ElMessage.success(next ? '已启用' : '已禁用')
  } catch { /* interceptor has shown the error */ }
}

async function remove(row: CategoryNode) {
  await ElMessageBox.confirm(`确定删除分类“${row.name}”吗？`, '删除分类', { type: 'warning' })
  await categoryApi.remove(row.id)
  ElMessage.success('分类已删除')
  await loadCategories()
}

onMounted(loadCategories)
</script>

<template>
  <div class="page-stack">
    <header class="page-header"><div><h1>分类管理</h1><p>维护两级内容分类、展示顺序和可用状态</p></div><el-button type="primary" :icon="Plus" @click="openCreate()">新增一级分类</el-button></header>
    <el-card shadow="never" class="content-card">
      <el-table v-loading="loading" :data="tree" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="name" label="分类名称" min-width="260" />
        <el-table-column prop="sort" label="排序" width="100" align="center" />
        <el-table-column label="状态" width="130" align="center">
          <template #default="{ row }"><el-switch :model-value="row.status" :active-value="1" :inactive-value="0" inline-prompt active-text="启用" inactive-text="禁用" @change="toggleStatus(row, $event)" /></template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="right">
          <template #default="{ row }">
            <el-button v-if="isRoot(row)" link type="primary" @click="openCreate(row)">新增子分类</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="84px">
        <el-form-item label="上级分类"><span>{{ form.parentId ? tree.find(i => i.id === form.parentId)?.name : '一级分类' }}</span></el-form-item>
        <el-form-item label="分类名称" prop="name"><el-input v-model="form.name" maxlength="50" show-word-limit /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="9999" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">禁用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
