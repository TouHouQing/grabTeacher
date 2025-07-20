<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

// 定义组件名称
defineOptions({
  name: 'OptimizedImage'
})

interface Props {
  src: string
  alt?: string
  lazy?: boolean
  placeholder?: string
  quality?: 'low' | 'medium' | 'high' | 'auto'
  sizes?: string
  webp?: boolean
  loading?: 'lazy' | 'eager'
  aspectRatio?: string
  objectFit?: 'cover' | 'contain' | 'fill' | 'scale-down' | 'none'
}

const props = withDefaults(defineProps<Props>(), {
  alt: '',
  lazy: true,
  placeholder: '',
  quality: 'auto',
  sizes: '100vw',
  webp: true,
  loading: 'lazy',
  aspectRatio: '',
  objectFit: 'cover'
})

const imageRef = ref<HTMLImageElement>()
const isLoaded = ref(false)
const isError = ref(false)
const isIntersecting = ref(false)

// 检测WebP支持
const supportsWebP = ref(false)

// 检测网络状况
const networkQuality = ref<'low' | 'medium' | 'high'>('high')

// 计算最终的图片质量
const finalQuality = computed(() => {
  if (props.quality === 'auto') {
    return networkQuality.value
  }
  return props.quality
})

// 生成优化后的图片URL
const optimizedSrc = computed(() => {
  let url = props.src
  
  // 如果支持WebP且启用了WebP
  if (supportsWebP.value && props.webp) {
    url = url.replace(/\.(jpg|jpeg|png)$/i, '.webp')
  }
  
  // 根据质量添加参数
  const qualityMap = {
    low: 30,
    medium: 60,
    high: 90
  }
  
  const quality = qualityMap[finalQuality.value]
  
  // 如果URL包含查询参数，添加质量参数
  if (url.includes('?')) {
    url += `&quality=${quality}`
  } else {
    url += `?quality=${quality}`
  }
  
  return url
})

// 生成srcset
const srcSet = computed(() => {
  const baseSrc = optimizedSrc.value
  const sizes = [1, 1.5, 2, 3] // 不同的像素密度
  
  return sizes.map(size => {
    const width = Math.round(300 * size) // 基础宽度300px
    let url = baseSrc
    
    if (url.includes('?')) {
      url += `&w=${width}`
    } else {
      url += `?w=${width}`
    }
    
    return `${url} ${size}x`
  }).join(', ')
})

// 容器样式
const containerStyle = computed(() => {
  const styles: Record<string, string> = {}
  
  if (props.aspectRatio) {
    styles.aspectRatio = props.aspectRatio
  }
  
  return styles
})

// 图片样式
const imageStyle = computed(() => {
  const styles: Record<string, string> = {
    objectFit: props.objectFit,
    transition: 'opacity 0.3s ease'
  }
  
  if (!isLoaded.value) {
    styles.opacity = '0'
  }
  
  return styles
})

// 检测WebP支持
const detectWebPSupport = () => {
  const canvas = document.createElement('canvas')
  canvas.width = 1
  canvas.height = 1
  const dataURL = canvas.toDataURL('image/webp')
  supportsWebP.value = dataURL.indexOf('data:image/webp') === 0
}

// 检测网络状况
const detectNetworkQuality = () => {
  const connection = (navigator as any).connection || 
                    (navigator as any).mozConnection || 
                    (navigator as any).webkitConnection
  
  if (connection) {
    const effectiveType = connection.effectiveType
    
    switch (effectiveType) {
      case 'slow-2g':
      case '2g':
        networkQuality.value = 'low'
        break
      case '3g':
        networkQuality.value = 'medium'
        break
      case '4g':
      default:
        networkQuality.value = 'high'
        break
    }
  }
}

// 图片加载成功
const handleLoad = () => {
  isLoaded.value = true
  isError.value = false
}

// 图片加载失败
const handleError = () => {
  isError.value = true
  isLoaded.value = false
}

// 设置Intersection Observer
const setupIntersectionObserver = () => {
  if (!props.lazy || !('IntersectionObserver' in window)) {
    isIntersecting.value = true
    return
  }
  
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          isIntersecting.value = true
          observer.disconnect()
        }
      })
    },
    {
      rootMargin: '50px 0px',
      threshold: 0.01
    }
  )
  
  if (imageRef.value) {
    observer.observe(imageRef.value)
  }
  
  return observer
}

onMounted(() => {
  detectWebPSupport()
  detectNetworkQuality()
  
  const observer = setupIntersectionObserver()
  
  onUnmounted(() => {
    observer?.disconnect()
  })
})
</script>

<template>
  <div class="optimized-image" :style="containerStyle">
    <!-- 占位符 -->
    <div
      v-if="!isLoaded && placeholder"
      class="image-placeholder"
      :style="{ backgroundImage: `url(${placeholder})` }"
    ></div>
    
    <!-- 加载状态 -->
    <div
      v-if="!isLoaded && !isError && isIntersecting"
      class="image-loading"
    >
      <div class="loading-spinner"></div>
    </div>
    
    <!-- 错误状态 -->
    <div
      v-if="isError"
      class="image-error"
    >
      <div class="error-icon">📷</div>
      <div class="error-text">图片加载失败</div>
    </div>
    
    <!-- 实际图片 -->
    <img
      v-if="isIntersecting"
      ref="imageRef"
      :src="optimizedSrc"
      :srcset="srcSet"
      :sizes="sizes"
      :alt="alt"
      :loading="loading"
      :style="imageStyle"
      class="optimized-image__img"
      @load="handleLoad"
      @error="handleError"
    />
  </div>
</template>

<style scoped>
.optimized-image {
  position: relative;
  display: block;
  width: 100%;
  overflow: hidden;
  background-color: #f5f5f5;
}

.optimized-image__img {
  width: 100%;
  height: 100%;
  display: block;
}

.image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  filter: blur(5px);
  transform: scale(1.1);
}

.image-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #e1e5e9;
  border-top: 2px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.image-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  text-align: center;
}

.error-icon {
  font-size: 32px;
  margin-bottom: 8px;
  opacity: 0.5;
}

.error-text {
  font-size: 12px;
  color: #666;
}

/* 深色主题 */
@media (prefers-color-scheme: dark) {
  .optimized-image {
    background-color: #2a2a2a;
  }
  
  .loading-spinner {
    border-color: #4a4a4a;
    border-top-color: #409eff;
  }
  
  .error-text {
    color: #ccc;
  }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
  .optimized-image__img {
    transition: none;
  }
  
  .loading-spinner {
    animation: none;
  }
}
</style>
