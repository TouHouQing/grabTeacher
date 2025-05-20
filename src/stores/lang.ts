import { defineStore } from 'pinia'
import { ref } from 'vue'
import i18n from '../i18n'

export const useLangStore = defineStore('lang', () => {
  const currentLang = ref(i18n.global.locale.value)

  function toggleLang() {
    const newLang = currentLang.value === 'zh' ? 'en' : 'zh'
    currentLang.value = newLang
    i18n.global.locale.value = newLang
    // 可以选择将语言偏好保存到本地存储
    localStorage.setItem('language', newLang)
  }

  // 初始化时从本地存储加载语言偏好
  function initLang() {
    const savedLang = localStorage.getItem('language')
    if (savedLang && (savedLang === 'zh' || savedLang === 'en')) {
      currentLang.value = savedLang
      i18n.global.locale.value = savedLang
    }
  }

  return {
    currentLang,
    toggleLang,
    initLang
  }
})
