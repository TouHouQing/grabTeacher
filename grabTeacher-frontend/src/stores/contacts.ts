import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminPublicAPI } from '../utils/api'

export interface ContactsInfo {
  realName?: string
  wechatQrcodeUrl?: string
  whatsappNumber?: string
  email?: string
}

export const useContactsStore = defineStore('contacts', () => {
  const contacts = ref<ContactsInfo>({})
  const updatedAt = ref<number>(0)
  const loading = ref(false)

  // 默认 TTL 10 分钟
  const defaultTtlMs = 10 * 60 * 1000

  async function fetchContacts(force = false, ttlMs = defaultTtlMs): Promise<ContactsInfo | null> {
    const now = Date.now()
    const notExpired = contacts.value && (now - updatedAt.value) < ttlMs

    if (!force && notExpired) {
      return contacts.value
    }

    try {
      loading.value = true
      const res = await adminPublicAPI.getContacts()
      const data = (res && (res as any).data) ? (res as any).data : res
      contacts.value = (data || {}) as ContactsInfo
      updatedAt.value = Date.now()
      return contacts.value
    } catch (err) {
      console.error('获取管理员联系方式失败', err)
      return contacts.value // 失败时返回旧缓存，避免空白
    } finally {
      loading.value = false
    }
  }

  return {
    contacts,
    updatedAt,
    loading,
    fetchContacts,
  }
})

