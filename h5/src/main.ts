import { createApp } from 'vue'
import Vant from 'vant'
import 'vant/lib/index.css'
import './styles/index.css'
import App from './App.vue'
import router from './router'
import { captureTokenFromUrl } from './utils/auth'

captureTokenFromUrl()
createApp(App).use(router).use(Vant).mount('#app')
