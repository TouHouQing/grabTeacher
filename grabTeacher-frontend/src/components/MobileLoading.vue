<script setup lang="ts">
import { computed } from 'vue'

// 定义组件名称
defineOptions({
  name: 'MobileLoading'
})

interface Props {
  loading?: boolean
  text?: string
  type?: 'spinner' | 'dots' | 'pulse' | 'skeleton'
  size?: 'small' | 'medium' | 'large'
  overlay?: boolean
  fullscreen?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  text: '加载中...',
  type: 'spinner',
  size: 'medium',
  overlay: false,
  fullscreen: false
})

const loadingClass = computed(() => [
  'mobile-loading',
  `mobile-loading--${props.type}`,
  `mobile-loading--${props.size}`,
  {
    'mobile-loading--overlay': props.overlay,
    'mobile-loading--fullscreen': props.fullscreen,
    'mobile-loading--visible': props.loading
  }
])
</script>

<template>
  <transition name="loading-fade">
    <div v-if="loading" :class="loadingClass">
      <div class="loading-content">
        <!-- 旋转加载器 -->
        <div v-if="type === 'spinner'" class="spinner">
          <div class="spinner-circle"></div>
        </div>

        <!-- 点状加载器 -->
        <div v-else-if="type === 'dots'" class="dots">
          <div class="dot"></div>
          <div class="dot"></div>
          <div class="dot"></div>
        </div>

        <!-- 脉冲加载器 -->
        <div v-else-if="type === 'pulse'" class="pulse">
          <div class="pulse-circle"></div>
        </div>

        <!-- 骨架屏 -->
        <div v-else-if="type === 'skeleton'" class="skeleton">
          <div class="skeleton-line skeleton-line--title"></div>
          <div class="skeleton-line skeleton-line--text"></div>
          <div class="skeleton-line skeleton-line--text"></div>
          <div class="skeleton-line skeleton-line--short"></div>
        </div>

        <!-- 加载文本 -->
        <div v-if="text && type !== 'skeleton'" class="loading-text">
          {{ text }}
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.mobile-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.mobile-loading--overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.9);
  z-index: 1000;
}

.mobile-loading--fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.95);
  z-index: 9999;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

/* 旋转加载器 */
.spinner {
  position: relative;
}

.spinner-circle {
  width: 32px;
  height: 32px;
  border: 3px solid #e1e5e9;
  border-top: 3px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.mobile-loading--small .spinner-circle {
  width: 24px;
  height: 24px;
  border-width: 2px;
}

.mobile-loading--large .spinner-circle {
  width: 48px;
  height: 48px;
  border-width: 4px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 点状加载器 */
.dots {
  display: flex;
  gap: 4px;
}

.dot {
  width: 8px;
  height: 8px;
  background-color: #409eff;
  border-radius: 50%;
  animation: dot-bounce 1.4s ease-in-out infinite both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
.dot:nth-child(3) { animation-delay: 0s; }

.mobile-loading--small .dot {
  width: 6px;
  height: 6px;
}

.mobile-loading--large .dot {
  width: 12px;
  height: 12px;
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 脉冲加载器 */
.pulse {
  position: relative;
}

.pulse-circle {
  width: 40px;
  height: 40px;
  background-color: #409eff;
  border-radius: 50%;
  animation: pulse-scale 1.5s ease-in-out infinite;
}

.mobile-loading--small .pulse-circle {
  width: 30px;
  height: 30px;
}

.mobile-loading--large .pulse-circle {
  width: 60px;
  height: 60px;
}

@keyframes pulse-scale {
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(1);
    opacity: 0;
  }
}

/* 骨架屏 */
.skeleton {
  width: 100%;
  max-width: 300px;
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 12px;
}

.skeleton-line--title {
  height: 20px;
  width: 60%;
}

.skeleton-line--text {
  width: 100%;
}

.skeleton-line--short {
  width: 40%;
  margin-bottom: 0;
}

@keyframes skeleton-loading {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* 加载文本 */
.loading-text {
  color: #666;
  font-size: 14px;
  text-align: center;
}

.mobile-loading--small .loading-text {
  font-size: 12px;
}

.mobile-loading--large .loading-text {
  font-size: 16px;
}

/* 过渡动画 */
.loading-fade-enter-active,
.loading-fade-leave-active {
  transition: opacity 0.3s ease;
}

.loading-fade-enter-from,
.loading-fade-leave-to {
  opacity: 0;
}

/* 深色主题 */
@media (prefers-color-scheme: dark) {
  .mobile-loading--overlay,
  .mobile-loading--fullscreen {
    background-color: rgba(0, 0, 0, 0.9);
  }

  .spinner-circle {
    border-color: #4a4a4a;
    border-top-color: #409eff;
  }

  .loading-text {
    color: #ccc;
  }

  .skeleton-line {
    background: linear-gradient(90deg, #2a2a2a 25%, #3a3a3a 50%, #2a2a2a 75%);
    background-size: 200% 100%;
  }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
  .spinner-circle,
  .dot,
  .pulse-circle,
  .skeleton-line {
    animation: none;
  }

  .loading-fade-enter-active,
  .loading-fade-leave-active {
    transition: none;
  }
}
</style>
