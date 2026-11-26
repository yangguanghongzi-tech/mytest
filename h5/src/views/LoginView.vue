<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { loginUrl, setToken } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const isDev = import.meta.env.DEV
const username = ref('user')
const password = ref('User@123')
const defaultUser = import.meta.env.VITE_DEV_USER || 'user'
const defaultPassword = import.meta.env.VITE_DEV_PASSWORD || 'User@123'

function localLogin() {
  if (username.value.trim() !== defaultUser || password.value !== defaultPassword) {
    return showFailToast('账号或密码错误')
  }
  setToken('dev:t1:u1')
  router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
}

function unifiedLogin() {
  window.location.assign(loginUrl())
}
</script>

<template>
  <main class="login-page">
    <section class="login-hero">
      <div class="logo">学</div>
      <p class="eyebrow">LEARNING HUB</p>
      <h1>每一次学习，<br />都让成长发生。</h1>
      <p class="hero-copy">精选课程与资料，随时随地开始学习。</p>
    </section>
    <section class="login-panel">
      <h2>欢迎回来</h2>
      <p>登录后查看你的学习内容</p>
      <van-button type="primary" block round size="large" @click="unifiedLogin">统一账号登录</van-button>
      <template v-if="isDev">
        <div class="divider"><span>本地开发</span></div>
        <van-cell-group inset>
          <van-field v-model="username" label="账号" placeholder="请输入账号" autocomplete="username" />
          <van-field v-model="password" label="密码" type="password" placeholder="请输入密码" autocomplete="current-password" @keyup.enter="localLogin" />
        </van-cell-group>
        <van-button class="dev-button" block round size="large" @click="localLogin">登录</van-button>
        <div class="credential">默认账号 user　密码 User@123</div>
      </template>
    </section>
  </main>
</template>
