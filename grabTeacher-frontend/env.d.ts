/// <reference types="vite/client" />

// 解决Vue导入问题
declare module 'vue' {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export const ref: <T = any>(value?: T) => { value: T };

  export const reactive: <T extends object>(target: T) => T;

  export const computed: <T>(getter: () => T) => { readonly value: T };

  export const onMounted: (callback: () => void) => void;
  export const onUnmounted: (callback: () => void) => void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export const defineOptions: (options: Record<string, any>) => void;

  export const markRaw: <T extends object>(value: T) => T;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export const watch: any;

  export const watchEffect: (effect: (onCleanup: (cleanupFn: () => void) => void) => void) => void;

  export const nextTick: (callback?: () => void) => Promise<void>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export const toRef: any;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export const toRefs: any;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export const provide: any;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export const inject: any;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  export const createApp: any;
}



// 解决图片导入问题
declare module '*.jpg' {
  const src: string
  export default src
}

declare module '*.jpeg' {
  const src: string
  export default src
}

declare module '*.png' {
  const src: string
  export default src
}

declare module '*.gif' {
  const src: string
  export default src
}

declare module '*.svg' {
  const src: string
  export default src
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  // 更多环境变量...
}

interface ImportMeta {
  readonly env: ImportMetaEnv
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  readonly glob: (pattern: string) => Record<string, () => Promise<any>>
}

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $getImageUrl: (url: string) => string | Promise<{ default: string }>
  }
}
