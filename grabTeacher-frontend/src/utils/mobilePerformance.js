/**
 * 移动端性能优化工具
 * 包括图片懒加载、动画优化、内存管理等
 */

class MobilePerformanceOptimizer {
  constructor() {
    this.observers = new Map()
    this.animationFrameId = null
    this.isLowEndDevice = this.detectLowEndDevice()
    this.init()
  }

  /**
   * 初始化性能优化
   */
  init() {
    this.setupImageLazyLoading()
    this.setupAnimationOptimization()
    this.setupMemoryManagement()
    this.setupNetworkOptimization()
  }

  /**
   * 检测低端设备
   */
  detectLowEndDevice() {
    // 检测设备内存
    const memory = navigator.deviceMemory || 4
    
    // 检测CPU核心数
    const cores = navigator.hardwareConcurrency || 4
    
    // 检测连接速度
    const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
    const effectiveType = connection?.effectiveType || '4g'
    
    // 低端设备判断条件
    return memory <= 2 || cores <= 2 || effectiveType === 'slow-2g' || effectiveType === '2g'
  }

  /**
   * 设置图片懒加载
   */
  setupImageLazyLoading() {
    if ('IntersectionObserver' in window) {
      const imageObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const img = entry.target
            this.loadImage(img)
            imageObserver.unobserve(img)
          }
        })
      }, {
        rootMargin: '50px 0px',
        threshold: 0.01
      })

      this.observers.set('images', imageObserver)
      this.observeImages()
    }
  }

  /**
   * 观察图片元素
   */
  observeImages() {
    const images = document.querySelectorAll('img[data-src]')
    const observer = this.observers.get('images')
    
    if (observer) {
      images.forEach(img => observer.observe(img))
    }
  }

  /**
   * 加载图片
   */
  loadImage(img) {
    return new Promise((resolve, reject) => {
      const src = img.dataset.src
      if (!src) {
        reject(new Error('No data-src attribute'))
        return
      }

      // 创建新的图片对象
      const newImg = new Image()
      
      newImg.onload = () => {
        // 渐进式加载效果
        img.style.opacity = '0'
        img.src = src
        img.removeAttribute('data-src')
        
        // 淡入动画
        requestAnimationFrame(() => {
          img.style.transition = 'opacity 0.3s ease'
          img.style.opacity = '1'
        })
        
        resolve(img)
      }
      
      newImg.onerror = reject
      newImg.src = src
    })
  }

  /**
   * 设置动画优化
   */
  setupAnimationOptimization() {
    // 低端设备减少动画
    if (this.isLowEndDevice) {
      document.documentElement.classList.add('low-end-device')
      
      // 添加CSS规则减少动画
      const style = document.createElement('style')
      style.textContent = `
        .low-end-device * {
          animation-duration: 0.1s !important;
          animation-delay: 0s !important;
          transition-duration: 0.1s !important;
          transition-delay: 0s !important;
        }
        .low-end-device .complex-animation {
          display: none !important;
        }
      `
      document.head.appendChild(style)
    }

    // 页面可见性变化时暂停动画
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.pauseAnimations()
      } else {
        this.resumeAnimations()
      }
    })
  }

  /**
   * 暂停动画
   */
  pauseAnimations() {
    document.documentElement.style.animationPlayState = 'paused'
  }

  /**
   * 恢复动画
   */
  resumeAnimations() {
    document.documentElement.style.animationPlayState = 'running'
  }

  /**
   * 设置内存管理
   */
  setupMemoryManagement() {
    // 定期清理未使用的资源
    setInterval(() => {
      this.cleanupUnusedResources()
    }, 30000) // 每30秒清理一次

    // 监听内存压力
    if ('memory' in performance) {
      setInterval(() => {
        const memInfo = performance.memory
        const usedRatio = memInfo.usedJSHeapSize / memInfo.jsHeapSizeLimit
        
        if (usedRatio > 0.8) {
          console.warn('High memory usage detected:', usedRatio)
          this.forceGarbageCollection()
        }
      }, 10000)
    }
  }

  /**
   * 清理未使用的资源
   */
  cleanupUnusedResources() {
    // 清理已经离开视口的图片
    const images = document.querySelectorAll('img')
    images.forEach(img => {
      if (!this.isElementInViewport(img) && img.src && img.src.startsWith('blob:')) {
        URL.revokeObjectURL(img.src)
      }
    })

    // 清理缓存
    if ('caches' in window) {
      caches.keys().then(names => {
        names.forEach(name => {
          if (name.includes('old-') || name.includes('temp-')) {
            caches.delete(name)
          }
        })
      })
    }
  }

  /**
   * 检查元素是否在视口中
   */
  isElementInViewport(element) {
    const rect = element.getBoundingClientRect()
    return (
      rect.top >= 0 &&
      rect.left >= 0 &&
      rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
      rect.right <= (window.innerWidth || document.documentElement.clientWidth)
    )
  }

  /**
   * 强制垃圾回收（仅在支持的浏览器中）
   */
  forceGarbageCollection() {
    if (window.gc) {
      window.gc()
    }
  }

  /**
   * 设置网络优化
   */
  setupNetworkOptimization() {
    // 预加载关键资源
    this.preloadCriticalResources()
    
    // 根据网络状况调整资源质量
    this.adaptToNetworkConditions()
  }

  /**
   * 预加载关键资源
   */
  preloadCriticalResources() {
    const criticalResources = [
      '/api/user/profile',
      '/api/courses/featured'
    ]

    criticalResources.forEach(url => {
      const link = document.createElement('link')
      link.rel = 'prefetch'
      link.href = url
      document.head.appendChild(link)
    })
  }

  /**
   * 根据网络状况调整
   */
  adaptToNetworkConditions() {
    const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
    
    if (connection) {
      const updateImageQuality = () => {
        const effectiveType = connection.effectiveType
        let quality = 'high'
        
        if (effectiveType === 'slow-2g' || effectiveType === '2g') {
          quality = 'low'
        } else if (effectiveType === '3g') {
          quality = 'medium'
        }
        
        document.documentElement.setAttribute('data-image-quality', quality)
      }
      
      updateImageQuality()
      connection.addEventListener('change', updateImageQuality)
    }
  }

  /**
   * 优化滚动性能
   */
  optimizeScrollPerformance() {
    let ticking = false
    
    const updateScrollPosition = () => {
      // 执行滚动相关的操作
      this.handleScroll()
      ticking = false
    }
    
    const onScroll = () => {
      if (!ticking) {
        requestAnimationFrame(updateScrollPosition)
        ticking = true
      }
    }
    
    window.addEventListener('scroll', onScroll, { passive: true })
  }

  /**
   * 处理滚动事件
   */
  handleScroll() {
    // 更新懒加载图片
    this.observeImages()
    
    // 虚拟滚动优化（如果需要）
    this.updateVirtualScroll()
  }

  /**
   * 更新虚拟滚动
   */
  updateVirtualScroll() {
    const containers = document.querySelectorAll('[data-virtual-scroll]')
    containers.forEach(container => {
      const items = container.children
      const containerHeight = container.clientHeight
      const scrollTop = container.scrollTop
      
      Array.from(items).forEach((item, index) => {
        const itemTop = index * 100 // 假设每个项目高度为100px
        const itemBottom = itemTop + 100
        
        if (itemBottom < scrollTop || itemTop > scrollTop + containerHeight) {
          item.style.display = 'none'
        } else {
          item.style.display = 'block'
        }
      })
    })
  }

  /**
   * 销毁优化器
   */
  destroy() {
    // 清理观察器
    this.observers.forEach(observer => observer.disconnect())
    this.observers.clear()
    
    // 取消动画帧
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId)
    }
  }
}

// 创建全局实例
const mobilePerformanceOptimizer = new MobilePerformanceOptimizer()

// 页面加载完成后初始化
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    mobilePerformanceOptimizer.optimizeScrollPerformance()
  })
} else {
  mobilePerformanceOptimizer.optimizeScrollPerformance()
}

export default mobilePerformanceOptimizer
