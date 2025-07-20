/**
 * 移动端测试工具
 * 用于检测和测试移动端功能
 */

class MobileTestUtils {
  constructor() {
    this.testResults = [];
    this.isTestMode = process.env.NODE_ENV === 'development';
  }

  /**
   * 检测设备类型
   */
  detectDevice() {
    const userAgent = navigator.userAgent;
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent);
    const isTablet = /iPad|Android(?=.*\bMobile\b)(?=.*\bSafari\b)|Android(?=.*\bTablet\b)/i.test(userAgent);
    const isIOS = /iPad|iPhone|iPod/.test(userAgent);
    const isAndroid = /Android/.test(userAgent);

    return {
      isMobile,
      isTablet,
      isIOS,
      isAndroid,
      userAgent
    };
  }

  /**
   * 检测屏幕尺寸和方向
   */
  detectScreen() {
    return {
      width: window.innerWidth,
      height: window.innerHeight,
      devicePixelRatio: window.devicePixelRatio || 1,
      orientation: window.innerWidth > window.innerHeight ? 'landscape' : 'portrait',
      availableWidth: screen.availWidth,
      availableHeight: screen.availHeight
    };
  }

  /**
   * 检测触摸支持
   */
  detectTouch() {
    return {
      touchSupport: 'ontouchstart' in window || navigator.maxTouchPoints > 0,
      maxTouchPoints: navigator.maxTouchPoints || 0,
      pointerEvents: 'PointerEvent' in window
    };
  }

  /**
   * 测试移动端导航
   */
  testMobileNavigation() {
    const results = [];
    
    // 检测移动端导航组件
    const mobileNav = document.querySelector('.mobile-nav');
    const mobileBottomNav = document.querySelector('.mobile-bottom-nav');
    const desktopNav = document.querySelector('.header.desktop-only');

    results.push({
      test: 'Mobile Navigation Visibility',
      passed: !!mobileNav,
      message: mobileNav ? 'Mobile navigation found' : 'Mobile navigation not found'
    });

    results.push({
      test: 'Bottom Navigation Visibility',
      passed: !!mobileBottomNav,
      message: mobileBottomNav ? 'Bottom navigation found' : 'Bottom navigation not found'
    });

    results.push({
      test: 'Desktop Navigation Hidden on Mobile',
      passed: !desktopNav || window.getComputedStyle(desktopNav).display === 'none',
      message: 'Desktop navigation properly hidden on mobile'
    });

    return results;
  }

  /**
   * 测试响应式布局
   */
  testResponsiveLayout() {
    const results = [];
    const screenWidth = window.innerWidth;

    // 检测断点
    const breakpoints = {
      mobile: screenWidth <= 768,
      tablet: screenWidth > 768 && screenWidth <= 992,
      desktop: screenWidth > 992
    };

    results.push({
      test: 'Screen Width Detection',
      passed: true,
      message: `Screen width: ${screenWidth}px, Category: ${Object.keys(breakpoints).find(key => breakpoints[key])}`
    });

    // 检测容器宽度
    const containers = document.querySelectorAll('.container, .mobile-container');
    containers.forEach((container, index) => {
      const containerWidth = container.offsetWidth;
      const parentWidth = container.parentElement.offsetWidth;
      const widthRatio = containerWidth / parentWidth;

      results.push({
        test: `Container ${index + 1} Responsive Width`,
        passed: widthRatio <= 1,
        message: `Container width: ${containerWidth}px (${(widthRatio * 100).toFixed(1)}% of parent)`
      });
    });

    return results;
  }

  /**
   * 测试触摸目标大小
   */
  testTouchTargets() {
    const results = [];
    const minTouchSize = 44; // 最小触摸目标大小 (px)

    const touchElements = document.querySelectorAll('button, a, .nav-item, .el-button, input[type="submit"]');
    
    touchElements.forEach((element, index) => {
      const rect = element.getBoundingClientRect();
      const width = rect.width;
      const height = rect.height;
      const minDimension = Math.min(width, height);

      results.push({
        test: `Touch Target ${index + 1} Size`,
        passed: minDimension >= minTouchSize,
        message: `Size: ${width.toFixed(1)}x${height.toFixed(1)}px (min: ${minDimension.toFixed(1)}px)`
      });
    });

    return results;
  }

  /**
   * 测试字体大小
   */
  testFontSizes() {
    const results = [];
    const minFontSize = 14; // 最小字体大小 (px)

    const textElements = document.querySelectorAll('p, span, div, a, button, input, label');
    const fontSizes = new Map();

    textElements.forEach(element => {
      const fontSize = parseFloat(window.getComputedStyle(element).fontSize);
      if (fontSize > 0) {
        fontSizes.set(fontSize, (fontSizes.get(fontSize) || 0) + 1);
      }
    });

    fontSizes.forEach((count, size) => {
      results.push({
        test: `Font Size ${size}px`,
        passed: size >= minFontSize,
        message: `${count} elements with ${size}px font size`
      });
    });

    return results;
  }

  /**
   * 测试滚动性能
   */
  testScrollPerformance() {
    return new Promise((resolve) => {
      const results = [];
      let frameCount = 0;
      let startTime = performance.now();
      
      const measureFrames = () => {
        frameCount++;
        if (performance.now() - startTime < 1000) {
          requestAnimationFrame(measureFrames);
        } else {
          const fps = frameCount;
          results.push({
            test: 'Scroll Performance (FPS)',
            passed: fps >= 30,
            message: `Average FPS: ${fps}`
          });
          resolve(results);
        }
      };

      // 模拟滚动
      window.scrollBy(0, 10);
      requestAnimationFrame(measureFrames);
    });
  }

  /**
   * 运行所有测试
   */
  async runAllTests() {
    if (!this.isTestMode) {
      console.warn('Mobile tests should only run in development mode');
      return;
    }

    console.log('🔍 Running Mobile Tests...');
    
    const deviceInfo = this.detectDevice();
    const screenInfo = this.detectScreen();
    const touchInfo = this.detectTouch();

    console.log('📱 Device Info:', deviceInfo);
    console.log('📺 Screen Info:', screenInfo);
    console.log('👆 Touch Info:', touchInfo);

    const allResults = [
      ...this.testMobileNavigation(),
      ...this.testResponsiveLayout(),
      ...this.testTouchTargets(),
      ...this.testFontSizes(),
      ...(await this.testScrollPerformance())
    ];

    this.testResults = allResults;
    this.displayResults();
    
    return {
      deviceInfo,
      screenInfo,
      touchInfo,
      testResults: allResults
    };
  }

  /**
   * 显示测试结果
   */
  displayResults() {
    const passed = this.testResults.filter(r => r.passed).length;
    const total = this.testResults.length;
    
    console.log(`\n📊 Test Results: ${passed}/${total} passed`);
    
    this.testResults.forEach(result => {
      const icon = result.passed ? '✅' : '❌';
      console.log(`${icon} ${result.test}: ${result.message}`);
    });

    if (passed === total) {
      console.log('🎉 All mobile tests passed!');
    } else {
      console.log('⚠️ Some mobile tests failed. Please review the issues above.');
    }
  }

  /**
   * 创建测试报告
   */
  generateReport() {
    const report = {
      timestamp: new Date().toISOString(),
      device: this.detectDevice(),
      screen: this.detectScreen(),
      touch: this.detectTouch(),
      tests: this.testResults,
      summary: {
        total: this.testResults.length,
        passed: this.testResults.filter(r => r.passed).length,
        failed: this.testResults.filter(r => !r.passed).length
      }
    };

    return report;
  }
}

// 创建全局实例
const mobileTestUtils = new MobileTestUtils();

// 在开发模式下自动运行测试
if (process.env.NODE_ENV === 'development') {
  // 页面加载完成后运行测试
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      setTimeout(() => mobileTestUtils.runAllTests(), 1000);
    });
  } else {
    setTimeout(() => mobileTestUtils.runAllTests(), 1000);
  }
}

// 导出工具类
export default mobileTestUtils;
