import { computed, ref } from 'vue'
import { categoryApi } from '../api/category'
import type { CategoryNode } from '../types'

export function flattenCategories(nodes: CategoryNode[], prefix = ''): Array<{ id: string; name: string; disabled: boolean }> {
  return nodes.flatMap((node) => [
    { id: node.id, name: `${prefix}${node.name}`, disabled: node.status === 0 },
    ...flattenCategories(node.children || [], `${prefix}　`),
  ])
}

export function useCategories() {
  const tree = ref<CategoryNode[]>([])
  const loading = ref(false)
  const options = computed(() => flattenCategories(tree.value))
  const nameMap = computed(() => new Map(options.value.map((item) => [item.id, item.name.trim()])))

  async function loadCategories() {
    loading.value = true
    try { tree.value = await categoryApi.tree() } finally { loading.value = false }
  }

  return { tree, options, nameMap, loading, loadCategories }
}
