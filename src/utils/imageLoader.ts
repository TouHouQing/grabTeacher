// 导入所有图片资源
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const imageModules: Record<string, any> = import.meta.glob('../assets/pictures/**/*.{jpg,jpeg,png,gif}', { eager: true });

/**
 * 获取图片的URL
 * @param path 图片路径，支持@/assets/路径
 * @returns 处理后的图片URL
 */
export function getImageUrl(path: string): string {
  if (!path) return '';

  // 处理@/路径
  if (path.startsWith('@/')) {
    const normalizedPath = path.replace('@/', '../');

    // 查找匹配的图片模块
    for (const key in imageModules) {
      if (key.includes(normalizedPath.split('/').pop() || '')) {
        // 使用eager模式导入时，直接可以访问default
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return (imageModules[key] as any).default;
      }
    }

    // 如果找不到匹配的模块，尝试从public目录加载
    return path.replace('@/', '/');
  }

  return path;
}

// 导出一个Vue插件
export default {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  install: (app: any) => {
    app.config.globalProperties.$getImageUrl = getImageUrl;
  }
}
