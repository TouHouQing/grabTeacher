import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import imageLoader from './utils/imageLoader'
import i18n from './i18n'
import { useUserStore } from './stores/user'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 禁用开发工具按钮
// app.config.devtools = false

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(imageLoader)
app.use(i18n)

// 初始化认证状态
const userStore = useUserStore()
userStore.initializeAuth()

app.mount('#app')
