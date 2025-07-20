#!/usr/bin/env node

/**
 * 移动端测试启动脚本
 * 用于快速启动开发服务器并打开移动端测试页面
 */

const { spawn } = require('child_process');
const open = require('open');

console.log('🚀 Starting Mobile Development Server...');

// 启动开发服务器
const devServer = spawn('npm', ['run', 'dev'], {
  stdio: 'inherit',
  shell: true
});

// 等待服务器启动
setTimeout(() => {
  console.log('\n📱 Opening Mobile Test URLs...');
  
  const baseUrl = 'http://localhost:5173';
  
  // 不同设备的测试URL
  const testUrls = [
    // iPhone SE
    `${baseUrl}?mobile-test=iphone-se`,
    // iPhone 12
    `${baseUrl}?mobile-test=iphone-12`,
    // iPad
    `${baseUrl}?mobile-test=ipad`,
    // Android
    `${baseUrl}?mobile-test=android`
  ];

  // 打开第一个测试URL
  open(testUrls[0]);
  
  console.log('\n📋 Mobile Test Checklist:');
  console.log('1. ✅ Navigation menu works on mobile');
  console.log('2. ✅ Bottom navigation is visible and functional');
  console.log('3. ✅ Forms are properly sized for mobile input');
  console.log('4. ✅ Touch targets are at least 44px');
  console.log('5. ✅ Content is readable without zooming');
  console.log('6. ✅ Images and media scale properly');
  console.log('7. ✅ Page layouts work in both orientations');
  console.log('8. ✅ Performance is smooth on mobile devices');
  
  console.log('\n🔗 Test URLs:');
  testUrls.forEach((url, index) => {
    const devices = ['iPhone SE', 'iPhone 12', 'iPad', 'Android'];
    console.log(`   ${devices[index]}: ${url}`);
  });
  
  console.log('\n💡 Tips:');
  console.log('- Use Chrome DevTools Device Mode for testing');
  console.log('- Test on real devices when possible');
  console.log('- Check console for mobile test results');
  console.log('- Verify touch interactions work properly');
  
}, 3000);

// 处理进程退出
process.on('SIGINT', () => {
  console.log('\n🛑 Stopping development server...');
  devServer.kill('SIGINT');
  process.exit(0);
});

devServer.on('close', (code) => {
  console.log(`\n📊 Development server exited with code ${code}`);
  process.exit(code);
});
