<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

// 定义组件名称
defineOptions({
  name: 'SwipeGesture'
})

interface Props {
  threshold?: number // 滑动阈值（像素）
  velocity?: number // 速度阈值（像素/毫秒）
  disabled?: boolean
  direction?: 'horizontal' | 'vertical' | 'all'
}

const props = withDefaults(defineProps<Props>(), {
  threshold: 50,
  velocity: 0.3,
  disabled: false,
  direction: 'all'
})

const emit = defineEmits<{
  swipeLeft: [event: { distance: number; velocity: number; duration: number }]
  swipeRight: [event: { distance: number; velocity: number; duration: number }]
  swipeUp: [event: { distance: number; velocity: number; duration: number }]
  swipeDown: [event: { distance: number; velocity: number; duration: number }]
  swipeStart: [event: TouchEvent]
  swipeMove: [event: { deltaX: number; deltaY: number; touch: Touch }]
  swipeEnd: [event: TouchEvent]
}>()

const containerRef = ref<HTMLElement>()
const isTracking = ref(false)

let startX = 0
let startY = 0
let startTime = 0
let currentX = 0
let currentY = 0

// 触摸开始
const handleTouchStart = (event: TouchEvent) => {
  if (props.disabled || event.touches.length !== 1) return

  const touch = event.touches[0]
  startX = touch.clientX
  startY = touch.clientY
  currentX = startX
  currentY = startY
  startTime = Date.now()
  isTracking.value = true

  emit('swipeStart', event)
}

// 触摸移动
const handleTouchMove = (event: TouchEvent) => {
  if (!isTracking.value || props.disabled || event.touches.length !== 1) return

  const touch = event.touches[0]
  currentX = touch.clientX
  currentY = touch.clientY

  const deltaX = currentX - startX
  const deltaY = currentY - startY

  emit('swipeMove', { deltaX, deltaY, touch })

  // 防止页面滚动（可选）
  if (props.direction === 'horizontal' && Math.abs(deltaX) > Math.abs(deltaY)) {
    event.preventDefault()
  } else if (props.direction === 'vertical' && Math.abs(deltaY) > Math.abs(deltaX)) {
    event.preventDefault()
  }
}

// 触摸结束
const handleTouchEnd = (event: TouchEvent) => {
  if (!isTracking.value || props.disabled) return

  const endTime = Date.now()
  const duration = endTime - startTime
  const deltaX = currentX - startX
  const deltaY = currentY - startY
  const distanceX = Math.abs(deltaX)
  const distanceY = Math.abs(deltaY)
  const velocityX = distanceX / duration
  const velocityY = distanceY / duration

  isTracking.value = false

  // 判断滑动方向和是否满足阈值
  const isHorizontalSwipe = distanceX > distanceY
  const isVerticalSwipe = distanceY > distanceX

  if (isHorizontalSwipe && (props.direction === 'horizontal' || props.direction === 'all')) {
    if (distanceX >= props.threshold || velocityX >= props.velocity) {
      if (deltaX > 0) {
        // 向右滑动
        emit('swipeRight', { distance: distanceX, velocity: velocityX, duration })
      } else {
        // 向左滑动
        emit('swipeLeft', { distance: distanceX, velocity: velocityX, duration })
      }
    }
  } else if (isVerticalSwipe && (props.direction === 'vertical' || props.direction === 'all')) {
    if (distanceY >= props.threshold || velocityY >= props.velocity) {
      if (deltaY > 0) {
        // 向下滑动
        emit('swipeDown', { distance: distanceY, velocity: velocityY, duration })
      } else {
        // 向上滑动
        emit('swipeUp', { distance: distanceY, velocity: velocityY, duration })
      }
    }
  }

  emit('swipeEnd', event)
}

// 触摸取消
const handleTouchCancel = () => {
  isTracking.value = false
}

onMounted(() => {
  if (containerRef.value) {
    const element = containerRef.value
    
    // 添加触摸事件监听器
    element.addEventListener('touchstart', handleTouchStart, { passive: false })
    element.addEventListener('touchmove', handleTouchMove, { passive: false })
    element.addEventListener('touchend', handleTouchEnd, { passive: true })
    element.addEventListener('touchcancel', handleTouchCancel, { passive: true })
  }
})

onUnmounted(() => {
  if (containerRef.value) {
    const element = containerRef.value
    
    // 移除触摸事件监听器
    element.removeEventListener('touchstart', handleTouchStart)
    element.removeEventListener('touchmove', handleTouchMove)
    element.removeEventListener('touchend', handleTouchEnd)
    element.removeEventListener('touchcancel', handleTouchCancel)
  }
})
</script>

<template>
  <div
    ref="containerRef"
    class="swipe-gesture"
    :class="{
      'swipe-gesture--tracking': isTracking,
      'swipe-gesture--disabled': disabled
    }"
  >
    <slot></slot>
  </div>
</template>

<style scoped>
.swipe-gesture {
  position: relative;
  width: 100%;
  height: 100%;
  touch-action: manipulation;
  user-select: none;
  -webkit-user-select: none;
  -webkit-touch-callout: none;
}

.swipe-gesture--tracking {
  /* 可以添加跟踪状态的样式 */
}

.swipe-gesture--disabled {
  pointer-events: none;
}

/* 防止文本选择 */
.swipe-gesture * {
  user-select: none;
  -webkit-user-select: none;
  -webkit-touch-callout: none;
}

/* 针对不同方向的优化 */
.swipe-gesture[data-direction="horizontal"] {
  touch-action: pan-y;
}

.swipe-gesture[data-direction="vertical"] {
  touch-action: pan-x;
}

.swipe-gesture[data-direction="all"] {
  touch-action: none;
}
</style>
