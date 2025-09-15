import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
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

const pinia = createPinia()
app.use(pinia)
app.use(ElementPlus, { locale: zhCn })
app.use(imageLoader)
app.use(i18n)

// 启动前触发认证初始化（异步进行），避免刷新后被误判为未登录
const userStore = useUserStore(pinia)
userStore.initializeAuth().catch(() => {})

app.use(router)
app.mount('#app')
