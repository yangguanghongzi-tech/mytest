<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { loginUrl } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isDev = import.meta.env.DEV
const username = ref('admin')
const password = ref('Admin@123')
const defaultUsername = import.meta.env.VITE_DEV_ADMIN_USER || 'admin'
const defaultPassword = import.meta.env.VITE_DEV_ADMIN_PASSWORD || 'Admin@123'

function localLogin() {
  if (!username.value.trim() || !password.value) return ElMessage.warning('请输入账号和密码')
  if (username.value.trim() !== defaultUsername || password.value !== defaultPassword) {
    return ElMessage.error('账号或密码错误')
  }
  auth.login('dev:t1:u1:admin')
  router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
}
</script>

<template>
  <div class="login-page">
    <section class="login-intro">
      <div class="brand large"><div class="brand-mark">学</div><div><strong>学习平台</strong><span>让知识持续流动</span></div></div>
      <h1>把好内容，<br />交付给每一位学习者。</h1>
      <p>统一管理分类、视频、文档与图文内容，清晰掌握每一条内容的发布状态。</p>
    </section>
    <el-card class="login-card" shadow="never">
      <h2>登录管理后台</h2>
      <p class="muted">请使用统一账号站点完成身份验证</p>
      <el-button type="primary" size="large" class="wide" @click="window.location.assign(loginUrl())">前往统一登录</el-button>
      <template v-if="isDev">
        <el-divider>本地开发</el-divider>
        <el-form @submit.prevent="localLogin">
          <el-form-item><el-input v-model="username" size="large" autocomplete="username" placeholder="账号" /></el-form-item>
          <el-form-item><el-input v-model="password" type="password" size="large" autocomplete="current-password" show-password placeholder="密码" @keyup.enter="localLogin" /></el-form-item>
          <el-button type="primary" native-type="submit" size="large" class="wide dev-login">登录</el-button>
        </el-form>
        <p class="dev-credential">默认账号：<code>admin</code>　密码：<code>Admin@123</code></p>
      </template>
      <p class="login-tip">统一站点返回时，地址中的 token 会被安全保存到当前标签页会话。</p>
    </el-card>
  </div>
</template>
