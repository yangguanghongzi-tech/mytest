<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, Document, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { loginUrl } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const activeMenu = computed(() => route.path.startsWith('/categories') ? '/categories' : '/contents')

function logout() {
  auth.logout()
  const target = loginUrl()
  if (target.startsWith('/')) router.replace('/login')
  else window.location.assign(target)
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="224px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">学</div>
        <div><strong>学习平台</strong><span>管理后台</span></div>
      </div>
      <el-menu :default-active="activeMenu" router class="side-menu">
        <el-menu-item index="/contents"><el-icon><Document /></el-icon><span>内容管理</span></el-menu-item>
        <el-menu-item index="/categories"><el-icon><Collection /></el-icon><span>分类管理</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div class="topbar-title">内容运营中心</div>
        <el-button text :icon="SwitchButton" @click="logout">退出登录</el-button>
      </el-header>
      <el-main class="page-main"><RouterView /></el-main>
    </el-container>
  </el-container>
</template>
