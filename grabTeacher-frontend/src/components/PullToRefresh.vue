<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

// 定义组件名称
defineOptions({
  name: 'PullToRefresh'
})

interface Props {
  disabled?: boolean
  threshold?: number // 触发刷新的阈值
  maxDistance?: number // 最大下拉距离
  refreshing?: boolean // 是否正在刷新
  refreshText?: string
  pullingText?: string
  releasingText?: string
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  threshold: 60,
  maxDistance: 120,
  refreshing: false,
  refreshText: '正在刷新...',
  pullingText: '下拉刷新',
  releasingText: '释放刷新'
})

const emit = defineEmits<{
  refresh: []
}>()

const containerRef = ref<HTMLElement>()
const pullDistance = ref(0)
const isPulling = ref(false)
const canRefresh = ref(false)

let startY = 0
let isTracking = false
let scrollTop = 0

// 计算下拉状态
const pullStatus = computed(() => {
  if (props.refreshing) return 'refreshing'
  if (canRefresh.value) return 'releasing'
  if (isPulling.value) return 'pulling'
  return 'idle'
})

// 计算显示文本
const displayText = computed(() => {
  switch (pullStatus.value) {
    case 'refreshing':
      return props.refreshText
    case 'releasing':
      return props.releasingText
    case 'pulling':
      return props.pullingText
    default:
      return props.pullingText
  }
})

// 计算下拉进度
const pullProgress = computed(() => {
  return Math.min(pullDistance.value / props.threshold, 1)
})

// 计算变换样式
const pullTransform = computed(() => {
  const distance = Math.min(pullDistance.value, props.maxDistance)
  return `translateY(${distance}px)`
})

// 触摸开始
const handleTouchStart = (event: TouchEvent) => {
  if (props.disabled || props.refreshing) return

  const touch = event.touches[0]
  startY = touch.clientY
  scrollTop = containerRef.value?.scrollTop || 0

  // 只有在顶部时才允许下拉刷新
  if (scrollTop === 0) {
    isTracking = true
  }
}

// 触摸移动
const handleTouchMove = (event: TouchEvent) => {
  if (!isTracking || props.disabled || props.refreshing) return

  const touch = event.touches[0]
  const deltaY = touch.clientY - startY

  // 只处理向下拉的手势
  if (deltaY > 0) {
    event.preventDefault()

    // 计算下拉距离，添加阻尼效果
    const damping = 0.5
    const distance = deltaY * damping

    pullDistance.value = Math.min(distance, props.maxDistance)
    isPulling.value = pullDistance.value > 0
    canRefresh.value = pullDistance.value >= props.threshold

    // 触觉反馈
    if (canRefresh.value && 'vibrate' in navigator) {
      navigator.vibrate(10)
    }
  }
}

// 触摸结束
const handleTouchEnd = () => {
  if (!isTracking) return

  isTracking = false

  if (canRefresh.value && !props.refreshing) {
    // 触发刷新
    emit('refresh')
  } else {
    // 重置状态
    resetPull()
  }
}

// 重置下拉状态
const resetPull = () => {
  pullDistance.value = 0
  isPulling.value = false
  canRefresh.value = false
}

// 监听刷新状态变化
const handleRefreshingChange = () => {
  if (!props.refreshing) {
    resetPull()
  }
}

// 滚动事件处理
const handleScroll = () => {
  if (isTracking && (containerRef.value?.scrollTop || 0) > 0) {
    // 如果开始滚动，取消下拉
    isTracking = false
    resetPull()
  }
}

onMounted(() => {
  if (containerRef.value) {
    const element = containerRef.value

    element.addEventListener('touchstart', handleTouchStart, { passive: false })
    element.addEventListener('touchmove', handleTouchMove, { passive: false })
    element.addEventListener('touchend', handleTouchEnd, { passive: true })
    element.addEventListener('scroll', handleScroll, { passive: true })
  }
})

onUnmounted(() => {
  if (containerRef.value) {
    const element = containerRef.value

    element.removeEventListener('touchstart', handleTouchStart)
    element.removeEventListener('touchmove', handleTouchMove)
    element.removeEventListener('touchend', handleTouchEnd)
    element.removeEventListener('scroll', handleScroll)
  }
})

// 监听刷新状态
watch(() => props.refreshing, handleRefreshingChange)
</script>

<template>
  <div
    ref="containerRef"
    class="pull-to-refresh"
    :style="{ transform: pullTransform }"
  >
    <!-- 下拉指示器 -->
    <div
      class="pull-indicator"
      :class="`pull-indicator--${pullStatus}`"
      :style="{ opacity: isPulling || props.refreshing ? 1 : 0 }"
    >
      <div class="pull-icon">
        <div
          v-if="pullStatus === 'refreshing'"
          class="loading-spinner"
        ></div>
        <div
          v-else
          class="pull-arrow"
          :class="{ 'pull-arrow--flip': canRefresh }"
          :style="{ transform: `rotate(${pullProgress * 180}deg)` }"
        >
          ↓
        </div>
      </div>
      <div class="pull-text">{{ displayText }}</div>
      <div class="pull-progress">
        <div
          class="pull-progress-bar"
          :style="{ width: `${pullProgress * 100}%` }"
        ></div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="pull-content">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.pull-to-refresh {
  position: relative;
  width: 100%;
  height: 100%;
  overflow-y: auto;
  transition: transform 0.3s ease;
}

.pull-indicator {
  position: absolute;
  top: -60px;
  left: 0;
  right: 0;
  height: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
  transition: opacity 0.3s ease;
  z-index: 10;
}

.pull-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-bottom: 4px;
}

.pull-arrow {
  font-size: 16px;
  color: #409eff;
  transition: transform 0.3s ease;
}

.pull-arrow--flip {
  transform: rotate(180deg);
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e1e5e9;
  border-top: 2px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.pull-text {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.pull-progress {
  width: 40px;
  height: 2px;
  background-color: #e1e5e9;
  border-radius: 1px;
  overflow: hidden;
}

.pull-progress-bar {
  height: 100%;
  background-color: #409eff;
  transition: width 0.1s ease;
}

.pull-content {
  position: relative;
  z-index: 1;
}

/* 不同状态的样式 */
.pull-indicator--pulling {
  background-color: #f8f9fa;
}

.pull-indicator--releasing {
  background-color: #e8f4fd;
}

.pull-indicator--refreshing {
  background-color: #e8f4fd;
}

/* 深色主题 */
@media (prefers-color-scheme: dark) {
  .pull-indicator {
    background-color: #2a2a2a;
  }

  .pull-indicator--releasing,
  .pull-indicator--refreshing {
    background-color: #1a3a5c;
  }

  .pull-text {
    color: #ccc;
  }

  .loading-spinner {
    border-color: #4a4a4a;
    border-top-color: #409eff;
  }

  .pull-progress {
    background-color: #4a4a4a;
  }
}
</style>
