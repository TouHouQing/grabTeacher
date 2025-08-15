<script setup lang="ts">
defineOptions({ name: 'ContactUs' })
import { ref, onMounted } from 'vue'
import { adminPublicAPI } from '../utils/api'
import wechatIcon from '../assets/icons/wechat.svg'
import whatsappIcon from '../assets/icons/whatsapp.svg'
import emailIcon from '../assets/icons/mail.svg'

const loading = ref(false)
const contacts = ref<{ realName?: string; wechatQrcodeUrl?: string; whatsappNumber?: string; email?: string }>({})

const loadContacts = async () => {
  try {
    loading.value = true
    const res = await adminPublicAPI.getContacts()
    const data = (res && res.data) ? res.data : res
    contacts.value = data || {}
  } catch (e) {
    console.error('获取管理员联系方式失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadContacts)
</script>

<template>
  <div class="contact-us">
    <h2 class="title">联系我们</h2>
    <div class="cards">
      <div class="card">
        <div class="icon"><img :src="whatsappIcon" alt="WhatsApp" class="icon-img" /></div>
        <div class="content">
          <div class="label">WhatsApp</div>
          <div class="value">{{ contacts.whatsappNumber || '-' }}</div>
        </div>
      </div>
      <div class="card">
        <div class="icon"><img :src="emailIcon" alt="Email" class="icon-img" /></div>
        <div class="content">
          <div class="label">电子邮箱</div>
          <div class="value">{{ contacts.email || '-' }}</div>
        </div>
      </div>
      <div class="card">
        <div class="icon"><img :src="wechatIcon" alt="微信" class="icon-img" /></div>
        <div class="content">
          <div class="value">
            <img v-if="contacts.wechatQrcodeUrl" :src="contacts.wechatQrcodeUrl" alt="微信二维码" class="qrcode" />
            <span v-else class="placeholder">暂无</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contact-us { margin-top: 40px; }
.title { text-align: center; font-size: 22px; color: #333; position: relative; margin-bottom: 20px; }
.title::after { content: ''; display:block; width: 40px; height:3px; background:#409EFF; margin:8px auto 0; border-radius:2px; }
.cards { display:grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.card { display:flex; gap:12px; align-items:center; padding:16px; background:#fff; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.05); }
.icon { width:36px; height:36px; display:flex; align-items:center; justify-content:center; font-size:18px; background:#ecf5ff; color:#409EFF; border-radius:8px; }
.icon-img { width:22px; height:22px; display:block; }
.label { font-size:14px; color:#333; margin-bottom:4px; }
.value { color:#666; font-size:14px; }
.qrcode { width:96px; height:96px; object-fit:contain; border:1px solid #eee; border-radius:6px; }
.placeholder { color:#bbb; }

@media (max-width: 768px) {
  .cards { grid-template-columns: 1fr; }
}
</style>

