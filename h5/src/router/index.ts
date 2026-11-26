import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/contents/:id', name: 'detail', component: () => import('../views/ContentDetailView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  if (!to.meta.public && !getToken()) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && getToken()) return { name: 'home' }
})

export default router
