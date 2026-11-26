import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('../layouts/AdminLayout.vue'),
      redirect: '/contents',
      children: [
        { path: 'categories', name: 'categories', component: () => import('../views/CategoryView.vue') },
        { path: 'contents', name: 'contents', component: () => import('../views/ContentListView.vue') },
        { path: 'contents/new', name: 'content-new', component: () => import('../views/ContentEditView.vue') },
        { path: 'contents/:id/edit', name: 'content-edit', component: () => import('../views/ContentEditView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.public && !getToken()) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && getToken()) return '/'
})

export default router
