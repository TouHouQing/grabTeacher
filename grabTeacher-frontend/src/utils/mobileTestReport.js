/**
 * 移动端测试报告生成器
 * 生成详细的移动端兼容性和性能测试报告
 */

class MobileTestReportGenerator {
  constructor() {
    this.testResults = {
      device: {},
      performance: {},
      compatibility: {},
      accessibility: {},
      usability: {},
      timestamp: new Date().toISOString()
    }
  }

  /**
   * 运行完整的移动端测试
   */
  async runCompleteTest() {
    console.log('🔍 开始移动端完整测试...')
    
    try {
      await this.testDeviceInfo()
      await this.testPerformance()
      await this.testCompatibility()
      await this.testAccessibility()
      await this.testUsability()
      
      const report = this.generateReport()
      this.displayReport(report)
      
      return report
    } catch (error) {
      console.error('测试过程中发生错误:', error)
      return null
    }
  }

  /**
   * 测试设备信息
   */
  async testDeviceInfo() {
    const device = {
      userAgent: navigator.userAgent,
      platform: navigator.platform,
      language: navigator.language,
      cookieEnabled: navigator.cookieEnabled,
      onLine: navigator.onLine,
      deviceMemory: navigator.deviceMemory || 'unknown',
      hardwareConcurrency: navigator.hardwareConcurrency || 'unknown',
      maxTouchPoints: navigator.maxTouchPoints || 0,
      screen: {
        width: screen.width,
        height: screen.height,
        availWidth: screen.availWidth,
        availHeight: screen.availHeight,
        colorDepth: screen.colorDepth,
        pixelDepth: screen.pixelDepth,
        orientation: screen.orientation?.type || 'unknown'
      },
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight,
        devicePixelRatio: window.devicePixelRatio || 1
      },
      connection: this.getConnectionInfo()
    }

