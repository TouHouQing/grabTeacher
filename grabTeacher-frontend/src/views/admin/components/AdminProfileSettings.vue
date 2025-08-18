<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getApiBaseUrl } from '../../../utils/env'
import { fileAPI } from '../../../utils/api'

const apiBase = getApiBaseUrl()

// 管理员资料表单
const form = reactive({
  realName: '',
  whatsappNumber: '',
  email: '',
  avatarUrl: '',
  wechatQrcodeUrl: ''
})

const loading = ref(false)
const uploadLoading = ref(false)

// 本会话内最近一次“临时上传”的URL（用于在下次上传成功后删除上一次临时文件）
const lastTempAvatarUrl = ref<string | null>(null)
const lastTempQrcodeUrl = ref<string | null>(null)

// 初始化：获取当前管理员资料
async function fetchProfile() {
  try {
    loading.value = true
    const resp = await fetch(`${apiBase}/api/admin/profile`, { credentials: 'include', headers: { 'Authorization': `Bearer ${localStorage.getItem('token')||''}` } })
    const data = await resp.json()
    if (data?.success && data.data) {
      Object.assign(form, data.data)
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchProfile)

// 本地预览用的临时对象URL
const avatarPreview = ref<string | null>(null)
const qrcodePreview = ref<string | null>(null)
let avatarFile: File | null = null
let qrcodeFile: File | null = null

async function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || !input.files[0]) return
  avatarFile = input.files[0]
  // 仅做本地预览，不直传OSS
  avatarPreview.value = URL.createObjectURL(avatarFile)
  input.value = ''
}

async function onQrcodeChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || !input.files[0]) return
  qrcodeFile = input.files[0]
  qrcodePreview.value = URL.createObjectURL(qrcodeFile)
  input.value = ''
}

// 保存时：若选择了新文件，则先直传到OSS，再调用后端更新资料（后端会删除旧文件）
async function uploadIfNeeded(file: File | null, field: 'avatar'|'qrcode'): Promise<string | null> {
  if (!file) return null
  const module = `admin/${field}`
  return await fileAPI.presignAndPut(file, module)
}

async function submit() {
  try {
    loading.value = true

    // 1) 若选择了新头像/二维码，先上传至OSS，拿到最终URL
    const uploadedAvatarUrl = await uploadIfNeeded(avatarFile, 'avatar')
    const uploadedQrcodeUrl = await uploadIfNeeded(qrcodeFile, 'qrcode')

    // 2) 组装要提交的数据：未选择新文件则沿用现有值
    const payload = { ...form }
    if (uploadedAvatarUrl) payload.avatarUrl = uploadedAvatarUrl
    if (uploadedQrcodeUrl) payload.wechatQrcodeUrl = uploadedQrcodeUrl

    const resp = await fetch(`${apiBase}/api/admin/profile`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${localStorage.getItem('token')||''}` },
      body: JSON.stringify(payload)
    })
    const data = await resp.json()

    if (data.success) {
      ElMessage.success('更新成功')
      // 清理文件状态
      avatarFile = null
      qrcodeFile = null
      avatarPreview.value = null
      qrcodePreview.value = null
      // 刷新服务端数据
      await fetchProfile()
    } else {
      ElMessage.error(data.message || '更新失败')
    }
  } catch (e:any) {
    ElMessage.error(e.message || '网络错误')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-profile-settings">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>管理员资料</span>
        </div>
      </template>

      <el-form label-width="120px" :model="form" :disabled="loading">
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="请输入管理员姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" type="email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="WhatsApp 号码">
          <el-input v-model="form.whatsappNumber" placeholder="请输入WhatsApp号码" />
        </el-form-item>

        <el-form-item label="头像">
          <div class="uploader">
            <img v-if="avatarPreview || form.avatarUrl" :src="avatarPreview || form.avatarUrl" class="preview" alt="avatar" />
            <el-button :loading="uploadLoading" type="primary" @click="$refs.avatarInput.click()">上传/更换头像</el-button>
            <input ref="avatarInput" type="file" accept="image/*" class="hidden-input" @change="onAvatarChange" />
          </div>
        </el-form-item>

        <el-form-item label="微信二维码">
          <div class="uploader">
            <img v-if="qrcodePreview || form.wechatQrcodeUrl" :src="qrcodePreview || form.wechatQrcodeUrl" class="preview" alt="qrcode" />
            <el-button :loading="uploadLoading" @click="$refs.qrcodeInput.click()">上传/更换二维码</el-button>
            <input ref="qrcodeInput" type="file" accept="image/*" class="hidden-input" @change="onQrcodeChange" />
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.admin-profile-settings { padding: 20px; }
.card-header { font-weight: 600; }
.uploader { display: flex; align-items: center; gap: 12px; }
.preview { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; border: 1px solid #eee; }
.hidden-input { display: none; }
</style>

