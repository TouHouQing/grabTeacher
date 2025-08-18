<template>
  <div class="avatar-uploader">
    <div class="avatar-preview">
      <img
        :src="displayAvatarUrl"
        :alt="altText"
        @error="handleImageError"
        class="avatar-image"
        :style="{ width: size + 'px', height: size + 'px' }"
      />
    </div>
    <div class="upload-controls" v-if="!readonly">
      <input
        type="file"
        accept="image/*"
        @change="handleFileSelect"
        ref="fileInputRef"
        class="file-input"
      />
      <el-button
        v-if="showUploadButton && selectedFile"
        size="small"
        type="primary"
        @click="handleUpload"
        :loading="uploading"
        :disabled="!selectedFile"
      >
        {{ uploading ? '上传中...' : '上传' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fileAPI } from '../utils/api'

interface Props {
  modelValue?: string
  defaultAvatar?: string
  size?: number
  altText?: string
  readonly?: boolean
  showUploadButton?: boolean
  uploadModule?: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'upload-success', url: string): void
  (e: 'upload-error', error: string): void
}

const props = withDefaults(defineProps<Props>(), {
  size: 48,
  altText: '头像',
  readonly: false,
  showUploadButton: true,
  uploadModule: 'avatar'
})

const emit = defineEmits<Emits>()

const fileInputRef = ref<HTMLInputElement>()
const selectedFile = ref<File | null>(null)
const previewUrl = ref<string | null>(null)
const uploading = ref(false)

const displayAvatarUrl = computed(() => {
  return previewUrl.value || props.modelValue || props.defaultAvatar || ''
})

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) {
    selectedFile.value = null
    previewUrl.value = null
    return
  }

  // 文件类型验证
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }

  // 文件大小验证
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB')
    return
  }

  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)

  // 如果不显示上传按钮，直接上传
  if (!props.showUploadButton) {
    handleUpload()
  }
}

const handleUpload = async () => {
  if (!selectedFile.value) return

  uploading.value = true
  try {
    const url = await fileAPI.presignAndPut(selectedFile.value, props.uploadModule)
    const finalUrl = url.split('?')[0] // 去掉签名参数

    emit('update:modelValue', finalUrl)
    emit('upload-success', finalUrl)

    selectedFile.value = null
    previewUrl.value = null

    ElMessage.success('头像上传成功')
  } catch (error: any) {
    emit('upload-error', error.message || '上传失败')
    ElMessage.error(error.message || '头像上传失败')
  } finally {
    uploading.value = false
  }
}

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  if (props.defaultAvatar) {
    img.src = props.defaultAvatar
  }
}

// 清理预览URL
watch(previewUrl, (newUrl, oldUrl) => {
  if (oldUrl && oldUrl.startsWith('blob:')) {
    URL.revokeObjectURL(oldUrl)
  }
})
</script>

<style scoped>
.avatar-uploader {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-preview {
  flex-shrink: 0;
}

.avatar-image {
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #eee;
  background-color: #f5f5f5;
}

.upload-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-input {
  font-size: 14px;
}
</style>