    this.testResults.device = device
  }

  /**
   * 获取网络连接信息
   */
  getConnectionInfo() {
    const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection
    
    if (connection) {
      return {
        effectiveType: connection.effectiveType,
        downlink: connection.downlink,
        rtt: connection.rtt,
        saveData: connection.saveData
      }
    }
    
    return { effectiveType: 'unknown' }
  }

  /**
   * 测试性能
   */
  async testPerformance() {
    const performance = {
      navigation: this.getNavigationTiming(),
      memory: this.getMemoryInfo(),
      fps: await this.measureFPS(),
      scrollPerformance: await this.testScrollPerformance(),
      touchLatency: await this.testTouchLatency()
    }

    this.testResults.performance = performance
  }

  /**
   * 获取导航时间
   */
  getNavigationTiming() {
    const timing = window.performance.timing
    const navigation = window.performance.navigation
    
    return {
      type: navigation.type,
      redirectCount: navigation.redirectCount,
      domainLookup: timing.domainLookupEnd - timing.domainLookupStart,
      connect: timing.connectEnd - timing.connectStart,
      request: timing.responseStart - timing.requestStart,
      response: timing.responseEnd - timing.responseStart,
      domProcessing: timing.domComplete - timing.domLoading,
      loadComplete: timing.loadEventEnd - timing.navigationStart
    }
  }

  /**
   * 获取内存信息
   */
  getMemoryInfo() {
    if (window.performance.memory) {
      const memory = window.performance.memory
      return {
        usedJSHeapSize: memory.usedJSHeapSize,
        totalJSHeapSize: memory.totalJSHeapSize,
        jsHeapSizeLimit: memory.jsHeapSizeLimit,
        usagePercentage: (memory.usedJSHeapSize / memory.jsHeapSizeLimit * 100).toFixed(2)
      }
    }
    return { available: false }
  }

  /**
   * 测量FPS
   */
  measureFPS() {
    return new Promise((resolve) => {
      let frames = 0
      const startTime = performance.now()
      
      const countFrame = () => {
        frames++
        if (performance.now() - startTime < 1000) {
          requestAnimationFrame(countFrame)
        } else {
          resolve(frames)
        }
      }
      
      requestAnimationFrame(countFrame)
    })
  }

  /**
   * 测试滚动性能
   */
  testScrollPerformance() {
    return new Promise((resolve) => {
      let frameCount = 0
      let startTime = performance.now()
      
      const measureScroll = () => {
        frameCount++
        if (performance.now() - startTime < 1000) {
          requestAnimationFrame(measureScroll)
        } else {
          resolve({
            fps: frameCount,
            smooth: frameCount >= 50 // 50fps以上认为流畅
          })
        }
      }
      
      // 模拟滚动
      window.scrollBy(0, 1)
      requestAnimationFrame(measureScroll)
    })
  }

  /**
   * 测试触摸延迟
   */
  testTouchLatency() {
    return new Promise((resolve) => {
      if (!('ontouchstart' in window)) {
        resolve({ supported: false })
        return
      }

      let touchStartTime = 0
      let touchEndTime = 0
      
      const testElement = document.createElement('div')
      testElement.style.cssText = `
        position: fixed;
        top: -100px;
        left: -100px;
        width: 50px;
        height: 50px;
        background: transparent;
        pointer-events: auto;
      `
      
      document.body.appendChild(testElement)
      
      testElement.addEventListener('touchstart', () => {
        touchStartTime = performance.now()
      })
      
      testElement.addEventListener('touchend', () => {
        touchEndTime = performance.now()
        const latency = touchEndTime - touchStartTime
        
        document.body.removeChild(testElement)
        resolve({
          supported: true,
          latency: latency,
          responsive: latency < 100 // 100ms以下认为响应良好
        })
      })
      
      // 模拟触摸事件
      setTimeout(() => {
        const touchEvent = new TouchEvent('touchstart', {
          touches: [new Touch({
            identifier: 0,
            target: testElement,
            clientX: 0,
            clientY: 0
          })]
        })
        testElement.dispatchEvent(touchEvent)
        
        setTimeout(() => {
          const touchEndEvent = new TouchEvent('touchend', { touches: [] })
          testElement.dispatchEvent(touchEndEvent)
        }, 50)
      }, 100)
    })
  }

  /**
   * 测试兼容性
   */
  async testCompatibility() {
    const compatibility = {
      css: this.testCSSFeatures(),
      javascript: this.testJSFeatures(),
      apis: this.testWebAPIs(),
      viewport: this.testViewportMeta(),
      touch: this.testTouchSupport()
    }

    this.testResults.compatibility = compatibility
  }

  /**
   * 测试CSS特性
   */
  testCSSFeatures() {
    const features = {
      flexbox: CSS.supports('display', 'flex'),
      grid: CSS.supports('display', 'grid'),
      transforms: CSS.supports('transform', 'translateX(1px)'),
      transitions: CSS.supports('transition', 'all 1s'),
      animations: CSS.supports('animation', 'test 1s'),
      customProperties: CSS.supports('--test', '1'),
      calc: CSS.supports('width', 'calc(100% - 1px)'),
      viewport: CSS.supports('width', '100vw')
    }

    return {
      features,
      supportedCount: Object.values(features).filter(Boolean).length,
      totalCount: Object.keys(features).length
    }
  }

  /**
   * 测试JavaScript特性
   */
  testJSFeatures() {
    const features = {
      es6Classes: typeof class {} === 'function',
      arrowFunctions: (() => true)(),
      promises: typeof Promise !== 'undefined',
      asyncAwait: (async () => true)() instanceof Promise,
      modules: typeof import !== 'undefined',
      intersectionObserver: 'IntersectionObserver' in window,
      serviceWorker: 'serviceWorker' in navigator,
      webWorkers: typeof Worker !== 'undefined'
    }

    return {
      features,
      supportedCount: Object.values(features).filter(Boolean).length,
      totalCount: Object.keys(features).length
    }
  }

  /**
   * 测试Web APIs
   */
  testWebAPIs() {
    const apis = {
      geolocation: 'geolocation' in navigator,
      camera: 'mediaDevices' in navigator,
      notifications: 'Notification' in window,
      vibration: 'vibrate' in navigator,
      battery: 'getBattery' in navigator,
      deviceOrientation: 'DeviceOrientationEvent' in window,
      deviceMotion: 'DeviceMotionEvent' in window,
      fullscreen: 'requestFullscreen' in document.documentElement
    }

    return {
      apis,
      supportedCount: Object.values(apis).filter(Boolean).length,
      totalCount: Object.keys(apis).length
    }
  }

  /**
   * 测试viewport meta标签
   */
  testViewportMeta() {
    const viewportMeta = document.querySelector('meta[name="viewport"]')
    
    if (!viewportMeta) {
      return { exists: false, recommendation: '建议添加viewport meta标签' }
    }

    const content = viewportMeta.getAttribute('content') || ''
    const hasWidth = content.includes('width=device-width')
    const hasInitialScale = content.includes('initial-scale=1')
    
    return {
      exists: true,
      content,
      hasWidth,
      hasInitialScale,
      isOptimal: hasWidth && hasInitialScale
    }
  }

  /**
   * 测试触摸支持
   */
  testTouchSupport() {
    return {
      touchEvents: 'ontouchstart' in window,
      pointerEvents: 'PointerEvent' in window,
      maxTouchPoints: navigator.maxTouchPoints || 0,
      touchSupported: navigator.maxTouchPoints > 0 || 'ontouchstart' in window
    }
  }

  /**
   * 测试可访问性
   */
  async testAccessibility() {
    const accessibility = {
      colorContrast: this.testColorContrast(),
      focusManagement: this.testFocusManagement(),
      ariaLabels: this.testAriaLabels(),
      semanticHTML: this.testSemanticHTML()
    }

    this.testResults.accessibility = accessibility
  }

  /**
   * 测试颜色对比度
   */
  testColorContrast() {
    // 简化的对比度测试
    const elements = document.querySelectorAll('*')
    let lowContrastCount = 0
    let totalElements = 0

    elements.forEach(el => {
      const styles = window.getComputedStyle(el)
      const color = styles.color
      const backgroundColor = styles.backgroundColor
      
      if (color && backgroundColor && color !== 'rgba(0, 0, 0, 0)' && backgroundColor !== 'rgba(0, 0, 0, 0)') {
        totalElements++
        // 这里应该实现真正的对比度计算
        // 简化处理：如果颜色太相似则认为对比度低
        if (color === backgroundColor) {
          lowContrastCount++
        }
      }
    })

    return {
      totalElements,
      lowContrastCount,
      passRate: totalElements > 0 ? ((totalElements - lowContrastCount) / totalElements * 100).toFixed(2) : 100
    }
  }

  /**
   * 测试焦点管理
   */
  testFocusManagement() {
    const focusableElements = document.querySelectorAll(
      'a[href], button, input, textarea, select, [tabindex]:not([tabindex="-1"])'
    )
    
    let elementsWithFocusStyles = 0
    
    focusableElements.forEach(el => {
      const styles = window.getComputedStyle(el, ':focus')
      if (styles.outline !== 'none' || styles.boxShadow !== 'none') {
        elementsWithFocusStyles++
      }
    })

    return {
      totalFocusableElements: focusableElements.length,
      elementsWithFocusStyles,
      focusStylesRate: focusableElements.length > 0 ? 
        (elementsWithFocusStyles / focusableElements.length * 100).toFixed(2) : 0
    }
  }

  /**
   * 测试ARIA标签
   */
  testAriaLabels() {
    const elementsNeedingLabels = document.querySelectorAll('input, button, img')
    let elementsWithLabels = 0

    elementsNeedingLabels.forEach(el => {
      if (el.getAttribute('aria-label') || 
          el.getAttribute('aria-labelledby') || 
          el.getAttribute('alt') ||
          el.textContent?.trim()) {
        elementsWithLabels++
      }
    })

    return {
      totalElements: elementsNeedingLabels.length,
      elementsWithLabels,
      labelRate: elementsNeedingLabels.length > 0 ? 
        (elementsWithLabels / elementsNeedingLabels.length * 100).toFixed(2) : 100
    }
  }

  /**
   * 测试语义化HTML
   */
  testSemanticHTML() {
    const semanticElements = ['header', 'nav', 'main', 'section', 'article', 'aside', 'footer']
    const foundElements = semanticElements.filter(tag => 
      document.querySelector(tag)
    )

    return {
      semanticElements,
      foundElements,
      semanticRate: (foundElements.length / semanticElements.length * 100).toFixed(2)
    }
  }

  /**
   * 测试可用性
   */
  async testUsability() {
    const usability = {
      touchTargets: this.testTouchTargets(),
      textReadability: this.testTextReadability(),
      navigationUsability: this.testNavigationUsability(),
      formUsability: this.testFormUsability()
    }

    this.testResults.usability = usability
  }

  /**
   * 测试触摸目标大小
   */
  testTouchTargets() {
    const interactiveElements = document.querySelectorAll('button, a, input, [onclick]')
    let adequateSizeCount = 0
    const minSize = 44 // 最小触摸目标大小

    interactiveElements.forEach(el => {
      const rect = el.getBoundingClientRect()
      if (rect.width >= minSize && rect.height >= minSize) {
        adequateSizeCount++
      }
    })

    return {
      totalElements: interactiveElements.length,
      adequateSizeCount,
      adequacyRate: interactiveElements.length > 0 ? 
        (adequateSizeCount / interactiveElements.length * 100).toFixed(2) : 100
    }
  }

  /**
   * 测试文本可读性
   */
  testTextReadability() {
    const textElements = document.querySelectorAll('p, span, div, h1, h2, h3, h4, h5, h6')
    let readableCount = 0
    const minFontSize = 14 // 最小字体大小

    textElements.forEach(el => {
      const styles = window.getComputedStyle(el)
      const fontSize = parseFloat(styles.fontSize)
      if (fontSize >= minFontSize) {
        readableCount++
      }
    })

    return {
      totalElements: textElements.length,
      readableCount,
      readabilityRate: textElements.length > 0 ? 
        (readableCount / textElements.length * 100).toFixed(2) : 100
    }
  }

  /**
   * 测试导航可用性
   */
  testNavigationUsability() {
    const mobileNav = document.querySelector('.mobile-nav')
    const bottomNav = document.querySelector('.mobile-bottom-nav')
    const hamburgerMenu = document.querySelector('.menu-btn')

    return {
      hasMobileNav: !!mobileNav,
      hasBottomNav: !!bottomNav,
      hasHamburgerMenu: !!hamburgerMenu,
      mobileOptimized: !!(mobileNav && (bottomNav || hamburgerMenu))
    }
  }

  /**
   * 测试表单可用性
   */
  testFormUsability() {
    const forms = document.querySelectorAll('form')
    const inputs = document.querySelectorAll('input')
    let inputsWithLabels = 0
    let inputsWithPlaceholders = 0

    inputs.forEach(input => {
      const label = document.querySelector(`label[for="${input.id}"]`)
      if (label || input.getAttribute('aria-label')) {
        inputsWithLabels++
      }
      if (input.placeholder) {
        inputsWithPlaceholders++
      }
    })

    return {
      totalForms: forms.length,
      totalInputs: inputs.length,
      inputsWithLabels,
      inputsWithPlaceholders,
      labelRate: inputs.length > 0 ? (inputsWithLabels / inputs.length * 100).toFixed(2) : 100,
      placeholderRate: inputs.length > 0 ? (inputsWithPlaceholders / inputs.length * 100).toFixed(2) : 100
    }
  }

  /**
   * 生成测试报告
   */
  generateReport() {
    const { device, performance, compatibility, accessibility, usability } = this.testResults

    // 计算总体评分
    const scores = {
      performance: this.calculatePerformanceScore(performance),
      compatibility: this.calculateCompatibilityScore(compatibility),
      accessibility: this.calculateAccessibilityScore(accessibility),
      usability: this.calculateUsabilityScore(usability)
    }

    const overallScore = Object.values(scores).reduce((sum, score) => sum + score, 0) / 4

    return {
      timestamp: this.testResults.timestamp,
      device,
      scores,
      overallScore: Math.round(overallScore),
      details: {
        performance,
        compatibility,
        accessibility,
        usability
      },
      recommendations: this.generateRecommendations(scores)
    }
  }

  /**
   * 计算性能评分
   */
  calculatePerformanceScore(performance) {
    let score = 100

    // FPS评分
    if (performance.fps < 30) score -= 20
    else if (performance.fps < 50) score -= 10

    // 内存使用评分
    if (performance.memory.available && performance.memory.usagePercentage > 80) {
      score -= 15
    }

    // 滚动性能评分
    if (!performance.scrollPerformance.smooth) {
      score -= 15
    }

    // 触摸延迟评分
    if (performance.touchLatency.supported && !performance.touchLatency.responsive) {
      score -= 10
    }

    return Math.max(0, score)
  }

  /**
   * 计算兼容性评分
   */
  calculateCompatibilityScore(compatibility) {
    const cssScore = (compatibility.css.supportedCount / compatibility.css.totalCount) * 30
    const jsScore = (compatibility.javascript.supportedCount / compatibility.javascript.totalCount) * 30
    const apiScore = (compatibility.apis.supportedCount / compatibility.apis.totalCount) * 20
    const viewportScore = compatibility.viewport.isOptimal ? 20 : 0

    return Math.round(cssScore + jsScore + apiScore + viewportScore)
  }

  /**
   * 计算可访问性评分
   */
  calculateAccessibilityScore(accessibility) {
    const contrastScore = parseFloat(accessibility.colorContrast.passRate) * 0.3
    const focusScore = parseFloat(accessibility.focusManagement.focusStylesRate) * 0.3
    const labelScore = parseFloat(accessibility.ariaLabels.labelRate) * 0.2
    const semanticScore = parseFloat(accessibility.semanticHTML.semanticRate) * 0.2

    return Math.round(contrastScore + focusScore + labelScore + semanticScore)
  }

  /**
   * 计算可用性评分
   */
  calculateUsabilityScore(usability) {
    const touchScore = parseFloat(usability.touchTargets.adequacyRate) * 0.3
    const readabilityScore = parseFloat(usability.textReadability.readabilityRate) * 0.3
    const navScore = usability.navigationUsability.mobileOptimized ? 20 : 0
    const formScore = parseFloat(usability.formUsability.labelRate) * 0.2

    return Math.round(touchScore + readabilityScore + navScore + formScore)
  }

  /**
   * 生成改进建议
   */
  generateRecommendations(scores) {
    const recommendations = []

    if (scores.performance < 80) {
      recommendations.push('优化页面性能：减少JavaScript执行时间，优化图片加载')
    }

    if (scores.compatibility < 80) {
      recommendations.push('提升浏览器兼容性：添加必要的polyfill，优化CSS兼容性')
    }

    if (scores.accessibility < 80) {
      recommendations.push('改善可访问性：增加ARIA标签，提高颜色对比度，完善焦点管理')
    }

    if (scores.usability < 80) {
      recommendations.push('提升移动端可用性：增大触摸目标，优化导航结构，改善表单体验')
    }

    return recommendations
  }

  /**
   * 显示测试报告
   */
  displayReport(report) {
    console.log('\n📊 移动端测试报告')
    console.log('='.repeat(50))
    console.log(`测试时间: ${new Date(report.timestamp).toLocaleString()}`)
    console.log(`总体评分: ${report.overallScore}/100`)
    console.log('\n📈 各项评分:')
    console.log(`性能: ${report.scores.performance}/100`)
    console.log(`兼容性: ${report.scores.compatibility}/100`)
    console.log(`可访问性: ${report.scores.accessibility}/100`)
    console.log(`可用性: ${report.scores.usability}/100`)
    
    if (report.recommendations.length > 0) {
      console.log('\n💡 改进建议:')
      report.recommendations.forEach((rec, index) => {
        console.log(`${index + 1}. ${rec}`)
      })
    }
    
    console.log('\n📱 设备信息:')
    console.log(`屏幕尺寸: ${report.device.screen.width}x${report.device.screen.height}`)
    console.log(`视口尺寸: ${report.device.viewport.width}x${report.device.viewport.height}`)
    console.log(`像素比: ${report.device.viewport.devicePixelRatio}`)
    console.log(`网络类型: ${report.device.connection.effectiveType}`)
    
    console.log('\n✅ 测试完成!')
  }
}

// 创建全局实例
const mobileTestReportGenerator = new MobileTestReportGenerator()

// 导出
export default mobileTestReportGenerator
