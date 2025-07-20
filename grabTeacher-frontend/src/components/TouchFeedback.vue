<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

// 定义组件名称
defineOptions({
  name: 'TouchFeedback'
})

interface Props {
  disabled?: boolean
  haptic?: boolean // 是否启用触觉反馈
  ripple?: boolean // 是否显示波纹效果
  scale?: boolean // 是否启用缩放效果
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  haptic: true,
  ripple: true,
  scale: true
})

const emit = defineEmits<{
  click: [event: Event]
  touchstart: [event: TouchEvent]
  touchend: [event: TouchEvent]
}>()

const containerRef = ref<HTMLElement>()
const isPressed = ref(false)
const ripples = ref<Array<{ id: number; x: number; y: number; timestamp: number }>>([])

let rippleId = 0

// 触摸开始
const handleTouchStart = (event: TouchEvent) => {
  if (props.disabled) return

  isPressed.value = true
  
  // 触觉反馈
  if (props.haptic && 'vibrate' in navigator) {
    navigator.vibrate(10)
  }

  // 波纹效果
  if (props.ripple && containerRef.value) {
    const rect = containerRef.value.getBoundingClientRect()
    const touch = event.touches[0]
    const x = touch.clientX - rect.left
    const y = touch.clientY - rect.top
    
    ripples.value.push({
      id: rippleId++,
      x,
      y,
      timestamp: Date.now()
    })

    // 清理旧的波纹
    setTimeout(() => {
      ripples.value = ripples.value.filter(ripple => 
        Date.now() - ripple.timestamp < 600
      )
    }, 600)
  }

  emit('touchstart', event)
}

// 触摸结束
const handleTouchEnd = (event: TouchEvent) => {
  if (props.disabled) return

  isPressed.value = false
  emit('touchend', event)
}

// 点击事件
const handleClick = (event: Event) => {
  if (props.disabled) return
  emit('click', event)
}

// 清理波纹
const clearRipples = () => {
  ripples.value = []
}

onMounted(() => {
  // 防止长按选择文本
  if (containerRef.value) {
    containerRef.value.style.webkitUserSelect = 'none'
    containerRef.value.style.userSelect = 'none'
    containerRef.value.style.webkitTouchCallout = 'none'
  }
})

onUnmounted(() => {
  clearRipples()
})
</script>

<template>
  <div
    ref="containerRef"
    class="touch-feedback"
    :class="{
      'touch-feedback--pressed': isPressed && props.scale,
      'touch-feedback--disabled': props.disabled
    }"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
    @click="handleClick"
  >
    <!-- 波纹效果 -->
    <div v-if="props.ripple" class="ripple-container">
      <div
        v-for="ripple in ripples"
        :key="ripple.id"
        class="ripple"
        :style="{
          left: ripple.x + 'px',
          top: ripple.y + 'px'
        }"
      ></div>
    </div>

    <!-- 插槽内容 -->
    <slot></slot>
  </div>
</template>

<style scoped>
.touch-feedback {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.1s ease;
  -webkit-tap-highlight-color: transparent;
}

.touch-feedback--pressed {
  transform: scale(0.98);
}

.touch-feedback--disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.ripple-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
}

.ripple {
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.6);
  transform: translate(-50%, -50%) scale(0);
  animation: ripple-animation 0.6s ease-out;
  pointer-events: none;
}

@keyframes ripple-animation {
  0% {
    transform: translate(-50%, -50%) scale(0);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(4);
    opacity: 0;
  }
}

/* 深色主题下的波纹效果 */
@media (prefers-color-scheme: dark) {
  .ripple {
    background-color: rgba(255, 255, 255, 0.3);
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .touch-feedback--pressed {
    transform: scale(0.95);
  }
  
  .ripple {
    background-color: rgba(0, 0, 0, 0.5);
  }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
  .touch-feedback {
    transition: none;
  }
  
  .touch-feedback--pressed {
    transform: none;
  }
  
  .ripple {
    animation: none;
    opacity: 0;
  }
}
</style>
